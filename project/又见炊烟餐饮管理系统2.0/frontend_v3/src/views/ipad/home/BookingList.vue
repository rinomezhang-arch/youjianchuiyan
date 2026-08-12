<template>
  <div class="ipad-page">
    <div class="page-top">
      <button class="back-link" @click="router.push('/ipad/home')">← 返回桌台</button>
      <h1 class="page-title">今日预定 · Today's Bookings</h1>
    </div>
    <div class="booking-list">
      <div v-for="b in bookings" :key="b.booking_id" class="booking-card" @click="goToOrder(b)">
        <div class="bc-left">
          <div class="bc-name">{{ b.customer_name }}</div>
          <div class="bc-phone">{{ b.customer_phone }}</div>
        </div>
        <div class="bc-center">
          <div class="bc-time">{{ b.booking_time?.slice(0, 5) }}</div>
          <div class="bc-count">{{ b.guest_count }}人</div>
        </div>
        <div class="bc-right">
          <span :class="['status-tag', b.booking_status]">{{ statusText(b.booking_status) }}</span>
        </div>
      </div>
      <div v-if="!bookings.length" class="empty-state">
        <p>今日暂无预定</p>
        <p class="empty-en">No bookings today</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useIpadStore } from '@/store/ipad'
import { ipadBookingToday } from '@/api/ipad'

const router = useRouter()
const ipad = useIpadStore()
const bookings = ref([])

function statusText(s) {
  const map = { confirmed: '已确认', pending: '待确认', cancelled: '已取消', completed: '已完成' }
  return map[s] || s
}

function goToOrder(b) {
  ipad.openTable(b)
  router.push(`/ipad/order/${b.booking_id}`)
}

onMounted(async () => {
  try {
    const res = await ipadBookingToday()
    if (res.code === 200) bookings.value = res.data || []
  } catch {
    bookings.value = [
      { booking_id: 'B001', customer_name: '张先生', customer_phone: '138****8000', booking_time: '18:00:00', guest_count: 10, booking_status: 'confirmed' },
      { booking_id: 'B002', customer_name: '李女士', customer_phone: '139****6000', booking_time: '19:30:00', guest_count: 6, booking_status: 'pending' },
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
.booking-list { flex: 1; overflow-y: auto; padding: 20px 24px; display: flex; flex-direction: column; gap: 12px; }
.booking-card {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px; background: var(--color-card);
  border: 1px solid var(--color-border); border-radius: var(--radius-lg);
  cursor: pointer; transition: all 0.2s;
}
.booking-card:hover { border-color: var(--color-primary); box-shadow: var(--shadow-md); }
.bc-name { font-size: 16px; font-weight: 600; color: var(--color-text); }
.bc-phone { font-size: 13px; color: var(--color-text-muted); margin-top: 2px; }
.bc-center { text-align: center; }
.bc-time { font-size: 20px; font-weight: 700; color: var(--color-primary); }
.bc-count { font-size: 13px; color: var(--color-text-secondary); }
.status-tag { padding: 4px 10px; border-radius: 4px; font-size: 12px; font-weight: 600; }
.status-tag.confirmed { background: var(--color-tag-green); color: var(--color-success); }
.status-tag.pending { background: var(--color-tag-yellow); color: var(--color-warning); }
.status-tag.cancelled { background: var(--color-tag-red); color: var(--color-danger); }
.status-tag.completed { background: var(--color-tag-blue); color: var(--color-info); }
.empty-state { text-align: center; padding: 60px; color: var(--color-text-muted); }
.empty-en { font-size: 12px; margin-top: 4px; }
</style>
