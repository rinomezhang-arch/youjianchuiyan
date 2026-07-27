<template>
  <div class="engineering-page">
    <div class="page-header">
      <h2 class="page-title">工程管理 · Engineering Management</h2>
      <p class="page-subtitle">工单全生命周期追踪 · Work Order Lifecycle</p>
      <div class="header-actions">
        <button class="btn-primary" @click="showCreateModal = true">
          <span>新建工单</span>
        </button>
        <button class="btn-secondary" @click="goTo('/dashboard/gm-office')">
          <span>总经办</span>
        </button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon pending">
          <span class="stat-number">{{ stats.pending }}</span>
        </div>
        <div class="stat-content">
          <div class="stat-label">待受理</div>
          <div class="stat-sub">等待工程部接单</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon processing">
          <span class="stat-number">{{ stats.processing }}</span>
        </div>
        <div class="stat-content">
          <div class="stat-label">进行中</div>
          <div class="stat-sub">工程部处理中</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon completed">
          <span class="stat-number">{{ stats.completed }}</span>
        </div>
        <div class="stat-content">
          <div class="stat-label">已完成</div>
          <div class="stat-sub">本月完成工单</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon escalated">
          <span class="stat-number">{{ stats.escalated }}</span>
        </div>
        <div class="stat-content">
          <div class="stat-label">已升级</div>
          <div class="stat-sub">待总经办审批</div>
        </div>
      </div>
    </div>

    <!-- 状态流转说明 -->
    <div class="workflow-guide">
      <div class="workflow-step">
        <span class="step-dot pending"></span>
        <span class="step-text">部门下工单</span>
      </div>
      <span class="workflow-arrow">→</span>
      <div class="workflow-step">
        <span class="step-dot processing"></span>
        <span class="step-text">工程受理</span>
      </div>
      <span class="workflow-arrow">→</span>
      <div class="workflow-step">
        <span class="step-dot processing"></span>
        <span class="step-text">开始施工</span>
      </div>
      <span class="workflow-arrow">→</span>
      <div class="workflow-step">
        <span class="step-dot processing"></span>
        <span class="step-text">进度跟踪</span>
      </div>
      <span class="workflow-arrow">→</span>
      <div class="workflow-step">
        <span class="step-dot completed"></span>
        <span class="step-text">完成</span>
      </div>
      <span class="workflow-arrow or">或</span>
      <div class="workflow-step">
        <span class="step-dot escalated"></span>
        <span class="step-text">升级总经办</span>
      </div>
    </div>

    <!-- 工单看板 -->
    <div class="kanban-container">
      <!-- 待受理 -->
      <div class="kanban-column">
        <div class="column-header pending">
          <div class="column-title">
            <span class="column-badge">01</span>
            <span>待受理</span>
          </div>
          <span class="column-count">{{ ordersByStatus.pending.length }}</span>
        </div>
        <div class="column-content">
          <div v-for="order in ordersByStatus.pending" :key="order.id" class="work-card" @click="openOrderDetail(order)">
            <div class="card-badge-group">
              <span class="priority-tag" :class="order.priority">{{ priorityText(order.priority) }}</span>
              <span class="dept-tag">{{ order.department }}</span>
            </div>
            <div class="card-title">{{ order.title }}</div>
            <div class="card-info">
              <span class="info-item">📍 {{ order.location }}</span>
              <span class="info-item">👤 {{ order.requester }}</span>
            </div>
            <div class="card-time">{{ order.createdAt }}</div>
            <div class="card-actions">
              <button class="btn-accept" @click.stop="acceptOrder(order)">受理</button>
              <button class="btn-detail" @click.stop="openOrderDetail(order)">详情</button>
            </div>
          </div>
        </div>
      </div>

      <!-- 进行中 -->
      <div class="kanban-column">
        <div class="column-header processing">
          <div class="column-title">
            <span class="column-badge">02</span>
            <span>进行中</span>
          </div>
          <span class="column-count">{{ ordersByStatus.processing.length }}</span>
        </div>
        <div class="column-content">
          <div v-for="order in ordersByStatus.processing" :key="order.id" class="work-card" @click="openOrderDetail(order)">
            <div class="card-badge-group">
              <span class="priority-tag" :class="order.priority">{{ priorityText(order.priority) }}</span>
              <span class="assignee-tag">执行人: {{ order.assignee }}</span>
            </div>
            <div class="card-title">{{ order.title }}</div>
            <div class="card-progress-section">
              <div class="progress-bar-wrapper">
                <div class="progress-bar">
                  <div class="progress-fill" :style="{ width: order.progress + '%' }"></div>
                </div>
                <span class="progress-percent">{{ order.progress }}%</span>
              </div>
              <div class="progress-meta">
                <span>预计: {{ order.expectedDate }}</span>
              </div>
            </div>
            <div class="card-info">
              <span class="info-item">📍 {{ order.location }}</span>
              <span class="info-item">{{ order.updatedAt }}</span>
            </div>
            <div class="card-actions">
              <button class="btn-progress" @click.stop="updateProgress(order)">更新进度</button>
              <button class="btn-complete" @click.stop="completeOrder(order)">完成</button>
              <button class="btn-escalate" @click.stop="escalateOrder(order)">升级</button>
            </div>
          </div>
        </div>
      </div>

      <!-- 已完成 -->
      <div class="kanban-column">
        <div class="column-header completed">
          <div class="column-title">
            <span class="column-badge">03</span>
            <span>已完成</span>
          </div>
          <span class="column-count">{{ ordersByStatus.completed.length }}</span>
        </div>
        <div class="column-content">
          <div v-for="order in ordersByStatus.completed" :key="order.id" class="work-card completed-card">
            <div class="card-badge-group">
              <span class="status-tag completed">已完成</span>
            </div>
            <div class="card-title">{{ order.title }}</div>
            <div class="card-info">
              <span class="info-item">{{ order.department }} · {{ order.location }}</span>
              <span class="info-item">执行人: {{ order.assignee }}</span>
            </div>
            <div class="card-time">完成时间: {{ order.completedAt }}</div>
          </div>
        </div>
      </div>

      <!-- 已升级 -->
      <div class="kanban-column">
        <div class="column-header escalated">
          <div class="column-title">
            <span class="column-badge">04</span>
            <span>已升级</span>
          </div>
          <span class="column-count">{{ ordersByStatus.escalated.length }}</span>
        </div>
        <div class="column-content">
          <div v-for="order in ordersByStatus.escalated" :key="order.id" class="work-card escalated-card">
            <div class="card-badge-group">
              <span class="status-tag escalated">待总经办</span>
              <span class="priority-tag high">紧急</span>
            </div>
            <div class="card-title">{{ order.title }}</div>
            <div class="card-escalate-reason">
              <span class="reason-label">升级原因:</span>
              <span class="reason-text">{{ order.escalateReason }}</span>
            </div>
            <div class="card-info">
              <span class="info-item">{{ order.department }} · {{ order.location }}</span>
              <span class="info-item">申请人: {{ order.requester }}</span>
            </div>
            <div class="card-time">升级时间: {{ order.escalatedAt }}</div>
            <div class="card-actions">
              <button class="btn-gm" @click.stop="goTo('/dashboard/gm-office')">查看总经办</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 新建工单弹窗 -->
    <div v-if="showCreateModal" class="modal-overlay" @click="showCreateModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>新建工单</h3>
          <button class="close-btn" @click="showCreateModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="form-row">
            <label>工单标题</label>
            <input type="text" v-model="newOrder.title" placeholder="请输入工单标题" class="form-input"/>
          </div>
          <div class="form-row">
            <label>所属部门</label>
            <select v-model="newOrder.department" class="form-select">
              <option value="前厅部">前厅部</option>
              <option value="后厨">后厨</option>
              <option value="后勤部">后勤部</option>
              <option value="安保部">安保部</option>
              <option value="IT部">IT部</option>
            </select>
          </div>
          <div class="form-row">
            <label>位置</label>
            <input type="text" v-model="newOrder.location" placeholder="请输入位置" class="form-input"/>
          </div>
          <div class="form-row">
            <label>优先级</label>
            <select v-model="newOrder.priority" class="form-select">
              <option value="high">紧急</option>
              <option value="medium">中等</option>
              <option value="low">一般</option>
            </select>
          </div>
          <div class="form-row">
            <label>详细描述</label>
            <textarea v-model="newOrder.description" placeholder="请输入详细描述..." class="form-textarea"></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="showCreateModal = false">取消</button>
          <button class="btn-confirm" @click="submitNewOrder">提交</button>
        </div>
      </div>
    </div>

    <!-- 进度更新弹窗 -->
    <div v-if="showProgressModal" class="modal-overlay" @click="showProgressModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>更新进度 - {{ currentOrder?.title }}</h3>
          <button class="close-btn" @click="showProgressModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="form-row">
            <label>当前进度</label>
            <input type="range" v-model="progressValue" min="0" max="100" class="progress-slider"/>
            <span class="progress-value-display">{{ progressValue }}%</span>
          </div>
          <div class="form-row">
            <label>进度备注</label>
            <textarea v-model="progressRemark" placeholder="输入进度备注..." class="form-textarea"></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="showProgressModal = false">取消</button>
          <button class="btn-confirm" @click="submitProgress">确认更新</button>
        </div>
      </div>
    </div>

    <!-- 升级原因弹窗 -->
    <div v-if="showEscalateModal" class="modal-overlay" @click="showEscalateModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>升级工单 - {{ currentOrder?.title }}</h3>
          <button class="close-btn" @click="showEscalateModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="form-row">
            <label>升级原因</label>
            <select v-model="escalateReason" class="form-select">
              <option value="">请选择升级原因</option>
              <option value="预算超支需审批">预算超支需审批</option>
              <option value="技术难题无法解决">技术难题无法解决</option>
              <option value="工期延误需协调">工期延误需协调</option>
              <option value="资源不足需调配">资源不足需调配</option>
              <option value="其他原因">其他原因</option>
            </select>
          </div>
          <div class="form-row">
            <label>详细说明</label>
            <textarea v-model="escalateDetail" placeholder="请输入详细说明..." class="form-textarea"></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="showEscalateModal = false">取消</button>
          <button class="btn-escalate-confirm" @click="submitEscalate">确认升级</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const goTo = (path) => {
  router.push(path)
}

const stats = ref({
  pending: 3,
  processing: 5,
  completed: 12,
  escalated: 1,
})

const orders = ref([
  // 待受理
  { id: 1, orderNo: 'WO20260727001', title: '大厅空调制冷异常', department: '前厅部', location: '大厅', priority: 'high', requester: '张经理', createdAt: '07-27 09:30', status: 'pending', assignee: '', progress: 0, description: '' },
  { id: 2, orderNo: 'WO20260727002', title: '厨房排烟风机异响', department: '后厨', location: '厨房', priority: 'high', requester: '李厨师长', createdAt: '07-27 10:15', status: 'pending', assignee: '', progress: 0, description: '' },
  { id: 3, orderNo: 'WO20260727003', title: '卫生间水龙头更换', department: '后勤部', location: '2F卫生间', priority: 'low', requester: '王主管', createdAt: '07-27 11:00', status: 'pending', assignee: '', progress: 0, description: '' },
  // 进行中
  { id: 4, orderNo: 'WO20260726001', title: '包厢灯具更换', department: '前厅部', location: 'VIP包厢', priority: 'medium', requester: '张经理', createdAt: '07-26 14:00', status: 'processing', assignee: '刘工', progress: 60, expectedDate: '07-28', updatedAt: '07-27 14:00', description: '' },
  { id: 5, orderNo: 'WO20260726002', title: '后厨洗碗机维修', department: '后厨', location: '后厨', priority: 'high', requester: '李厨师长', createdAt: '07-26 09:00', status: 'processing', assignee: '陈工', progress: 30, expectedDate: '07-29', updatedAt: '07-27 10:00', description: '' },
  { id: 6, orderNo: 'WO20260725001', title: '监控系统升级', department: '安保部', location: '全店', priority: 'medium', requester: '赵队长', createdAt: '07-25 10:00', status: 'processing', assignee: '刘工', progress: 80, expectedDate: '07-27', updatedAt: '07-27 09:00', description: '' },
  { id: 7, orderNo: 'WO20260725002', title: '网络设备检修', department: 'IT部', location: '机房', priority: 'low', requester: '孙技术员', createdAt: '07-25 15:00', status: 'processing', assignee: '陈工', progress: 45, expectedDate: '07-28', updatedAt: '07-27 11:00', description: '' },
  { id: 8, orderNo: 'WO20260724001', title: '停车场照明改造', department: '后勤部', location: '停车场', priority: 'medium', requester: '王主管', createdAt: '07-24 08:30', status: 'processing', assignee: '刘工', progress: 20, expectedDate: '07-30', updatedAt: '07-27 08:30', description: '' },
  // 已完成
  { id: 9, orderNo: 'WO20260723001', title: '办公室空调清洗', department: '后勤部', location: '办公室', priority: 'low', requester: '王主管', createdAt: '07-23', status: 'completed', assignee: '陈工', completedAt: '07-24', description: '' },
  { id: 10, orderNo: 'WO20260722001', title: '宴会厅音响调试', department: '前厅部', location: '宴会厅', priority: 'high', requester: '张经理', createdAt: '07-22', status: 'completed', assignee: '刘工', completedAt: '07-23', description: '' },
  { id: 11, orderNo: 'WO20260721001', title: '冷藏库温度校准', department: '后厨', location: '冷藏库', priority: 'high', requester: '李厨师长', createdAt: '07-21', status: 'completed', assignee: '陈工', completedAt: '07-22', description: '' },
  // 已升级
  { id: 12, orderNo: 'WO20260720001', title: '消防系统改造', department: '安保部', location: '全店', priority: 'high', requester: '赵队长', createdAt: '07-20', status: 'escalated', assignee: '', escalateReason: '预算超支需审批', escalatedAt: '07-26', description: '' },
])

const ordersByStatus = computed(() => ({
  pending: orders.value.filter(o => o.status === 'pending'),
  processing: orders.value.filter(o => o.status === 'processing'),
  completed: orders.value.filter(o => o.status === 'completed'),
  escalated: orders.value.filter(o => o.status === 'escalated'),
}))

const priorityText = (p) => ({ high: '紧急', medium: '中等', low: '一般' }[p] || p)

// 新建工单
const showCreateModal = ref(false)
const newOrder = ref({
  title: '',
  department: '前厅部',
  location: '',
  priority: 'medium',
  description: '',
})

const submitNewOrder = () => {
  if (!newOrder.value.title || !newOrder.value.location) {
    alert('请填写完整信息')
    return
  }
  const id = orders.value.length + 1
  const now = new Date()
  const dateStr = `${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
  orders.value.unshift({
    id,
    orderNo: `WO${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}${String(id).padStart(3, '0')}`,
    title: newOrder.value.title,
    department: newOrder.value.department,
    location: newOrder.value.location,
    priority: newOrder.value.priority,
    requester: '当前用户',
    createdAt: dateStr,
    status: 'pending',
    assignee: '',
    progress: 0,
    description: newOrder.value.description,
  })
  stats.value.pending++
  showCreateModal.value = false
  newOrder.value = { title: '', department: '前厅部', location: '', priority: 'medium', description: '' }
}

// 进度更新
const currentOrder = ref(null)
const showProgressModal = ref(false)
const progressValue = ref(0)
const progressRemark = ref('')

const updateProgress = (order) => {
  currentOrder.value = order
  progressValue.value = order.progress
  progressRemark.value = ''
  showProgressModal.value = true
}

const submitProgress = () => {
  if (currentOrder.value) {
    currentOrder.value.progress = progressValue.value
    currentOrder.value.updatedAt = new Date().toLocaleString()
    if (progressValue.value >= 100) {
      currentOrder.value.status = 'completed'
      currentOrder.value.completedAt = new Date().toLocaleDateString()
      stats.value.processing--
      stats.value.completed++
    }
  }
  showProgressModal.value = false
}

// 工单操作
const acceptOrder = (order) => {
  order.status = 'processing'
  order.assignee = '刘工'
  order.progress = 10
  order.expectedDate = new Date(Date.now() + 2 * 24 * 60 * 60 * 1000).toLocaleDateString().slice(5)
  order.updatedAt = new Date().toLocaleString()
  stats.value.pending--
  stats.value.processing++
}

const completeOrder = (order) => {
  order.status = 'completed'
  order.progress = 100
  order.completedAt = new Date().toLocaleDateString()
  stats.value.processing--
  stats.value.completed++
}

// 升级工单
const showEscalateModal = ref(false)
const escalateReason = ref('')
const escalateDetail = ref('')

const escalateOrder = (order) => {
  currentOrder.value = order
  escalateReason.value = ''
  escalateDetail.value = ''
  showEscalateModal.value = true
}

const submitEscalate = () => {
  if (!escalateReason.value) {
    alert('请选择升级原因')
    return
  }
  if (currentOrder.value) {
    currentOrder.value.status = 'escalated'
    currentOrder.value.escalateReason = escalateReason.value + (escalateDetail.value ? ' - ' + escalateDetail.value : '')
    currentOrder.value.escalatedAt = new Date().toLocaleString()
    stats.value.processing--
    stats.value.escalated++
  }
  showEscalateModal.value = false
}

const openOrderDetail = (order) => {
  currentOrder.value = order
}
</script>

<style scoped>
.engineering-page {
  padding: 24px 32px;
  background: #f5f6f5;
  min-height: calc(100vh - 100px);
}

.page-header {
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #1a2f23;
  margin: 0 0 4px 0;
}

.page-subtitle {
  font-size: 13px;
  color: #8a9a8e;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.btn-primary, .btn-secondary {
  padding: 8px 20px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
}

.btn-primary {
  background: #2D4A3E;
  color: #fff;
}

.btn-primary:hover {
  background: #1a2f23;
}

.btn-secondary {
  background: #fff;
  color: #2D4A3E;
  border: 1px solid #2D4A3E;
}

.btn-secondary:hover {
  background: rgba(45, 74, 62, 0.06);
}

/* 统计卡片 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid #e8ece9;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon.pending { background: linear-gradient(135deg, #FFF8E7, #FEF0C8); }
.stat-icon.processing { background: linear-gradient(135deg, #E8F5EB, #D4EBD9); }
.stat-icon.completed { background: linear-gradient(135deg, #DCEBE3, #C8DED3); }
.stat-icon.escalated { background: linear-gradient(135deg, #FDEBEB, #F9D5D5); }

.stat-number {
  font-size: 28px;
  font-weight: 700;
}

.stat-icon.pending .stat-number { color: #D4A853; }
.stat-icon.processing .stat-number { color: #4A7C59; }
.stat-icon.completed .stat-number { color: #2D4A3E; }
.stat-icon.escalated .stat-number { color: #C0392B; }

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: 14px;
  font-weight: 600;
  color: #1a2f23;
  margin-bottom: 4px;
}

.stat-sub {
  font-size: 12px;
  color: #8a9a8e;
}

/* 工作流说明 */
.workflow-guide {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px 24px;
  background: #fff;
  border-radius: 10px;
  border: 1px solid #e8ece9;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.workflow-step {
  display: flex;
  align-items: center;
  gap: 8px;
}

.step-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.step-dot.pending { background: #D4A853; }
.step-dot.processing { background: #4A7C59; }
.step-dot.completed { background: #2D4A3E; }
.step-dot.escalated { background: #C0392B; }

.step-text {
  font-size: 12px;
  color: #6a7a6e;
}

.workflow-arrow {
  color: #d0d8d2;
  font-size: 14px;
  font-weight: 300;
}

.workflow-arrow.or {
  color: #D4A853;
  font-weight: 500;
  margin: 0 8px;
}

/* 看板 */
.kanban-container {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.kanban-column {
  background: #fff;
  border-radius: 14px;
  border: 1px solid #e8ece9;
  display: flex;
  flex-direction: column;
  min-height: 500px;
}

.column-header {
  padding: 18px;
  border-bottom: 3px solid;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.column-header.pending { border-color: #D4A853; }
.column-header.processing { border-color: #4A7C59; }
.column-header.completed { border-color: #2D4A3E; }
.column-header.escalated { border-color: #C0392B; }

.column-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  color: #1a2f23;
}

.column-badge {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: rgba(0,0,0,0.06);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  color: #6a7a6e;
}

.column-count {
  background: rgba(0,0,0,0.04);
  padding: 4px 12px;
  border-radius: 14px;
  font-size: 12px;
  font-weight: 600;
  color: #6a7a6e;
}

.column-content {
  flex: 1;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
}

/* 工单卡片 */
.work-card {
  background: #f8f9f8;
  border-radius: 10px;
  padding: 14px;
  cursor: pointer;
  transition: all 0.25s;
  border-left: 4px solid #e8ece9;
}

.work-card:hover {
  background: #fff;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  transform: translateY(-2px);
}

.work-card.pending { border-left-color: #D4A853; }
.work-card.processing { border-left-color: #4A7C59; }
.completed-card { border-left-color: #2D4A3E; opacity: 0.85; }
.escalated-card { border-left-color: #C0392B; background: rgba(192,57,43,0.05); }

.card-badge-group {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.priority-tag {
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 600;
}

.priority-tag.high { background: rgba(192,57,43,0.1); color: #C0392B; }
.priority-tag.medium { background: rgba(212,168,83,0.1); color: #b8922e; }
.priority-tag.low { background: rgba(74,124,89,0.1); color: #4A7C59; }

.dept-tag, .assignee-tag {
  font-size: 10px;
  color: #8a9a8e;
  background: rgba(0,0,0,0.04);
  padding: 2px 8px;
  border-radius: 4px;
}

.status-tag {
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 600;
}

.status-tag.completed { background: rgba(45,74,62,0.1); color: #2D4A3E; }
.status-tag.escalated { background: rgba(192,57,43,0.1); color: #C0392B; }

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a2f23;
  margin-bottom: 8px;
  line-height: 1.4;
}

.card-info {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 11px;
  color: #8a9a8e;
  margin-bottom: 6px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.card-time {
  font-size: 11px;
  color: #a0b0a5;
  margin-bottom: 10px;
}

.card-progress-section {
  margin-bottom: 8px;
}

.progress-bar-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.progress-bar {
  flex: 1;
  height: 6px;
  background: #e8ece9;
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #2D4A3E, #4A7C59);
  border-radius: 3px;
  transition: width 0.3s;
}

.progress-percent {
  font-size: 11px;
  font-weight: 600;
  color: #4A7C59;
  min-width: 36px;
  text-align: right;
}

.progress-meta {
  font-size: 10px;
  color: #8a9a8e;
}

.card-escalate-reason {
  padding: 8px;
  background: rgba(192,57,43,0.08);
  border-radius: 6px;
  margin-bottom: 8px;
  font-size: 11px;
}

.reason-label {
  color: #C0392B;
  font-weight: 600;
}

.reason-text {
  color: #6a7a6e;
  margin-left: 4px;
}

.card-actions {
  display: flex;
  gap: 6px;
}

.card-actions button {
  flex: 1;
  padding: 5px 8px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
}

.btn-accept {
  background: #2D4A3E;
  color: #fff;
}

.btn-accept:hover {
  background: #1a2f23;
}

.btn-detail {
  background: #fff;
  color: #6a7a6e;
  border: 1px solid #e8ece9;
}

.btn-detail:hover {
  background: rgba(0,0,0,0.04);
}

.btn-progress {
  background: rgba(212,168,83,0.1);
  color: #D4A853;
}

.btn-progress:hover {
  background: rgba(212,168,83,0.15);
}

.btn-complete {
  background: rgba(74,124,89,0.1);
  color: #4A7C59;
}

.btn-complete:hover {
  background: rgba(74,124,89,0.15);
}

.btn-escalate {
  background: rgba(192,57,43,0.1);
  color: #C0392B;
}

.btn-escalate:hover {
  background: rgba(192,57,43,0.15);
}

.btn-gm {
  background: #C0392B;
  color: #fff;
}

.btn-gm:hover {
  background: #a93226;
}

/* 弹窗 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: #fff;
  border-radius: 16px;
  width: 480px;
  max-width: 90vw;
  padding: 24px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.modal-header h3 {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  color: #1a2f23;
}

.close-btn {
  background: none;
  border: none;
  font-size: 26px;
  color: #ccc;
  cursor: pointer;
  padding: 0 8px;
}

.close-btn:hover {
  color: #999;
}

.form-row {
  margin-bottom: 16px;
}

.form-row label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #6a7a6e;
  margin-bottom: 6px;
}

.form-input, .form-select, .form-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e8ece9;
  border-radius: 8px;
  font-size: 14px;
  color: #1a2f23;
  box-sizing: border-box;
}

.form-input:focus, .form-select:focus, .form-textarea:focus {
  outline: none;
  border-color: #2D4A3E;
}

.form-textarea {
  height: 100px;
  resize: vertical;
}

.progress-slider {
  width: 100%;
  height: 8px;
  margin-bottom: 8px;
}

.progress-value-display {
  font-size: 14px;
  font-weight: 600;
  color: #2D4A3E;
  display: block;
  text-align: right;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

.btn-cancel, .btn-confirm, .btn-escalate-confirm {
  padding: 10px 24px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.btn-cancel {
  background: #f0f1f0;
  color: #6a7a6e;
}

.btn-cancel:hover {
  background: #e0e1e0;
}

.btn-confirm {
  background: #2D4A3E;
  color: #fff;
}

.btn-confirm:hover {
  background: #1a2f23;
}

.btn-escalate-confirm {
  background: #C0392B;
  color: #fff;
}

.btn-escalate-confirm:hover {
  background: #a93226;
}

/* 响应式 */
@media (max-width: 1200px) {
  .kanban-container {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .kanban-container {
    grid-template-columns: 1fr;
  }
  .workflow-guide {
    padding: 12px 16px;
  }
  .modal-content {
    width: 95vw;
    padding: 16px;
  }
}
</style>