<template>
  <div class="member-page">
    <div class="page-topbar">
      <div class="topbar-left">
        <h1 class="page-title">会员管理</h1>
        <span class="page-desc">会员档案 · 等级 · 充值 · 消费记录</span>
      </div>
      <el-button type="primary" @click="showAddDialog = true">新增会员</el-button>
    </div>

    <div class="filter-bar">
      <el-input v-model="search" placeholder="搜索会员姓名 / 电话 / 卡号" clearable style="width:220px" />
      <el-select v-model="filterLevel" placeholder="会员等级" clearable style="width:140px">
        <el-option label="普通" value="普通" />
        <el-option label="银卡" value="银卡" />
        <el-option label="金卡" value="金卡" />
        <el-option label="钻石" value="钻石" />
      </el-select>
      <el-select v-model="filterStatus" placeholder="状态" clearable style="width:120px">
        <el-option label="正常" value="正常" />
        <el-option label="冻结" value="冻结" />
      </el-select>
      <el-button @click="doSearch">查询</el-button>
      <el-button @click="resetFilter">重置</el-button>
    </div>

    <el-table :data="filteredList" stripe class="data-table" v-loading="loading">
      <el-table-column prop="cardNo" label="卡号" width="180" />
      <el-table-column prop="memberName" label="姓名" width="120" />
      <el-table-column prop="memberPhone" label="电话" width="150" />
      <el-table-column prop="level" label="等级" width="100">
        <template #default="{ row }">
          <el-tag :type="levelTag(row.level)" size="small" effect="plain">{{ row.level || '普通' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="balance" label="余额" width="120" align="right">
        <template #default="{ row }">
          <span style="color:#C25555;font-weight:600">¥{{ (row.balance || 0).toFixed(2) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="points" label="积分" width="80" align="center" />
      <el-table-column prop="totalSpent" label="累计消费" width="120" align="right">
        <template #default="{ row }">¥{{ (row.totalSpent || 0).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column prop="visitCount" label="消费次数" width="90" align="center" />
      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === '正常' ? 'success' : 'danger'" size="small" effect="plain">{{ row.status || '正常' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="registerDate" label="注册日期" width="120" />
      <el-table-column label="操作" min-width="160">
        <template #default="{ row }">
          <el-button text size="small" @click="viewMember(row)">详情</el-button>
          <el-button text size="small" type="primary" @click="rechargeMember(row)">充值</el-button>
          <el-button text size="small" @click="editMember(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="table-footer">
      <span class="total-text">共 {{ filteredList.length }} 条</span>
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="filteredList.length"
        layout="prev, pager, next"
        small
      />
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="showAddDialog" :title="editingMember ? '编辑会员' : '新增会员'" width="500px">
      <el-form :model="memberForm" label-width="80px">
        <el-form-item label="姓名"><el-input v-model="memberForm.memberName" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="memberForm.memberPhone" /></el-form-item>
        <el-form-item label="等级">
          <el-select v-model="memberForm.level" style="width:100%">
            <el-option label="普通" value="普通" />
            <el-option label="银卡" value="银卡" />
            <el-option label="金卡" value="金卡" />
            <el-option label="钻石" value="钻石" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="memberForm.status" style="width:100%">
            <el-option label="正常" value="正常" />
            <el-option label="冻结" value="冻结" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="saveMember">保存</el-button>
      </template>
    </el-dialog>

    <!-- 充值对话框 -->
    <el-dialog v-model="showRechargeDialog" title="会员充值" width="400px">
      <div v-if="rechargeTarget" style="margin-bottom:16px">
        <p>会员：{{ rechargeTarget.memberName }}</p>
        <p>卡号：{{ rechargeTarget.cardNo }}</p>
        <p>当前余额：¥{{ (rechargeTarget.balance || 0).toFixed(2) }}</p>
      </div>
      <el-form>
        <el-form-item label="充值金额">
          <el-input-number v-model="rechargeAmount" :min="1" :max="10000" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRechargeDialog = false">取消</el-button>
        <el-button type="primary" @click="doRecharge">确认充值</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const members = ref([])
const search = ref('')
const filterLevel = ref('')
const filterStatus = ref('')
const currentPage = ref(1)
const pageSize = ref(20)

const showAddDialog = ref(false)
const showRechargeDialog = ref(false)
const editingMember = ref(null)
const rechargeTarget = ref(null)
const rechargeAmount = ref(0)

const memberForm = ref({
  memberName: '', memberPhone: '', level: '普通', status: '正常'
})

const filteredList = computed(() => {
  let list = members.value
  if (search.value) {
    const q = search.value.toLowerCase()
    list = list.filter(m =>
      (m.memberName || '').includes(q) ||
      (m.memberPhone || '').includes(q) ||
      (m.cardNo || '').toLowerCase().includes(q)
    )
  }
  if (filterLevel.value) list = list.filter(m => m.level === filterLevel.value)
  if (filterStatus.value) list = list.filter(m => m.status === filterStatus.value)
  return list
})

function levelTag(level) {
  return { '普通': 'info', '银卡': '', '金卡': 'warning', '钻石': 'danger' }[level] || 'info'
}

async function loadData() {
  loading.value = true
  try {
    const res = await request.get('/api/members', { params: { page: currentPage.value, pageSize: pageSize.value, keyword: search.value, level: filterLevel.value, status: filterStatus.value } })
    const data = res.data || res
    members.value = data?.list || data?.content || data?.rows || data || []
  } catch (e) {
    console.error('获取会员列表失败', e)
    ElMessage.error('获取会员列表失败')
    members.value = []
  } finally {
    loading.value = false
  }
}

function doSearch() { currentPage.value = 1 }
function resetFilter() { search.value = ''; filterLevel.value = ''; filterStatus.value = '' }

function viewMember(row) { ElMessage.info(`查看会员：${row.memberName}`) }
function editMember(row) {
  editingMember.value = row
  memberForm.value = { ...row }
  showAddDialog.value = true
}
function rechargeMember(row) {
  rechargeTarget.value = row
  rechargeAmount.value = 0
  showRechargeDialog.value = true
}
async function doRecharge() {
  if (rechargeAmount.value <= 0) { ElMessage.warning('请输入充值金额'); return }
  try {
    await request.post(`/api/members/${rechargeTarget.value.cardNo}/recharge`, { amount: rechargeAmount.value })
    ElMessage.success(`充值成功：¥${rechargeAmount.value}`)
    showRechargeDialog.value = false
    loadData()
  } catch (e) {
    console.error('充值失败', e)
    ElMessage.error(e.response?.data?.message || '充值失败')
  }
}
async function saveMember() {
  try {
    if (editingMember.value) {
      await request.put(`/api/members/${editingMember.value.cardNo}`, memberForm.value)
    } else {
      await request.post('/api/members', memberForm.value)
    }
    ElMessage.success('保存成功')
    showAddDialog.value = false
    loadData()
  } catch (e) {
    console.error('保存会员失败', e)
    ElMessage.error(e.response?.data?.message || '保存失败')
  }
}

onMounted(() => { loadData() })
</script>

<style scoped>
.member-page { max-width: 1400px; margin: 0 auto; padding-bottom: 40px; }
.page-topbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.topbar-left { display: flex; flex-direction: column; }
.page-title { font-size: 22px; font-weight: 700; color: var(--color-text-primary); margin: 0; }
.page-desc { font-size: 13px; color: var(--color-text-secondary); margin-top: 2px; }
.filter-bar { display: flex; gap: 10px; margin-bottom: 16px; align-items: center; }
.data-table { border-radius: 2px; overflow: hidden; }
.table-footer { display: flex; align-items: center; justify-content: space-between; margin-top: 12px; }
.total-text { font-size: 13px; color: var(--color-text-secondary); }
</style>
