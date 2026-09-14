# CL-PROD-AUTH-PRESERVE-67 裁决报告

只读对照，未改业务代码，未跑测试。生产快照取自 `artifacts/tr62-prod-src.tar.gz` 指定 6 文件；
候选固定 `6cf5e6d15fc908a159d4aa29b64d1e3bdb82a652`（`git show` 提取，同目录留存两侧原文）。

## 决策表

| 文件 | 决策 | 依据（文件行） | 三账号/门店影响 |
| --- | --- | --- | --- |
| **AuthController.java** | **需窄合并** | 候选第 66 行只判 `username==null\|\|password==null`；生产 `LoginCredential.isUsablePassword()`（common/LoginCredential.java:44-46）额外拒绝空串/纯空白密码。候选第 98 行明文分支 `staffPassword.equals(password)`——若库中某账号密码字段是空字符串（非NULL），提交空密码即可登录，**复现生产已修过的漏洞**（生产注释原话："原实现只判password==null...若库里那条记录密码也是空串，不输密码即可登录成功"）。诱饵哈希防时序侧信道（第52行起）已带入，等价。 | 三账号密码字段目前应非空，**当前登录不受影响**；但该口子对**任何**空密码字段的员工账号敞开，含新建/重置密码流程可能产生的空值记录。 |
| **IpadAuthController.java** | **需窄合并** | 同一漏洞，候选第37行`phone==null\|\|password==null`、第57行`staffPassword.equals(password)`，逻辑与AuthController一致地缺失空串校验。生产`LoginCredential`本就是给PC/iPad两端共用的。 | iPad 收银端同样敞口，影响面等同上条。 |
| **JwtAuthInterceptor.java** | **候选已覆盖，且更强** | 生产版直接信 JWT claims（prod JwtAuthInterceptor.java:81/103-105）；候选版第108-114行接入 `StaffRealtimeGuard` 做实时在职/角色/门店复核，claims 只用于取 staffId 查库，role/storeId 一律以库内当前值为准（第122-123行注释）——这是已验收的 CL-AUTH-REALTIME-14 改动，不是本卡新增风险，仅确认未丢失。 | 三账号（秋哥/张晓秋、张总/张婧、张律师）登录后角色/门店随时按库内当前值生效，比生产更严格，不会更松。 |
| **RoleScopeInterceptor.java（生产独立类）** | **候选已覆盖，功能等价，损失一项可配置性** | 候选**没有同名类**，逻辑并入 JwtAuthInterceptor 的 `OUTSIDER_SCOPES` 静态 Map（第36-39行）：`lawyer→[/api/legal,/api/auth/me,/api/auth/logout]`，与生产 `@Value` 注入的默认值完全一致；匹配语义相同（`withinScope`，第46-53行，精确匹配或`前缀+"/"`，与生产 `isAllowed` 的 `path.startsWith(prefix+"/")` 等价，**均不存在前缀误放行问题**）。**差异**：生产可通过 `security.external-roles`/`security.external-role-allowed-prefixes` 配置项热改角色与前缀，候选硬编码在代码里，改动需要重新编译发布。错误响应文案也不同（候选："该账号无权访问此功能"；生产："该账号仅被授权查阅法务案卷..."），均为403，不影响拦截效果。 | 张律师当前范围不变，**行为等价**；今后若要新增/调整外部角色只能走代码改动，不能只改配置——留作后续增强点，不阻塞整合。 |
| **LoginCredential.java（生产共用类）** | **需窄合并（即上述空密码问题的根因）** | 候选无对应类，规则被拆散内联进两个 Controller，且内联时**丢了空密码校验与用户名格式校验**（`isValidUsername`/`USERNAME_PATTERN`，common/LoginCredential.java:26-30，候选未见任何用户名格式限制）。BCrypt分支与诱饵哈希均已带入，只是明文分支的空值防护和格式校验在搬迁中被漏掉。 | 用户名格式无限制目前不构成已知风险（生产数据本就规范），空密码是唯一需要堵的口子。 |
| **WebMvcConfig.java** | **候选已覆盖，一致** | 拦截器注册顺序 RateLimit(-1)→Jwt(0)→Ipad(1)一致；候选未注册 `roleScopeInterceptor` 属预期（逻辑已并入Jwt拦截器，无需单独注册）；候选新增 `ApprovalAuthorityInterceptor`(order=2) 是另一张已验收卡（工资/审批白名单）的产物，与本卡6项对照无关，且已显式排除 `/api/legal/**`，未触碰法务边界。 | 无变化。 |

## 法务冻结边界

律师（张律师）权限范围检查（OUTSIDER_SCOPES）与法务接口本身**未被本次对照涉及的任何差异触碰**；
`ApprovalAuthorityInterceptor` 已显式排除 `/api/legal/**`。本报告不建议改动法务相关配置或代码，
边界维持"任何人不得动"。

## 最小整合顺序与验收闸门

1. 把 `LoginCredential.isUsablePassword()` 的空密码/空白密码拒绝逻辑，补进候选
   `AuthController.login()` 与 `IpadAuthController` 对应方法的明文比对分支前（各一行守卫），
   这是唯一阻断级缺口，必须先做。
2. 用户名格式校验（`USERNAME_PATTERN`）可选择性补入，非阻断，建议一并带上避免二次改动。
3. 验收闸门（不在本卡范围内执行，留给下一步）：三账号真实登录不受影响；构造一条库内密码字段为
   空字符串的合成测试账号，验证提交空密码返回 401 而非登录成功；律师账号越权访问非法务接口仍 403。
4. RoleScopeInterceptor 的配置化能力是否要补，不阻断整合，交由后续任务决定。

## 边界

未修改任何业务代码，未跑任何测试，未展开生产快照全包，只读提取了指定 6 文件（两个未在候选按原名
找到的文件已找到等价实现并证明，未凭空推断）。未碰法务文件与配置。仅用 `git show`/`tar -x` 只读操作。
