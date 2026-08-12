<template>
  <section class="notice-page" v-loading="loading">
    <header class="hero">
      <div>
        <p class="eyebrow">BANQUET OPERATIONS</p>
        <h1>宴会通知单</h1>
        <p>从部门协同、签收回收，到纸质签字扫描归档的完整闭环</p>
      </div>
      <el-button type="primary" size="large" @click="openCreate">新建通知单</el-button>
    </header>

    <div class="summary-grid">
      <article v-for="item in summaries" :key="item.label" class="summary-card">
        <span>{{ item.label }}</span><strong>{{ item.value }}</strong><small>{{ item.hint }}</small>
      </article>
    </div>

    <div class="content-card">
      <div class="filters">
        <el-input v-model="filters.keyword" clearable placeholder="通知编号、客人或地点" @keyup.enter="loadData" />
        <el-select v-model="filters.status" clearable placeholder="全部状态">
          <el-option v-for="(text, value) in statusLabels" :key="value" :label="text" :value="value" />
        </el-select>
        <el-date-picker v-model="filters.range" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" />
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>

      <el-table :data="rows" stripe empty-text="暂无宴会通知单">
        <el-table-column prop="noticeNo" label="通知编号" min-width="172" />
        <el-table-column prop="banquetDate" label="日期" width="112" />
        <el-table-column prop="banquetTime" label="时间" width="90" />
        <el-table-column prop="location" label="地点" min-width="130" />
        <el-table-column prop="banquetType" label="宴会性质" width="120" />
        <el-table-column prop="reservedQuantity" label="预定数量" width="95" align="center" />
        <el-table-column prop="customerName" label="联系人" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><el-tag :type="statusType(row.status)">{{ statusLabels[row.status] || row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" min-width="310" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="view(row)">查看</el-button>
            <el-button v-if="row.status === 'draft'" link type="primary" @click="edit(row)">编辑</el-button>
            <el-button link @click="copyRow(row)">复制</el-button>
            <el-button link @click="printRow(row)">打印</el-button>
            <el-button v-if="nextStatus[row.status]" link type="success" @click="advance(row)">{{ transitionLabels[nextStatus[row.status]] }}</el-button>
            <el-button v-if="row.status !== 'draft' && row.status !== 'archived'" link @click="openScan(row)">扫描件</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="editorVisible" :title="form.id ? '编辑宴会通知单' : '新建宴会通知单'" width="860px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="宴会日期" prop="banquetDate"><el-date-picker v-model="form.banquetDate" value-format="YYYY-MM-DD" /></el-form-item>
          <el-form-item label="时间"><el-time-select v-model="form.banquetTime" start="08:00" step="00:30" end="23:30" /></el-form-item>
          <el-form-item label="宴会地点" prop="location"><el-input v-model.trim="form.location" maxlength="120" /></el-form-item>
          <el-form-item label="预定数量" prop="reservedQuantity"><el-input-number v-model="form.reservedQuantity" :min="1" :max="9999" /></el-form-item>
          <el-form-item label="宴会性质" prop="banquetType"><el-input v-model.trim="form.banquetType" maxlength="60" placeholder="如：婚宴、寿宴、商务宴" /></el-form-item>
          <el-form-item label="联系人"><el-input v-model.trim="form.customerName" maxlength="80" /></el-form-item>
          <el-form-item label="联系电话"><el-input v-model.trim="form.customerPhone" maxlength="30" /></el-form-item>
          <el-form-item label="关联预订号"><el-input v-model.trim="form.bookingId" maxlength="40" /></el-form-item>
        </div>
        <el-form-item label="菜单内容"><el-input v-model="form.menuContent" type="textarea" :rows="5" maxlength="8000" show-word-limit /></el-form-item>
        <div class="department-title"><strong>部门协同事项</strong><span>勾选任务并补充执行说明</span></div>
        <div class="department-grid">
          <article v-for="dept in form.departments" :key="dept.code" class="department-card">
            <div class="department-head"><el-checkbox v-model="dept.enabled">{{ dept.name }}</el-checkbox><el-tag v-if="dept.confirmed" type="success">已确认</el-tag></div>
            <el-input v-model="dept.note" type="textarea" :rows="2" :disabled="!dept.enabled" :placeholder="dept.placeholder" maxlength="500" />
          </article>
        </div>
      </el-form>
      <template #footer><el-button @click="editorVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存草稿</el-button></template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="宴会通知单详情" width="780px">
      <div v-if="selected" ref="printArea" class="print-sheet">
        <div class="print-title"><small>又见炊烟私房菜</small><h2>宴会通知单</h2><span>{{ selected.noticeNo }}</span></div>
        <div class="notice-meta"><span><b>日期：</b>{{ selected.banquetDate }} {{ selected.banquetTime }}</span><span><b>地点：</b>{{ selected.location }}</span><span><b>数量：</b>{{ selected.reservedQuantity }}</span><span><b>性质：</b>{{ selected.banquetType }}</span><span><b>联系人：</b>{{ selected.customerName || '—' }} {{ selected.customerPhone || '' }}</span></div>
        <section><h3>菜单内容</h3><p class="preline">{{ selected.menuContent || '未填写' }}</p></section>
        <section><h3>部门执行事项</h3><table class="print-table"><thead><tr><th>部门</th><th>事项与说明</th><th>负责人签字</th></tr></thead><tbody><tr v-for="dept in selectedDepartments" :key="dept.code"><td>{{ dept.name }}</td><td>{{ dept.note || '按宴会标准执行' }}</td><td></td></tr></tbody></table></section>
        <footer class="print-footer"><span>总经理办公室回收：________________</span><span>打印时间：{{ printTime }}</span></footer>
      </div>
      <template #footer><el-button @click="detailVisible = false">关闭</el-button><el-button type="primary" @click="printCurrent">打印 A4</el-button></template>
    </el-dialog>

    <el-dialog v-model="scanVisible" title="上传纸质签字扫描件" width="520px">
      <el-upload drag :http-request="uploadScan" :show-file-list="false" accept="image/jpeg,image/png,image/webp">
        <div class="upload-copy"><strong>点击或拖入扫描图片</strong><span>支持 JPG、PNG、WebP，单个不超过 10MB</span></div>
      </el-upload>
      <el-alert v-if="scanForm.url" type="success" :closable="false" :title="`已上传：${scanForm.name}`" />
      <template #footer><el-button @click="scanVisible = false">取消</el-button><el-button type="primary" :disabled="!scanForm.url" :loading="saving" @click="saveScan">关联扫描件</el-button></template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { attachBanquetNoticeScan, copyBanquetNotice, createBanquetNotice, listBanquetNotices, transitionBanquetNotice, updateBanquetNotice } from '@/api/banquetNotice'

const statusLabels = { draft: '草稿', published: '已发布', confirmed: '部门已确认', returned: '已回收', archived: '已归档' }
const nextStatus = { draft: 'published', published: 'confirmed', confirmed: 'returned', returned: 'archived' }
const transitionLabels = { published: '发布', confirmed: '确认完成', returned: '确认回收', archived: '归档' }
const departmentDefaults = [
  ['art', '美工', '条幅、指引牌、桌卡、大屏内容与摆放位置'], ['engineering', '工程', '场地布置、音响、灯光、空调及电力保障'],
  ['security', '安保', '车辆引导、秩序、消防通道及重点区域值守'], ['kitchen', '厨房', '上菜时间、特殊菜品、忌口及出品节奏'],
  ['cashier', '收银', '押金、折扣授权、结算方式与发票要求'], ['front', '前厅', '桌型、宝宝椅、新娘房、迎宾及特殊服务']
]
const blankDepartments = () => departmentDefaults.map(([code, name, placeholder]) => ({ code, name, placeholder, enabled: false, note: '', confirmed: false }))
const blankForm = () => ({ id: null, storeId: Number(localStorage.getItem('currentStoreId') || localStorage.getItem('storeId') || 1), bookingId: '', banquetDate: '', banquetTime: '18:00', location: '', reservedQuantity: 1, banquetType: '', customerName: '', customerPhone: '', menuContent: '', departments: blankDepartments() })
const loading = ref(false), saving = ref(false), editorVisible = ref(false), detailVisible = ref(false), scanVisible = ref(false)
const rows = ref([]), selected = ref(null), formRef = ref(), printArea = ref()
const filters = reactive({ keyword: '', status: '', range: [] }), form = reactive(blankForm()), scanForm = reactive({ id: null, url: '', name: '' })
const rules = { banquetDate: [{ required: true, message: '请选择宴会日期', trigger: 'change' }], location: [{ required: true, message: '请输入宴会地点', trigger: 'blur' }], reservedQuantity: [{ required: true, message: '请输入预定数量', trigger: 'change' }], banquetType: [{ required: true, message: '请输入宴会性质', trigger: 'blur' }] }
const summaries = computed(() => [{ label: '全部通知', value: rows.value.length, hint: '当前查询范围' }, { label: '待部门确认', value: rows.value.filter(i => i.status === 'published').length, hint: '请及时跟进' }, { label: '待扫描归档', value: rows.value.filter(i => i.status === 'returned').length, hint: '纸质签字件' }, { label: '已归档', value: rows.value.filter(i => i.status === 'archived').length, hint: '可追溯查询' }])
const selectedDepartments = computed(() => parseDepartments(selected.value?.departmentItems).filter(i => i.enabled))
const printTime = computed(() => new Date().toLocaleString('zh-CN'))
const parseDepartments = (value) => { try { return JSON.parse(value || '[]') } catch { return [] } }
const statusType = (status) => ({ draft: 'info', published: 'warning', confirmed: 'primary', returned: 'success', archived: 'success' }[status] || 'info')

async function loadData() { loading.value = true; try { const res = await listBanquetNotices({ keyword: filters.keyword || undefined, status: filters.status || undefined, startDate: filters.range?.[0], endDate: filters.range?.[1] }); rows.value = res.data || [] } finally { loading.value = false } }
function resetFilters() { Object.assign(filters, { keyword: '', status: '', range: [] }); loadData() }
function setForm(data = blankForm()) { Object.assign(form, blankForm(), data); form.departments = data.departmentItems ? mergeDepartments(parseDepartments(data.departmentItems)) : blankDepartments() }
function mergeDepartments(saved) { return blankDepartments().map(base => ({ ...base, ...(saved.find(i => i.code === base.code) || {}) })) }
function openCreate() { setForm(); editorVisible.value = true }
function edit(row) { setForm(row); editorVisible.value = true }
function view(row) { selected.value = row; detailVisible.value = true }
async function save() { await formRef.value.validate(); const enabled = form.departments.filter(i => i.enabled); if (!enabled.length) return ElMessage.warning('至少勾选一个部门事项'); saving.value = true; try { const payload = { ...form, departmentItems: JSON.stringify(form.departments) }; delete payload.departments; form.id ? await updateBanquetNotice(form.id, payload) : await createBanquetNotice(payload); ElMessage.success('草稿已保存'); editorVisible.value = false; await loadData() } finally { saving.value = false } }
async function copyRow(row) { await ElMessageBox.confirm('将复制为新的草稿通知单，是否继续？', '复制通知单'); await copyBanquetNotice(row.id, { storeId: row.storeId }); ElMessage.success('已复制为新草稿'); loadData() }
async function advance(row) { const target = nextStatus[row.status]; await ElMessageBox.confirm(`确认执行“${transitionLabels[target]}”？此操作将记录到业务流程。`, '流程确认', { type: 'warning' }); await transitionBanquetNotice(row.id, { storeId: row.storeId, status: target }); ElMessage.success('流程状态已更新'); loadData() }
function printRow(row) { view(row); nextTick(printCurrent) }
function printCurrent() { window.print() }
function openScan(row) { Object.assign(scanForm, { id: row.id, storeId: row.storeId, url: row.scanUrl || '', name: row.scanName || '' }); scanVisible.value = true }
async function uploadScan(options) { const body = new FormData(); body.append('file', options.file); const res = await request({ url: '/upload/image', method: 'post', data: body, headers: { 'Content-Type': 'multipart/form-data' } }); scanForm.url = res.data.url; scanForm.name = res.data.original_name; ElMessage.success('扫描件上传成功') }
async function saveScan() { saving.value = true; try { await attachBanquetNoticeScan(scanForm.id, scanForm); ElMessage.success('扫描件已关联'); scanVisible.value = false; loadData() } finally { saving.value = false } }
onMounted(loadData)
</script>

<style scoped>
.notice-page{padding:28px 32px;color:#20352b}.hero{display:flex;align-items:flex-end;justify-content:space-between;padding:28px 32px;margin-bottom:18px;color:#f8faf8;background:#25483a;border-radius:14px}.eyebrow{margin:0 0 8px;color:#d7b66f;font-size:12px;letter-spacing:.18em}.hero h1{margin:0;font-size:30px}.hero p:last-child{margin:8px 0 0;color:#d5e0da}.summary-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:14px;margin-bottom:18px}.summary-card,.content-card{background:#fff;border:1px solid #e3e9e5;border-radius:12px}.summary-card{display:flex;flex-direction:column;padding:18px}.summary-card span,.summary-card small{color:#718078}.summary-card strong{margin:4px 0;font-size:28px}.content-card{padding:18px}.filters{display:flex;gap:10px;margin-bottom:18px}.filters .el-input{max-width:260px}.filters .el-select{width:160px}.form-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:0 14px}.department-title{display:flex;align-items:center;justify-content:space-between;margin:4px 0 12px}.department-title span{color:#718078;font-size:13px}.department-grid{display:grid;grid-template-columns:repeat(2,1fr);gap:12px}.department-card{padding:14px;background:#f7f9f7;border:1px solid #e3e9e5;border-radius:10px}.department-head{display:flex;justify-content:space-between;margin-bottom:10px}.print-sheet{color:#111;background:#fff}.print-title{text-align:center;border-bottom:2px solid #111;padding-bottom:12px}.print-title small,.print-title span{display:block}.print-title h2{margin:6px 0;font-size:28px;letter-spacing:.25em}.notice-meta{display:grid;grid-template-columns:repeat(2,1fr);gap:10px;padding:16px 0}.print-sheet h3{font-size:16px}.preline{white-space:pre-line;line-height:1.8}.print-table{width:100%;border-collapse:collapse}.print-table th,.print-table td{padding:10px;border:1px solid #222;text-align:left}.print-table td:last-child{width:120px;height:42px}.print-footer{display:flex;justify-content:space-between;margin-top:24px;font-size:12px}.upload-copy{display:flex;flex-direction:column;gap:8px;color:#718078}.upload-copy strong{color:#20352b}@media(max-width:900px){.notice-page{padding:16px}.summary-grid,.form-grid,.department-grid{grid-template-columns:1fr 1fr}.filters{flex-wrap:wrap}.hero{align-items:flex-start;gap:16px}}@media(max-width:560px){.hero{flex-direction:column}.summary-grid,.form-grid,.department-grid{grid-template-columns:1fr}}@media print{.notice-page>*:not(.el-overlay){display:none!important}:global(body *){visibility:hidden}.print-sheet,.print-sheet *{visibility:visible}.print-sheet{position:fixed;inset:0;padding:12mm;font-size:12pt}.print-title h2{font-size:22pt}:global(.el-dialog__header),:global(.el-dialog__footer){display:none!important}}
</style>
