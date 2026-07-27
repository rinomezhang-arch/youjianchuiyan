<template>
  <div class="app-container">
    <!-- 顶栏 -->
    <header class="header">
      <div class="brand">🍲 又见炊烟 <span>· 采购管理</span></div>
      <div class="user-badge"><i class="fas fa-user-circle"></i> super_admin / rino</div>
    </header>

    <!-- 统计卡片 -->
    <section class="stats-grid">
      <div class="stat-card"><div class="stat-label">📦 总采购单</div><div class="stat-value">{{ totalOrders }}</div></div>
      <div class="stat-card warning"><div class="stat-label">⏳ 待入库</div><div class="stat-value">{{ pendingOrders }}</div></div>
      <div class="stat-card success"><div class="stat-label">✅ 已入库</div><div class="stat-value">{{ completedOrders }}</div></div>
      <div class="stat-card"><div class="stat-label">💰 总金额</div><div class="stat-value">¥{{ totalAmount }}</div></div>
    </section>

    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <button class="btn btn-primary" @click="openCreateDrawer"><i class="fas fa-plus"></i> 新建采购单</button>
        <button class="btn btn-outline" @click="showToast('导出采购单')"><i class="fas fa-download"></i> 导出</button>
      </div>
      <div class="filter-group">
        <input type="text" v-model="filters.keyword" placeholder="🔍 搜索单号/供应商" />
        <span class="filter-divider"></span>
        <select v-model="filters.status">
          <option value="">全部状态</option>
          <option value="待入库">待入库</option>
          <option value="已入库">已入库</option>
          <option value="已取消">已取消</option>
        </select>
        <span class="filter-divider"></span>
        <input type="date" v-model="filters.dateFrom" title="开始日期" style="width:140px;" />
        <span>至</span>
        <input type="date" v-model="filters.dateTo" title="结束日期" style="width:140px;" />
        <button class="btn btn-outline reset-btn" @click="resetFilters">重置</button>
      </div>
    </div>

    <!-- 表格 -->
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>采购单号</th>
            <th>供应商</th>
            <th>采购日期</th>
            <th>总金额 (¥)</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in filteredOrders" :key="order.id">
            <td><strong>{{ order.id }}</strong></td>
            <td>{{ order.supplier }}</td>
            <td>{{ order.date }}</td>
            <td>{{ order.total.toFixed(2) }}</td>
            <td><span class="badge" :class="statusClass(order.status)"><span class="dot" :class="statusDot(order.status)"></span>{{ order.status }}</span></td>
            <td>
              <button v-if="order.status === '待入库'" class="action-btn" @click="receiveOrder(order.id)"><i class="fas fa-boxes"></i> 入库</button>
              <button class="action-btn" @click="viewOrder(order.id)"><i class="fas fa-eye"></i> 详情</button>
              <button v-if="order.status === '待入库'" class="action-btn danger" @click="cancelOrder(order.id)"><i class="fas fa-times"></i> 取消</button>
            </td>
          </tr>
          <tr v-if="filteredOrders.length === 0">
            <td colspan="6" style="text-align:center; padding:40px; color:#86909c;">暂无采购单</td>
          </tr>
        </tbody>
      </table>
    </div>
    <div class="pagination">
      <span>共 <strong>{{ filteredOrders.length }}</strong> 项</span>
      <button class="btn btn-outline" @click="showToast('上一页')"><i class="fas fa-chevron-left"></i></button>
      <span>1 / 1</span>
      <button class="btn btn-outline" @click="showToast('下一页')"><i class="fas fa-chevron-right"></i></button>
    </div>

    <!-- 新建采购单抽屉 -->
    <div class="drawer-overlay" :class="{ open: createDrawerVisible }" @click="closeCreateDrawer"></div>
    <div class="drawer" :class="{ open: createDrawerVisible }">
      <button class="drawer-close" @click="closeCreateDrawer">&times;</button>
      <div class="drawer-title">📝 新建采购单</div>
      <div class="drawer-field">
        <label>供应商 <span style="color:#f53f3f;">*</span></label>
        <input type="text" v-model="newOrder.supplier" placeholder="请输入供应商名称" />
      </div>
      <div class="drawer-field">
        <label>采购日期</label>
        <input type="date" v-model="newOrder.date" />
      </div>
      <div class="drawer-field">
        <label>物料明细</label>
        <div v-for="(item, index) in newOrder.items" :key="index" style="display:flex; gap:8px; margin-bottom:8px; align-items:center;">
          <select v-model="item.code" style="flex:2; padding:8px; border-radius:8px; border:1px solid #e5e6eb;">
            <option v-for="mat in materialOptions" :key="mat.code" :value="mat.code">{{ mat.name }} ({{ mat.code }})</option>
          </select>
          <input type="number" v-model="item.quantity" placeholder="数量" style="flex:1; padding:8px; border-radius:8px; border:1px solid #e5e6eb;" />
          <input type="number" v-model="item.price" placeholder="单价" style="flex:1; padding:8px; border-radius:8px; border:1px solid #e5e6eb;" />
          <button class="btn btn-danger" style="padding:4px 10px;" @click="removeItem(index)"><i class="fas fa-trash"></i></button>
        </div>
        <button class="btn btn-outline" @click="addItem"><i class="fas fa-plus"></i> 添加物料</button>
      </div>
      <div class="drawer-field" style="text-align:right; font-weight:600;">
        预估总金额：¥ {{ newOrderTotal.toFixed(2) }}
      </div>
      <div class="drawer-footer">
        <button class="btn btn-primary" @click="submitOrder">确认创建</button>
        <button class="btn btn-outline" @click="closeCreateDrawer">取消</button>
      </div>
    </div>

    <!-- 详情抽屉（只读） -->
    <div class="drawer-overlay" :class="{ open: detailDrawerVisible }" @click="closeDetailDrawer"></div>
    <div class="drawer" :class="{ open: detailDrawerVisible }">
      <button class="drawer-close" @click="closeDetailDrawer">&times;</button>
      <div class="drawer-title">📄 采购单详情</div>
      <div v-if="detailOrder">
        <p><strong>单号：</strong>{{ detailOrder.id }}</p>
        <p><strong>供应商：</strong>{{ detailOrder.supplier }}</p>
        <p><strong>日期：</strong>{{ detailOrder.date }}</p>
        <p><strong>状态：</strong>{{ detailOrder.status }}</p>
        <hr style="margin:16px 0;" />
        <table style="width:100%; font-size:13px; border-collapse:collapse;">
          <thead><tr><th>物料</th><th>数量</th><th>单价</th><th>小计</th></tr></thead>
          <tbody>
            <tr v-for="(item, idx) in detailOrder.items" :key="idx">
              <td>{{ getMaterialName(item.code) }}</td>
              <td>{{ item.quantity }}</td>
              <td>{{ item.price.toFixed(2) }}</td>
              <td>{{ (item.quantity * item.price).toFixed(2) }}</td>
            </tr>
          </tbody>
        </table>
        <div style="text-align:right; margin-top:12px; font-weight:600;">总计：¥ {{ detailOrder.total.toFixed(2) }}</div>
      </div>
      <div class="drawer-footer">
        <button class="btn btn-outline" @click="closeDetailDrawer">关闭</button>
      </div>
    </div>

    <!-- Toast -->
    <div class="toast" :class="{ show: toastVisible }">{{ toastMessage }}</div>
  </div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'

// ---------- 模拟物料数据（与盘点保持一致） ----------
const materialOptions = [
  { code: 'YLLHS-00083', name: '高汤' },
  { code: 'YLLHS-00084', name: '戈鱼' },
  { code: 'YLLHS-00085', name: '鸽蛋' },
  { code: 'YLLHS-00086', name: '鸽子' },
  { code: 'YLLHS-00087', name: '枸杞' },
  { code: 'YLLHS-00088', name: '光鸭' },
  { code: 'YLLHS-00089', name: '桂花' },
  { code: 'YLLHS-00090', name: '桂皮' },
  { code: 'YLLHS-00091', name: '桂鱼' },
  { code: 'YLLHS-00092', name: '海苔' },
  { code: 'YLLHS-00093', name: '海星' },
  { code: 'YLLHS-00094', name: '海蟹' },
  { code: 'YLLHS-00095', name: '海蛰' },
  { code: 'YLLHS-00096', name: '旱芹' },
  { code: 'YLLHS-00097', name: '杭椒' },
  { code: 'YLLHS-00098', name: '耗油' },
  { code: 'YLLHS-00099', name: '河蚌' },
  { code: 'YLLHS-00100', name: '河粉' },
]

// ---------- 模拟采购订单数据 ----------
const orders = ref([
  {
    id: 'PO-2026-001',
    supplier: '鑫源食品',
    date: '2026-07-10',
    total: 1250.00,
    status: '待入库',
    items: [
      { code: 'YLLHS-00083', quantity: 10, price: 25.00 },
      { code: 'YLLHS-00087', quantity: 5, price: 200.00 },
    ]
  },
  {
    id: 'PO-2026-002',
    supplier: '海盛水产',
    date: '2026-07-09',
    total: 880.00,
    status: '已入库',
    items: [
      { code: 'YLLHS-00084', quantity: 20, price: 44.00 },
    ]
  },
  {
    id: 'PO-2026-003',
    supplier: '绿源蔬菜',
    date: '2026-07-08',
    total: 320.00,
    status: '已取消',
    items: [
      { code: 'YLLHS-00096', quantity: 30, price: 6.50 },
      { code: 'YLLHS-00097', quantity: 15, price: 8.00 },
    ]
  },
])

// ---------- 统计 ----------
const totalOrders = computed(() => orders.value.length)
const pendingOrders = computed(() => orders.value.filter(o => o.status === '待入库').length)
const completedOrders = computed(() => orders.value.filter(o => o.status === '已入库').length)
const totalAmount = computed(() => orders.value.reduce((sum, o) => sum + o.total, 0).toFixed(2))

// ---------- 筛选 ----------
const filters = reactive({
  keyword: '',
  status: '',
  dateFrom: '',
  dateTo: '',
})

const filteredOrders = computed(() => {
  return orders.value.filter(order => {
    const matchKeyword = !filters.keyword || order.id.toLowerCase().includes(filters.keyword.toLowerCase()) ||
                          order.supplier.includes(filters.keyword)
    const matchStatus = !filters.status || order.status === filters.status
    let matchDate = true
    if (filters.dateFrom) {
      matchDate = matchDate && order.date >= filters.dateFrom
    }
    if (filters.dateTo) {
      matchDate = matchDate && order.date <= filters.dateTo
    }
    return matchKeyword && matchStatus && matchDate
  })
})

const resetFilters = () => {
  filters.keyword = ''
  filters.status = ''
  filters.dateFrom = ''
  filters.dateTo = ''
  showToast('🔄 已重置筛选')
}

// ---------- 状态样式 ----------
const statusClass = (status) => {
  if (status === '待入库') return 'badge-warning'
  if (status === '已入库') return 'badge-green'
  if (status === '已取消') return 'badge-gray'
  return ''
}
const statusDot = (status) => {
  if (status === '待入库') return 'dot-yellow'
  if (status === '已入库') return 'dot-green'
  if (status === '已取消') return 'dot-gray'
  return ''
}

// ---------- 操作 ----------
const receiveOrder = (id) => {
  const order = orders.value.find(o => o.id === id)
  if (order) {
    order.status = '已入库'
    showToast(`✅ 采购单 ${id} 已入库`)
  }
}
const cancelOrder = (id) => {
  const order = orders.value.find(o => o.id === id)
  if (order) {
    order.status = '已取消'
    showToast(`⛔ 采购单 ${id} 已取消`)
  }
}
const viewOrder = (id) => {
  const order = orders.value.find(o => o.id === id)
  if (order) {
    detailOrder.value = JSON.parse(JSON.stringify(order))
    detailDrawerVisible.value = true
  }
}

// ---------- 新建采购单 ----------
const createDrawerVisible = ref(false)
const newOrder = reactive({
  supplier: '',
  date: new Date().toISOString().slice(0,10),
  items: [
    { code: 'YLLHS-00083', quantity: 1, price: 0 }
  ]
})

const addItem = () => {
  newOrder.items.push({ code: 'YLLHS-00083', quantity: 1, price: 0 })
}
const removeItem = (index) => {
  if (newOrder.items.length > 1) {
    newOrder.items.splice(index, 1)
  } else {
    showToast('至少保留一项物料')
  }
}
const newOrderTotal = computed(() => {
  return newOrder.items.reduce((sum, item) => sum + (item.quantity || 0) * (item.price || 0), 0)
})

const openCreateDrawer = () => {
  // 重置表单
  newOrder.supplier = ''
  newOrder.date = new Date().toISOString().slice(0,10)
  newOrder.items = [{ code: 'YLLHS-00083', quantity: 1, price: 0 }]
  createDrawerVisible.value = true
}
const closeCreateDrawer = () => {
  createDrawerVisible.value = false
}

const submitOrder = () => {
  if (!newOrder.supplier.trim()) {
    showToast('⚠️ 请填写供应商')
    return
  }
  if (newOrder.items.some(item => item.quantity <= 0 || item.price < 0)) {
    showToast('⚠️ 物料数量或单价不合法')
    return
  }
  // 生成新单号
  const maxId = orders.value.reduce((max, o) => {
    const num = parseInt(o.id.split('-')[2])
    return num > max ? num : max
  }, 0)
  const newId = `PO-2026-${String(maxId + 1).padStart(3, '0')}`
  const total = newOrderTotal.value
  const order = {
    id: newId,
    supplier: newOrder.supplier,
    date: newOrder.date,
    total: total,
    status: '待入库',
    items: newOrder.items.map(item => ({ ...item }))
  }
  orders.value.push(order)
  closeCreateDrawer()
  showToast(`✅ 采购单 ${newId} 创建成功`)
}

// ---------- 详情抽屉 ----------
const detailDrawerVisible = ref(false)
const detailOrder = ref(null)
const closeDetailDrawer = () => {
  detailDrawerVisible.value = false
  detailOrder.value = null
}

const getMaterialName = (code) => {
  const mat = materialOptions.find(m => m.code === code)
  return mat ? mat.name : code
}

// ---------- Toast ----------
const toastVisible = ref(false)
const toastMessage = ref('')
let toastTimer = null
const showToast = (msg) => {
  toastMessage.value = msg
  toastVisible.value = true
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    toastVisible.value = false
  }, 2500)
}
</script>

<style scoped>
/* 复用之前的样式，稍作扩展 */
* { margin: 0; padding: 0; box-sizing: border-box; font-family: system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif; }
.app-container { max-width: 1440px; margin: 0 auto; background: #fff; border-radius: 24px; box-shadow: 0 20px 60px rgba(0,0,0,0.08); padding: 24px 28px; }
.header { display: flex; justify-content: space-between; align-items: center; padding-bottom: 20px; border-bottom: 1px solid #f0f2f5; flex-wrap: wrap; gap: 12px; }
.brand { font-weight: 700; font-size: 18px; color: #1d2129; }
.brand span { color: #86909c; font-weight: 400; font-size: 14px; }
.user-badge { background: #f2f3f5; padding: 6px 14px; border-radius: 40px; font-size: 13px; color: #4e5969; }
.user-badge i { margin-right: 6px; }

.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin: 20px 0 24px; }
.stat-card { background: #fafafa; padding: 16px 20px; border-radius: 16px; border: 1px solid #f0f0f0; transition: 0.2s; }
.stat-card:hover { background: #f4f6fa; border-color: #c9cdd4; }
.stat-label { font-size: 13px; color: #86909c; margin-bottom: 6px; }
.stat-value { font-size: 26px; font-weight: 600; color: #1d2129; letter-spacing: 0.5px; }
.stat-value small { font-size: 14px; font-weight: 400; color: #86909c; margin-left: 8px; }
.stat-card.warning .stat-value { color: #e8590c; }
.stat-card.success .stat-value { color: #00b42a; }

.toolbar { display: flex; flex-wrap: wrap; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 16px; }
.toolbar-left { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; }
.btn { display: inline-flex; align-items: center; gap: 6px; padding: 8px 18px; border-radius: 40px; border: 1px solid #d9dde4; background: #fff; font-size: 14px; cursor: pointer; transition: 0.2s; color: #1d2129; }
.btn-primary { background: #165dff; border-color: #165dff; color: #fff; }
.btn-primary:hover { background: #0e42d2; border-color: #0e42d2; }
.btn-outline:hover { background: #f2f3f5; }
.btn-success { background: #00b42a; border-color: #00b42a; color: #fff; }
.btn-success:hover { background: #009a24; }
.btn-danger { background: #f53f3f; border-color: #f53f3f; color: #fff; }
.btn-danger:hover { background: #d93030; }

.filter-group { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; background: #f7f8fa; padding: 6px 12px; border-radius: 40px; }
.filter-group select, .filter-group input { border: none; background: transparent; padding: 6px 8px; font-size: 13px; outline: none; color: #1d2129; }
.filter-group select { cursor: pointer; }
.filter-group input::placeholder { color: #b3b8c4; }
.filter-divider { width: 1px; height: 24px; background: #e5e6eb; }
.reset-btn { border: none; background: #e5e6eb; }

.table-wrap { overflow-x: auto; border-radius: 16px; border: 1px solid #f0f0f0; }
table { width: 100%; border-collapse: collapse; font-size: 14px; }
th { background: #fafbfc; padding: 14px 16px; text-align: left; font-weight: 600; color: #4e5969; border-bottom: 2px solid #f0f0f0; white-space: nowrap; }
td { padding: 14px 16px; border-bottom: 1px solid #f5f6f8; color: #1d2129; vertical-align: middle; }
tr:hover td { background: #f7f9fc; }

.badge { display: inline-flex; align-items: center; gap: 4px; padding: 4px 12px; border-radius: 40px; font-size: 12px; font-weight: 500; }
.badge-gray { background: #f2f3f5; color: #4e5969; }
.badge-green { background: #e8f9ed; color: #00b42a; }
.badge-warning { background: #fff7e6; color: #d46b08; }
.badge-red { background: #ffece8; color: #f53f3f; }
.dot { width: 6px; height: 6px; border-radius: 50%; display: inline-block; }
.dot-gray { background: #c9cdd4; }
.dot-green { background: #00b42a; }
.dot-yellow { background: #faad14; }
.dot-red { background: #f53f3f; }

.action-btn { border: none; background: transparent; color: #165dff; cursor: pointer; padding: 4px 8px; border-radius: 8px; font-size: 13px; }
.action-btn:hover { background: #e8f1ff; }
.action-btn.danger { color: #f53f3f; }
.action-btn.danger:hover { background: #ffece8; }

.drawer-overlay { display: none; position: fixed; inset: 0; background: rgba(0,0,0,0.35); z-index: 999; backdrop-filter: blur(2px); }
.drawer-overlay.open { display: block; }
.drawer { position: fixed; right: 0; top: 0; width: 580px; max-width: 90vw; height: 100%; background: #fff; box-shadow: -8px 0 40px rgba(0,0,0,0.12); padding: 32px 28px; overflow-y: auto; z-index: 1000; transform: translateX(100%); transition: transform 0.3s cubic-bezier(0.23, 1, 0.32, 1); }
.drawer.open { transform: translateX(0); }
.drawer-close { float: right; border: none; background: none; font-size: 24px; cursor: pointer; color: #86909c; }
.drawer-title { font-size: 20px; font-weight: 600; margin-bottom: 20px; }
.drawer-field { margin-bottom: 20px; }
.drawer-field label { display: block; font-weight: 500; margin-bottom: 6px; font-size: 14px; color: #4e5969; }
.drawer-field input, .drawer-field select, .drawer-field textarea { width: 100%; padding: 10px 14px; border: 1px solid #e5e6eb; border-radius: 12px; font-size: 14px; outline: none; }
.drawer-field input:focus, .drawer-field select:focus, .drawer-field textarea:focus { border-color: #165dff; box-shadow: 0 0 0 3px rgba(22, 93, 255, 0.1); }
.drawer-footer { display: flex; gap: 12px; margin-top: 32px; border-top: 1px solid #f0f0f0; padding-top: 24px; }

.pagination { display: flex; justify-content: flex-end; align-items: center; gap: 12px; padding: 18px 0 0; font-size: 14px; color: #4e5969; }

.toast { position: fixed; top: 30px; left: 50%; transform: translateX(-50%); background: #1d2129; color: #fff; padding: 12px 28px; border-radius: 60px; font-size: 14px; z-index: 2000; opacity: 0; transition: opacity 0.25s; box-shadow: 0 8px 20px rgba(0,0,0,0.2); pointer-events: none; }
.toast.show { opacity: 1; }
</style>