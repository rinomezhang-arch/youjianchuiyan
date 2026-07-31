<template>
  <div class="safety-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">安全管理 · Safety Management</h2>
        <p class="page-subtitle">Safety inspection, hazard tracking and compliance</p>
      </div>
      <button class="btn-primary" @click="showAddDialog = true">+ 上报隐患</button>
    </div>

    <!-- 统计 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon" style="background:rgba(192,57,43,0.08)">
          <svg viewBox="0 0 24 24" fill="none" stroke="#C0392B" stroke-width="2">
            <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
            <line x1="12" y1="9" x2="12" y2="13"/>
            <line x1="12" y1="17" x2="12.01" y2="17"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">待整改 · Pending</div>
          <div class="stat-value" style="color:#C0392B">{{ pendingCount }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:rgba(212,168,83,0.08)">
          <svg viewBox="0 0 24 24" fill="none" stroke="#D4A853" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <polyline points="12 6 12 12 16 14"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">整改中 · In Progress</div>
          <div class="stat-value" style="color:#D4A853">{{ processingCount }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:rgba(74,124,89,0.08)">
          <svg viewBox="0 0 24 24" fill="none" stroke="#4A7C59" stroke-width="2">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
            <polyline points="22 4 12 14.01 9 11.01"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">已整改 · Resolved</div>
          <div class="stat-value" style="color:#4A7C59">{{ resolvedCount }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:rgba(45,74,62,0.08)">
          <svg viewBox="0 0 24 24" fill="none" stroke="#2D4A3E" stroke-width="2">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">本月巡检 · Inspections</div>
          <div class="stat-value" style="color:#2D4A3E">{{ inspectionCount }}</div>
        </div>
      </div>
    </div>

    <!-- 隐患列表 -->
    <div class="content-grid">
      <div class="content-card wide">
        <h3 class="section-title">安全隐患台账 · Hazard Register</h3>
        <div class="filter-bar">
          <select v-model="filterSeverity">
            <option value="">全部级别</option>
            <option value="high">高</option>
            <option value="medium">中</option>
            <option value="low">低</option>
          </select>
          <select v-model="filterStatus">
            <option value="">全部状态</option>
            <option value="pending">待整改</option>
            <option value="processing">整改中</option>
            <option value="resolved">已整改</option>
          </select>
        </div>
        <table class="data-table">
          <thead>
            <tr>
              <th>编号</th>
              <th>隐患描述</th>
              <th>位置</th>
              <th>级别</th>
              <th>发现时间</th>
              <th>责任人</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="s in filteredIssues" :key="s.id">
              <td>{{ s.no }}</td>
              <td>{{ s.title }}</td>
              <td>{{ s.location }}</td>
              <td><span :class="['severity-badge', s.severity]">{{ severityText(s.severity) }}</span></td>
              <td>{{ s.time }}</td>
              <td>{{ s.responsible || '-' }}</td>
              <td><span :class="['status-badge', s.status]">{{ statusText(s.status) }}</span></td>
              <td>
                <button v-if="s.status !== 'resolved'" class="btn-sm" @click="advanceStatus(s)">推进</button>
                <span v-else style="color:#4A7C59;font-size:12px">已闭环</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 安全巡检记录 -->
      <div class="content-card">
        <h3 class="section-title">巡检记录 · Inspection Log</h3>
        <div class="inspection-list">
          <div v-for="insp in inspections" :key="insp.id" class="inspection-item">
            <div class="inspection-date">
              <div class="date-day">{{ insp.day }}</div>
              <div class="date-month">{{ insp.month }}</div>
            </div>
            <div class="inspection-info">
              <div class="inspection-title">{{ insp.title }}</div>
              <div class="inspection-meta">{{ insp.inspector }} · {{ insp.findings }} 项发现</div>
            </div>
            <span :class="['inspection-result', insp.result]">{{ insp.result === 'pass' ? '合格' : '需整改' }}</span>
          </div>
        </div>
      </div>

      <!-- 消防设施 -->
      <div class="content-card">
        <h3 class="section-title">消防设施 · Fire Equipment</h3>
        <div class="equipment-list">
          <div v-for="eq in fireEquipment" :key="eq.id" class="equipment-item">
            <div class="eq-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="#C0392B" stroke-width="2">
                <path d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z"/>
              </svg>
            </div>
            <div class="eq-info">
              <div class="eq-name">{{ eq.name }}</div>
              <div class="eq-meta">{{ eq.location }} · 有效期至 {{ eq.expiry }}</div>
            </div>
            <span :class="['eq-status', eq.status]">{{ eq.status === 'valid' ? '有效' : '待更换' }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 上报弹窗 -->
    <div v-if="showAddDialog" class="dialog-overlay" @click.self="showAddDialog = false">
      <div class="dialog-box">
        <h3>上报安全隐患</h3>
        <div class="form-grid">
          <div class="form-item full">
            <label>隐患描述</label>
            <input v-model="form.title" placeholder="详细描述安全隐患" />
          </div>
          <div class="form-item">
            <label>位置</label>
            <input v-model="form.location" placeholder="例：大厅入口" />
          </div>
          <div class="form-item">
            <label>级别</label>
            <select v-model="form.severity">
              <option value="high">高</option>
              <option value="medium">中</option>
              <option value="low">低</option>
            </select>
          </div>
          <div class="form-item">
            <label>责任人</label>
            <input v-model="form.responsible" placeholder="整改责任人" />
          </div>
        </div>
        <div class="dialog-actions">
          <button class="btn-cancel" @click="showAddDialog = false">取消</button>
          <button class="btn-primary" @click="saveIssue">上报</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const showAddDialog = ref(false)
const filterSeverity = ref('')
const filterStatus = ref('')
const form = ref({ title: '', location: '', severity: 'medium', responsible: '' })

const issues = ref([
  { id: 1, no: 'SF-20260709-001', title: '消防通道堆放杂物', location: '后门通道', severity: 'high', time: '2026-07-09 08:30', responsible: '张主管', status: 'pending' },
  { id: 2, no: 'SF-20260709-002', title: '地面湿滑未设警示牌', location: '大厅入口', severity: 'medium', time: '2026-07-09 09:00', responsible: '李领班', status: 'processing' },
  { id: 3, no: 'SF-20260708-001', title: '灭火器过期未更换', location: '厨房', severity: 'high', time: '2026-07-08 10:00', responsible: '王工', status: 'resolved' },
  { id: 4, no: 'SF-20260708-002', title: '应急灯不亮', location: '2F走廊', severity: 'medium', time: '2026-07-08 14:30', responsible: '张工', status: 'resolved' },
  { id: 5, no: 'SF-20260707-001', title: '燃气管道接口松动', location: '厨房', severity: 'high', time: '2026-07-07 11:00', responsible: '李工', status: 'resolved' },
])

const inspections = ref([
  { id: 1, day: '09', month: '7月', title: '日常安全巡检', inspector: '张主管', findings: 2, result: 'fail' },
  { id: 2, day: '08', month: '7月', title: '消防设施检查', inspector: '王工', findings: 1, result: 'fail' },
  { id: 3, day: '07', month: '7月', title: '日常安全巡检', inspector: '张主管', findings: 0, result: 'pass' },
  { id: 4, day: '06', month: '7月', title: '厨房专项检查', inspector: '李领班', findings: 0, result: 'pass' },
  { id: 5, day: '05', month: '7月', title: '日常安全巡检', inspector: '张主管', findings: 1, result: 'fail' },
])

const fireEquipment = ref([
  { id: 1, name: '干粉灭火器 4kg', location: '大厅', expiry: '2027-03', status: 'valid' },
  { id: 2, name: '干粉灭火器 4kg', location: '厨房', expiry: '2026-08', status: 'valid' },
  { id: 3, name: '二氧化碳灭火器', location: '机房', expiry: '2026-07', status: 'expired' },
  { id: 4, name: '消防水带', location: '2F走廊', expiry: '2027-01', status: 'valid' },
  { id: 5, name: '烟感报警器', location: '包厢区', expiry: '2027-06', status: 'valid' },
])

const pendingCount = computed(() => issues.value.filter(s => s.status === 'pending').length)
const processingCount = computed(() => issues.value.filter(s => s.status === 'processing').length)
const resolvedCount = computed(() => issues.value.filter(s => s.status === 'resolved').length)
const inspectionCount = computed(() => inspections.value.length)

const filteredIssues = computed(() => {
  return issues.value.filter(s => {
    if (filterSeverity.value && s.severity !== filterSeverity.value) return false
    if (filterStatus.value && s.status !== filterStatus.value) return false
    return true
  })
})

const severityText = (s) => ({ high: '高', medium: '中', low: '低' }[s] || s)
const statusText = (s) => ({ pending: '待整改', processing: '整改中', resolved: '已整改' }[s] || s)

const advanceStatus = (s) => {
  if (s.status === 'pending') s.status = 'processing'
  else if (s.status === 'processing') s.status = 'resolved'
}

const saveIssue = () => {
  if (!form.value.title) return
  const now = new Date()
  const dateStr = now.toISOString().slice(0, 10) + ' ' + now.toTimeString().slice(0, 5)
  const no = 'SF-' + now.toISOString().slice(0, 10).replace(/-/g, '') + '-' + String(issues.value.length + 1).padStart(3, '0')
  issues.value.unshift({
    id: Date.now(),
    no,
    ...form.value,
    time: dateStr,
    status: 'pending',
  })
  showAddDialog.value = false
  form.value = { title: '', location: '', severity: 'medium', responsible: '' }
}
</script>

<style scoped>
.safety-page { padding: 24px 32px; }
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
  background: #fff; border-radius: 8px; padding: 18px 20px;
  border: 1px solid #e8ece9; display: flex; align-items: flex-start; gap: 14px;
}
.stat-icon {
  width: 44px; height: 44px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.stat-icon svg { width: 22px; height: 22px; }
.stat-content { flex: 1; }
.stat-label { font-size: 12px; color: #8a9a8e; margin-bottom: 4px; }
.stat-value { font-size: 28px; font-weight: 700; }

.content-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.content-card { background: #fff; border-radius: 8px; padding: 20px; border: 1px solid #e8ece9; }
.content-card.wide { grid-column: 1 / -1; }
.section-title { font-size: 15px; font-weight: 600; color: #1a2f23; margin: 0 0 16px 0; }

.filter-bar { display: flex; gap: 10px; margin-bottom: 14px; }
.filter-bar select {
  padding: 5px 10px; border: 1px solid #d0d8d2; border-radius: 4px;
  font-size: 12px; color: #3a4a3e; background: #fff;
}

.data-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.data-table th {
  text-align: left; padding: 10px 12px; font-weight: 600; color: #6a7a6e;
  border-bottom: 2px solid #e8ece9; font-size: 12px; white-space: nowrap;
}
.data-table td { padding: 10px 12px; border-bottom: 1px solid #f0f2f0; color: #3a4a3e; }

.severity-badge { padding: 2px 8px; border-radius: 4px; font-size: 11px; font-weight: 500; }
.severity-badge.high { background: rgba(192,57,43,0.1); color: #C0392B; }
.severity-badge.medium { background: rgba(212,168,83,0.12); color: #b8922e; }
.severity-badge.low { background: rgba(74,124,89,0.1); color: #4A7C59; }

.status-badge { padding: 3px 10px; border-radius: 12px; font-size: 11px; font-weight: 500; }
.status-badge.pending { background: rgba(192,57,43,0.08); color: #C0392B; }
.status-badge.processing { background: rgba(212,168,83,0.12); color: #b8922e; }
.status-badge.resolved { background: rgba(74,124,89,0.1); color: #4A7C59; }

.btn-sm {
  padding: 4px 12px; border-radius: 4px; font-size: 12px; cursor: pointer;
  border: 1px solid #2D4A3E; background: #fff; color: #2D4A3E;
}
.btn-sm:hover { background: #2D4A3E; color: #fff; }

.inspection-list { display: flex; flex-direction: column; gap: 10px; }
.inspection-item {
  display: flex; align-items: center; gap: 12px;
  padding: 12px; background: #f8f9f8; border-radius: 6px;
}
.inspection-date {
  text-align: center; min-width: 40px;
}
.date-day { font-size: 20px; font-weight: 700; color: #1a2f23; line-height: 1; }
.date-month { font-size: 10px; color: #8a9a8e; }
.inspection-info { flex: 1; }
.inspection-title { font-size: 13px; font-weight: 500; color: #1a2f23; }
.inspection-meta { font-size: 11px; color: #8a9a8e; margin-top: 2px; }
.inspection-result { font-size: 11px; padding: 3px 10px; border-radius: 12px; font-weight: 500; }
.inspection-result.pass { background: rgba(74,124,89,0.1); color: #4A7C59; }
.inspection-result.fail { background: rgba(192,57,43,0.08); color: #C0392B; }

.equipment-list { display: flex; flex-direction: column; gap: 8px; }
.equipment-item {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 12px; background: #f8f9f8; border-radius: 6px;
}
.eq-icon {
  width: 36px; height: 36px; border-radius: 8px;
  background: rgba(192,57,43,0.06); display: flex;
  align-items: center; justify-content: center; flex-shrink: 0;
}
.eq-icon svg { width: 18px; height: 18px; }
.eq-info { flex: 1; }
.eq-name { font-size: 13px; font-weight: 500; color: #1a2f23; }
.eq-meta { font-size: 11px; color: #8a9a8e; margin-top: 2px; }
.eq-status { font-size: 11px; padding: 3px 10px; border-radius: 12px; font-weight: 500; }
.eq-status.valid { background: rgba(74,124,89,0.1); color: #4A7C59; }
.eq-status.expired { background: rgba(192,57,43,0.08); color: #C0392B; }

.dialog-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.4);
  display: flex; align-items: center; justify-content: center; z-index: 1000;
}
.dialog-box {
  background: #fff; border-radius: 12px; padding: 28px; width: 480px; max-width: 90vw;
  box-shadow: 0 20px 60px rgba(0,0,0,0.15);
}
.dialog-box h3 { font-size: 18px; color: #1a2f23; margin: 0 0 20px 0; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.form-item { display: flex; flex-direction: column; gap: 4px; }
.form-item.full { grid-column: 1 / -1; }
.form-item label { font-size: 12px; color: #6a7a6e; font-weight: 500; }
.form-item input, .form-item select {
  padding: 8px 10px; border: 1px solid #d0d8d2; border-radius: 6px;
  font-size: 13px; color: #3a4a3e; outline: none;
}
.form-item input:focus, .form-item select:focus { border-color: #2D4A3E; }
.dialog-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; }
.btn-cancel {
  padding: 8px 20px; border-radius: 6px; font-size: 13px; cursor: pointer;
  border: 1px solid #d0d8d2; background: #fff; color: #6a7a6e;
}
</style>
