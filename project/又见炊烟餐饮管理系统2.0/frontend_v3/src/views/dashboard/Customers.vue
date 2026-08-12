<template>
  <div class="page">
    <div class="page-header">
      <h2>客户管理 · Customer Management</h2>
      <p class="page-desc">客户搜索 · 历史记录 · 会员等级</p>
    </div>
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input v-model="keyword" placeholder="客户姓名/电话" class="search-box" clearable @keyup.enter="fetchData" />
      </div>
      <div class="toolbar-right">
        <el-button type="primary" @click="showAdd = true">+ 新增客户</el-button>
      </div>
    </div>
    <el-table :data="list" stripe class="data-table" v-loading="loading">
      <el-table-column prop="customerName" label="姓名" width="100" />
      <el-table-column prop="customerPhone" label="电话" width="140" />
      <el-table-column prop="bookingCount" label="预订次数" width="80" />
      <el-table-column prop="totalAmount" label="累计消费" width="120" />
      <el-table-column prop="memberLevel" label="会员等级" width="80" />
      <el-table-column prop="lastBookingDate" label="最后预订" width="120" />
      <el-table-column prop="remark" label="备注" min-width="150" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button text size="small" @click="editRow(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination background layout="prev,pager,next" :total="total" :page-size="20" @current-change="fetchData" class="mt-4" />

    <el-dialog v-model="showAdd" :title="editing ? '编辑客户' : '新增客户'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="客户姓名" required><el-input v-model="form.customerName" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="form.customerPhone" /></el-form-item>
        <el-form-item label="会员等级">
          <el-select v-model="form.memberLevel">
            <el-option label="V1 普通" value="v1" />
            <el-option label="V2 银卡" value="v2" />
            <el-option label="V3 金卡" value="v3" />
            <el-option label="V4 钻石" value="v4" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdd = false">取消</el-button>
        <el-button type="primary" @click="saveCustomer">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getCustomers, createCustomer, updateCustomer } from '@/api/customer'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const keyword = ref('')
const showAdd = ref(false)
const editing = ref(false)
const form = ref({ customerName: '', customerPhone: '', memberLevel: 'v1', remark: '' })

async function fetchData() {
  loading.value = true
  try {
    const res = await getCustomers({ keyword: keyword.value })
    if (res.code === 200) list.value = res.data?.content || res.data || []
  } catch (e) {
    console.error(e)
    ElMessage.error('加载客户数据失败')
  } finally {
    loading.value = false
  }
}

function editRow(row) {
  editing.value = true
  form.value = { ...row }
  showAdd.value = true
}

async function saveCustomer() {
  loading.value = true
  try {
    const res = editing.value
      ? await updateCustomer(form.value.id || form.value.customerId, form.value)
      : await createCustomer(form.value)
    if (res.code === 200) {
      ElMessage.success('保存成功')
      showAdd.value = false
      editing.value = false
      fetchData()
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('保存客户失败')
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>

<style scoped>
.page-header { display:flex; align-items:center; gap:12px; margin-bottom:16px; }
.page-header h2 { font-size:18px; font-weight:600; margin:0; }
.page-desc { font-size:13px; color:#64748b; margin:2px 0 0; }
</style>
