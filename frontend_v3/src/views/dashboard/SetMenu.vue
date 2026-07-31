<template>
  <div class="set-menu-page">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <span class="breadcrumb-link" @click="$router.push('/dashboard/home')">首页</span>
      <span class="breadcrumb-sep">/</span>
      <span class="breadcrumb-current">套餐管理 · Set Menu</span>
      <span class="breadcrumb-desc">套餐配置 · 菜品搭配 · 成本管控</span>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-row">
      <div class="stat-card">
        <div class="stat-icon" style="background:rgba(100,140,120,0.12);">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#5A8A72" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18M9 21V9"/></svg>
        </div>
        <div class="stat-info">
          <span class="stat-label">套餐总数</span>
          <span class="stat-value">{{ packages.length }}</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:rgba(100,160,140,0.12);">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#6BA58C" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
        </div>
        <div class="stat-info">
          <span class="stat-label">在售套餐</span>
          <span class="stat-value">{{ activeCount }}</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:rgba(196,163,90,0.12);">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#C4A35A" stroke-width="2"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>
        </div>
        <div class="stat-info">
          <span class="stat-label">套餐总价值</span>
          <span class="stat-value">¥{{ totalValue }}</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:rgba(180,100,100,0.12);">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#B46464" stroke-width="2"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>
        </div>
        <div class="stat-info">
          <span class="stat-label">套餐毛利率</span>
          <span class="stat-value">{{ avgMargin }}%</span>
        </div>
      </div>
    </div>

    <!-- 操作栏 -->
    <div class="action-bar">
      <div class="action-left">
        <el-input v-model="searchQuery" placeholder="搜索套餐..." clearable size="small" style="width:200px" />
        <el-select v-model="filterCategory" placeholder="宴会分类" clearable size="small" style="width:120px;margin-left:8px">
          <el-option v-for="cat in categories" :key="cat" :label="cat" :value="cat" />
        </el-select>
      </div>
      <el-button type="primary" size="small" @click="createNew">+ 新建套餐</el-button>
    </div>

    <!-- 套餐卡片网格 -->
    <div class="package-grid" v-loading="loading">
      <div v-for="pkg in filteredList" :key="pkg.packageId" class="package-card">
        <!-- 大字背景 -->
        <div class="card-char-bg">{{ getFirstChar(pkg.packageName) }}</div>
        <!-- 卡片内容 -->
        <div class="card-content">
          <div class="card-top">
            <h3 class="card-title">{{ pkg.packageName }}</h3>
            <span class="card-category-tag">{{ pkg.occasionType || pkg.category || '商务宴' }}</span>
          </div>
          <p class="card-desc">{{ pkg.description || getDesc(pkg) }}</p>
          <div class="card-bottom">
            <div class="card-meta">
              <span>¥{{ (pkg.packageTotalPrice || 0).toFixed(0) }}</span>
              <span>{{ pkg.minGuests || 8 }}-{{ pkg.maxGuests || 10 }}人</span>
              <span>{{ pkg.dishCount || 0 }}道菜</span>
            </div>
            <div class="card-actions">
              <el-button text size="small" @click.stop="editPackage(pkg)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                编辑此套餐
              </el-button>
              <el-button text size="small" type="danger" @click.stop="deletePackage(pkg)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                删除
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="!loading && filteredList.length === 0" class="empty-state">
        <p>暂无套餐数据</p>
        <el-button type="primary" size="small" @click="createNew">+ 新建套餐</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getPackages } from '@/api/package'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const packages = ref([])
const searchQuery = ref('')
const filterCategory = ref('')
const categories = ['婚宴', '寿宴', '商务宴', '谢师宴', '满月宴', '团圆宴']

const activeCount = computed(() => packages.value.filter(p => p.status === 'active' || p.status === '在售').length)
const totalValue = computed(() => packages.value.reduce((s, p) => s + (p.packageTotalPrice || 0), 0))
const avgMargin = computed(() => {
  const rates = packages.value.map(p => p.costRate || 0).filter(r => r > 0)
  if (!rates.length) return 0
  return (100 - rates.reduce((a, b) => a + b, 0) / rates.length).toFixed(1)
})

const filteredList = computed(() => {
  let result = packages.value
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    result = result.filter(p =>
      (p.packageName || '').toLowerCase().includes(q) ||
      (p.packageId || '').toLowerCase().includes(q)
    )
  }
  if (filterCategory.value) {
    result = result.filter(p => (p.occasionType || p.category || '') === filterCategory.value)
  }
  return result
})

function getFirstChar(name) {
  if (!name) return '套'
  const char = name.charAt(0)
  // 如果是英文字母，返回大写
  if (/[a-zA-Z]/.test(char)) return char.toUpperCase()
  return char
}

function getDesc(pkg) {
  const parts = []
  if (pkg.occasionType || pkg.category) parts.push(pkg.occasionType || pkg.category)
  if (pkg.minGuests || pkg.maxGuests) parts.push(`${pkg.minGuests || 8}-${pkg.maxGuests || 10}人`)
  if (pkg.dishCount) parts.push(`${pkg.dishCount}道菜`)
  return parts.join(' · ') || '暂无描述'
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getPackages()
    if (res.code === 200) {
      packages.value = res.data?.content || res.data || []
    }
  } catch (e) {
    console.error('获取套餐列表失败', e)
  } finally {
    loading.value = false
  }
}

function createNew() {
  router.push('/dashboard/set-menu-edit')
}

function editPackage(pkg) {
  router.push(`/dashboard/set-menu-edit?id=${pkg.packageId}`)
}

async function deletePackage(pkg) {
  try {
    await ElMessageBox.confirm(`确定删除套餐"${pkg.packageName}"？`, '确认删除', { type: 'warning' })
    ElMessage.success('已删除')
    fetchData()
  } catch {}
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.set-menu-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 20px;
}

/* 面包屑 */
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #8A9A92;
  margin-bottom: 20px;
}
.breadcrumb-link {
  color: #6BA58C;
  cursor: pointer;
}
.breadcrumb-link:hover {
  text-decoration: underline;
}
.breadcrumb-sep {
  color: #B0BEB6;
}
.breadcrumb-current {
  color: #2D4A3E;
  font-weight: 600;
}
.breadcrumb-desc {
  color: #B0BEB6;
  font-size: 12px;
  margin-left: 8px;
}

/* 统计卡片 */
.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
.stat-card {
  background: #FAF8F4;
  border: 1px solid #E8E4DC;
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
}
.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.stat-label {
  font-size: 12px;
  color: #8A9A92;
}
.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #2D4A3E;
}

/* 操作栏 */
.action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

/* 套餐卡片网格 */
.package-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.package-card {
  background: #FAF8F4;
  border: 1px solid #E8E4DC;
  border-radius: 12px;
  overflow: hidden;
  position: relative;
  transition: all 0.3s ease;
  cursor: default;
}
.package-card:hover {
  box-shadow: 0 4px 16px rgba(0,0,0,0.08);
  border-color: #C4A35A;
}

/* 大字背景 */
.card-char-bg {
  font-size: 72px;
  font-weight: 700;
  color: rgba(45, 74, 62, 0.06);
  text-align: center;
  padding: 12px 0 0 0;
  line-height: 1;
  font-family: 'STSong', 'SimSun', serif;
  user-select: none;
}

/* 卡片内容 */
.card-content {
  padding: 12px 16px 16px;
}
.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}
.card-title {
  font-size: 16px;
  font-weight: 700;
  color: #2D4A3E;
  margin: 0;
}
.card-category-tag {
  padding: 2px 8px;
  border-radius: 8px;
  font-size: 11px;
  font-weight: 600;
  background: rgba(180, 100, 100, 0.1);
  color: #B46464;
  flex-shrink: 0;
}
.card-desc {
  font-size: 12px;
  color: #8A9A92;
  margin: 0 0 12px 0;
  line-height: 1.5;
}
.card-bottom {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
}
.card-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #6BA58C;
  font-weight: 600;
}
.card-actions {
  display: flex;
  gap: 4px;
}
.card-actions .el-button {
  font-size: 12px;
  padding: 4px 8px;
}

/* 空状态 */
.empty-state {
  grid-column: 1 / -1;
  text-align: center;
  padding: 60px 0;
  color: #8A9A92;
}
.empty-state p {
  font-size: 15px;
  margin-bottom: 16px;
}

@media (max-width: 1200px) {
  .package-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 768px) {
  .stat-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .package-grid {
    grid-template-columns: 1fr;
  }
}
</style>
