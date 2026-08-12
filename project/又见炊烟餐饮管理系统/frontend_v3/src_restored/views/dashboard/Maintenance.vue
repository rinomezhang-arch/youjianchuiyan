<template>
  <div class="maintenance-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">设备维护 · Maintenance Management</h2>
        <p class="page-subtitle">Equipment maintenance and work order tracking</p>
      </div>
      <button class="btn-primary" @click="showAddDialog = true">+ 新建工单</button>
    </div>

    <!-- 统计 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-label">待处理</div>
        <div class="stat-value" style="color:#C0392B">{{ pendingCount }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">处理中</div>
        <div class="stat-value" style="color:#D4A853">{{ processingCount }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">已完成</div>
        <div class="stat-value" style="color:#4A7C59">{{ doneCount }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">设备总数</div>
        <div class="stat-value" style="color:#2D4A3E">{{ equipmentCount }}</div>
      </div>
    </div>

    <!-- 工单列表 -->
    <div class="order-table-wrapper">
      <table class="data-table">
        <thead>
          <tr>
            <th>工单号</th>
            <th>设备名称</th>
            <th>故障描述</th>
            <th>位置</th>
            <th>优先级</th>
            <th>报修时间</th>
            <th>处理人</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="o in orders" :key="o.id">
            <td>{{ o.orderNo }}</td>
            <td>{{ o.equipment }}</td>
            <td>{{ o.description }}</td>
            <td>{{ o.location }}</td>
            <td><span :class="['priority-badge', o.priority]">{{ priorityText(o.priority) }}</span></td>
            <td>{{ o.time }}</td>
            <td>{{ o.handler || '-' }}</td>
            <td><span :class="['status-badge', o.status]">{{ statusText(o.status) }}</span></td>
            <td>
              <button v-if="o.status !== 'done'" class="btn-sm" @click="updateStatus(o)">处理</button>
              <span v-else style="color:#4A7C59;font-size:12px">已完成</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 新建工单弹窗 -->
    <div v-if="showAddDialog" class="dialog-overlay" @click.self="showAddDialog = false">
      <div class="dialog-box">
        <h3>新建维护工单</h3>
        <div class="form-grid">
          <div class="form-item">
            <label>设备名称</label>
            <input v-model="form.equipment" placeholder="例：中央空调主机" />
          </div>
          <div class="form-item">
            <label>位置</label>
            <input v-model="form.location" placeholder="例：大厅" />
          </div>
          <div class="form-item">
            <label>优先级</label>
            <select v-model="form.priority">
              <option value="high">高</option>
              <option value="medium">中</option>
              <option value="low">低</option>
            </select>
          </div>
          <div class="form-item">
            <label>处理人</label>
            <input v-model="form.handler" placeholder="维修人员姓名" />
          </div>
          <div class="form-item full">
            <label>故障描述</label>
            <textarea v-model="form.description" rows="3" placeholder="详细描述故障情况..."></textarea>
          </div>
        </div>
        <div class="dialog-actions">
          <button class="btn-cancel" @click="showAddDialog = false">取消</button>
          <button class="btn-primary" @click="saveOrder">提交</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const showAddDialog = ref(false)
const orders = ref([
  { id: 1, orderNo: 'WO-20260709-001', equipment: '中央空调主机', description: '制冷效果差，出风温度偏高', location: '机房', priority: 'high', time: '2026-07-09 09:30', handler: '张工', status: 'processing' },
  { id: 2, orderNo: 'WO-20260709-002', equipment: '洗碗机', description: '底部漏水，密封圈老化', location: '后厨', priority: 'high', time: '2026-07-09 10:15', handler: '李工', status: 'pending' },
  { id: 3, orderNo: 'WO-20260709-003', equipment: '排烟风机', description: '运转时有异常噪音', location: '厨房', priority: 'medium', time: '2026-07-09 11:00', handler: '', status: 'pending' },
  { id: 4, orderNo: 'WO-20260708-001', equipment: '卫生间水龙头', description: '水龙头滴水，需要更换阀芯', location: '2F卫生间', priority: 'low', time: '2026-07-08 14:00', handler: '王工', status: 'done' },
  { id: 5, orderNo: 'WO-20260708-002', equipment: '冰柜', description: '温度显示异常，实际制冷正常', location: '后厨', priority: 'medium', time: '2026-07-08 16:30', handler: '张工', status: 'done' },
])

const form = ref({ equipment: '', location: '', priority: 'medium', handler: '', description: '' })

const pendingCount = computed(() => orders.value.filter(o => o.status === 'pending').length)
const processingCount = computed(() => orders.value.filter(o => o.status === 'processing').length)
const doneCount = computed(() => orders.value.filter(o => o.status === 'done').length)
const equipmentCount = computed(() => new Set(orders.value.map(o => o.equipment)).size)

const priorityText = (p) => ({ high: '高', medium: '中', low: '低' }[p] || p)
const statusText = (s) => ({ pending: '待处理', processing: '处理中', done: '已完成' }[s] || s)

const updateStatus = (o) => {
  if (o.status === 'pending') o.status = 'processing'
  else if (o.status === 'processing') o.status = 'done'
}

const saveOrder = () => {
  if (!form.value.equipment || !form.value.description) return
  const now = new Date()
  const dateStr = now.toISOString().slice(0, 10) + ' ' + now.toTimeString().slice(0, 5)
  const orderNo = 'WO-' + now.toISOString().slice(0, 10).replace(/-/g, '') + '-' + String(orders.value.length + 1).padStart(3, '0')
  orders.value.unshift({
    id: Date.now(),
    orderNo,
    ...form.value,
    time: dateStr,
    status: 'pending',
  })
  showAddDialog.value = false
  form.value = { equipment: '', location: '', priority: 'medium', handler: '', description: '' }
}
</script>

<style scoped>
.maintenance-page { padding: 24px 32px; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; flex-wrap: wrap; gap: 12px; }
.page-title { font-size: 22px; font-weight: 700; color: #1a2f23; margin: 0; }
.page-subtitle { font-size: 13px; color: #8a9a8e; margin: 4px 0 0 0; }

.btn-primary {
  background: #2D4A3E; color: #fff; border: none; padding: 8px 20px;
  border-radius: 6px; font-size: 13px; cursor: pointer; font-weight: 500;
}
.btn-primary:hover { background: #3a5f50; }

.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 20px; }
.stat-card {
  background: #fff; border-radius: 8px; padding: 16px 20px;
  border: 1px solid #e8ece9; text-align: center;
}
.stat-label { font-size: 12px; color: #8a9a8e; margin-bottom: 4px; }
.stat-value { font-size: 28px; font-weight: 700; }

.order-table-wrapper { background: #fff; border-radius: 8px; border: 1px solid #e8ece9; overflow-x: auto; }
.data-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.data-table th {
  text-align: left; padding: 12px; font-weight: 600; color: #6a7a6e;
  border-bottom: 2px solid #e8ece9; font-size: 12px; white-space: nowrap;
}
.data-table td { padding: 12px; border-bottom: 1px solid #f0f2f0; color: #3a4a3e; }

.priority-badge { padding: 2px 8px; border-radius: 4px; font-size: 11px; font-weight: 500; }
.priority-badge.high { background: rgba(192,57,43,0.1); color: #C0392B; }
.priority-badge.medium { background: rgba(212,168,83,0.12); color: #b8922e; }
.priority-badge.low { background: rgba(74,124,89,0.1); color: #4A7C59; }

.status-badge { padding: 3px 10px; border-radius: 12px; font-size: 11px; font-weight: 500; }
.status-badge.pending { background: rgba(192,57,43,0.08); color: #C0392B; }
.status-badge.processing { background: rgba(212,168,83,0.12); color: #b8922e; }
.status-badge.done { background: rgba(74,124,89,0.1); color: #4A7C59; }

.btn-sm {
  padding: 4px 12px; border-radius: 4px; font-size: 12px; cursor: pointer;
  border: 1px solid #2D4A3E; background: #fff; color: #2D4A3E;
}
.btn-sm:hover { background: #2D4A3E; color: #fff; }

.dialog-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.4);
  display: flex; align-items: center; justify-content: center; z-index: 1000;
}
.dialog-box {
  background: #fff; border-radius: 12px; padding: 28px; width: 520px; max-width: 90vw;
  box-shadow: 0 20px 60px rgba(0,0,0,0.15);
}
.dialog-box h3 { font-size: 18px; color: #1a2f23; margin: 0 0 20px 0; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.form-item { display: flex; flex-direction: column; gap: 4px; }
.form-item.full { grid-column: 1 / -1; }
.form-item label { font-size: 12px; color: #6a7a6e; font-weight: 500; }
.form-item input, .form-item select, .form-item textarea {
  padding: 8px 10px; border: 1px solid #d0d8d2; border-radius: 6px;
  font-size: 13px; color: #3a4a3e; outline: none;
}
.form-item input:focus, .form-item select:focus, .form-item textarea:focus { border-color: #2D4A3E; }
.dialog-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; }
.btn-cancel {
  padding: 8px 20px; border-radius: 6px; font-size: 13px; cursor: pointer;
  border: 1px solid #d0d8d2; background: #fff; color: #6a7a6e;
}
</style>
