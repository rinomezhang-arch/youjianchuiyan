<template>
  <div class="approval-center">
    <!-- 顶部标题 -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">审批中心</h1>
        <p class="page-subtitle">Approval Center · 统一处理所有部门单据</p>
      </div>
      <div class="header-right">
        <div class="filter-group">
          <select v-model="statusFilter" class="filter-select">
            <option value="all">全部状态</option>
            <option value="pending">待处理</option>
            <option value="approved">已通过</option>
            <option value="rejected">已驳回</option>
          </select>
          <select v-model="departmentFilter" class="filter-select">
            <option value="all">全部门</option>
            <option value="ningguo">宁国店</option>
            <option value="xuancheng">宣城店</option>
            <option value="hangzhou">杭州店</option>
          </select>
        </div>
        <button class="refresh-btn" @click="refreshData">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="23 4 23 10 17 10"/>
            <polyline points="1 20 1 14 7 14"/>
            <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
          </svg>
          刷新
        </button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card pending" @click="filterByStatus('pending')">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <polyline points="12 6 12 12 16 14"/>
          </svg>
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ stats.pending }}</div>
          <div class="stat-label">待处理</div>
        </div>
        <div class="stat-arrow">→</div>
      </div>
      <div class="stat-card approved" @click="filterByStatus('approved')">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
            <polyline points="22 4 12 14.01 9 11.01"/>
          </svg>
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ stats.approved }}</div>
          <div class="stat-label">已通过</div>
        </div>
      </div>
      <div class="stat-card rejected" @click="filterByStatus('rejected')">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <line x1="15" y1="9" x2="9" y2="15"/>
            <line x1="9" y1="9" x2="15" y2="15"/>
          </svg>
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ stats.rejected }}</div>
          <div class="stat-label">已驳回</div>
        </div>
      </div>
      <div class="stat-card total">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="4" width="18" height="18" rx="2"/>
            <line x1="3" y1="10" x2="21" y2="10"/>
          </svg>
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ stats.total }}</div>
          <div class="stat-label">本月单据</div>
        </div>
      </div>
    </div>

    <!-- 审批类型标签页 -->
    <div class="type-tabs">
      <button 
        v-for="tab in typeTabs" 
        :key="tab.key"
        :class="['type-tab', { active: activeTab === tab.key }]"
        @click="activeTab = tab.key"
      >
        <span class="tab-icon" v-html="tab.icon"></span>
        <span class="tab-text">{{ tab.name }}</span>
        <span class="tab-badge" v-if="tab.count > 0">{{ tab.count }}</span>
      </button>
    </div>

    <!-- 审批列表区域 -->
    <div class="list-container">
      <!-- 采购审批列表 -->
      <div v-if="activeTab === 'procurement'" class="approval-list">
        <div class="list-header">
          <h3 class="list-title">采购申请列表</h3>
          <button class="export-btn">导出Excel</button>
        </div>
        <div class="table-wrapper">
          <table class="approval-table">
            <thead>
              <tr>
                <th>单据编号</th>
                <th>申请部门</th>
                <th>申请品类</th>
                <th>预估金额</th>
                <th>需求日期</th>
                <th>申请人</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in procurementList" :key="item.id">
                <td><span class="order-no">{{ item.no }}</span></td>
                <td>{{ item.department }}</td>
                <td>{{ item.category }}</td>
                <td class="amount">¥{{ item.amount }}</td>
                <td>{{ item.deliveryDate }}</td>
                <td>{{ item.applicant }}</td>
                <td><span class="status-tag" :class="item.status">{{ item.statusText }}</span></td>
                <td>
                  <div class="action-btns">
                    <button class="action-btn view" @click="viewDetail(item)">详情</button>
                    <button v-if="item.status === 'pending'" class="action-btn approve" @click="handleApprove(item)">通过</button>
                    <button v-if="item.status === 'pending'" class="action-btn reject" @click="openRejectDialog(item)">驳回</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-if="procurementList.length === 0" class="empty-state">
          <svg viewBox="0 0 24 24" fill="none" stroke="#ccc" stroke-width="2">
            <circle cx="11" cy="8" r="4"/>
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
          </svg>
          <span>暂无采购申请单据</span>
        </div>
      </div>

      <!-- 请假审批列表 -->
      <div v-if="activeTab === 'leave'" class="approval-list">
        <div class="list-header">
          <h3 class="list-title">员工请假列表</h3>
          <button class="export-btn">导出Excel</button>
        </div>
        <div class="table-wrapper">
          <table class="approval-table">
            <thead>
              <tr>
                <th>姓名</th>
                <th>部门</th>
                <th>职位</th>
                <th>请假类型</th>
                <th>请假时长</th>
                <th>请假日期</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in leaveList" :key="item.id">
                <td>{{ item.name }}</td>
                <td>{{ item.department }}</td>
                <td>{{ item.position }}</td>
                <td><span class="leave-type" :class="item.leaveType">{{ item.leaveTypeText }}</span></td>
                <td>{{ item.duration }}</td>
                <td>{{ item.dateRange }}</td>
                <td><span class="status-tag" :class="item.status">{{ item.statusText }}</span></td>
                <td>
                  <div class="action-btns">
                    <button class="action-btn view" @click="viewDetail(item)">详情</button>
                    <button v-if="item.status === 'pending'" class="action-btn approve" @click="handleApprove(item)">通过</button>
                    <button v-if="item.status === 'pending'" class="action-btn reject" @click="openRejectDialog(item)">驳回</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-if="leaveList.length === 0" class="empty-state">
          <svg viewBox="0 0 24 24" fill="none" stroke="#ccc" stroke-width="2">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
          </svg>
          <span>暂无请假申请</span>
        </div>
      </div>

      <!-- 报修审批列表 -->
      <div v-if="activeTab === 'repair'" class="approval-list">
        <div class="list-header">
          <h3 class="list-title">维修报修列表</h3>
          <button class="export-btn">导出Excel</button>
        </div>
        <div class="table-wrapper">
          <table class="approval-table">
            <thead>
              <tr>
                <th>单据编号</th>
                <th>申请门店</th>
                <th>设备类型</th>
                <th>维修费用</th>
                <th>紧急程度</th>
                <th>申请时间</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in repairList" :key="item.id">
                <td><span class="order-no">{{ item.no }}</span></td>
                <td>{{ item.store }}</td>
                <td>{{ item.deviceType }}</td>
                <td class="amount">¥{{ item.cost }}</td>
                <td><span class="priority-tag" :class="item.priority">{{ item.priorityText }}</span></td>
                <td>{{ item.applyTime }}</td>
                <td><span class="status-tag" :class="item.status">{{ item.statusText }}</span></td>
                <td>
                  <div class="action-btns">
                    <button class="action-btn view" @click="viewDetail(item)">详情</button>
                    <button v-if="item.status === 'pending'" class="action-btn approve" @click="handleApprove(item)">通过</button>
                    <button v-if="item.status === 'pending'" class="action-btn reject" @click="openRejectDialog(item)">驳回</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-if="repairList.length === 0" class="empty-state">
          <svg viewBox="0 0 24 24" fill="none" stroke="#ccc" stroke-width="2">
            <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/>
          </svg>
          <span>暂无维修报修申请</span>
        </div>
      </div>

      <!-- 费用报销列表 -->
      <div v-if="activeTab === 'expense'" class="approval-list">
        <div class="list-header">
          <h3 class="list-title">费用报销列表</h3>
          <button class="export-btn">导出Excel</button>
        </div>
        <div class="table-wrapper">
          <table class="approval-table">
            <thead>
              <tr>
                <th>单据编号</th>
                <th>申请人</th>
                <th>部门</th>
                <th>费用类型</th>
                <th>报销金额</th>
                <th>申请时间</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in expenseList" :key="item.id">
                <td><span class="order-no">{{ item.no }}</span></td>
                <td>{{ item.applicant }}</td>
                <td>{{ item.department }}</td>
                <td>{{ item.expenseType }}</td>
                <td class="amount">¥{{ item.amount }}</td>
                <td>{{ item.applyTime }}</td>
                <td><span class="status-tag" :class="item.status">{{ item.statusText }}</span></td>
                <td>
                  <div class="action-btns">
                    <button class="action-btn view" @click="viewDetail(item)">详情</button>
                    <button v-if="item.status === 'pending'" class="action-btn approve" @click="handleApprove(item)">通过</button>
                    <button v-if="item.status === 'pending'" class="action-btn reject" @click="openRejectDialog(item)">驳回</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-if="expenseList.length === 0" class="empty-state">
          <svg viewBox="0 0 24 24" fill="none" stroke="#ccc" stroke-width="2">
            <line x1="12" y1="1" x2="12" y2="23"/>
            <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
          </svg>
          <span>暂无费用报销申请</span>
        </div>
      </div>

      <!-- 供应商对账列表 -->
      <div v-if="activeTab === 'reconciliation'" class="approval-list">
        <div class="list-header">
          <h3 class="list-title">供应商对账列表</h3>
          <button class="export-btn">导出Excel</button>
        </div>
        <div class="table-wrapper">
          <table class="approval-table">
            <thead>
              <tr>
                <th>供应商名称</th>
                <th>对账月份</th>
                <th>送货次数</th>
                <th>对账金额</th>
                <th>已核对数量</th>
                <th>未核对数量</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in reconciliationList" :key="item.id">
                <td>{{ item.supplierName }}</td>
                <td>{{ item.month }}</td>
                <td>{{ item.deliveryCount }}</td>
                <td class="amount">¥{{ item.totalAmount }}</td>
                <td>{{ item.checkedCount }}</td>
                <td class="warning">{{ item.uncheckedCount }}</td>
                <td><span class="status-tag" :class="item.status">{{ item.statusText }}</span></td>
                <td>
                  <div class="action-btns">
                    <button class="action-btn view" @click="viewDetail(item)">详情</button>
                    <button v-if="item.status === 'pending'" class="action-btn approve" @click="handleApprove(item)">确认</button>
                    <button v-if="item.status === 'pending'" class="action-btn reject" @click="openRejectDialog(item)">驳回</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-if="reconciliationList.length === 0" class="empty-state">
          <svg viewBox="0 0 24 24" fill="none" stroke="#ccc" stroke-width="2">
            <path d="M20.42 4.58a5.4 5.4 0 0 0-7.65 0l-.77.78-.77-.78a5.4 5.4 0 0 0-7.65 0C1.46 6.7 1.33 10.28 4 13l8 8 8-8c2.67-2.72 2.54-6.3.42-8.42z"/>
          </svg>
          <span>暂无供应商对账单</span>
        </div>
      </div>
    </div>

    <!-- 驳回弹窗 -->
    <div v-if="showRejectDialog" class="modal-overlay" @click.self="closeRejectDialog">
      <div class="modal-dialog">
        <div class="modal-header">
          <h3 class="modal-title">驳回原因</h3>
          <button class="modal-close" @click="closeRejectDialog">×</button>
        </div>
        <div class="modal-body">
          <div class="reject-form">
            <label class="form-label">驳回原因 <span class="required">*</span></label>
            <textarea v-model="rejectReason" class="form-textarea" rows="4" placeholder="请输入驳回原因..."></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="modal-btn btn-cancel" @click="closeRejectDialog">取消</button>
          <button class="modal-btn btn-confirm" @click="confirmReject" :disabled="!rejectReason.trim()">确认驳回</button>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <div v-if="showDetailDialog" class="modal-overlay" @click.self="closeDetailDialog">
      <div class="modal-dialog detail-modal">
        <div class="modal-header">
          <h3 class="modal-title">{{ detailData?.title || '单据详情' }}</h3>
          <button class="modal-close" @click="closeDetailDialog">×</button>
        </div>
        <div class="modal-body">
          <div class="detail-content">
            <div class="detail-section">
              <h4 class="section-title">基本信息</h4>
              <div class="info-grid">
                <div class="info-item">
                  <span class="info-label">单据编号</span>
                  <span class="info-value">{{ detailData?.no || '-' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">申请部门</span>
                  <span class="info-value">{{ detailData?.department || '-' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">申请人</span>
                  <span class="info-value">{{ detailData?.applicant || '-' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">申请时间</span>
                  <span class="info-value">{{ detailData?.applyTime || '-' }}</span>
                </div>
              </div>
            </div>
            <div class="detail-section">
              <h4 class="section-title">申请内容</h4>
              <div class="content-text">{{ detailData?.description || '-' }}</div>
            </div>
            <div class="detail-section" v-if="detailData?.amount">
              <h4 class="section-title">金额信息</h4>
              <div class="amount-info">
                <span class="amount-label">预估金额</span>
                <span class="amount-value">¥{{ detailData.amount }}</span>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="modal-btn btn-close" @click="closeDetailDialog">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

const activeTab = ref('procurement')
const statusFilter = ref('all')
const departmentFilter = ref('all')
const showRejectDialog = ref(false)
const showDetailDialog = ref(false)
const rejectReason = ref('')
const rejectTarget = ref(null)
const detailData = ref(null)

// 审批类型标签页
const typeTabs = ref([
  { key: 'procurement', name: '采购申请', icon: '<circle cx="11" cy="8" r="4"/><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>', count: 4 },
  { key: 'leave', name: '员工请假', icon: '<rect x="3" y="4" width="18" height="18" rx="2"/><line x1="3" y1="10" x2="21" y2="10"/>', count: 3 },
  { key: 'repair', name: '维修报修', icon: '<path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/>', count: 2 },
  { key: 'expense', name: '费用报销', icon: '<line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>', count: 2 },
  { key: 'reconciliation', name: '供应商对账', icon: '<path d="M20.42 4.58a5.4 5.4 0 0 0-7.65 0l-.77.78-.77-.78a5.4 5.4 0 0 0-7.65 0C1.46 6.7 1.33 10.28 4 13l8 8 8-8c2.67-2.72 2.54-6.3.42-8.42z"/>', count: 1 },
])

// 统计数据
const stats = computed(() => ({
  pending: typeTabs.value.reduce((sum, tab) => sum + tab.count, 0),
  approved: 28,
  rejected: 5,
  total: 49
}))

// 采购申请列表
const procurementList = ref([
  { id: 1, no: 'CG-20260727-001', department: '宁国店后厨', category: '蔬菜类', amount: '3,500', deliveryDate: '2026-07-28', applicant: '王大厨', status: 'pending', statusText: '待审批', description: '申请采购各类蔬菜，用于日常菜品制作' },
  { id: 2, no: 'CG-20260727-002', department: '宣城店前厅', category: '白酒', amount: '8,200', deliveryDate: '2026-07-29', applicant: '李经理', status: 'pending', statusText: '待审批', description: '申请采购高端白酒，用于宴会接待' },
  { id: 3, no: 'CG-20260726-005', department: '杭州店后厨', category: '海鲜类', amount: '12,800', deliveryDate: '2026-07-27', applicant: '张主厨', status: 'approved', statusText: '已通过', description: '申请采购海鲜食材' },
  { id: 4, no: 'CG-20260725-003', department: '宁国店后厨', category: '肉类', amount: '5,600', deliveryDate: '2026-07-26', applicant: '王大厨', status: 'rejected', statusText: '已驳回', description: '申请采购各类肉类' },
])

// 请假列表
const leaveList = ref([
  { id: 1, name: '张三', department: '宁国店前厅', position: '服务员', leaveType: 'personal', leaveTypeText: '事假', duration: '1天', dateRange: '2026-07-28', status: 'pending', statusText: '待审批', description: '家中有事需请假一天' },
  { id: 2, name: '李四', department: '宣城店后厨', position: '厨师', leaveType: 'sick', leaveTypeText: '病假', duration: '3天', dateRange: '2026-07-28 ~ 2026-07-30', status: 'pending', statusText: '待审批', description: '身体不适需休息治疗' },
  { id: 3, name: '王五', department: '杭州店行政', position: '文员', leaveType: 'compensate', leaveTypeText: '调休', duration: '1天', dateRange: '2026-07-29', status: 'approved', statusText: '已通过', description: '加班调休一天' },
])

// 维修报修列表
const repairList = ref([
  { id: 1, no: 'WX-20260727-001', store: '杭州店', deviceType: '空调', cost: '1,500', priority: 'high', priorityText: '紧急', applyTime: '2026-07-27 09:30', status: 'pending', statusText: '待审批', description: '大厅空调不制冷，需要维修' },
  { id: 2, no: 'WX-20260726-002', store: '宁国店', deviceType: '油烟机', cost: '800', priority: 'medium', priorityText: '一般', applyTime: '2026-07-26 14:20', status: 'pending', statusText: '待审批', description: '后厨油烟机需要清洗维护' },
])

// 费用报销列表
const expenseList = ref([
  { id: 1, no: 'BX-20260727-001', applicant: '赵六', department: '宁国店行政', expenseType: '办公用品', amount: '680', applyTime: '2026-07-27 10:00', status: 'pending', statusText: '待审批', description: '采购办公用品一批' },
  { id: 2, no: 'BX-20260726-002', applicant: '钱七', department: '宣城店营销', expenseType: '营销费用', amount: '2,300', applyTime: '2026-07-26 15:30', status: 'pending', statusText: '待审批', description: '促销活动物料费用' },
])

// 供应商对账列表
const reconciliationList = ref([
  { id: 1, supplierName: '鑫源食品', month: '2026年07月', deliveryCount: 12, totalAmount: '28,500', checkedCount: 10, uncheckedCount: 2, status: 'pending', statusText: '待确认', description: '月度送货对账' },
  { id: 2, supplierName: '海味鲜', month: '2026年07月', deliveryCount: 8, totalAmount: '15,600', checkedCount: 8, uncheckedCount: 0, status: 'approved', statusText: '已确认', description: '海鲜供应商对账' },
])

function filterByStatus(status) {
  statusFilter.value = status
}

function refreshData() {
  ElMessage.success('数据已刷新')
}

function viewDetail(item) {
  detailData.value = item
  showDetailDialog.value = true
}

function closeDetailDialog() {
  showDetailDialog.value = false
  detailData.value = null
}

function openRejectDialog(item) {
  rejectTarget.value = item
  rejectReason.value = ''
  showRejectDialog.value = true
}

function closeRejectDialog() {
  showRejectDialog.value = false
  rejectTarget.value = null
  rejectReason.value = ''
}

function confirmReject() {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请输入驳回原因')
    return
  }
  
  ElMessage.success(`已驳回单据 ${rejectTarget.value.no || rejectTarget.value.name}`)
  closeRejectDialog()
}

function handleApprove(item) {
  ElMessage.success(`已通过单据 ${item.no || item.name}`)
}
</script>

<style scoped>
.approval-center {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: #2D4A3E;
  margin: 0;
}

.page-subtitle {
  font-size: 14px;
  color: #7a8c84;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.filter-group {
  display: flex;
  gap: 12px;
}

.filter-select {
  padding: 8px 16px;
  border: 1px solid #e8edea;
  border-radius: 8px;
  font-size: 14px;
  background: #fff;
  cursor: pointer;
}

.refresh-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  font-size: 14px;
  color: #2D4A3E;
  background: rgba(45,74,62,0.06);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.refresh-btn:hover {
  background: rgba(45,74,62,0.12);
}

.refresh-btn svg {
  width: 18px;
  height: 18px;
}

/* 统计卡片 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  border: 1px solid #e8edea;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.06);
}

.stat-card.pending {
  border-left: 4px solid #E6A23C;
}

.stat-card.approved {
  border-left: 4px solid #67C23A;
}

.stat-card.rejected {
  border-left: 4px solid #F56C6C;
}

.stat-card.total {
  border-left: 4px solid #5B7B8A;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-card.pending .stat-icon {
  background: rgba(230,162,60,0.1);
  color: #E6A23C;
}

.stat-card.approved .stat-icon {
  background: rgba(103,194,58,0.1);
  color: #67C23A;
}

.stat-card.rejected .stat-icon {
  background: rgba(245,108,108,0.1);
  color: #F56C6C;
}

.stat-card.total .stat-icon {
  background: rgba(91,123,138,0.1);
  color: #5B7B8A;
}

.stat-icon svg {
  width: 24px;
  height: 24px;
}

.stat-info {
  flex: 1;
}

.stat-num {
  font-size: 28px;
  font-weight: 700;
  color: #2D4A3E;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 13px;
  color: #999;
}

.stat-arrow {
  font-size: 20px;
  color: #ccc;
}

/* 类型标签页 */
.type-tabs {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.type-tab {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  font-size: 14px;
  color: #7a8c84;
  background: #fff;
  border: 1px solid #e8edea;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.type-tab:hover {
  border-color: #2D4A3E;
  color: #2D4A3E;
}

.type-tab.active {
  background: #2D4A3E;
  color: #fff;
  border-color: #2D4A3E;
}

.tab-icon {
  width: 20px;
  height: 20px;
}

.tab-icon svg {
  width: 20px;
  height: 20px;
}

.tab-text {
  font-weight: 600;
}

.tab-badge {
  padding: 2px 10px;
  background: #E6A23C;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  border-radius: 12px;
}

.type-tab.active .tab-badge {
  background: #fff;
  color: #2D4A3E;
}

/* 列表区域 */
.list-container {
  background: #fff;
  border-radius: 16px;
  border: 1px solid #e8edea;
  overflow: hidden;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #f0f2f1;
}

.list-title {
  font-size: 16px;
  font-weight: 700;
  color: #2D4A3E;
  margin: 0;
}

.export-btn {
  padding: 8px 16px;
  font-size: 13px;
  color: #2D4A3E;
  background: rgba(45,74,62,0.06);
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

.table-wrapper {
  overflow-x: auto;
}

.approval-table {
  width: 100%;
  border-collapse: collapse;
}

.approval-table th,
.approval-table td {
  padding: 14px 16px;
  text-align: left;
  font-size: 14px;
}

.approval-table th {
  background: #fafbfb;
  color: #7a8c84;
  font-weight: 600;
}

.approval-table tbody tr {
  border-bottom: 1px solid #f0f2f1;
}

.approval-table tbody tr:hover {
  background: #fafbfb;
}

.order-no {
  font-family: monospace;
  font-weight: 600;
  color: #2D4A3E;
}

.amount {
  font-weight: 700;
  color: #2D4A3E;
}

.status-tag {
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
}

.status-tag.pending {
  background: rgba(230,162,60,0.1);
  color: #E6A23C;
}

.status-tag.approved {
  background: rgba(103,194,58,0.1);
  color: #67C23A;
}

.status-tag.rejected {
  background: rgba(245,108,108,0.1);
  color: #F56C6C;
}

.action-btns {
  display: flex;
  gap: 8px;
}

.action-btn {
  padding: 6px 12px;
  font-size: 12px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn.view {
  background: #f5f7f6;
  color: #7a8c84;
}

.action-btn.view:hover {
  background: #e8edea;
}

.action-btn.approve {
  background: #2D4A3E;
  color: #fff;
}

.action-btn.approve:hover {
  background: #1D3A2E;
}

.action-btn.reject {
  background: rgba(245,108,108,0.1);
  color: #F56C6C;
}

.action-btn.reject:hover {
  background: rgba(245,108,108,0.2);
}

.leave-type {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
}

.leave-type.personal {
  background: rgba(230,162,60,0.1);
  color: #E6A23C;
}

.leave-type.sick {
  background: rgba(245,108,108,0.1);
  color: #F56C6C;
}

.leave-type.compensate {
  background: rgba(103,194,58,0.1);
  color: #67C23A;
}

.priority-tag {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.priority-tag.high {
  background: rgba(245,108,108,0.1);
  color: #F56C6C;
}

.priority-tag.medium {
  background: rgba(230,162,60,0.1);
  color: #E6A23C;
}

.priority-tag.low {
  background: rgba(103,194,58,0.1);
  color: #67C23A;
}

.warning {
  color: #E6A23C;
  font-weight: 600;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
  gap: 12px;
}

.empty-state svg {
  width: 50px;
  height: 50px;
}

.empty-state span {
  font-size: 14px;
  color: #999;
}

/* 弹窗 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-dialog {
  background: #fff;
  border-radius: 16px;
  padding: 0;
  width: 480px;
  max-width: 90vw;
  box-shadow: 0 20px 60px rgba(0,0,0,0.15);
}

.modal-dialog.detail-modal {
  width: 600px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #f0f2f1;
}

.modal-title {
  font-size: 18px;
  font-weight: 700;
  color: #2D4A3E;
  margin: 0;
}

.modal-close {
  width: 32px;
  height: 32px;
  background: #f5f7f6;
  border: none;
  border-radius: 8px;
  font-size: 20px;
  color: #7a8c84;
  cursor: pointer;
}

.modal-body {
  padding: 24px;
}

.reject-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 14px;
  font-weight: 600;
  color: #2D4A3E;
}

.required {
  color: #F56C6C;
}

.form-textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #e8edea;
  border-radius: 8px;
  font-size: 14px;
  resize: vertical;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.detail-section {
  background: #fafbfb;
  border-radius: 12px;
  padding: 16px;
}

.section-title {
  font-size: 14px;
  font-weight: 700;
  color: #2D4A3E;
  margin-bottom: 12px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 12px;
  color: #999;
}

.info-value {
  font-size: 14px;
  font-weight: 500;
  color: #2D4A3E;
}

.content-text {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
}

.amount-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.amount-label {
  font-size: 14px;
  color: #666;
}

.amount-value {
  font-size: 24px;
  font-weight: 700;
  color: #2D4A3E;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 20px 24px;
  border-top: 1px solid #f0f2f1;
}

.modal-btn {
  padding: 10px 24px;
  font-size: 14px;
  font-weight: 500;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel {
  background: #f5f7f6;
  color: #7a8c84;
}

.btn-cancel:hover {
  background: #e8edea;
}

.btn-confirm {
  background: #F56C6C;
  color: #fff;
}

.btn-confirm:hover:not(:disabled) {
  background: #E55555;
}

.btn-confirm:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-close {
  background: #2D4A3E;
  color: #fff;
}

.btn-close:hover {
  background: #1D3A2E;
}

/* 响应式 */
@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: 1fr;
  }
  .type-tabs {
    flex-direction: column;
  }
}
</style>