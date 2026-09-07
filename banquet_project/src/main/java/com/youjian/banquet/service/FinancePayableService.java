package com.youjian.banquet.service;

import com.youjian.banquet.entity.FinancePayable;
import com.youjian.banquet.entity.PayableCreateRequest;
import com.youjian.banquet.entity.PayableSettlementRecord;
import com.youjian.banquet.repository.FinancePayableRepository;
import com.youjian.banquet.repository.PayableCreateRequestRepository;
import com.youjian.banquet.repository.PayableSettlementRecordRepository;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 供应商应付服务。对应 GET/POST /api/finance/payables。
 * POST 用于结算记账，更新 paid_amount / pending_amount / status。
 */
@Service
public class FinancePayableService {

    public static class PayableAccessDeniedException extends IllegalArgumentException {
        public PayableAccessDeniedException() { super("无权限操作该门店应付单"); }
    }

    /** 同一个 requestId 被拿去结算不同的单或不同的金额——这是调用方的错，必须拒绝而不是照做。 */
    public static class SettlementConflictException extends IllegalArgumentException {
        public SettlementConflictException(String message) { super(message); }
    }

    /**
     * 并发撞锁，本次没有记账。可以用同一个 requestId 安全重试——幂等键就是为这个准备的。
     */
    public static class SettlementRetryableException extends IllegalArgumentException {
        public SettlementRetryableException() {
            super("结算遇到并发冲突，本次未记账。请用同一个 requestId 重试；"
                    + "若重试后提示该 requestId 已被占用，说明它已被另一笔结算使用，请换新的 requestId");
        }
    }

    /** 是不是某个具体约束被违反。按约束名认，避免把所有完整性错误混为一谈。 */
    private static boolean isViolationOf(Throwable e, String constraintName) {
        if (!isConstraintViolation(e)) return false;
        for (Throwable t = e; t != null; t = t.getCause()) {
            String msg = t.getMessage();
            if (msg != null && msg.contains(constraintName)) return true;
        }
        return false;
    }

    /** 底层是不是唯一键冲突。同样顺着 cause 链认，不依赖 Spring 的异常翻译是否装配。 */
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

    /** 底层是不是锁竞争（死锁、等锁超时）。不同容器下异常包装不同，只能顺着 cause 链认。 */
    private static boolean isLockContention(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof org.springframework.dao.PessimisticLockingFailureException) return true;
            if (t instanceof java.sql.SQLTransactionRollbackException) return true;
            String name = t.getClass().getName();
            if (name.equals("org.hibernate.exception.LockAcquisitionException")
                    || name.equals("org.hibernate.dialect.lock.LockingStrategyException")) return true;
            String msg = t.getMessage();
            if (msg != null && (msg.contains("Deadlock found") || msg.contains("Lock wait timeout"))) return true;
        }
        return false;
    }

    /**
     * 冲突文案。刻意写成与具体单据无关的一句话：
     * requestId 全局唯一、跨门店也可能撞上，回显对方的单号或金额等于给了一条探测别店数据的路。
     */
    static final String CONFLICT_MESSAGE =
            "该 requestId 已被另一笔结算占用，且与本次提交的参数不一致，无法按重试处理。"
                    + "重试请沿用原来的应付单和金额；这是一笔新的结算请改用新的 requestId";

    /**
     * 手工创建结果。{@code replayed=true} 表示这次是重试，返回的是原来那张单，没有新建。
     * 回执里的 requestId / payableId / payableNo 供前端逐项核对。
     */
    public static class CreateResult {
        public final FinancePayable payable;
        public final String requestId;
        public final boolean replayed;

        public CreateResult(FinancePayable payable, String requestId, boolean replayed) {
            this.payable = payable;
            this.requestId = requestId;
            this.replayed = replayed;
        }
    }

    /**
     * 同一个 requestId 的创建正在进行、或刚刚完成但本次事务读不到。
     * 本次没有建单，用同一个 requestId 重试即可取回原回执。
     */
    public static class CreateInFlightException extends IllegalArgumentException {
        public CreateInFlightException() {
            super("该 requestId 的创建请求正在处理或刚刚完成，本次未新建单据。"
                    + "请用同一个 requestId 重试以取回原回执");
        }
    }

    /** 创建冲突文案：同样不回显另一张单的任何内容。 */
    static final String CREATE_CONFLICT_MESSAGE =
            "该 requestId 此前已创建过一张应付单，且与本次提交的业务参数不一致，无法按重试处理。"
                    + "重试请沿用原来的参数；这是一张新单请改用新的 requestId";

    /** 结算结果。{@code replayed=true} 表示这次是重试，返回的是原来那一笔，没有产生新结算。 */
    public static class SettlementResult {
        public final FinancePayable payable;
        public final PayableSettlementRecord record;
        public final boolean replayed;

        public SettlementResult(FinancePayable payable, PayableSettlementRecord record, boolean replayed) {
            this.payable = payable;
            this.record = record;
            this.replayed = replayed;
        }
    }

    @Autowired
    private FinancePayableRepository financePayableRepository;

    @Autowired
    private PayableSettlementRecordRepository settlementRepository;

    @Autowired
    private PayableCreateRequestRepository createRequestRepository;

    /**
     * 日历基准。生产用系统时钟；测试注入固定时钟来推进"今天"，
     * 从而在不改系统时钟的前提下验证跨日期行为。
     */
    private java.time.Clock clock = java.time.Clock.systemDefaultZone();

    /** 仅供测试推进日历。 */
    void setClock(java.time.Clock clock) {
        this.clock = clock == null ? java.time.Clock.systemDefaultZone() : clock;
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    public List<FinancePayable> list(Long storeId, Long supplierId, String status) {
        boolean hasSupplier = supplierId != null;
        boolean hasStatus = status != null && !status.isEmpty();
        if (hasSupplier) {
            List<FinancePayable> list = financePayableRepository
                    .findByStoreIdAndSupplierIdOrderByPayableIdDesc(storeId, supplierId);
            if (hasStatus) {
                list.removeIf(p -> !status.equals(p.getStatus()));
            }
            return list;
        }
        if (hasStatus) {
            return financePayableRepository.findByStoreIdAndStatusOrderByPayableIdDesc(storeId, status);
        }
        return financePayableRepository.findByStoreIdOrderByPayableIdDesc(storeId);
    }

    /**
     * 结算记账：锁定单据后同步已付、待付及状态，并写一条独立结算流水。<b>不执行银行支付。</b>
     * <p>
     * 幂等由调用方给的 {@code requestId} 保证：同一个 requestId 无论提交多少次，
     * 都只产生一笔结算，重试返回原来那一笔。这解决基线的两个真实问题——
     * 前端超时重试或用户手抖点两次会把同一笔结算记两遍；以及事后只看得到"已付多少"，
     * 看不出结算了几次、谁结的。
     * <p>
     * <b>requestId 必须由调用方提供，服务端绝不代为生成</b>：服务端自己生成的话每次都不一样，
     * 幂等就成了摆设，重试照样重复扣款。
     *
     * @param payableId    应付单ID
     * @param settleAmount 本次结算金额
     * @param requestId    调用方生成的幂等键，重试时必须保持不变
     */
    @Transactional
    public SettlementResult settle(Long payableId, BigDecimal settleAmount, String requestId) {
        String key = requestId == null ? null : requestId.trim();
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException(
                    "缺少 requestId：结算必须带幂等键，且重试时保持同一个值。"
                            + "服务端不会代为生成——那样每次都是新键，重试会重复记账");
        }
        if (key.length() > 64) {
            throw new IllegalArgumentException("requestId 不能超过 64 字符");
        }
        if (settleAmount == null || settleAmount.signum() <= 0 || settleAmount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("结算金额必须为正数，最多保留两位小数");
        }

        // 加锁顺序：**先应付单，再查幂等键**，而且幂等键那一步刻意不加锁。
        //
        // 这个顺序是两次实测换来的：
        //   先查键再锁单 —— 同键并发时两个事务都拿到不存在行的间隙锁（间隙锁互相兼容），
        //     然后各自插入，触发 insert-intention 死锁；
        //   先锁单再用加锁读查键 —— 跨单同键时锁的是不同行，照样在键上交叉等待，同样死锁。
        // 现在：同单并发在应付单行锁上排队；排到之后的普通读才建立一致性快照，
        // 能看到前一个事务刚提交的流水（InnoDB 的 read view 在第一次一致性读时才创建，
        // 加锁读不创建），所以不会读到过期快照而重复记账。
        // 跨单同键由 request_id 唯一键兜底，约束冲突翻译成可解释的冲突提示。
        FinancePayable existing;
        Optional<PayableSettlementRecord> prior;
        try {
            existing = financePayableRepository.findForSettlement(payableId)
                    .orElseThrow(() -> new IllegalArgumentException("应付单不存在: " + payableId));
            prior = settlementRepository.findForIdempotency(key);
        } catch (RuntimeException e) {
            if (isLockContention(e)) throw new SettlementRetryableException();
            throw e;
        }
        try { UserContext.assertStoreAccess(existing.getStoreId()); }
        catch (PayableAccessDeniedException e) { throw e; }
        catch (IllegalArgumentException e) { throw new PayableAccessDeniedException(); }

        if (prior.isPresent()) {
            PayableSettlementRecord record = prior.get();
            boolean sameBill = record.getPayableId().equals(payableId);
            boolean sameAmount = record.getSettleAmount().compareTo(settleAmount) == 0;
            if (!sameBill || !sameAmount) {
                // 冲突提示刻意不带另一张单的单号、金额或原 requestId。
                // 幂等键是全局唯一的，跨门店也可能撞上；一旦回显，调用方就能拿一个键去探测
                // 别的门店有哪些应付单、金额多少。说清"怎么办"就够了，不需要说"对方是谁"。
                throw new SettlementConflictException(CONFLICT_MESSAGE);
            }
            // 参数一致的重试：返回原来那一笔，不产生新结算。
            return new SettlementResult(existing, record, true);
        }

        BigDecimal total = existing.getTotalAmount();
        BigDecimal paid = existing.getPaidAmount() == null ? BigDecimal.ZERO : existing.getPaidAmount();
        if (total == null || total.signum() < 0 || paid.signum() < 0 || paid.compareTo(total) > 0) {
            throw new IllegalArgumentException("应付单金额不一致，请先核对原单");
        }
        BigDecimal newPaid = paid.add(settleAmount);
        if (newPaid.compareTo(total) > 0) {
            throw new IllegalArgumentException("结算金额不能超过待付金额");
        }
        BigDecimal unpaid = total.subtract(newPaid);
        existing.setPaidAmount(newPaid);
        existing.setPendingAmount(unpaid);
        if (unpaid.signum() <= 0) {
            existing.setStatus("paid");
        } else if (newPaid.signum() > 0) {
            existing.setStatus("partial");
        }

        // 流水与原单在同一个事务里写：任一步失败两边一起回滚，不会出现
        // "钱记上了但没有流水"或"有流水但原单没动"这种对不上的账。
        PayableSettlementRecord record = new PayableSettlementRecord();
        record.setRequestId(key);
        record.setSettlementNo(nextSettlementNo());
        record.setStoreId(existing.getStoreId());
        record.setPayableId(existing.getPayableId());
        record.setPayableNo(existing.getPayableNo());
        record.setSettleAmount(settleAmount);
        record.setPaidAfter(newPaid);
        record.setPendingAfter(unpaid);
        record.setStatusAfter(existing.getStatus());
        record.setOperatorName(UserContext.getUsername());
        record.setCreatedAt(LocalDateTime.now());
        record.setNote("仅为账务流水，不代表银行实际付款");
        PayableSettlementRecord saved;
        try {
            // 必须 flush：两个线程结算**不同**的应付单却用了同一个 requestId 时，
            // 它们锁的是不同的行，谁也拦不住谁，只有 request_id 唯一键能挡。
            // 不 flush 的话约束冲突要等事务提交才爆，调用方拿到的是一个说不清的 500。
            saved = settlementRepository.saveAndFlush(record);
        } catch (RuntimeException e) {
            // 唯一键撞上：这一笔没有记账，事务随即回滚。给调用方一条能照着做的冲突提示，
            // 而不是把底层约束异常原样抛出去。
            //
            // 不能只 catch Spring 的 DataIntegrityViolationException：那层翻译由
            // PersistenceExceptionTranslator 提供，容器装配不同就可能拿到 Hibernate 原生异常
            // （实测在精简的测试上下文里就是原生的），只认一种类型会漏掉。
            // 按具体是哪个约束分类，不能一律说成"requestId 冲突"：
            // 那样外键失败或结算号撞号也会让调用方去换 key 重记，等于把一笔本该查清的问题
            // 引导成重复记账。
            if (isViolationOf(e, "uk_payable_settlement_request")) {
                throw new SettlementConflictException(CONFLICT_MESSAGE);
            }
            if (isViolationOf(e, "uk_payable_settlement_no")) {
                // 结算号随机撞号：本次未记账，用同一 requestId 重试会重新生成号，安全。
                throw new SettlementRetryableException();
            }
            if (isViolationOf(e, "fk_payable_settlement_payable")) {
                // 流水挂不上应付单：单据不存在或门店不匹配。属数据问题，不是幂等冲突。
                throw new IllegalArgumentException(
                        "结算流水无法关联该应付单：单据不存在或与当前门店不匹配，本次未记账");
            }
            if (isConstraintViolation(e)) {
                // 其他完整性错误：整笔已回滚，给不泄密的可解释错误，不引导换 key。
                throw new IllegalArgumentException(
                        "结算写入被数据库约束拒绝，本次未记账，请联系管理员核对单据数据");
            }
            if (isLockContention(e)) throw new SettlementRetryableException();
            throw e;
        }

        FinancePayable updated = financePayableRepository.save(existing);
        return new SettlementResult(updated, saved, false);
    }

    /**
     * 旧的两参数结算入口，<b>已废弃</b>。
     * <p>
     * 保留它不是为了兼容，而是为了让残留的调用点得到一条说得清的错误，而不是编译期一片红。
     * 加幂等键之后没有 requestId 就没法保证重试不重复记账，
     * 这里绝不代为生成一个——那等于把幂等做成摆设。
     *
     * @deprecated 改用 {@link #settle(Long, BigDecimal, String)}，由调用方提供稳定的 requestId
     */
    @Deprecated
    public SettlementResult settle(Long payableId, BigDecimal settleAmount) {
        return settle(payableId, settleAmount, null);
    }

    /** 某张应付单的结算流水，按发生顺序。对账用。 */
    public List<PayableSettlementRecord> settlements(Long payableId) {
        FinancePayable existing = financePayableRepository.findById(payableId)
                .orElseThrow(() -> new IllegalArgumentException("应付单不存在: " + payableId));
        try { UserContext.assertStoreAccess(existing.getStoreId()); }
        catch (IllegalArgumentException e) { throw new PayableAccessDeniedException(); }
        return settlementRepository.findByPayableIdOrderBySettlementIdAsc(payableId);
    }

    /** 结算号：ST + 日期 + 随机后缀，唯一键兜底。 */
    private String nextSettlementNo() {
        return "ST" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    /**
     * 手工新增应付单，带请求幂等。
     * <p>
     * 基线没有幂等：前端超时重发、用户点两次就多一张一模一样的单；
     * 而且第一次的响应丢了之后，调用方拿不回结果，只能去列表里翻，翻不到就再点一次。
     * <p>
     * 现在同一个 requestId：参数一致就把原来那张单的回执还回去（{@code replayed=true}），
     * 参数变了就明确拒绝——不会照着新参数又建一张。
     * <p>
     * <b>requestId 必须由调用方提供，服务端绝不代为生成</b>，理由与结算一致：
     * 服务端生成的键每次都不一样，重试照样重复建单。
     */
    @Transactional
    public CreateResult create(FinancePayable payable, String requestId) {
        String key = requestId == null ? null : requestId.trim();
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException(
                    "缺少 requestId：手工新增应付必须带幂等键，且重试时保持同一个值。"
                            + "服务端不会代为生成——那样每次都是新键，重试会重复建单");
        }
        if (key.length() > 64) throw new IllegalArgumentException("requestId 不能超过 64 字符");

        if (payable == null) throw new IllegalArgumentException("请填写应付单");

        // 顺序是刻意的：先认人，再比对原始参数，最后才做带日历默认值的完整校验。
        //
        // 身份/门店必须在重放之前校验 —— 否则别店的人拿到一个 key 就能读出原单回执。
        if (payable.getStoreId() == null || payable.getStoreId() <= 0) {
            throw new IllegalArgumentException("请选择有效门店");
        }
        try { UserContext.assertStoreAccess(payable.getStoreId()); }
        catch (PayableAccessDeniedException e) { throw e; }
        catch (IllegalArgumentException e) { throw new PayableAccessDeniedException(); }

        // 指纹一律用**调用方提交的原始值**，不能用服务端补默认之后的值：
        //   日期 —— validateForCreate 会把没传的 payableDate 补成"今天"，
        //           拿补过的值算指纹，不传日期的请求隔天重试指纹就变了，本该重放的恢复被判成 409；
        //   单号 —— 没传时服务端每次生成一个新的 PY...，进了指纹就永远对不上，次次 409。
        // 显式改日期或显式改单号仍然会改变指纹，冲突照报，不是靠"少比几个字段"来掩盖。
        java.time.LocalDate rawPayableDate = payable.getPayableDate();
        java.time.LocalDate rawDueDate = payable.getDueDate();
        String rawPayableNo = payable.getPayableNo() == null || payable.getPayableNo().isBlank()
                ? null : payable.getPayableNo().trim();
        String fingerprint = fingerprintOf(payable, rawPayableDate, rawDueDate, rawPayableNo);

        Optional<PayableCreateRequest> prior = createRequestRepository.findByRequestId(key);
        if (prior.isPresent()) {
            PayableCreateRequest record = prior.get();
            if (!record.getParamsHash().equals(fingerprint)) {
                // 同键换参数：不照做，也不回显原来那张单的任何内容。
                throw new SettlementConflictException(CREATE_CONFLICT_MESSAGE);
            }
            // 走到这里说明身份没问题、参数与首次完全一致。
            // **不再跑 validateForCreate**：那里会把 payableDate 补成"今天"再校验到期日，
            // 于是「首次没传应付日、显式到期日」的请求在到期日过后来恢复，会被日历默认值挡成 400。
            // 首次创建时那套校验照旧执行，非法日期依然拒绝 —— 这里只是不拿新日历去否决一次旧的合法请求。
            FinancePayable existing = financePayableRepository.findById(record.getPayableId())
                    .orElseThrow(() -> new IllegalStateException(
                            "创建登记指向的应付单不存在，数据不一致，请联系管理员核对"));
            return new CreateResult(existing, key, true);
        }

        FinancePayable prepared = validateForCreate(payable);
        FinancePayable saved = financePayableRepository.save(prepared);

        PayableCreateRequest record = new PayableCreateRequest();
        record.setRequestId(key);
        record.setStoreId(saved.getStoreId());
        record.setPayableId(saved.getPayableId());
        record.setPayableNo(saved.getPayableNo());
        record.setParamsHash(fingerprint);
        record.setOperatorName(UserContext.getUsername());
        record.setCreatedAt(LocalDateTime.now());
        try {
            // 必须 flush：并发同键时没有可锁的父行（单据还不存在），
            // 只有主键冲突能挡住第二张。不 flush 的话冲突要等提交才爆，
            // 调用方拿到的是一个说不清的底层异常。
            createRequestRepository.saveAndFlush(record);
        } catch (RuntimeException e) {
            if (isViolationOf(e, "PRIMARY") || isConstraintViolation(e)) {
                // 登记没写进去，整笔（含刚建的单）随事务一起回滚，不会留下无主的应付单。
                throw new CreateInFlightException();
            }
            if (isLockContention(e)) throw new CreateInFlightException();
            throw e;
        }
        return new CreateResult(saved, key, false);
    }

    /**
     * 业务参数指纹：门店、供应商、金额、日期、备注任何一项变了都算不同请求。
     * <p>
     * 日期用调用方**原始提交的值**（没传就是 null），而不是服务端补默认之后的值：
     * 否则「没传日期」这种最常见的请求，其指纹会随服务器当天日期漂移，
     * 跨天重试必然误判成参数变更。显式传了日期并且改了，指纹照样变，冲突照报。
     */
    private static String fingerprintOf(FinancePayable p,
                                        java.time.LocalDate rawPayableDate,
                                        java.time.LocalDate rawDueDate,
                                        String rawPayableNo) {
        StringBuilder sb = new StringBuilder()
                .append(p.getStoreId()).append('|')
                .append(p.getSupplierId()).append('|')
                .append(p.getSupplierName() == null ? "" : p.getSupplierName().trim()).append('|')
                .append(p.getTotalAmount() == null ? "" : p.getTotalAmount().stripTrailingZeros().toPlainString()).append('|')
                .append(rawPayableDate).append('|')
                .append(rawDueDate).append('|')
                // 调用方显式提交的单号属于业务参数：换了单号就是另一张单，
                // 不能拿旧回执糊弄过去 —— 前端严格核单号会永远对不上而挂住。
                // 没传时这里是 null，服务端后面生成的号不进指纹。
                .append(rawPayableNo == null ? "" : rawPayableNo).append('|')
                .append(p.getRemark() == null ? "" : p.getRemark().trim());
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : digest) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    /**
     * 旧的单参数创建入口，已废弃。保留只为让残留调用点得到一条说得清的错误。
     *
     * @deprecated 改用 {@link #create(FinancePayable, String)}，由调用方提供稳定的 requestId
     */
    @Deprecated
    @Transactional
    public FinancePayable create(FinancePayable payable) {
        return create(payable, null).payable;
    }

    /** 原有的创建前校验，原样保留，只是抽出来供幂等分支复用。 */
    private FinancePayable validateForCreate(FinancePayable payable) {
        if (payable == null) throw new IllegalArgumentException("请填写应付单");
        if (payable.getStoreId() == null || payable.getStoreId() <= 0)
            throw new IllegalArgumentException("请选择有效门店");
        try { UserContext.assertStoreAccess(payable.getStoreId()); }
        catch (IllegalArgumentException e) { throw new PayableAccessDeniedException(); }
        if (payable.getPayableId() != null || payable.getSourceReceiptId() != null || payable.getSourceReceiptNo() != null)
            throw new IllegalArgumentException("手工新增不能覆盖原单或冒用收货来源");
        BigDecimal total=payable.getTotalAmount();
        if (total == null || total.signum() <= 0 || total.stripTrailingZeros().scale() > 2 || total.compareTo(new BigDecimal("9999999999.99")) > 0)
            throw new IllegalArgumentException("应付金额必须为正数且最多两位小数");
        if (payable.getPaidAmount() != null && payable.getPaidAmount().signum() != 0)
            throw new IllegalArgumentException("新增应付不能直接标记已付款，请使用结算记账");
        if (payable.getStatus() != null && !"unpaid".equals(payable.getStatus()))
            throw new IllegalArgumentException("新增应付必须为未付款状态");
        if (payable.getSupplierId() == null && (payable.getSupplierName() == null || payable.getSupplierName().isBlank()))
            throw new IllegalArgumentException("请填写供应商");
        if (payable.getPayableNo() == null || payable.getPayableNo().isBlank())
            payable.setPayableNo("PY"+java.util.UUID.randomUUID().toString().replace("-",""));
        if (payable.getPayableNo().length() > 50) throw new IllegalArgumentException("应付单号不能超过50字");
        if (payable.getPayableDate() == null) payable.setPayableDate(today());
        if (payable.getDueDate() != null && payable.getDueDate().isBefore(payable.getPayableDate()))
            throw new IllegalArgumentException("到期日不能早于应付日期");
        payable.setPaidAmount(BigDecimal.ZERO);
        payable.setPendingAmount(total);
        payable.setStatus("unpaid");
        payable.setOperatorName(UserContext.getUsername());
        return payable;
    }
}
