<template>
  <div class="ai-page">
    <div class="page-header">
      <h1 class="page-title">AI助手 · AI Assistant</h1>
      <p class="page-subtitle">让AI帮您策划完美宴会</p>
    </div>
    <div class="ai-card">
      <div class="card-tabs">
        <button v-for="tab in tabs" :key="tab.key" :class="['tab-btn', { active: activeTab === tab.key }]" @click="activeTab = tab.key">
          <span class="tab-icon">{{ tab.icon }}</span>
          <span>{{ tab.name }}</span>
        </button>
      </div>
      <div class="card-content">
        <div v-if="activeTab === 'banquet'" class="form-section">
          <h3 class="section-title">宴会策划</h3>
          <el-form :model="banquetForm" label-width="100px">
            <el-form-item label="宴会场合">
              <el-select v-model="banquetForm.occasion" placeholder="请选择场合">
                <el-option label="生日宴" value="生日宴"></el-option>
                <el-option label="婚宴" value="婚宴"></el-option>
                <el-option label="寿宴" value="寿宴"></el-option>
                <el-option label="公司年会" value="公司年会"></el-option>
                <el-option label="商务宴请" value="商务宴请"></el-option>
                <el-option label="朋友聚会" value="朋友聚会"></el-option>
                <el-option label="其他" value="其他"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="宾客人数">
              <el-input-number v-model="banquetForm.guestCount" :min="1" :max="200" placeholder="请输入人数"></el-input-number>
            </el-form-item>
            <el-form-item label="饮食偏好">
              <el-input v-model="banquetForm.preferences" placeholder="例如：海鲜、素食、无辣等"></el-input>
            </el-form-item>
          </el-form>
          <button class="submit-btn" @click="generateBanquet" :disabled="loading">
            <span v-if="loading">生成中...</span>
            <span v-else>✨ 生成宴会方案</span>
          </button>
        </div>
        <div v-if="activeTab === 'dish'" class="form-section">
          <h3 class="section-title">菜品推荐</h3>
          <el-form :model="dishForm" label-width="100px">
            <el-form-item label="宴会场合">
              <el-select v-model="dishForm.occasion" placeholder="请选择场合">
                <el-option label="生日宴" value="生日宴"></el-option>
                <el-option label="婚宴" value="婚宴"></el-option>
                <el-option label="寿宴" value="寿宴"></el-option>
                <el-option label="公司年会" value="公司年会"></el-option>
                <el-option label="商务宴请" value="商务宴请"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="宾客人数">
              <el-input-number v-model="dishForm.guestCount" :min="1" :max="200" placeholder="请输入人数"></el-input-number>
            </el-form-item>
            <el-form-item label="饮食限制">
              <el-input v-model="dishForm.dietaryRestrictions" placeholder="例如：有素食者、海鲜过敏等"></el-input>
            </el-form-item>
          </el-form>
          <button class="submit-btn" @click="generateDish" :disabled="loading">
            <span v-if="loading">生成中...</span>
            <span v-else>🍽️ 推荐菜品</span>
          </button>
        </div>
        <div v-if="activeTab === 'copy'" class="form-section">
          <h3 class="section-title">营销文案</h3>
          <el-form :model="copyForm" label-width="100px">
            <el-form-item label="文案类型">
              <el-select v-model="copyForm.type" placeholder="请选择类型">
                <el-option label="宴会宣传" value="宴会"></el-option>
                <el-option label="菜品介绍" value="菜品"></el-option>
                <el-option label="节日祝福" value="节日"></el-option>
                <el-option label="朋友圈文案" value="朋友圈"></el-option>
                <el-option label="活动海报" value="海报"></el-option>
                <el-option label="其他" value="其他"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="关键词">
              <el-input v-model="copyForm.keywords" placeholder="例如：浪漫、高端、定制"></el-input>
            </el-form-item>
          </el-form>
          <button class="submit-btn" @click="generateCopy" :disabled="loading">
            <span v-if="loading">生成中...</span>
            <span v-else>📝 生成文案</span>
          </button>
        </div>
      </div>
    </div>
    <div v-if="result" class="result-card">
      <div class="result-header">
        <h3 class="result-title">🎯 AI生成结果</h3>
        <button class="copy-btn" @click="copyResult">复制内容</button>
      </div>
      <pre class="result-content">{{ result }}</pre>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { generateBanquetSuggestion, generateDishRecommendation, generateCopywriting } from '@/api/ai'

const activeTab = ref('banquet')
const loading = ref(false)
const result = ref('')

const tabs = [
  { key: 'banquet', name: '宴会建议', icon: '🎂' },
  { key: 'dish', name: '菜品推荐', icon: '🍽️' },
  { key: 'copy', name: '文案生成', icon: '📝' }
]

const banquetForm = ref({ occasion: '', guestCount: 10, preferences: '' })
const dishForm = ref({ occasion: '', guestCount: 10, dietaryRestrictions: '' })
const copyForm = ref({ type: '', keywords: '' })

const generateBanquet = async () => {
  if (!banquetForm.value.occasion) { ElMessage.warning('请选择宴会场合'); return }
  loading.value = true
  try { const res = await generateBanquetSuggestion(banquetForm.value); result.value = res.data }
  catch { ElMessage.error('生成失败，请稍后重试') }
  finally { loading.value = false }
}

const generateDish = async () => {
  if (!dishForm.value.occasion) { ElMessage.warning('请选择宴会场合'); return }
  loading.value = true
  try { const res = await generateDishRecommendation(dishForm.value); result.value = res.data }
  catch { ElMessage.error('生成失败，请稍后重试') }
  finally { loading.value = false }
}

const generateCopy = async () => {
  if (!copyForm.value.type) { ElMessage.warning('请选择文案类型'); return }
  loading.value = true
  try { const res = await generateCopywriting(copyForm.value); result.value = res.data }
  catch { ElMessage.error('生成失败，请稍后重试') }
  finally { loading.value = false }
}

const copyResult = () => {
  navigator.clipboard.writeText(result.value)
  ElMessage.success('复制成功')
}
</script>

<style scoped>
.ai-page { max-width: 900px; margin: 0 auto; }
.page-header { text-align: center; margin-bottom: 32px; }
.page-title { font-size: 28px; font-weight: 700; color: var(--color-text); margin-bottom: 8px; }
.page-subtitle { font-size: 14px; color: var(--color-text-muted); }
.ai-card { background: var(--color-card); border-radius: var(--radius-lg); padding: 24px; margin-bottom: 24px; box-shadow: var(--shadow-sm); }
.card-tabs { display: flex; gap: 8px; margin-bottom: 24px; padding-bottom: 16px; border-bottom: 1px solid var(--color-border); }
.tab-btn { display: flex; align-items: center; gap: 8px; padding: 10px 20px; border: none; border-radius: var(--radius-md); background: transparent; color: var(--color-text-secondary); font-size: 14px; font-weight: 500; cursor: pointer; transition: var(--transition); }
.tab-btn:hover { background: var(--color-bg-alt); color: var(--color-text); }
.tab-btn.active { background: rgba(79,70,229,0.08); color: var(--color-primary); font-weight: 600; }
.tab-icon { font-size: 16px; }
.card-content { min-height: 200px; }
.form-section { max-width: 500px; }
.section-title { font-size: 16px; font-weight: 600; color: var(--color-text); margin-bottom: 20px; }
.submit-btn { width: 100%; height: 44px; margin-top: 24px; background: linear-gradient(135deg, #4F46E5, #7C3AED); color: #fff; border: none; border-radius: var(--radius-md); font-size: 15px; font-weight: 600; cursor: pointer; transition: var(--transition); }
.submit-btn:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(79,70,229,0.4); }
.submit-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.result-card { background: var(--color-card); border-radius: var(--radius-lg); padding: 24px; box-shadow: var(--shadow-sm); }
.result-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid var(--color-border); }
.result-title { font-size: 16px; font-weight: 600; color: var(--color-text); }
.copy-btn { padding: 6px 16px; background: var(--color-bg-alt); color: var(--color-text-secondary); border: none; border-radius: var(--radius-sm); font-size: 13px; cursor: pointer; transition: var(--transition); }
.copy-btn:hover { background: var(--color-border); color: var(--color-text); }
.result-content { max-height: 600px; overflow-y: auto; font-size: 14px; line-height: 1.8; color: var(--color-text-secondary); white-space: pre-wrap; font-family: inherit; }
</style>
