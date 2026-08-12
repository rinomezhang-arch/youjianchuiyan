<template>
  <div class="reconciliation-page">
    <div class="page-header">
      <h2>供应商对账 · Supplier Reconciliation</h2>
      <p class="page-desc">采购对账 · 应付统计 · 结算确认</p>
    </div>

    <!-- 第一部分：对账统计概览 -->
    <div class="overview-section">
      <div
        class="overview-card overview-red"
        @click="filterByOverdue"
      >
        <div class="overview-label">总未付金额</div>
        <div class="overview-value">¥{{ formatAmount(overviewStats.totalUnpaid) }}</div>
        <div class="overview-sub">共 {{ overviewStats.pendingCount + overviewStats.reconcilingCount + overviewStats.confirmedCount + overviewStats.disputedCount }} 笔待结</div>
      </div>
      <div
        class="overview-card overview-orange"
        @click="filterByStatus('0')"
      >
        <div class="overview-label">待对账笔数</div>
        <div class="overview-value">{{ overviewStats.pendingCount }} 笔</div>
        <div class="overview-sub">点击查看</div>
      </div>
      <div
        class="overview-card overview-deepred"
        @click="filterByOverdue"
      >
        <div class="overview-label">逾期未付</div>
        <div class="overview-value">{{ overviewStats.overdueCount }} 笔</div>
        <div class="overview-sub">¥{{ formatAmount(overviewStats.overdueAmount) }}</div>
      </div>
      <div
        class="overview-card overview-blue"
        @click="filterByMonthPayable"
      >
        <div class="overview-label">本月应付</div>
        <div class="overview-value">¥{{ formatAmount(overviewStats.monthPayable) }}</div>
        <div class="overview-sub">{{ new Date().getFullYear() }}年{{ new Date().getMonth() + 1 }}月</div>
      </div>
    </div>

    <!-- 第二部分：未对账汇总卡片 -->
    <div class="summary-section">
      <div class="section-title">
        <span class="title-icon">◇</span>
        <span>未对账汇总 · Unbilled Summary</span>
      </div>
      <div class="summary-cards" v-loading="summaryLoading">
        <div
          v-for="item in unbilledSummary"
          :key="item.supplier_id"
          class="summary-card"
          @click="filterBySupplier(item.supplier_id)"
          @dblclick="openGenerateDialog(item.supplier_id)"
        >
          <div class="card-header">
            <span class="supplier-name">{{ item.supplier_name }}</span>
            <span class="card-arrow">→</span>
          </div>
          <div class="card-body">
            <div class="card-row">
              <span class="label">入库金额</span>
              <span class="value receipt">¥{{ formatAmount(item.receipt_amount) }}</span>
            </div>
            <div class="card-row">
              <span class="label">退货金额</span>
              <span class="value return">¥{{ formatAmount(item.return_amount) }}</span>
            </div>
            <div class="card-row net">
              <span class="label">对账净额</span>
              <span class="value net-value">¥{{ formatAmount(item.receipt_amount - item.return_amount) }}</span>
            </div>
          </div>
        </div>
        <div v-if="unbilledSummary.length === 0 && !summaryLoading" class="empty-cards">
          暂无未对账数据
        </div>
      </div>
    </div>

    <!-- 第二部分：工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-date-picker
          v-model="queryForm.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 260px"
        />
        <el-select
          v-model="queryForm.supplierId"
          placeholder="选择供应商"
          filterable
          style="width: 200px"
        >
          <el-option
            v-for="s in supplierList"
            :key="s.supplier_id"
            :label="s.supplier_name"
            :value="s.supplier_id"
          />
        </el-select>
        <el-select v-model="queryForm.status" placeholder="状态筛选" style="width: 130px">
          <el-option label="全部" value="" />
          <el-option label="待对账" value="0" />
          <el-option label="对账中" value="1" />
          <el-option label="已确认" value="2" />
          <el-option label="有异议" value="3" />
          <el-option label="已付款" value="4" />
        </el-select>
        <el-button type="primary" @click="fetchSettlements" class="btn-primary">查询</el-button>
        <el-button @click="resetQuery" class="btn-default">重置</el-button>
      </div>
      <div class="toolbar-right">
        <el-button type="primary" @click="openGenerateDialog()" class="btn-primary">+ 生成对账单</el-button>
        <el-button @click="exportData" class="btn-gold">导出</el-button>
        <el-button @click="printSelected" class="btn-default">打印</el-button>
      </div>
    </div>

    <!-- 第三部分：对账单列表 -->
    <div class="table-container" v-loading="tableLoading">
      <el-table :data="settlementList" border style="width: 100%" class="reconciliation-table" @row-dblclick="viewDetail" :row-class-name="getRowClassName">
        <el-table-column prop="settlement_id" label="对账单号" width="150" />
        <el-table-column prop="settlement_month" label="对账月份" width="110" />
        <el-table-column prop="supplier_name" label="供应商" width="150" />
        <el-table-column prop="receipt_amount" label="入库金额" width="110" align="right">
          <template #default="{ row }">¥{{ formatAmount(row.receipt_amount) }}</template>
        </el-table-column>
        <el-table-column prop="return_amount" label="退货金额" width="110" align="right">
          <template #default="{ row }">
            <span class="text-red">¥{{ formatAmount(row.return_amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="total_amount" label="对账总额" width="110" align="right">
          <template #default="{ row }">
            <span class="font-bold">¥{{ formatAmount(row.total_amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="paid_amount" label="已付金额" width="110" align="right">
          <template #default="{ row }">¥{{ formatAmount(row.paid_amount) }}</template>
        </el-table-column>
        <el-table-column prop="unpaid_amount" label="未付金额" width="110" align="right">
          <template #default="{ row }">
            <span class="text-red font-bold">¥{{ formatAmount(row.unpaid_amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="due_date" label="到期日" width="110">
          <template #default="{ row }">{{ formatDate(row.due_date) }}</template>
        </el-table-column>
        <el-table-column label="逾期天数" width="100">
          <template #default="{ row }">
            <span v-if="isOverdue(row)" class="overdue-days">逾期 {{ getOverdueDays(row) }} 天</span>
            <span v-else style="color: #95A5A6;">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small" effect="light">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="payment_terms" label="账期" width="100" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button size="small" @click="viewDetail(row)" class="btn-link">查看</el-button>
              <el-button v-if="row.status === 0" size="small" type="primary" @click.stop="reconcileSettlement(row)" class="btn-link btn-link-primary">发起对账</el-button>
              <el-button v-if="row.status === 1" size="small" type="success" @click.stop="confirmSettlement(row)" class="btn-link btn-link-success">确认对账</el-button>
              <el-button v-if="row.status === 1" size="small" type="danger" @click.stop="openDisputeDialog(row)" class="btn-link btn-link-danger">标记异议</el-button>
              <el-button v-if="row.status === 3" size="small" type="warning" @click.stop="resolveDispute(row)" class="btn-link btn-link-warning">解决异议</el-button>
              <el-button v-if="row.status === 2 || row.status === 3" size="small" type="primary" @click.stop="viewPaymentPlans(row)" class="btn-link btn-link-primary">付款</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchSettlements"
          @current-change="fetchSettlements"
        />
      </div>
    </div>

    <!-- 弹窗1：生成对账单 -->
    <el-dialog v-model="generateDialogVisible" title="生成对账单" width="500px" class="reconciliation-dialog">
      <el-form :model="generateForm" :rules="generateRules" ref="generateFormRef" label-width="100px">
        <el-form-item label="供应商" prop="supplier_id">
          <el-select v-model="generateForm.supplier_id" placeholder="请选择供应商" filterable style="width: 100%">
            <el-option
              v-for="s in supplierList"
              :key="s.supplier_id"
              :label="s.supplier_name"
              :value="s.supplier_id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="对账日期" prop="dateRange">
          <el-date-picker
            v-model="generateForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="generateDialogVisible = false" class="btn-default">取消</el-button>
        <el-button type="primary" @click="generateSettlement" class="btn-primary">生成</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗2：对账单详情 -->
    <el-dialog v-model="detailDialogVisible" :title="detailTitle" width="900px" class="detail-dialog">
      <div v-if="currentSettlement" class="detail-content">
        <div class="detail-header">
          <div class="detail-row">
            <div class="detail-item">
              <span class="detail-label">对账单号：</span>
              <span class="detail-value">{{ currentSettlement.settlement_id }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">对账月份：</span>
              <span class="detail-value">{{ currentSettlement.settlement_month }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">供应商：</span>
              <span class="detail-value">{{ currentSettlement.supplier_name }}</span>
            </div>
          </div>
          <div class="detail-row">
            <div class="detail-item">
              <span class="detail-label">账期：</span>
              <span class="detail-value">{{ currentSettlement.payment_terms }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">到期日：</span>
              <span class="detail-value">{{ formatDate(currentSettlement.due_date) }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">状态：</span>
              <el-tag :type="getStatusType(currentSettlement.status)" size="small">
                {{ getStatusText(currentSettlement.status) }}
              </el-tag>
            </div>
          </div>
        </div>

        <div class="amount-summary">
          <div class="amount-item">
            <span class="amount-label">入库金额</span>
            <span class="amount-value receipt">¥{{ formatAmount(currentSettlement.receipt_amount) }}</span>
          </div>
          <div class="amount-item">
            <span class="amount-label">退货金额</span>
            <span class="amount-value return">¥{{ formatAmount(currentSettlement.return_amount) }}</span>
          </div>
          <div class="amount-item">
            <span class="amount-label">对账总额</span>
            <span class="amount-value total">¥{{ formatAmount(currentSettlement.total_amount) }}</span>
          </div>
          <div class="amount-item">
            <span class="amount-label">已付金额</span>
            <span class="amount-value paid">¥{{ formatAmount(currentSettlement.paid_amount) }}</span>
          </div>
          <div class="amount-item">
            <span class="amount-label">未付金额</span>
            <span class="amount-value unpaid">¥{{ formatAmount(currentSettlement.unpaid_amount) }}</span>
          </div>
        </div>

        <el-tabs v-model="activeTab" class="detail-tabs">
          <el-tab-pane label="入库明细" name="receipt">
            <el-table :data="receiptDetails" border style="width: 100%" size="small" class="detail-table">
              <el-table-column prop="bill_no" label="单据号" width="130" />
              <el-table-column prop="bill_date" label="日期" width="100">
                <template #default="{ row }">{{ formatDate(row.bill_date) }}</template>
              </el-table-column>
              <el-table-column prop="material_id" label="原料ID" width="100" />
              <el-table-column prop="material_name" label="原料名称" min-width="120" />
              <el-table-column prop="unit" label="单位" width="60" />
              <el-table-column prop="quantity" label="数量" width="90" align="right" />
              <el-table-column prop="price" label="单价" width="90" align="right">
                <template #default="{ row }">¥{{ formatAmount(row.price) }}</template>
              </el-table-column>
              <el-table-column prop="amount" label="金额" width="110" align="right">
                <template #default="{ row }">¥{{ formatAmount(row.amount) }}</template>
              </el-table-column>
              <el-table-column prop="dispute_reason" label="备注" min-width="120" show-overflow-tooltip>
                <template #default="{ row }">
                  <span v-if="row.dispute_reason" style="color: #C25555;">{{ row.dispute_reason }}</span>
                  <span v-else style="color: #95A5A6;">-</span>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="退货明细" name="return">
            <el-table :data="returnDetails" border style="width: 100%" size="small" class="detail-table">
              <el-table-column prop="bill_no" label="单据号" width="130" />
              <el-table-column prop="bill_date" label="日期" width="100">
                <template #default="{ row }">{{ formatDate(row.bill_date) }}</template>
              </el-table-column>
              <el-table-column prop="material_id" label="原料ID" width="100" />
              <el-table-column prop="material_name" label="原料名称" min-width="120" />
              <el-table-column prop="unit" label="单位" width="60" />
              <el-table-column prop="quantity" label="数量" width="90" align="right" />
              <el-table-column prop="price" label="单价" width="90" align="right">
                <template #default="{ row }">¥{{ formatAmount(row.price) }}</template>
              </el-table-column>
              <el-table-column prop="amount" label="金额" width="110" align="right">
                <template #default="{ row }">¥{{ formatAmount(row.amount) }}</template>
              </el-table-column>
              <el-table-column prop="dispute_reason" label="备注" min-width="120" show-overflow-tooltip>
                <template #default="{ row }">
                  <span v-if="row.dispute_reason" style="color: #C25555;">{{ row.dispute_reason }}</span>
                  <span v-else style="color: #95A5A6;">-</span>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="付款计划" name="payment">
            <div class="payment-plan-header">
              <el-button type="primary" size="small" @click="openPaymentPlanDialog" class="btn-primary-small">+ 创建付款计划</el-button>
            </div>
            <el-table :data="paymentPlans" border style="width: 100%" size="small" class="detail-table">
              <el-table-column prop="plan_no" label="计划编号" width="130" />
              <el-table-column prop="plan_amount" label="计划金额" width="110" align="right">
                <template #default="{ row }">¥{{ formatAmount(row.plan_amount) }}</template>
              </el-table-column>
              <el-table-column prop="plan_date" label="计划付款日期" width="120">
                <template #default="{ row }">{{ formatDate(row.plan_date) }}</template>
              </el-table-column>
              <el-table-column prop="payment_method" label="付款方式" width="100" />
              <el-table-column prop="status" label="状态" width="90">
                <template #default="{ row }">
                  <el-tag :type="getPlanStatusType(row.status)" size="small">
                    {{ getPlanStatusText(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>

      <template #footer>
        <div style="display: flex; justify-content: space-between; align-items: center; width: 100%;">
          <div style="display: flex; gap: 8px;">
            <el-button @click="exportDetailPdf" class="btn-gold" :disabled="!currentSettlement">导出PDF</el-button>
            <el-button @click="printSettlement(currentSettlement)" class="btn-default" :disabled="!currentSettlement">打印</el-button>
          </div>
          <div style="display: flex; gap: 8px;">
            <el-button @click="detailDialogVisible = false" class="btn-default">关闭</el-button>
            <el-button v-if="currentSettlement && currentSettlement.status === 0" type="primary" @click="reconcileSettlement(currentSettlement)" class="btn-primary">发起对账</el-button>
            <el-button v-if="currentSettlement && currentSettlement.status === 1" type="danger" @click="openDisputeDialog(currentSettlement)" class="btn-danger-small">标记异议</el-button>
            <el-button v-if="currentSettlement && currentSettlement.status === 1" type="success" @click="confirmSettlement(currentSettlement)" class="btn-success-small">确认对账</el-button>
            <el-button v-if="currentSettlement && currentSettlement.status === 3" type="warning" @click="resolveDispute(currentSettlement)" class="btn-warning-small">解决异议</el-button>
            <el-button v-if="currentSettlement && (currentSettlement.status == 2 || currentSettlement.status == 3)" type="primary" @click="viewPaymentPlans(currentSettlement)" class="btn-primary">付款</el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- 弹窗3：标记异议 -->
    <el-dialog v-model="disputeDialogVisible" title="标记异议" width="500px" class="reconciliation-dialog">
      <el-form :model="disputeForm" :rules="disputeRules" ref="disputeFormRef" label-width="100px">
        <el-form-item label="异议原因" prop="dispute_reason">
          <el-input v-model="disputeForm.dispute_reason" type="textarea" :rows="4" placeholder="请输入异议原因" />
        </el-form-item>
        <el-form-item label="附件" prop="dispute_attachment">
          <el-input v-model="disputeForm.dispute_attachment" placeholder="请输入附件链接" />
        </el-form-item>
        <el-form-item label="备注" prop="our_remark">
          <el-input v-model="disputeForm.our_remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="disputeDialogVisible = false" class="btn-default">取消</el-button>
        <el-button type="primary" @click="submitDispute" class="btn-primary">确认标记</el-button>
      </template>
    </el-dialog>

    <!-- 弹窗4：创建付款计划 -->
    <el-dialog v-model="paymentPlanDialogVisible" title="创建付款计划" width="500px" class="reconciliation-dialog">
      <el-form :model="paymentPlanForm" :rules="paymentPlanRules" ref="paymentPlanFormRef" label-width="100px">
        <el-form-item label="计划金额" prop="plan_amount">
          <el-input-number v-model="paymentPlanForm.plan_amount" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="计划付款日期" prop="plan_date">
          <el-date-picker
            v-model="paymentPlanForm.plan_date"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="付款方式" prop="payment_method">
          <el-select v-model="paymentPlanForm.payment_method" placeholder="请选择付款方式" style="width: 100%">
            <el-option label="银行转账" value="银行转账" />
            <el-option label="现金" value="现金" />
            <el-option label="承兑" value="承兑" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="paymentPlanForm.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="paymentPlanDialogVisible = false" class="btn-default">取消</el-button>
        <el-button type="primary" @click="createPaymentPlan" class="btn-primary">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()

const summaryLoading = ref(false)
const tableLoading = ref(false)
const unbilledSummary = ref([])
const overviewStats = reactive({
  totalUnpaid: 0,
  pendingCount: 0,
  reconcilingCount: 0,
  confirmedCount: 0,
  disputedCount: 0,
  paidCount: 0,
  overdueCount: 0,
  overdueAmount: 0,
  monthPayable: 0
})
const supplierList = ref([])
const settlementList = ref([])

const queryForm = reactive({
  dateRange: [],
  supplierId: '',
  status: ''
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const generateDialogVisible = ref(false)
const generateFormRef = ref(null)
const generateForm = reactive({
  supplier_id: '',
  dateRange: []
})
const generateRules = {
  supplier_id: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  dateRange: [{ required: true, message: '请选择对账日期范围', trigger: 'change' }]
}

const detailDialogVisible = ref(false)
const currentSettlement = ref(null)
const activeTab = ref('receipt')
const receiptDetails = ref([])
const returnDetails = ref([])
const paymentPlans = ref([])

const detailTitle = computed(() => {
  return currentSettlement.value ? `对账单详情 - ${currentSettlement.value.settlement_id}` : '对账单详情'
})

const disputeDialogVisible = ref(false)
const disputeFormRef = ref(null)
const disputeType = ref('settlement')
const currentDisputeRow = ref(null)
const disputeForm = reactive({
  dispute_reason: '',
  dispute_attachment: '',
  our_remark: ''
})
const disputeRules = {
  dispute_reason: [{ required: true, message: '请输入异议原因', trigger: 'blur' }]
}

const paymentPlanDialogVisible = ref(false)
const paymentPlanFormRef = ref(null)
const paymentPlanForm = reactive({
  plan_amount: 0,
  plan_date: '',
  payment_method: '',
  remark: ''
})
const paymentPlanRules = {
  plan_amount: [{ required: true, message: '请输入计划金额', trigger: 'blur' }],
  plan_date: [{ required: true, message: '请选择计划付款日期', trigger: 'change' }],
  payment_method: [{ required: true, message: '请选择付款方式', trigger: 'change' }]
}

function formatAmount(amount) {
  if (amount === null || amount === undefined) return '0.00'
  return Number(amount).toFixed(2)
}

function formatDate(date) {
  if (!date) return '-'
  return String(date).slice(0, 10)
}

function getStatusType(status) {
  const map = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger', 4: 'primary' }
  return map[status] || 'info'
}

function getStatusText(status) {
  const map = { 0: '待对账', 1: '对账中', 2: '已确认', 3: '有异议', 4: '已付款' }
  return map[status] || '未知'
}

function isOverdue(row) {
  if (!row.due_date || row.status == 4 || !row.unpaid_amount || row.unpaid_amount <= 0) return false
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const due = new Date(row.due_date)
  due.setHours(0, 0, 0, 0)
  return today > due
}

function getOverdueDays(row) {
  if (!row.due_date) return 0
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const due = new Date(row.due_date)
  due.setHours(0, 0, 0, 0)
  const diff = Math.floor((today - due) / (1000 * 60 * 60 * 24))
  return diff > 0 ? diff : 0
}

function getRowClassName({ row }) {
  if (isOverdue(row)) return 'row-overdue'
  return ''
}

function getDetailStatusType(status) {
  const map = { 0: 'info', 1: 'success', 2: 'danger' }
  return map[status] || 'info'
}

function getDetailStatusText(status) {
  const map = { 0: '待确认', 1: '已确认', 2: '有异议' }
  return map[status] || '未知'
}

function getPlanStatusType(status) {
  const map = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
  return map[status] || 'info'
}

function getPlanStatusText(status) {
  const map = { 0: '待审批', 1: '待付款', 2: '已付款', 3: '已取消' }
  return map[status] || '未知'
}

function getCurrentMonth() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}

async function fetchOverviewStats() {
  try {
    const d = await request({ url: '/menu-api/settlements/stats/overview', method: 'get' })
    if (d.code === 200 && d.data) {
      Object.assign(overviewStats, d.data)
    }
  } catch (e) { console.error(e) }
}

async function fetchUnbilledSummary() {
  summaryLoading.value = true
  try {
    const d = await request({ url: '/menu-api/unbilled-summary', method: 'get' })
    if (d.code === 200) {
      unbilledSummary.value = d.data || []
    }
  } catch (e) {
    console.error(e)
  } finally {
    summaryLoading.value = false
  }
}

async function fetchSuppliers() {
  try {
    const d = await request({ url: '/menu-api/suppliers', method: 'get' })
    if (d.code === 200) {
      supplierList.value = (d.data || []).map(item => ({
        supplier_id: item.supplierId || item.supplier_id,
        supplier_name: item.supplierName || item.supplier_name,
        payment_terms: item.paymentTerms || item.payment_terms
      }))
    }
  } catch (e) {
    console.error(e)
  }
}

async function fetchSettlements() {
  tableLoading.value = true
  try {
    const params = {}
    if (queryForm.supplierId) params.supplierId = queryForm.supplierId
    if (queryForm.dateRange && queryForm.dateRange.length === 2) {
      params.startDate = queryForm.dateRange[0]
      params.endDate = queryForm.dateRange[1]
    }
    if (queryForm.status !== '') params.status = queryForm.status
    params.page = pagination.page
    params.pageSize = pagination.pageSize
    const d = await request({ url: '/menu-api/settlements', method: 'get', params })
    if (d.code === 200) {
      settlementList.value = d.data || []
      pagination.total = d.total || 0
    }
  } catch (e) {
    console.error(e)
  } finally {
    tableLoading.value = false
  }
}

function resetQuery() {
  queryForm.dateRange = []
  queryForm.supplierId = ''
  queryForm.status = ''
  pagination.page = 1
  fetchSettlements()
}

function filterByStatus(status) {
  queryForm.status = status
  queryForm.supplierId = ''
  queryForm.dateRange = []
  pagination.page = 1
  fetchSettlements()
}

function filterByOverdue() {
  queryForm.status = ''
  queryForm.supplierId = ''
  queryForm.dateRange = []
  pagination.page = 1
  fetchSettlements()
  ElMessage.info('逾期未付对账单已筛选，可在到期日列查看')
}

function filterByMonthPayable() {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  queryForm.dateRange = [`${year}-${month}-01`, `${year}-${month}-${new Date(year, now.getMonth() + 1, 0).getDate()}`]
  queryForm.status = ''
  queryForm.supplierId = ''
  pagination.page = 1
  fetchSettlements()
}

function filterBySupplier(supplierId) {
  queryForm.supplierId = supplierId
  queryForm.status = ''
  queryForm.dateRange = []
  pagination.page = 1
  fetchSettlements()
}

function openGenerateDialog(supplierId = '') {
  generateForm.supplier_id = supplierId
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 30)
  generateForm.dateRange = [
    start.toISOString().slice(0, 10),
    end.toISOString().slice(0, 10)
  ]
  generateDialogVisible.value = true
}

async function generateSettlement() {
  if (!generateFormRef.value) return
  await generateFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const body = {
          supplier_id: generateForm.supplier_id,
          start_date: generateForm.dateRange[0],
          end_date: generateForm.dateRange[1]
        }
        const d = await request({ url: '/menu-api/settlements/generate', method: 'post', data: body })
        if (d.code === 200) {
          ElMessage.success('对账单生成成功')
          generateDialogVisible.value = false
          fetchSettlements()
          fetchUnbilledSummary()
        } else {
          ElMessage.error(d.message || '生成失败')
        }
      } catch (e) {
        ElMessage.error('生成失败')
      }
    }
  })
}

async function viewDetail(row) {
  try {
    const id = row.settlement_id || row.id
    const d = await request({ url: `/menu-api/settlements/${id}`, method: 'get' })
    if (d.code === 200) {
      currentSettlement.value = d.data?.head || d.data
      const allDetails = d.data?.details || []
      receiptDetails.value = allDetails.filter(item => item.bill_type === 1)
      returnDetails.value = allDetails.filter(item => item.bill_type === 2)
      activeTab.value = 'receipt'
      detailDialogVisible.value = true
      fetchPaymentPlans(id)
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('获取详情失败')
  }
}

async function fetchPaymentPlans(settlementId) {
  try {
    const d = await request({ url: `/menu-api/settlements/${settlementId}/payment-plans`, method: 'get' })
    if (d.code === 200) {
      paymentPlans.value = d.data || []
    }
  } catch (e) {
    console.error(e)
  }
}

async function reconcileSettlement(row) {
  ElMessageBox.confirm('确认发起对账？', '提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const id = row.settlement_id || row.id
      const d = await request({ url: `/menu-api/settlements/${id}/reconcile`, method: 'post' })
      if (d.code === 200) {
        ElMessage.success('已发起对账')
        fetchSettlements()
        if (detailDialogVisible.value) {
          viewDetail(row)
        }
      } else {
        ElMessage.error(d.message || '操作失败')
      }
    } catch (e) {
      ElMessage.error('操作失败')
    }
  })
}

async function confirmSettlement(row) {
  ElMessageBox.confirm('确认对账完成？', '提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'success'
  }).then(async () => {
    try {
      const id = row.settlement_id || row.id
      const d = await request({ url: `/menu-api/settlements/${id}/confirm`, method: 'post', data: { reconciled_by: '当前用户' } })
      if (d.code === 200) {
        ElMessage.success('对账已确认')
        fetchSettlements()
        if (detailDialogVisible.value) {
          viewDetail(row)
        }
      } else {
        ElMessage.error(d.message || '操作失败')
      }
    } catch (e) {
      ElMessage.error('操作失败')
    }
  })
}

function openDisputeDialog(row) {
  disputeType.value = 'settlement'
  currentDisputeRow.value = row
  disputeForm.dispute_reason = ''
  disputeForm.dispute_attachment = ''
  disputeForm.our_remark = ''
  disputeDialogVisible.value = true
}

function openDetailDisputeDialog(row) {
  disputeType.value = 'detail'
  currentDisputeRow.value = row
  disputeForm.dispute_reason = row.dispute_reason || ''
  disputeForm.dispute_attachment = ''
  disputeForm.our_remark = ''
  disputeDialogVisible.value = true
}

async function submitDispute() {
  if (!disputeFormRef.value) return
  await disputeFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        let url, data, method
        if (disputeType.value === 'settlement') {
          url = `/menu-api/settlements/${currentDisputeRow.value.settlement_id || currentDisputeRow.value.id}/dispute`
          data = { ...disputeForm }
          method = 'post'
        } else {
          url = `/menu-api/settlements/${currentSettlement.value.settlement_id}/details/${currentDisputeRow.value.id || currentDisputeRow.value.detail_id}`
          data = {
            our_status: 2,
            dispute_reason: disputeForm.dispute_reason
          }
          method = 'put'
        }
        const d = await request({ url, method, data })
        if (d.code === 200) {
          ElMessage.success('异议已标记')
          disputeDialogVisible.value = false
          fetchSettlements()
          if (detailDialogVisible.value && currentSettlement.value) {
            viewDetail(currentSettlement.value)
          }
        } else {
          ElMessage.error(d.message || '操作失败')
        }
      } catch (e) {
        ElMessage.error('操作失败')
      }
    }
  })
}

async function resolveDispute(row) {
  ElMessageBox.confirm('确认解决该异议？', '提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const id = row.settlement_id || row.id
      const d = await request({ url: `/menu-api/settlements/${id}/resolve-dispute`, method: 'post' })
      if (d.code === 200) {
        ElMessage.success('异议已解决')
        fetchSettlements()
        if (detailDialogVisible.value) {
          viewDetail(row)
        }
      } else {
        ElMessage.error(d.message || '操作失败')
      }
    } catch (e) {
      ElMessage.error('操作失败')
    }
  })
}

function viewPaymentPlans(row) {
  viewDetail(row).then(() => {
    activeTab.value = 'payment'
  })
}

function openPaymentPlanDialog() {
  if (currentSettlement.value) {
    paymentPlanForm.plan_amount = Number(currentSettlement.value.unpaid_amount) || 0
  }
  paymentPlanForm.plan_date = ''
  paymentPlanForm.payment_method = ''
  paymentPlanForm.remark = ''
  paymentPlanDialogVisible.value = true
}

async function createPaymentPlan() {
  if (!paymentPlanFormRef.value) return
  await paymentPlanFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const d = await request({
          url: `/menu-api/settlements/${currentSettlement.value.settlement_id}/payment-plans`,
          method: 'post',
          data: paymentPlanForm
        })
        if (d.code === 200) {
          ElMessage.success('付款计划创建成功')
          paymentPlanDialogVisible.value = false
          if (currentSettlement.value) {
            viewDetail(currentSettlement.value)
          }
        } else {
          ElMessage.error(d.message || '创建失败')
        }
      } catch (e) {
        ElMessage.error('创建失败')
      }
    }
  })
}

function exportData() {
  const params = new URLSearchParams()
  if (queryForm.supplierId) params.set('supplierId', queryForm.supplierId)
  if (queryForm.dateRange && queryForm.dateRange.length === 2) {
    params.set('startDate', queryForm.dateRange[0])
    params.set('endDate', queryForm.dateRange[1])
  }
  if (queryForm.status !== '') params.set('status', queryForm.status)
  window.open(`/menu-api/settlements/export?${params}`, '_blank')
}

function printSelected() {
  ElMessage.info('请点击列表右侧的"打印"按钮打印对账单')
}

function exportDetailPdf() {
  if (!currentSettlement.value) return
  const id = currentSettlement.value.settlement_id || currentSettlement.value.id
  window.open(`/menu-api/settlements/${id}/print`, '_blank')
}

async function printSettlement(row) {
  try {
    const id = row.settlement_id || row.id
    const d = await request({ url: `/menu-api/settlements/${id}/print`, method: 'get' })
    if (d.code === 200) {
      openPrintWindow(d.data)
    } else {
      ElMessage.error(d.message || '获取打印数据失败')
    }
  } catch (e) {
    ElMessage.error('获取打印数据失败')
  }
}

function openPrintWindow(data) {
  const head = data.head || {}
  const receiptDetails = data.receiptDetails || []
  const returnDetails = data.returnDetails || []
  const statusMap = { 0: '待对账', 1: '对账中', 2: '已确认', 3: '有异议', 4: '已付款' }

  const receiptRows = receiptDetails.map((item, idx) => `
    <tr>
      <td style="padding:6px 8px;border:1px solid #999;text-align:center;">${idx + 1}</td>
      <td style="padding:6px 8px;border:1px solid #999;">${item.bill_no || ''}</td>
      <td style="padding:6px 8px;border:1px solid #999;">${item.bill_date ? String(item.bill_date).slice(0, 10) : ''}</td>
      <td style="padding:6px 8px;border:1px solid #999;">${item.material_name || ''}</td>
      <td style="padding:6px 8px;border:1px solid #999;text-align:center;">${item.unit || ''}</td>
      <td style="padding:6px 8px;border:1px solid #999;text-align:right;">${Number(item.quantity || 0).toFixed(2)}</td>
      <td style="padding:6px 8px;border:1px solid #999;text-align:right;">${Number(item.price || 0).toFixed(2)}</td>
      <td style="padding:6px 8px;border:1px solid #999;text-align:right;">${Number(item.amount || 0).toFixed(2)}</td>
    </tr>
  `).join('')

  const returnRows = returnDetails.map((item, idx) => `
    <tr>
      <td style="padding:6px 8px;border:1px solid #999;text-align:center;">${idx + 1}</td>
      <td style="padding:6px 8px;border:1px solid #999;">${item.bill_no || ''}</td>
      <td style="padding:6px 8px;border:1px solid #999;">${item.bill_date ? String(item.bill_date).slice(0, 10) : ''}</td>
      <td style="padding:6px 8px;border:1px solid #999;">${item.material_name || ''}</td>
      <td style="padding:6px 8px;border:1px solid #999;text-align:center;">${item.unit || ''}</td>
      <td style="padding:6px 8px;border:1px solid #999;text-align:right;">${Number(item.quantity || 0).toFixed(2)}</td>
      <td style="padding:6px 8px;border:1px solid #999;text-align:right;">${Number(item.price || 0).toFixed(2)}</td>
      <td style="padding:6px 8px;border:1px solid #999;text-align:right;">${Number(item.amount || 0).toFixed(2)}</td>
    </tr>
  `).join('')

  const html = `
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>对账单 - ${head.settlement_id}</title>
<style>
  body { font-family: "Microsoft YaHei", sans-serif; padding: 20px; font-size: 14px; }
  .print-title { text-align: center; font-size: 24px; font-weight: bold; margin-bottom: 10px; color: #2D4A3E; }
  .print-subtitle { text-align: center; font-size: 14px; color: #666; margin-bottom: 20px; }
  .info-table { width: 100%; margin-bottom: 20px; border-collapse: collapse; }
  .info-table td { padding: 6px 10px; }
  .info-label { color: #666; width: 80px; }
  .section-title { font-size: 16px; font-weight: bold; margin: 20px 0 10px 0; color: #2D4A3E; border-left: 4px solid #C4A35A; padding-left: 8px; }
  .detail-table { width: 100%; border-collapse: collapse; margin-bottom: 10px; }
  .detail-table th { background: #FAF8F5; padding: 8px; border: 1px solid #999; color: #2D4A3E; font-weight: 600; }
  .amount-box { display: flex; justify-content: flex-end; gap: 30px; margin: 20px 0; font-size: 15px; }
  .amount-item { text-align: right; }
  .amount-item .label { color: #666; margin-right: 10px; }
  .amount-item .value { font-weight: bold; }
  .total .value { color: #C4A35A; font-size: 18px; }
  .sign-area { display: flex; justify-content: space-between; margin-top: 60px; }
  .sign-col { width: 30%; text-align: center; }
  .sign-line { border-bottom: 1px solid #333; margin-bottom: 5px; height: 30px; }
  @media print {
    body { padding: 0; }
  }
</style>
</head>
<body>
  <div class="print-title">又见炊烟私房菜 · 供应商对账单</div>
  <div class="print-subtitle">Youjianchuiyan · Supplier Settlement Statement</div>

  <table class="info-table">
    <tr>
      <td class="info-label">对账单号：</td>
      <td style="font-weight:bold;">${head.settlement_id || ''}</td>
      <td class="info-label">供应商：</td>
      <td>${head.supplier_name || ''}</td>
    </tr>
    <tr>
      <td class="info-label">对账月份：</td>
      <td>${head.settlement_month || ''}</td>
      <td class="info-label">账期：</td>
      <td>${head.payment_terms || ''}</td>
    </tr>
    <tr>
      <td class="info-label">到期日：</td>
      <td>${head.due_date ? String(head.due_date).slice(0, 10) : ''}</td>
      <td class="info-label">状态：</td>
      <td>${statusMap[head.status] || '未知'}</td>
    </tr>
  </table>

  ${receiptRows ? `
  <div class="section-title">入库明细</div>
  <table class="detail-table">
    <thead>
      <tr>
        <th style="width:50px;">序号</th>
        <th style="width:130px;">入库单号</th>
        <th style="width:100px;">日期</th>
        <th>原料名称</th>
        <th style="width:60px;">单位</th>
        <th style="width:80px;">数量</th>
        <th style="width:80px;">单价</th>
        <th style="width:100px;">金额</th>
      </tr>
    </thead>
    <tbody>
      ${receiptRows}
    </tbody>
  </table>
  ` : ''}

  ${returnRows ? `
  <div class="section-title">退货明细</div>
  <table class="detail-table">
    <thead>
      <tr>
        <th style="width:50px;">序号</th>
        <th style="width:130px;">退货单号</th>
        <th style="width:100px;">日期</th>
        <th>原料名称</th>
        <th style="width:60px;">单位</th>
        <th style="width:80px;">数量</th>
        <th style="width:80px;">单价</th>
        <th style="width:100px;">金额</th>
      </tr>
    </thead>
    <tbody>
      ${returnRows}
    </tbody>
  </table>
  ` : ''}

  <div class="amount-box">
    <div class="amount-item">
      <span class="label">入库金额：</span>
      <span class="value">¥${Number(head.receipt_amount || 0).toFixed(2)}</span>
    </div>
    <div class="amount-item">
      <span class="label">退货金额：</span>
      <span class="value" style="color:#dc2626;">¥${Number(head.return_amount || 0).toFixed(2)}</span>
    </div>
    <div class="amount-item total">
      <span class="label">对账总额：</span>
      <span class="value">¥${Number(head.total_amount || 0).toFixed(2)}</span>
    </div>
  </div>

  <div class="sign-area">
    <div class="sign-col">
      <div class="sign-line"></div>
      <div>供应商签字/盖章</div>
    </div>
    <div class="sign-col">
      <div class="sign-line"></div>
      <div>制单人</div>
    </div>
    <div class="sign-col">
      <div class="sign-line"></div>
      <div>财务审核</div>
    </div>
  </div>
</body>
</html>
  `

  const printWin = window.open('', '_blank', 'width=900,height=700')
  printWin.document.write(html)
  printWin.document.close()
  printWin.focus()
  setTimeout(() => {
    printWin.print()
  }, 300)
}

watch(() => route.query.action, (newAction) => {
  if (newAction === 'new') {
    openGenerateDialog()
  }
})

onMounted(() => {
  fetchSuppliers()
  fetchOverviewStats()
  fetchUnbilledSummary()
  fetchSettlements()
  if (route.query.action === 'new') {
    openGenerateDialog()
  }
})
</script>

<style scoped>
.reconciliation-page {
  width: 100%;
  background: #FAF8F5;
  min-height: 100%;
  padding: 16px;
  box-sizing: border-box;
}

.overview-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.overview-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #E8E4DE;
  position: relative;
  overflow: hidden;
}

.overview-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
}

.overview-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
}

.overview-label {
  font-size: 13px;
  color: #8B95A5;
  margin-bottom: 10px;
  font-weight: 500;
}

.overview-value {
  font-size: 26px;
  font-weight: 700;
  margin-bottom: 8px;
  font-family: 'Georgia', serif;
}

.overview-sub {
  font-size: 12px;
  color: #A0A5AB;
}

.overview-red::before { background: linear-gradient(180deg, #C25555, #E8855A); }
.overview-red .overview-value { color: #C25555; }

.overview-orange::before { background: linear-gradient(180deg, #D4A853, #E8C76E); }
.overview-orange .overview-value { color: #B8860B; }

.overview-deepred::before { background: linear-gradient(180deg, #A63A3A, #C25555); }
.overview-deepred .overview-value { color: #A63A3A; }

.overview-blue::before { background: linear-gradient(180deg, #5A8C9E, #7AB0C4); }
.overview-blue .overview-value { color: #3A6B7E; }

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.page-header h2 {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
  color: #2D4A3E;
  letter-spacing: 1px;
}

.page-desc {
  font-size: 13px;
  color: #6b7280;
  margin: 0;
}

/* 未对账汇总卡片 */
.summary-section {
  margin-bottom: 20px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 2px;
  padding: 16px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #2D4A3E;
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 2px solid #C4A35A;
}

.title-icon {
  color: #C4A35A;
  font-size: 14px;
}

.summary-cards {
  display: flex;
  gap: 14px;
  overflow-x: auto;
  padding-bottom: 8px;
}

.summary-card {
  flex-shrink: 0;
  width: 240px;
  background: linear-gradient(135deg, #FAF8F5 0%, #f5f0e8 100%);
  border: 1px solid #C4A35A;
  border-radius: 2px;
  padding: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.summary-card:hover {
  border-color: #2D4A3E;
  box-shadow: 0 2px 8px rgba(45, 74, 62, 0.15);
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px dashed #C4A35A;
}

.supplier-name {
  font-size: 14px;
  font-weight: 600;
  color: #2D4A3E;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 180px;
}

.card-arrow {
  color: #C4A35A;
  font-size: 14px;
}

.card-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.card-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
}

.card-row .label {
  color: #6b7280;
}

.card-row .value {
  font-weight: 500;
}

.card-row .receipt {
  color: #2D4A3E;
}

.card-row .return {
  color: #dc2626;
}

.card-row.net {
  margin-top: 4px;
  padding-top: 8px;
  border-top: 1px solid #e5e7eb;
}

.net-value {
  font-size: 16px;
  font-weight: 700;
  color: #C4A35A;
}

.empty-cards {
  width: 100%;
  text-align: center;
  color: #9ca3af;
  padding: 30px 0;
  font-size: 14px;
}

/* 工具栏 */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 2px;
  padding: 12px 16px;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  gap: 10px;
  align-items: center;
}

/* 表格 */
.table-container {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 2px;
  padding: 16px;
}

.reconciliation-table {
  --el-table-border-color: #e5e7eb;
  --el-table-header-bg-color: #FAF8F5;
  --el-table-header-text-color: #2D4A3E;
  user-select: none;
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
}

.reconciliation-table * {
  user-select: none;
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
}

.reconciliation-table :deep(.row-overdue) {
  background-color: #FEF2F2 !important;
}

.reconciliation-table :deep(.row-overdue:hover > td) {
  background-color: #FEE2E2 !important;
}

.overdue-days {
  color: #DC2626;
  font-weight: 600;
  font-size: 12px;
}

:deep(.reconciliation-table .el-table__header th) {
  background-color: #FAF8F5 !important;
  color: #2D4A3E;
  font-weight: 600;
}

:deep(.reconciliation-table .el-table__row:hover > td) {
  background-color: #f9fafb;
}

.text-red {
  color: #dc2626;
}

.font-bold {
  font-weight: 600;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

/* 按钮样式 */
.btn-primary {
  background: #2D4A3E;
  border-color: #2D4A3E;
  border-radius: 2px;
  color: #fff;
}

.btn-primary:hover {
  background: #3d5c4f;
  border-color: #3d5c4f;
}

.btn-default {
  border-radius: 2px;
  border-color: #d1d5db;
  background: #fff;
  color: #374151;
}

.btn-default:hover {
  border-color: #C4A35A;
  color: #C4A35A;
}

.btn-gold {
  background: #C4A35A;
  border-color: #C4A35A;
  border-radius: 2px;
  color: #fff;
}

.btn-gold:hover {
  background: #d4b36a;
  border-color: #d4b36a;
}

.btn-primary-small {
  background: #2D4A3E;
  border-color: #2D4A3E;
  border-radius: 2px;
}

.btn-success-small {
  background: #059669;
  border-color: #059669;
  border-radius: 2px;
}

.btn-danger-small {
  background: #dc2626;
  border-color: #dc2626;
  border-radius: 2px;
}

.btn-warning-small {
  background: #d97706;
  border-color: #d97706;
  border-radius: 2px;
}

.btn-link {
  color: #2D4A3E;
  padding: 4px 0;
}

.btn-link:hover {
  color: #C4A35A;
}

.btn-link-primary {
  color: #2D4A3E;
}
.btn-link-primary:hover {
  color: #4A7C59;
}

.btn-link-success {
  color: #2D4A3E;
}
.btn-link-success:hover {
  color: #4A7C59;
}

.btn-link-danger {
  color: #C25555;
}
.btn-link-danger:hover {
  color: #A63A3A;
}

.btn-link-warning {
  color: #B8860B;
}
.btn-link-warning:hover {
  color: #D4A853;
}

.action-btns {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
}

/* 弹窗样式 */
:deep(.reconciliation-dialog .el-dialog) {
  border-radius: 16px;
  overflow: hidden;
}

:deep(.reconciliation-dialog .el-dialog__header) {
  background: linear-gradient(135deg, #FAF8F5 0%, #F5F2ED 100%);
  border-bottom: 2px solid #C4A35A;
  margin-right: 0 !important;
  margin-left: 0 !important;
  padding: 18px 24px !important;
  width: 100% !important;
  box-sizing: border-box !important;
}

:deep(.reconciliation-dialog .el-dialog__title) {
  color: #2D4A3E;
  font-weight: 600;
  font-size: 16px;
  letter-spacing: 1px;
  font-family: 'Noto Serif SC', serif;
}

:deep(.reconciliation-dialog .el-dialog__footer) {
  border-top: 1px solid #E8E4DE;
  padding: 14px 24px 18px !important;
  background: #FAF8F5;
  margin-right: 0 !important;
  margin-left: 0 !important;
  box-sizing: border-box !important;
  width: 100% !important;
}

:deep(.reconciliation-dialog .el-dialog__body) {
  padding: 24px !important;
}

/* 详情弹窗 */
:deep(.detail-dialog .el-dialog) {
  border-radius: 16px;
  overflow: hidden;
  width: 900px !important;
}

:deep(.detail-dialog .el-dialog__header) {
  background: linear-gradient(135deg, #FAF8F5 0%, #F5F2ED 100%);
  border-bottom: 2px solid #C4A35A;
  margin-right: 0 !important;
  margin-left: 0 !important;
  padding: 18px 24px !important;
  width: 100% !important;
  box-sizing: border-box !important;
}

:deep(.detail-dialog .el-dialog__title) {
  color: #2D4A3E;
  font-weight: 600;
  font-size: 16px;
  letter-spacing: 1px;
  font-family: 'Noto Serif SC', serif;
}

:deep(.detail-dialog .el-dialog__footer) {
  border-top: 1px solid #E8E4DE;
  padding: 14px 24px 18px !important;
  background: #FAF8F5;
  margin-right: 0 !important;
  margin-left: 0 !important;
  box-sizing: border-box !important;
  width: 100% !important;
}

:deep(.detail-dialog .el-dialog__body) {
  padding: 20px 24px !important;
}

.detail-content {
  padding: 0 4px;
}

.detail-header {
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #FAF8F5;
  border: 1px solid #e5e7eb;
  border-radius: 2px;
}

.detail-row {
  display: flex;
  gap: 24px;
  margin-bottom: 8px;
}

.detail-row:last-child {
  margin-bottom: 0;
}

.detail-item {
  flex: 1;
  font-size: 13px;
}

.detail-label {
  color: #6b7280;
}

.detail-value {
  color: #1f2937;
  font-weight: 500;
}

.amount-summary {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  padding: 16px;
  background: linear-gradient(135deg, #2D4A3E 0%, #3d5c4f 100%);
  border-radius: 2px;
}

.amount-item {
  flex: 1;
  text-align: center;
  padding: 8px;
}

.amount-label {
  display: block;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 6px;
}

.amount-value {
  display: block;
  font-size: 20px;
  font-weight: 700;
  color: #fff;
}

.amount-value.receipt {
  color: #86efac;
}

.amount-value.return {
  color: #fca5a5;
}

.amount-value.total {
  color: #fde68a;
  font-size: 24px;
}

.amount-value.paid {
  color: #93c5fd;
}

.amount-value.unpaid {
  color: #f87171;
  font-size: 24px;
}

.detail-tabs {
  margin-top: 16px;
}

:deep(.detail-tabs .el-tabs__item.is-active) {
  color: #2D4A3E;
}

:deep(.detail-tabs .el-tabs__active-bar) {
  background-color: #C4A35A;
}

:deep(.detail-tabs .el-tabs__nav-wrap::after) {
  background-color: #e5e7eb;
}

.detail-table {
  margin-top: 12px;
}

:deep(.detail-table .el-table__header th) {
  background-color: #FAF8F5 !important;
  color: #2D4A3E;
  font-weight: 600;
}

.payment-plan-header {
  margin-bottom: 12px;
}

/* Element Plus 覆盖样式 */
:deep(.el-input__wrapper),
:deep(.el-textarea__inner),
:deep(.el-select .el-input__wrapper),
:deep(.el-date-editor.el-input__wrapper),
:deep(.el-input-number) {
  border-radius: 2px;
}

:deep(.el-tag) {
  border-radius: 2px;
}

:deep(.el-button) {
  border-radius: 2px;
}

:deep(.el-pagination .el-pager li) {
  border-radius: 2px;
}

:deep(.el-pagination button) {
  border-radius: 2px;
}

:deep(.el-dialog) {
  border-radius: 2px;
}
</style>
