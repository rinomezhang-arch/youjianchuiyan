<template>
  <div class="receipt-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">入库验收 · Receipt</h2>
        <p class="page-desc">采购收货登记 · 验收确认 · 异常处理</p>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-label">待验收</div>
        <div class="stat-value">{{ stats.pending }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">今日已验</div>
        <div class="stat-value">{{ stats.todayVerified }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">本月入库</div>
        <div class="stat-value">{{ stats.monthReceipt }}</div>
      </div>
      <div class="stat-card stat-card--accent">
        <div class="stat-label">异常单</div>
        <div class="stat-value">{{ stats.abnormal }}</div>
      </div>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-select v-model="filters.status" placeholder="状态" style="width:140px" clearable>
            <el-option label="全部" value="" />
            <el-option label="待验收" value="pending" />
            <el-option label="已验收" value="verified" />
            <el-option label="异常" value="abnormal" />
          </el-select>
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width:260px"
          />
          <el-input v-model="filters.keyword" placeholder="搜索单号/供应商" style="width:220px" clearable />
          <el-button @click="resetFilters">重置</el-button>
        </div>
        <div class="toolbar-right">
          <el-button @click="exportData">导出</el-button>
          <el-button type="primary" @click="openAddDialog">新增验收单</el-button>
        </div>
      </div>

      <el-table :data="filteredList" stripe v-loading="loading" empty-text="暂无入库验收记录">
        <el-table-column prop="receiptNo" label="单号" width="160" />
        <el-table-column prop="supplierName" label="供应商" min-width="160" />
        <el-table-column prop="receiptType" label="入库类型" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="row.receiptType === '采购入库' ? '' : 'warning'">{{ row.receiptType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="itemCount" label="品项数" width="90" align="center" />
        <el-table-column prop="totalAmount" label="总金额" width="120" align="right">
          <template #default="{ row }">¥{{ formatMoney(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column prop="receiver" label="验收人" width="100" />
        <el-table-column prop="verifyTime" label="验收时间" width="160" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'pending'" type="warning" size="small">待验收</el-tag>
            <el-tag v-else-if="row.status === 'verified'" type="success" size="small">已验收</el-tag>
            <el-tag v-else-if="row.status === 'abnormal'" type="danger" size="small">异常</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'pending'" size="small" type="success" @click="verifyReceipt(row)">验收</el-button>
            <el-button v-if="row.status === 'pending'" size="small" type="danger" @click="rejectReceipt(row)">驳回</el-button>
            <el-button size="small" @click="viewDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="showAddDialog" title="新增入库验收单" width="820px" @close="resetAddForm">
      <el-form :model="addForm" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="供应商" required>
              <el-select v-model="addForm.supplierId" filterable placeholder="请选择供应商" style="width:100%">
                <el-option v-for="s in supplierOptions" :key="s.id" :label="s.name" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入库类型">
              <el-select v-model="addForm.receiptType" placeholder="请选择" style="width:100%">
                <el-option label="采购入库" value="采购入库" />
                <el-option label="退货入库" value="退货入库" />
                <el-option label="调拨入库" value="调拨入库" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="入库明细">
          <el-table :data="addForm.items" border size="small">
            <el-table-column label="品项" min-width="180">
              <template #default="{ row }">
                <el-select v-model="row.materialId" filterable size="small" placeholder="选择品项" style="width:100%">
                  <el-option v-for="m in materialOptions" :key="m.id" :label="m.name" :value="m.id" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="单位" width="80">
              <template #default="{ row }">{{ row.unit }}</template>
            </el-table-column>
            <el-table-column label="数量" width="130">
              <template #default="{ row }">
                <el-input-number v-model="row.quantity" :min="0" size="small" style="width:100%" />
              </template>
            </el-table-column>
            <el-table-column label="单价" width="140">
              <template #default="{ row }">
                <el-input-number v-model="row.price" :min="0" :step="0.01" :precision="2" size="small" style="width:100%" />
              </template>
            </el-table-column>
            <el-table-column label="金额" width="100" align="right">
              <template #default="{ row }">¥{{ formatMoney((row.quantity || 0) * (row.price || 0)) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="70" align="center">
              <template #default="{ $index }">
                <el-button type="danger" text size="small" @click="removeAddItem($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="add-items-footer">
            <el-button size="small" @click="addAddItem">添加品项</el-button>
            <span class="add-total">合计: <b>¥{{ formatMoney(addTotalAmount) }}</b></span>
          </div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="addForm.remark" type="textarea" :rows="2" placeholder="备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="submitAdd">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const list = ref([])
const supplierOptions = ref([])
const materialOptions = ref([])

const filters = reactive({
  status: '',
  dateRange: [],
  keyword: ''
})

const stats = computed(() => ({
  pending: list.value.filter(r => r.status === 'pending').length,
  todayVerified: 0,
  monthReceipt: 0,
  abnormal: list.value.filter(r => r.status === 'abnormal').length
}))

const filteredList = computed(() => {
  return list.value.filter(row => {
    if (filters.status && row.status !== filters.status) return false
    if (filters.keyword) {
      const kw = filters.keyword.toLowerCase()
      if (!String(row.receiptNo || '').toLowerCase().includes(kw) &&
          !String(row.supplierName || '').toLowerCase().includes(kw)) return false
    }
    if (filters.dateRange && filters.dateRange.length === 2) {
      const d = row.verifyTime ? String(row.verifyTime).slice(0, 10) : ''
      if (d < filters.dateRange[0] || d > filters.dateRange[1]) return false
    }
    return true
  })
})

function formatMoney(n) {
  return Number(n || 0).toFixed(2)
}

function resetFilters() {
  filters.status = ''
  filters.dateRange = []
  filters.keyword = ''
}

const showAddDialog = ref(false)
const addForm = ref({ supplierId: '', receiptType: '采购入库', items: [], remark: '' })

const addTotalAmount = computed(() => {
  return addForm.value.items.reduce((sum, i) => sum + (i.quantity || 0) * (i.price || 0), 0)
})

function openAddDialog() {
  resetAddForm()
  showAddDialog.value = true
}

function resetAddForm() {
  addForm.value = { supplierId: '', receiptType: '采购入库', items: [emptyItem()], remark: '' }
}

function emptyItem() {
  return { materialId: '', unit: '', quantity: 1, price: 0 }
}

function addAddItem() {
  addForm.value.items.push(emptyItem())
}

function removeAddItem(index) {
  addForm.value.items.splice(index, 1)
}

function submitAdd() {
  if (!addForm.value.supplierId) { ElMessage.warning('请选择供应商'); return }
  const valid = addForm.value.items.filter(i => i.materialId && i.quantity > 0)
  if (valid.length === 0) { ElMessage.warning('请添加有效的品项'); return }
  ElMessage.success('入库验收单已提交（待接入API）')
  showAddDialog.value = false
}

function verifyReceipt(row) {
  ElMessageBox.confirm(`确认验收单据 "${row.receiptNo}" ?`, '验收确认', {
    confirmButtonText: '确认验收', cancelButtonText: '取消', type: 'warning'
  }).then(() => {
    row.status = 'verified'
    ElMessage.success('验收成功')
  }).catch(() => {})
}

function rejectReceipt(row) {
  ElMessageBox.confirm(`确认将单据 "${row.receiptNo}" 标记为异常?`, '异常确认', {
    confirmButtonText: '确认', cancelButtonText: '取消', type: 'error'
  }).then(() => {
    row.status = 'abnormal'
    ElMessage.success('已标记为异常')
  }).catch(() => {})
}

function viewDetail(row) {
  ElMessage.info(`查看单据: ${row.receiptNo}`)
}

function exportData() {
  ElMessage.info('导出功能待接入')
}
</script>

<style scoped>
.receipt-page {
  padding: 24px 32px;
  background: #FAF8F5;
  min-height: 100%;
  color: #1a2f23;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}
.header-left { display: flex; flex-direction: column; gap: 4px; }
.page-title { font-size: 22px; font-weight: 700; color: #2D4A3E; margin: 0; }
.page-desc { font-size: 13px; color: #6b7c70; margin: 0; }
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
.stat-card {
  background: #fff;
  border-radius: 10px;
  padding: 18px 20px;
  border: 1px solid #ece7df;
  border-left: 4px solid #2D4A3E;
}
.stat-card--accent { border-left-color: #C4A35A; }
.stat-label { font-size: 13px; color: #6b7c70; margin-bottom: 8px; }
.stat-value { font-size: 26px; font-weight: 700; color: #2D4A3E; }
.stat-card--accent .stat-value { color: #C4A35A; }
.content-card {
  background: #fff;
  border-radius: 10px;
  padding: 20px;
  border: 1px solid #ece7df;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 12px;
  flex-wrap: wrap;
}
.toolbar-left, .toolbar-right {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}
.add-items-footer {
  margin-top: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.add-total { font-size: 14px; color: #1a2f23; }
.add-total b { color: #C4A35A; font-size: 15px; }
:deep(.el-table) { width: 100%; }
:deep(.el-button--primary) {
  background: #2D4A3E;
  border-color: #2D4A3E;
}
:deep(.el-button--primary:hover) {
  background: #3a5e4f;
  border-color: #3a5e4f;
}
</style>
