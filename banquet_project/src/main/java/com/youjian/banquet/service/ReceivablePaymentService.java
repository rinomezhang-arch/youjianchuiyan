package com.youjian.banquet.service;

import com.youjian.banquet.entity.ReceivablePaymentRequest;
import com.youjian.banquet.repository.ReceivablePaymentRequestRepository;
import com.youjian.banquet.util.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 应收创建 → 部分收款 → 收清 的唯一入口。
 * <p>
 * 基线 {@code FinanceController} 的六个方法有几处必须修掉的问题，每一处都直接关系到钱：
 * <ol>
 *   <li><b>收款与应收根本没有关联</b>：{@code createPaymentRecord} 压根不读请求里的
 *       {@code receivableId}，{@code finance_receivable} 的已收/待收/状态永不更新。
 *       "部分收款→收清"这条闭环在基线里不是有 bug，是<b>不存在</b>；</li>
 *   <li><b>金额用 double</b>：与实体的 {@code BigDecimal(12,2)} 冲突，浮点误差直接进账；</li>
 *   <li><b>主键用 System.currentTimeMillis()</b>：两列本来都是自增，显式赋值既与自增冲突，
 *       同毫秒并发还会撞主键；</li>
 *   <li><b>物理删除</b>：历史应收与收款被 DELETE 抹掉，查不回来；</li>
 *   <li><b>没有幂等</b>：超时重发或点两次就多一条应收、多收一笔款。</li>
 * </ol>
 * <p>
 * 本服务的规则：金额全程 {@link BigDecimal}；{@code total = received + pending} 每一步守恒；
 * 引用要么为空、要么真实且同店，<b>绝不构造假父记录</b>；超额支付拒绝（不生成负余额，也不做预收）；
 * 删除一律拒绝，历史留痕。
 * <p>
 * <b>本服务不写生产库、不执行真实收付款。</b>
 */
@Service
public class ReceivablePaymentService {

    private static final Logger log = LoggerFactory.getLogger(ReceivablePaymentService.class);

    public static final String OP_RECEIVABLE = "receivable";
    public static final String OP_PAYMENT = "payment";

    public static final String STATUS_UNPAID = "unpaid";
    public static final String STATUS_PARTIAL = "partial";
    public static final String STATUS_PAID = "paid";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ReceivablePaymentRequestRepository requestRepository;

    /** 日历基准。生产用系统时钟；测试注入固定时钟推进"今天"，不改系统时钟。 */
    private Clock clock = Clock.systemDefaultZone();

    void setClock(Clock clock) {
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    // ==================== 异常 ====================

    /** 无身份或跨店。对外 403。 */
    public static class ReceivableAccessDeniedException extends IllegalArgumentException {
        public ReceivableAccessDeniedException(String message) { super(message); }
    }

    /** 同键换参数等冲突。对外 409。 */
    public static class ReceivableConflictException extends IllegalArgumentException {
        public ReceivableConflictException(String message) { super(message); }
    }

    /** 同键在途或并发撞锁：本次未落库，可用同一 requestId 安全重试。对外 409。 */
    public static class ReceivableRetryableException extends IllegalArgumentException {
        public ReceivableRetryableException() {
            super("该 requestId 的请求正在处理或刚刚完成，本次未产生新记录。"
                    + "请用同一个 requestId 重试以取回原回执");
        }
    }

    /** 历史记录不允许物理删除。对外 409。 */
    public static class DeletionNotAllowedException extends IllegalArgumentException {
        public DeletionNotAllowedException(String what) {
            super(what + "属于账务历史，不支持删除。如需更正请通过冲销或新增记录处理，"
                    + "保留原始留痕以便对账");
        }
    }

    /** 冲突文案：与具体单据无关，避免拿一个 key 探测别店数据。 */
    static final String CONFLICT_MESSAGE =
            "该 requestId 此前已用于另一次提交，且与本次的业务参数不一致，无法按重试处理。"
                    + "重试请沿用原来的参数；这是一次新的业务请改用新的 requestId";

    // ==================== 命令与结果 ====================

    /** 创建应收的入参。引用字段允许为空，但给了就必须真实且同店。 */
    public static class ReceivableCommand {
        public Long storeId;
        public String receivableNo;
        public Integer customerId;
        public String customerName;
        public String bookingId;      // 业务字符串，不是数字 id
        public String bookingNo;
        public BigDecimal totalAmount;
        public LocalDate receivableDate;
        public LocalDate dueDate;
        public Integer creditDays;
        public String remark;
    }

    /** 登记收款的入参。 */
    public static class PaymentCommand {
        public Long storeId;
        public String paymentNo;
        public LocalDate paymentDate;
        public Long receivableId;     // 可空：无来源手工收款是既有业务事实
        public Integer customerId;
        public String customerName;
        public String bookingId;
        public String bookingNo;
        public BigDecimal amount;
        public String paymentMethod;
        public Long accountId;
        public String category;       // 手工收款（无应收来源）时必填
        public String remark;         // 手工收款（无应收来源）时必填
    }

    /** 统一回执。{@code replayed=true} 表示这次是重试取回原记录，没有新增。 */
    public static class OperationResult {
        public final String requestId;
        public final Long id;
        public final String no;
        public final boolean replayed;
        public final Map<String, Object> snapshot;

        public OperationResult(String requestId, Long id, String no, boolean replayed,
                               Map<String, Object> snapshot) {
            this.requestId = requestId;
            this.id = id;
            this.no = no;
            this.replayed = replayed;
            this.snapshot = snapshot;
        }
    }

    // ==================== 应收创建 ====================

    @Transactional
    public OperationResult createReceivable(ReceivableCommand cmd, String requestId) {
        String key = requireRequestId(requestId, "创建应收");
        if (cmd == null) throw new IllegalArgumentException("请填写应收单");
        Long storeId = requireStore(cmd.storeId);

        BigDecimal total = requireAmount(cmd.totalAmount, "应收金额", false);

        // bookingNo 会真的落库，漏进指纹的话同键改这个字段会被当成原样重试而返回旧回执。
        String fingerprint = fingerprint(
                OP_RECEIVABLE, storeId, blankToNull(cmd.receivableNo), cmd.customerId,
                blankToNull(cmd.customerName), blankToNull(cmd.bookingId), blankToNull(cmd.bookingNo),
                total.toPlainString(), cmd.receivableDate, cmd.dueDate, cmd.creditDays,
                blankToNull(cmd.remark));

        Optional<ReceivablePaymentRequest> prior = requestRepository.findByRequestId(key);
        if (prior.isPresent()) {
            // **重放判定必须早于引用校验。** 重放的用途是把一次结果未知的请求的原回执取回来，
            // 而引用（客户、预订单）是会变的：当初合法的客户后来被停用、删除或改了门店，
            // 若先校验引用，恢复就会被一个与本次操作无关的历史变更挡死，
            // 调用方永远拿不回那张已经存在的单据，只能猜"到底记没记"。
            // 指纹一致即证明业务参数与当初完全相同，返回原回执不会产生任何新的写入。
            return replay(prior.get(), key, fingerprint, OP_RECEIVABLE);
        }

        // 到这里才是**新登记**：引用给了就必须真实且同店；为空则放行，不构造假父记录。
        validateCustomer(cmd.customerId, storeId);
        validateBooking(cmd.bookingId, storeId);

        // 日期默认值在指纹之后才补，避免"没传日期"的请求指纹随当天日期漂移，跨天恢复被误判成改参数。
        LocalDate receivableDate = cmd.receivableDate != null ? cmd.receivableDate : today();
        Integer creditDays = cmd.creditDays != null ? cmd.creditDays : 30;
        LocalDate dueDate = cmd.dueDate != null ? cmd.dueDate : receivableDate.plusDays(creditDays);
        if (dueDate.isBefore(receivableDate)) {
            throw new IllegalArgumentException("到期日不能早于应收日期");
        }
        String operator = requireOperator();

        KeyHolder keys = new GeneratedKeyHolder();
        final LocalDate rDate = receivableDate, dDate = dueDate;
        final Integer days = creditDays;
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO finance_receivable (store_id, receivable_no, customer_id, customer_name, "
                            + "booking_id, booking_no, total_amount, received_amount, pending_amount, "
                            + "receivable_date, due_date, status, credit_days, operator_name, remark, created_at) "
                            + "VALUES (?,?,?,?,?,?,?,0,?,?,?,?,?,?,?,NOW())",
                    Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setLong(i++, storeId);
            ps.setString(i++, blankToNull(cmd.receivableNo) == null
                    ? "RV" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase()
                    : cmd.receivableNo.trim());
            setNullableInt(ps, i++, cmd.customerId);
            ps.setString(i++, blankToNull(cmd.customerName));
            ps.setString(i++, blankToNull(cmd.bookingId));
            ps.setString(i++, blankToNull(cmd.bookingNo));
            ps.setBigDecimal(i++, total);
            ps.setBigDecimal(i++, total);           // pending 初值 = total，received 恒为 0
            ps.setObject(i++, rDate);
            ps.setObject(i++, dDate);
            ps.setString(i++, STATUS_UNPAID);
            ps.setInt(i++, days);
            ps.setString(i++, operator);
            ps.setString(i, blankToNull(cmd.remark));
            return ps;
        }, keys);
        Number generated = keys.getKey();
        if (generated == null) throw new IllegalStateException("应收单未返回主键");
        long receivableId = generated.longValue();
        String receivableNo = jdbc.queryForObject(
                "SELECT receivable_no FROM finance_receivable WHERE receivable_id=?", String.class, receivableId);

        register(key, OP_RECEIVABLE, storeId, receivableId, receivableNo, fingerprint, operator);
        log.info("【应收创建】{} 门店 {} 创建应收 {}，金额 {}", operator, storeId, receivableId, total);
        return new OperationResult(key, receivableId, receivableNo, false, receivableSnapshot(receivableId));
    }

    // ==================== 收款登记 ====================

    @Transactional
    public OperationResult recordPayment(PaymentCommand cmd, String requestId) {
        String key = requireRequestId(requestId, "登记收款");
        if (cmd == null) throw new IllegalArgumentException("请填写收款单");
        Long storeId = requireStore(cmd.storeId);
        BigDecimal amount = requireAmount(cmd.amount, "收款金额", true);

        // 加锁必须是本事务的**第一条数据库语句**。
        // InnoDB 的一致性读快照在第一次普通查询时建立，加锁读不建立；
        // 若把引用校验那几条普通查询放在加锁之前，后到的事务会带着过期快照进来，
        // 读不到先到者刚提交的幂等登记，同一个 key 就会生效两次（实测复现过）。
        Map<String, Object> receivable = null;
        if (cmd.receivableId != null) {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT receivable_id, store_id, total_amount, received_amount, pending_amount, status "
                            + "FROM finance_receivable WHERE receivable_id=? FOR UPDATE", cmd.receivableId);
            if (rows.isEmpty()) {
                throw new IllegalArgumentException("应收单不存在：" + cmd.receivableId);
            }
            receivable = rows.get(0);
            if (!storeId.equals(((Number) receivable.get("store_id")).longValue())) {
                // 错误脱敏：不回显对方门店与金额
                throw new ReceivableAccessDeniedException("该应收单不属于当前门店，无权登记收款");
            }
        }

        // 锁已经拿到（或本次本就没有可锁的父行）。
        // 下面先算指纹、先判重放，**引用校验留到确认是新登记之后再做**——理由见 replay 分支处。
        String fingerprint = fingerprint(
                OP_PAYMENT, storeId, blankToNull(cmd.paymentNo), cmd.receivableId, cmd.customerId,
                blankToNull(cmd.customerName), blankToNull(cmd.bookingId), blankToNull(cmd.bookingNo),
                amount.toPlainString(), cmd.paymentDate, blankToNull(cmd.paymentMethod), cmd.accountId,
                blankToNull(cmd.category), blankToNull(cmd.remark));

        Optional<ReceivablePaymentRequest> prior = requestRepository.findByRequestId(key);
        if (prior.isPresent()) {
            // **重放判定必须早于引用校验。** 一笔当初成功的收款，其引用的账户/客户/预订单
            // 事后完全可能被停用或改动；若先校验引用，用原 requestId 做恢复就会被挡死，
            // 而那笔钱其实早就记过账了——调用方拿不回回执，只会重发或人工重记，风险远大于放行。
            // 指纹一致即证明业务参数与当初逐字节相同，返回原回执是纯读，不产生任何新写入。
            // 注意：这条只对**已存在的登记**成立；新登记仍须通过下面的全部校验。
            return replay(prior.get(), key, fingerprint, OP_PAYMENT);
        }

        // ===== 以下只有新登记会走到 =====
        validateCustomer(cmd.customerId, storeId);
        validateBooking(cmd.bookingId, storeId);
        validateAccount(cmd.accountId, storeId);

        if (cmd.receivableId == null) {
            // 无来源手工收款：历史上确实存在（961 条里 960 条无引用），因此不强制必须有应收或预订单。
            // 但**旧数据无来源不是新数据可以无来源的依据**：新记录必须写明类别与业务说明，
            // 并带可追踪的操作人，否则事后无法判断这笔钱因何而收。
            if (blankToNull(cmd.category) == null) {
                throw new IllegalArgumentException(
                        "无应收来源的手工收款必须填写收款类别 category，用于说明这笔钱因何而收");
            }
            if (blankToNull(cmd.remark) == null) {
                throw new IllegalArgumentException(
                        "无应收来源的手工收款必须填写业务说明 remark，仅有类别不足以追溯");
            }
            if (cmd.category.trim().length() > 32) {
                throw new IllegalArgumentException("收款类别不能超过 32 字");
            }
        }

        String operator = requireOperator();
        LocalDate paymentDate = cmd.paymentDate != null ? cmd.paymentDate : today();

        BigDecimal newReceived = null, newPending = null;
        String newStatus = null;
        if (receivable != null) {
            BigDecimal total = scale2((BigDecimal) receivable.get("total_amount"));
            BigDecimal received = scale2(nvl((BigDecimal) receivable.get("received_amount")));
            newReceived = scale2(received.add(amount));
            if (newReceived.compareTo(total) > 0) {
                // 文档未规定超额支付如何处理。当前明确规则：拒绝，不生成负余额，也不扩预收功能。
                throw new IllegalArgumentException(
                        "收款金额超过该应收单的待收金额，本次未登记。"
                                + "待收 " + scale2(total.subtract(received)).toPlainString()
                                + "，本次 " + amount.toPlainString());
            }
            newPending = scale2(total.subtract(newReceived));
            newStatus = newPending.signum() == 0 ? STATUS_PAID
                    : (newReceived.signum() > 0 ? STATUS_PARTIAL : STATUS_UNPAID);
        }

        KeyHolder keys = new GeneratedKeyHolder();
        final LocalDate pDate = paymentDate;
        try {
            jdbc.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO finance_payment_record (store_id, payment_no, payment_date, receivable_id, "
                                + "customer_id, customer_name, booking_id, booking_no, amount, payment_method, "
                                + "account_id, operator_name, payment_category, remark, created_at) "
                                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW())",
                        Statement.RETURN_GENERATED_KEYS);
                int i = 1;
                ps.setLong(i++, storeId);
                ps.setString(i++, blankToNull(cmd.paymentNo) == null
                        ? "PAY" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase()
                        : cmd.paymentNo.trim());
                ps.setObject(i++, pDate);
                setNullableLong(ps, i++, cmd.receivableId);
                setNullableInt(ps, i++, cmd.customerId);
                ps.setString(i++, blankToNull(cmd.customerName));
                ps.setString(i++, blankToNull(cmd.bookingId));
                ps.setString(i++, blankToNull(cmd.bookingNo));
                ps.setBigDecimal(i++, amount);
                ps.setString(i++, blankToNull(cmd.paymentMethod) == null ? "cash" : cmd.paymentMethod.trim());
                setNullableLong(ps, i++, cmd.accountId);
                ps.setString(i++, operator);
                ps.setString(i++, blankToNull(cmd.category));
                ps.setString(i, blankToNull(cmd.remark));
                return ps;
            }, keys);
        } catch (RuntimeException e) {
            if (isLockContention(e)) throw new ReceivableRetryableException();
            throw e;
        }
        Number generated = keys.getKey();
        if (generated == null) throw new IllegalStateException("收款单未返回主键");
        long paymentId = generated.longValue();
        String paymentNo = jdbc.queryForObject(
                "SELECT payment_no FROM finance_payment_record WHERE payment_id=?", String.class, paymentId);

        // 收款与应收余额在同一事务里推进：任一步失败一起回滚，不会出现"收了款但应收没动"。
        if (receivable != null) {
            int updated = jdbc.update(
                    "UPDATE finance_receivable SET received_amount=?, pending_amount=?, status=?, updated_at=NOW() "
                            + "WHERE receivable_id=? AND store_id=?",
                    newReceived, newPending, newStatus, cmd.receivableId, storeId);
            if (updated != 1) {
                throw new IllegalStateException("应收单余额更新行数异常，已回滚：" + updated);
            }
        }

        register(key, OP_PAYMENT, storeId, paymentId, paymentNo, fingerprint, operator);
        log.info("【收款登记】{} 门店 {} 收款 {}，金额 {}，来源应收 {}",
                operator, storeId, paymentId, amount, cmd.receivableId);
        Map<String, Object> snapshot = paymentSnapshot(paymentId);
        if (cmd.receivableId != null) {
            snapshot.put("receivable", receivableSnapshot(cmd.receivableId));
        }
        return new OperationResult(key, paymentId, paymentNo, false, snapshot);
    }

    // ==================== 查询 ====================

    public List<Map<String, Object>> listReceivables(Long storeId) {
        return jdbc.queryForList(
                "SELECT * FROM finance_receivable WHERE store_id=? ORDER BY due_date ASC, receivable_id DESC",
                requireStore(storeId));
    }

    /** 同店详情：应收本体 + 它名下的收款流水，供列表页展开核对。 */
    public Map<String, Object> receivableDetail(Long receivableId, Long storeId) {
        Long sid = requireStore(storeId);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM finance_receivable WHERE receivable_id=? AND store_id=?", receivableId, sid);
        if (rows.isEmpty()) {
            // 别店 id 与不存在的 id 给同一句提示，避免用探测手段确认别店单据是否存在
            throw new IllegalArgumentException("应收单不存在或不属于当前门店");
        }
        Map<String, Object> detail = new LinkedHashMap<>(rows.get(0));
        detail.put("payments", jdbc.queryForList(
                "SELECT * FROM finance_payment_record WHERE receivable_id=? AND store_id=? "
                        + "ORDER BY payment_date ASC, payment_id ASC", receivableId, sid));
        return detail;
    }

    public List<Map<String, Object>> listPayments(Long storeId) {
        return jdbc.queryForList(
                "SELECT * FROM finance_payment_record WHERE store_id=? "
                        + "ORDER BY payment_date DESC, payment_id DESC LIMIT 200",
                requireStore(storeId));
    }

    // ==================== 内部 ====================

    private OperationResult replay(ReceivablePaymentRequest record, String key,
                                   String fingerprint, String expectedOp) {
        if (!record.getOpType().equals(expectedOp) || !record.getParamsHash().equals(fingerprint)) {
            throw new ReceivableConflictException(CONFLICT_MESSAGE);
        }
        Map<String, Object> snapshot = OP_RECEIVABLE.equals(expectedOp)
                ? receivableSnapshot(record.getTargetId())
                : paymentSnapshot(record.getTargetId());
        return new OperationResult(key, record.getTargetId(), record.getTargetNo(), true, snapshot);
    }

    private void register(String key, String opType, Long storeId, Long targetId,
                          String targetNo, String fingerprint, String operator) {
        ReceivablePaymentRequest record = new ReceivablePaymentRequest();
        record.setRequestId(key);
        record.setOpType(opType);
        record.setStoreId(storeId);
        record.setTargetId(targetId);
        record.setTargetNo(targetNo);
        record.setParamsHash(fingerprint);
        record.setOperatorName(operator);
        record.setCreatedAt(LocalDateTime.now(clock));
        try {
            // 必须 flush：创建应收没有可锁的父行，只有主键冲突能挡住并发的第二条。
            requestRepository.saveAndFlush(record);
        } catch (RuntimeException e) {
            if (isConstraintViolation(e) || isLockContention(e)) throw new ReceivableRetryableException();
            throw e;
        }
    }

    private Map<String, Object> receivableSnapshot(Long id) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT receivable_id, store_id, receivable_no, total_amount, received_amount, "
                        + "pending_amount, status FROM finance_receivable WHERE receivable_id=?", id);
        return rows.isEmpty() ? new LinkedHashMap<>() : new LinkedHashMap<>(rows.get(0));
    }

    private Map<String, Object> paymentSnapshot(Long id) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT payment_id, store_id, payment_no, receivable_id, amount, payment_method, "
                        + "payment_category, payment_date FROM finance_payment_record WHERE payment_id=?", id);
        return rows.isEmpty() ? new LinkedHashMap<>() : new LinkedHashMap<>(rows.get(0));
    }

    private String requireRequestId(String requestId, String what) {
        String key = requestId == null ? null : requestId.trim();
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException(
                    "缺少 requestId：" + what + "必须带幂等键，且重试时保持同一个值。"
                            + "服务端不会代为生成——那样每次都是新键，重试会重复记账");
        }
        if (key.length() > 64) throw new IllegalArgumentException("requestId 不能超过 64 字符");
        return key;
    }

    /** 门店 fail-closed：无身份拒绝；非总经理不得指定别店；总经理必须显式指定。 */
    private Long requireStore(Long requested) {
        if (UserContext.get() == null) {
            throw new ReceivableAccessDeniedException("未登录或身份无效，无法访问应收与收款数据");
        }
        if (requested != null && requested <= 0L) {
            throw new IllegalArgumentException("请选择有效门店");
        }
        if (UserContext.isGeneralManager()) {
            if (requested == null) {
                throw new IllegalArgumentException("总经理操作应收与收款必须显式指定门店");
            }
            return requested;
        }
        Long own = UserContext.currentStoreId();
        if (own == null || own <= 0L) {
            throw new ReceivableAccessDeniedException("当前身份没有解析出门店，无法确定可操作范围");
        }
        if (requested != null && !own.equals(requested)) {
            throw new ReceivableAccessDeniedException("无权操作其他门店的应收与收款数据");
        }
        return own;
    }

    private String requireOperator() {
        String operator = UserContext.getUsername();
        if (operator == null || operator.isBlank()) {
            // 基线取不到就写死一个默认名，等于把账记在一个不存在的人头上，必须拒绝。
            throw new ReceivableAccessDeniedException("无法确定操作人身份，拒绝记账");
        }
        return operator;
    }

    private BigDecimal requireAmount(BigDecimal amount, String label, boolean strictlyPositive) {
        if (amount == null) throw new IllegalArgumentException(label + "必填");
        if (strictlyPositive ? amount.signum() <= 0 : amount.signum() < 0) {
            throw new IllegalArgumentException(label + (strictlyPositive ? "必须大于 0" : "不能为负"));
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(label + "最多两位小数：" + amount.toPlainString());
        }
        return scale2(amount);
    }

    private void validateCustomer(Integer customerId, Long storeId) {
        if (customerId == null) return;   // 合法可空，不构造假客户
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM customer_master WHERE customer_id=? AND store_id=?",
                Integer.class, customerId, storeId);
        if (n == null || n == 0) {
            throw new IllegalArgumentException("客户不存在或不属于当前门店");
        }
    }

    /** booking_id 是业务字符串（形如 BK...），不是数字 id，不做强转。 */
    private void validateBooking(String bookingId, Long storeId) {
        String id = blankToNull(bookingId);
        if (id == null) return;
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM booking_master WHERE booking_id=? AND store_id=?",
                Integer.class, id, storeId);
        if (n == null || n == 0) {
            throw new IllegalArgumentException("预订单不存在或不属于当前门店");
        }
    }

    /**
     * 收款账户校验：**新登记只能用本店且已启用的账户。**
     * <p>
     * 三种不合格情况——不存在、属于别店、已停用——一律给**同一句**提示。
     * 分开提示等于给出一个探测接口：换个 id 试，从"不属于本店"和"不存在"的差异就能
     * 反推出别店有哪些账户。账务接口不值得为这点便利泄漏他店信息。
     * <p>
     * {@code is_active} 为 NULL 时**判为不启用**（SQL 里 {@code =1} 天然不匹配 NULL）。
     * 迁移中该列定义为 {@code DEFAULT 1}，正常写入不会是 NULL；若生产存在历史 NULL 行，
     * 这些账户将无法用于新收款——这是本任务口径下的必然结果，已在报告中列为边界。
     * 已存在登记的重放不走本方法，故不受影响。
     */
    private void validateAccount(Long accountId, Long storeId) {
        if (accountId == null) return;
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM finance_account WHERE account_id=? AND store_id=? AND is_active=1",
                Integer.class, accountId, storeId);
        if (n == null || n == 0) {
            throw new IllegalArgumentException("收款账户不存在、不属于当前门店或已停用");
        }
    }

    /**
     * 业务参数指纹。
     * <p>
     * <b>编码必须无歧义</b>：早先是用 {@code |} 直接拼接，字段值里本来就可能含竖线，
     * 于是 {@code category="A|B", remark="C"} 与 {@code category="A", remark="B|C"}
     * 会拼出同一个串、算出同一个指纹——两个业务含义完全不同的请求被当成同一次重试。
     * <p>
     * 现在改为带类型标记与长度前缀的编码：每段写成 {@code 类型:字节长度:内容;}，
     * null 单独记为 {@code N;}。长度前缀让分隔符失去歧义作用，内容里再多竖线也不影响切分；
     * 类型标记让字符串 "1" 与数字 1 不会互相冒充。
     */
    private static String fingerprint(Object... parts) {
        StringBuilder sb = new StringBuilder();
        for (Object p : parts) {
            if (p == null) {
                sb.append("N;");
                continue;
            }
            char tag;
            if (p instanceof Number) tag = 'N';
            else if (p instanceof java.time.temporal.Temporal) tag = 'D';
            else if (p instanceof Boolean) tag = 'B';
            else tag = 'S';
            String value = p.toString();
            sb.append(tag).append(':')
              .append(value.getBytes(StandardCharsets.UTF_8).length).append(':')
              .append(value).append(';');
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : digest) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    private static void setNullableInt(PreparedStatement ps, int index, Integer value) throws java.sql.SQLException {
        if (value == null) ps.setNull(index, Types.INTEGER); else ps.setInt(index, value);
    }

    private static void setNullableLong(PreparedStatement ps, int index, Long value) throws java.sql.SQLException {
        if (value == null) ps.setNull(index, Types.BIGINT); else ps.setLong(index, value);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal scale2(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v.setScale(2, RoundingMode.HALF_UP);
    }

    private static boolean isConstraintViolation(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof org.springframework.dao.DataIntegrityViolationException) return true;
            if (t instanceof java.sql.SQLIntegrityConstraintViolationException) return true;
            if (t.getClass().getName().equals("org.hibernate.exception.ConstraintViolationException")) return true;
            String msg = t.getMessage();
            if (msg != null && msg.contains("Duplicate entry")) return true;
        }
        return false;
    }

    private static boolean isLockContention(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof org.springframework.dao.PessimisticLockingFailureException) return true;
            if (t instanceof java.sql.SQLTransactionRollbackException) return true;
            String name = t.getClass().getName();
            if (name.equals("org.hibernate.exception.LockAcquisitionException")) return true;
            String msg = t.getMessage();
            if (msg != null && (msg.contains("Deadlock found") || msg.contains("Lock wait timeout"))) return true;
        }
        return false;
    }
}
