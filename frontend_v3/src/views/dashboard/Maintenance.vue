<template>
  <div class="maintenance-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">设备维护 · Maintenance</h2>
        <p class="page-desc">设备维修工单管理 · 报修跟踪 · 优先级与处理时效</p>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="openAddDialog">新建工单</el-button>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <span class="stat-label">待处理 · Pending</span>
        <span class="stat-value" style="color:#C0392B">{{ stats.pending }}</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">处理中 · Processing</span>
        <span class="stat-value" style="color:#C4A35A">{{ stats.processing }}</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">已完成 · Done</span>
        <span class="stat-value" style="color:#4A7C59">{{ stats.done }}</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">本月完成 · This Month</span>
        <span class="stat-value">{{ stats.monthDone }}</span>
      </div>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-select v-model="filterStatus" placeholder="全部状态" clearable style="width: 140px">
            <el-option label="待处理" value="pending" />
            <el-option label="处理中" value="processing" />
            <el-option label="已完成" value="done" />
          </el-select>
          <el-select v-model="filterPriority" placeholder="全部优先级" clearable style="width: 140px">
            <el-option label="高" value="high" />
            <el-option label="中" value="medium" />
            <el-option label="低" value="low" />
          </el-select>
          <el-input v-model="searchKey" placeholder="搜索工单号/设备名称" clearable style="width: 240px" />
        </div>
      </div>

      <el-table :data="filteredOrders" stripe style="width: 100%">
        <el-table-column prop="orderNo" label="工单号" width="160" />
        <el-table-column prop="equipment" label="设备名称" min-width="140" />
        <el-table-column prop="location" label="位置" width="110" />
        <el-table-column prop="time" label="报修时间" width="160" />
        <el-table-column label="优先级" width="90">
          <template #default="{ row }">
            <el-tag :type="priorityType(row.priority)" effect="plain">{{ priorityText(row.priority) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="plain">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handler" label="负责人" width="100">
          <template #default="{ row }">{{ row.handler || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 'done'" size="small" type="primary" link @click="advanceStatus(row)">推进</el-button>
            <el-button v-if="row.status === 'done'" size="small" type="info" link disabled>已闭环</el-button>
            <el-button size="small" type="danger" link @click="deleteOrder(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" title="新建维护工单" width="560px">
      <el-form :model="form" label-width="100px" label-position="right">
        <el-form-item label="设备名称">
          <el-input v-model="form.equipment" placeholder="例：中央空调主机" />
        </el-form-item>
        <el-form-item label="位置">
          <el-input v-model="form.location" placeholder="例：机房" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="form.priority" placeholder="请选择优先级" style="width: 100%">
            <el-option label="高" value="high" />
            <el-option label="中" value="medium" />
            <el-option label="低" value="low" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理人">
          <el-input v-model="form.handler" placeholder="维修人员姓名" />
        </el-form-item>
        <el-form-item label="故障描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="详细描述故障情况..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveOrder">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const filterStatus = ref('')
const filterPriority = ref('')
const searchKey = ref('')
const dialogVisible = ref(false)

const orders = ref([])

const form = ref({ equipment: '', location: '', priority: 'medium', handler: '', description: '' })

const stats = computed(() => {
  const now = new Date()
  const ym = now.toISOString().slice(0, 7)
  return {
    pending: orders.value.filter(o => o.status === 'pending').length,
    processing: orders.value.filter(o => o.status === 'processing').length,
    done: orders.value.filter(o => o.status === 'done').length,
    monthDone: orders.value.filter(o => o.status === 'done' && (o.time || '').startsWith(ym)).length,
  }
})

const filteredOrders = computed(() => {
  return orders.value.filter(o => {
    if (filterStatus.value && o.status !== filterStatus.value) return false
    if (filterPriority.value && o.priority !== filterPriority.value) return false
    if (searchKey.value) {
      const k = searchKey.value.toLowerCase()
      if (!o.orderNo?.toLowerCase().includes(k) && !o.equipment?.toLowerCase().includes(k)) return false
    }
    return true
  })
})

const priorityType = (p) => ({ high: 'danger', medium: 'warning', low: 'success' }[p] || 'info')
const priorityText = (p) => ({ high: '高', medium: '中', low: '低' }[p] || p)
const statusType = (s) => ({ pending: 'danger', processing: 'warning', done: 'success' }[s] || 'info')
const statusText = (s) => ({ pending: '待处理', processing: '处理中', done: '已完成' }[s] || s)

const genOrderNo = () => {
  const now = new Date()
  const dateStr = now.toISOString().slice(0, 10).replace(/-/g, '')
  const seq = String(orders.value.length + 1).padStart(3, '0')
  return `WO-${dateStr}-${seq}`
}

const nowStr = () => {
  const now = new Date()
  return now.toISOString().slice(0, 10) + ' ' + now.toTimeString().slice(0, 5)
}

const openAddDialog = () => {
  form.value = { equipment: '', location: '', priority: 'medium', handler: '', description: '' }
  dialogVisible.value = true
}

const saveOrder = () => {
  if (!form.value.equipment || !form.value.description) {
    ElMessage.warning('请填写设备名称和故障描述')
    return
  }
  orders.value.unshift({
    id: Date.now(),
    orderNo: genOrderNo(),
    ...form.value,
    time: nowStr(),
    status: 'pending',
  })
  ElMessage.success('工单已提交')
  dialogVisible.value = false
  form.value = { equipment: '', location: '', priority: 'medium', handler: '', description: '' }
}

const advanceStatus = (row) => {
  if (row.status === 'pending') {
    row.status = 'processing'
    ElMessage.success('工单已开始处理')
  } else if (row.status === 'processing') {
    row.status = 'done'
    ElMessage.success('工单已完成')
  }
}

const deleteOrder = (row) => {
  ElMessageBox.confirm(`确定删除工单「${row.orderNo}」吗？`, '提示', { type: 'warning' })
    .then(() => {
      orders.value = orders.value.filter(o => o.id !== row.id)
      ElMessage.success('工单已删除')
    })
    .catch(() => {})
}
</script>

<style scoped>
.maintenance-page { padding: 24px 32px; }

.page-header {
  display: flex; align-items: flex-start; justify-content: space-between;
  margin-bottom: 24px; gap: 16px; flex-wrap: wrap;
}
.header-left { display: flex; flex-direction: column; gap: 4px; }
.page-title { font-size: 22px; font-weight: 700; color: #1a2f23; margin: 0; }
.page-desc { font-size: 13px; color: #8a9a8e; margin: 0; }

.stats-row {
  display: grid; grid-template-columns: repeat(4, 1fr);
  gap: 16px; margin-bottom: 20px;
}
.stat-card {
  background: #fff; border: 1px solid #e8ece9; border-radius: 10px;
  padding: 18px 20px; display: flex; flex-direction: column; gap: 6px;
}
.stat-label { font-size: 12px; color: #8a9a8e; }
.stat-value { font-size: 28px; font-weight: 700; color: #2D4A3E; }

.content-card {
  background: #fff; border: 1px solid #e8ece9; border-radius: 10px; padding: 20px;
}

.toolbar {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 16px; flex-wrap: wrap; gap: 12px;
}
.toolbar-left { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }

.text-muted { font-size: 12px; color: #a0b0a5; }
</style>
