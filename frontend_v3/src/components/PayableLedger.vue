<template>
  <section class="payable-ledger" aria-labelledby="payable-title">
    <header><div><h3 id="payable-title">供应商应付与结算</h3><p>核对收货来源、待付余额和每次结算记录。记账不代表银行到账。</p></div></header>
    <form class="query-bar" @submit.prevent="load">
      <label>门店 <el-input-number v-model="storeId" :min="1" :precision="0" :disabled="busy || creating || showSettle || showCreate" /></label>
      <label>状态 <el-select v-model="status" clearable placeholder="全部状态"><el-option label="未付" value="unpaid"/><el-option label="部分结算" value="partial"/><el-option label="已结清" value="paid"/></el-select></label>
      <el-button native-type="submit" :loading="loading" :disabled="busy || creating">查询</el-button>
      <el-button @click="openCreate" :disabled="!canStartWrite">手工新增应付</el-button>
    </form>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <el-alert v-if="recoveryError" :title="recoveryError" type="error" :closable="false" show-icon />
    <el-alert v-if="creationPending" type="warning" :closable="false" :title="`新增应付 ${creationPending.payload.payableNo} 结果待核对，请核对原单或恢复原请求。`">
      <p>{{ creationPending.payload.supplierName }} · {{ creationPending.payload.totalAmount }} 元。{{ creationRecoveryNote }}</p>
      <el-button :disabled="busy || creating" @click="load">查询并核对原单</el-button>
      <el-button v-if="creationPending.version === 2" :disabled="!canResumeCreation" :loading="creating" @click="resumeCreation">恢复原请求（原金额、原供应商）</el-button>
      <p v-else>旧版记录没有创建请求号，只能查询核对，不能重发。</p>
    </el-alert>
    <el-alert v-if="pending" title="有一笔结果待确认的结算。请恢复原请求，核对成功后再开始下一笔。" type="warning" :closable="false"><el-button :disabled="busy || creating || !!recoveryError" @click="resume">恢复待确认结算</el-button></el-alert>
    <el-table :data="rows" v-loading="loading" border empty-text="当前查询没有应付单">
      <el-table-column prop="payableNo" label="应付单号" min-width="180"/>
      <el-table-column prop="supplierName" label="供应商" min-width="130"/>
      <el-table-column prop="sourceReceiptNo" label="收货来源" min-width="160"/>
      <el-table-column prop="payableDate" label="应付日期" width="120"/>
      <el-table-column label="总额 / 已付 / 待付" min-width="210"><template #default="{row}">{{ amount(row.totalAmount) }} / {{ amount(row.paidAmount) }} / {{ amount(row.pendingAmount) }}</template></el-table-column>
      <el-table-column label="操作" width="160"><template #default="{row}"><el-button link @click="history(row)">流水</el-button><el-button link type="primary" @click="openSettlement(row)" :disabled="!canStartWrite || Number(row.pendingAmount) <= 0">结算记账</el-button></template></el-table-column>
    </el-table>
    <el-dialog v-model="showSettle" title="结算记账" width="min(480px, 94vw)" :close-on-click-modal="false" :close-on-press-escape="!busy" :show-close="!busy">
      <p>应付单：{{ selected?.payableNo || selected?.payableId }}。请先核对原始付款凭据。</p>
      <el-input v-model="settleAmount" inputmode="decimal" aria-label="本次结算金额" :disabled="busy || creating || Boolean(pending) || !!recoveryError" placeholder="本次金额，最多两位小数" @keyup.enter="settle" />
      <p v-if="pending">失败重试保留原单、原金额和请求号，不会创建第二笔结算。</p>
      <template #footer><el-button :disabled="busy" @click="showSettle = false">稍后核对</el-button><el-button type="primary" :loading="busy" :disabled="!canSubmitSettlement" @click="settle">{{ pending ? '按原请求重试' : '确认结算记账' }}</el-button></template>
    </el-dialog>
    <el-dialog v-model="showHistory" title="结算流水" width="min(860px, 96vw)">
      <p>仅展示系统已登记流水；历史已付基数可能没有逐笔记录。</p>
      <el-alert v-if="historyError" :title="historyError" type="error" :closable="false"/>
      <el-table :data="records" v-loading="historyLoading" empty-text="尚无可查询流水">
        <el-table-column prop="settlementNo" label="流水号" min-width="180"/><el-table-column prop="createdAt" label="时间" min-width="170"/><el-table-column prop="settleAmount" label="本次金额"/><el-table-column prop="paidAfter" label="记账后已付"/><el-table-column prop="pendingAfter" label="记账后待付"/>
      </el-table>
    </el-dialog>
    <el-dialog v-model="showCreate" title="手工新增应付" width="min(480px, 94vw)" :close-on-click-modal="false" :show-close="!creating" :close-on-press-escape="!creating">
      <el-form label-position="top" @submit.prevent="create">
        <el-form-item label="供应商" required><el-input v-model="draft.supplierName" :disabled="creating || !!creationPending || !!recoveryError" maxlength="100"/></el-form-item>
        <el-form-item label="应付金额" required><el-input v-model="draft.totalAmount" inputmode="decimal" :disabled="creating || !!creationPending || !!recoveryError"/></el-form-item>
        <el-form-item label="备注"><el-input v-model="draft.remark" :disabled="creating || !!creationPending || !!recoveryError" maxlength="200"/></el-form-item>
      </el-form><p>采购收货生成的应付请直接查询原单，避免重复手工录入。</p>
      <template #footer><el-button :loading="creating" :disabled="!canStartWrite" type="primary" @click="create">保存应付</el-button></template>
      <el-alert v-if="creationPending" title="提交结果待确认，请先查询应付列表核对，不要重复新增。" type="warning" :closable="false"/>
    </el-dialog>
  </section>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { createSettlementAttempt } from '@/utils/payableSettlement'
import { createPayableCreation } from '@/utils/payableCreation'
const staffId = ref(null), storeId = ref(null), status = ref(''), ready = ref(false), loading = ref(false), busy = ref(false)
const rows = ref([]), error = ref(''), pending = ref(null), selected = ref(null), settleAmount = ref('')
const showSettle = ref(false), showHistory = ref(false), records = ref([]), historyLoading = ref(false), historyError = ref('')
const showCreate = ref(false), creating = ref(false), creationPending = ref(null), recoveryError = ref(''), creationRecoveryNote = ref('')
const draft = reactive({ supplierName: '', totalAmount: '', remark: '' })
let attempt, creator, loadedStore, generation = 0, historyGeneration = 0, scopeVersion = 0, disposed = false
const message = e => e?.response?.data?.message || e?.message || '请求失败，请重试'
const amount = v => v === null || v === undefined ? '—' : Number(v).toFixed(2)
const validScope = () => Number.isSafeInteger(staffId.value) && staffId.value > 0 && Number.isSafeInteger(storeId.value) && storeId.value > 0
const scopeIsCurrent = (version, sid) => !disposed && version === scopeVersion && sid === storeId.value
const writeReady = computed(() => validScope() && ready.value && loadedStore === storeId.value && !loading.value && !busy.value && !creating.value && !recoveryError.value)
const canResumeCreation = computed(() => writeReady.value && !pending.value && creationPending.value?.version === 2)
const canStartWrite = computed(() => writeReady.value && !pending.value && !creationPending.value)
const canSubmitSettlement = computed(() => !!selected.value && !busy.value && !creating.value && !recoveryError.value && validScope() &&
  (pending.value ? pending.value.payableId === selected.value.payableId : canStartWrite.value && rows.value.includes(selected.value)))
function readRecovery() {
  recoveryError.value = ''; pending.value = null; creationPending.value = null
  const failures = []
  if (validScope() && (!attempt || !creator)) failures.push('恢复存储不可用，可只读查询，写入已锁定。')
  try { pending.value = attempt?.pending() || null }
  catch { failures.push('结算恢复记录损坏或不可读取，原条目已保留；可查询列表与流水，写入已锁定，请人工核对。') }
  try { creationPending.value = creator?.pending() || null }
  catch { failures.push('新增应付恢复记录损坏或不可读取，原条目已保留；可查询列表与流水，写入已锁定，请人工核对。') }
  recoveryError.value = failures.join(' ')
}
function invalidateQuery() {
  generation++; historyGeneration++; rows.value = []; records.value = []; ready.value = false; loadedStore = undefined
  loading.value = false; historyLoading.value = false; historyError.value = ''; showHistory.value = false
  selected.value = null; showSettle.value = false
}
function setupScope() {
  scopeVersion++; invalidateQuery(); error.value = ''; showCreate.value = false; creationRecoveryNote.value = ''
  attempt = null; creator = null
  Object.assign(draft, { supplierName: '', totalAmount: '', remark: '' })
  if (validScope()) {
    const scope = `${staffId.value}:${storeId.value}`
    try {
      attempt = createSettlementAttempt({ storage: sessionStorage, scope, send: data => request.post('/finance/payables', data) })
      creator = createPayableCreation({ storage: sessionStorage, scope, storeId: storeId.value, send: data => request.post('/finance/payables', data) })
    } catch { recoveryError.value = '恢复存储不可用，可只读查询，写入已锁定'; return }
  }
  readRecovery()
  if (creationPending.value) Object.assign(draft, creationPending.value.payload)
}
watch([staffId, storeId], setupScope, { flush: 'sync' })
watch(status, () => { invalidateQuery(); error.value = '' }, { flush: 'sync' })
async function load() {
  if (busy.value || creating.value) return
  const mine = ++generation, version = scopeVersion, sid = storeId.value
  rows.value = []; error.value = ''; ready.value = false
  if (!validScope()) { loading.value = false; error.value = '请确认登录身份并选择有效门店'; return }
  loading.value = true
  readRecovery()
  try {
    const res = await request.get('/finance/payables', { params: { storeId: sid, status: status.value || undefined } })
    if (mine !== generation || !scopeIsCurrent(version, sid)) return
    if (res.code !== 200 || !Array.isArray(res.data)) throw new Error(res.message || '应付列表格式不正确')
    if (res.data.some(row => Number(row.storeId) !== sid)) throw new Error('返回数据的门店与查询不一致')
    rows.value = res.data; loadedStore = sid; ready.value = true
    if (creator && !recoveryError.value) {
      try {
        const result = creator.reconcile(rows.value)
        creationRecoveryNote.value = ({ missing: '当前列表未找到原单，不能视为未提交；可选择全部状态继续核对。', ambiguous: '发现重复单号，需人工核对；尚未解除锁定。', mismatch: '原单金额或供应商不一致，需人工核对；尚未解除锁定。' })[result] || ''
        if (result === 'confirmed') { Object.assign(draft, { supplierName: '', totalAmount: '', remark: '' }); ElMessage.success('原新增应付已唯一匹配确认，未重复提交') }
        readRecovery()
      } catch { recoveryError.value = '新增恢复核对失败，原条目已保留；写入已锁定，请人工核对' }
    }
  } catch (e) { if (mine === generation && scopeIsCurrent(version, sid)) error.value = message(e) }
  finally { if (mine === generation && scopeIsCurrent(version, sid)) loading.value = false }
}
function openCreate() { if (canStartWrite.value) showCreate.value = true }
function openSettlement(row) {
  if (!canStartWrite.value || !rows.value.includes(row) || Number(row.storeId) !== storeId.value || Number(row.pendingAmount) <= 0) { error.value = '请先查询当前门店或核对恢复记录'; return }
  selected.value = row; settleAmount.value = String(row.pendingAmount); showSettle.value = true
}
function resume() {
  if (!pending.value || busy.value || creating.value || recoveryError.value) return
  selected.value = { payableId: pending.value.payableId }; settleAmount.value = pending.value.settleAmount; showSettle.value = true
}
async function settle() {
  if (!canSubmitSettlement.value || !attempt) return
  const ownAttempt = attempt, version = scopeVersion, sid = storeId.value
  busy.value = true; error.value = ''
  let succeeded = false
  try {
    const data = await ownAttempt.submit(Number(selected.value.payableId), settleAmount.value)
    if (!scopeIsCurrent(version, sid)) return
    readRecovery(); showSettle.value = false; succeeded = true
    ElMessage.success(data.replayed ? '原结算已确认，未重复记账' : '结算记账成功，请以银行流水核实到账')
  } catch (e) { if (scopeIsCurrent(version, sid)) { error.value = message(e); readRecovery() } }
  finally { busy.value = false }
  if (succeeded && scopeIsCurrent(version, sid)) await load()
}
async function history(row) {
  if (!ready.value || loadedStore !== storeId.value || !rows.value.includes(row)) return
  const mine = ++historyGeneration, version = scopeVersion, sid = storeId.value
  showHistory.value = true; historyLoading.value = true; records.value = []; historyError.value = ''
  try {
    const res = await request.get(`/finance/payables/${row.payableId}/settlements`)
    if (mine !== historyGeneration || !scopeIsCurrent(version, sid)) return
    if (res.code !== 200 || !Array.isArray(res.data)) throw new Error(res.message || '流水格式不正确')
    records.value = res.data
  } catch (e) { if (mine === historyGeneration && scopeIsCurrent(version, sid)) historyError.value = message(e) }
  finally { if (mine === historyGeneration && scopeIsCurrent(version, sid)) historyLoading.value = false }
}
async function create() { return sendCreation(false) }
async function resumeCreation() { return sendCreation(true) }
async function sendCreation(restoring) {
  if (!(restoring ? canResumeCreation.value : canStartWrite.value) || !creator) return
  const ownCreator = creator, version = scopeVersion, sid = storeId.value
  creating.value = true; error.value = ''
  let succeeded = false
  try {
    if (restoring) await ownCreator.resume()
    else await ownCreator.submit({ ...draft })
    if (!scopeIsCurrent(version, sid)) return
    succeeded = true; showCreate.value = false
  } catch (e) { if (scopeIsCurrent(version, sid)) error.value = message(e) }
  finally {
    creating.value = false
    if (scopeIsCurrent(version, sid)) readRecovery()
  }
  if (succeeded && scopeIsCurrent(version, sid)) await load()
}
onMounted(async () => {
  try {
    const res = await request.get('/auth/me')
    if (disposed) return
    if (res.code !== 200) throw new Error(res.message || '无法确认登录身份')
    staffId.value = Number(res.data?.user?.staffId)
    storeId.value = Number(res.data?.storeId) === 0 ? null : Number(res.data?.storeId)
    await load()
  } catch (e) { if (!disposed) error.value = message(e) }
})
onUnmounted(() => { disposed = true; scopeVersion++; invalidateQuery() })
</script>
<style scoped>
.payable-ledger { margin: 24px 0; padding: 24px; border: 1px solid var(--color-border); background: var(--color-card); border-radius: 12px; }
h3 { margin: 0 0 8px; } p { color: var(--color-text-secondary); line-height: 1.6; font-size: 13px; }
.query-bar { display: flex; align-items: end; flex-wrap: wrap; gap: 12px; margin: 20px 0; }
label { display: flex; flex-direction: column; gap: 6px; font-size: 13px; }
.el-select { width: 160px; } .el-alert { margin: 12px 0; }
@media(max-width:600px) { .payable-ledger { padding: 14px; } .query-bar label { width: 100%; } .el-select { width: 100%; } }
</style>
