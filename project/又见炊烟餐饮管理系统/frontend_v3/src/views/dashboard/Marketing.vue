<template>
  <div class="marketing-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">营销会员 · Marketing & Members</h2>
        <p class="page-desc">会员档案 · 营销活动 · 优惠券管理</p>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-label">会员总数</div>
        <div class="stat-value">1,694</div>
        <div class="stat-sub">累计注册</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">本月新增</div>
        <div class="stat-value">156</div>
        <div class="stat-sub">较上月 +12.5%</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">活跃会员</div>
        <div class="stat-value">832</div>
        <div class="stat-sub">近 30 天有消费</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">营销活动</div>
        <div class="stat-value">8</div>
        <div class="stat-sub">进行中 4 个</div>
      </div>
    </div>

    <div class="content-card">
      <el-tabs v-model="activeTab" class="marketing-tabs">
        <el-tab-pane label="会员列表" name="members">
          <div class="toolbar">
            <div class="toolbar-left">
              <el-input
                v-model="memberSearch"
                placeholder="搜索姓名 / 手机号"
                clearable
                style="width: 240px"
              />
              <el-select v-model="memberLevel" placeholder="会员等级" clearable style="width: 140px">
                <el-option label="全部" value="" />
                <el-option label="普通" value="普通" />
                <el-option label="银卡" value="银卡" />
                <el-option label="金卡" value="金卡" />
                <el-option label="钻石" value="钻石" />
              </el-select>
              <el-button type="primary" @click="onSearch">查询</el-button>
              <el-button @click="onReset">重置</el-button>
            </div>
          </div>
          <el-table :data="filteredMembers" stripe v-loading="loading">
            <el-table-column prop="cardNo" label="会员号" width="180" />
            <el-table-column prop="name" label="姓名" width="120" />
            <el-table-column prop="phone" label="手机" width="140" />
            <el-table-column prop="level" label="等级" width="100">
              <template #default="{ row }">
                <el-tag :type="levelTagType(row.level)" size="small" effect="plain">
                  {{ row.level || '普通' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="points" label="积分" width="100" align="right" />
            <el-table-column prop="balance" label="余额" width="120" align="right">
              <template #default="{ row }">¥{{ (row.balance || 0).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column prop="registerDate" label="注册日期" width="120" />
            <el-table-column label="操作" min-width="160">
              <template #default="{ row }">
                <el-button text size="small" @click="viewMember(row)">详情</el-button>
                <el-button text size="small" type="primary" @click="editMember(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="营销活动" name="activities">
          <div class="toolbar">
            <div class="toolbar-left">
              <el-input
                v-model="activitySearch"
                placeholder="搜索活动名称"
                clearable
                style="width: 240px"
              />
              <el-select v-model="activityType" placeholder="活动类型" clearable style="width: 160px">
                <el-option label="全部" value="" />
                <el-option label="折扣" value="折扣" />
                <el-option label="满减" value="满减" />
                <el-option label="赠品" value="赠品" />
                <el-option label="节日" value="节日" />
              </el-select>
              <el-button type="primary" @click="onSearch">查询</el-button>
              <el-button @click="onReset">重置</el-button>
            </div>
          </div>
          <el-table :data="filteredActivities" stripe v-loading="loading">
            <el-table-column prop="name" label="活动名称" min-width="180" />
            <el-table-column prop="type" label="类型" width="100">
              <template #default="{ row }">
                <el-tag size="small" effect="plain">{{ row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="startDate" label="开始日期" width="120" />
            <el-table-column prop="endDate" label="结束日期" width="120" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="activityStatusType(row.status)" size="small" effect="plain">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="participants" label="参与人数" width="120" align="right" />
            <el-table-column label="操作" min-width="160">
              <template #default="{ row }">
                <el-button text size="small" @click="viewActivity(row)">详情</el-button>
                <el-button text size="small" type="primary" @click="editActivity(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="优惠券" name="coupons">
          <div class="toolbar">
            <div class="toolbar-left">
              <el-input
                v-model="couponSearch"
                placeholder="搜索券名"
                clearable
                style="width: 240px"
              />
              <el-select v-model="couponStatus" placeholder="状态" clearable style="width: 140px">
                <el-option label="全部" value="" />
                <el-option label="进行中" value="进行中" />
                <el-option label="已停用" value="已停用" />
                <el-option label="已过期" value="已过期" />
              </el-select>
              <el-button type="primary" @click="onSearch">查询</el-button>
              <el-button @click="onReset">重置</el-button>
            </div>
          </div>
          <el-table :data="filteredCoupons" stripe v-loading="loading">
            <el-table-column prop="name" label="券名" min-width="160" />
            <el-table-column prop="amount" label="面值" width="100" align="right">
              <template #default="{ row }">¥{{ row.amount }}</template>
            </el-table-column>
            <el-table-column prop="condition" label="适用条件" min-width="160" />
            <el-table-column prop="issued" label="已发" width="100" align="right" />
            <el-table-column prop="used" label="已用" width="100" align="right" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="couponStatusType(row.status)" size="small" effect="plain">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="160">
              <template #default="{ row }">
                <el-button text size="small" @click="viewCoupon(row)">详情</el-button>
                <el-button text size="small" type="primary" @click="editCoupon(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const activeTab = ref('members')

const memberSearch = ref('')
const memberLevel = ref('')
const activitySearch = ref('')
const activityType = ref('')
const couponSearch = ref('')
const couponStatus = ref('')

const members = ref([])
const activities = ref([])
const coupons = ref([])

const filteredMembers = computed(() => {
  let list = members.value
  if (memberSearch.value) {
    const q = memberSearch.value.toLowerCase()
    list = list.filter(m =>
      (m.name || '').toLowerCase().includes(q) ||
      (m.phone || '').includes(q) ||
      (m.cardNo || '').toLowerCase().includes(q)
    )
  }
  if (memberLevel.value) list = list.filter(m => m.level === memberLevel.value)
  return list
})

const filteredActivities = computed(() => {
  let list = activities.value
  if (activitySearch.value) {
    const q = activitySearch.value.toLowerCase()
    list = list.filter(a => (a.name || '').toLowerCase().includes(q))
  }
  if (activityType.value) list = list.filter(a => a.type === activityType.value)
  return list
})

const filteredCoupons = computed(() => {
  let list = coupons.value
  if (couponSearch.value) {
    const q = couponSearch.value.toLowerCase()
    list = list.filter(c => (c.name || '').toLowerCase().includes(q))
  }
  if (couponStatus.value) list = list.filter(c => c.status === couponStatus.value)
  return list
})

function levelTagType(level) {
  return { '普通': 'info', '银卡': '', '金卡': 'warning', '钻石': 'danger' }[level] || 'info'
}
function activityStatusType(status) {
  return { '进行中': 'success', '未开始': 'info', '已结束': 'info' }[status] || 'info'
}
function couponStatusType(status) {
  return { '进行中': 'success', '已停用': 'info', '已过期': 'info' }[status] || 'info'
}

function onSearch() { /* 触发查询，数据已通过 computed 实时过滤 */ }
function onReset() {
  memberSearch.value = ''
  memberLevel.value = ''
  activitySearch.value = ''
  activityType.value = ''
  couponSearch.value = ''
  couponStatus.value = ''
}

function viewMember(row) { ElMessage.info(`查看会员：${row.name || row.cardNo}`) }
function editMember(row) { ElMessage.info(`编辑会员：${row.name || row.cardNo}`) }
function viewActivity(row) { ElMessage.info(`查看活动：${row.name}`) }
function editActivity(row) { ElMessage.info(`编辑活动：${row.name}`) }
function viewCoupon(row) { ElMessage.info(`查看优惠券：${row.name}`) }
function editCoupon(row) { ElMessage.info(`编辑优惠券：${row.name}`) }
</script>

<style scoped>
.marketing-page {
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
  margin: 0 0 6px 0;
  letter-spacing: 0.5px;
}

.page-desc {
  font-size: 13px;
  color: #5D6D7E;
  margin: 0;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: #FFFFFF;
  border: 1px solid #E8E4DE;
  border-radius: 10px;
  padding: 20px;
  position: relative;
  overflow: hidden;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
  background: linear-gradient(180deg, #2D4A3E 0%, #C4A35A 100%);
}

.stat-card:hover {
  box-shadow: 0 8px 24px rgba(45, 74, 62, 0.1);
  transform: translateY(-2px);
}

.stat-label {
  font-size: 13px;
  color: #95A5A6;
  margin-bottom: 8px;
  font-weight: 500;
  letter-spacing: 0.5px;
}

.stat-value {
  font-size: 30px;
  font-weight: 700;
  color: #1a2f23;
  line-height: 1.2;
  letter-spacing: -0.3px;
}

.stat-sub {
  font-size: 12px;
  color: #95A5A6;
  margin-top: 6px;
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

.marketing-tabs :deep(.el-tabs__item.is-active) {
  color: #2D4A3E;
}

.marketing-tabs :deep(.el-tabs__active-bar) {
  background: #C4A35A;
}

@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: 1fr;
  }
}
</style>
