<template>
  <div class="gm-office-page">
    <!-- 顶部 -->
    <div class="page-topbar">
      <div class="topbar-left">
        <h1 class="page-title">总经办 · GM Office</h1>
        <span class="page-desc">总经理决策中心 · Decision Center</span>
      </div>
      <div class="topbar-actions">
        <el-button @click="$router.back()" plain>
          <el-icon><ArrowLeft /></el-icon>
          <span>返回工作台</span>
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card pai">
        <div class="stat-icon">拍</div>
        <div class="stat-info">
          <div class="stat-num">3</div>
          <div class="stat-label">待批阅</div>
        </div>
      </div>
      <div class="stat-card yue">
        <div class="stat-icon">阅</div>
        <div class="stat-info">
          <div class="stat-num">5</div>
          <div class="stat-label">待了解</div>
        </div>
      </div>
      <div class="stat-card pi">
        <div class="stat-icon">批</div>
        <div class="stat-info">
          <div class="stat-num">2</div>
          <div class="stat-label">待批复</div>
        </div>
      </div>
      <div class="stat-card ban">
        <div class="stat-icon">办</div>
        <div class="stat-info">
          <div class="stat-num">4</div>
          <div class="stat-label">待办</div>
        </div>
      </div>
    </div>

    <!-- 待批阅事项 -->
    <div class="section-card">
      <h3 class="section-title">待批阅事项 · Pending Review</h3>
      <div class="item-list">
        <div v-for="(item, idx) in reviewItems" :key="'review-'+idx" class="list-item">
          <span class="item-num">{{ idx + 1 }}</span>
          <div class="item-content">
            <span class="item-text">{{ item.title }}</span>
            <el-tag :type="item.tagType" size="small" effect="plain">{{ item.tag }}</el-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- 待批复事项 -->
    <div class="section-card">
      <h3 class="section-title">待批复事项 · Pending Approval</h3>
      <div class="item-list">
        <div v-for="(item, idx) in approvalItems" :key="'approval-'+idx" class="list-item">
          <span class="item-num">{{ idx + 1 }}</span>
          <div class="item-content">
            <span class="item-text">{{ item.title }}</span>
            <el-tag :type="item.tagType" size="small" effect="plain">{{ item.tag }}</el-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- 待了解事项 -->
    <div class="section-card">
      <h3 class="section-title">待了解事项 · To Know</h3>
      <div class="item-list">
        <div v-for="(item, idx) in infoItems" :key="'info-'+idx" class="list-item">
          <span class="item-num">{{ idx + 1 }}</span>
          <div class="item-content">
            <span class="item-text">{{ item.title }}</span>
            <el-tag :type="item.tagType" size="small" effect="plain">{{ item.tag }}</el-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- 待办事项 -->
    <div class="section-card">
      <h3 class="section-title">待办事项 · To Do</h3>
      <div class="item-list">
        <div v-for="(item, idx) in todoItems" :key="'todo-'+idx" class="list-item">
          <span class="item-num">{{ idx + 1 }}</span>
          <div class="item-content">
            <span class="item-text">{{ item.title }}</span>
            <el-tag :type="item.tagType" size="small" effect="plain">{{ item.tag }}</el-tag>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ArrowLeft } from '@element-plus/icons-vue'

const reviewItems = ref([
  { title: '宁国店6月营收报表', tag: '待阅', tagType: 'warning' },
  { title: '宣城店6月营收报表', tag: '待阅', tagType: 'warning' },
  { title: '本月新品销售分析', tag: '待阅', tagType: 'warning' }
])

const approvalItems = ref([
  { title: '厨房设备采购申请 - ¥28,000', tag: '待批', tagType: 'danger' },
  { title: '员工调薪申请 - 3人', tag: '待批', tagType: 'danger' }
])

const infoItems = ref([
  { title: '食品安全检查通知', tag: '已阅', tagType: 'info' },
  { title: '供应商合同到期提醒', tag: '待阅', tagType: 'warning' },
  { title: '消防演练安排', tag: '已阅', tagType: 'info' },
  { title: '会员系统升级方案', tag: '待阅', tagType: 'warning' },
  { title: '暑期营销活动计划', tag: '待阅', tagType: 'warning' }
])

const todoItems = ref([
  { title: '审批月度财务报表', tag: '待办', tagType: 'warning' },
  { title: '参加供应商评审会', tag: '待办', tagType: 'warning' },
  { title: '审核员工培训计划', tag: '待办', tagType: 'warning' },
  { title: '确认新店选址方案', tag: '待办', tagType: 'warning' }
])
</script>

<style scoped>
.gm-office-page { max-width: 1400px; margin: 0 auto; padding-bottom: 40px; }

.page-topbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; flex-wrap: wrap; gap: 12px; }
.topbar-left { display: flex; flex-direction: column; }
.page-title { font-size: 22px; font-weight: 700; color: var(--color-text-primary, var(--color-text)); margin: 0; }
.page-desc { font-size: 13px; color: var(--color-text-secondary); margin-top: 4px; }

.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  transition: var(--transition);
}
.stat-card:hover { box-shadow: var(--shadow-md); transform: translateY(-2px); }
.stat-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 700;
  border-radius: var(--radius-md);
  color: #fff;
}
.stat-card.pai .stat-icon { background: var(--color-primary); }
.stat-card.yue .stat-icon { background: var(--color-info); }
.stat-card.pi .stat-icon { background: var(--color-danger); }
.stat-card.ban .stat-icon { background: var(--color-warning); }
.stat-info { flex: 1; }
.stat-num { font-size: 28px; font-weight: 700; color: var(--color-text); line-height: 1.2; }
.stat-label { font-size: 13px; color: var(--color-text-secondary); margin-top: 4px; }

.section-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px 24px;
  margin-bottom: 16px;
}
.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 16px 0;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--color-border-light);
}

.item-list { display: flex; flex-direction: column; gap: 10px; }
.list-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  background: var(--color-bg-alt);
  border-radius: var(--radius-sm);
  transition: var(--transition);
}
.list-item:hover { background: var(--color-bg-side); }
.item-num {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-secondary);
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 50%;
  flex-shrink: 0;
}
.item-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.item-text { font-size: 14px; color: var(--color-text); }

@media (max-width: 1200px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 768px) {
  .stats-row { grid-template-columns: 1fr; }
  .item-content { flex-direction: column; align-items: flex-start; gap: 6px; }
}
</style>
