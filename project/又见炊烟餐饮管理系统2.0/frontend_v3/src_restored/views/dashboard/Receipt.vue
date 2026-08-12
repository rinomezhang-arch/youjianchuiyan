<template>
  <div class="page">
    <div class="page-header">
      <h2>入库验收 · Receipt</h2>
      <p class="page-desc">采购收货 · 验收登记 · 入库确认</p>
    </div>
    <div class="toolbar">
      <div class="toolbar-left">
        <el-select v-model="status" placeholder="状态" style="width:120px" @change="fetchData">
          <el-option label="全部" value="" />
          <el-option label="待验收" :value="0" />
          <el-option label="已入库" :value="1" />
        </el-select>
        <el-select v-model="supplierId" placeholder="选择供应商" style="width:180px" filterable @change="fetchData">
          <el-option v-for="s in suppliers" :key="s.supplier_id" :label="s.supplier_name" :value="s.supplier_id" />
        </el-select>
        <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" style="width:240px" />
        <el-input v-model="keyword" placeholder="搜索单号/原料" class="search-box" clearable @keyup.enter="fetchData" />
        <el-button type="primary" @click="fetchData">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>
      <div class="toolbar-right">
        <el-button type="success" @click="openAddDialog">+ 录入入库</el-button>
        <el-button type="warning" @click="exportData">导出</el-button>
      </div>
    </div>
    <el-table :data="list" stripe v-loading="loading">
      <el-table-column prop="receipt_no" label="入库单号" width="150" />
      <el-table-column prop="delivery_no" label="送货单号" width="150" />
      <el-table-column prop="supplier_name" label="供应商" width="150" />
      <el-table-column prop="item_count" label="品种数" width="80" align="center" />
      <el-table-column prop="total_amount" label="总金额" width="110">
        <template #default="{ row }">¥{{ row.total_amount }}</template>
      </el-table-column>
      <el-table-column prop="buyer" label="采购员" width="90" />
      <el-table-column prop="receiver" label="收货员" width="90" />
      <el-table-column prop="receipt_date" label="收货日期" width="110">
        <template #default="{ row }">{{ formatDate(row.receipt_date) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.status===0" type="warning" size="small">待验收</el-tag>
          <el-tag v-else type="success" size="small">已入库</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="viewDetail(row)">查看</el-button>
          <el-button v-if="row.status===0" size="small" type="success" @click="auditRow(row)">验收入库</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-if="total > pageSize" layout="total, prev, pager, next" :total="total" :current-page="page" :page-size="pageSize" @current-change="onPageChange" style="margin-top:12px;justify-content:flex-end;display:flex" />

    <el-dialog v-model="showAddDialog" title="录入入库单" width="800px" @close="closeAddDialog">
      <el-form :model="addForm" label-width="90px" label-position="right">
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="供应商">
              <el-select v-model="addForm.supplier_id" filterable placeholder="请选择" style="width:100%" @change="onSupplierChange">
                <el-option v-for="s in suppliers" :key="s.supplier_id" :label="s.supplier_name" :value="s.supplier_id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="关联采购单">
              <el-select v-model="addForm.order_no" filterable placeholder="可选" style="width:100%">
                <el-option v-for="o in pendingOrders" :key="o.order_no" :label="o.order_no" :value="o.order_no" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="送货单号">
              <el-input v-model="addForm.delivery_no" placeholder="请输入" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="入库明细">
          <el-table :data="addForm.items" border stripe size="small">
            <el-table-column label="原料" width="200">
              <template #default="{ row }">
                <el-select v-model="row.material_id" filterable size="small" placeholder="选择原料" style="width:100%" @change="onMaterialChange(row)">
                  <el-option v-for="m in materialList" :key="m.material_id" :label="m.material_name + '(' + m.material_id + ')'" :value="m.material_id" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="单位" width="70">
              <template #default="{ row }">{{ row.unit }}</template>
            </el-table-column>
            <el-table-column label="数量" width="100">
              <template #default="{ row }">
                <el-input-number v-model="row.quantity" :min="0" :step="1" size="small" style="width:100%" />
              </template>
            </el-table-column>
            <el-table-column label="单价" width="140">
              <template #default="{ row }">
                <el-input-number v-model="row.price" :min="0" :step="0.01" :precision="4" size="small" style="width:100%" />
                <div v-if="row.last_price" style="font-size:11px;color:#C4A35A;margin-top:2px;cursor:pointer" @click="showPriceHistory(row)">
                  最近入库: ¥{{ row.last_price }} (点击查看历史)
                </div>
              </template>
            </el-table-column>
            <el-table-column label="金额" width="90" align="right">
              <template #default="{ row }">¥{{ (row.quantity * row.price).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="60" align="center">
              <template #default="{ $index }">
                <el-button type="danger" text size="small" @click="removeItem($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div style="margin-top:8px;text-align:right">
            <el-button type="primary" size="small" @click="addItem">+ 添加原料</el-button>
          </div>
        </el-form-item>
        <el-form-item label="合计金额" label-width="90px">
          <span style="font-size:18px;font-weight:700;color:#C25555">¥{{ totalAddAmount.toFixed(2) }}</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="addForm.remark" type="textarea" :rows="2" placeholder="备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="submitAdd">提交入库</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showDetailDialog" title="入库单详情" width="900px">
      <div v-if="detailData" class="detail-view">
        <div style="display:flex;gap:24px;flex-wrap:wrap;margin-bottom:16px;padding:12px;background:#fafafa;border-radius:4px">
          <div><b>入库单号:</b> {{ detailData.head?.receipt_no }}</div>
          <div><b>送货单号:</b> {{ detailData.head?.delivery_no || '-' }}</div>
          <div><b>供应商:</b> {{ detailData.head?.supplier_name }}</div>
          <div><b>日期:</b> {{ formatDate(detailData.head?.receipt_date) }}</div>
          <div><b>采购员:</b> {{ detailData.head?.buyer }}</div>
          <div><b>收货员:</b> {{ detailData.head?.receiver }}</div>
        </div>
        <el-table :data="detailData.details" border stripe size="small">
          <el-table-column prop="material_id" label="原料编码" width="120" />
          <el-table-column prop="material_name" label="原料名称" width="160" />
          <el-table-column prop="unit" label="单位" width="60" />
          <el-table-column prop="quantity" label="数量" width="90" align="right" />
          <el-table-column prop="price" label="单价" width="100" align="right">
            <template #default="{ row }">¥{{ row.price }}</template>
          </el-table-column>
          <el-table-column prop="amount" label="金额" width="100" align="right">
            <template #default="{ row }">¥{{ row.amount }}</template>
          </el-table-column>
          <el-table-column label="最近入库价" width="110" align="right">
            <template #default="{ row }">
              <span v-if="row.latest_price">¥{{ row.latest_price }}</span>
              <span v-else style="color:#999">-</span>
            </template>
          </el-table-column>
          <el-table-column label="采购单单价" width="100" align="right">
            <template #default="{ row }">
              <span v-if="row.last_order_price">¥{{ row.last_order_price }}</span>
              <span v-else style="color:#999">-</span>
            </template>
          </el-table-column>
        </el-table>
        <div style="text-align:right;margin-top:12px;font-size:16px">
          合计金额: <b style="color:#C25555;font-size:18px">¥{{ detailData.head?.total_amount }}</b>
        </div>
        <div v-if="detailData.head?.remark" style="margin-top:12px;padding:8px;background:#fff7e6;border-radius:4px">
          <b>备注:</b> {{ detailData.head.remark }}
        </div>
      </div>
    </el-dialog>
    <el-dialog v-model="showPriceDialog" :title="currentMaterialName + ' - 最近价格历史'" width="600px">
      <el-table :data="currentPriceHistory" border stripe size="small">
        <el-table-column prop="receipt_no" label="入库单号" width="140" />
        <el-table-column label="入库日期" width="110">
          <template #default="{ row }">{{ formatDate(row.receipt_date) }}</template>
        </el-table-column>
        <el-table-column prop="supplier_name" label="供应商" width="140" />
        <el-table-column prop="price" label="单价" width="100" align="right">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="80" align="right" />
      </el-table>
      <div v-if="currentPriceHistory.length === 0" style="text-align:center;padding:20px;color:#999">
        暂无历史价格记录
      </div>
      <template #footer>
        <el-button type="primary" @click="showPriceDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const loading = ref(false); const list = ref([]); const total = ref(0); const page = ref(1); const pageSize = ref(20)
const keyword = ref(''); const status = ref(''); const supplierId = ref(''); const dateRange = ref([])
const suppliers = ref([]); const materialList = ref([]); const pendingOrders = ref([])
const showAddDialog = ref(false); const showDetailDialog = ref(false); const detailData = ref(null)
const showPriceDialog = ref(false); const currentPriceHistory = ref([]); const currentMaterialName = ref('')
const addForm = ref({ supplier_id: '', order_no: '', delivery_no: '', items: [], remark: '' })

function formatDate(date) { if (!date) return '-'; return String(date).slice(0, 10) }

const totalAddAmount = computed(() => {
  return addForm.value.items.reduce((sum, item) => sum + (item.quantity || 0) * (item.price || 0), 0)
})

async function fetchSuppliers() {
  try {
    const res = await fetch('/menu-api/suppliers', { method: 'GET' })
    const d = await res.json()
    if (d.code === 200) suppliers.value = (d.data || []).map(s => ({
      supplier_id: s.supplierId,
      supplier_name: s.supplierName
    }))
  } catch (e) { console.error(e) }
}

async function fetchMaterials() {
  try {
    const res = await fetch('/menu-api/ingredients?pageSize=500', { method: 'GET' })
    const d = await res.json()
    if (d.code === 200) materialList.value = (d.data || []).map(m => ({
      material_id: m.ingredientId,
      material_name: m.ingredientName,
      purchase_unit: m.purchaseUnit,
      minor_unit: m.minorUnit,
      latest_price: m.latestPrice
    }))
  } catch (e) { console.error(e) }
}

const allOrders = ref([])

async function fetchPendingOrders() {
  try {
    const res = await fetch('/menu-api/purchases?status=pending&pageSize=200', { method: 'GET' })
    const d = await res.json()
    if (d.code === 200) {
      const orderSet = new Set()
      allOrders.value = (d.data || []).filter(item => {
        if (orderSet.has(item.orderNo)) return false
        orderSet.add(item.orderNo)
        return true
      }).map(item => ({ order_no: item.orderNo, supplier_name: item.supplierName }))
      pendingOrders.value = [...allOrders.value]
    }
  } catch (e) { console.error(e) }
}

async function fetchData() {
  loading.value = true
  try {
    const params = new URLSearchParams()
    if (keyword.value) params.set('keyword', keyword.value)
    if (supplierId.value) params.set('supplierId', supplierId.value)
    if (status.value !== '') params.set('status', status.value)
    if (dateRange.value.length === 2) params.set('dateRange', dateRange.value)
    params.set('page', page.value)
    params.set('pageSize', pageSize.value)
    const res = await fetch('/menu-api/receipts?' + params.toString(), { method: 'GET' })
    const d = await res.json()
    if (d.code === 200) { list.value = d.data || []; total.value = d.total || 0 }
  } catch (e) { console.error(e) } finally { loading.value = false }
}

function resetSearch() {
  keyword.value = ''; status.value = ''; supplierId.value = ''; dateRange.value = []; page.value = 1
  fetchData()
}

function onPageChange(p) { page.value = p; fetchData() }

function openAddDialog() {
  addForm.value = { supplier_id: '', order_no: '', delivery_no: '', items: [emptyItem()], remark: '' }
  showAddDialog.value = true
}

function emptyItem() {
  return { material_id: '', material_name: '', unit: '', quantity: 1, price: 0, last_price: 0 }
}

function addItem() { addForm.value.items.push(emptyItem()) }
function removeItem(index) { addForm.value.items.splice(index, 1) }

function onSupplierChange(sid) {
  const s = suppliers.value.find(x => x.supplier_id === sid)
  if (s) {
    pendingOrders.value = allOrders.value.filter(o => o.supplier_name === s.supplier_name)
  } else {
    pendingOrders.value = [...allOrders.value]
  }
}

async function fetchPriceHistory(materialId) {
  try {
    const res = await fetch(`/menu-api/receipts/price-history/${materialId}`, { method: 'GET' })
    const d = await res.json()
    if (d.code === 200) return d.data || []
    return []
  } catch (e) { return [] }
}

async function onMaterialChange(row) {
  const m = materialList.value.find(x => x.material_id === row.material_id)
  if (m) {
    row.material_name = m.material_name
    row.unit = m.purchase_unit || m.minor_unit || '克'
    row.price = m.latest_price || 0
    row.last_price = m.latest_price || 0
    const history = await fetchPriceHistory(row.material_id)
    if (history.length > 0) {
      row.price_history = history
      if (history[0]?.price) {
        row.last_price = history[0].price
      }
    }
  }
}

function showPriceHistory(row) {
  currentMaterialName.value = row.material_name || '原料'
  currentPriceHistory.value = row.price_history || []
  showPriceDialog.value = true
}

async function closeAddDialog() {
  showAddDialog.value = false
}

async function submitAdd() {
  if (!addForm.value.supplier_id) { ElMessage.warning('请选择供应商'); return }
  if (addForm.value.items.length === 0) { ElMessage.warning('请添加入库原料'); return }
  const validItems = addForm.value.items.filter(i => i.material_id && i.quantity > 0)
  if (validItems.length === 0) { ElMessage.warning('请填写有效的原料信息'); return }
  try {
    const res = await fetch('/menu-api/receipts', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ...addForm.value, items: validItems })
    })
    const d = await res.json()
    if (d.code === 200) {
      ElMessage.success('入库单创建成功')
      showAddDialog.value = false
      fetchData()
    } else { ElMessage.error(d.message || '创建失败') }
  } catch (e) { ElMessage.error('创建失败') }
}

async function viewDetail(row) {
  try {
    const res = await fetch(`/menu-api/receipts/${row.receipt_no}/detail`, { method: 'GET' })
    const d = await res.json()
    if (d.code === 200) { detailData.value = d.data; showDetailDialog.value = true }
  } catch (e) { console.error(e) }
}

function auditRow(row) {
  ElMessageBox.confirm(`确认入库单 "${row.receipt_no}" 验收入库? 入库后将更新库存和成本。`, '确认验收', {
    confirmButtonText: '确认入库', cancelButtonText: '取消', type: 'warning'
  }).then(async () => {
    try {
      const res = await fetch(`/menu-api/receipts/${row.receipt_no}/audit`, { method: 'POST' })
      const d = await res.json()
      if (d.code === 200) { ElMessage.success('验收入库成功'); fetchData() }
      else ElMessage.error(d.message || '操作失败')
    } catch (e) { ElMessage.error('操作失败') }
  })
}

function exportData() {
  ElMessage.info('导出功能开发中')
}

onMounted(() => {
  fetchSuppliers()
  fetchMaterials()
  fetchPendingOrders()
  fetchData()
  if (route.query.action === 'new') {
    openAddDialog()
  }
})

watch(() => route.query.action, (newVal) => {
  if (newVal === 'new') {
    openAddDialog()
  }
})
</script>

<style scoped>
.page { width:100%; }
.page-header { display:flex; align-items:center; gap:12px; margin-bottom:12px; }
.page-header h2 { font-size:18px; font-weight:600; margin:0; }
.page-desc { font-size:13px; color:#64748b; margin:0; }
.toolbar { display:flex; justify-content:space-between; align-items:center; margin-bottom:12px; gap:8px; flex-wrap:wrap; }
.toolbar-left, .toolbar-right { display:flex; gap:8px; align-items:center; flex-wrap:wrap; }
.search-box { width:200px; }
:deep(.el-table) { width:100%; }
.detail-view { padding: 8px 0; }
</style>
