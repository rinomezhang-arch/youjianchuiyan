<template>
  <main class="organization-page" v-loading="loading">
    <header class="page-header">
      <div>
        <h2>门店与组织</h2>
        <p>所有门店、部门、岗位与人员数量均来自业务数据库</p>
      </div>
      <el-button @click="loadAll">刷新数据</el-button>
    </header>

    <section class="section-block">
      <div class="section-heading"><h3>门店实体</h3><span>{{ stores.length }} 家营业门店</span></div>
      <div class="store-cards">
        <article v-for="store in stores" :key="store.store_id" class="store-card">
          <div><strong>{{ store.store_name }}</strong><small>{{ store.store_code }}</small></div>
          <el-tag type="success" effect="plain">{{ store.status === 'open' ? '营业中' : store.status }}</el-tag>
          <p>{{ store.address || '未维护地址' }}</p>
          <dl>
            <div><dt>员工</dt><dd>{{ countStaff(store.store_id) }}</dd></div>
            <div><dt>部门</dt><dd>{{ countDepartments(store.store_id) }}</dd></div>
            <div><dt>电话</dt><dd>{{ store.phone || '未维护' }}</dd></div>
          </dl>
        </article>
      </div>
    </section>

    <section class="section-block">
      <div class="section-heading">
        <div><h3>部门与岗位</h3><span>选择部门可查看并维护其岗位</span></div>
        <el-button type="primary" @click="openDepartment()">新增部门</el-button>
      </div>
      <el-table :data="departments" border stripe highlight-current-row @current-change="selectDepartment">
        <el-table-column prop="deptCode" label="编码" width="120" />
        <el-table-column prop="deptName" label="部门" min-width="150" />
        <el-table-column label="所属门店" min-width="140"><template #default="{ row }">{{ storeName(row.storeId) }}</template></el-table-column>
        <el-table-column label="在职人数" width="100"><template #default="{ row }">{{ staffByDepartment(row).length }}</template></el-table-column>
        <el-table-column prop="description" label="职能说明" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="170"><template #default="{ row }">
          <el-button size="small" @click.stop="openDepartment(row)">编辑</el-button>
          <el-button size="small" type="danger" plain @click.stop="removeDepartment(row)">删除</el-button>
        </template></el-table-column>
      </el-table>
    </section>

    <section class="section-block">
      <div class="section-heading">
        <div><h3>岗位编制</h3><span>{{ selectedDepartment ? selectedDepartment.deptName : '请先选择部门' }}</span></div>
        <el-button type="primary" :disabled="!selectedDepartment" @click="openPost()">新增岗位</el-button>
      </div>
      <el-empty v-if="!selectedDepartment" description="点击上方部门行查看岗位" />
      <el-table v-else :data="posts" border stripe>
        <el-table-column prop="postCode" label="编码" width="130" />
        <el-table-column prop="postName" label="岗位" min-width="160" />
        <el-table-column prop="headcount" label="编制" width="90" />
        <el-table-column label="实际在岗" width="100"><template #default="{ row }">{{ staffByPost(row).length }}</template></el-table-column>
        <el-table-column prop="remark" label="说明" min-width="220" />
        <el-table-column label="操作" width="170"><template #default="{ row }">
          <el-button size="small" @click="openPost(row)">编辑</el-button>
          <el-button size="small" type="danger" plain @click="removePost(row)">删除</el-button>
        </template></el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="departmentDialog" :title="departmentForm.deptId ? '编辑部门' : '新增部门'" width="520px">
      <el-form :model="departmentForm" label-width="90px">
        <el-form-item label="所属门店" required><el-select v-model="departmentForm.storeId" style="width:100%"><el-option v-for="s in stores" :key="s.store_id" :label="s.store_name" :value="s.store_id" /></el-select></el-form-item>
        <el-form-item label="部门名称" required><el-input v-model="departmentForm.deptName" /></el-form-item>
        <el-form-item label="部门编码" required><el-input v-model="departmentForm.deptCode" /></el-form-item>
        <el-form-item label="职能说明"><el-input v-model="departmentForm.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="departmentDialog=false">取消</el-button><el-button type="primary" :loading="saving" @click="saveDepartment">保存并回读</el-button></template>
    </el-dialog>

    <el-dialog v-model="postDialog" :title="postForm.postId ? '编辑岗位' : '新增岗位'" width="500px">
      <el-form :model="postForm" label-width="90px">
        <el-form-item label="所属部门"><el-input :model-value="selectedDepartment?.deptName" disabled /></el-form-item>
        <el-form-item label="岗位名称" required><el-input v-model="postForm.postName" /></el-form-item>
        <el-form-item label="岗位编码" required><el-input v-model="postForm.postCode" /></el-form-item>
        <el-form-item label="岗位编制"><el-input-number v-model="postForm.headcount" :min="1" :max="999" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="postForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="postDialog=false">取消</el-button><el-button type="primary" :loading="saving" @click="savePost">保存并回读</el-button></template>
    </el-dialog>
  </main>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const saving = ref(false)
const stores = ref([])
const departments = ref([])
const staff = ref([])
const posts = ref([])
const selectedDepartment = ref(null)
const departmentDialog = ref(false)
const postDialog = ref(false)
const departmentForm = reactive({ deptId:null, storeId:1, deptName:'', deptCode:'', description:'', status:'active', sortOrder:0, level:1 })
const postForm = reactive({ postId:null, deptId:null, postName:'', postCode:'', headcount:1, sortOrder:0, remark:'' })

const storeName = id => stores.value.find(s => Number(s.store_id) === Number(id))?.store_name || `门店 ${id}`
const countStaff = id => staff.value.filter(s => Number(s.storeId) === Number(id) && s.employmentStatus === 'active').length
const countDepartments = id => departments.value.filter(d => Number(d.storeId) === Number(id)).length
const staffByDepartment = row => staff.value.filter(s => s.employmentStatus === 'active' && (Number(s.deptId) === Number(row.deptId) || s.department === row.deptName))
const staffByPost = row => staffByDepartment(selectedDepartment.value).filter(s => s.staffPosition === row.postName)

async function loadAll() {
  loading.value = true
  try {
    const [storeRes, deptRes, staffRes] = await Promise.all([request.get('/stores'), request.get('/hr/departments'), request.get('/hr/staff')])
    stores.value = storeRes.data || []
    departments.value = deptRes.data || []
    staff.value = staffRes.data || []
    if (selectedDepartment.value) {
      const refreshed = departments.value.find(d => d.deptId === selectedDepartment.value.deptId)
      if (refreshed) await selectDepartment(refreshed)
    }
  } finally { loading.value = false }
}

async function selectDepartment(row) {
  selectedDepartment.value = row || null
  posts.value = row ? (await request.get('/hr/posts', { params:{ deptId:row.deptId } })).data || [] : []
}

function openDepartment(row) {
  Object.assign(departmentForm, row ? { ...row } : { deptId:null, storeId:stores.value[0]?.store_id || 1, deptName:'', deptCode:`E2E-DEPT-${Date.now()}`, description:'', status:'active', sortOrder:0, level:1 })
  departmentDialog.value = true
}
async function saveDepartment() {
  if (!departmentForm.deptName.trim() || !departmentForm.deptCode.trim()) return ElMessage.warning('请填写部门名称和编码')
  saving.value = true
  try {
    if (departmentForm.deptId) await request.put(`/hr/departments/${departmentForm.deptId}`, departmentForm)
    else await request.post('/hr/departments', departmentForm)
    departmentDialog.value = false
    await loadAll()
    ElMessage.success('部门已写入数据库并重新抓取')
  } finally { saving.value = false }
}
async function removeDepartment(row) {
  if (staffByDepartment(row).length) return ElMessage.warning('部门仍有关联员工，禁止删除')
  await ElMessageBox.confirm(`确认删除部门“${row.deptName}”？`, '数据约束确认')
  await request.delete(`/hr/departments/${row.deptId}`)
  if (selectedDepartment.value?.deptId === row.deptId) { selectedDepartment.value=null; posts.value=[] }
  await loadAll()
  ElMessage.success('部门已删除并重新抓取')
}
function openPost(row) {
  Object.assign(postForm, row ? { ...row } : { postId:null, deptId:selectedDepartment.value.deptId, postName:'', postCode:`E2E-POST-${Date.now()}`, headcount:1, sortOrder:0, remark:'' })
  postDialog.value = true
}
async function savePost() {
  if (!postForm.postName.trim() || !postForm.postCode.trim()) return ElMessage.warning('请填写岗位名称和编码')
  saving.value = true
  try {
    if (postForm.postId) await request.put(`/hr/posts/${postForm.postId}`, postForm)
    else await request.post('/hr/posts', postForm)
    postDialog.value = false
    await selectDepartment(selectedDepartment.value)
    ElMessage.success('岗位已写入数据库并重新抓取')
  } finally { saving.value = false }
}
async function removePost(row) {
  if (staffByPost(row).length) return ElMessage.warning('岗位仍有关联员工，禁止删除')
  await ElMessageBox.confirm(`确认删除岗位“${row.postName}”？`, '数据约束确认')
  await request.delete(`/hr/posts/${row.postId}`)
  await selectDepartment(selectedDepartment.value)
  ElMessage.success('岗位已删除并重新抓取')
}
onMounted(loadAll)
</script>

<style scoped>
.organization-page{max-width:1400px}.page-header,.section-heading{display:flex;align-items:center;justify-content:space-between;gap:16px}.page-header{margin-bottom:20px}.page-header h2,.section-heading h3{margin:0;color:var(--color-text)}.page-header p,.section-heading span{margin:4px 0 0;color:var(--color-text-muted);font-size:13px}.section-block{padding:20px;margin-bottom:18px;background:var(--color-card);border:1px solid var(--color-border);border-radius:var(--radius-lg)}.section-heading{margin-bottom:14px}.store-cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:12px}.store-card{padding:16px;background:var(--color-bg-alt);border:1px solid var(--color-border);border-radius:8px}.store-card>div:first-child{display:flex;align-items:baseline;gap:8px}.store-card small,.store-card p{color:var(--color-text-muted)}.store-card p{min-height:36px;font-size:13px}.store-card dl{display:flex;gap:18px;margin:12px 0 0}.store-card dl div{display:flex;flex-direction:column;gap:3px}.store-card dt{font-size:12px;color:var(--color-text-muted)}.store-card dd{margin:0;font-weight:600;color:var(--color-text)}
</style>
