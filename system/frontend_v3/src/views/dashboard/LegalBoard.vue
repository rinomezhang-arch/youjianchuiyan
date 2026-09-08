<template>
  <div class="legal-page">
    <div class="page-topbar">
      <div class="topbar-left">
        <h1 class="page-title">法务看板 · Legal Board</h1>
        <span class="page-desc">劳动合同 · 证照资质 · 到期预警 · Contracts · Licenses · Expiry Alerts</span>
      </div>
      <div class="topbar-actions">
        <el-button @click="loadAll" :loading="loading">刷新 · Refresh</el-button>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card" v-for="s in stats" :key="s.label" :class="s.cls">
        <div class="stat-num">{{ s.value }}</div>
        <div class="stat-label">{{ s.label }}</div>
      </div>
    </div>

    <el-alert
      v-if="loadError"
      :title="loadError"
      type="warning"
      show-icon
      :closable="false"
      class="load-error"
    />

    <el-tabs v-model="activeTab" class="legal-tabs">
      <el-tab-pane label="劳动合同 · Contracts" name="contract">
        <div class="filter-bar">
          <el-input v-model="contractSearch" placeholder="搜索员工姓名 / 合同编号..." class="search-input" clearable />
          <el-select v-model="contractStatus" placeholder="状态筛选" clearable style="width:160px">
            <el-option v-for="s in contractStatusOptions" :key="s" :label="s" :value="s" />
          </el-select>
        </div>
        <el-table :data="filteredContracts" stripe class="data-table" v-loading="loading">
          <el-table-column prop="contractNo" label="合同编号 · No." width="170" show-overflow-tooltip />
          <el-table-column prop="staffName" label="员工 · Staff" width="120" />
          <el-table-column prop="contractType" label="合同类型 · Type" width="150" show-overflow-tooltip />
          <el-table-column prop="startDate" label="生效日期 · Start" width="130" />
          <el-table-column prop="endDate" label="到期日期 · End" width="130" />
          <el-table-column label="剩余天数 · Days Left" width="150" align="center">
            <template #default="{ row }">
              <el-tag :type="expiryTag(row.endDate)" size="small" effect="plain">{{ daysLeftText(row.endDate) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态 · Status" width="120" />
          <el-table-column prop="remark" label="备注 · Remark" min-width="160" show-overflow-tooltip />
          <template #empty><span class="empty-text">暂无合同记录</span></template>
        </el-table>
        <div class="table-footer">共 {{ filteredContracts.length }} 条合同记录</div>
      </el-tab-pane>

      <el-tab-pane label="证照资质 · Licenses" name="license">
        <div class="filter-bar">
          <el-input v-model="licenseSearch" placeholder="搜索员工姓名 / 证照编号..." class="search-input" clearable />
        </div>
        <el-table :data="filteredLicenses" stripe class="data-table" v-loading="loading">
          <el-table-column prop="licenseNo" label="证照编号 · No." width="170" show-overflow-tooltip />
          <el-table-column prop="staffName" label="持证人 · Holder" width="120" />
          <el-table-column prop="licenseType" label="证照类型 · Type" width="170" show-overflow-tooltip />
          <el-table-column prop="issueDate" label="发证日期 · Issue" width="130" />
          <el-table-column prop="expireDate" label="到期日期 · Expire" width="130" />
          <el-table-column label="剩余天数 · Days Left" width="150" align="center">
            <template #default="{ row }">
              <el-tag :type="expiryTag(row.expireDate)" size="small" effect="plain">{{ daysLeftText(row.expireDate) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态 · Status" width="120" />
          <template #empty><span class="empty-text">暂无证照记录</span></template>
        </el-table>
        <div class="table-footer">共 {{ filteredLicenses.length }} 条证照记录</div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const loading = ref(false)
const loadError = ref('')
const activeTab = ref('contract')
const contracts = ref([])
const licenses = ref([])
const contractSearch = ref('')
const contractStatus = ref('')
const licenseSearch = ref('')

/** 到期预警阈值：30 天内到期视为预警 */
const EXPIRY_WARN_DAYS = 30

function daysLeft(dateStr) {
  if (!dateStr) return null
  const target = new Date(dateStr)
  if (Number.isNaN(target.getTime())) return null
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  target.setHours(0, 0, 0, 0)
  return Math.round((target - today) / 86400000)
}

function daysLeftText(dateStr) {
  const days = daysLeft(dateStr)
  if (days === null) return '未填写'
  if (days < 0) return `已过期 ${Math.abs(days)} 天`
  if (days === 0) return '今日到期'
  return `${days} 天`
}

function expiryTag(dateStr) {
  const days = daysLeft(dateStr)
  if (days === null) return 'info'
  if (days < 0) return 'danger'
  if (days <= EXPIRY_WARN_DAYS) return 'warning'
  return 'success'
}

const contractStatusOptions = computed(() => {
  const set = new Set(contracts.value.map(c => c.status).filter(Boolean))
  return [...set]
})

const filteredContracts = computed(() => {
  const keyword = contractSearch.value.trim().toLowerCase()
  return contracts.value.filter(c => {
    const matchKeyword = !keyword
      || String(c.staffName || '').toLowerCase().includes(keyword)
      || String(c.contractNo || '').toLowerCase().includes(keyword)
    const matchStatus = !contractStatus.value || c.status === contractStatus.value
    return matchKeyword && matchStatus
  })
})

const filteredLicenses = computed(() => {
  const keyword = licenseSearch.value.trim().toLowerCase()
  if (!keyword) return licenses.value
  return licenses.value.filter(l =>
    String(l.staffName || '').toLowerCase().includes(keyword)
    || String(l.licenseNo || '').toLowerCase().includes(keyword))
})

const stats = computed(() => {
  const expiringContracts = contracts.value.filter(c => {
    const d = daysLeft(c.endDate)
    return d !== null && d >= 0 && d <= EXPIRY_WARN_DAYS
  }).length
  const expiredLicenses = licenses.value.filter(l => {
    const d = daysLeft(l.expireDate)
    return d !== null && d < 0
  }).length
  return [
    { label: '劳动合同总数 · Contracts', value: contracts.value.length, cls: 'c-total' },
    { label: `${EXPIRY_WARN_DAYS} 天内到期合同 · Expiring`, value: expiringContracts, cls: 'c-warn' },
    { label: '证照总数 · Licenses', value: licenses.value.length, cls: 'c-total' },
    { label: '已过期证照 · Expired', value: expiredLicenses, cls: 'c-danger' }
  ]
})

function normalizeList(res) {
  const data = res?.data ?? res
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.list)) return data.list
  if (Array.isArray(data?.records)) return data.records
  return []
}

async function loadAll() {
  loading.value = true
  loadError.value = ''
  const storeId = userStore.storeId || 1
  // 数据全部来自本系统后端，不发起任何外部请求
  const [contractRes, licenseRes] = await Promise.allSettled([
    request.get('/hr/contract', { params: { storeId } }),
    request.get('/hr/license', { params: { storeId } })
  ])

  const failed = []
  if (contractRes.status === 'fulfilled') {
    contracts.value = normalizeList(contractRes.value)
  } else {
    contracts.value = []
    failed.push('劳动合同')
  }
  if (licenseRes.status === 'fulfilled') {
    licenses.value = normalizeList(licenseRes.value)
  } else {
    licenses.value = []
    failed.push('证照资质')
  }
  if (failed.length) {
    loadError.value = `${failed.join(' / ')}数据加载失败，请稍后重试或联系管理员`
  }
  loading.value = false
}

onMounted(loadAll)
</script>

<style scoped>
.legal-page {
  padding: 20px 24px;
}

.page-topbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
  gap: 16px;
  flex-wrap: wrap;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 4px;
  letter-spacing: 1px;
}

.page-desc {
  font-size: 13px;
  color: var(--color-text-muted);
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.stat-card {
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 16px 18px;
}

.stat-num {
  font-size: 26px;
  font-weight: 700;
  color: var(--color-text);
  line-height: 1.2;
}

.stat-label {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-top: 6px;
}

.stat-card.c-warn .stat-num { color: #E6A23C; }
.stat-card.c-danger .stat-num { color: #F56C6C; }

.load-error {
  margin-bottom: 16px;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.search-input {
  width: 260px;
}

.data-table {
  width: 100%;
}

.table-footer {
  margin-top: 12px;
  font-size: 12px;
  color: var(--color-text-muted);
}

.empty-text {
  color: var(--color-text-muted);
  font-size: 13px;
}
</style>
