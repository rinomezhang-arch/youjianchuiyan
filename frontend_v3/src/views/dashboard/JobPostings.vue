<template>
  <div class="job-postings-page">
    <div class="page-topbar">
      <div class="topbar-left">
        <h1 class="page-title">招聘岗位 · Job Postings</h1>
        <span class="page-desc">员工自助登记页浏览的在招岗位 · 只有"招聘中"状态对外可见</span>
      </div>
      <div class="topbar-actions">
        <el-button type="primary" @click="openCreate">
          <el-icon style="margin-right:4px"><Plus /></el-icon>
          新增岗位 · New
        </el-button>
        <el-button @click="fetchList" :loading="loading">
          <el-icon style="margin-right:4px"><Refresh /></el-icon>
          刷新 · Refresh
        </el-button>
      </div>
    </div>

    <el-table :data="list" v-loading="loading" class="jp-table">
      <el-table-column prop="department" label="部门" width="100" />
      <el-table-column prop="position" label="职位" width="140" />
      <el-table-column prop="headcount" label="招聘人数" width="90" align="center" />
      <el-table-column prop="salaryRange" label="薪资范围" width="140" />
      <el-table-column prop="workTime" label="上班时间" width="180" />
      <el-table-column prop="requirements" label="任职要求" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'open' ? 'success' : 'info'">
            {{ row.status === 'open' ? '招聘中' : '已关闭' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.status === 'open'" text type="warning" @click="handleClose(row)">关闭</el-button>
          <el-button text type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑岗位' : '新增岗位'" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="部门" required>
          <el-select v-model="form.department" placeholder="请选择部门" style="width:100%">
            <el-option label="前厅" value="前厅" />
            <el-option label="厨房" value="厨房" />
            <el-option label="财务" value="财务" />
            <el-option label="人事" value="人事" />
            <el-option label="管理" value="管理" />
          </el-select>
        </el-form-item>
        <el-form-item label="职位" required>
          <el-input v-model="form.position" placeholder="如：服务员" />
        </el-form-item>
        <el-form-item label="招聘人数">
          <el-input-number v-model="form.headcount" :min="1" :max="99" />
        </el-form-item>
        <el-form-item label="薪资范围">
          <el-input v-model="form.salaryRange" placeholder="如：4000-5500元/月" />
        </el-form-item>
        <el-form-item label="上班时间">
          <el-input v-model="form.workTime" placeholder="如：早班8:00-17:00" />
        </el-form-item>
        <el-form-item label="任职要求">
          <el-input v-model="form.requirements" type="textarea" :rows="2" placeholder="如：形象好，能吃苦" />
        </el-form-item>
        <el-form-item label="岗位描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="工作内容简述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import request from '@/utils/request'

const list = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)

const form = reactive({
  department: '',
  position: '',
  headcount: 1,
  salaryRange: '',
  workTime: '',
  requirements: '',
  description: ''
})

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/hr/job-postings')
    list.value = res.data || []
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.department = ''
  form.position = ''
  form.headcount = 1
  form.salaryRange = ''
  form.workTime = ''
  form.requirements = ''
  form.description = ''
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  form.department = row.department
  form.position = row.position
  form.headcount = row.headcount
  form.salaryRange = row.salaryRange
  form.workTime = row.workTime
  form.requirements = row.requirements
  form.description = row.description
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.department || !form.position) {
    ElMessage.warning('部门和职位不能为空')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await request.put(`/api/hr/job-postings/${editingId.value}`, { ...form })
      ElMessage.success('已保存')
    } else {
      await request.post('/api/hr/job-postings', { ...form })
      ElMessage.success('已新增')
    }
    dialogVisible.value = false
    fetchList()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleClose(row) {
  try {
    await ElMessageBox.confirm(`确认关闭「${row.position}」这个岗位吗？关闭后应聘页不再显示。`, '确认关闭', { type: 'warning' })
  } catch { return }
  try {
    await request.post(`/api/hr/job-postings/${row.id}/close`)
    ElMessage.success('已关闭')
    fetchList()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除「${row.position}」这个岗位吗？此操作不可撤销。`, '确认删除', { type: 'warning' })
  } catch { return }
  try {
    await request.delete(`/api/hr/job-postings/${row.id}`)
    ElMessage.success('已删除')
    fetchList()
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  }
}

onMounted(fetchList)
</script>

<style scoped>
.job-postings-page {
  padding: 20px;
}

.page-topbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #2D4A3E;
  margin: 0 0 4px;
}

.page-desc {
  font-size: 13px;
  color: #9aaba3;
}

.topbar-actions {
  display: flex;
  gap: 8px;
}

.jp-table {
  width: 100%;
}
</style>
