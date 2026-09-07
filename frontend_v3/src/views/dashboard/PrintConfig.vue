<template>
  <div class="print-config-page">
    <header class="page-header"><div><h2>打印配置 · Print Config</h2><p>打印机管理与出票规则</p></div><el-button :loading="state.busy === 'load'" :disabled="!!state.busy" @click="refresh">刷新配置</el-button></header>
    <el-alert :title="PRINT_NOTE" type="info" :closable="false" show-icon />
    <div class="toolbar">
      <span v-if="!identity">登录身份尚未确认</span>
      <template v-else-if="gm"><label for="print-store">门店编号</label><el-input-number id="print-store" v-model="selectedStore" :min="1" :precision="0" :controls="false" placeholder="选择门店" /><span>与当前门店选择联动</span></template>
      <span v-else>当前门店：{{ state.sid || '未确认' }}</span>
      <el-checkbox v-model="showArchived">显示归档历史</el-checkbox>
    </div>
    <el-alert v-if="state.error" :title="state.error" type="error" :closable="false" show-icon />
    <el-alert v-if="state.notice" :title="state.notice" type="success" :closable="false" />
    <el-alert v-if="state.uncertain || state.expected" title="上次保存结果尚未确认，请刷新并核对记录。为避免重复新增，暂不能继续保存。" type="warning" :closable="false" />
    <section><div class="section-heading"><h3>打印机管理</h3><el-button :disabled="!writable" @click="openEditor('printers')">添加打印机</el-button></div>
      <el-empty v-if="!visiblePrinters.length" :description="state.ready ? '暂无打印机配置' : '请选择门店并加载配置'" />
      <div class="printer-grid"><article v-for="printer in visiblePrinters" :key="printer.id" class="printer-card">
        <div class="section-heading"><h4>{{ printer.name }}</h4><el-tag :type="printer.archive ? 'info' : 'warning'">{{ printer.archive ? '已归档' : '连接未验证' }}</el-tag></div>
        <p>{{ PRINT_TYPES[printer.type] || printer.type }} · {{ printer.paperWidth === 'A4' ? 'A4' : printer.paperWidth + 'mm' }} · {{ printer.copies }}份</p>
        <p class="address">设备地址：{{ printer.address || '未填写' }}</p>
        <div class="actions"><el-button :disabled="!writable || printer.archive" @click="openEditor('printers', printer)">编辑打印机</el-button><el-button :disabled="!writable || printer.archive" @click="archiveRow('printers', printer)">归档打印机</el-button></div>
      </article></div>
    </section>
    <section><div class="section-heading"><h3>出票规则</h3><el-button :disabled="!writable || !activePrinters.length" @click="openEditor('rules')">添加规则</el-button></div>
      <p>规则启用表示已保存的选择；实际生效状态单独显示。归档后保留历史，不可恢复。</p>
      <div class="table-wrapper"><el-table :data="visibleRules" stripe empty-text="暂无规则配置">
        <el-table-column prop="name" label="规则名称" min-width="150" />
        <el-table-column label="单据" min-width="110"><template #default="{ row }">{{ DOCUMENT_TYPES[row.documentType] || row.documentType }}</template></el-table-column>
        <el-table-column label="目标打印机" min-width="140"><template #default="{ row }">{{ printerName(row.printerId) }}</template></el-table-column>
        <el-table-column label="配置选择" width="100"><template #default="{ row }">{{ row.configuredEnabled ? '已启用' : '未启用' }}</template></el-table-column>
        <el-table-column label="实际状态" width="110"><template #default="{ row }">{{ row.archive ? '已归档' : row.effectiveEnabled === true ? '已生效' : '尚未生效' }}</template></el-table-column>
        <el-table-column label="操作" width="170"><template #default="{ row }"><el-button text :disabled="!writable || row.archive" @click="openEditor('rules', row)">编辑规则</el-button><el-button text :disabled="!writable || row.archive" @click="archiveRow('rules', row)">归档规则</el-button></template></el-table-column>
      </el-table></div>
    </section>
    <el-dialog v-model="dialogOpen" :title="(draft.id ? '编辑' : '添加') + (kind === 'printers' ? '打印机' : '规则')" width="min(520px, calc(100vw - 24px))" :close-on-click-modal="false" :close-on-press-escape="!state.busy" :show-close="!state.busy">
      <el-alert v-if="state.error" :title="state.error" type="error" :closable="false" />
      <el-form label-position="top" :disabled="!!state.busy">
        <el-form-item label="名称" required><el-input v-model="draft.name" maxlength="80" /></el-form-item>
        <template v-if="kind === 'printers'">
          <el-form-item label="类型" required><el-select v-model="draft.type"><el-option v-for="(label, value) in PRINT_TYPES" :key="value" :label="label" :value="value" /></el-select></el-form-item>
          <el-form-item label="设备地址"><el-input v-model="draft.address" maxlength="255" placeholder="仅保存地址，不验证连接" /></el-form-item>
          <el-form-item label="纸张"><el-select v-model="draft.paperWidth"><el-option v-for="value in ['58', '80', 'A4']" :key="value" :label="value === 'A4' ? value : value + 'mm'" :value="value" /></el-select></el-form-item>
          <el-form-item label="份数"><el-input-number v-model="draft.copies" :min="1" :max="5" :precision="0" /></el-form-item>
        </template>
        <template v-else>
          <el-form-item label="打印机" required><el-select v-model="draft.printerId"><el-option v-for="printer in activePrinters" :key="printer.id" :label="printer.name" :value="printer.id" /></el-select></el-form-item>
          <el-form-item label="单据分类" required><el-select v-model="draft.documentType"><el-option v-for="(label, value) in DOCUMENT_TYPES" :key="value" :label="label" :value="value" /></el-select></el-form-item>
          <el-form-item label="配置启用"><el-switch v-model="draft.configuredEnabled" /><span class="switch-note">启用配置不代表已实际生效</span></el-form-item>
        </template>
      </el-form>
      <template #footer><el-button :disabled="!!state.busy" @click="dialogOpen = false">关闭</el-button><el-button type="primary" :loading="state.busy === 'save'" :disabled="!writable" @click="saveDraft">保存并回读</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'
import { PRINT_TYPES, DOCUMENT_TYPES, PRINT_NOTE, printScope, createRestaurantPrintActions } from '@/utils/restaurantPrint'

const userStore = useUserStore()
const identity = ref(null), gm = ref(false), selectedStore = ref(null), showArchived = ref(false)
const state = reactive({ sid: null, printers: [], rules: [], ready: false, busy: '', error: '', notice: '', expected: null, uncertain: false })
const actions = createRestaurantPrintActions(state, request)
const dialogOpen = ref(false), kind = ref('printers'), draft = ref({})
const writable = computed(() => !!identity.value && state.ready && !state.busy && !state.uncertain && !state.expected)
const activePrinters = computed(() => state.printers.filter(row => !row.archive))
const visiblePrinters = computed(() => state.printers.filter(row => showArchived.value || !row.archive))
const visibleRules = computed(() => state.rules.filter(row => showArchived.value || !row.archive))
let identityVersion = 0
function changeScope(sid) {
  dialogOpen.value = false
  actions.invalidate(sid)
  if (sid) void actions.load()
}
watch(selectedStore, value => { if (identity.value && gm.value) changeScope(printScope(identity.value, value).sid) }, { flush: 'sync' })
watch(() => userStore.storeId, value => { if (identity.value && gm.value) selectedStore.value = Number(value) > 0 ? Number(value) : null }, { flush: 'sync' })
watch(() => userStore.token, () => { identity.value = null; identityVersion++; changeScope(null); void authenticate() }, { flush: 'sync' })
async function authenticate() {
  const current = ++identityVersion
  try {
    const response = await request.get('/auth/me')
    if (current !== identityVersion) return
    if (response?.code !== 200) throw new Error(response?.message || '登录身份读取失败')
    const scope = printScope(response.data, localStorage.getItem('storeId'))
    identity.value = response.data; gm.value = scope.gm
    // Raw persisted choice avoids the shared store's fallback of 1 for a GM.
    if (scope.gm && selectedStore.value !== scope.sid) selectedStore.value = scope.sid
    else changeScope(scope.sid)
  } catch (error) { if (current === identityVersion) state.error = error.response?.data?.message || error.message }
}
async function refresh() { if (!identity.value) await authenticate(); else await actions.load() }
function openEditor(type, row) {
  if (!writable.value || row?.archive) return
  kind.value = type
  draft.value = row ? { ...row } : type === 'printers' ? { name: '', type: 'network', address: '', paperWidth: '80', copies: 1 } : { name: '', printerId: null, documentType: 'receipt', configuredEnabled: false }
  state.error = ''; dialogOpen.value = true
}
async function saveDraft() {
  const sid = state.sid, savedDraft = draft.value
  if (await actions.save(kind.value, savedDraft) && sid === state.sid && draft.value === savedDraft) dialogOpen.value = false
}
async function archiveRow(type, row) {
  await actions.save(type, row, true, async () => {
    await ElMessageBox.confirm(`归档“${row.name}”后保留历史且不可恢复。打印机须先归档关联规则。`, '确认归档', { type: 'warning', confirmButtonText: '归档', cancelButtonText: '取消' })
    return true
  })
}
function printerName(id) { return state.printers.find(row => row.id === id)?.name || `打印机 #${id}` }
onMounted(authenticate)
onBeforeUnmount(() => { identityVersion++; actions.invalidate(null) })
</script>

<style scoped>
.print-config-page { max-width: 1400px; min-width: 0; margin: 0 auto; color: var(--color-text, #26352e); }
.page-header, .section-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
h2 { font-size: 22px; margin: 0; } h3 { font-size: 17px; } h4 { margin: 0; overflow-wrap: anywhere; }
p { font-size: 13px; color: var(--color-text-secondary, #647168); line-height: 1.7; }
.toolbar { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; margin: 18px 0; font-size: 13px; }
.toolbar .el-input-number { width: 140px; }
.el-alert { margin: 12px 0; }
section { margin-top: 24px; min-width: 0; }
.printer-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 270px), 1fr)); gap: 16px; }
.printer-card { padding: 18px; min-width: 0; background: var(--color-card, #fff); border: 1px solid var(--color-border, #dce3de); border-radius: 12px; }
.address { overflow-wrap: anywhere; }
.actions { display: flex; flex-wrap: wrap; gap: 8px; } .actions .el-button { margin-left: 0; }
.table-wrapper { width: 100%; overflow-x: auto; }
.el-select { width: 100%; } .switch-note { margin-left: 10px; font-size: 12px; }
@media (max-width: 480px) { h2 { font-size: 20px; } .printer-card { padding: 14px; } .page-header { align-items: flex-start; } }
</style>
