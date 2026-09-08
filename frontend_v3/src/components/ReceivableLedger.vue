<template>
  <section class="receivable-ledger" aria-labelledby="receivable-title">
    <header>
      <div>
        <h3 id="receivable-title">客户应收与收款</h3>
        <p>核对应收余额、每笔收款流水，并登记手工收款。登记只是账务记录，不代表银行实际到账。</p>
      </div>
    </header>

    <form class="query-bar" @submit.prevent="load">
      <label>门店 <el-input-number v-model="storeId" :min="1" :precision="0" :disabled="anyBusy" /></label>
      <label>状态 <el-select v-model="status" clearable placeholder="全部状态">
        <el-option label="未收款" value="unpaid" /><el-option label="部分收款" value="partial" /><el-option label="已收清" value="paid" />
      </el-select></label>
      <el-button native-type="submit" :loading="loading" :disabled="anyBusy">查询</el-button>
      <el-button :disabled="!canStartWrite" @click="openCreate">新建应收</el-button>
      <el-button :disabled="!canStartWrite" @click="openPayment(null)">手工收款</el-button>
    </form>

    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <el-alert v-if="recoveryError" :title="recoveryError" type="error" :closable="false" show-icon />

    <el-alert v-if="receivablePending" type="warning" :closable="false"
              :title="`新建应收 ${receivablePending.payload.receivableNo} 结果待核对，请勿重复新建。`">
      <p>金额 {{ receivablePending.payload.totalAmount }} 元。{{ receivableNote }}</p>
      <el-button :disabled="anyBusy" @click="load">查询并核对原单</el-button>
      <el-button :disabled="!canResume" :loading="creating" @click="resumeReceivable">恢复原请求（原金额、原参数）</el-button>
    </el-alert>

    <el-alert v-if="paymentPending" type="warning" :closable="false"
              :title="`收款 ${paymentPending.payload.paymentNo} 结果待核对，请勿重复收款。`">
      <p>金额 {{ paymentPending.payload.amount }} 元。{{ paymentNote }}</p>
      <el-button :disabled="anyBusy" @click="loadPayments">查询并核对流水</el-button>
      <el-button :disabled="!canResume" :loading="paying" @click="resumePayment">恢复原请求（原金额、原参数）</el-button>
    </el-alert>

    <el-table :data="visibleRows" v-loading="loading" border empty-text="当前查询没有应收单">
      <el-table-column prop="receivable_no" label="应收单号" min-width="180" />
      <el-table-column prop="customer_name" label="客户" min-width="120" />
      <el-table-column prop="booking_no" label="预订单" min-width="130" />
      <el-table-column prop="due_date" label="到期日" width="120" />
      <el-table-column label="总额 / 已收 / 待收" min-width="220">
        <template #default="{ row }">
          {{ money(row.total_amount) }} / {{ money(row.received_amount) }} / {{ pendingOf(row) }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">{{ statusText(row.status) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button link @click="openDetail(row)">明细</el-button>
          <el-button link type="primary" :disabled="!canStartWrite || row.status === 'paid'"
                     @click="openPayment(row)">登记收款</el-button>
        </template>
      </el-table-column>
    </el-table>
    <p class="note">账务记录不提供删除。如需更正请通过冲销或新增记录处理，保留原始留痕以便对账。</p>

    <el-dialog v-model="showDetail" title="应收明细与收款流水" width="min(880px, 96vw)">
      <el-alert v-if="detailError" :title="detailError" type="error" :closable="false" />
      <template v-if="detail">
        <p>
          {{ detail.receivable_no }} · 客户 {{ detail.customer_name || '未填写' }} ·
          应收 {{ money(detail.total_amount) }}，已收 {{ money(detail.received_amount) }}，
          待收 {{ pendingOf(detail) }}（{{ statusText(detail.status) }}）
        </p>
        <el-table :data="detail.payments || []" v-loading="detailLoading" empty-text="尚无收款流水">
          <el-table-column prop="payment_no" label="收款单号" min-width="180" />
          <el-table-column prop="payment_date" label="收款日期" width="120" />
          <el-table-column label="金额" width="120"><template #default="{ row }">{{ money(row.amount) }}</template></el-table-column>
          <el-table-column prop="payment_method" label="方式" width="110" />
          <el-table-column prop="payment_category" label="类别" min-width="120" />
          <el-table-column prop="operator_name" label="操作人" width="110" />
          <el-table-column prop="remark" label="业务说明" min-width="160" />
        </el-table>
      </template>
    </el-dialog>

    <el-dialog v-model="showCreate" title="新建应收" width="min(520px, 94vw)"
               :close-on-click-modal="false" :show-close="!creating" :close-on-press-escape="!creating">
      <el-form label-position="top" @submit.prevent="createReceivable">
        <el-form-item label="应收金额" required>
          <el-input v-model="receivableDraft.totalAmount" inputmode="decimal" :disabled="writeLocked" />
        </el-form-item>
        <el-form-item label="客户名称"><el-input v-model="receivableDraft.customerName" :disabled="writeLocked" /></el-form-item>
        <el-form-item label="客户编号"><el-input v-model="receivableDraft.customerId" :disabled="writeLocked" placeholder="留空表示不关联客户档案" /></el-form-item>
        <el-form-item label="预订单号"><el-input v-model="receivableDraft.bookingNo" :disabled="writeLocked" /></el-form-item>
        <el-form-item label="应收日期"><el-input v-model="receivableDraft.receivableDate" :disabled="writeLocked" placeholder="yyyy-MM-dd，留空由服务端取当天" /></el-form-item>
        <el-form-item label="账期天数"><el-input v-model="receivableDraft.creditDays" :disabled="writeLocked" placeholder="留空由服务端取 30 天" /></el-form-item>
        <el-form-item label="到期日"><el-input v-model="receivableDraft.dueDate" :disabled="writeLocked" placeholder="留空按应收日期加账期推算" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="receivableDraft.remark" :disabled="writeLocked" /></el-form-item>
      </el-form>
      <p>留空的日期与账期由服务端补默认值；这里不代填，避免同一个请求跨天恢复时被当成改了参数。</p>
      <template #footer>
        <el-button :disabled="creating" @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="creating" :disabled="!canStartWrite" @click="createReceivable">保存应收</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showPayment" :title="paymentTarget ? '登记收款' : '手工收款（无应收来源）'"
               width="min(520px, 94vw)" :close-on-click-modal="false" :show-close="!paying" :close-on-press-escape="!paying">
      <p v-if="paymentTarget">
        应收单 {{ paymentTarget.receivable_no }}，待收 {{ pendingOf(paymentTarget) }}。
        超过待收金额会被服务端拒绝，系统不支持预收。
      </p>
      <p v-else>没有应收来源的收款必须写明类别与业务说明，否则事后无法判断这笔钱因何而收。</p>
      <el-form label-position="top" @submit.prevent="recordPayment">
        <el-form-item label="收款金额" required>
          <el-input v-model="paymentDraft.amount" inputmode="decimal" :disabled="writeLocked" />
        </el-form-item>
        <el-form-item label="收款日期"><el-input v-model="paymentDraft.paymentDate" :disabled="writeLocked" placeholder="yyyy-MM-dd，留空由服务端取当天" /></el-form-item>
        <el-form-item label="收款方式"><el-input v-model="paymentDraft.paymentMethod" :disabled="writeLocked" /></el-form-item>
        <el-form-item label="收款账户编号"><el-input v-model="paymentDraft.accountId" :disabled="writeLocked" placeholder="留空表示不指定账户" /></el-form-item>
        <el-form-item :label="paymentTarget ? '收款类别' : '收款类别（必填）'" :required="!paymentTarget">
          <el-input v-model="paymentDraft.category" :disabled="writeLocked" maxlength="32" show-word-limit />
        </el-form-item>
        <el-form-item :label="paymentTarget ? '业务说明' : '业务说明（必填）'" :required="!paymentTarget">
          <el-input v-model="paymentDraft.remark" :disabled="writeLocked" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="paying" @click="showPayment = false">取消</el-button>
        <el-button type="primary" :loading="paying" :disabled="!canStartWrite" @click="recordPayment">确认登记</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import request from '@/utils/request'
import {
  createReceivableLedger, displayAmount, pendingText, explainError, STATUS_TEXT
} from '@/utils/receivableLedger/index.js'

const staffId = ref(null), storeId = ref(null), status = ref('')
const loading = ref(false), creating = ref(false), paying = ref(false), ready = ref(false)
const rows = ref([]), error = ref(''), recoveryError = ref('')
const receivablePending = ref(null), paymentPending = ref(null)
const receivableNote = ref(''), paymentNote = ref('')
const showCreate = ref(false), showPayment = ref(false), showDetail = ref(false)
const detail = ref(null), detailLoading = ref(false), detailError = ref('')
const paymentTarget = ref(null)

const receivableDraft = reactive(emptyReceivable())
const paymentDraft = reactive(emptyPayment())

let ledger = null, loadedStore, generation = 0, detailGeneration = 0, scopeVersion = 0, disposed = false

const money = displayAmount
const pendingOf = pendingText
const statusText = s => STATUS_TEXT[s] || s || '未知'

const anyBusy = computed(() => loading.value || creating.value || paying.value)
const validScope = () => Number.isSafeInteger(staffId.value) && staffId.value > 0 &&
  Number.isSafeInteger(storeId.value) && storeId.value > 0
const scopeIsCurrent = (version, sid) => !disposed && version === scopeVersion && sid === storeId.value
// 有任何一笔结果未确认，就不许开新的写入——先把上一笔弄清楚
const canStartWrite = computed(() => validScope() && ready.value && loadedStore === storeId.value &&
  !anyBusy.value && !recoveryError.value && !receivablePending.value && !paymentPending.value)
const canResume = computed(() => validScope() && !anyBusy.value && !recoveryError.value)
const writeLocked = computed(() => anyBusy.value || !!recoveryError.value)
const visibleRows = computed(() => status.value ? rows.value.filter(r => r.status === status.value) : rows.value)

function emptyReceivable() {
  return { totalAmount: '', customerName: '', customerId: '', bookingNo: '', receivableDate: '', creditDays: '', dueDate: '', remark: '' }
}
function emptyPayment() {
  return { amount: '', paymentDate: '', paymentMethod: '', accountId: '', category: '', remark: '' }
}

function api() {
  return {
    createReceivable: body => request.post('/finance/receivable', body),
    recordPayment: body => request.post('/finance/payment', body),
    listReceivables: sid => request.get('/finance/receivable', { params: { storeId: sid } }),
    receivableDetail: (id, sid) => request.get('/finance/receivable', { params: { storeId: sid, id } }),
    listPayments: sid => request.get('/finance/payment', { params: { storeId: sid } })
  }
}

function readRecovery() {
  recoveryError.value = ''
  receivablePending.value = null
  paymentPending.value = null
  const failures = []
  if (validScope() && !ledger) failures.push('恢复存储不可用，可只读查询，写入已锁定。')
  try { receivablePending.value = ledger?.receivable.pending() || null } catch (e) { failures.push(e.message) }
  try { paymentPending.value = ledger?.payment.pending() || null } catch (e) { failures.push(e.message) }
  recoveryError.value = failures.join(' ')
}

function invalidateQuery() {
  generation++; detailGeneration++
  rows.value = []; ready.value = false; loadedStore = undefined
  loading.value = false; detailLoading.value = false; detailError.value = ''
  showDetail.value = false; detail.value = null
}

function setupScope() {
  scopeVersion++; invalidateQuery()
  error.value = ''; receivableNote.value = ''; paymentNote.value = ''
  showCreate.value = false; showPayment.value = false; paymentTarget.value = null
  Object.assign(receivableDraft, emptyReceivable())
  Object.assign(paymentDraft, emptyPayment())
  ledger = null
  if (validScope()) {
    try {
      ledger = createReceivableLedger({
        storage: sessionStorage, staffId: staffId.value, storeId: storeId.value, api: api()
      })
    } catch { recoveryError.value = '恢复存储不可用或身份无效，可只读查询，写入已锁定'; return }
  }
  readRecovery()
  // 有未确认的请求时，把原参数回填到表单，方便人工比对；但表单不能改这条的重发内容
  if (receivablePending.value) Object.assign(receivableDraft, formFromReceivable(receivablePending.value.payload))
  if (paymentPending.value) Object.assign(paymentDraft, formFromPayment(paymentPending.value.payload))
}

function formFromReceivable(p) {
  return {
    totalAmount: p.totalAmount, customerName: p.customerName ?? '', customerId: p.customerId ?? '',
    bookingNo: p.bookingNo ?? '', receivableDate: p.receivableDate ?? '',
    creditDays: p.creditDays ?? '', dueDate: p.dueDate ?? '', remark: p.remark ?? ''
  }
}
function formFromPayment(p) {
  return {
    amount: p.amount, paymentDate: p.paymentDate ?? '', paymentMethod: p.paymentMethod ?? '',
    accountId: p.accountId ?? '', category: p.category ?? '', remark: p.remark ?? ''
  }
}

watch([staffId, storeId], setupScope, { flush: 'sync' })

async function load() {
  if (anyBusy.value) return
  const mine = ++generation, version = scopeVersion, sid = storeId.value
  rows.value = []; error.value = ''; ready.value = false
  if (!validScope()) { error.value = '请确认登录身份并选择有效门店'; return }
  loading.value = true
  readRecovery()
  try {
    const res = await ledger.listReceivables()
    if (mine !== generation || !scopeIsCurrent(version, sid)) return
    if (res.code !== 200 || !Array.isArray(res.data)) throw new Error('应收列表格式不正确')
    if (res.data.some(row => Number(row.store_id) !== sid)) throw new Error('返回数据的门店与查询不一致')
    rows.value = res.data; loadedStore = sid; ready.value = true
    reconcile('receivable', res.data)
  } catch (e) {
    if (mine === generation && scopeIsCurrent(version, sid)) error.value = explainError(e)
  } finally {
    if (mine === generation && scopeIsCurrent(version, sid)) { loading.value = false; readRecovery() }
  }
}

async function loadPayments() {
  if (anyBusy.value || !validScope()) return
  const version = scopeVersion, sid = storeId.value
  loading.value = true; error.value = ''
  try {
    const res = await ledger.listPayments()
    if (!scopeIsCurrent(version, sid)) return
    if (res.code !== 200 || !Array.isArray(res.data)) throw new Error('收款流水格式不正确')
    reconcile('payment', res.data)
  } catch (e) {
    if (scopeIsCurrent(version, sid)) error.value = explainError(e)
  } finally {
    if (scopeIsCurrent(version, sid)) { loading.value = false; readRecovery() }
  }
}

// 核对结果一律如实说明。"列表里没找到"绝不等于"没提交"，不能据此解锁写入。
const RECONCILE_NOTE = {
  missing: '当前列表未找到原单，这不等于没有提交；请换个状态或稍后再查，锁定不解除。',
  ambiguous: '发现重复单号，需人工核对，锁定不解除。',
  mismatch: '原单的门店或主键与本次请求不一致，需人工核对，锁定不解除。',
  waiting: '仍在提交中，请稍候。'
}
function reconcile(which, list) {
  if (!ledger || recoveryError.value) return
  const target = which === 'receivable' ? ledger.receivable : ledger.payment
  const note = which === 'receivable' ? receivableNote : paymentNote
  try {
    note.value = RECONCILE_NOTE[target.reconcile(list)] || ''
  } catch (e) {
    note.value = e.message
  }
}

function openCreate() {
  if (!canStartWrite.value) return
  Object.assign(receivableDraft, emptyReceivable())
  showCreate.value = true
}

function openPayment(row) {
  if (!canStartWrite.value) return
  paymentTarget.value = row
  Object.assign(paymentDraft, emptyPayment())
  showPayment.value = true
}

async function openDetail(row) {
  if (!ready.value || loadedStore !== storeId.value) return
  const mine = ++detailGeneration, version = scopeVersion, sid = storeId.value
  showDetail.value = true; detailLoading.value = true; detail.value = null; detailError.value = ''
  try {
    const res = await ledger.receivableDetail(Number(row.receivable_id))
    if (mine !== detailGeneration || !scopeIsCurrent(version, sid)) return
    if (res.code !== 200 || !res.data) throw new Error('应收明细格式不正确')
    detail.value = res.data
  } catch (e) {
    if (mine === detailGeneration && scopeIsCurrent(version, sid)) detailError.value = explainError(e)
  } finally {
    if (mine === detailGeneration && scopeIsCurrent(version, sid)) detailLoading.value = false
  }
}

async function createReceivable() { await write('receivable', false) }
async function resumeReceivable() { await write('receivable', true) }
async function recordPayment() { await write('payment', false) }
async function resumePayment() { await write('payment', true) }

async function write(which, restoring) {
  if (!ledger) return
  if (restoring ? !canResume.value : !canStartWrite.value) return
  const op = which === 'receivable' ? ledger.receivable : ledger.payment
  const flag = which === 'receivable' ? creating : paying
  const version = scopeVersion, sid = storeId.value
  flag.value = true; error.value = ''
  let ok = false
  try {
    if (restoring) await op.resume()
    else if (which === 'receivable') await op.submit({ ...receivableDraft })
    else await op.submit({ ...paymentDraft, receivableId: paymentTarget.value?.receivable_id ?? null })
    if (!scopeIsCurrent(version, sid)) return
    ok = true
    showCreate.value = false; showPayment.value = false
  } catch (e) {
    if (scopeIsCurrent(version, sid)) error.value = explainError(e)
  } finally {
    flag.value = false
    if (scopeIsCurrent(version, sid)) readRecovery()
  }
  if (ok && scopeIsCurrent(version, sid)) {
    await load()
    if (which === 'payment') await loadPayments()
  }
}

onMounted(async () => {
  try {
    const res = await request.get('/auth/me')
    if (disposed) return
    if (res.code !== 200) throw new Error('无法确认登录身份')
    staffId.value = Number(res.data?.user?.staffId)
    storeId.value = Number(res.data?.storeId) === 0 ? null : Number(res.data?.storeId)
    await load()
  } catch (e) {
    if (!disposed) error.value = explainError(e)
  }
})

onUnmounted(() => { disposed = true; scopeVersion++; invalidateQuery() })
</script>

<style scoped>
.receivable-ledger { margin: 24px 0; padding: 24px; border: 1px solid var(--color-border); background: var(--color-card); border-radius: 12px; }
h3 { margin: 0 0 8px; }
p { color: var(--color-text-secondary); line-height: 1.6; font-size: 13px; }
.note { margin-top: 12px; }
.query-bar { display: flex; align-items: end; flex-wrap: wrap; gap: 12px; margin: 20px 0; }
label { display: flex; flex-direction: column; gap: 6px; font-size: 13px; }
.el-select { width: 160px; }
.el-alert { margin: 12px 0; }
@media (max-width: 600px) {
  .receivable-ledger { padding: 14px; }
  .query-bar label { width: 100%; }
  .el-select { width: 100%; }
}
</style>
