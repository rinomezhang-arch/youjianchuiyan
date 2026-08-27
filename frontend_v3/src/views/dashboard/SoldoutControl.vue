<template>
  <div class="soldout-control-page">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">沽清管控 · Soldout Control</h2>
        <p class="page-subtitle">菜品沽清设置 · 临时下架 · 库存预警</p>
      </div>
      <div class="page-header-right">
        <el-input v-model="searchQuery" placeholder="搜索菜品..." clearable class="search-input" />
        <el-button @click="toggleSoldoutFilter" :type="showOnlySoldout ? 'primary' : 'default'">
          {{ showOnlySoldout ? '显示全部' : '仅显示沽清' }}
        </el-button>
      </div>
    </div>

    <!-- 统计 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-label">总菜品数</div>
        <div class="stat-value">{{ allDishes.length }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">在售</div>
        <div class="stat-value stat-success">{{ allDishes.filter(d => !d.soldout).length }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">沽清</div>
        <div class="stat-value stat-danger">{{ allDishes.filter(d => d.soldout).length }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">临时沽清</div>
        <div class="stat-value stat-warning">{{ allDishes.filter(d => d.soldout && d.tempSoldout).length }}</div>
      </div>
    </div>

    <!-- 菜品沽清列表 -->
    <div class="soldout-grid">
      <div
        v-for="dish in filteredDishes"
        :key="dish.dishId"
        :class="['soldout-card', { 'is-soldout': dish.soldout }]"
      >
        <div class="card-image">
          <img v-if="dish.imageUrl || dish.image" :src="dish.imageUrl || dish.image" :alt="dish.dishName" />
          <div v-else class="img-placeholder">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <rect x="3" y="3" width="18" height="18" rx="2"/>
              <circle cx="8.5" cy="8.5" r="1.5"/>
              <polyline points="21 15 16 10 5 21"/>
            </svg>
          </div>
          <div class="soldout-badge" v-if="dish.soldout">沽清</div>
        </div>
        <div class="card-content">
          <div class="dish-name">{{ dish.dishName }}</div>
          <div class="dish-meta">
            <el-tag size="small">{{ dish.categoryName || dish.category }}</el-tag>
            <span class="dish-price">¥{{ (dish.salePrice || 0).toFixed(0) }}</span>
          </div>
          <div class="card-actions">
            <el-switch
              :model-value="dish.soldout"
              @change="(val) => toggleSoldout(dish, val)"
              active-text="沽清"
              inactive-text="在售"
              inline-prompt
            />
            <el-dropdown trigger="click" @command="(cmd) => handleCommand(cmd, dish)">
              <el-button text size="small">更多</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="temp">临时沽清（今日）</el-dropdown-item>
                  <el-dropdown-item command="timed">定时沽清</el-dropdown-item>
                  <el-dropdown-item command="reason">设置原因</el-dropdown-item>
                  <el-dropdown-item command="restore" v-if="dish.soldout">恢复在售</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          <div class="soldout-reason" v-if="dish.soldoutReason">
            原因: {{ dish.soldoutReason }}
          </div>
        </div>
      </div>
    </div>

    <!-- 定时沽清弹窗 -->
    <el-dialog v-model="showTimedDialog" title="定时沽清" width="400px">
      <el-form label-width="80px">
        <el-form-item label="菜品">
          <span>{{ currentDish?.dishName }}</span>
        </el-form-item>
        <el-form-item label="恢复时间">
          <el-date-picker
            v-model="timedRestoreAt"
            type="datetime"
            placeholder="选择恢复在售时间"
            class="full-width"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showTimedDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmTimed">确定</el-button>
      </template>
    </el-dialog>

    <!-- 设置原因弹窗 -->
    <el-dialog v-model="showReasonDialog" title="设置沽清原因" width="400px">
      <el-input v-model="reasonText" type="textarea" :rows="3" placeholder="请输入沽清原因..." />
      <template #footer>
        <el-button @click="showReasonDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmReason">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getDishes, updateDish } from '@/api/dish'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const currentStoreId = computed(() => userStore.currentStore?.storeId || userStore.stores?.[0]?.storeId || 1)

const loading = ref(false)
const allDishes = ref([])
const searchQuery = ref('')
const showOnlySoldout = ref(false)
const showTimedDialog = ref(false)
const showReasonDialog = ref(false)
const currentDish = ref(null)
const timedRestoreAt = ref(null)
const reasonText = ref('')

const filteredDishes = computed(() => {
  let result = allDishes.value
  if (showOnlySoldout.value) result = result.filter(d => d.soldout)
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    result = result.filter(d => (d.dishName || '').toLowerCase().includes(q))
  }
  return result
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getDishes({ storeId: currentStoreId.value })
    allDishes.value = (res.data || []).map(d => ({
      ...d,
      // 真实 status 只有 active/inactive 两种值，从未有过 'soldout'
      soldout: d.status !== 'active',
      tempSoldout: d.tempSoldout || false,
      soldoutReason: d.soldoutReason || ''
    }))
  } catch (e) { console.error(e); ElMessage.error('获取菜品列表失败') }
  finally { loading.value = false }
}

function toggleSoldoutFilter() {
  showOnlySoldout.value = !showOnlySoldout.value
}

async function toggleSoldout(dish, val) {
  dish.soldout = val
  dish.tempSoldout = false
  if (!val) dish.soldoutReason = ''
  try {
    await updateDish(dish.dishId, { status: val ? 'inactive' : 'active' }, currentStoreId.value)
    ElMessage.success(val ? `"${dish.dishName}"已沽清` : `"${dish.dishName}"已恢复在售`)
  } catch (e) {
    dish.soldout = !val
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

function handleCommand(cmd, dish) {
  currentDish.value = dish
  switch (cmd) {
    case 'temp':
      dish.soldout = true
      dish.tempSoldout = true
      dish.soldoutReason = '临时沽清（今日）'
      updateDish(dish.dishId, { status: 'inactive' }, currentStoreId.value)
      ElMessage.success(`"${dish.dishName}"临时沽清`)
      break
    case 'timed':
      timedRestoreAt.value = null
      showTimedDialog.value = true
      break
    case 'reason':
      reasonText.value = dish.soldoutReason || ''
      showReasonDialog.value = true
      break
    case 'restore':
      toggleSoldout(dish, false)
      break
  }
}

async function confirmTimed() {
  if (!timedRestoreAt.value) { ElMessage.warning('请选择恢复时间'); return }
  currentDish.value.soldout = true
  currentDish.value.tempSoldout = true
  currentDish.value.soldoutReason = `定时沽清，恢复时间：${new Date(timedRestoreAt.value).toLocaleString()}`
  ElMessage.success('定时沽清已设置')
  showTimedDialog.value = false
}

async function confirmReason() {
  currentDish.value.soldoutReason = reasonText.value
  ElMessage.success('原因已设置')
  showReasonDialog.value = false
}

onMounted(fetchData)
</script>

<style scoped>
.soldout-control-page { max-width: 1400px; margin: 0 auto; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; flex-wrap: wrap; gap: 12px; }
.page-title { font-size: 22px; font-weight: 700; color: var(--color-text); margin-bottom: 4px; }
.page-subtitle { font-size: 13px; color: var(--color-text-muted); }
.page-header-right { display: flex; gap: 10px; align-items: center; }
.search-input { width: 220px; }
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
.stat-card { background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-lg); padding: 20px; text-align: center; }
.stat-label { font-size: 12px; color: var(--color-text-muted); margin-bottom: 8px; }
.stat-value { font-size: 28px; font-weight: 700; color: var(--color-text); }
.stat-success { color: var(--color-success); }
.stat-danger { color: var(--color-danger); }
.stat-warning { color: var(--color-warning); }
.soldout-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; }
.soldout-card { background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-lg); overflow: hidden; transition: var(--transition); }
.soldout-card:hover { box-shadow: var(--shadow-md); }
.soldout-card.is-soldout { opacity: 0.75; border-color: var(--color-danger); }
.soldout-card.is-soldout .card-image { position: relative; }
.card-image { height: 140px; overflow: hidden; position: relative; background: var(--color-bg-alt); }
.card-image img { width: 100%; height: 100%; object-fit: cover; }
.img-placeholder { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; color: var(--color-border); }
.soldout-badge { position: absolute; top: 8px; right: 8px; padding: 4px 10px; background: var(--color-danger); color: #fff; font-size: 11px; font-weight: 600; border-radius: 4px; }
.card-content { padding: 14px; }
.dish-name { font-size: 15px; font-weight: 600; color: var(--color-text); margin-bottom: 8px; }
.dish-meta { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.dish-price { font-size: 14px; font-weight: 600; color: var(--color-accent-dark); }
.card-actions { display: flex; align-items: center; justify-content: space-between; }
.soldout-reason { margin-top: 8px; padding: 6px 10px; background: rgba(194, 85, 85, 0.06); border-radius: var(--radius-sm); font-size: 12px; color: var(--color-danger); }
.full-width { width: 100%; }
</style>
