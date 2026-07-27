<template>
  <div class="customers-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">客户管理 · Customers</h2>
        <p class="page-subtitle">Customer profiles, consumption records, and analytics</p>
      </div>
      <div class="header-actions">
        <button class="btn-primary" @click="showAddDialog = true">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
          新增客户
        </button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card" style="--stat-color: #2D4A3E">
        <div class="stat-icon">👥</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.total }}</div>
          <div class="stat-label">总客户数</div>
        </div>
      </div>
      <div class="stat-card" style="--stat-color: #4A7C59">
        <div class="stat-icon">📅</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.newThisMonth }}</div>
          <div class="stat-label">本月新增</div>
        </div>
      </div>
      <div class="stat-card" style="--stat-color: #C4A35A">
        <div class="stat-icon">💰</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalSpent }}</div>
          <div class="stat-label">累计消费(元)</div>
        </div>
      </div>
      <div class="stat-card" style="--stat-color: #5B7B8A">
        <div class="stat-icon">📊</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.avgVisits }}</div>
          <div class="stat-label">平均到店次数</div>
        </div>
      </div>
    </div>

    <!-- 标签筛选 -->
    <div class="tag-filter">
      <button v-for="tag in tags" :key="tag" :class="['tag-btn', {active: selectedTag === tag}]" @click="selectTag(tag)">
        {{ tag }}
      </button>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <input class="search-input" v-model="keyword" placeholder="搜索客户姓名 / 电话 / 备注" @input="handleSearch" />
      <button class="search-btn" @click="handleSearch">搜索</button>
    </div>

    <!-- 客户列表 -->
    <div class="customer-list">
      <div v-for="customer in customers" :key="customer.id" class="customer-card" @click="showDetail(customer)">
        <div class="customer-avatar">
          <span>{{ customer.name?.charAt(0) || '客' }}</span>
        </div>
        <div class="customer-info">
          <div class="customer-name">{{ customer.name }}</div>
          <div class="customer-phone">{{ formatPhone(customer.phone) }}</div>
          <div class="customer-tags">
            <span v-for="t in customer.tags?.split(',') || []" :key="t" class="mini-tag">{{ t }}</span>
          </div>
        </div>
        <div class="customer-meta">
          <div class="meta-item">📅 {{ customer.visitCount || 0 }}次</div>
          <div class="meta-item">💰 {{ customer.totalAmount || 0 }}元</div>
          <div class="meta-item">📝 {{ customer.lastVisit || '-' }}</div>
        </div>
        <div class="customer-arrow">→</div>
      </div>
    </div>

    <!-- 客户详情弹窗 -->
    <div v-if="selectedCustomer" class="detail-modal" @click.self="closeDetail">
      <div class="modal-content">
        <div class="modal-header">
          <h3>客户详情 · {{ selectedCustomer.name }}</h3>
          <button class="close-btn" @click="closeDetail">×</button>
        </div>
        <div class="modal-body">
          <div class="detail-section">
            <h4>基本信息</h4>
            <div class="detail-row">
              <span class="detail-label">姓名</span>
              <span class="detail-value">{{ selectedCustomer.name }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">电话</span>
              <span class="detail-value">{{ selectedCustomer.phone }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">性别</span>
              <span class="detail-value">{{ selectedCustomer.gender === 'male' ? '男' : selectedCustomer.gender === 'female' ? '女' : '-' }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">生日</span>
              <span class="detail-value">{{ selectedCustomer.birthday || '-' }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">备注</span>
              <span class="detail-value">{{ selectedCustomer.remark || '-' }}</span>
            </div>
          </div>

          <div class="detail-section">
            <h4>消费统计</h4>
            <div class="stats-grid">
              <div class="mini-stat">
                <div class="mini-value">{{ selectedCustomer.visitCount || 0 }}</div>
                <div class="mini-label">到店次数</div>
              </div>
              <div class="mini-stat">
                <div class="mini-value">{{ selectedCustomer.totalAmount || 0 }}</div>
                <div class="mini-label">累计消费</div>
              </div>
              <div class="mini-stat">
                <div class="mini-value">{{ selectedCustomer.avgAmount || 0 }}</div>
                <div class="mini-label">平均消费</div>
              </div>
              <div class="mini-stat">
                <div class="mini-value">{{ selectedCustomer.lastVisit || '-' }}</div>
                <div class="mini-label">最近到店</div>
              </div>
            </div>
          </div>

          <div class="detail-section">
            <h4>消费记录</h4>
            <div v-if="customerHistory.length === 0" class="empty-history">暂无消费记录</div>
            <div v-else class="history-list">
              <div v-for="record in customerHistory" :key="record.id" class="history-item">
                <div class="history-date">{{ record.bookingDate }}</div>
                <div class="history-info">
                  <div class="history-type">{{ record.occasionType || '聚餐' }}</div>
                  <div class="history-tables">{{ record.tableNames }}</div>
                </div>
                <div class="history-amount">{{ record.amount || 0 }}元</div>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-secondary" @click="closeDetail">关闭</button>
          <button class="btn-primary" @click="editCustomer">编辑</button>
        </div>
      </div>
    </div>

    <!-- 新增/编辑客户弹窗 -->
    <div v-if="showAddDialog" class="detail-modal" @click.self="showAddDialog = false">
      <div class="modal-content">
        <div class="modal-header">
          <h3>{{ editingCustomer ? '编辑客户' : '新增客户' }}</h3>
          <button class="close-btn" @click="showAddDialog = false">×</button>
        </div>
        <div class="modal-body">
          <div class="form-row">
            <div class="form-group">
              <label>姓名 *</label>
              <input class="form-input" v-model="form.name" placeholder="请输入客户姓名" />
            </div>
            <div class="form-group">
              <label>电话 *</label>
              <input class="form-input" v-model="form.phone" placeholder="请输入手机号码" />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>性别</label>
              <select class="form-select" v-model="form.gender">
                <option value="">请选择</option>
                <option value="male">男</option>
                <option value="female">女</option>
              </select>
            </div>
            <div class="form-group">
              <label>生日</label>
              <input class="form-input" type="date" v-model="form.birthday" />
            </div>
          </div>
          <div class="form-group">
            <label>标签</label>
            <input class="form-input" v-model="form.tags" placeholder="多个标签用逗号分隔" />
          </div>
          <div class="form-group">
            <label>备注</label>
            <textarea class="form-textarea" v-model="form.remark" placeholder="客户备注信息"></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-secondary" @click="showAddDialog = false">取消</button>
          <button class="btn-primary" @click="saveCustomer">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCustomers, createCustomer, updateCustomer, getCustomerHistory } from '@/api/customer'

const router = useRouter()

const customers = ref([])
const selectedCustomer = ref(null)
const customerHistory = ref([])
const showAddDialog = ref(false)
const editingCustomer = ref(null)
const keyword = ref('')
const selectedTag = ref('全部')

const tags = ['全部', 'VIP客户', '企业客户', '常客', '新客户', '生日客户']

const stats = reactive({
  total: 0,
  newThisMonth: 0,
  totalSpent: 0,
  avgVisits: 0
})

const form = reactive({
  name: '',
  phone: '',
  gender: '',
  birthday: '',
  tags: '',
  remark: ''
})

function formatPhone(phone) {
  if (!phone) return '-'
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

async function loadCustomers() {
  try {
    const res = await getCustomers({ page_size: 999 })
    customers.value = res?.data?.list || res?.data || getMockCustomers()
    updateStats()
  } catch (e) {
    console.error('加载客户失败', e)
    customers.value = getMockCustomers()
    updateStats()
  }
}

function getMockCustomers() {
  return [
    { id: 1, name: '张三', phone: '13812345678', gender: 'male', birthday: '1990-01-15', tags: 'VIP客户,常客', visitCount: 15, totalAmount: 8600, avgAmount: 573, lastVisit: '2026-07-24', remark: '喜欢包间' },
    { id: 2, name: '李四', phone: '13987654321', gender: 'female', birthday: '1988-05-20', tags: '企业客户', visitCount: 8, totalAmount: 12000, avgAmount: 1500, lastVisit: '2026-07-22', remark: '公司聚餐常用' },
    { id: 3, name: '王五', phone: '13711112222', gender: 'male', birthday: '', tags: '新客户', visitCount: 2, totalAmount: 680, avgAmount: 340, lastVisit: '2026-07-25', remark: '' },
    { id: 4, name: '赵六', phone: '13633334444', gender: 'female', birthday: '1992-11-08', tags: '生日客户', visitCount: 5, totalAmount: 3200, avgAmount: 640, lastVisit: '2026-07-20', remark: '下个月生日' },
    { id: 5, name: '钱七', phone: '13555556666', gender: 'male', birthday: '', tags: '常客', visitCount: 20, totalAmount: 15000, avgAmount: 750, lastVisit: '2026-07-23', remark: '每周来两次' },
    { id: 6, name: '孙八', phone: '13477778888', gender: 'female', birthday: '1995-03-12', tags: 'VIP客户', visitCount: 12, totalAmount: 9800, avgAmount: 817, lastVisit: '2026-07-21', remark: '' }
  ]
}

function updateStats() {
  stats.total = customers.value.length
  stats.newThisMonth = customers.value.filter(c => c.lastVisit && c.lastVisit.startsWith('2026-07')).length
  stats.totalSpent = customers.value.reduce((sum, c) => sum + (c.totalAmount || 0), 0)
  stats.avgVisits = customers.value.length > 0 
    ? Math.round(customers.value.reduce((sum, c) => sum + (c.visitCount || 0), 0) / customers.value.length)
    : 0
}

function handleSearch() {
  if (!keyword.value) {
    loadCustomers()
    return
  }
  customers.value = customers.value.filter(c => 
    c.name?.includes(keyword.value) || c.phone?.includes(keyword.value) || c.remark?.includes(keyword.value)
  )
}

function selectTag(tag) {
  selectedTag.value = tag
  if (tag === '全部') {
    loadCustomers()
    return
  }
  customers.value = customers.value.filter(c => (c.tags || '').includes(tag))
}

async function showDetail(customer) {
  selectedCustomer.value = customer
  try {
    const res = await getCustomerHistory(customer.id)
    customerHistory.value = res?.data?.list || res?.data || getMockHistory(customer.id)
  } catch (e) {
    customerHistory.value = getMockHistory(customer.id)
  }
}

function getMockHistory(customerId) {
  return [
    { id: 1, bookingDate: '2026-07-24', occasionType: '家庭聚餐', tableNames: '二楼201', amount: 580 },
    { id: 2, bookingDate: '2026-07-18', occasionType: '朋友聚会', tableNames: '一楼大厅3号', amount: 420 },
    { id: 3, bookingDate: '2026-07-10', occasionType: '商务宴请', tableNames: '二楼203', amount: 1200 }
  ]
}

function closeDetail() {
  selectedCustomer.value = null
  customerHistory.value = []
}

function editCustomer() {
  if (!selectedCustomer.value) return
  editingCustomer.value = selectedCustomer.value
  Object.assign(form, {
    name: selectedCustomer.value.name,
    phone: selectedCustomer.value.phone,
    gender: selectedCustomer.value.gender || '',
    birthday: selectedCustomer.value.birthday || '',
    tags: selectedCustomer.value.tags || '',
    remark: selectedCustomer.value.remark || ''
  })
  showAddDialog.value = true
}

function saveCustomer() {
  if (!form.name || !form.phone) {
    alert('请填写姓名和电话')
    return
  }
  
  if (editingCustomer.value) {
    updateCustomer(editingCustomer.value.id, form).then(() => {
      alert('修改成功')
      showAddDialog.value = false
      loadCustomers()
    })
  } else {
    createCustomer(form).then(() => {
      alert('添加成功')
      showAddDialog.value = false
      loadCustomers()
    })
  }
  
  Object.assign(form, { name: '', phone: '', gender: '', birthday: '', tags: '', remark: '' })
  editingCustomer.value = null
}

onMounted(() => {
  loadCustomers()
})
</script>

<style scoped>
.customers-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 4px;
}

.page-subtitle {
  font-size: 14px;
  color: var(--color-text-muted);
}

.btn-primary {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  font-size: 14px;
  cursor: pointer;
  transition: var(--transition);
}

.btn-primary:hover {
  background: var(--color-primary-dark);
}

.btn-secondary {
  padding: 10px 20px;
  background: var(--color-card);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  cursor: pointer;
  transition: var(--transition);
}

.btn-secondary:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  background: color-mix(in srgb, var(--stat-color) 10%, transparent);
  border-radius: var(--radius-md);
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--stat-color);
}

.stat-label {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-top: 2px;
}

.tag-filter {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.tag-btn {
  padding: 6px 14px;
  border: 1px solid var(--color-border);
  border-radius: 20px;
  background: var(--color-card);
  color: var(--color-text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: var(--transition);
}

.tag-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.tag-btn.active {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.search-input {
  flex: 1;
  padding: 10px 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  background: var(--color-card);
}

.search-btn {
  padding: 10px 24px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  font-size: 14px;
  cursor: pointer;
}

.customer-list {
  display: grid;
  gap: 12px;
}

.customer-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: var(--transition);
}

.customer-card:hover {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-sm);
}

.customer-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
}

.customer-info {
  flex: 1;
}

.customer-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
}

.customer-phone {
  font-size: 13px;
  color: var(--color-text-muted);
  margin-top: 4px;
}

.customer-tags {
  display: flex;
  gap: 6px;
  margin-top: 6px;
}

.mini-tag {
  font-size: 11px;
  padding: 2px 8px;
  background: rgba(45, 74, 62, 0.08);
  color: var(--color-primary);
  border-radius: 10px;
}

.customer-meta {
  display: flex;
  gap: 16px;
}

.meta-item {
  font-size: 12px;
  color: var(--color-text-muted);
}

.customer-arrow {
  font-size: 16px;
  color: var(--color-text-muted);
}

.detail-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  width: 90%;
  max-width: 600px;
  max-height: 90vh;
  background: var(--color-bg);
  border-radius: var(--radius-lg);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-card);
}

.modal-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0;
}

.close-btn {
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  color: var(--color-text-muted);
  font-size: 20px;
  cursor: pointer;
}

.modal-body {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.detail-section {
  margin-bottom: 24px;
}

.detail-section:last-child {
  margin-bottom: 0;
}

.detail-section h4 {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--color-border);
}

.detail-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px dashed var(--color-border);
}

.detail-label {
  font-size: 13px;
  color: var(--color-text-muted);
}

.detail-value {
  font-size: 13px;
  color: var(--color-text);
  font-weight: 500;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.mini-stat {
  text-align: center;
  padding: 12px;
  background: var(--color-card);
  border-radius: var(--radius-md);
}

.mini-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-primary);
}

.mini-label {
  font-size: 11px;
  color: var(--color-text-muted);
  margin-top: 4px;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.history-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  background: var(--color-card);
  border-radius: var(--radius-md);
}

.history-date {
  font-size: 13px;
  color: var(--color-text-muted);
  width: 100px;
}

.history-info {
  flex: 1;
  margin-left: 16px;
}

.history-type {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text);
}

.history-tables {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-top: 2px;
}

.history-amount {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-primary);
}

.empty-history {
  text-align: center;
  padding: 40px;
  color: var(--color-text-muted);
  font-size: 14px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid var(--color-border);
  background: var(--color-card);
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  margin-bottom: 6px;
}

.form-input, .form-select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  background: var(--color-card);
  box-sizing: border-box;
}

.form-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  background: var(--color-card);
  min-height: 80px;
  resize: vertical;
  box-sizing: border-box;
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .customer-card {
    flex-wrap: wrap;
  }
  .customer-meta {
    width: 100%;
    margin-top: 8px;
  }
  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
