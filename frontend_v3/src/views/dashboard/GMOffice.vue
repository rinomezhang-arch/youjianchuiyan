<template>
  <div class="gm-office-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">总经办 · GM Office</h2>
        <p class="page-subtitle">总经理决策中心 · Decision Center</p>
      </div>
      <div class="header-actions">
        <button class="btn-back" @click="goBack">
          <span>返回工作台</span>
        </button>
      </div>
    </div>

    <!-- 概览统计 -->
    <div class="overview-row">
      <div class="overview-card">
        <div class="overview-icon decision">
          <span class="icon-text">拍</span>
        </div>
        <div class="overview-content">
          <div class="overview-count">{{ stats.decision }}</div>
          <div class="overview-label">待拍板</div>
        </div>
      </div>
      <div class="overview-card">
        <div class="overview-icon inform">
          <span class="icon-text">阅</span>
        </div>
        <div class="overview-content">
          <div class="overview-count">{{ stats.inform }}</div>
          <div class="overview-label">待了解</div>
        </div>
      </div>
      <div class="overview-card">
        <div class="overview-icon approve">
          <span class="icon-text">批</span>
        </div>
        <div class="overview-content">
          <div class="overview-count">{{ stats.approve }}</div>
          <div class="overview-label">待批复</div>
        </div>
      </div>
      <div class="overview-card">
        <div class="overview-icon todo">
          <span class="icon-text">办</span>
        </div>
        <div class="overview-content">
          <div class="overview-count">{{ stats.todo }}</div>
          <div class="overview-label">待办</div>
        </div>
      </div>
    </div>

    <!-- 四大板块 -->
    <div class="main-content">
      <!-- 左侧：待拍板 + 待了解 -->
      <div class="left-section">
        <!-- 待拍板事项 -->
        <div class="section-card">
          <div class="section-header decision">
            <div class="section-title">
              <span class="section-badge">01</span>
              <span>待拍板事项</span>
            </div>
            <span class="section-count">{{ items.decision.length }}</span>
          </div>
          <div class="section-body">
            <div v-for="item in items.decision" :key="item.id" class="item-card" @click="openDetail(item)">
              <div class="item-priority" :class="item.priority"></div>
              <div class="item-content">
                <div class="item-title">{{ item.title }}</div>
                <div class="item-meta">
                  <span class="meta-item">{{ item.source }}</span>
                  <span class="meta-item">{{ item.time }}</span>
                </div>
                <div class="item-desc">{{ item.description }}</div>
              </div>
              <div class="item-actions">
                <button class="btn-decide" @click.stop="handleDecision(item)">拍板</button>
              </div>
            </div>
          </div>
        </div>

        <!-- 待了解事项 -->
        <div class="section-card">
          <div class="section-header inform">
            <div class="section-title">
              <span class="section-badge">02</span>
              <span>待了解事项</span>
            </div>
            <span class="section-count">{{ items.inform.length }}</span>
          </div>
          <div class="section-body">
            <div v-for="item in items.inform" :key="item.id" class="item-card" @click="openDetail(item)">
              <div class="item-type" :class="item.type"></div>
              <div class="item-content">
                <div class="item-title">{{ item.title }}</div>
                <div class="item-meta">
                  <span class="meta-item">{{ item.source }}</span>
                  <span class="meta-item">{{ item.time }}</span>
                </div>
                <div class="item-desc">{{ item.description }}</div>
              </div>
              <div class="item-actions">
                <button class="btn-read" @click.stop="markRead(item)">已阅</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：待批复 + 待办 -->
      <div class="right-section">
        <!-- 待批复事项 -->
        <div class="section-card">
          <div class="section-header approve">
            <div class="section-title">
              <span class="section-badge">03</span>
              <span>待批复事项</span>
            </div>
            <span class="section-count">{{ items.approve.length }}</span>
          </div>
          <div class="section-body">
            <div v-for="item in items.approve" :key="item.id" class="item-card" @click="openDetail(item)">
              <div class="item-priority" :class="item.priority"></div>
              <div class="item-content">
                <div class="item-title">{{ item.title }}</div>
                <div class="item-meta">
                  <span class="meta-item">{{ item.source }}</span>
                  <span class="meta-item">{{ item.amount }}</span>
                </div>
                <div class="item-desc">{{ item.description }}</div>
              </div>
              <div class="item-actions">
                <button class="btn-approve" @click.stop="handleApprove(item)">批准</button>
                <button class="btn-reject" @click.stop="handleReject(item)">驳回</button>
              </div>
            </div>
          </div>
        </div>

        <!-- 待办事项 -->
        <div class="section-card">
          <div class="section-header todo">
            <div class="section-title">
              <span class="section-badge">04</span>
              <span>待办事项</span>
            </div>
            <span class="section-count">{{ items.todo.length }}</span>
          </div>
          <div class="section-body">
            <div v-for="item in items.todo" :key="item.id" class="item-card" @click="openDetail(item)">
              <div class="item-status" :class="item.status"></div>
              <div class="item-content">
                <div class="item-title">{{ item.title }}</div>
                <div class="item-meta">
                  <span class="meta-item">{{ item.deadline }}</span>
                  <span class="meta-item">{{ item.assignee }}</span>
                </div>
                <div class="item-desc">{{ item.description }}</div>
              </div>
              <div class="item-actions">
                <button class="btn-start" @click.stop="startTask(item)" v-if="item.status === 'pending'">开始</button>
                <button class="btn-finish" @click.stop="finishTask(item)" v-else>完成</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <div v-if="showDetailModal" class="modal-overlay" @click="showDetailModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>{{ currentItem?.title }}</h3>
          <button class="close-btn" @click="showDetailModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="detail-row">
            <label>来源</label>
            <span>{{ currentItem?.source }}</span>
          </div>
          <div class="detail-row">
            <label>时间</label>
            <span>{{ currentItem?.time }}</span>
          </div>
          <div class="detail-row">
            <label>优先级</label>
            <span class="priority-text" :class="currentItem?.priority">{{ priorityText(currentItem?.priority) }}</span>
          </div>
          <div class="detail-row">
            <label>详细描述</label>
            <p>{{ currentItem?.description }}</p>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="showDetailModal = false">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const goBack = () => {
  router.push('/dashboard/home')
}

const stats = reactive({
  decision: 3,
  inform: 5,
  approve: 2,
  todo: 4,
})

const items = reactive({
  decision: [
    { id: 1, title: '消防系统改造方案审批', source: '安保部', time: '07-27 10:30', priority: 'high', description: '由于原消防系统已使用超过10年，需进行全面改造升级。方案已提交，涉及预算58万元，需要总经理拍板决定是否执行。' },
    { id: 2, title: '新菜品开发方向决策', source: '后厨', time: '07-27 09:00', priority: 'medium', description: '后厨提交了三个新菜品开发方向：徽菜创新、融合菜、养生菜。需要总经理确定重点发展方向。' },
    { id: 3, title: '年度团建方案选择', source: '人事部', time: '07-26 16:00', priority: 'low', description: '人事部提交了三个团建方案：户外拓展、温泉度假、城市定向。需要总经理选择方案并批准预算。' },
  ],
  inform: [
    { id: 1, title: '7月份经营数据分析报告', source: '财务部', time: '07-27 08:30', type: 'report', description: '7月份营业收入环比增长12%，毛利率提升3个百分点，主要得益于新菜品推广和会员活动。' },
    { id: 2, title: 'VIP客户投诉处理结果', source: '前厅部', time: '07-27 11:00', type: 'complaint', description: '上周VIP客户张先生投诉服务问题，已处理完毕并获得客户谅解，客户表示继续支持本店。' },
    { id: 3, title: '供应商合同续签提醒', source: '采购部', time: '07-26 14:00', type: 'reminder', description: '三家核心供应商合同将在8月到期，需提前安排续签谈判，建议在本月底前完成。' },
    { id: 4, title: '员工培训计划进展', source: '人事部', time: '07-26 10:00', type: 'progress', description: '本月员工培训计划已完成75%，服务礼仪培训效果良好，员工满意度提升明显。' },
    { id: 5, title: '设备维护月度报告', source: '工程部', time: '07-25 17:00', type: 'report', description: '本月完成设备维护28项，发现潜在问题5处，已安排整改，建议关注冷库设备老化问题。' },
  ],
  approve: [
    { id: 1, title: '采购申请审批', source: '后厨', amount: '¥128,000', priority: 'high', description: '后厨申请采购新设备（洗碗机、冷藏柜）及原材料补充，总金额12.8万元。' },
    { id: 2, title: '报销审批', source: '采购部', amount: '¥25,600', priority: 'medium', description: '采购部报销差旅费及招待费用，共计2.56万元。' },
  ],
  todo: [
    { id: 1, title: '参加行业协会会议', deadline: '07-28 09:00', assignee: '总经理', status: 'pending', description: '本周六参加餐饮行业协会年度会议，需准备发言材料。' },
    { id: 2, title: '审批员工晋升申请', deadline: '07-27 18:00', assignee: '总经理', status: 'pending', description: '人事部提交了3名员工晋升申请，需在今日下班前完成审批。' },
    { id: 3, title: '接待重要客户', deadline: '07-27 14:00', assignee: '总经理', status: 'processing', description: '下午2点接待集团总部领导视察，需提前安排接待流程。' },
    { id: 4, title: '审阅财务报表', deadline: '07-29 12:00', assignee: '总经理', status: 'processing', description: '审阅上半年财务报表，准备董事会汇报材料。' },
  ],
})

const showDetailModal = ref(false)
const currentItem = ref(null)

const priorityText = (p) => ({ high: '紧急', medium: '中等', low: '一般' }[p] || p)

const openDetail = (item) => {
  currentItem.value = item
  showDetailModal.value = true
}

const handleDecision = (item) => {
  const decision = prompt(`请输入您的拍板决定：\n\n${item.title}\n\n${item.description}`)
  if (decision) {
    alert(`拍板完成：${decision}`)
    stats.decision--
    items.decision = items.decision.filter(i => i.id !== item.id)
  }
}

const markRead = (item) => {
  alert(`已阅：${item.title}`)
  stats.inform--
  items.inform = items.inform.filter(i => i.id !== item.id)
}

const handleApprove = (item) => {
  alert(`已批准：${item.title}`)
  stats.approve--
  items.approve = items.approve.filter(i => i.id !== item.id)
}

const handleReject = (item) => {
  const reason = prompt('请输入驳回原因：')
  if (reason) {
    alert(`已驳回：${item.title}\n原因：${reason}`)
    stats.approve--
    items.approve = items.approve.filter(i => i.id !== item.id)
  }
}

const startTask = (item) => {
  item.status = 'processing'
  alert(`已开始：${item.title}`)
}

const finishTask = (item) => {
  item.status = 'completed'
  stats.todo--
  items.todo = items.todo.filter(i => i.id !== item.id)
  alert(`已完成：${item.title}`)
}
</script>

<style scoped>
.gm-office-page {
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

.btn-back {
  padding: 8px 20px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border: 1px solid #2D4A3E;
  color: #2D4A3E;
  background: #fff;
  transition: all 0.2s;
}

.btn-back:hover {
  background: rgba(45, 74, 62, 0.06);
}

/* 概览统计 */
.overview-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.overview-card {
  background: #fff;
  border-radius: 14px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid #e8ece9;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.overview-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.overview-icon.decision { background: linear-gradient(135deg, #C0392B, #A93226); }
.overview-icon.inform { background: linear-gradient(135deg, #2D4A3E, #1A2F23); }
.overview-icon.approve { background: linear-gradient(135deg, #D4A853, #B8922E); }
.overview-icon.todo { background: linear-gradient(135deg, #4A7C59, #3D6B4D); }

.icon-text {
  font-size: 22px;
  font-weight: 700;
  color: #fff;
}

.overview-content {
  flex: 1;
}

.overview-count {
  font-size: 28px;
  font-weight: 700;
  color: #1a2f23;
  line-height: 1.2;
}

.overview-label {
  font-size: 13px;
  color: #8a9a8e;
  margin-top: 4px;
}

/* 主内容区 */
.main-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.section-card {
  background: #fff;
  border-radius: 14px;
  border: 1px solid #e8ece9;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.section-header {
  padding: 18px;
  border-bottom: 3px solid;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-header.decision { border-color: #C0392B; }
.section-header.inform { border-color: #2D4A3E; }
.section-header.approve { border-color: #D4A853; }
.section-header.todo { border-color: #4A7C59; }

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  color: #1a2f23;
}

.section-badge {
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

.section-count {
  background: rgba(0,0,0,0.04);
  padding: 4px 12px;
  border-radius: 14px;
  font-size: 12px;
  font-weight: 600;
  color: #6a7a6e;
}

.section-body {
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 事项卡片 */
.item-card {
  background: #f8f9f8;
  border-radius: 10px;
  padding: 14px;
  cursor: pointer;
  transition: all 0.25s;
  display: flex;
  gap: 12px;
  border-left: 4px solid #e8ece9;
}

.item-card:hover {
  background: #fff;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  transform: translateY(-2px);
}

.item-priority {
  width: 8px;
  border-radius: 4px;
  flex-shrink: 0;
}

.item-priority.high { background: #C0392B; }
.item-priority.medium { background: #D4A853; }
.item-priority.low { background: #4A7C59; }

.item-type {
  width: 8px;
  border-radius: 4px;
  flex-shrink: 0;
}

.item-type.report { background: #2D4A3E; }
.item-type.complaint { background: #C0392B; }
.item-type.reminder { background: #D4A853; }
.item-type.progress { background: #4A7C59; }

.item-status {
  width: 8px;
  border-radius: 4px;
  flex-shrink: 0;
}

.item-status.pending { background: #D4A853; }
.item-status.processing { background: #4A7C59; }
.item-status.completed { background: #2D4A3E; }

.item-content {
  flex: 1;
  min-width: 0;
}

.item-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a2f23;
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-meta {
  display: flex;
  gap: 12px;
  font-size: 11px;
  color: #8a9a8e;
  margin-bottom: 6px;
}

.meta-item {
  display: flex;
  align-items: center;
}

.item-desc {
  font-size: 12px;
  color: #6a7a6e;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex-shrink: 0;
}

.item-actions button {
  padding: 5px 12px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
  white-space: nowrap;
}

.btn-decide {
  background: #C0392B;
  color: #fff;
}

.btn-decide:hover {
  background: #A93226;
}

.btn-read {
  background: #2D4A3E;
  color: #fff;
}

.btn-read:hover {
  background: #1A2F23;
}

.btn-approve {
  background: #4A7C59;
  color: #fff;
}

.btn-approve:hover {
  background: #3D6B4D;
}

.btn-reject {
  background: rgba(192,57,43,0.1);
  color: #C0392B;
}

.btn-reject:hover {
  background: rgba(192,57,43,0.15);
}

.btn-start {
  background: #D4A853;
  color: #fff;
}

.btn-start:hover {
  background: #B8922E;
}

.btn-finish {
  background: #2D4A3E;
  color: #fff;
}

.btn-finish:hover {
  background: #1A2F23;
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
  width: 500px;
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

.detail-row {
  display: flex;
  margin-bottom: 16px;
}

.detail-row label {
  width: 80px;
  font-size: 13px;
  font-weight: 500;
  color: #8a9a8e;
  flex-shrink: 0;
}

.detail-row span {
  flex: 1;
  font-size: 14px;
  color: #1a2f23;
}

.detail-row p {
  flex: 1;
  font-size: 14px;
  color: #6a7a6e;
  line-height: 1.6;
  margin: 0;
}

.priority-text {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
}

.priority-text.high { background: rgba(192,57,43,0.1); color: #C0392B; }
.priority-text.medium { background: rgba(212,168,83,0.1); color: #b8922e; }
.priority-text.low { background: rgba(74,124,89,0.1); color: #4A7C59; }

.modal-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
}

.btn-cancel {
  padding: 10px 24px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  background: #f0f1f0;
  color: #6a7a6e;
  border: none;
  transition: all 0.2s;
}

.btn-cancel:hover {
  background: #e0e1e0;
}

/* 响应式 */
@media (max-width: 1200px) {
  .main-content {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .overview-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .item-card {
    flex-direction: column;
  }
  .item-actions {
    flex-direction: row;
  }
  .modal-content {
    width: 95vw;
    padding: 16px;
  }
}
</style>