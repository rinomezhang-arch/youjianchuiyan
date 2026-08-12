<template>
  <div class="empty-state-wrap">
    <div class="empty-card">
      <h2 class="empty-title">{{ title }}</h2>
      <p v-if="desc" class="empty-desc">{{ desc }}</p>

      <div v-if="stats && Object.keys(stats).length" class="empty-stats">
        <div v-for="(val, key) in stats" :key="key" class="stat-item">
          <span class="stat-val">{{ val }}</span>
          <span class="stat-label">{{ key }}</span>
        </div>
      </div>

      <el-button
        v-if="actionPath && actionLabel"
        type="primary"
        plain
        @click="goAction"
      >{{ actionLabel }}</el-button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const props = defineProps({
  title: { type: String, default: '暂无数据' },
  desc: { type: String, default: '' },
  actionPath: { type: String, default: '' },
  actionLabel: { type: String, default: '' },
  stats: { type: Object, default: () => ({}) }
})

const router = useRouter()
function goAction() {
  if (props.actionPath) router.push(props.actionPath)
}
</script>

<style scoped>
.empty-state-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 60vh;
  padding: 24px;
}
.empty-card {
  text-align: center;
  padding: 48px 40px;
  background: #fdfbf7;
  border: 1px solid #e7e1d6;
  border-radius: 8px;
  max-width: 520px;
  width: 100%;
}
.empty-title {
  font-size: 20px;
  color: #3a3a3a;
  margin: 0 0 12px;
  font-weight: 600;
}
.empty-desc {
  font-size: 13px;
  color: #9ca3af;
  margin: 0 0 24px;
  line-height: 1.6;
}
.empty-stats {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 32px;
  margin-bottom: 28px;
}
.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
.stat-val {
  font-size: 28px;
  font-weight: 700;
  color: #5a8a6a;
}
.stat-label {
  font-size: 12px;
  color: #9ca3af;
}
</style>
