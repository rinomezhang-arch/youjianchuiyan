<template>
  <div class="bill-manage-page">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">账单管理 · Bill Management</h2>
        <p class="page-subtitle">账单查询 · 退款处理 · 对账管理</p>
      </div>
      <div class="page-header-right">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          class="date-picker"
        />
        <el-input v-model="searchQuery" placeholder="搜索桌号 / 单号..." clearable class="search-input" />
        <el-select v-model="filterStatus" placeholder="账单状态" clearable class="filter-select">
          <el-option label="已结" value="settled" />
          <el-option label="未结" value="unsettled" />
          <el-option label="退款" value="refunded" />
        </el-select>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card" style="color: #2D4A3E">
        <div class="stat-label">今日账单数</div>
        <div class="stat-value">{{ stats.todayCount }}</div>
        <div class="stat-sub">笔</div>
      </div>
      <div class="stat-card" style="color: #4A7C59">
        <div class="stat-label">今日营收</div>
        <div class="stat-value">¥{{ stats.todayRevenue.toLocaleString() }}</div>
        <div class="stat-sub">已结</div>
      </div>
      <div class="stat-card" style="color: #D4A853">
        <div class="stat-label">未结账单</div>
        <div class="stat-value">{{ stats.unsettledCount }}</div>
        <div class="stat-sub">待处理</div>
      </div>
      <div class="stat-card" style="color: #C25555">
        <div class="stat-label">今日退款</div>
        <div class="stat-value">¥{{ stats.todayRefund.toLocaleString() }}</div>
        <div class="stat-sub">{{ stats.refundCount }}笔</div>
      </div>
    </div>

    <!-- 账单列表 -->
    <div class="bill-table-wrapper">
      <el-table :data="filteredBills" stripe v-loading="loading" @row-click="viewBill">
        <el-table-column prop="billNo" label="账单号" width="160" />
        <el-table-column prop="tableName" label="桌台" width="100" />
        <el-table-column prop="guestCount" label="人数" width="70" />
        <el-table-column prop="dishCount" label="菜品数" width="80" />
        <el-table-column prop="totalAmount" label="消费金额" width="120">
          <template #default="{ row }">
            <span class="amount">¥{{ row.totalAmount.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="discount" label="折扣" width="80">
          <template #default="{ row }">{{ (row.discount || 100).toFixed(0) }}%</template>
        </el-table-column>
        <el-table-column prop="payAmount" label="实付金额" width="120">
          <template #default="{ row }">
            <span class="pay-amount">¥{{ row.payAmount.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="payMethod" label="支付方式" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.payMethod || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="settledAt" label="结算时间" width="160" />
        <el-table-column prop="operator" label="操作员" width="90" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" @click.stop="viewBill(row)">详情</el-button>
            <el-button text size="small" @click.stop="printBill(row)">打印</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 账单详情弹窗 -->
    <el-dialog v-model="showDetail" :title="`账单详情 - ${currentBill?.billNo}`" width="700px">
      <div class="bill-detail" v-if="currentBill">
        <div class="detail-section">
          <h4>基本信息</h4>
          <div class="detail-grid">
            <div class="detail-row"><span class="label">账单号</span><span>{{ currentBill.billNo }}</span></div>
            <div class="detail-row"><span class="label">桌台</span><span>{{ currentBill.tableName }}</span></div>
            <div class="detail-row"><span class="label">人数</span><span>{{ currentBill.guestCount }}人</span></div>
            <div class="detail-row"><span class="label">开台时间</span><span>{{ currentBill.openTime }}</span></div>
            <div class="detail-row"><span class="label">结算时间</span><span>{{ currentBill.settledAt || '-' }}</span></div>
            <div class="detail-row"><span class="label">操作员</span><span>{{ currentBill.operator || '-' }}</span></div>
          </div>
        </div>
        <div class="detail-section">
          <h4>菜品明细 ({{ currentBill.dishes?.length || 0 }}道)</h4>
          <div class="dish-list">
            <div v-for="(dish, idx) in (currentBill.dishes || [])" :key="idx" class="dish-row">
              <span class="dish-name">{{ dish.dishName }}</span>
              <span class="dish-qty">x{{ dish.quantity }}</span>
              <span class="dish-price">¥{{ (dish.price * dish.quantity).toFixed(2) }}</span>
            </div>
          </div>
        </div>
        <div class="detail-section summary">
          <div class="summary-row"><span>菜品小计</span><span>¥{{ currentBill.totalAmount?.toFixed(2) }}</span></div>
          <div class="summary-row"><span>折扣 ({{ currentBill.discount || 100 }}%)</span><span>-¥{{ ((currentBill.totalAmount || 0) - (currentBill.payAmount || 0)).toFixed(2) }}</span></div>
          <div class="summary-row total"><span>实付金额</span><span>¥{{ currentBill.payAmount?.toFixed(2) }}</span></div>
          <div class="summary-row"><span>支付方式</span><span>{{ currentBill.payMethod || '-' }}</span></div>
          <div class="summary-row"><span>人均消费</span><span>¥{{ ((currentBill.payAmount || 0) / (currentBill.guestCount || 1)).toFixed(2) }}</span></div>
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
const searchQuery = ref('')
const filterStatus = ref('')
const dateRange = ref(null)
const showDetail = ref(false)
const currentBill = ref(null)

const stats = ref({
  todayCount: 0,
  todayRevenue: 0,
  unsettledCount: 0,
  todayRefund: 0,
  refundCount: 0
})

const filteredBills = computed(() => {
  let result = bills.value
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    result = result.filter(b =>
      (b.billNo || '').toLowerCase().includes(q) ||
      (b.tableName || '').toLowerCase().includes(q)
    )
  }
  if (filterStatus.value) {
    result = result.filter(b => b.status === filterStatus.value)
  }
  return result
})

function statusType(s) {
  if (s === 'settled') return 'success'
  if (s === 'unsettled') return 'warning'
  if (s === 'refunded') return 'danger'
  return 'info'
}

function statusLabel(s) {
  if (s === 'settled') return '已结'
  if (s === 'unsettled') return '未结'
  if (s === 'refunded') return '退款'
  return s
}

async function fetchBills() {
  loading.value = true
  try {
    const res = await request.get('/bills', { params: { page_size: 100 } })
    if (res.data) {
      bills.value = res.data.content || res.data || []
    }
  } catch (e) {
    // 使用模拟数据
    bills.value = [
      { billNo: 'BILL-20260730-001', tableName: '牡丹厅', guestCount: 8, dishCount: 12, totalAmount: 2680, discount: 95, payAmount: 2546, payMethod: '微信支付', status: 'settled', settledAt: '2026-07-30 13:45', operator: '小王', openTime: '2026-07-30 11:30', dishes: [{ dishName: '招牌红烧肉', quantity: 1, price: 188 }, { dishName: '清蒸鲈鱼', quantity: 1, price: 168 }, { dishName: '蒜蓉西兰花', quantity: 2, price: 48 }] },
      { billNo: 'BILL-20260730-002', tableName: '荷花厅', guestCount: 6, dishCount: 8, totalAmount: 1860, discount: 100, payAmount: 1860, payMethod: '支付宝', status: 'settled', settledAt: '2026-07-30 14:20', operator: '小李', openTime: '2026-07-30 12:00', dishes: [] },
      { billNo: 'BILL-20260730-003', tableName: '3号桌', guestCount: 4, dishCount: 5, totalAmount: 580, discount: 90, payAmount: 522, payMethod: '现金', status: 'unsettled', settledAt: null, operator: '小王', openTime: '2026-07-30 18:30', dishes: [] },
      { billNo: 'BILL-20260730-004', tableName: '兰亭', guestCount: 10, dishCount: 15, totalAmount: 3680, discount: 88, payAmount: 3238.4, payMethod: '银行卡', status: 'settled', settledAt: '2026-07-30 20:15', operator: '小张', openTime: '2026-07-30 17:00', dishes: [] }
    ]
    // 计算统计
    const settled = bills.value.filter(b => b.status === 'settled')
    stats.value = {
      todayCount: bills.value.length,
      todayRevenue: settled.reduce((s, b) => s + b.payAmount, 0),
      unsettledCount: bills.value.filter(b => b.status === 'unsettled').length,
      todayRefund: bills.value.filter(b => b.status === 'refunded').reduce((s, b) => s + b.payAmount, 0),
      refundCount: bills.value.filter(b => b.status === 'refunded').length
    }
  } finally {
    loading.value = false
  }
}

function viewBill(row) {
  currentBill.value = row
  showDetail.value = true
}

function printBill(bill) {
  ElMessage.success(`正在打印账单 ${bill.billNo}`)
}

onMounted(fetchBills)
</script>

<style scoped>
.bill-manage-page { max-width: 1400px; margin: 0 auto; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; flex-wrap: wrap; gap: 12px; }
.page-title { font-size: 22px; font-weight: 700; color: var(--color-text); margin-bottom: 4px; }
.page-subtitle { font-size: 13px; color: var(--color-text-muted); }
.page-header-right { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.date-picker { width: 260px; }
.search-input { width: 200px; }
.filter-select { width: 120px; }
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
.stat-card { background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-lg); padding: 20px; text-align: center; }
.stat-label { font-size: 12px; color: var(--color-text-muted); margin-bottom: 8px; }
.stat-value { font-size: 28px; font-weight: 700; color: var(--color-text); }
.stat-sub { font-size: 11px; color: var(--color-text-muted); margin-top: 4px; }
.bill-table-wrapper { background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-lg); overflow: hidden; }
.amount { font-weight: 500; }
.pay-amount { font-weight: 700; color: var(--color-primary); }
/* 账单详情 */
.bill-detail { max-height: 60vh; overflow-y: auto; }
.detail-section { margin-bottom: 20px; }
.detail-section h4 { font-size: 14px; font-weight: 600; color: var(--color-text); margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px solid var(--color-border-light); }
.detail-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.detail-row { display: flex; flex-direction: column; gap: 2px; font-size: 13px; }
.detail-row .label { color: var(--color-text-muted); font-size: 11px; }
.dish-list { display: flex; flex-direction: column; gap: 4px; }
.dish-row { display: flex; align-items: center; padding: 8px 12px; background: var(--color-bg-alt); border-radius: var(--radius-sm); font-size: 13px; }
.dish-row .dish-name { flex: 1; font-weight: 500; }
.dish-row .dish-qty { color: var(--color-text-muted); margin: 0 16px; }
.dish-row .dish-price { font-weight: 600; color: var(--color-accent-dark); }
.summary { background: var(--color-bg-alt); border-radius: var(--radius-md); padding: 16px; }
.summary-row { display: flex; justify-content: space-between; padding: 6px 0; font-size: 13px; color: var(--color-text-secondary); }
.summary-row.total { font-size: 16px; font-weight: 700; color: var(--color-primary); border-top: 1px solid var(--color-border); padding-top: 10px; margin-top: 4px; }
</style>
