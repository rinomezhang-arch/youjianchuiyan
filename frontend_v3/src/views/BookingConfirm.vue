<template>
  <div class="bc-page">
    <div class="bc-container">
      <div class="bc-header">
        <div class="bc-logo">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M3 3v18h18"/>
            <path d="M18.7 8l-5.1 5.1-2.8-2.8L7 14"/>
          </svg>
        </div>
        <h1 class="bc-title">又见炊烟 · 宴会预订确认</h1>
        <p class="bc-subtitle">Booking Confirmation</p>
      </div>

      <div v-if="loading" class="bc-card bc-loading">加载中...</div>

      <div v-else-if="loadError" class="bc-card bc-error">
        <p>{{ loadError }}</p>
      </div>

      <template v-else>
        <div class="bc-card">
          <div v-if="info.banquetName" class="bc-banquet-name">{{ info.banquetName }}</div>
          <div class="bc-row"><span>预订人</span><strong>{{ info.customerName || '—' }}</strong></div>
          <div class="bc-row"><span>日期</span><strong>{{ info.bookingDate || '—' }}</strong></div>
          <div class="bc-row"><span>时间</span><strong>{{ info.bookingTime || '—' }}</strong></div>
          <div class="bc-row"><span>桌数</span><strong>{{ info.tableCount != null ? info.tableCount + ' 桌' : '—' }}</strong></div>
          <div class="bc-row"><span>人数</span><strong>{{ info.guestCount != null ? info.guestCount + ' 位' : '—' }}</strong></div>
          <div v-if="info.packageName" class="bc-row"><span>套餐</span><strong>{{ info.packageName }}</strong></div>
          <div v-if="info.depositAmount" class="bc-row"><span>定金</span><strong>¥{{ info.depositAmount }}</strong></div>
          <div v-if="info.totalAmount" class="bc-row"><span>预估总额</span><strong>¥{{ info.totalAmount }}</strong></div>
          <div v-if="info.specialRequest" class="bc-row bc-row-full"><span>特殊要求</span><strong>{{ info.specialRequest }}</strong></div>
        </div>

        <div v-if="info.dishes && info.dishes.length" class="bc-card">
          <h3 class="bc-section-title">菜单 · Menu</h3>
          <div class="bc-dish-list">
            <div v-for="(d, i) in info.dishes" :key="i" class="bc-dish-row">
              <span>{{ d.dishName }}</span>
              <span class="bc-dish-qty">×{{ d.quantity }}</span>
            </div>
          </div>
        </div>

        <div class="bc-card bc-action-card">
          <template v-if="info.guestConfirmed">
            <div class="bc-confirmed-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                <polyline points="22 4 12 14.01 9 11.01"/>
              </svg>
            </div>
            <p class="bc-confirmed-text">您已确认此预订</p>
            <p class="bc-confirmed-time" v-if="info.guestConfirmTime">确认时间：{{ formatTime(info.guestConfirmTime) }}</p>
          </template>
          <template v-else>
            <p class="bc-action-hint">请核对以上预订信息，确认无误后点击下方按钮</p>
            <el-button type="primary" class="bc-confirm-btn" :loading="confirming" @click="handleConfirm">
              确认预订 · Confirm
            </el-button>
          </template>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const route = useRoute()
const token = route.params.token

const loading = ref(true)
const loadError = ref('')
const confirming = ref(false)
const info = ref({})

function formatTime(val) {
  if (!val) return ''
  return String(val).replace('T', ' ').slice(0, 16)
}

async function loadInfo() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await request.get(`/api/bookings/confirm/${token}`)
    info.value = res.data || {}
  } catch (e) {
    loadError.value = e.message || '确认链接无效或已过期'
  } finally {
    loading.value = false
  }
}

async function handleConfirm() {
  confirming.value = true
  try {
    const res = await request.post(`/api/bookings/confirm/${token}`)
    info.value.guestConfirmed = true
    info.value.guestConfirmTime = res.data?.guestConfirmTime
    ElMessage.success('确认成功')
  } catch (e) {
    ElMessage.error(e.message || '确认失败，请稍后重试')
  } finally {
    confirming.value = false
  }
}

onMounted(loadInfo)
</script>

<style scoped>
.bc-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f0f4f3 0%, #e8edea 50%, #dce5e1 100%);
  display: flex;
  justify-content: center;
  padding: 24px 16px 48px;
}

.bc-container {
  width: 100%;
  max-width: 480px;
}

.bc-header {
  text-align: center;
  margin-bottom: 24px;
  padding-top: 16px;
}

.bc-logo {
  width: 56px;
  height: 56px;
  margin: 0 auto 12px;
  color: #2D4A3E;
  background: #fff;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 12px rgba(45, 74, 62, 0.12);
}

.bc-logo svg { width: 30px; height: 30px; }

.bc-title {
  font-size: 19px;
  font-weight: 700;
  color: #2D4A3E;
  margin: 0 0 4px;
}

.bc-subtitle {
  font-size: 13px;
  color: #7a8c84;
  margin: 0;
}

.bc-card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.06);
  margin-bottom: 14px;
}

.bc-loading, .bc-error {
  text-align: center;
  color: #9aaba3;
  padding: 40px 20px;
}

.bc-banquet-name {
  font-size: 17px;
  font-weight: 700;
  color: #2D4A3E;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 2px solid #f0f4f3;
}

.bc-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 14px;
}

.bc-row span { color: #9aaba3; }
.bc-row strong { color: #2D4A3E; font-weight: 500; text-align: right; }
.bc-row-full { flex-direction: column; gap: 4px; }
.bc-row-full strong { text-align: left; }

.bc-section-title {
  font-size: 15px;
  font-weight: 600;
  color: #2D4A3E;
  margin: 0 0 12px;
}

.bc-dish-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  font-size: 13px;
  color: #4a5c55;
  border-bottom: 1px solid #f5f7f6;
}

.bc-dish-qty { color: #9aaba3; }

.bc-action-card {
  text-align: center;
}

.bc-action-hint {
  font-size: 13px;
  color: #7a8c84;
  margin: 0 0 16px;
}

.bc-confirm-btn {
  --el-button-bg-color: #2D4A3E;
  --el-button-border-color: #2D4A3E;
  --el-button-hover-bg-color: #3a5e4f;
  --el-button-hover-border-color: #3a5e4f;
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
}

.bc-confirmed-icon {
  width: 56px;
  height: 56px;
  margin: 0 auto 12px;
  background: linear-gradient(135deg, #2D4A3E, #4A7C59);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.bc-confirmed-icon svg { width: 30px; height: 30px; }

.bc-confirmed-text {
  font-size: 16px;
  font-weight: 600;
  color: #2D4A3E;
  margin: 0 0 4px;
}

.bc-confirmed-time {
  font-size: 12px;
  color: #9aaba3;
  margin: 0;
}
</style>
