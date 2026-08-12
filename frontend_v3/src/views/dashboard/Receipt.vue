<template>
  <div class="page">
    <div class="page-header">
      <h2>入库验收 · Receipt Management</h2>
      <p class="page-desc">采购入库登记 · 验收确认</p>
    </div>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-input v-model="searchKw" placeholder="搜索入库单/材料" class="search-box" clearable @keyup.enter="loadData" />
        <el-select v-model="statusFilter" placeholder="状态" clearable class="sel-box" @change="loadData">
          <el-option label="待入库" value="0" />
          <el-option label="已入库" value="1" />
          <el-option label="已关闭" value="2" />
        </el-select>
        <el-date-picker v-model="dateRange" type="daterange" range-separator="至"
          start-placeholder="开始日期" end-placeholder="结束日期"
          value-format="YYYY-MM-DD" class="sel-box" @change="loadData" />
      </div>
      <div class="toolbar-right">
        <el-button type="primary" @click="openAddDialog()">+ 新增入库单</el-button>
        <el-button @click="loadData">刷新</el-button>
      </div>
    </div>

    <el-table :data="tableList" stripe class="data-table" v-loading="loading" @selection-change="rows=>selectedRows=rows">
      <el-table-column type="selection" width="50" />
      <el-table-column prop="id" label="单号" width="90" />
      <el-table-column prop="materialName" label="材料名称" min-width="160" />
      <el-table-column prop="category" label="种类" width="100" />
      <el-table-column prop="specification" label="规格" width="110" />
      <el-table-column prop="stock" label="数量" width="80" />
      <el-table-column prop="price" label="单价" width="90">
        <template #default="{ row }">¥{{ row.price || '0' }}</template>
      </el-table-column>
      <el-table-column label="金额" width="100">
        <template #default="{ row }">¥{{ ((row.stock||0)*(row.price||0)).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column prop="supplierName" label="供应商" width="120" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag v-if="String(row.status)==='0'" type="warning" size="small">待入库</el-tag>
          <el-tag v-else-if="String(row.status)==='1'" type="success" size="small">已入库</el-tag>
          <el-tag v-else-if="String(row.status)==='2'" type="danger" size="small">已关闭</el-tag>
          <el-tag v-else type="info" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="inTime" label="入库时间" width="160" />
      <el-table-column prop="storeId" label="门店" width="70" />
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button v-if="String(row.status)==='0'" link size="small" type="success" @click="confirmReceive(row)">验收</el-button>
          <el-button link size="small" type="primary" @click="openAddDialog(row)">编辑</el-button>
          <el-popconfirm title="确认删除?" @confirm="removeRow(row)">
            <template #reference><el-button link size="small" type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      v-model:page-size="limit"
      :total="total"
      @size-change="handleSizeChange"
      @current-change="onPageChange"
      layout="total, sizes, prev, pager, next, jumper"
      class="pgn"
      background />

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="addDialogVisible" :title="dialogEditing?'编辑入库单':'新增入库单'" width="720px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="材料名称" required>
              <el-select v-model="form.materialName" filterable allow-create style="width:100%" placeholder="选择或输入">
                <el-option v-for="m in materialChoices" :key="m.id" :label="m.materialName" :value="m.materialName" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="种类">
              <el-select v-model="form.category" filterable allow-create style="width:100%">
                <el-option v-for="c in categoryChoices" :key="c.id" :label="c.categoryName" :value="c.categoryName" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="规格"><el-input v-model="form.specification" placeholder="如：500g/袋" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="入库数量" required><el-input-number v-model="form.stock" :min="0" :precision="2" style="width:100%" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单价(元)"><el-input-number v-model="form.price" :min="0" :precision="2" :step="0.5" style="width:100%" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="供应商">
              <el-select v-model="form.supplierAccount" filterable style="width:100%" placeholder="选择供应商" @change="onSupplierPick">
                <el-option v-for="s in supplierChoices" :key="s.id" :label="s.supplierName + '(' + (s.supplierAccount||'') + ')'" :value="s.supplierAccount" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供应商名称"><el-input v-model="form.supplierName" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width:100%">
                <el-option label="待入库" :value="0" />
                <el-option label="已入库" :value="1" />
                <el-option label="已关闭" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="门店ID"><el-input v-model="form.storeId" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" placeholder="批次、运输、质检等备注" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible=false">取消</el-button>
        <el-button type="primary" @click="saveForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 验收确认弹窗 -->
    <el-dialog v-model="receiveVisible" title="验收入库" width="480px" destroy-on-close>
      <el-form label-width="90px" :model="receiveForm">
        <el-descriptions :column="1" border size="small" style="margin-bottom:16px">
          <el-descriptions-item label="材料">{{ receiveRow.materialName }}</el-descriptions-item>
          <el-descriptions-item label="种类">{{ receiveRow.category }}</el-descriptions-item>
          <el-descriptions-item label="规格/数量">{{ receiveRow.specification || '-' }} · {{ receiveRow.stock }}</el-descriptions-item>
          <el-descriptions-item label="供应商">{{ receiveRow.supplierName || receiveRow.supplierAccount || '-' }}</el-descriptions-item>
        </el-descriptions>
        <el-form-item label="实收数量"><el-input-number v-model="receiveForm.actualQty" :min="0" :precision="2" style="width:100%" /></el-form-item>
        <el-form-item label="验收人"><el-input v-model="receiveForm.operator" placeholder="签收人姓名" /></el-form-item>
        <el-form-item label="验收备注"><el-input v-model="receiveForm.note" type="textarea" :rows="2" placeholder="质检、破损、短少等" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="receiveVisible=false">取消</el-button>
        <el-button type="success" @click="submitReceive">确认入库</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const currentStoreId = computed(() => userStore.currentStore?.storeId || userStore.stores?.[0]?.storeId || 1)

const loading = ref(false)
const searchKw = ref('')
const statusFilter = ref('')
const dateRange = ref([])
const page = ref(1)
const limit = ref(15)
const total = ref(0)
const tableList = ref([])
const selectedRows = ref([])

const categoryChoices = ref([])
const materialChoices = ref([])
const supplierChoices = ref([])

const addDialogVisible = ref(false)
const dialogEditing = ref(false)
const form = ref({
  id: null, materialName: '', category: '', specification: '', stock: 0, price: 0,
  supplierAccount: '', supplierName: '', inTime: null, remark: '', status: 0, storeId: null
})

const receiveVisible = ref(false)
const receiveRow = ref({})
const receiveForm = ref({ actualQty: 0, operator: '', note: '' })

async function loadBasics() {
  try {
    const [cr, mr, sr] = await Promise.all([
      request.get('/api/purchase/material-category/page', { params: { storeId: currentStoreId.value, page: 1, limit: 500 } }),
      request.get('/api/purchase/material-info/page', { params: { storeId: currentStoreId.value, page: 1, limit: 500 } }),
      request.get('/api/purchase/supplier/page', { params: { storeId: currentStoreId.value, page: 1, limit: 500 } })
    ])
    categoryChoices.value = (cr.data || cr).data || []
    materialChoices.value = (mr.data || mr).data || []
    supplierChoices.value = (sr.data || sr).data || []
  } catch (e) { console.error(e) }
}

async function loadData() {
  loading.value = true
  try {
    const params = {
      storeId: currentStoreId.value,
      page: page.value,
      limit: limit.value
    }
    if (statusFilter.value !== '' && statusFilter.value != null) params.status = statusFilter.value
    const res = await request.get('/api/purchase/purchase-in/page', { params })
    const d = res.data || res
    let list = d.data || []
    if (searchKw.value) {
      const kw = String(searchKw.value).toLowerCase()
      list = list.filter(x =>
        String(x.materialName||'').toLowerCase().includes(kw) ||
        String(x.id||'').includes(kw) ||
        String(x.supplierName||'').toLowerCase().includes(kw)
      )
    }
    if (dateRange.value && dateRange.value.length === 2) {
      const [from, to] = dateRange.value
      list = list.filter(x => {
        const t = x.inTime ? String(x.inTime).slice(0,10) : ''
        return t && t >= from && t <= to + ' 23:59:59'.slice(0,0)
      })
    }
    total.value = list.length < limit.value ? ((page.value - 1) * limit.value + list.length) : d.total || list.length
    tableList.value = list
  } catch (e) { console.error(e); ElMessage.error('加载入库单失败') } finally { loading.value = false }
}

function handleSizeChange(val) { limit.value = val; page.value = 1; loadData() }
function onPageChange(p) { page.value = p; loadData() }

function openAddDialog(row) {
  if (row) {
    dialogEditing.value = true
    form.value = { ...row }
    if (form.value.status == null) form.value.status = 0
  } else {
    dialogEditing.value = false
    form.value = {
      id: null, materialName: '', category: '', specification: '', stock: 0, price: 0,
      supplierAccount: '', supplierName: '', inTime: null, remark: '', status: 0, storeId: currentStoreId.value
    }
  }
  addDialogVisible.value = true
}

function onSupplierPick(acc) {
  const s = supplierChoices.value.find(x => x.supplierAccount === acc)
  if (s && !form.value.supplierName) form.value.supplierName = s.supplierName
}

async function saveForm() {
  if (!form.value.materialName) { ElMessage.warning('请输入材料名称'); return }
  if (form.value.stock == null || Number(form.value.stock) < 0) { ElMessage.warning('请填写入库数量'); return }
  try {
    const payload = { ...form.value }
    delete payload.inTime
    const url = dialogEditing.value ? '/api/purchase/purchase-in/update' : '/api/purchase/purchase-in/save'
    const res = await request.post(url, payload)
    const d = res.data || res
    if (d.code === 0) {
      ElMessage.success('保存成功')
      addDialogVisible.value = false
      loadData()
    } else {
      ElMessage.error(d.msg || '保存失败')
    }
  } catch (e) { ElMessage.error('保存失败') }
}

async function removeRow(row) {
  try {
    const res = await request.post('/api/purchase/purchase-in/delete', [row.id])
    const d = res.data || res
    if (d.code === 0) { ElMessage.success('已删除'); loadData() }
  } catch (e) { ElMessage.error('删除失败') }
}

function confirmReceive(row) {
  receiveRow.value = { ...row }
  receiveForm.value = { actualQty: row.stock || 0, operator: userStore.userInfo?.name || userStore.userInfo?.username || '', note: '' }
  receiveVisible.value = true
}

async function submitReceive() {
  try {
    const nowTime = new Date().toISOString().slice(0,19).replace('T',' ')
    const payload = {
      ...receiveRow.value,
      status: 1,
      stock: receiveForm.value.actualQty != null ? receiveForm.value.actualQty : receiveRow.value.stock,
      inTime: null,
      remark: [receiveRow.value.remark || '', receiveForm.value.note ? ('验收：' + receiveForm.value.note + ' 签收人：' + (receiveForm.value.operator||'—')) : ''].filter(Boolean).join('；')
    }
    const res = await request.post('/api/purchase/purchase-in/update', payload)
    const d = res.data || res
    if (d.code === 0) {
      ElMessage.success('验收入库成功')
      receiveVisible.value = false
      loadData()
    } else {
      ElMessage.error(d.msg || '操作失败')
    }
  } catch (e) { ElMessage.error('操作失败') }
}

onMounted(() => {
  loadBasics().then(loadData)
})
</script>

<style scoped>
.page { width:100%; }
.page-header { display:flex; align-items:center; gap:12px; margin-bottom:12px; }
.page-header h2 { font-size:18px; font-weight:600; margin:0; }
.page-desc { font-size:13px; color:#64748b; margin:0; }
.toolbar { display:flex; justify-content:space-between; align-items:center; margin-bottom:12px; gap:10px; flex-wrap:wrap; }
.toolbar-left, .toolbar-right { display:flex; gap:8px; align-items:center; flex-wrap:wrap; }
.search-box { width:220px; }
.sel-box { width:200px; }
.data-table { width:100%; }
.pgn { margin-top:16px; justify-content:flex-end; }
</style>
