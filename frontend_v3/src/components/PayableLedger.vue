<template>
  <section class="payable-ledger" aria-labelledby="payable-title">
    <header><div><h3 id="payable-title">供应商应付与结算</h3><p>核对收货来源、待付余额和每次结算记录。记账不代表银行到账。</p></div></header>
    <form class="query-bar" @submit.prevent="load">
      <label>门店 <el-input-number v-model="storeId" :min="1" :precision="0" :disabled="busy || showSettle || showCreate || Boolean(pending)" /></label>
      <label>状态 <el-select v-model="status" clearable placeholder="全部状态"><el-option label="未付" value="unpaid"/><el-option label="部分结算" value="partial"/><el-option label="已结清" value="paid"/></el-select></label>
      <el-button native-type="submit" :loading="loading" :disabled="busy">查询</el-button>
      <el-button @click="showCreate = true" :disabled="!ready || busy">手工新增应付</el-button>
    </form>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <el-alert v-if="pending" title="有一笔结果待确认的结算。请恢复原请求，核对成功后再开始下一笔。" type="warning" :closable="false"><el-button @click="resume">恢复待确认结算</el-button></el-alert>
    <el-table :data="rows" v-loading="loading" border empty-text="当前查询没有应付单">
      <el-table-column prop="payableNo" label="应付单号" min-width="180"/>
      <el-table-column prop="supplierName" label="供应商" min-width="130"/>
      <el-table-column prop="sourceReceiptNo" label="收货来源" min-width="160"/>
      <el-table-column prop="payableDate" label="应付日期" width="120"/>
      <el-table-column label="总额 / 已付 / 待付" min-width="210"><template #default="{row}">{{ amount(row.totalAmount) }} / {{ amount(row.paidAmount) }} / {{ amount(row.pendingAmount) }}</template></el-table-column>
      <el-table-column label="操作" width="160"><template #default="{row}"><el-button link @click="history(row)">流水</el-button><el-button link type="primary" @click="openSettlement(row)" :disabled="busy || Boolean(pending) || Number(row.pendingAmount) <= 0">结算记账</el-button></template></el-table-column>
    </el-table>
    <el-dialog v-model="showSettle" title="结算记账" width="min(480px, 94vw)" :close-on-click-modal="false" :close-on-press-escape="!busy" :show-close="!busy">
      <p>应付单：{{ selected?.payableNo || selected?.payableId }}。请先核对原始付款凭据。</p>
      <el-input v-model="settleAmount" inputmode="decimal" aria-label="本次结算金额" :disabled="busy || Boolean(pending)" placeholder="本次金额，最多两位小数" @keyup.enter="settle" />
      <p v-if="pending">失败重试保留原单、原金额和请求号，不会创建第二笔结算。</p>
      <template #footer><el-button :disabled="busy" @click="showSettle = false">稍后核对</el-button><el-button type="primary" :loading="busy" @click="settle">{{ pending ? '按原请求重试' : '确认结算记账' }}</el-button></template>
    </el-dialog>
    <el-dialog v-model="showHistory" title="结算流水" width="min(860px, 96vw)">
      <p>仅展示系统已登记流水；历史已付基数可能没有逐笔记录。</p>
      <el-alert v-if="historyError" :title="historyError" type="error" :closable="false"/>
      <el-table :data="records" v-loading="historyLoading" empty-text="尚无可查询流水">
        <el-table-column prop="settlementNo" label="流水号" min-width="180"/><el-table-column prop="createdAt" label="时间" min-width="170"/><el-table-column prop="settleAmount" label="本次金额"/><el-table-column prop="paidAfter" label="记账后已付"/><el-table-column prop="pendingAfter" label="记账后待付"/>
      </el-table>
    </el-dialog>
    <el-dialog v-model="showCreate" title="手工新增应付" width="min(480px, 94vw)" :close-on-click-modal="false" :show-close="!creating">
      <el-form label-position="top" @submit.prevent="create">
        <el-form-item label="供应商" required><el-input v-model="draft.supplierName" :disabled="creating" maxlength="100"/></el-form-item>
        <el-form-item label="应付金额" required><el-input v-model="draft.totalAmount" inputmode="decimal" :disabled="creating"/></el-form-item>
        <el-form-item label="备注"><el-input v-model="draft.remark" :disabled="creating" maxlength="200"/></el-form-item>
      </el-form><p>采购收货生成的应付请直接查询原单，避免重复手工录入。</p>
      <template #footer><el-button :loading="creating" :disabled="createUncertain" type="primary" @click="create">保存应付</el-button></template>
      <el-alert v-if="createUncertain" title="提交结果待确认，请先查询应付列表核对，不要重复新增。" type="warning" :closable="false"/>
    </el-dialog>
  </section>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { createSettlementAttempt, moneyText } from '@/utils/payableSettlement'
const storeId = ref(null), status = ref(''), ready = ref(false), loading = ref(false), busy = ref(false)
const rows = ref([]), error = ref(''), pending = ref(null), selected = ref(null), settleAmount = ref('')
const showSettle = ref(false), showHistory = ref(false), records = ref([]), historyLoading = ref(false), historyError = ref('')
const showCreate = ref(false), creating = ref(false), createUncertain = ref(false)
const draft = reactive({ supplierName: '', totalAmount: '', remark: '' })
let staffId, attempt, loadedStore, generation = 0, historyGeneration = 0
const message = e => e?.response?.data?.message || e?.message || '请求失败，请重试'
const amount = v => v === null || v === undefined ? '—' : Number(v).toFixed(2)
function setupAttempt() {
  attempt = createSettlementAttempt({ storage: sessionStorage, scope: `${staffId}:${storeId.value}`, send: data => request.post('/finance/payables', data) })
  pending.value = attempt.pending()
}
async function load() {
  const mine = ++generation
  rows.value = []; error.value = ''; ready.value = false
  if (!staffId || !Number.isSafeInteger(storeId.value) || storeId.value <= 0) { error.value = '请确认登录身份并选择有效门店'; return }
  loading.value = true
  try {
    setupAttempt()
    const sid = storeId.value
    const res = await request.get('/finance/payables', { params: { storeId: sid, status: status.value || undefined } })
    if (mine !== generation) return
    if (!Array.isArray(res.data)) throw new Error('应付列表格式不正确')
    if (res.data.some(row => Number(row.storeId) !== sid)) throw new Error('返回数据的门店与查询不一致')
    rows.value = res.data; loadedStore = sid; ready.value = true
  } catch (e) { if (mine === generation) error.value = message(e) }
  finally { if (mine === generation) loading.value = false }
}
function openSettlement(row) {
  if (!ready.value || loadedStore !== storeId.value || pending.value) { error.value = '请先查询当前门店或核对待确认结算'; return }
  selected.value = row; settleAmount.value = String(row.pendingAmount); showSettle.value = true
}
function resume() { selected.value = { payableId: pending.value.payableId }; settleAmount.value = pending.value.settleAmount; showSettle.value = true }
async function settle() {
  if (busy.value || !selected.value) return
  busy.value = true; error.value = ''
  try {
    const data = await attempt.submit(Number(selected.value.payableId), settleAmount.value)
    pending.value = attempt.pending(); showSettle.value = false
    ElMessage.success(data.replayed ? '原结算已确认，未重复记账' : '结算记账成功，请以银行流水核实到账')
    await load()
  } catch (e) { error.value = message(e); pending.value = attempt.pending() }
  finally { busy.value = false }
}
async function history(row) {
  const mine = ++historyGeneration
  showHistory.value = true; historyLoading.value = true; records.value = []; historyError.value = ''
  try {
    const res = await request.get(`/finance/payables/${row.payableId}/settlements`)
    if (mine !== historyGeneration) return
    if (!Array.isArray(res.data)) throw new Error('流水格式不正确')
    records.value = res.data
  } catch (e) { if (mine === historyGeneration) historyError.value = message(e) }
  finally { if (mine === historyGeneration) historyLoading.value = false }
}
async function create() {
  if (creating.value || createUncertain.value) return
  let totalAmount
  try { totalAmount = moneyText(draft.totalAmount); if (!draft.supplierName.trim()) throw new Error('请填写供应商'); if (!ready.value || loadedStore !== storeId.value) throw new Error('请先查询当前门店') }
  catch (e) { error.value = message(e); return }
  creating.value = true
  try {
    const res = await request.post('/finance/payables', { storeId: loadedStore, supplierName: draft.supplierName.trim(), totalAmount, remark: draft.remark })
    if (!res.data?.payableId) throw new Error('未收到应付单号')
    showCreate.value = false; Object.assign(draft, { supplierName: '', totalAmount: '', remark: '' }); await load()
  } catch (e) { error.value = message(e); createUncertain.value = true }
  finally { creating.value = false }
}
onMounted(async () => {
  try {
    const res = await request.get('/auth/me')
    staffId = res.data?.user?.staffId
    storeId.value = Number(res.data?.storeId) === 0 ? 1 : Number(res.data?.storeId)
    await load()
  } catch (e) { error.value = message(e) }
})
</script>
<style scoped>
.payable-ledger { margin: 24px 0; padding: 24px; border: 1px solid var(--color-border); background: var(--color-card); border-radius: 12px; }
h3 { margin: 0 0 8px; } p { color: var(--color-text-secondary); line-height: 1.6; font-size: 13px; }
.query-bar { display: flex; align-items: end; flex-wrap: wrap; gap: 12px; margin: 20px 0; }
label { display: flex; flex-direction: column; gap: 6px; font-size: 13px; }
.el-select { width: 160px; } .el-alert { margin: 12px 0; }
@media(max-width:600px) { .payable-ledger { padding: 14px; } .query-bar label { width: 100%; } .el-select { width: 100%; } }
</style>
