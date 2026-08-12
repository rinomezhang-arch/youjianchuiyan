<template>
  <div class="member-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">会员管理 · Member List</h2>
        <p class="page-desc">会员档案 · 等级 · 积分 · 消费记录</p>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-label">总会员</div>
        <div class="stat-value">1,694</div>
        <div class="stat-sub">累计注册</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">金卡</div>
        <div class="stat-value">86</div>
        <div class="stat-sub">高价值会员</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">银卡</div>
        <div class="stat-value">328</div>
        <div class="stat-sub">中价值会员</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">普通</div>
        <div class="stat-value">1,280</div>
        <div class="stat-sub">基础会员</div>
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
          <el-select v-model="filterLevel" placeholder="会员等级" clearable style="width: 140px">
            <el-option label="全部" value="" />
            <el-option label="普通" value="普通" />
            <el-option label="银卡" value="银卡" />
            <el-option label="金卡" value="金卡" />
          </el-select>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </div>
        <div class="toolbar-right">
          <el-button type="primary" @click="openAdd">+ 新增会员</el-button>
        </div>
      </div>

      <el-table :data="filteredList" stripe v-loading="loading">
        <el-table-column prop="cardNo" label="会员号" width="180" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="level" label="等级" width="100">
          <template #default="{ row }">
            <el-tag :type="levelTagType(row.level)" size="small" effect="plain">
              {{ row.level || '普通' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="points" label="积分" width="100" align="right" />
        <el-table-column prop="totalSpent" label="累计消费" width="120" align="right">
          <template #default="{ row }">¥{{ (row.totalSpent || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="lastVisit" label="最近到店" width="120" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === '正常' ? 'success' : 'danger'" size="small" effect="plain">
              {{ row.status || '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="180" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" @click="viewDetail(row)">详情</el-button>
            <el-button text size="small" type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="showFormDialog" :title="editing ? '编辑会员' : '新增会员'" width="520px">
      <el-form :model="memberForm" label-width="80px">
        <el-form-item label="姓名"><el-input v-model="memberForm.name" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="memberForm.phone" /></el-form-item>
        <el-form-item label="等级">
          <el-select v-model="memberForm.level" style="width: 100%">
            <el-option label="普通" value="普通" />
            <el-option label="银卡" value="银卡" />
            <el-option label="金卡" value="金卡" />
          </el-select>
        </el-form-item>
        <el-form-item label="生日">
          <el-date-picker
            v-model="memberForm.birthday"
            type="date"
            placeholder="选择生日"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="memberForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showFormDialog = false">取消</el-button>
        <el-button type="primary" @click="saveMember">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog v-model="showDetailDialog" title="会员详情" width="560px">
      <div v-if="detailRow" class="detail-grid">
        <div class="detail-item">
          <span class="detail-label">会员号</span>
          <span class="detail-value">{{ detailRow.cardNo || '-' }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">姓名</span>
          <span class="detail-value">{{ detailRow.name || '-' }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">手机号</span>
          <span class="detail-value">{{ detailRow.phone || '-' }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">等级</span>
          <span class="detail-value">
            <el-tag :type="levelTagType(detailRow.level)" size="small" effect="plain">
              {{ detailRow.level || '普通' }}
            </el-tag>
          </span>
        </div>
        <div class="detail-item">
          <span class="detail-label">积分</span>
          <span class="detail-value">{{ detailRow.points || 0 }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">累计消费</span>
          <span class="detail-value">¥{{ (detailRow.totalSpent || 0).toFixed(2) }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">最近到店</span>
          <span class="detail-value">{{ detailRow.lastVisit || '-' }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">状态</span>
          <span class="detail-value">
            <el-tag :type="detailRow.status === '正常' ? 'success' : 'danger'" size="small" effect="plain">
              {{ detailRow.status || '正常' }}
            </el-tag>
          </span>
        </div>
        <div class="detail-item full">
          <span class="detail-label">备注</span>
          <span class="detail-value">{{ detailRow.remark || '无' }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="showDetailDialog = false">关闭</el-button>
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

const members = ref([])

const showFormDialog = ref(false)
const showDetailDialog = ref(false)
const editing = ref(false)
const detailRow = ref(null)

const memberForm = ref({
  name: '',
  phone: '',
  level: '普通',
  birthday: '',
  remark: ''
})

const filteredList = computed(() => {
  let list = members.value
  if (search.value) {
    const q = search.value.toLowerCase()
    list = list.filter(m =>
      (m.name || '').toLowerCase().includes(q) ||
      (m.phone || '').includes(q) ||
      (m.cardNo || '').toLowerCase().includes(q)
    )
  }
  if (filterLevel.value) list = list.filter(m => m.level === filterLevel.value)
  return list
})

function levelTagType(level) {
  return { '普通': 'info', '银卡': '', '金卡': 'warning' }[level] || 'info'
}

function onSearch() { /* 数据已通过 computed 实时过滤 */ }
function onReset() {
  search.value = ''
  filterLevel.value = ''
}

function openAdd() {
  editing.value = false
  memberForm.value = { name: '', phone: '', level: '普通', birthday: '', remark: '' }
  showFormDialog.value = true
}

function openEdit(row) {
  editing.value = true
  memberForm.value = { ...row }
  showFormDialog.value = true
}

function saveMember() {
  ElMessage.success('保存成功')
  showFormDialog.value = false
}

function viewDetail(row) {
  detailRow.value = row
  showDetailDialog.value = true
}
</script>

<style scoped>
.member-page {
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

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px 24px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-item.full {
  grid-column: 1 / -1;
}

.detail-label {
  font-size: 12px;
  color: #95A5A6;
  font-weight: 500;
}

.detail-value {
  font-size: 14px;
  color: #1a2f23;
  font-weight: 600;
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
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
