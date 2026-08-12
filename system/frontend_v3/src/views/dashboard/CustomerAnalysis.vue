<template>
  <el-dialog
    v-model="visible"
    title="客户分析 · Customer Analysis"
    width="800px"
    :close-on-click-modal="false"
  >
    <div class="customer-analysis">
      <!-- 搜索区域 -->
      <div class="search-section">
        <el-input
          v-model="searchPhone"
          placeholder="输入电话号码搜索 · Enter phone number"
          clearable
          @keyup.enter="searchCustomer"
          style="width: 300px"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" @click="searchCustomer">搜索 · Search</el-button>
      </div>

      <!-- 客户信息 -->
      <div v-if="customerInfo" class="customer-info">
        <div class="info-header">
          <h3>客户信息 · Customer Info</h3>
          <div class="visit-badge">
            来访 <strong>{{ customerInfo.visitCount }}</strong> 次
          </div>
        </div>
        <div class="info-grid">
          <div class="info-item">
            <label>电话 · Phone</label>
            <span>{{ customerInfo.phone }}</span>
          </div>
          <div class="info-item">
            <label>总人数 · Total Guests</label>
            <span>{{ customerInfo.totalGuests }} 人</span>
          </div>
          <div class="info-item">
            <label>首次预订 · First Visit</label>
            <span>{{ customerInfo.firstVisit || '无记录' }}</span>
          </div>
          <div class="info-item">
            <label>最近预订 · Last Visit</label>
            <span>{{ customerInfo.lastVisit || '无记录' }}</span>
          </div>
        </div>
      </div>

      <!-- 预订历史 -->
      <div v-if="bookingHistory.length > 0" class="history-section">
        <h3>预订历史 · Booking History</h3>
        <el-table :data="bookingHistory" style="width: 100%" max-height="400">
          <el-table-column prop="booking_date" label="日期 · Date" width="120" />
          <el-table-column prop="time_type" label="时段 · Period" width="100">
            <template #default="{ row }">
              {{ row.time_type === 'lunch' ? '午餐' : '晚餐' }}
            </template>
          </el-table-column>
          <el-table-column prop="table_number" label="桌号 · Table" width="100" />
          <el-table-column prop="customer_name" label="姓名 · Name" width="120" />
          <el-table-column prop="guest_count" label="人数 · Guests" width="80" />
          <el-table-column prop="banquet_name" label="宴会类型 · Banquet" />
          <el-table-column prop="remark" label="备注 · Remark" />
        </el-table>
      </div>

      <!-- 空状态 -->
      <div v-if="searched && !customerInfo" class="empty-state">
        <el-empty description="未找到该客户的预订记录 · No bookings found" />
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { listBookings } from '@/api/booking'

const props = defineProps({
  modelValue: Boolean,
  initialPhone: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue', 'select'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const searchPhone = ref(props.initialPhone)
const customerInfo = ref(null)
const bookingHistory = ref([])
const searched = ref(false)

// 搜索客户
const searchCustomer = async () => {
  if (!searchPhone.value.trim()) {
    return
  }

  searched.value = true

  try {
    // 查询所有预订记录
    const res = await listBookings({
      customer_phone: searchPhone.value,
      limit: 1000
    })

    if (res.code === 200 && res.data && res.data.length > 0) {
      const bookings = res.data

      // 计算客户信息
      const totalGuests = bookings.reduce((sum, b) => sum + (b.guest_count || 0), 0)
      const dates = bookings.map(b => b.booking_date).sort()
      
      customerInfo.value = {
        phone: searchPhone.value,
        visitCount: bookings.length,
        totalGuests,
        firstVisit: dates[0],
        lastVisit: dates[dates.length - 1]
      }

      bookingHistory.value = bookings
    } else {
      customerInfo.value = null
      bookingHistory.value = []
    }
  } catch (error) {
    console.error('搜索客户失败:', error)
    customerInfo.value = null
    bookingHistory.value = []
  }
}

// 初始化搜索
if (props.initialPhone) {
  searchCustomer()
}
</script>

<style scoped>
.customer-analysis {
  padding: 0;
}

.search-section {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.customer-info {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 20px;
}

.info-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.info-header h3 {
  margin: 0;
  font-size: 16px;
}

.visit-badge {
  background: #409eff;
  color: white;
  padding: 6px 12px;
  border-radius: 16px;
  font-size: 14px;
}

.visit-badge strong {
  font-size: 18px;
  margin: 0 4px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-item label {
  font-size: 12px;
  color: #909399;
}

.info-item span {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.history-section {
  margin-top: 20px;
}

.history-section h3 {
  margin: 0 0 12px 0;
  font-size: 16px;
}

.empty-state {
  margin-top: 40px;
}
</style>
