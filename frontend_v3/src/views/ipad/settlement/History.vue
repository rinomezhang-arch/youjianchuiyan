<template>
  <div class="ipad-page">
    <div class="page-top">
      <button class="back-link" @click="router.push('/ipad/home')">← 返回桌台</button>
      <h1 class="page-title">历史订单 · History</h1>
    </div>
    <div class="history-list">
      <div v-for="h in history" :key="h.booking_id" class="history-card">
        <div class="hc-left">
          <div class="hc-name">{{ h.customer_name }}</div>
          <div class="hc-date">{{ h.booking_date }}</div>
        </div>
        <div class="hc-right">
          <div class="hc-amount">¥{{ h.final_amount?.toFixed(2) || h.total_amount?.toFixed(2) }}</div>
          <span :class="['status-tag', h.payment_status]">{{ h.payment_status === 'paid' ? '已支付' : '未支付' }}</span>
        </div>
      </div>
      <div v-if="!history.length" class="empty-state">
        <p>暂无历史订单</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ipadSettlementHistory } from '@/api/ipad'

const router = useRouter()
const history = ref([])

onMounted(async () => {
  try {
    const res = await ipadSettlementHistory({ pageNum: 1, pageSize: 20 })
    if (res.code === 200) history.value = res.data?.list || res.data || []
  } catch {
    history.value = [
      { booking_id: 'B001', customer_name: '张先生', booking_date: '2026-07-25', total_amount: 1288, final_amount: 1188, payment_status: 'paid' },
      { booking_id: 'B002', customer_name: '李女士', booking_date: '2026-07-25', total_amount: 688, final_amount: 688, payment_status: 'paid' },
    ]
  }
})
</script>

<style scoped>
.ipad-page { width: 100%; height: 100%; display: flex; flex-direction: column; background: var(--color-bg); }
.page-top { padding: 16px 24px; background: var(--color-card); border-bottom: 1px solid var(--color-border); display: flex; align-items: center; gap: 16px; flex-shrink: 0; }
.back-link { border: none; background: none; color: var(--color-text-muted); font-size: 13px; cursor: pointer; }
.page-title { font-size: 18px; font-weight: 700; color: var(--color-text); letter-spacing: 2px; }
.history-list { flex: 1; overflow-y: auto; padding: 20px 24px; display: flex; flex-direction: column; gap: 10px; }
.history-card { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-lg); }
.hc-name { font-size: 16px; font-weight: 600; color: var(--color-text); }
.hc-date { font-size: 13px; color: var(--color-text-muted); margin-top: 2px; }
.hc-right { text-align: right; }
.hc-amount { font-size: 18px; font-weight: 700; color: var(--color-accent-dark); }
.status-tag { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 12px; font-weight: 600; margin-top: 4px; }
.status-tag.paid { background: var(--color-tag-green); color: var(--color-success); }
.status-tag.unpaid { background: var(--color-tag-yellow); color: var(--color-warning); }
.empty-state { text-align: center; padding: 60px; color: var(--color-text-muted); }
</style>
