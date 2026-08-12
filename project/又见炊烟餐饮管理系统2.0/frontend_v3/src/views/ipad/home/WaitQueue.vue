<template>
  <div class="ipad-page">
    <div class="page-top">
      <button class="back-link" @click="router.push('/ipad/home')">← 返回桌台</button>
      <h1 class="page-title">等位队列 · Wait Queue</h1>
    </div>
    <div class="wait-list">
      <div v-for="(w, i) in waitList" :key="w.booking_id || i" class="wait-card">
        <div class="wait-num">{{ i + 1 }}</div>
        <div class="wait-info">
          <div class="wait-name">{{ w.customer_name }}</div>
          <div class="wait-count">{{ w.guest_count }}人</div>
        </div>
        <div class="wait-time">{{ w.booking_time?.slice(0, 5) || '--:--' }}</div>
      </div>
      <div v-if="!waitList.length" class="empty-state">
        <p>暂无等位</p>
        <p class="empty-en">No one waiting</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ipadWaitList } from '@/api/ipad'

const router = useRouter()
const waitList = ref([])

onMounted(async () => {
  try {
    const res = await ipadWaitList()
    if (res.code === 200) waitList.value = res.data || []
  } catch {
    waitList.value = [
      { booking_id: 'W001', customer_name: '王先生', guest_count: 4, booking_time: '18:30:00' },
      { booking_id: 'W002', customer_name: '赵女士', guest_count: 2, booking_time: '19:00:00' },
    ]
  }
})
</script>

<style scoped>
.ipad-page { width: 100%; height: 100%; display: flex; flex-direction: column; background: var(--color-bg); }
.page-top { padding: 16px 24px; background: var(--color-card); border-bottom: 1px solid var(--color-border); display: flex; align-items: center; gap: 16px; flex-shrink: 0; }
.back-link { border: none; background: none; color: var(--color-text-muted); font-size: 13px; cursor: pointer; }
.back-link:hover { color: var(--color-primary); }
.page-title { font-size: 18px; font-weight: 700; color: var(--color-text); letter-spacing: 2px; }
.wait-list { flex: 1; overflow-y: auto; padding: 20px 24px; display: flex; flex-direction: column; gap: 10px; }
.wait-card {
  display: flex; align-items: center; gap: 16px;
  padding: 16px 20px; background: var(--color-card);
  border: 1px solid var(--color-border); border-radius: var(--radius-lg);
}
.wait-num { width: 36px; height: 36px; border-radius: 50%; background: var(--color-primary); color: white; font-size: 16px; font-weight: 700; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.wait-name { font-size: 16px; font-weight: 600; color: var(--color-text); }
.wait-count { font-size: 13px; color: var(--color-text-secondary); }
.wait-time { margin-left: auto; font-size: 18px; font-weight: 700; color: var(--color-accent-dark); }
.empty-state { text-align: center; padding: 60px; color: var(--color-text-muted); }
.empty-en { font-size: 12px; margin-top: 4px; }
</style>
