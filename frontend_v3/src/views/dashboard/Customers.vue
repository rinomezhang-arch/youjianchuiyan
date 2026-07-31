<template>
  <div class="customer-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">客户管理 · Customer Management</h2>
        <p class="page-desc">客户档案 · 等级管理 · 消费历史 · 回头客分析</p>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-label">客户总数</div>
        <div class="stat-value">{{ stats.total }}</div>
        <div class="stat-sub">累计建档</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">本月新增</div>
        <div class="stat-value">{{ stats.newThisMonth }}</div>
        <div class="stat-sub">近30天</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">VIP客户</div>
        <div class="stat-value">{{ stats.vip }}</div>
        <div class="stat-sub">高价值客户</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">回头客</div>
        <div class="stat-value">{{ stats.repeat }}</div>
        <div class="stat-sub">二次以上到店</div>
      </div>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="search"
            placeholder="搜索姓名 / 手机号"
            clearable
            style="width: 240px"
          />
          <el-select v-model="filterLevel" placeholder="客户等级" clearable style="width: 140px">
            <el-option label="全部" value="" />
            <el-option label="VIP" value="VIP" />
            <el-option label="普通" value="普通" />
            <el-option label="黑名单" value="黑名单" />
          </el-select>
          <el-button @click="onReset">重置</el-button>
        </div>
        <div class="toolbar-right">
          <el-button type="primary" @click="openAdd">+ 新增客户</el-button>
        </div>
      </div>

      <el-table :data="filteredList" stripe v-loading="loading">
        <el-table-column prop="customerNo" label="客户编号" width="140" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="level" label="等级" width="100">
          <template #default="{ row }">
            <el-tag :type="levelTagType(row.level)" size="small" effect="plain">
              {{ row.level || '普通' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalSpent" label="累计消费" width="130" align="right">
          <template #default="{ row }">¥{{ (row.totalSpent || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="lastVisit" label="最近到店" width="130" />
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" min-width="180" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" @click="viewHistory(row)">历史</el-button>
            <el-button text size="small" type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="showFormDialog" :title="editing ? '编辑客户' : '新增客户'" width="520px">
      <el-form :model="customerForm" label-width="80px">
        <el-form-item label="姓名" required><el-input v-model="customerForm.name" /></el-form-item>
        <el-form-item label="手机号" required><el-input v-model="customerForm.phone" /></el-form-item>
        <el-form-item label="等级">
          <el-select v-model="customerForm.level" style="width: 100%">
            <el-option label="VIP" value="VIP" />
            <el-option label="普通" value="普通" />
            <el-option label="黑名单" value="黑名单" />
          </el-select>
        </el-form-item>
        <el-form-item label="生日">
          <el-date-picker
            v-model="customerForm.birthday"
            type="date"
            placeholder="选择生日"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="customerForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showFormDialog = false">取消</el-button>
        <el-button type="primary" @click="saveCustomer">保存</el-button>
      </template>
    </el-dialog>

    <!-- 到店历史对话框 -->
    <el-dialog v-model="showHistoryDialog" title="到店历史 · Visit History" width="640px">
      <div v-if="historyRow" class="history-header">
        <div class="history-customer">
          <span class="history-name">{{ historyRow.name || '-' }}</span>
          <el-tag :type="levelTagType(historyRow.level)" size="small" effect="plain">
            {{ historyRow.level || '普通' }}
          </el-tag>
          <span class="history-phone">{{ historyRow.phone || '-' }}</span>
        </div>
        <div class="history-summary">
          共到店 {{ visitHistory.length }} 次 · 累计消费 ¥{{ (historyRow.totalSpent || 0).toFixed(2) }}
        </div>
      </div>
      <el-table :data="visitHistory" stripe size="small" empty-text="暂无到店记录">
        <el-table-column prop="visitDate" label="到店日期" width="130" />
        <el-table-column prop="tableNo" label="桌号" width="90" />
        <el-table-column prop="personCount" label="人数" width="80" align="right" />
        <el-table-column prop="amount" label="消费金额" width="130" align="right">
          <template #default="{ row }">¥{{ (row.amount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
      </el-table>
      <template #footer>
        <el-button @click="showHistoryDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const search = ref('')
const filterLevel = ref('')

const customers = ref([])
const visitHistory = ref([])

const stats = ref({ total: 0, newThisMonth: 0, vip: 0, repeat: 0 })

const showFormDialog = ref(false)
const showHistoryDialog = ref(false)
const editing = ref(false)
const historyRow = ref(null)

const customerForm = ref({
  name: '',
  phone: '',
  level: '普通',
  birthday: '',
  remark: ''
})

const filteredList = computed(() => {
  let list = customers.value
  if (search.value) {
    const q = search.value.toLowerCase()
    list = list.filter(c =>
      (c.name || '').toLowerCase().includes(q) ||
      (c.phone || '').includes(q) ||
      (c.customerNo || '').toLowerCase().includes(q)
    )
  }
  if (filterLevel.value) list = list.filter(c => c.level === filterLevel.value)
  return list
})

function levelTagType(level) {
  return { 'VIP': 'warning', '普通': 'info', '黑名单': 'danger' }[level] || 'info'
}

function onReset() {
  search.value = ''
  filterLevel.value = ''
}

function openAdd() {
  editing.value = false
  customerForm.value = { name: '', phone: '', level: '普通', birthday: '', remark: '' }
  showFormDialog.value = true
}

function openEdit(row) {
  editing.value = true
  customerForm.value = { ...row }
  showFormDialog.value = true
}

function saveCustomer() {
  if (!customerForm.value.name) {
    ElMessage.warning('请填写客户姓名')
    return
  }
  if (!customerForm.value.phone) {
    ElMessage.warning('请填写手机号')
    return
  }
  ElMessage.success('保存成功')
  showFormDialog.value = false
}

function viewHistory(row) {
  historyRow.value = row
  visitHistory.value = []
  showHistoryDialog.value = true
}
</script>

<style scoped>
.customer-page {
  padding: 24px 32px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.header-left {
  display: flex;
  flex-direction: column;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a2f23;
  margin: 0 0 6px 0;
  letter-spacing: 0.5px;
}

.page-desc {
  font-size: 13px;
  color: #5D6D7E;
  margin: 0;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: #FFFFFF;
  border: 1px solid #E8E4DE;
  border-radius: 10px;
  padding: 20px;
  position: relative;
  overflow: hidden;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
  background: linear-gradient(180deg, #2D4A3E 0%, #C4A35A 100%);
}

.stat-card:hover {
  box-shadow: 0 8px 24px rgba(45, 74, 62, 0.1);
  transform: translateY(-2px);
}

.stat-label {
  font-size: 13px;
  color: #95A5A6;
  margin-bottom: 8px;
  font-weight: 500;
  letter-spacing: 0.5px;
}

.stat-value {
  font-size: 30px;
  font-weight: 700;
  color: #1a2f23;
  line-height: 1.2;
  letter-spacing: -0.3px;
}

.stat-sub {
  font-size: 12px;
  color: #95A5A6;
  margin-top: 6px;
}

.content-card {
  background: #FFFFFF;
  border: 1px solid #E8E4DE;
  border-radius: 10px;
  padding: 20px 24px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.history-header {
  padding-bottom: 16px;
  margin-bottom: 16px;
  border-bottom: 1px solid #E8E4DE;
}

.history-customer {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.history-name {
  font-size: 18px;
  font-weight: 700;
  color: #1a2f23;
}

.history-phone {
  font-size: 13px;
  color: #5D6D7E;
}

.history-summary {
  font-size: 13px;
  color: #95A5A6;
}

@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: 1fr;
  }
}
</style>
