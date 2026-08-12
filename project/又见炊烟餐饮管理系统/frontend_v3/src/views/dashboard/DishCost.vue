<template>
  <div class="cost-page">
    <div class="sub-header">
      <div>
        <h2>菜品成本管理 · Dish Cost Management</h2>
        <p class="page-desc">菜品成本卡查询 · Cost Card</p>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card" v-for="s in stats" :key="s.label" :style="{ color: s.color }">
        <div class="stat-content">
          <div class="stat-label">{{ s.label }}</div>
          <div class="stat-value">{{ s.value }}</div>
        </div>
      </div>
    </div>

    <div class="cost-toolbar">
      <el-button type="primary" @click="openNew">成本录入</el-button>
      <el-select v-model="catFilter" placeholder="全部分类 · Category" clearable size="small" style="width:160px">
        <el-option v-for="c in dishCategories" :key="c" :label="c" :value="c" />
      </el-select>
      <el-input v-model="search" placeholder="搜索菜品名称" size="small" clearable style="width:240px" />
    </div>
    <el-table :data="filteredDishes" stripe size="small" max-height="calc(100vh - 280px)" row-key="dishId"
      @row-dblclick="openEdit" @row-click="onRowClick" :row-class-name="rowClassName" @row-contextmenu.prevent="onContextMenu">
      <el-table-column prop="dishName" label="菜品名 · Name" min-width="150" />
      <el-table-column prop="dishCategory" label="分类 · Category" width="110" />
      <el-table-column label="售价 · Price" width="100" align="right">
        <template #default="{ row }">¥{{ (row.salePrice||0).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="成本 · Cost" width="100" align="right">
        <template #default="{ row }">¥{{ (row.costPrice||0).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="毛利率 · Margin" width="110" align="center">
        <template #default="{ row }">
          <span :style="{ color: marginColor(grossMargin(row)) }">{{ grossMargin(row).toFixed(1) }}%</span>
        </template>
      </el-table-column>
      <el-table-column label="状态 · Status" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="(row.costPrice||0)>0?'success':'info'" size="small">{{ (row.costPrice||0)>0?'已配':'未配' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作 · Actions" width="100" align="center">
        <template #default="{ row }">
          <el-button link size="small" type="primary" @click="openEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div v-if="ctxVisible" class="ctx-menu" :style="{ left: ctxX+'px', top: ctxY+'px' }">
      <div class="ctx-item" @click="openEdit(ctxRow)">编辑成本卡</div>
      <div class="ctx-item ctx-danger" @click="confirmDelete(ctxRow)">删除成本卡</div>
    </div>

    <!-- ========== 配料表弹窗 ========== -->
    <el-dialog v-model="showDlg" width="1100px" destroy-on-close @opened="onDlgOpened">
      <template #header>
        <div class="excel-hd">
          <span class="excel-label-top">又见炊烟私房菜菜单信息录入系统</span>
          <span style="flex:1" />
          <el-button size="small" @click="openNew">新菜肴</el-button>
        </div>
        <div class="excel-sub"><span class="excel-title">餐饮标准配料表</span></div>
      </template>

      <!-- 编码+份数一行 -->
      <div style="display:flex;align-items:flex-end;gap:12px;margin-bottom:8px">
        <div style="width:140px">
          <span style="font-size:10px;color:#6b7280;display:block">RECIPE NO. / 编号</span>
          <el-input :model-value="editDish.dishId" disabled />
        </div>
        <div style="width:100px">
          <span style="font-size:10px;color:#6b7280;display:block">SERVINGS / 份数</span>
          <el-input-number ref="f9" v-model="editDish.servings" :min="1" :max="99" @keyup.enter="focusRef('f0')" />
        </div>
      </div>

            <div style="display:flex;gap:8px">
        <div class="six-col-grid" style="flex:1">
        <div class="sc-row">
          <div class="sc-item">
            <span class="sc-label">NAME OF DISH / 菜名</span>
            <el-input ref="f0" v-model="editDish.dishName" size="small" @keyup.enter="focusRef('f7')" />
          </div>
          <div class="sc-item">
            <span class="sc-label">MAIN INGREDIENT / 主料</span>
            <el-input ref="f7" v-model="editDish.mainIngredient" size="small" @keyup.enter="focusRef('f8')" />
          </div>
          <div class="sc-item">
            <span class="sc-label">SALES PRICE / 售价</span>
            <el-input-number ref="f1" v-model="editDish.salePrice" :min="0" :precision="0" controls-position="right" size="small" @keyup.enter="focusRef('f3')" />
          </div>
        </div>
        <div class="sc-row">
          <div class="sc-item">
            <span class="sc-label">DISH CATEGORY / 菜肴类别</span>
            <el-select ref="f3" v-model="editDish.dishCategory" size="small" @keyup.enter="focusRef('f9')"><el-option v-for="c in dishCategories" :key="c" :label="c" :value="c" /></el-select>
          </div>
          <div class="sc-item">
            <span class="sc-label">INGREDIENT TYPE / 主料类别</span>
            <el-select ref="f8" v-model="editDish.mainIngredientType" size="small" filterable allow-create @keyup.enter="focusRef('f5')"><el-option v-for="t in ingredientTypes" :key="t" :label="t" :value="t" /></el-select>
          </div>
          <div class="sc-item">
            <span class="sc-label">UNIT COST / 成本价</span>
            <el-input :model-value="'¥'+unitCost.toFixed(2)" size="small" disabled />
          </div>
        </div>
        <div class="sc-row">
          <div class="sc-item">
            <span class="sc-label">SPICY / 辣度</span>
            <span class="spicy-icons" tabindex="0" @keyup.enter="focusRef('f5')"><span v-for="i in 5" :key="i" class="spicy-pepper" :class="{ active: i <= editDish.spicyLevel }" @click="editDish.spicyLevel = editDish.spicyLevel === i ? i-1 : i"></span></span>
          </div>
          <div class="sc-item">
            <span class="sc-label">FESTIVE NAME / 喜庆名称</span>
            <el-input ref="f4" v-model="editDish.festiveName" size="small" @keyup.enter="focusRef('f6')" />
          </div>
          <div class="sc-item">
            <span class="sc-label">COST % / 成本比率</span>
            <el-input :model-value="curCostRate.toFixed(1)+'%'" size="small" disabled />
          </div>
        </div>
        <div class="sc-row">
          <div class="sc-item">
            <span class="sc-label">COOKING TIME / 出菜时长</span>
            <el-input-number ref="f5" v-model="editDish.cookingTime" :min="1" :max="120" controls-position="right" size="small" @keyup.enter="focusRef('f6')" />
          </div>
          <div class="sc-item">
            <span class="sc-label">ENGLISH NAME / 英文名称</span>
            <el-input ref="f6" v-model="editDish.englishName" size="small" @keyup.enter="focusFirstIng" />
          </div>
          <div class="sc-item"></div>
        </div>
      </div>

        <div class="img-block">
          <span class="sc-label">菜品图片</span>
          <div v-if="editDish.imageUrl" class="img-box" @click="triggerUpload" title="点击更换">
            <img :src="editDish.imageUrl" @error="e=>e.target.style.display='none'" />
            <div class="img-box-overlay"><span @click.stop="triggerUpload">换图</span><span class="del" @click.stop="removeImage">删除</span></div>
          </div>
          <div v-else class="img-box img-box-empty" @click="triggerUpload">点击上传</div>
          <input ref="fileInput" type="file" accept="image/*" style="display:none" @change="onFilePicked" />
        </div>
      </div>

      <!-- 原料表格 -->
<table class="ing-tbl">
        <colgroup>
          <col style="width:32px"><col style="width:auto">
          <col style="width:50px"><col style="width:80px">
          <col style="width:70px"><col style="width:90px">
          <col style="width:80px"><col style="width:90px"><col style="width:28px">
        </colgroup>
        <thead><tr>
          <th>序号</th>
          <th>Raw Material Name / 原料名称</th>
          <th>Unit / 单位</th>
          <th>Price / 价格</th>
          <th>Quantity / 数量</th>
          <th>Yield / 出成率</th>
          <th>Total Cost / 共计</th>
          <th>Last Entry / 最近录入</th>
          <th></th>
        </tr></thead>
        <tbody>
          <tr v-for="(row, idx) in items" :key="idx">
            <td class="td-center">{{ idx + 1 }}</td>
            <td><el-autocomplete v-model="row.ingredientName" value-key="ingredientName" :fetch-suggestions="(q, cb) => searchIng(q, cb)" placeholder="原料/拼音" trigger-on-focus @select="(item) => onIngSelect(idx, item)" @keyup.enter="onIngEnter(idx)">
              <template #default="{ item }">
                <div class="ing-dd" v-if="item._new">+ 新增原料：{{ item.ingredientName }}</div>
                <div class="ing-dd" v-else>
                  <span class="ing-dd-name">{{ item.ingredientName }}</span>
                  <span class="ing-dd-meta">最低价 ¥{{ (item.minPrice || item.avgPrice || 0).toFixed(2) }} / {{ item.purchaseUnit || item.unit || '-' }}</span>
                  <span class="ing-dd-date">录入 {{ formatDate(item.lastEntryDate) || formatDate(item.updatedAt) || '-' }}</span>
                </div>
              </template>
            </el-autocomplete></td>
            <td class="td-center">{{ row.unit || '-' }}</td>
            <td class="td-right">¥{{ (row.unitPrice || 0).toFixed(4) }}</td>
            <td><el-input v-model.number="row.quantity" @input="calcRow(row)" @keyup.enter="onIngFieldEnter(idx,'qty')" /></td>
            <td><div style="display:flex;gap:2px;align-items:center"><el-input v-model.number="row.yieldRate" style="width:56px" @input="calcRow(row)" @keyup.enter="onIngFieldEnter(idx,'yield')" /><el-button link size="small" @click="openYieldForm(idx,row)">录入</el-button></div></td>
            <td class="td-right td-bold">¥{{ (row.totalCost || 0).toFixed(2) }}</td>
            <td class="td-center">{{ row.lastEntryDate || '-' }}</td>
            <td class="td-center"><el-button link size="small" type="danger" @click="items.splice(idx,1)">删除</el-button></td>
          </tr>
        </tbody>
      </table>

      <template #footer>
        <div style="display:flex;justify-content:space-between;align-items:center;width:100%">
          <div>
            <el-button size="small" :disabled="!hasPrev" @click="navPrev">上一条</el-button>
            <el-button size="small" :disabled="!hasNext" @click="navNext">下一条</el-button>
            <span style="font-size:12px;color:#9ca3af;margin-left:8px">
              {{ dishNavIdx+1 }} / {{ filteredDishes.length }}
            </span>
          </div>
          <div>
            <el-button @click="showDlg=false">取消</el-button>
            <el-button type="primary" @click="doSave">保存</el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- 新增原料 -->
    <el-dialog v-model="showNewIng" title="新增原料" width="420px">
      <el-form :model="newIng" label-width="80px" size="small">
        <el-form-item label="原料名称" required><el-input v-model="newIng.ingredientName" /></el-form-item>
        <el-form-item label="采购单位"><el-input v-model="newIng.purchaseUnit" /></el-form-item>
        <el-form-item label="单价"><el-input-number v-model="newIng.avgPrice" :min="0" :precision="4" style="width:100%" /></el-form-item>
        <el-form-item label="分类"><el-input v-model="newIng.ingredientCategory" /></el-form-item>
        <el-form-item label="出成率(%)"><el-input-number v-model="newIng.yieldRate" :min="0" :max="100" :precision="1" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="showNewIng=false">取消</el-button><el-button type="primary" @click="doCreateIng">创建</el-button></template>
    </el-dialog>

    <!-- 出成率录入 -->
    <el-dialog v-model="showYieldDlg" title="录入出成率" width="360px">
      <div style="margin-bottom:12px;font-size:13px;color:#6b7280">原料：<strong>{{ yieldForm.ingredientName }}</strong></div>
      <el-form size="small" label-width="100px"><el-form-item label="出成率 (%)"><el-input-number v-model="yieldForm.yieldRate" :min="0" :max="100" :precision="1" style="width:100%" /></el-form-item></el-form>
      <template #footer><el-button @click="showYieldDlg=false">取消</el-button><el-button type="primary" @click="doSaveYield">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, reactive, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDishes, getRecipe, saveRecipe, getIngredients, createDish, updateDish, uploadImage, deleteImage } from '@/api/booking'
import { pinyin } from 'pinyin-pro'

const dishes = ref([])
const search = ref('')
const catFilter = ref('')
const showDlg = ref(false)
const editDish = ref({})
const items = ref([])
const allIngredients = ref([])
const fileInput = ref(null)

const ctxVisible = ref(false)
const curRow = ref(null)
const ctxX = ref(0); const ctxY = ref(0); const ctxRow = ref(null)

const showNewIng = ref(false)
const newIng = reactive({ ingredientName: '', purchaseUnit: '斤', avgPrice: 0, ingredientCategory: '', yieldRate: 0 })
let newIngForIdx = -1

const showYieldDlg = ref(false)
const yieldForm = reactive({ idx: -1, ingredientName: '', yieldRate: 0 })

const dishCategories = ['凉菜刺身','尊享珍馔','干锅煲仔','水产海鲜','滋补汤羹','热菜小炒','美味汤羹']
const ingredientTypes = ['海鲜','肉类','禽类','拼盘','蔬菜','干货','调料','其他']

const filteredDishes = computed(() => {
  let arr = dishes.value
  if (catFilter.value) arr = arr.filter(d => d.dishCategory === catFilter.value)
  const q = search.value.trim().toLowerCase()
  if (!q) return arr
  return arr.filter(d => {
    const nm = (d.dishName || '').toLowerCase()
    const cid = (d.dishId || '').toLowerCase()
    const py = pinyin(d.dishName || '', { toneType: 'none', type: 'array' }).join('').toLowerCase()
    const pyf = pinyin(d.dishName || '', { toneType: 'none', pattern: 'first' }).replace(/\s/g, '').toLowerCase()
    return nm.includes(q) || cid.includes(q) || py.includes(q) || pyf.includes(q)
  })
})
const totalCost = computed(() => items.value.reduce((s, r) => s + (r.totalCost || 0), 0))
// 成本价 = 原料总成本 / 份数
const unitCost = computed(() => (editDish.value.servings || 1) > 0 ? totalCost.value / editDish.value.servings : totalCost.value)
const curCostRate = computed(() => (editDish.value.salePrice || 0) > 0 ? unitCost.value / editDish.value.salePrice * 100 : 0)

// 列表统计与毛利率
const stats = computed(() => {
  const total = dishes.value.length
  const costed = dishes.value.filter(d => (d.costPrice || 0) > 0)
  const avgMargin = costed.length ? costed.reduce((s, d) => s + grossMargin(d), 0) / costed.length : 0
  return [
    { label: '菜品总数 · Total', value: total, color: '#2D4A3E' },
    { label: '已配成本 · Costed', value: costed.length, color: '#4A7C59' },
    { label: '未配成本 · Uncosted', value: total - costed.length, color: '#C25555' },
    { label: '平均毛利率 · Avg Margin', value: avgMargin.toFixed(1) + '%', color: '#C4A35A' },
  ]
})
function grossMargin(row) {
  const sp = row.salePrice || 0
  return sp > 0 ? ((sp - (row.costPrice || 0)) / sp) * 100 : 0
}
function marginColor(m) {
  if (m >= 60) return '#4A7C59'
  if (m >= 40) return '#D4A853'
  return '#C25555'
}
function onRowClick(row) { curRow.value = row }

// 上一条/下一条导航
const dishNavIdx = ref(-1)
const hasPrev = computed(() => dishNavIdx.value > 0)
const hasNext = computed(() => dishNavIdx.value < filteredDishes.value.length - 1)
function navPrev() { if (hasPrev.value) dishNavIdx.value--; loadNavDish() }
function navNext() { if (hasNext.value) dishNavIdx.value++; loadNavDish() }
async function loadNavDish() {
  const row = filteredDishes.value[dishNavIdx.value]
  if (!row) return
  await openEdit(row)
}

function initItem() {
  return { ingredientName: '', ingredientId: '', quantity: 0, unit: '', unitPrice: 0, wastageRate: 0, yieldRate: 0, netUnitPrice: 0, totalCost: 0, lastEntryDate: '' }
}

function openNew() {
  dishNavIdx.value = -1
  editDish.value = { dishId: 'NEW_' + Date.now(), dishName: '', dishCategory: '', spicyLevel: 0, mainIngredientType: '', mainIngredient: '', englishName: '', salePrice: 0, cookingTime: 20, festiveName: '', imageUrl: '', servings: 1 }
  items.value = [initItem()]
  showDlg.value = true
}

function onDlgOpened() {
  nextTick(() => focusRef('f0'))
}
function focusRef(name) {
  nextTick(() => {
    const r = $refs?.[name]; if (!r) return
    const el = r.$el?.querySelector('input') || r.input || r
    if (el) el.focus()
  })
}

async function openEdit(row) {
  if (!row) row = ctxRow.value; if (!row) return
  ctxVisible.value = false
  editDish.value = { ...row }
  // 强制重新打开弹窗，避免跟新建模式的旧状态冲突
  if (showDlg.value) { showDlg.value = false; await nextTick() }
  // 计算在 filteredDishes 中的索引
  dishNavIdx.value = filteredDishes.value.findIndex(d => d.dishId === row.dishId)
  try {
    const res = await getRecipe(row.dishId)
    if (res.code === 200 && res.data?.length) {
      items.value = res.data.map(r => ({
        ingredientId: r.ingredientId, ingredientName: r.ingredientName || r.ingredientId || '',
        quantity: r.quantity, unit: r.unit || '',
        unitPrice: r.unitPrice || 0, wastageRate: r.wastageRate || 0,
        yieldRate: r.yieldRate || 0, netUnitPrice: r.netUnitPrice || 0, totalCost: r.totalCost || 0,
        lastEntryDate: r.lastEntryDate || ''
      }))
    } else items.value = [initItem()]
  } catch { items.value = [initItem()] }
  showDlg.value = true
}

function focusFirstIng() { nextTick(() => { const i = document.querySelector('.itr-name input'); if (i) i.focus() }) }
function onContextMenu(row, col, evt) { ctxRow.value = row; ctxX.value = evt.clientX; ctxY.value = evt.clientY; ctxVisible.value = true }
function hideCtx() { ctxVisible.value = false }
function rowClassName({ row }) { return curRow.value?.dishId === row.dishId ? 'row-selected' : '' }

async function confirmDelete(row) {
  ctxVisible.value = false
  try { await ElMessageBox.confirm(`删除「${row.dishName}」成本卡？`, '确认', { type: 'warning' }); await saveRecipe(row.dishId, []); ElMessage.success('已删除'); await fetchData() } catch {}
}

function searchIng(query, cb) {
  if (!query) { cb(allIngredients.value.map(i => ({ value: i.ingredientName, ...i })).slice(0, 20)); return }
  const q = query.toLowerCase()
  const res = allIngredients.value.filter(i => {
    const nm = (i.ingredientName || '').toLowerCase()
    const py = pinyin(i.ingredientName || '', { toneType: 'none', type: 'array' }).join('')
    const pyf = pinyin(i.ingredientName || '', { toneType: 'none', pattern: 'first' }).replace(/\s/g, '')
    return nm.includes(q) || i.ingredientId?.toLowerCase().includes(q) || py.includes(q) || pyf.includes(q)
  }).slice(0, 15)
  if (res.length === 0) res.push({ ingredientName: query, ingredientId: '', unit: '', avgPrice: 0, yieldRate: 0, _new: true })
  cb(res.map(i => ({
    ingredientName: i.ingredientName, ingredientId: i.ingredientId || '',
    purchaseUnit: i.purchaseUnit || i.unit || '', unit: i.purchaseUnit || i.unit || '',
    avgPrice: i.avgPrice || 0, minPrice: i.minPrice || 0,
    yieldRate: i.yieldRate || 0,
    lastEntryDate: i.lastEntryDate || '', updatedAt: i.updatedAt || '',
    _new: i._new || false, value: i.ingredientName
  })))
}

function today() { return new Date().toISOString().split('T')[0] }
function formatDate(d) { if (!d) return ''; const s = String(d); return s.includes('T') ? s.split('T')[0] : s }

function onIngSelect(idx, item) {
  if (item._new) {
    newIng.ingredientName = item.ingredientName; newIng.purchaseUnit = '斤'; newIng.avgPrice = 0; newIng.ingredientCategory = ''; newIng.yieldRate = 0
    newIngForIdx = idx; showNewIng.value = true; return
  }
  items.value[idx].ingredientId = item.ingredientId; items.value[idx].ingredientName = item.ingredientName
  items.value[idx].unit = item.purchaseUnit || item.unit || ''; items.value[idx].unitPrice = item.avgPrice || 0
  items.value[idx].yieldRate = item.yieldRate || 0; items.value[idx].lastEntryDate = today()
  calcRow(items.value[idx])
  if (!items.value[idx + 1]) addRow()
  nextTick(() => focusIngName(idx + 1))
}

function onIngEnter(idx) {
  nextTick(() => { const qs = document.querySelectorAll('.ing-tbl tbody tr'); if (qs[idx]) { const inp = qs[idx].querySelectorAll('input')[1]; if (inp) inp.focus() } })
}
function onIngFieldEnter(idx, field) {
  const trs = document.querySelectorAll('.ing-tbl tbody tr');
  if (field === 'qty') { nextTick(() => { const inp = trs[idx]?.querySelectorAll('input')[2]; if (inp) inp.focus() }) }
  else if (field === 'yield') { if (!items.value[idx+1]) addRow(); nextTick(() => { const inp = trs[idx+1]?.querySelector('input'); if (inp) inp.focus() }) }
}
function focusIngName(idx) { const inp = document.querySelectorAll('.ing-tbl tbody tr')[idx]?.querySelector('input'); if (inp) inp.focus() }

function openYieldForm(idx, row) { yieldForm.idx = idx; yieldForm.ingredientName = row.ingredientName || '(未选)'; yieldForm.yieldRate = row.yieldRate || 0; showYieldDlg.value = true }
function doSaveYield() { if (yieldForm.idx >= 0 && items.value[yieldForm.idx]) { items.value[yieldForm.idx].yieldRate = yieldForm.yieldRate; calcRow(items.value[yieldForm.idx]) } showYieldDlg.value = false }

async function doCreateIng() {
  const { createIngredient } = await import('@/api/booking')
  try {
    const res = await createIngredient({ ingredient_name: newIng.ingredientName, purchase_unit: newIng.purchaseUnit, avg_price: newIng.avgPrice, ingredient_category: newIng.ingredientCategory, yield_rate: newIng.yieldRate })
    if (res.code === 200) {
      ElMessage.success('原料已添加'); showNewIng.value = false
      if (newIngForIdx >= 0) {
        const item = res.data; const idx = newIngForIdx
        items.value[idx].ingredientId = item.ingredientId; items.value[idx].ingredientName = item.ingredientName
        items.value[idx].unit = item.purchaseUnit || ''; items.value[idx].unitPrice = item.avgPrice || 0
        items.value[idx].yieldRate = item.yieldRate || 0
        calcRow(items.value[idx]); if (!items.value[idx+1]) addRow(); nextTick(() => focusIngName(idx+1))
      }
      await fetchAllIngredients()
    }
  } catch { ElMessage.error('创建失败') }
}

function calcRow(row) { const r = 1 - (row.wastageRate || 0) / 100; row.netUnitPrice = r > 0 ? (row.unitPrice || 0) / r : 0; row.totalCost = Math.round((row.quantity || 0) * row.netUnitPrice * 100) / 100 }
function addRow() { items.value.push(initItem()) }

function triggerUpload() { fileInput.value?.click() }
async function onFilePicked(e) {
  const file = e.target.files?.[0]; if (!file) return
  try { ElMessage.info('上传中...'); const res = await uploadImage(file); if (res.code===200) { editDish.value.imageUrl=res.data?.url||''; ElMessage.success('已上传') } else ElMessage.error(res.message) } catch { ElMessage.error('上传失败') }
  e.target.value=''
}
async function removeImage() { const u = editDish.value.imageUrl; if (!u) return; try { await deleteImage(u.split('/').pop()) } catch {}; editDish.value.imageUrl=''; ElMessage.success('已删除') }

async function doSave() {
  if (!editDish.value.dishId) { ElMessage.warning('请先选择菜品'); return }
  try {
    const isNew = String(editDish.value.dishId).startsWith('NEW_'); let dishId = editDish.value.dishId
    if (isNew) {
      const res = await createDish({ dish_name: editDish.value.dishName, dish_category: editDish.value.dishCategory, spicy_level: editDish.value.spicyLevel||0, main_ingredient_type: editDish.value.mainIngredientType, main_ingredient: editDish.value.mainIngredient, english_name: editDish.value.englishName, sale_price: editDish.value.salePrice, cooking_time: editDish.value.cookingTime, festive_name: editDish.value.festiveName, image_url: editDish.value.imageUrl, servings: editDish.value.servings||1 })
      if (res.code===200&&res.data) { dishId=res.data.dishId; editDish.value.dishId=dishId } else { ElMessage.error(res.message||'创建失败'); return }
    } else {
      await updateDish(dishId, { dish_name: editDish.value.dishName, dish_category: editDish.value.dishCategory, spicy_level: editDish.value.spicyLevel||0, main_ingredient_type: editDish.value.mainIngredientType, main_ingredient: editDish.value.mainIngredient, english_name: editDish.value.englishName, sale_price: editDish.value.salePrice, cooking_time: editDish.value.cookingTime, festive_name: editDish.value.festiveName, image_url: editDish.value.imageUrl, servings: editDish.value.servings||1 })
    }
    const payload = items.value.map(r => ({ ingredientId: r.ingredientId, ingredientName: r.ingredientName, quantity: r.quantity, unit: r.unit, unitPrice: r.unitPrice, wastageRate: r.wastageRate, yieldRate: r.yieldRate, netUnitPrice: r.netUnitPrice, totalCost: r.totalCost }))
    await saveRecipe(dishId, payload)
    ElMessage.success('保存成功'); showDlg.value=false; await fetchData()
  } catch { ElMessage.error('保存失败') }
}

async function fetchAllIngredients() { try { const r=await getIngredients({limit:2000}); allIngredients.value=r.data?.content||r.data||[] } catch {} }
async function fetchData() { try { const r=await getDishes({}); if(r.code===200) dishes.value=r.data?.content||r.data||[] } catch {} }

onMounted(() => { fetchData(); fetchAllIngredients(); document.addEventListener('click', hideCtx) })
onUnmounted(() => document.removeEventListener('click', hideCtx))
</script>

<style scoped>
.cost-page { padding: 16px; }
.sub-header { margin-bottom: 12px; }
.sub-header h2 { font-size: 18px; margin: 0; }
.page-desc { font-size: 12px; color: #9ca3af; margin: 2px 0 0; }
.cost-toolbar { display: flex; gap: 10px; align-items: center; margin-bottom: 10px; }
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 16px; }
.stat-card { background: var(--color-card); border: 1px solid var(--color-border); border-radius: 10px; padding: 18px 20px; position: relative; overflow: hidden; }
.stat-card::after { content: ''; position: absolute; top: 0; right: 0; width: 64px; height: 64px; background: currentColor; opacity: 0.03; border-radius: 0 0 0 64px; }
.stat-content { flex: 1; }
.stat-label { font-size: 12px; color: var(--color-text-muted); margin-bottom: 6px; font-weight: 500; }
.stat-value { font-size: 24px; font-weight: 700; color: var(--color-text); line-height: 1.2; }
.ctx-menu { position: fixed; z-index: 9999; background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; box-shadow: 0 4px 16px rgba(0,0,0,0.12); min-width: 150px; padding: 4px 0; }
.ctx-item { padding: 8px 16px; cursor: pointer; font-size: 13px; }
.ctx-item:hover { background: #f3f4f6; }
.ctx-danger { color: #dc2626; }
.ctx-danger:hover { background: #fef2f2; }
:deep(.row-selected) { background-color: #fde68a !important; }
:deep(.row-selected td) { background-color: #fde68a !important; }

.excel-hd { display: flex; align-items: center; padding: 6px 12px; background: #f8fafc; border-bottom: 1px solid #e5e7eb; }
.excel-label-top { font-size: 14px; font-weight: 700; color: #1f2937; letter-spacing: 1px; }
.excel-sub { text-align: center; padding: 6px 0; border-bottom: 2px solid #1f2937; }
.excel-title { font-size: 16px; font-weight: 700; letter-spacing: 4px; }
.top-bar { display: flex; align-items: center; justify-content: center; gap: 16px; }
.top-bar-small { width: 120px; }
.top-bar-name { width: 240px; }
.top-bar-item { text-align: left; }
.top-bar-label { font-size: 10px; color: #6b7280; display: block; line-height: 1.3; }

.info-table { width: 100%; border-collapse: collapse; background: #fff; border: 1px solid #d1d5db; border-radius: 4px; margin-bottom: 10px; table-layout: fixed; }
.info-table td { padding: 4px 6px; border: 1px solid #e5e7eb; vertical-align: middle; }
.it-hd { font-size: 10px; color: #6b7280; line-height: 1.3; white-space: nowrap; background: #f9fafb; }
:deep(.info-table .el-input-number) { width: 100% !important; }

.spicy-icons { display: inline-flex; gap: 2px; align-items: center; outline: none; }
.spicy-icons:focus-visible { box-shadow: 0 0 0 2px #fde68a; border-radius: 4px; }
.spicy-pepper { width: 14px; height: 14px; border-radius: 50%; background: #e5e7eb; cursor: pointer; transition: all 0.15s; display: inline-block; }
.spicy-pepper.active { background: #C25555; }
.spicy-pepper:hover { opacity: 0.8; }

.img-box { position: relative; width: 192px; height: 108px; border: 1px solid #d1d5db; border-radius: 6px; overflow: hidden; cursor: pointer; }
.img-box img { width: 100%; height: 100%; object-fit: cover; }
.img-box-overlay { position: absolute; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; gap: 6px; opacity: 0; transition: opacity 0.2s; font-size: 12px; color: #fff; }
.img-box:hover .img-box-overlay { opacity: 1; }
.img-box-overlay span { cursor: pointer; padding: 2px 6px; border-radius: 3px; background: rgba(255,255,255,0.15); }
.img-box-overlay span.del:hover { background: rgba(220,38,38,0.6); }
.img-box-empty { border-style: dashed; color: #9ca3af; font-size: 18px; }
.img-block { width: 192px; flex-shrink: 0; display: flex; flex-direction: column; gap: 2px; }

.ing-tbl { width: 100%; border-collapse: collapse; font-size: 12px; }
.ing-tbl th { background: #f3f4f6; border: 1px solid #d1d5db; padding: 4px 4px; font-size: 10px; color: #6b7280; line-height: 1.3; }
.ing-tbl td { border: 1px solid #e5e7eb; padding: 3px 4px; vertical-align: middle; }
.ing-tbl tbody tr:hover { background: #f9fafb; }
.td-center { text-align: center; color: #9ca3af; font-size: 11px; }
.td-right { text-align: right; font-size: 12px; color: #374151; }
.td-bold { font-weight: 700; font-size: 13px; }

/* 6列 flex 信息区 */
.six-col-grid { width: 100%; background: #fff; border: 1px solid #d1d5db; border-radius: 4px; padding: 8px; margin-bottom: 10px; }
.six-col-grid .sc-row { display: flex; }
.sc-item { flex: 1; padding: 3px 6px; display: flex; flex-direction: column; gap: 1px; border-right: 1px solid #f3f4f6; }
.sc-item:last-child { border-right: none; }
.sc-item .sc-label { font-size: 9px; color: #6b7280; line-height: 1.3; white-space: nowrap; }
.sc-item :deep(.el-input), .sc-item :deep(.el-select), .sc-item :deep(.el-input-number--small) { width: 100% !important; }
.ing-dd { display: flex; gap: 16px; align-items: center; font-size: 12px; padding: 2px 0; }
.ing-dd-name { font-weight: 600; flex: 0 0 180px; }
.ing-dd-meta { color: #6b7280; white-space: nowrap; flex: 1; min-width: 200px; }
.ing-dd-date { color: #9ca3af; font-size: 11px; white-space: nowrap; width: 110px; text-align: right; }

:deep(.el-dialog__header) { padding: 0; margin: 0; }
:deep(.el-dialog__headerbtn) { top: 12px; right: 14px; }
:deep(.el-dialog__body) { padding: 8px 20px 0; }
</style>
