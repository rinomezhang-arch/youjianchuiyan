<template>
  <div class="price-tiers-page">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">多价格体系 · Price Tiers</h2>
        <p class="page-subtitle">多门店价格 · 时段价格 · 会员价格</p>
      </div>
      <div class="page-header-right">
        <el-button type="primary" @click="openAddTier">+ 新增价格体系</el-button>
      </div>
    </div>

    <!-- 价格体系卡片 -->
    <div class="tier-grid">
      <div v-for="tier in tiers" :key="tier.id" class="tier-card" :class="{ active: tier.active }">
        <div class="tier-header">
          <div class="tier-icon" :style="{ background: tier.color }">
            <span>{{ tier.icon }}</span>
          </div>
          <div class="tier-info">
            <h4 class="tier-name">{{ tier.name }}</h4>
            <span class="tier-name-en">{{ tier.nameEn }}</span>
          </div>
          <el-switch v-model="tier.active" size="small" />
        </div>
        <div class="tier-body">
          <div class="tier-desc">{{ tier.description }}</div>
          <div class="tier-stats">
            <div class="stat-item">
              <span class="stat-label">菜品数</span>
              <span class="stat-value">{{ tier.dishCount }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">折扣率</span>
              <span class="stat-value">{{ tier.discount }}%</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">优先级</span>
              <span class="stat-value">{{ tier.priority }}</span>
            </div>
          </div>
        </div>
        <div class="tier-footer">
          <el-button text size="small" @click="editTier(tier)">编辑</el-button>
          <el-button text size="small" @click="viewDishes(tier)">查看菜品</el-button>
          <el-button text size="small" type="danger" @click="deleteTier(tier)">删除</el-button>
        </div>
      </div>

      <div class="tier-add-card" @click="openAddTier">
        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="12" y1="5" x2="12" y2="19"/>
          <line x1="5" y1="12" x2="19" y2="12"/>
        </svg>
        <span>新增价格体系</span>
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="showDialog" :title="editing ? '编辑价格体系' : '新增价格体系'" width="550px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="体系名称" required>
          <el-input v-model="form.name" placeholder="如：VIP价格、午市特惠" />
        </el-form-item>
        <el-form-item label="英文名">
          <el-input v-model="form.nameEn" placeholder="English name" />
        </el-form-item>
        <el-form-item label="体系类型" required>
          <el-select v-model="form.type" class="full-width">
            <el-option label="门店价格" value="store" />
            <el-option label="时段价格" value="time" />
            <el-option label="会员价格" value="member" />
            <el-option label="活动价格" value="event" />
          </el-select>
        </el-form-item>
        <el-form-item label="默认折扣">
          <el-input-number v-model="form.discount" :precision="1" :min="0" :max="200" :step="5" controls-position="right" class="full-width" />
          <span class="form-hint">百分比，100%为原价</span>
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="form.priority" :min="1" :max="10" controls-position="right" />
          <span class="form-hint">数字越小优先级越高</span>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="描述说明" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="emoji图标" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="saveTier">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const showDialog = ref(false)
const editing = ref(false)
const form = ref({ id: '', name: '', nameEn: '', type: 'store', discount: 100, priority: 5, description: '', icon: '🏷️', color: '#2D4A3E' })

const tiers = ref([
  { id: 1, name: '标准价格', nameEn: 'Standard', type: 'store', icon: '📋', color: '#2D4A3E', description: '门店标准售价体系', discount: 100, priority: 1, dishCount: 85, active: true },
  { id: 2, name: 'VIP价格', nameEn: 'VIP', type: 'member', icon: '👑', color: '#C4A35A', description: 'VIP会员专享9折', discount: 90, priority: 2, dishCount: 60, active: true },
  { id: 3, name: '午市特惠', nameEn: 'Lunch Special', type: 'time', icon: '☀️', color: '#4A7C59', description: '工作日午市85折', discount: 85, priority: 3, dishCount: 45, active: false },
  { id: 4, name: '宣城店价格', nameEn: 'Xuancheng', type: 'store', icon: '🏪', color: '#5B7B8A', description: '宣城门店独立定价', discount: 95, priority: 4, dishCount: 30, active: true }
])

function openAddTier() {
  editing.value = false
  form.value = { id: '', name: '', nameEn: '', type: 'store', discount: 100, priority: 5, description: '', icon: '🏷️', color: '#2D4A3E' }
  showDialog.value = true
}

function editTier(tier) {
  editing.value = true
  form.value = { ...tier }
  showDialog.value = true
}

function saveTier() {
  if (!form.value.name) { ElMessage.warning('请输入体系名称'); return }
  if (editing.value) {
    const idx = tiers.value.findIndex(t => t.id === form.value.id)
    if (idx >= 0) tiers.value[idx] = { ...form.value, dishCount: tiers.value[idx].dishCount }
  } else {
    tiers.value.push({ ...form.value, id: Date.now(), dishCount: 0, active: true })
  }
  ElMessage.success('保存成功')
  showDialog.value = false
}

async function deleteTier(tier) {
  try {
    await ElMessageBox.confirm(`确定删除价格体系"${tier.name}"？`, '确认删除', { type: 'warning' })
    tiers.value = tiers.value.filter(t => t.id !== tier.id)
    ElMessage.success('已删除')
  } catch (e) { /* cancel */ }
}

function viewDishes(tier) {
  ElMessage.info(`查看"${tier.name}"菜品列表`)
}

onMounted(() => {})
</script>

<style scoped>
.price-tiers-page { max-width: 1400px; margin: 0 auto; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; flex-wrap: wrap; gap: 12px; }
.page-title { font-size: 22px; font-weight: 700; color: var(--color-text); margin-bottom: 4px; }
.page-subtitle { font-size: 13px; color: var(--color-text-muted); }
.tier-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 20px; }
.tier-card { background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-lg); overflow: hidden; transition: var(--transition); }
.tier-card:hover { box-shadow: var(--shadow-lg); }
.tier-card.active { border-color: var(--color-accent); }
.tier-header { display: flex; align-items: center; gap: 12px; padding: 20px; border-bottom: 1px solid var(--color-border-light); }
.tier-icon { width: 44px; height: 44px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 22px; }
.tier-info { flex: 1; }
.tier-name { font-size: 16px; font-weight: 600; color: var(--color-text); margin: 0 0 2px; }
.tier-name-en { font-size: 11px; color: var(--color-text-muted); }
.tier-body { padding: 16px 20px; }
.tier-desc { font-size: 13px; color: var(--color-text-secondary); margin-bottom: 16px; line-height: 1.5; }
.tier-stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.stat-item { display: flex; flex-direction: column; gap: 2px; text-align: center; padding: 8px; background: var(--color-bg-alt); border-radius: var(--radius-sm); }
.stat-label { font-size: 11px; color: var(--color-text-muted); }
.stat-value { font-size: 15px; font-weight: 700; color: var(--color-text); }
.tier-footer { display: flex; gap: 8px; padding: 12px 20px; border-top: 1px solid var(--color-border-light); }
.tier-add-card { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; padding: 40px; border: 2px dashed var(--color-border); border-radius: var(--radius-lg); cursor: pointer; color: var(--color-text-muted); font-size: 14px; transition: all 0.2s; min-height: 200px; }
.tier-add-card:hover { border-color: var(--color-accent); color: var(--color-accent-dark); }
.full-width { width: 100%; }
.form-hint { display: block; font-size: 11px; color: var(--color-text-muted); margin-top: 4px; }
</style>
