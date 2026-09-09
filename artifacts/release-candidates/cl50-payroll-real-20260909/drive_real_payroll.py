# -*- coding: utf-8 -*-
"""CL50 真实工资链驱动器。

真 HTTP、真登录、真 MySQL。不现签 JWT——每个身份都走 /api/auth/login 拿真 token。
只读写隔离 schema co_pay50_20260909，不 DROP、不 DELETE、不 reset，失败现场原样保留。
断言全部对着数据库实查，不拿接口回执自证。
"""
import json
import os
import subprocess
import sys
import urllib.error
import urllib.request

BASE = "http://127.0.0.1:18095"
SCHEMA = "co_pay50_20260909"
MYSQL = r"C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql"
MONTH = "2026-08"

passed = []
failed = []


def sql(q):
    r = subprocess.run([MYSQL, "-h", "127.0.0.1", "-P", "13317", "-u", "root",
                        "--skip-password", "-N", "-B", SCHEMA, "-e", q],
                       capture_output=True, text=True, encoding="utf-8")
    if r.returncode != 0:
        raise RuntimeError("SQL failed: " + q + "\n" + (r.stderr or ""))
    return [ln.split("\t") for ln in (r.stdout or "").strip().split("\n") if ln.strip()]


def one(q):
    rows = sql(q)
    return rows[0][0] if rows else None


def http(method, path, token=None, body=None, params=None):
    url = BASE + path
    if params:
        url += "?" + "&".join(k + "=" + str(v) for k, v in params.items())
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    if data is not None:
        req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8") or "{}")
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "replace")
        try:
            return e.code, json.loads(raw or "{}")
        except ValueError:
            return e.code, {"_raw": raw[:200]}


def check(name, cond, detail=""):
    line = name + ((" | " + detail) if detail else "")
    (passed if cond else failed).append(line)
    print(("PASS  " if cond else "FAIL  ") + line)


def login(account, password):
    st, body = http("POST", "/api/auth/login", body={"username": account, "password": password})
    if st != 200 or body.get("code") != 200:
        raise RuntimeError("login failed for %s: %s %s" % (account, st, body.get("message")))
    return body["data"]["token"]


def items(ids, base="1000", post="200", att="300", bonus="40", ot="50", allow="5"):
    return [{"emp_id": i, "base_salary": base, "post_salary": post, "attendance_pay": att,
             "bonus": bonus, "overtime_pay": ot, "allowance": allow} for i in ids]


def payroll(action, token, month, body=None):
    return http("POST", "/api/hr/payroll/" + action, token=token, body=body,
                params={"month": month})


def cur_status():
    return one("SELECT status FROM month_salary WHERE staff_id=2 AND salary_month='" + MONTH + "'")


def main():
    pw = open(os.path.join(os.environ["SCRATCH"], "cl50pw.txt")).read().strip()

    print("=== 0. 起点：隔离库现状 ===")
    print("month_salary=%s payroll_payout_record=%s audit_logs=%s"
          % (one("SELECT COUNT(*) FROM month_salary"),
             one("SELECT COUNT(*) FROM payroll_payout_record"),
             one("SELECT COUNT(*) FROM audit_logs")))

    print("\n=== A. 无 JWT：四个入口都必须挡在任何写入之前 ===")
    for ep in ["save", "approve", "payout"]:
        st, _ = payroll(ep, None, MONTH)
        check("A 无JWT POST /" + ep + " 返回401", st == 401, "实际 " + str(st))
    st, _ = http("GET", "/api/hr/payroll", params={"month": MONTH})
    check("A 无JWT GET 列表 返回401", st == 401, "实际 " + str(st))
    check("A 无JWT 后 month_salary 仍为 0", one("SELECT COUNT(*) FROM month_salary") == "0")
    check("A 无JWT 后 audit_logs 仍为 0", one("SELECT COUNT(*) FROM audit_logs") == "0")

    print("\n=== B. 真实登录（走 /api/auth/login，不现签 JWT）===")
    tok_appr = login("synthetic_approver", pw)
    tok_worker = login("synthetic_worker", pw)
    tok_gm = login("synthetic_gm", pw)
    tok_outsider = login("synthetic_outsider", pw)
    check("B 审批人真实登录拿到 token", len(tok_appr.split(".")) == 3, "JWT 三段")
    check("B 门店员工真实登录拿到 token", len(tok_worker.split(".")) == 3)
    check("B 总经理真实登录拿到 token", len(tok_gm.split(".")) == 3)
    st, b = http("POST", "/api/auth/login",
                 body={"username": "synthetic_approver", "password": pw + "x"})
    check("B 错误密码被拒", b.get("code") == 401, "code=" + str(b.get("code")))

    print("\n=== C. 无权身份（can_manage_hr=0）必须被拒且零写入 ===")
    st, b = payroll("save", tok_outsider, MONTH, items([2]))
    check("C 无权身份 save 被拒", b.get("code") != 200, "code=" + str(b.get("code")))
    check("C 无权身份 save 后 month_salary 仍为 0",
          one("SELECT COUNT(*) FROM month_salary") == "0")

    print("\n=== D. 跨店：一店审批人不能把二店员工混进同一批 ===")
    st, b = payroll("save", tok_appr, MONTH, items([2, 3]))
    check("D 跨店混合批被拒", b.get("code") == 400,
          "code=%s msg=%s" % (b.get("code"), b.get("message")))
    check("D 跨店被拒后 month_salary 仍为 0（整批回滚）",
          one("SELECT COUNT(*) FROM month_salary") == "0")

    print("\n=== E. 保存 → 状态 1 ===")
    st, b = payroll("save", tok_appr, MONTH, items([2]))
    check("E save 成功", b.get("code") == 200,
          "code=%s msg=%s" % (b.get("code"), b.get("message")))
    d = b.get("data") or {}
    check("E save 回执带 month", d.get("month") == MONTH, "回执 month=" + str(d.get("month")))
    check("E save 回执 saved=1", d.get("saved") == 1, "saved=" + str(d.get("saved")))
    net = one("SELECT net_salary FROM month_salary WHERE staff_id=2 AND salary_month='"
              + MONTH + "'")
    check("E 落库实发 = 1595.00（1000+200+300+40+50+5）", net == "1595.00",
          "库内 net_salary=" + str(net))
    check("E 落库状态 = 1 已保存", cur_status() == "1", "status=" + str(cur_status()))

    print("\n=== F. 列表：门店范围与已知数字 ===")
    st, b = http("GET", "/api/hr/payroll", token=tok_appr, params={"month": MONTH})
    rows = b.get("data") or []
    ids = [r.get("emp_id") for r in rows]
    check("F 一店审批人看不到二店员工(emp_id=3)", 3 not in ids, "看到 " + str(ids))
    row2 = None
    for r in rows:
        if r.get("emp_id") == 2:
            row2 = r
    check("F 列表含 emp_id=2", row2 is not None)
    if row2:
        check("F 列表 net_pay=1595",
              str(row2.get("net_pay")) in ("1595", "1595.00", "1595.0"),
              "net_pay=" + str(row2.get("net_pay")))
        check("F 列表 attendance_pay=300",
              str(row2.get("attendance_pay")) in ("300", "300.00", "300.0"),
              "attendance_pay=" + str(row2.get("attendance_pay")))
    st, b = http("GET", "/api/hr/payroll", token=tok_gm, params={"month": MONTH})
    gm_ids = [r.get("emp_id") for r in (b.get("data") or [])]
    check("F 总经理全门店范围能看到二店员工", 3 in gm_ids, "看到 " + str(gm_ids))

    print("\n=== G. 未审批就发放记账：必须拒绝且零写入 ===")
    before_ppr = one("SELECT COUNT(*) FROM payroll_payout_record")
    st, b = payroll("payout", tok_appr, MONTH)
    check("G 未审批 payout 被拒", b.get("code") != 200,
          "code=%s msg=%s" % (b.get("code"), b.get("message")))
    check("G 未审批 payout 后台账仍为 " + str(before_ppr) + "（零写入）",
          one("SELECT COUNT(*) FROM payroll_payout_record") == before_ppr)
    check("G 未审批 payout 后状态仍为 1", cur_status() == "1", "status=" + str(cur_status()))

    print("\n=== H. 不在批复白名单的人不能审批 ===")
    st, b = payroll("approve", tok_worker, MONTH)
    check("H 非批复人 approve 被拒", b.get("code") != 200,
          "code=%s msg=%s" % (b.get("code"), b.get("message")))
    check("H 非批复人被拒后状态仍为 1", cur_status() == "1", "status=" + str(cur_status()))

    print("\n=== I. 审批 1 → 2 ===")
    st, b = payroll("approve", tok_appr, MONTH)
    check("I approve 成功", b.get("code") == 200,
          "code=%s msg=%s" % (b.get("code"), b.get("message")))
    d = b.get("data") or {}
    check("I approve 回执带 month", d.get("month") == MONTH, "month=" + str(d.get("month")))
    check("I approve 回执 approved=1", d.get("approved") == 1,
          "approved=" + str(d.get("approved")))
    check("I 落库状态 = 2 已审批", cur_status() == "2", "status=" + str(cur_status()))
    check("I 落库审批人 = 真实登录账号 synthetic_approver",
          one("SELECT approved_by FROM month_salary WHERE staff_id=2 AND salary_month='"
              + MONTH + "'") == "synthetic_approver")

    print("\n=== J. 发放记账 2 → 3 + 台账 ===")
    st, b = payroll("payout", tok_appr, MONTH)
    check("J payout 成功", b.get("code") == 200,
          "code=%s msg=%s" % (b.get("code"), b.get("message")))
    d = b.get("data") or {}
    check("J payout 回执带 month", d.get("month") == MONTH, "month=" + str(d.get("month")))
    check("J payout 回执 paid=1", d.get("paid") == 1, "paid=" + str(d.get("paid")))
    check("J 回执明说只是记账不是银行到账",
          "不代表银行" in str(d.get("message", "")), "message=" + str(d.get("message")))
    check("J 落库状态 = 3 已发放记账", cur_status() == "3", "status=" + str(cur_status()))
    ppr = sql("SELECT payout_id,salary_month,headcount,total_net,recorded_by "
              "FROM payroll_payout_record")
    check("J 台账恰好 1 条", len(ppr) == 1, "实际 " + str(len(ppr)) + " 条")
    if ppr:
        pid_, m_, hc_, tn_, rb_ = ppr[0]
        check("J 台账 salary_month 与请求月一致", m_ == MONTH, "台账 month=" + str(m_))
        check("J 台账人数 = 库内该批人数",
              hc_ == one("SELECT COUNT(*) FROM month_salary WHERE payout_id=" + pid_),
              "headcount=" + str(hc_))
        check("J 台账合计 = 库内该批实发合计",
              tn_ == one("SELECT COALESCE(SUM(net_salary),0) FROM month_salary WHERE payout_id="
                         + pid_),
              "total_net=" + str(tn_))
        check("J 台账记账人 = 真实登录账号", rb_ == "synthetic_approver",
              "recorded_by=" + str(rb_))
    check("J 无孤儿：批次号指向的台账都存在且门店一致", one(
        "SELECT COUNT(*) FROM month_salary m LEFT JOIN payroll_payout_record p "
        "ON m.payout_id=p.payout_id AND m.salary_month=p.salary_month "
        "WHERE m.payout_id IS NOT NULL AND (p.payout_id IS NULL OR "
        "(p.store_id IS NOT NULL AND p.store_id<>m.store_id))") == "0")

    print("\n=== K. 重复发放记账：不得产生第二笔 ===")
    snap_ms = sql("SELECT salary_id,status,net_salary,payout_id FROM month_salary "
                  "ORDER BY salary_id")
    snap_pr = sql("SELECT payout_id,headcount,total_net FROM payroll_payout_record "
                  "ORDER BY payout_id")
    st, b = payroll("payout", tok_appr, MONTH)
    d = b.get("data") or {}
    check("K 重复 payout 明确回 alreadyRecorded", d.get("alreadyRecorded") is True,
          "data=" + json.dumps(d, ensure_ascii=False)[:160])
    check("K 重复 payout 后 month_salary 逐行未变",
          sql("SELECT salary_id,status,net_salary,payout_id FROM month_salary "
              "ORDER BY salary_id") == snap_ms)
    check("K 重复 payout 后台账逐行未变",
          sql("SELECT payout_id,headcount,total_net FROM payroll_payout_record "
              "ORDER BY payout_id") == snap_pr)

    print("\n=== L. 已发放记账后再保存：不得倒退状态、不得改金额 ===")
    st, b = payroll("save", tok_appr, MONTH,
                    items([2], base="9999", post="9999", att="9999"))
    after = sql("SELECT status,net_salary FROM month_salary WHERE staff_id=2 "
                "AND salary_month='" + MONTH + "'")[0]
    check("L 再保存后状态仍为 3（未被退回已保存）", after[0] == "3", "状态=" + str(after[0]))
    check("L 再保存后实发仍为 1595.00（金额未被改写）", after[1] == "1595.00",
          "net=" + str(after[1]))

    print("\n=== M. 审计留痕 ===")
    n_audit = one("SELECT COUNT(*) FROM audit_logs")
    check("M 全链留下审计记录（>=4 条）", int(n_audit) >= 4, "audit_logs=" + str(n_audit))

    print("\n================ 汇总 ================")
    print("PASSED=%d FAILED=%d" % (len(passed), len(failed)))
    for f in failed:
        print("  FAILED: " + f)
    return 0 if not failed else 1


if __name__ == "__main__":
    sys.exit(main())
