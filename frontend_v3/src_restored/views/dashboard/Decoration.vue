<template>
  <div class="decoration-page">
    <div class="page-header">
      <h2 class="page-title">装修管理 · Decoration Management</h2>
      <p class="page-subtitle">Manage renovation and decoration projects</p>
      <div class="header-actions">
        <button class="btn-primary" @click="showAddDialog = true">+ 新建项目</button>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-group">
        <label>状态</label>
        <select v-model="filterStatus">
          <option value="">全部</option>
          <option value="pending">待审批</option>
          <option value="active">进行中</option>
          <option value="done">已完成</option>
        </select>
      </div>
      <div class="filter-group">
        <label>类型</label>
        <select v-model="filterType">
          <option value="">全部</option>
          <option value="装修">装修</option>
          <option value="改造">改造</option>
          <option value="维修">维修</option>
        </select>
      </div>
    </div>

    <!-- 项目列表 -->
    <div class="project-list">
      <div v-for="p in filteredProjects" :key="p.id" class="project-card">
        <div class="project-header">
          <div>
            <h3 class="project-name">{{ p.name }}</h3>
            <span class="project-type">{{ p.type }}</span>
          </div>
          <span :class="['status-badge', p.status]">{{ statusText(p.status) }}</span>
        </div>
        <div class="project-meta">
          <span>负责人：{{ p.manager }}</span>
          <span>预算：¥{{ p.budget.toLocaleString() }}</span>
          <span>开工：{{ p.startDate }}</span>
          <span>预计完工：{{ p.endDate }}</span>
        </div>
        <div class="project-progress">
          <div class="progress-track">
            <div class="progress-fill" :style="{ width: p.progress + '%' }"></div>
          </div>
          <span class="progress-pct">{{ p.progress }}%</span>
        </div>
        <div class="project-desc">{{ p.description }}</div>
        <div class="project-actions">
          <button class="btn-sm" @click="editProject(p)">编辑</button>
          <button class="btn-sm danger" @click="deleteProject(p.id)">删除</button>
        </div>
      </div>
    </div>

    <!-- 新建/编辑弹窗 -->
    <div v-if="showAddDialog" class="dialog-overlay" @click.self="showAddDialog = false">
      <div class="dialog-box">
        <h3>{{ editingId ? '编辑项目' : '新建装修项目' }}</h3>
        <div class="form-grid">
          <div class="form-item">
            <label>项目名称</label>
            <input v-model="form.name" placeholder="例：大厅吊顶翻新" />
          </div>
          <div class="form-item">
            <label>类型</label>
            <select v-model="form.type">
              <option>装修</option>
              <option>改造</option>
              <option>维修</option>
            </select>
          </div>
          <div class="form-item">
            <label>负责人</label>
            <input v-model="form.manager" placeholder="负责人姓名" />
          </div>
          <div class="form-item">
            <label>预算（元）</label>
            <input v-model.number="form.budget" type="number" />
          </div>
          <div class="form-item">
            <label>开工日期</label>
            <input v-model="form.startDate" type="date" />
          </div>
          <div class="form-item">
            <label>预计完工</label>
            <input v-model="form.endDate" type="date" />
          </div>
          <div class="form-item full">
            <label>项目描述</label>
            <textarea v-model="form.description" rows="3" placeholder="项目详细说明..."></textarea>
          </div>
        </div>
        <div class="dialog-actions">
          <button class="btn-cancel" @click="showAddDialog = false">取消</button>
          <button class="btn-primary" @click="saveProject">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const filterStatus = ref('')
const filterType = ref('')
const showAddDialog = ref(false)
const editingId = ref(null)

const projects = ref([
  { id: 1, name: '大厅吊顶翻新', type: '装修', manager: '张工', budget: 85000, progress: 75, status: 'active', startDate: '2026-06-01', endDate: '2026-07-15', description: '大厅区域吊顶整体翻新，包含灯具更换和空调出风口调整' },
  { id: 2, name: '厨房排烟改造', type: '改造', manager: '李工', budget: 45000, progress: 30, status: 'active', startDate: '2026-06-20', endDate: '2026-08-01', description: '厨房排烟系统升级，增加油烟净化设备' },
  { id: 3, name: '包厢墙面翻新', type: '装修', manager: '王工', budget: 32000, progress: 0, status: 'pending', startDate: '2026-07-10', endDate: '2026-08-10', description: '牡丹厅、荷花厅、菊花厅墙面重新粉刷' },
])

const form = ref({ name: '', type: '装修', manager: '', budget: 0, startDate: '', endDate: '', description: '' })

const filteredProjects = computed(() => {
  return projects.value.filter(p => {
    if (filterStatus.value && p.status !== filterStatus.value) return false
    if (filterType.value && p.type !== filterType.value) return false
    return true
  })
})

const statusText = (s) => ({ active: '进行中', pending: '待审批', done: '已完成' }[s] || s)

const editProject = (p) => {
  editingId.value = p.id
  form.value = { ...p }
  showAddDialog.value = true
}

const saveProject = () => {
  if (!form.value.name) return
  if (editingId.value) {
    const idx = projects.value.findIndex(p => p.id === editingId.value)
    if (idx >= 0) projects.value[idx] = { ...form.value, id: editingId.value }
  } else {
    projects.value.push({ ...form.value, id: Date.now(), progress: 0, status: 'pending' })
  }
  showAddDialog.value = false
  editingId.value = null
  form.value = { name: '', type: '装修', manager: '', budget: 0, startDate: '', endDate: '', description: '' }
}

const deleteProject = (id) => {
  projects.value = projects.value.filter(p => p.id !== id)
}
</script>

<style scoped>
.decoration-page { padding: 24px 32px; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; flex-wrap: wrap; gap: 12px; }
.page-title { font-size: 22px; font-weight: 700; color: #1a2f23; margin: 0; }
.page-subtitle { font-size: 13px; color: #8a9a8e; margin: 0; }
.header-actions { display: flex; gap: 8px; }

.btn-primary {
  background: #2D4A3E; color: #fff; border: none; padding: 8px 20px;
  border-radius: 6px; font-size: 13px; cursor: pointer; font-weight: 500;
}
.btn-primary:hover { background: #3a5f50; }

.filter-bar {
  display: flex; gap: 16px; margin-bottom: 20px; padding: 12px 16px;
  background: #fff; border-radius: 8px; border: 1px solid #e8ece9;
}
.filter-group { display: flex; align-items: center; gap: 8px; }
.filter-group label { font-size: 12px; color: #6a7a6e; font-weight: 500; }
.filter-group select {
  padding: 5px 10px; border: 1px solid #d0d8d2; border-radius: 4px;
  font-size: 13px; color: #3a4a3e; background: #fff;
}

.project-list { display: flex; flex-direction: column; gap: 12px; }
.project-card {
  background: #fff; border-radius: 8px; padding: 20px;
  border: 1px solid #e8ece9;
}
.project-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 10px; }
.project-name { font-size: 16px; font-weight: 600; color: #1a2f23; margin: 0; }
.project-type {
  display: inline-block; font-size: 11px; padding: 2px 8px;
  background: rgba(45,74,62,0.08); color: #2D4A3E; border-radius: 4px; margin-top: 4px;
}
.project-meta {
  display: flex; gap: 20px; font-size: 12px; color: #6a7a6e; margin-bottom: 12px; flex-wrap: wrap;
}
.project-progress { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.progress-track { flex: 1; height: 8px; background: #e8ece9; border-radius: 4px; overflow: hidden; }
.progress-fill { height: 100%; background: linear-gradient(90deg, #2D4A3E, #4A7C59); border-radius: 4px; }
.progress-pct { font-size: 13px; font-weight: 600; color: #2D4A3E; min-width: 40px; }
.project-desc { font-size: 13px; color: #6a7a6e; margin-bottom: 12px; }
.project-actions { display: flex; gap: 8px; }

.btn-sm {
  padding: 5px 14px; border-radius: 4px; font-size: 12px; cursor: pointer;
  border: 1px solid #d0d8d2; background: #fff; color: #3a4a3e;
}
.btn-sm:hover { background: #f0f4f1; }
.btn-sm.danger { color: #C0392B; border-color: #e8c4c0; }
.btn-sm.danger:hover { background: #fdf0ee; }

.status-badge { padding: 3px 10px; border-radius: 12px; font-size: 11px; font-weight: 500; }
.status-badge.active { background: rgba(45,74,62,0.1); color: #2D4A3E; }
.status-badge.pending { background: rgba(212,168,83,0.12); color: #b8922e; }
.status-badge.done { background: rgba(74,124,89,0.1); color: #4A7C59; }

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
