<template>
  <div class="page">
    <div class="page-header">
      <button class="back-btn" @click="$router.push('/dashboard/menu')">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
        返回
      </button>
      <div>
        <h2>菜单管理 · Menu Management</h2>
        <p class="page-desc">右键增改删 · 四种菜单统一管理 · Right-click CRUD</p>
      </div>
      <button class="add-btn" @click="openAddDialog">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        新增菜品
      </button>
    </div>

    <!-- Tab 切换 -->
    <div class="tab-bar">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        :class="['tab-btn', { active: currentMenu === tab.value }]"
        @click="switchMenu(tab.value)"
      >
        {{ tab.label }}
        <span class="tab-count">{{ tabCounts[tab.value] || 0 }}</span>
      </button>
    </div>

    <!-- 分类筛选 -->
    <div class="filter-bar">
      <button
        :class="['filter-btn', { active: filterCat === '' }]"
        @click="filterCat = ''; applyFilter()"
      >
        全部 <span class="filter-count">{{ tableData.length }}</span>
      </button>
      <button
        v-for="cat in categories"
        :key="cat.id"
        :class="['filter-btn', { active: filterCat === cat.id }]"
        @click="filterCat = cat.id; applyFilter()"
      >
        {{ cat.name }} <span class="filter-count">{{ cat.count }}</span>
      </button>
    </div>

    <!-- 表格 -->
    <el-table
      :data="filteredData"
      stripe
      class="data-table"
      v-loading="loading"
      @row-contextmenu.prevent="showContextMenu"
      @row-click="selectedRow = $event"
      :row-class-name="({row}) => selectedRow && row.id === selectedRow.id ? 'selected-row' : ''"
    >
      <el-table-column prop="dishId" label="编号" width="100" />
      <el-table-column prop="dishName" label="菜品名称" min-width="180" />
      <el-table-column prop="menuCategoryName" label="菜单分类" width="130" />
      <el-table-column prop="dishCategory" label="厨房分类" width="100" />
      <el-table-column prop="salePrice" label="售价" width="80">
        <template #default="{ row }">¥{{ row.salePrice }}</template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="70" />
    </el-table>

    <!-- 右键菜单 -->
    <div v-if="contextMenu.visible" class="context-menu" :style="{ top: contextMenu.y + 'px', left: contextMenu.x + 'px' }">
      <div class="ctx-item" @click="openAddDialog">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        新增菜品
      </div>
      <div class="ctx-item" @click="openEditDialog" v-if="selectedRow">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
        编辑菜品
      </div>
      <div class="ctx-item danger" @click="deleteDish" v-if="selectedRow">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
        删除菜品
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'add' ? '新增菜品' : '编辑菜品'"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form :model="form" label-width="90px" size="small">
        <el-form-item label="菜品" v-if="dialogMode === 'add'">
          <el-select v-model="form.dishId" placeholder="选择菜品" filterable @change="onDishSelect" style="width:100%">
            <el-option
              v-for="d in allDishes"
              :key="d.dish_id"
              :label="`${d.dish_id} - ${d.dish_name}`"
              :value="d.dish_id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="菜品名称">
          <el-input v-model="form.dishName" />
        </el-form-item>
        <el-form-item label="菜单分类">
          <el-select v-model="form.menuCategoryId" placeholder="选择分类" style="width:100%" @change="onCatSelect">
            <el-option
              v-for="c in allCategories"
              :key="c.id"
              :label="c.category_name"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="厨房分类">
          <el-input v-model="form.dishCategory" />
        </el-form-item>
        <el-form-item label="售价">
          <el-input-number v-model="form.salePrice" :min="0" :step="1" style="width:100%" />
        </el-form-item>
        <el-form-item label="特殊价格">
          <el-input-number v-model="form.specialPrice" :min="0" :step="1" style="width:100%" placeholder="可选" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :step="1" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDish" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 点击空白关闭右键菜单 -->
    <div v-if="contextMenu.visible" class="ctx-overlay" @click="closeContextMenu"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'

const API_BASE = '/api'

const tabs = [
  { value: 'zero_point', label: '零点菜单' },
  { value: 'long_weekend', label: '小长假菜单' },
  { value: 'cny', label: '过年菜单' },
  { value: 'may_day', label: '五一菜单' },
]

const currentMenu = ref('zero_point')
const tabCounts = ref({})
const tableData = ref([])
const categories = ref([])
const filterCat = ref('')
const loading = ref(false)
const selectedRow = ref(null)
const allDishes = ref([])
const allCategories = ref([])

const contextMenu = ref({ visible: false, x: 0, y: 0 })
const dialogVisible = ref(false)
const dialogMode = ref('add')
const saving = ref(false)

const form = ref({
  dishId: '', dishName: '', dishCategory: '',
  menuCategoryId: null, menuCategoryName: '',
  salePrice: 0, specialPrice: null, sortOrder: 0
})

const filteredData = computed(() => {
  if (!filterCat.value) return tableData.value
  return tableData.value.filter(d => d.menu_category_id === filterCat.value)
})

async function switchMenu(type) {
  currentMenu.value = type
  filterCat.value = ''
  await loadData()
}

async function loadData() {
  loading.value = true
  try {
    const [listRes, catRes] = await Promise.all([
      fetch(`${API_BASE}/menu-api/list?menuType=${currentMenu.value}`).then(r => r.json()),
      fetch(`${API_BASE}/menu-api/categories?menuType=${currentMenu.value}`).then(r => r.json()),
    ])
    tableData.value = listRes.data || []
    categories.value = catRes.data || []

    // 更新 tab 计数
    for (const tab of tabs) {
      const res = await fetch(`${API_BASE}/menu-api/list?menuType=${tab.value}`).then(r => r.json())
      tabCounts.value[tab.value] = (res.data || []).length
    }
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

async function loadAllDishes() {
  const res = await fetch(`${API_BASE}/menu-api/all-dishes`).then(r => r.json())
  allDishes.value = res.data || []
}

async function loadAllCategories() {
  const res = await fetch(`${API_BASE}/menu-api/all-categories`).then(r => r.json())
  allCategories.value = res.data || []
}

function showContextMenu(row, column, event) {
  selectedRow.value = row
  contextMenu.value = { visible: true, x: event.clientX, y: event.clientY }
}

function closeContextMenu() {
  contextMenu.value.visible = false
}

function openAddDialog() {
  closeContextMenu()
  dialogMode.value = 'add'
  form.value = {
    dishId: '', dishName: '', dishCategory: '',
    menuCategoryId: null, menuCategoryName: '',
    salePrice: 0, specialPrice: null, sortOrder: 0
  }
  dialogVisible.value = true
}

function openEditDialog() {
  closeContextMenu()
  if (!selectedRow.value) return
  dialogMode.value = 'edit'
  form.value = {
    id: selectedRow.value.id,
    dishId: selectedRow.value.dish_id,
    dishName: selectedRow.value.dish_name,
    dishCategory: selectedRow.value.dish_category || '',
    menuCategoryId: selectedRow.value.menu_category_id,
    menuCategoryName: selectedRow.value.menu_category_name || '',
    salePrice: Number(selectedRow.value.sale_price) || 0,
    specialPrice: selectedRow.value.special_price ? Number(selectedRow.value.special_price) : null,
    sortOrder: selectedRow.value.sort_order || 0,
  }
  dialogVisible.value = true
}

function onDishSelect(id) {
  const d = allDishes.value.find(x => x.dish_id === id)
  if (d) {
    form.value.dishName = d.dish_name
    form.value.dishCategory = d.dish_category || ''
    form.value.salePrice = Number(d.sale_price) || 0
  }
}

function onCatSelect(id) {
  const c = allCategories.value.find(x => x.id === id)
  if (c) form.value.menuCategoryName = c.category_name
}

async function saveDish() {
  saving.value = true
  try {
    const tab = tabs.find(t => t.value === currentMenu.value)
    const body = {
      menuType: currentMenu.value,
      menuTypeName: tab.label,
      ...form.value,
    }
    if (dialogMode.value === 'add') {
      await fetch(`${API_BASE}/menu-api/dish`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
      })
    } else {
      await fetch(`${API_BASE}/menu-api/dish/${form.value.id}`, {
        method: 'PUT', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
      })
    }
    dialogVisible.value = false
    await loadData()
  } catch (e) { console.error(e) }
  finally { saving.value = false }
}

async function deleteDish() {
  closeContextMenu()
  if (!selectedRow.value) return
  if (!confirm(`确定删除「${selectedRow.value.dish_name}」？`)) return
  try {
    await fetch(`${API_BASE}/menu-api/dish/${selectedRow.value.id}`, { method: 'DELETE' })
    selectedRow.value = null
    await loadData()
  } catch (e) { console.error(e) }
}

function applyFilter() {
  // filteredData is computed, no action needed
}

onMounted(() => {
  loadData()
  loadAllDishes()
  loadAllCategories()
})
</script>

<style scoped>
.page-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.page-header h2 { font-size: 18px; font-weight: 600; margin: 0; }
.page-desc { font-size: 13px; color: var(--color-text-muted); margin: 2px 0 0; }
.back-btn {
  display: flex; align-items: center; gap: 4px;
  padding: 6px 14px; border: 1px solid var(--color-border);
  background: var(--color-card); color: var(--color-text);
  border-radius: 2px; cursor: pointer; font-size: 13px;
  transition: all 0.2s;
}
.back-btn:hover { background: var(--color-bg-alt); color: var(--color-primary); border-color: var(--color-accent); }

.tab-bar {
  display: flex; gap: 0; margin-bottom: 12px;
  border: 1px solid var(--color-border); border-radius: 2px;
  overflow: hidden; width: fit-content;
}
.tab-btn {
  padding: 8px 20px; border: none; background: var(--color-card);
  cursor: pointer; font-size: 13px; color: var(--color-text);
  transition: all 0.2s; display: flex; align-items: center; gap: 6px;
  border-right: 1px solid var(--color-border);
}
.tab-btn:last-child { border-right: none; }
.tab-btn:hover { background: rgba(196, 163, 90, 0.08); }
.tab-btn.active {
  background: rgba(196, 163, 90, 0.15); color: var(--color-accent);
  font-weight: 600; border-bottom: 2px solid var(--color-accent);
}
.tab-count {
  font-size: 11px; padding: 1px 6px; border-radius: 2px;
  background: var(--color-bg-alt); color: var(--color-text-muted);
}
.tab-btn.active .tab-count { background: rgba(196, 163, 90, 0.25); color: var(--color-accent); }

.filter-bar {
  display: flex; gap: 6px; margin-bottom: 12px; flex-wrap: wrap;
}
.filter-btn {
  padding: 5px 12px; border: 1px solid var(--color-border);
  border-radius: 2px; background: var(--color-card); cursor: pointer;
  font-size: 12px; color: var(--color-text); transition: all 0.2s;
  display: flex; align-items: center; gap: 4px;
}
.filter-btn:hover { border-color: var(--color-accent); }
.filter-btn.active {
  border-color: var(--color-accent); background: rgba(196, 163, 90, 0.12);
  color: var(--color-accent); font-weight: 600;
}
.filter-count { font-size: 10px; color: var(--color-text-muted); }

.data-table :deep(.selected-row) {
  background: rgba(196, 163, 90, 0.08) !important;
}
.data-table :deep(.selected-row td) {
  border-bottom: 1px solid var(--color-accent) !important;
}

.context-menu {
  position: fixed; z-index: 9999;
  background: var(--color-card); border: 1px solid var(--color-border);
  border-radius: 2px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  min-width: 140px; overflow: hidden;
}
.ctx-item {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 16px; cursor: pointer; font-size: 13px;
  color: var(--color-text); transition: background 0.15s;
}
.ctx-item:hover { background: rgba(196, 163, 90, 0.1); }
.ctx-item.danger { color: #c0392b; }
.ctx-item.danger:hover { background: rgba(192, 57, 43, 0.08); }

.ctx-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  z-index: 9998;
}
</style>
