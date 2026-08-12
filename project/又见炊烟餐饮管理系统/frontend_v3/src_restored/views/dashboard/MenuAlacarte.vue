<template>
  <div class="page" @click="closeContextMenu">
    <div class="page-header">
      <button class="back-btn" @click="$router.push('/dashboard/menu')">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
        返回
      </button>
      <div>
        <h2>零点菜单 · A La Carte Menu</h2>
        <p class="page-desc">日常零点菜品 · 分类浏览 · 右键增改删</p>
      </div>
    </div>

    <div class="layout-body">
      <!-- 左侧菜牌分类面板 -->
      <div class="category-panel">
        <div class="panel-title">
          <span>零点菜单分类</span>
          <span class="panel-title-en">Menu Card Categories</span>
        </div>
        <div class="version-select-wrap">
          <el-select v-model="menuVersion" placeholder="选择菜单版本" @change="onVersionChange" size="small" style="width:100%">
            <el-option label="跨年迎春零点菜单" value="cny" />
            <el-option label="零点全菜单" value="zero_point" />
            <el-option label="游客尊享零点菜单" value="tourist" />
            <el-option label="小长假零点菜单" value="long_weekend" />
            <el-option label="五一尊享零点菜单" value="may_day" />
          </el-select>
        </div>
        <div class="cat-list">
          <button
            :class="['cat-btn', { active: selectedCat === '' }]"
            @click="selectCategory('')"
          >
            <span class="cat-btn-name">全部菜品</span>
            <span class="cat-btn-count">{{ totalCount }}</span>
          </button>
          <button
            v-for="cat in dishCategories"
            :key="cat.name"
            :class="['cat-btn', { active: selectedCat === cat.name }]"
            @click="selectCategory(cat.name)"
          >
            <span class="cat-btn-name">{{ cat.name }}</span>
            <span class="cat-btn-count">{{ cat.count }}</span>
          </button>
        </div>
      </div>

      <!-- 右侧内容区 -->
      <div class="content-area">
        <div class="toolbar">
          <div class="toolbar-left">
            <el-input v-model="keyword" placeholder="搜索菜品" class="search-box" clearable @keyup.enter="applyFilters" @clear="applyFilters" />
          </div>
          <div class="toolbar-right">
            <el-button type="primary" size="small" @click="openAddDialog">+ 新增菜品</el-button>
            <span class="result-count">共 {{ total }} 道菜品</span>
          </div>
        </div>

        <el-table
          :data="list"
          stripe
          class="data-table no-select"
          v-loading="loading"
          :default-sort="{ prop: 'dishId', order: 'ascending' }"
          @row-contextmenu="onRowContextMenu"
          @row-dblclick="onRowDblclick"
          @contextmenu.prevent
          highlight-current-row
          @current-change="onCurrentChange"
        >
          <el-table-column prop="dishId" label="编号" width="100" sortable />
          <el-table-column prop="dishName" label="菜品名称" min-width="160" />
          <el-table-column prop="menuCategoryName" label="菜单分类" width="120" />
          <el-table-column prop="salePrice" label="售价" width="75" sortable>
            <template #default="{ row }">¥{{ row.salePrice }}</template>
          </el-table-column>
          <el-table-column prop="costPrice" label="成本" width="75" sortable>
            <template #default="{ row }">¥{{ row.costPrice }}</template>
          </el-table-column>
          <el-table-column prop="costRate" label="成本率" width="80" sortable>
            <template #default="{ row }">{{ row.costRate }}%</template>
          </el-table-column>
          <el-table-column prop="remark" label="特点" min-width="120" show-overflow-tooltip />
          <el-table-column prop="sortOrder" label="排序" width="65" sortable />
        </el-table>
      </div>
    </div>

    <!-- 右键菜单 -->
    <div
      v-if="contextMenu.visible"
      class="context-menu"
      :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
      @click.stop
    >
      <div class="ctx-item" @click="openEditDialog">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
        修改
      </div>
      <div class="ctx-item danger" @click="confirmDelete">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
        删除
      </div>
    </div>

    <!-- 新增/修改 弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'add' ? '新增菜品' : '修改菜品'"
      width="860px"
      :close-on-click-modal="false"
      class="dish-dialog"
    >
      <div class="dish-dialog-toolbar" v-if="dialogMode === 'edit'">
        <div class="toolbar-nav">
          <el-button size="default" @click="goPrev" :disabled="currentIndex <= 0">
            <span style="margin-right: 4px;">←</span>上一个
          </el-button>
          <span class="nav-indicator">{{ currentIndex + 1 }} / {{ list.length }}</span>
          <el-button size="default" @click="goNext" :disabled="currentIndex >= list.length - 1">
            下一个<span style="margin-left: 4px;">→</span>
          </el-button>
        </div>
        <div class="toolbar-actions">
          <el-button type="danger" size="default" @click="deleteFromDialog">删除菜品</el-button>
        </div>
      </div>
      <div class="dish-form-container">
        <div class="form-section">
          <div class="section-title">基本信息</div>
          <el-form :model="editForm" label-width="90px" size="default" class="dish-form">
            <div class="form-grid">
              <el-form-item label="菜品编码">
                <el-input v-model="editForm.dishCode" />
              </el-form-item>
              <el-form-item label="零点分类">
                <el-select v-model="editForm.categoryName" filterable allow-create style="width: 100%">
                  <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
                </el-select>
              </el-form-item>
              <el-form-item label="辣度">
                <el-select v-model="editForm.spicyLevel" style="width: 100%">
                  <el-option label="不辣" :value="0" />
                  <el-option label="微辣" :value="1" />
                  <el-option label="中辣" :value="2" />
                  <el-option label="香辣" :value="3" />
                  <el-option label="狠辣" :value="4" />
                </el-select>
              </el-form-item>
              <el-form-item label="菜肴名称">
                <el-input v-model="editForm.dishName" />
              </el-form-item>
              <el-form-item label="菜肴类别">
                <el-input v-model="editForm.dishCategory" />
              </el-form-item>
              <el-form-item label="单位">
                <el-select v-model="editForm.unit" style="width: 100%">
                  <el-option label="份" value="份" />
                  <el-option label="只" value="只" />
                  <el-option label="斤" value="斤" />
                  <el-option label="条" value="条" />
                  <el-option label="位" value="位" />
                  <el-option label="半只" value="半只" />
                  <el-option label="整只" value="整只" />
                </el-select>
              </el-form-item>
              <el-form-item label="售价">
                <el-input-number v-model="editForm.basePrice" :min="0" :precision="2" style="width: 100%" @change="calcCostRate" />
              </el-form-item>
              <el-form-item label="成本价">
                <el-input-number v-model="editForm.costPrice" :min="0" :precision="2" style="width: 100%" @change="calcCostRate" />
              </el-form-item>
              <el-form-item label="成本率">
                <div class="cost-rate-wrap">
                  <el-input-number v-model="editForm.costRate" :min="0" :max="100" :precision="2" style="flex: 1" />
                  <span class="percent-sign">%</span>
                </div>
              </el-form-item>
              <el-form-item label="特点" class="span-2">
                <el-input v-model="editForm.remark" placeholder="口味、规格描述" />
              </el-form-item>
              <el-form-item label="排序">
                <el-input-number v-model="editForm.sortOrder" :min="0" style="width: 100%" />
              </el-form-item>
            </div>
          </el-form>
        </div>

        <div class="form-section">
          <div class="section-title">原材料明细</div>
          <div class="materials-toolbar">
            <el-button type="primary" size="small" @click="addMaterial">+ 添加原材料</el-button>
          </div>
          <el-table :data="editForm.materials" size="small" border class="materials-table">
            <el-table-column prop="name" label="原材料名称" width="150">
              <template #default="{ row }">
                <el-input v-model="row.name" size="small" />
              </template>
            </el-table-column>
            <el-table-column prop="unit" label="单位" width="70">
              <template #default="{ row }">
                <el-select v-model="row.unit" size="small" style="width: 70px">
                  <el-option label="斤" value="斤" />
                  <el-option label="两" value="两" />
                  <el-option label="克" value="克" />
                  <el-option label="个" value="个" />
                  <el-option label="份" value="份" />
                  <el-option label="只" value="只" />
                  <el-option label="条" value="条" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column prop="price" label="单价" width="90">
              <template #default="{ row }">
                <el-input-number v-model="row.price" :min="0" :precision="2" size="small" style="width: 90px" @change="calcMaterialTotal" />
              </template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="90">
              <template #default="{ row }">
                <el-input-number v-model="row.quantity" :min="0" :precision="2" size="small" style="width: 90px" @change="calcMaterialTotal" />
              </template>
            </el-table-column>
            <el-table-column prop="amount" label="金额" width="90">
              <template #default="{ row }">¥{{ (row.price * row.quantity).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="60">
              <template #default="{ $index }">
                <el-button type="danger" size="small" @click="removeMaterial($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="materials-summary">
            <span>原材料总成本：</span>
            <span class="summary-value">¥{{ materialsTotal.toFixed(2) }}</span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button size="default" @click="dialogVisible = false">关闭窗体</el-button>
        <el-button type="primary" size="default" @click="saveDish" :loading="saving">保存记录</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const saving = ref(false)
const allDishes = ref([])
const list = ref([])
const total = ref(0)
const totalCount = ref(0)
const keyword = ref('')
const selectedCat = ref('')
const menuVersion = ref('zero_point')
const menuNameMap = {
  cny: '跨年迎春零点菜单',
  zero_point: '零点全菜单',
  tourist: '游客尊享零点菜单',
  long_weekend: '小长假零点菜单',
  may_day: '五一尊享零点菜单',
}
const dishCategories = ref([])
const currentRow = ref(null)
const currentIndex = ref(-1)

// 右键菜单
const contextMenu = ref({ visible: false, x: 0, y: 0 })

// 弹窗
const dialogVisible = ref(false)
const dialogMode = ref('add')
const allDishOptions = ref([])
const categoryOptions = ref([
  '本店招牌菜', '本地炖锅特色菜', '热菜-海鲜水产类', '热菜家常炒菜类',
  '生啫砂锅/煲仔类', '汤类/蒸菜类', '蔬菜类', '主食点心类', '凉菜小碟下酒类'
])
const editForm = ref({
  ldId: '',
  selectedDishId: '',
  dishName: '',
  dishCode: '',
  dishCategory: '',
  categoryName: '',
  priceStr: '',
  basePrice: 0,
  costPrice: 0,
  costRate: 0,
  remark: '',
  sortOrder: 0,
  unit: '份',
  spicyLevel: 0,
  materials: [],
})

const materialsTotal = computed(() => {
  return editForm.value.materials.reduce((sum, m) => sum + (m.price || 0) * (m.quantity || 0), 0)
})

function addMaterial() {
  editForm.value.materials.push({ name: '', unit: '斤', price: 0, quantity: 0 })
}

function removeMaterial(index) {
  editForm.value.materials.splice(index, 1)
}

function calcMaterialTotal() {
  const total = materialsTotal.value
  editForm.value.costPrice = Math.round(total * 100) / 100
  calcCostRate()
}

function selectCategory(val) {
  selectedCat.value = val
  applyFilters()
}

function onVersionChange() {
  selectedCat.value = ''
  keyword.value = ''
  fetchData()
}

function onCurrentChange(row) {
  currentRow.value = row
}

function onRowDblclick(row) {
  currentRow.value = row
  openEditDialog()
}

function onRowContextMenu(row, column, event) {
  event.preventDefault()
  currentRow.value = row
  contextMenu.value = {
    visible: true,
    x: event.clientX,
    y: event.clientY,
  }
}

function closeContextMenu() {
  contextMenu.value.visible = false
}

async function fetchData() {
  loading.value = true
  try {
    const [listRes, catRes] = await Promise.all([
      fetch(`/menu-api/ld-dishes?menuType=${menuVersion.value}`).then(r => r.json()),
      fetch(`/menu-api/ld-categories?menuType=${menuVersion.value}`).then(r => r.json()),
    ])

    allDishes.value = (listRes.data || []).map(d => ({
      ldId: d.ld_id,
      dishId: d.dish_code,
      dishName: d.dish_name,
      categoryName: d.category_name,
      dishCategory: d.dish_category || '',
      menuCategoryName: d.category_name,
      salePrice: Number(d.base_price) || 0,
      costPrice: Number(d.cost_price) || 0,
      costRate: Number(d.cost_rate) || 0,
      priceStr: d.price_str,
      remark: d.remark || '',
      sortOrder: d.sort_order || 0,
    }))
    totalCount.value = allDishes.value.length

    dishCategories.value = (catRes.data || []).map((c, i) => ({
      id: i + 1,
      name: c.name,
      count: c.count,
    }))

    applyFilters()
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

function applyFilters() {
  let filtered = allDishes.value

  if (selectedCat.value) {
    filtered = filtered.filter(d => d.categoryName === selectedCat.value)
  }

  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    filtered = filtered.filter(d => d.dishName.toLowerCase().includes(kw))
  }

  list.value = filtered
  total.value = filtered.length
}

// 新增
async function openAddDialog() {
  dialogMode.value = 'add'
  editForm.value = {
    ldId: '', selectedDishId: '', dishName: '', dishCode: '',
    dishCategory: '', categoryName: selectedCat.value || '',
    priceStr: '', basePrice: 0,
    costPrice: 0, costRate: 0, remark: '', sortOrder: 0,
    unit: '份', spicyLevel: 0, materials: [],
  }
  if (!allDishOptions.value.length) {
    try {
      const res = await fetch('/menu-api/ld-all-dishes').then(r => r.json())
      allDishOptions.value = res.data || []
    } catch (e) { console.error(e) }
  }
  dialogVisible.value = true
  closeContextMenu()
}

function onDishSelect(dishId) {
  const dish = allDishOptions.value.find(d => d.dish_id === dishId)
  if (dish) {
    editForm.value.dishName = dish.dish_name
    editForm.value.dishCode = dish.dish_id
  }
}

function calcCostRate() {
  if (editForm.value.basePrice > 0) {
    editForm.value.costRate = Math.round(editForm.value.costPrice / editForm.value.basePrice * 10000) / 100
  } else {
    editForm.value.costRate = 0
  }
}

// 修改
function openEditDialog() {
  if (!currentRow.value) return
  const idx = list.value.findIndex(d => d.ldId === currentRow.value.ldId)
  currentIndex.value = idx >= 0 ? idx : -1
  dialogMode.value = 'edit'
  editForm.value = {
    ldId: currentRow.value.ldId,
    selectedDishId: '',
    dishName: currentRow.value.dishName,
    dishCode: currentRow.value.dishId,
    dishCategory: currentRow.value.dishCategory || '',
    categoryName: currentRow.value.categoryName,
    priceStr: currentRow.value.priceStr || '',
    basePrice: currentRow.value.salePrice,
    costPrice: currentRow.value.costPrice,
    costRate: currentRow.value.costRate,
    remark: currentRow.value.remark || '',
    sortOrder: currentRow.value.sortOrder,
    unit: '份',
    spicyLevel: 0,
    materials: [],
  }
  dialogVisible.value = true
  closeContextMenu()
}

// 保存
async function saveDish() {
  if (!editForm.value.dishName) {
    ElMessage.warning('请选择菜品')
    return
  }
  if (!editForm.value.categoryName) {
    ElMessage.warning('请选择分类')
    return
  }
  saving.value = true
  try {
    if (dialogMode.value === 'add') {
      const res = await fetch('/menu-api/ld-dish', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          menuType: menuVersion.value,
          menuName: menuNameMap[menuVersion.value],
          dishCode: editForm.value.dishCode,
          dishName: editForm.value.dishName,
          categoryName: editForm.value.categoryName,
          priceStr: editForm.value.priceStr,
          basePrice: editForm.value.basePrice,
          costPrice: editForm.value.costPrice,
          costRate: editForm.value.costRate,
          remark: editForm.value.remark,
          sortOrder: editForm.value.sortOrder,
        }),
      }).then(r => r.json())
      if (res.code === 200) {
        ElMessage.success('新增成功')
        dialogVisible.value = false
        fetchData()
      } else {
        ElMessage.error(res.message || '新增失败')
      }
    } else {
      const res = await fetch(`/menu-api/ld-dish/${editForm.value.ldId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          dishName: editForm.value.dishName,
          categoryName: editForm.value.categoryName,
          priceStr: editForm.value.priceStr,
          basePrice: editForm.value.basePrice,
          costPrice: editForm.value.costPrice,
          costRate: editForm.value.costRate,
          remark: editForm.value.remark,
          sortOrder: editForm.value.sortOrder,
        }),
      }).then(r => r.json())
      if (res.code === 200) {
        ElMessage.success('修改成功')
        dialogVisible.value = false
        fetchData()
      } else {
        ElMessage.error(res.message || '修改失败')
      }
    }
  } catch (e) {
    ElMessage.error('请求失败: ' + e.message)
  } finally { saving.value = false }
}

// 上一个
function goPrev() {
  if (currentIndex.value <= 0) return
  currentIndex.value--
  const row = list.value[currentIndex.value]
  if (row) {
    currentRow.value = row
    editForm.value = {
      ldId: row.ldId,
      selectedDishId: '',
      dishName: row.dishName,
      dishCode: row.dishId,
      categoryName: row.categoryName,
      priceStr: row.priceStr || '',
      basePrice: row.salePrice,
      costPrice: row.costPrice,
      costRate: row.costRate,
      remark: row.remark || '',
      sortOrder: row.sortOrder,
      unit: '份',
      spicyLevel: 0,
      materials: [],
    }
  }
}

// 下一个
function goNext() {
  if (currentIndex.value >= list.value.length - 1) return
  currentIndex.value++
  const row = list.value[currentIndex.value]
  if (row) {
    currentRow.value = row
    editForm.value = {
      ldId: row.ldId,
      selectedDishId: '',
      dishName: row.dishName,
      dishCode: row.dishId,
      categoryName: row.categoryName,
      priceStr: row.priceStr || '',
      basePrice: row.salePrice,
      costPrice: row.costPrice,
      costRate: row.costRate,
      remark: row.remark || '',
      sortOrder: row.sortOrder,
      unit: '份',
      spicyLevel: 0,
      materials: [],
    }
  }
}

// 弹窗内删除
async function deleteFromDialog() {
  if (!editForm.value.ldId) return
  try {
    await ElMessageBox.confirm(
      `确定删除「${editForm.value.dishName}」吗？`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    const res = await fetch(`/menu-api/ld-dish/${editForm.value.ldId}`, {
      method: 'DELETE',
    }).then(r => r.json())
    if (res.code === 200) {
      ElMessage.success('删除成功')
      dialogVisible.value = false
      fetchData()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (e) {
    // 取消删除
  }
}

// 删除
async function confirmDelete() {
  if (!currentRow.value) return
  closeContextMenu()
  try {
    await ElMessageBox.confirm(
      `确定删除「${currentRow.value.dishName}」吗？`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    const res = await fetch(`/menu-api/ld-dish/${currentRow.value.ldId}`, {
      method: 'DELETE',
    }).then(r => r.json())
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchData()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (e) {
    // 取消删除
  }
}

onMounted(fetchData)
onUnmounted(closeContextMenu)
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

.layout-body {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.category-panel {
  width: 220px;
  flex-shrink: 0;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 2px;
  padding: 0;
  position: sticky;
  top: 16px;
}

.panel-title {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 14px 16px 10px;
  border-bottom: 1px solid var(--color-border);
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
  letter-spacing: 1px;
}

.panel-title-en {
  font-size: 10px;
  font-weight: 400;
  color: var(--color-text-muted);
  letter-spacing: 0.5px;
  margin-top: 2px;
}

.version-select-wrap {
  padding: 10px 12px;
  border-bottom: 1px solid var(--color-border);
}

.cat-list {
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.cat-btn {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 9px 12px;
  border: 1px solid transparent;
  border-radius: 2px;
  background: transparent;
  cursor: pointer;
  font-size: 13px;
  color: var(--color-text);
  transition: all 0.2s;
  text-align: left;
}

.cat-btn:hover {
  background: rgba(196, 163, 90, 0.08);
  border-color: rgba(196, 163, 90, 0.3);
}

.cat-btn.active {
  background: rgba(196, 163, 90, 0.12);
  border: 1px solid var(--color-accent);
  color: var(--color-accent);
  font-weight: 600;
  box-shadow: 0 1px 4px rgba(196, 163, 90, 0.15);
}

.cat-btn-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cat-btn-count {
  flex-shrink: 0;
  margin-left: 8px;
  font-size: 11px;
  color: var(--color-text-muted);
  background: var(--color-bg-alt);
  padding: 1px 7px;
  border-radius: 2px;
  min-width: 24px;
  text-align: center;
}

.cat-btn.active .cat-btn-count {
  background: rgba(196, 163, 90, 0.2);
  color: var(--color-accent);
}

.content-area {
  flex: 1;
  min-width: 0;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.toolbar-left { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.search-box { width: 200px; }

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.result-count {
  font-size: 13px;
  color: var(--color-text-muted);
}

/* 右键菜单 */
.context-menu {
  position: fixed;
  z-index: 9999;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 4px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  min-width: 120px;
  padding: 4px 0;
}

.ctx-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  cursor: pointer;
  font-size: 13px;
  color: var(--color-text);
  transition: background 0.15s;
}

.ctx-item:hover {
  background: rgba(196, 163, 90, 0.1);
}

.ctx-item.danger {
  color: #f56c6c;
}

.ctx-item.danger:hover {
  background: rgba(245, 108, 108, 0.1);
}

.no-select {
  user-select: none;
  -webkit-user-select: none;
}

.dish-form-container {
  padding: 8px 0;
}

.form-section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-accent);
  padding: 8px 14px;
  background: linear-gradient(90deg, rgba(196, 163, 90, 0.12), rgba(196, 163, 90, 0.02));
  border-left: 3px solid var(--color-accent);
  margin-bottom: 16px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px 20px;
}

.form-grid .el-form-item {
  margin-bottom: 0;
}

.form-grid .el-form-item.span-2 {
  grid-column: span 2;
}

.cost-rate-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
}

.cost-rate-wrap .el-input-number {
  flex: 1;
}

.percent-sign {
  color: #666;
  font-size: 14px;
  font-weight: 500;
}

.materials-toolbar {
  margin-bottom: 10px;
}

.materials-table {
  margin-bottom: 12px;
  width: 100%;
}

.materials-summary {
  text-align: right;
  font-size: 14px;
  font-weight: 600;
  padding: 8px 12px;
  background: rgba(196, 163, 90, 0.08);
  border-radius: 2px;
}

.summary-value {
  color: var(--color-accent);
  font-size: 16px;
  margin-left: 8px;
}

.dish-dialog :deep(.el-dialog__body) {
  padding-top: 10px;
}

.dish-dialog-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  margin-bottom: 16px;
  background: linear-gradient(135deg, rgba(196, 163, 90, 0.08), rgba(45, 74, 62, 0.04));
  border: 1px solid rgba(196, 163, 90, 0.2);
  border-radius: 2px;
}

.toolbar-nav {
  display: flex;
  align-items: center;
  gap: 12px;
}

.nav-indicator {
  font-size: 14px;
  color: #666;
  font-weight: 500;
  min-width: 70px;
  text-align: center;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
}
</style>
