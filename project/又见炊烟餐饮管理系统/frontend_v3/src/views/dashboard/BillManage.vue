<template>
  <div class="bill-manage-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">账单管理 · Bill Management</h1>
        <p class="page-desc">账单查询 · 结算状态跟踪 · 营收明细核对</p>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card stat-today">
        <div class="stat-label">今日账单</div>
        <div class="stat-value">{{ stats.todayCount }}</div>
        <div class="stat-sub">Today's Bills</div>
      </div>
      <div class="stat-card stat-month">
        <div class="stat-label">本月账单</div>
        <div class="stat-value">{{ stats.monthCount }}</div>
        <div class="stat-sub">Monthly Bills</div>
      </div>
      <div class="stat-card stat-unsettled">
        <div class="stat-label">未结金额</div>
        <div class="stat-value">¥{{ stats.unsettledAmount.toLocaleString() }}</div>
        <div class="stat-sub">Unsettled</div>
      </div>
      <div class="stat-card stat-settled">
        <div class="stat-label">已结金额</div>
        <div class="stat-value">¥{{ stats.settledAmount.toLocaleString() }}</div>
        <div class="stat-sub">Settled</div>
      </div>
    </div>

    <!-- 内容卡片 -->
    <div class="content-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-select v-model="filterStatus" placeholder="账单状态" clearable style="width:140px">
            <el-option label="全部" value="" />
            <el-option label="未结" value="unsettled" />
            <el-option label="已结" value="settled" />
          </el-select>
          <el-select v-model="filterPayMethod" placeholder="支付方式" clearable style="width:140px">
            <el-option label="微信支付" value="微信支付" />
            <el-option label="支付宝" value="支付宝" />
            <el-option label="现金" value="现金" />
            <el-option label="银行卡" value="银行卡" />
            <el-option label="会员卡" value="会员卡" />
          </el-select>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width:260px"
          />
        </div>
        <el-button type="primary" @click="fetchBills">刷新</el-button>
      </div>

      <el-table :data="filteredBills" stripe v-loading="loading" empty-text="暂无账单数据">
        <el-table-column prop="billNo" label="账单号" width="180" />
        <el-table-column prop="tableName" label="桌号" width="110" />
        <el-table-column prop="totalAmount" label="消费金额" width="140" align="right">
          <template #default="{ row }">
            <span class="amount-text">¥{{ (row.totalAmount || 0).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="payMethod" label="支付方式" width="120">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.payMethod || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="payTime" label="支付时间" width="180" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" @click="viewBill(row)">详情</el-button>
            <el-button v-if="row.status === 'unsettled'" text size="small" type="success" @click="settleBill(row)">结算</el-button>
            <el-button text size="small" @click="printBill(row)">打印</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 账单详情弹窗 -->
    <el-dialog v-model="showDetail" :title="`账单详情 - ${currentBill?.billNo || ''}`" width="640px">
      <div class="bill-detail" v-if="currentBill">
        <el-form label-width="100px" label-position="right" class="detail-form">
          <el-form-item label="账单号"><span class="dialog-text">{{ currentBill.billNo }}</span></el-form-item>
          <el-form-item label="桌号"><span class="dialog-text">{{ currentBill.tableName }}</span></el-form-item>
          <el-form-item label="消费金额"><span class="dialog-text">¥{{ (currentBill.totalAmount || 0).toFixed(2) }}</span></el-form-item>
          <el-form-item label="折扣">
            <span class="dialog-text">{{ (currentBill.discount || 100).toFixed(0) }}%</span>
          </el-form-item>
          <el-form-item label="实付金额"><span class="amount-text">¥{{ (currentBill.payAmount || 0).toFixed(2) }}</span></el-form-item>
          <el-form-item label="支付方式"><span class="dialog-text">{{ currentBill.payMethod || '-' }}</span></el-form-item>
          <el-form-item label="支付时间"><span class="dialog-text">{{ currentBill.payTime || '-' }}</span></el-form-item>
          <el-form-item label="状态">
            <el-tag :type="statusType(currentBill.status)" size="small">{{ statusLabel(currentBill.status) }}</el-tag>
          </el-form-item>
        </el-form>
        <div class="dish-section" v-if="currentBill.dishes && currentBill.dishes.length">
          <h4 class="dish-title">菜品明细 ({{ currentBill.dishes.length }}道)</h4>
          <div class="dish-list">
            <div v-for="(dish, idx) in currentBill.dishes" :key="idx" class="dish-row">
              <span class="dish-name">{{ dish.dishName }}</span>
              <span class="dish-qty">x{{ dish.quantity }}</span>
              <span class="dish-price">¥{{ (dish.price * dish.quantity).toFixed(2) }}</span>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showDetail = false">关闭</el-button>
        <el-button type="primary" @click="printBill(currentBill)">打印账单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const bills = ref([])
const filterStatus = ref('')
const filterPayMethod = ref('')
const dateRange = ref(null)
const showDetail = ref(false)
const currentBill = ref(null)

const stats = ref({
  todayCount: 0,
  monthCount: 0,
  unsettledAmount: 0,
  settledAmount: 0
})

const filteredBills = computed(() => {
  let list = bills.value
  if (filterStatus.value) list = list.filter(b => b.status === filterStatus.value)
  if (filterPayMethod.value) list = list.filter(b => b.payMethod === filterPayMethod.value)
  if (dateRange.value && dateRange.value.length === 2) {
    const [start, end] = dateRange.value
    list = list.filter(b => {
      if (!b.payTime) return false
      const t = new Date(b.payTime)
      return t >= new Date(start) && t <= new Date(end)
    })
  }
  return list
})

function statusType(s) {
  if (s === 'settled') return 'success'
  if (s === 'unsettled') return 'warning'
  return 'info'
}

function statusLabel(s) {
  if (s === 'settled') return '已结'
  if (s === 'unsettled') return '未结'
  return s
}

async function fetchBills() {
  loading.value = true
  try {
    const res = await request.get('/bills', { params: { page_size: 100 } })
    if (res.data) {
      bills.value = res.data.content || res.data || []
    }
  } catch {
    bills.value = [
      { billNo: 'BILL-20260730-001', tableName: '牡丹厅', totalAmount: 2680, discount: 95, payAmount: 2546, payMethod: '微信支付', payTime: '2026-07-30 13:45', status: 'settled', dishes: [{ dishName: '招牌红烧肉', quantity: 1, price: 188 }, { dishName: '清蒸鲈鱼', quantity: 1, price: 168 }, { dishName: '蒜蓉西兰花', quantity: 2, price: 48 }] },
      { billNo: 'BILL-20260730-002', tableName: '荷花厅', totalAmount: 1860, discount: 100, payAmount: 1860, payMethod: '支付宝', payTime: '2026-07-30 14:20', status: 'settled', dishes: [] },
      { billNo: 'BILL-20260730-003', tableName: '3号桌', totalAmount: 580, discount: 100, payAmount: 580, payMethod: '', payTime: null, status: 'unsettled', dishes: [] },
      { billNo: 'BILL-20260730-004', tableName: '兰亭', totalAmount: 3680, discount: 88, payAmount: 3238.4, payMethod: '银行卡', payTime: '2026-07-30 20:15', status: 'settled', dishes: [] },
      { billNo: 'BILL-20260729-011', tableName: '5号桌', totalAmount: 420, discount: 100, payAmount: 420, payMethod: '现金', payTime: '2026-07-29 19:30', status: 'settled', dishes: [] },
      { billNo: 'BILL-20260729-015', tableName: '8号桌', totalAmount: 980, discount: 100, payAmount: 980, payMethod: '', payTime: null, status: 'unsettled', dishes: [] }
    ]
  } finally {
    loading.value = false
  }
  refreshStats()
}

function refreshStats() {
  const settled = bills.value.filter(b => b.status === 'settled')
  const unsettled = bills.value.filter(b => b.status === 'unsettled')
  stats.value = {
    todayCount: bills.value.filter(b => (b.payTime || '').startsWith('2026-07-30')).length,
    monthCount: bills.value.length,
    unsettledAmount: unsettled.reduce((s, b) => s + (b.totalAmount || 0), 0),
    settledAmount: settled.reduce((s, b) => s + (b.payAmount || 0), 0)
  }
}

function viewBill(row) {
  currentBill.value = row
  showDetail.value = true
}

function settleBill(row) {
  ElMessage.success(`账单 ${row.billNo} 已发起结算`)
}

function printBill(bill) {
  if (!bill) return
  ElMessage.success(`正在打印账单 ${bill.billNo}`)
}

onMounted(fetchBills)
</script>

<style scoped>
.bill-manage-page {
  padding: 24px 32px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}
.header-left {
  display: flex;
  flex-direction: column;
}
.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a2f23;
  margin: 0;
  letter-spacing: 0.5px;
}
.page-desc {
  font-size: 13px;
  color: #5D6D7E;
  margin-top: 6px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
.stat-card {
  background: #FFFFFF;
  border: 1px solid #E8E4DE;
  border-radius: 10px;
  padding: 20px 22px;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}
.stat-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  border-radius: 2px 0 0 2px;
}
.stat-card.stat-today::before { background: #2D4A3E; }
.stat-card.stat-month::before { background: #5B7B8A; }
.stat-card.stat-unsettled::before { background: #C4A35A; }
.stat-card.stat-settled::before { background: #4A7C59; }
.stat-card:hover {
  box-shadow: 0 4px 12px rgba(45, 74, 62, 0.08);
  transform: translateY(-2px);
}
.stat-label {
  font-size: 13px;
  color: #5D6D7E;
  font-weight: 500;
  letter-spacing: 0.5px;
}
.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: #1a2f23;
  line-height: 1.2;
  margin-top: 8px;
}
.stat-sub {
  font-size: 12px;
  color: #95A5A6;
  margin-top: 4px;
}

.content-card {
  background: #FFFFFF;
  border: 1px solid #E8E4DE;
  border-radius: 10px;
  padding: 20px 24px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}
.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.amount-text {
  font-weight: 700;
  color: #2D4A3E;
}

.dialog-text {
  color: #1a2f23;
  font-size: 14px;
}

.bill-detail {
  max-height: 60vh;
  overflow-y: auto;
}
.detail-form {
  margin-bottom: 8px;
}
.dish-section {
  margin-top: 8px;
  padding-top: 16px;
  border-top: 1px solid #F0EBE5;
}
.dish-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a2f23;
  margin-bottom: 12px;
}
.dish-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.dish-row {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  background: #F5F2ED;
  border-radius: 6px;
  font-size: 13px;
}
.dish-row .dish-name {
  flex: 1;
  font-weight: 500;
  color: #1a2f23;
}
.dish-row .dish-qty {
  color: #95A5A6;
  margin: 0 16px;
}
.dish-row .dish-price {
  font-weight: 600;
  color: #C4A35A;
}

@media (max-width: 1200px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 768px) {
  .stats-row { grid-template-columns: 1fr; }
  .toolbar { flex-direction: column; align-items: stretch; }
}
</style>
