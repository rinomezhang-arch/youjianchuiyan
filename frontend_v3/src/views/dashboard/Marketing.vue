<template>
  <div class="marketing-page">
    <div class="page-header">
      <h2 class="page-title">营销会员总览</h2>
      <p class="page-subtitle">Marketing & Membership</p>
    </div>

    <div class="stats-row">
      <div class="stat-card" :style="{ color: '#2D4A3E' }">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
            <circle cx="9" cy="7" r="4"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">新增会员</div>
          <div class="stat-value">{{ overview.newMembers }}</div>
          <div class="stat-sub">本月累计 {{ overview.newMembersMonth }}</div>
        </div>
      </div>
      <div class="stat-card" :style="{ color: '#C4A35A' }">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="1" x2="12" y2="23"/>
            <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">储值金额</div>
          <div class="stat-value">¥{{ overview.storedValue }}</div>
          <div class="stat-sub">本月新增</div>
        </div>
      </div>
      <div class="stat-card" :style="{ color: '#4A7C59' }">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="2" y="3" width="20" height="16" rx="2"/>
            <path d="M2 9h20"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">线上团购核销</div>
          <div class="stat-value">{{ overview.groupBuyRedemptions }}</div>
          <div class="stat-sub">本周数据</div>
        </div>
      </div>
      <div class="stat-card" :style="{ color: '#5B7B8A' }">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 2L2 7l10 5 10-5-10-5z"/>
            <path d="M2 17l10 5 10-5"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">宴会订单数</div>
          <div class="stat-value">{{ overview.banquetOrders }}</div>
          <div class="stat-sub">本月预定</div>
        </div>
      </div>
    </div>

    <div class="quick-actions-card">
      <h3 class="section-title">快捷功能</h3>
      <div class="action-grid">
        <div class="action-card" @click="goTo('marketing')">
          <div class="action-icon" style="background: rgba(45,74,62,0.06)">
            <svg viewBox="0 0 24 24" fill="none" stroke="#2D4A3E" stroke-width="2">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
            </svg>
          </div>
          <span class="action-text">线上平台运营</span>
        </div>
        <div class="action-card" @click="goTo('marketing')">
          <div class="action-icon" style="background: rgba(74,124,89,0.06)">
            <svg viewBox="0 0 24 24" fill="none" stroke="#4A7C59" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <line x1="12" y1="8" x2="12" y2="12"/>
              <line x1="12" y1="16" x2="12.01" y2="16"/>
            </svg>
          </div>
          <span class="action-text">线下拓客</span>
        </div>
        <div class="action-card" @click="goTo('bookings')">
          <div class="action-icon" style="background: rgba(196,163,90,0.06)">
            <svg viewBox="0 0 24 24" fill="none" stroke="#C4A35A" stroke-width="2">
              <rect x="3" y="4" width="18" height="18" rx="2"/>
              <path d="M16 2v4"/>
              <path d="M8 2v4"/>
              <path d="M3 10h18"/>
            </svg>
          </div>
          <span class="action-text">宴会销售</span>
        </div>
        <div class="action-card" @click="goTo('customers')">
          <div class="action-icon" style="background: rgba(91,123,138,0.06)">
            <svg viewBox="0 0 24 24" fill="none" stroke="#5B7B8A" stroke-width="2">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
              <circle cx="9" cy="7" r="4"/>
            </svg>
          </div>
          <span class="action-text">会员管理</span>
        </div>
        <div class="action-card" @click="goTo('marketing')">
          <div class="action-icon" style="background: rgba(45,74,62,0.06)">
            <svg viewBox="0 0 24 24" fill="none" stroke="#2D4A3E" stroke-width="2">
              <rect x="2" y="6" width="20" height="14" rx="2"/>
              <path d="M16 2v4"/>
              <path d="M8 2v4"/>
            </svg>
          </div>
          <span class="action-text">营销活动</span>
        </div>
        <div class="action-card" @click="goTo('marketing')">
          <div class="action-icon" style="background: rgba(194,85,85,0.06)">
            <svg viewBox="0 0 24 24" fill="none" stroke="#C25555" stroke-width="2">
              <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/>
            </svg>
          </div>
          <span class="action-text">优惠券</span>
        </div>
      </div>
    </div>

    <div class="bottom-section">
      <div class="member-card">
        <h3 class="section-title">会员分层视图</h3>
        <div class="member-tiers">
          <div class="tier-card" v-for="(tier, index) in memberTiers" :key="index">
            <div class="tier-header">
              <div class="tier-icon" :style="{ background: tier.bgColor }">
                <span>{{ tier.icon }}</span>
              </div>
              <div class="tier-info">
                <div class="tier-name">{{ tier.name }}</div>
                <div class="tier-count">{{ tier.count }} 人</div>
              </div>
            </div>
            <div class="tier-stats">
              <div class="tier-stat">
                <span class="stat-label">本月消费</span>
                <span class="stat-value">{{ tier.spending }}</span>
              </div>
              <div class="tier-stat">
                <span class="stat-label">平均客单价</span>
                <span class="stat-value">{{ tier.avgSpend }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="side-cards">
        <div class="activity-card">
          <h3 class="section-title">进行中活动</h3>
          <div class="activity-list">
            <div class="activity-item" v-for="(activity, index) in activities" :key="index">
              <div class="activity-tag" :class="activity.type">{{ activity.tag }}</div>
              <div class="activity-content">
                <div class="activity-title">{{ activity.title }}</div>
                <div class="activity-meta">{{ activity.date }}</div>
              </div>
            </div>
          </div>
        </div>

        <div class="platform-card">
          <h3 class="section-title">线上平台数据</h3>
          <div class="platform-list">
            <div class="platform-item" v-for="(platform, index) in platforms" :key="index">
              <div class="platform-icon">{{ platform.icon }}</div>
              <div class="platform-info">
                <div class="platform-name">{{ platform.name }}</div>
                <div class="platform-value">{{ platform.value }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMarketingOverview, getMemberTiers, getActiveActivities, getPlatformStats } from '@/api/marketing'

const router = useRouter()

const loading = ref(true)

const overview = ref({
  newMembers: '--',
  newMembersMonth: '--',
  storedValue: '--',
  groupBuyRedemptions: '--',
  banquetOrders: '--'
})

const memberTiers = ref([])
const activities = ref([])
const platforms = ref([])

async function fetchOverview() {
  try {
    const res = await getMarketingOverview()
    if (res && res.data) {
      overview.value = res.data
    }
  } catch (e) {
    console.error('获取营销概览失败', e)
  }
}

async function fetchMemberTiers() {
  try {
    const res = await getMemberTiers()
    if (res && res.data) {
      memberTiers.value = res.data
    }
  } catch (e) {
    console.error('获取会员分层失败', e)
  }
}

async function fetchActivities() {
  try {
    const res = await getActiveActivities()
    if (res && res.data) {
      activities.value = res.data
    }
  } catch (e) {
    console.error('获取活动列表失败', e)
  }
}

async function fetchPlatformStats() {
  try {
    const res = await getPlatformStats()
    if (res && res.data) {
      platforms.value = res.data
    }
  } catch (e) {
    console.error('获取平台数据失败', e)
  }
}

function goTo(path) {
  router.push(`/dashboard/${path}`)
}

onMounted(async () => {
  loading.value = true
  await Promise.all([fetchOverview(), fetchMemberTiers(), fetchActivities(), fetchPlatformStats()])
  loading.value = false
})
</script>

<style scoped>
.marketing-page {
  max-width: 1400px;
  margin: 0 auto;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  position: relative;
  overflow: hidden;
}

.stat-card::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 80px;
  height: 80px;
  background: currentColor;
  opacity: 0.03;
  border-radius: 0 0 0 80px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  opacity: 0.7;
}

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-bottom: 4px;
  font-weight: 500;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text);
  line-height: 1.2;
}

.stat-sub {
  font-size: 11px;
  color: var(--color-text-muted);
  margin-top: 4px;
}

.quick-actions-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px;
  margin-bottom: 24px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 16px;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}

.action-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px;
  background: var(--color-bg-alt);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: var(--transition);
}

.action-card:hover {
  background: var(--color-bg-side);
  transform: translateY(-2px);
}

.action-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
}

.action-icon svg {
  width: 22px;
  height: 22px;
}

.action-text {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
}

.bottom-section {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
}

.member-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px;
}

.member-tiers {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.tier-card {
  background: var(--color-bg-alt);
  border-radius: var(--radius-md);
  padding: 16px;
}

.tier-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.tier-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
  color: var(--color-primary);
}

.tier-info {
  flex: 1;
}

.tier-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
}

.tier-count {
  font-size: 11px;
  color: var(--color-text-muted);
}

.tier-stats {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.tier-stat {
  display: flex;
  justify-content: space-between;
}

.tier-stat .stat-label {
  font-size: 11px;
  color: var(--color-text-muted);
}

.tier-stat .stat-value {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
}

.side-cards {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.activity-card, .platform-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px;
}

.activity-list, .platform-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.activity-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  background: var(--color-bg-alt);
  border-radius: var(--radius-sm);
}

.activity-tag {
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 600;
  border-radius: 4px;
  flex-shrink: 0;
}

.activity-tag.active {
  background: rgba(74, 124, 89, 0.08);
  color: #4A7C59;
}

.activity-tag.ended {
  background: rgba(149, 165, 166, 0.08);
  color: #95A5A6;
}

.activity-tag.upcoming {
  background: rgba(196, 163, 90, 0.08);
  color: #C4A35A;
}

.activity-content {
  flex: 1;
}

.activity-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text);
  margin-bottom: 2px;
}

.activity-meta {
  font-size: 11px;
  color: var(--color-text-muted);
}

.platform-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--color-bg-alt);
  border-radius: var(--radius-sm);
}

.platform-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: rgba(45, 74, 62, 0.06);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: var(--color-primary);
}

.platform-info {
  flex: 1;
}

.platform-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text);
}

.platform-value {
  font-size: 11px;
  color: var(--color-text-muted);
}

@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .action-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  .bottom-section {
    grid-template-columns: 1fr;
  }
  .member-tiers {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: 1fr;
  }
  .action-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .member-tiers {
    grid-template-columns: 1fr;
  }
}
</style>
