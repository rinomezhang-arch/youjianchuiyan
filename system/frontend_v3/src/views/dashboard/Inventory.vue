<template>
  <div class="page">
    <div class="page-header">
      <h2>库存管理 · Inventory Management</h2>
      <p class="page-desc">原料库存 · 材料种类/信息 · 出入库 · 预警</p>
    </div>

    <div class="loop-strip" v-loading="loopLoading">
      <div class="loop-stat is-ready"><span>待验收入库</span><strong>{{ loopStats.pendingReceipt || 0 }}</strong></div>
      <div class="loop-stat is-warning"><span>待审批/发料</span><strong>{{ loopStats.pendingRequisition || 0 }}</strong></div>
      <div class="loop-stat"><span>库存原料</span><strong>{{ overviewList.length }}</strong></div>
      <div class="loop-copy">验收合格数量按批次入账；领料审批后按先到期先出原则扣减库存。</div>
    </div>

    <el-tabs v-model="activeTab" class="main-tabs">
      <!-- ============ Tab1: 库存总览 ============ -->
      <el-tab-pane label="库存总览" name="overview">
        <div class="toolbar">
          <div class="toolbar-left">
            <el-input v-model="overviewKeyword" placeholder="搜索原料" class="search-box" clearable @keyup.enter="fetchOverview" />
            <el-select v-model="overviewCategory" placeholder="分类" clearable class="sel-box" @change="fetchOverview">
              <el-option v-for="c in categoryOptions" :key="c.categoryId" :label="c.categoryName" :value="c.categoryName" />
            </el-select>
          </div>
          <div class="toolbar-right">
            <el-button type="warning" plain @click="fetchWarnings">⚠ 查看预警</el-button>
            <el-button type="primary" @click="openStockInDialog">+ 入库</el-button>
            <el-button type="danger" plain @click="openStockOutDialog">- 出库</el-button>
          </div>
        </div>
        <el-table :data="overviewList" stripe class="data-table" v-loading="overviewLoading">
          <el-table-column prop="ingredientId" label="编码" width="100" />
          <el-table-column prop="ingredientName" label="原料名称" min-width="180" />
          <el-table-column prop="ingredientCategory" label="分类" width="100" />
          <el-table-column prop="currentStock" label="当前库存" width="100" />
          <el-table-column prop="usageUnit" label="单位" width="60" />
          <el-table-column prop="warningThreshold" label="预警阈值" width="100" />
          <el-table-column prop="avgPrice" label="平均成本" width="100">
            <template #default="{ row }">¥{{ row.avgPrice || '0' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag v-if="Number(row.currentStock) <= Number(row.warningThreshold)" type="danger" size="small">低库存</el-tag>
              <el-tag v-else type="success" size="small">正常</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ============ Tab2: 材料种类管理 ============ -->
      <el-tab-pane label="材料种类" name="category">
        <div class="toolbar">
          <div class="toolbar-left">
            <el-input v-model="categorySearch" placeholder="搜索种类名称" class="search-box" clearable @keyup.enter="fetchCategories" />
          </div>
          <div class="toolbar-right">
            <el-tag type="info">分类由原料档案实时汇总</el-tag>
          </div>
        </div>
        <el-table :data="categoryList" stripe class="data-table" v-loading="categoryLoading">
          <el-table-column prop="id" label="序号" width="80" />
          <el-table-column prop="categoryName" label="材料种类" min-width="200" />
          <el-table-column prop="materialCount" label="原料数量" width="120" />
          <el-table-column prop="storeId" label="门店ID" width="100" />
        </el-table>
      </el-tab-pane>

      <!-- ============ Tab3: 材料信息管理 ============ -->
      <el-tab-pane label="材料信息" name="material">
        <div class="toolbar">
          <div class="toolbar-left">
            <el-input v-model="materialSearch" placeholder="搜索材料名称" class="search-box" clearable @keyup.enter="fetchMaterials" />
            <el-select v-model="materialCategoryFilter" placeholder="种类" clearable class="sel-box" @change="fetchMaterials">
              <el-option v-for="c in categoryOptions" :key="c.categoryId" :label="c.categoryName" :value="c.categoryName" />
            </el-select>
            <el-select v-model="materialSupplierFilter" placeholder="供应商" clearable filterable class="sel-box" @change="fetchMaterials">
              <el-option v-for="s in supplierOptions" :key="s.id" :label="s.supplierName" :value="s.supplierAccount" />
            </el-select>
          </div>
          <div class="toolbar-right">
            <el-button type="primary" @click="openMaterialDialog()">+ 新增材料</el-button>
            <el-button type="danger" :disabled="materialSelections.length===0" @click="batchDeleteMaterial">批量删除</el-button>
          </div>
        </div>
        <el-table :data="materialList" stripe class="data-table" v-loading="materialLoading" @selection-change="s=>materialSelections=s">
          <el-table-column type="selection" width="50" />
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column label="图片" width="80">
            <template #default="{ row }">
              <el-image v-if="row.image" :src="row.image.startsWith('http')?row.image:'/'+row.image" style="width:50px;height:50px;border-radius:4px" fit="cover" />
              <span v-else style="color:#ccc">无图</span>
            </template>
          </el-table-column>
          <el-table-column prop="materialName" label="材料名称" min-width="140" />
          <el-table-column prop="category" label="种类" width="100" />
          <el-table-column prop="specification" label="规格" width="100" />
          <el-table-column prop="price" label="价格" width="80">
            <template #default="{ row }">¥{{ row.price || '0' }}</template>
          </el-table-column>
          <el-table-column prop="stock" label="库存" width="70" />
          <el-table-column prop="supplierName" label="供应商" width="120" />
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button link size="small" type="primary" @click="openMaterialDialog(row)">编辑</el-button>
              <el-popconfirm title="确认删除?" @confirm="deleteMaterial(row)">
                <template #reference><el-button link size="small" type="danger">删除</el-button></template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- ============================================ -->
    <!-- 材料信息 新增/编辑 弹窗                       -->
    <!-- ============================================ -->
    <el-dialog v-model="materialDialogVisible" :title="materialEditing?'编辑材料':'新增材料'" width="720px" destroy-on-close>
      <el-form :model="materialForm" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="材料名称" required><el-input v-model="materialForm.materialName" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="材料种类">
              <el-select v-model="materialForm.category" filterable placeholder="选择或输入" style="width:100%" allow-create>
                <el-option v-for="c in categoryOptions" :key="c.categoryId" :label="c.categoryName" :value="c.categoryName" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="规格"><el-input v-model="materialForm.specification" placeholder="如：500g/袋、10kg/箱" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="价格 (元)"><el-input-number v-model="materialForm.price" :min="0" :precision="2" :step="0.5" style="width:100%" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="库存"><el-input-number v-model="materialForm.stock" :min="0" style="width:100%" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单次限购"><el-input-number v-model="materialForm.singleLimit" :min="0" style="width:100%" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="门店ID"><el-input v-model="materialForm.storeId" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="供应商">
              <el-select v-model="materialForm.supplierAccount" filterable placeholder="选择供应商" style="width:100%" @change="onSupplierChange">
                <el-option v-for="s in supplierOptions" :key="s.id" :label="s.supplierName + ' (' + (s.supplierAccount||'') + ')'" :value="s.supplierAccount" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供应商名称"><el-input v-model="materialForm.supplierName" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="图片">
          <el-input v-model="materialForm.image" placeholder="图片URL或上传路径" />
        </el-form-item>
        <el-form-item label="详情描述">
          <el-input v-model="materialForm.detail" type="textarea" :rows="3" placeholder="材料详细介绍" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="materialDialogVisible=false">取消</el-button>
        <el-button type="primary" @click="saveMaterial">保存</el-button>
      </template>
    </el-dialog>

    <!-- ============================================ -->
    <!-- 入库弹窗                                      -->
    <!-- ============================================ -->
    <el-dialog v-model="stockInVisible" title="入库登记" width="640px" destroy-on-close>
      <el-form :model="stockInForm" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="材料名称" required>
              <el-select v-model="stockInForm.ingredientId" filterable style="width:100%" @change="onStockInMaterialChange">
                <el-option v-for="m in materialList" :key="m.id" :label="m.materialName" :value="m.ingredientId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="材料种类">
              <el-select v-model="stockInForm.category" filterable allow-create style="width:100%">
                <el-option v-for="c in categoryOptions" :key="c.categoryId" :label="c.categoryName" :value="c.categoryName" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="规格"><el-input v-model="stockInForm.specification" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="入库数量" required><el-input-number v-model="stockInForm.stock" :min="0" style="width:100%" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="入库时间">
              <el-date-picker v-model="stockInForm.inTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="供应商">
              <el-select v-model="stockInForm.supplierAccount" filterable style="width:100%" @change="onInSupplierChange">
                <el-option v-for="s in supplierOptions" :key="s.id" :label="s.supplierName" :value="s.supplierAccount" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供应商名称"><el-input v-model="stockInForm.supplierName" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注"><el-input v-model="stockInForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stockInVisible=false">取消</el-button>
        <el-button type="primary" @click="submitStockIn">确认入库</el-button>
      </template>
    </el-dialog>

    <!-- ============================================ -->
    <!-- 出库弹窗                                      -->
    <!-- ============================================ -->
    <el-dialog v-model="stockOutVisible" title="出库登记" width="560px" destroy-on-close>
      <el-form :model="stockOutForm" label-width="90px">
        <el-form-item label="材料" required>
          <el-select v-model="stockOutForm.ingredientId" filterable placeholder="选择原料" style="width:100%">
            <el-option v-for="m in overviewList" :key="m.ingredientId" :label="m.ingredientName + ' (库存:' + m.currentStock + ')'" :value="m.ingredientId" />
          </el-select>
        </el-form-item>
        <el-form-item label="出库数量" required><el-input-number v-model="stockOutForm.quantity" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="领用人"><el-input v-model="stockOutForm.operator" /></el-form-item>
        <el-form-item label="原因/用途"><el-input v-model="stockOutForm.reason" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stockOutVisible=false">取消</el-button>
        <el-button type="danger" @click="submitStockOut">确认出库</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const currentStoreId = computed(() => userStore.currentStore?.storeId || userStore.stores?.[0]?.storeId || 1)

const activeTab = ref('overview')
const loopLoading = ref(false)
const loopStats = ref({})

async function fetchLoopStats() {
  loopLoading.value = true
  try {
    const res = await request.get('/cost-procurement-loop/dashboard', { params: { storeId: currentStoreId.value } })
    loopStats.value = res.data || {}
  } catch {
    loopStats.value = {}
  } finally {
    loopLoading.value = false
  }
}

/* ========== 库存总览 ========== */
const overviewLoading = ref(false)
const overviewList = ref([])
const overviewKeyword = ref('')
const overviewCategory = ref('')

async function fetchOverview() {
  overviewLoading.value = true
  try {
    const res = await request.get('/inventory/summary', { params: { storeId: currentStoreId.value } })
    const d = res.data || res
    let list = d.data || d.list || []
    if (overviewKeyword.value) {
      const kw = overviewKeyword.value.toLowerCase()
      list = list.filter(i => (i.ingredientName||'').toLowerCase().includes(kw) || (i.ingredientId||'').toLowerCase().includes(kw))
    }
    if (overviewCategory.value) {
      list = list.filter(i => (i.ingredientCategory||'') === overviewCategory.value)
    }
    overviewList.value = list
  } catch (e) { console.error(e); ElMessage.error('加载库存数��失败') } finally { overviewLoading.value = false }
}

async function fetchWarnings() {
  try {
    const res = await request.get('/inventory/alerts', { params: { storeId: currentStoreId.value } })
    const d = res.data || res
    const warns = d.data || []
    if (warns.length === 0) ElMessage.info('暂无低库存预警')
    else ElMessageBox.alert(warns.map(w => `⚠ ${w.ingredientName || w.material_name}: 当前 ${w.currentStock || w.current_qty} ${w.usageUnit || w.unit}，阈值 ${w.warningThreshold || w.safety_qty}`).join('\n'), '低库存预警', { confirmButtonText: '知道了' })
  } catch (e) { console.error(e) }
}

/* ========== 材料种类 ========== */
const categoryLoading = ref(false)
const categoryList = ref([])
const categorySearch = ref('')
const categoryOptions = ref([])

async function fetchCategories(sourceMaterials) {
  categoryLoading.value = true
  try {
    let ingredients = sourceMaterials
    if (!ingredients) {
      const res = await request.get('/ingredients', { params: { storeId: currentStoreId.value } })
      ingredients = res.data || []
    }
    const counts = ingredients.reduce((result, item) => {
      const name = item.category || '未分类'
      result[name] = (result[name] || 0) + 1
      return result
    }, {})
    let list = Object.entries(counts).map(([categoryName, materialCount], index) => ({
      id: index + 1,
      categoryName,
      materialCount,
      storeId: currentStoreId.value
    }))
    if (categorySearch.value) list = list.filter(c => c.categoryName.includes(categorySearch.value))
    categoryList.value = list
    categoryOptions.value = list.map(c => ({ categoryId: c.id, categoryName: c.categoryName }))
  } catch (e) {
    console.error(e)
    ElMessage.error('加载原料分类失败')
  } finally {
    categoryLoading.value = false
  }
}

/* ========== 材料信息 ========== */
const materialLoading = ref(false)
const materialList = ref([])
const materialSearch = ref('')
const materialCategoryFilter = ref('')
const materialSupplierFilter = ref('')
const materialSelections = ref([])
const materialDialogVisible = ref(false)
const materialEditing = ref(false)
const supplierOptions = ref([])
const materialForm = ref({
  id: null, materialName: '', image: '', category: '', specification: '', detail: '',
  supplierAccount: '', supplierName: '', singleLimit: 0, stock: 0, price: 0, storeId: null, clickTime: null
})

async function fetchSuppliers() {
  try {
    const res = await request.get('/suppliers', { params: { storeId: currentStoreId.value } })
    supplierOptions.value = (res.data || []).map(s => ({ ...s, id: s.supplierId, supplierAccount: s.supplierId }))
  } catch (e) { console.error(e); ElMessage.error('加载供应商失败') }
}

async function fetchMaterials() {
  materialLoading.value = true
  try {
    const res = await request.get('/ingredients', { params: { storeId: currentStoreId.value } })
    const ingredients = res.data || []
    await fetchCategories(ingredients)
    let list = ingredients.map(item => ({
      ...item,
      id: item.ingredientId,
      materialName: item.ingredientName,
      price: item.unitPrice,
      stock: item.currentStock,
      specification: item.unit,
      supplierAccount: item.supplierId
    }))
    if (materialSearch.value) {
      const kw = materialSearch.value.toLowerCase()
      list = list.filter(m => (m.materialName||'').toLowerCase().includes(kw))
    }
    if (materialCategoryFilter.value) list = list.filter(m => (m.category||'未分类') === materialCategoryFilter.value)
    if (materialSupplierFilter.value) list = list.filter(m => (m.supplierAccount||'') === materialSupplierFilter.value)
    materialList.value = list
  } catch (e) { console.error(e); ElMessage.error('加载原料档案失败') } finally { materialLoading.value = false }
}

function openMaterialDialog(row) {
  if (row) {
    materialEditing.value = true
    materialForm.value = { ...row }
  } else {
    materialEditing.value = false
    materialForm.value = {
      id: null, materialName: '', image: '', category: '', specification: '', detail: '',
      supplierAccount: '', supplierName: '', singleLimit: 0, stock: 0, price: 0, storeId: currentStoreId.value, clickTime: null
    }
  }
  materialDialogVisible.value = true
}

function onSupplierChange(acc) {
  const s = supplierOptions.value.find(x => x.supplierAccount === acc)
  if (s && !materialForm.value.supplierName) materialForm.value.supplierName = s.supplierName
}

function toIngredientPayload(form) {
  return {
    ingredientId: form.ingredientId || form.id || undefined,
    storeId: String(form.storeId || currentStoreId.value),
    ingredientName: form.materialName,
    category: form.category || null,
    unit: form.specification || '份',
    currentStock: Number(form.stock || 0),
    minStock: Number(form.singleLimit || form.minStock || 0),
    unitPrice: Number(form.price || 0),
    supplierId: form.supplierAccount || null,
    supplierName: form.supplierName || null,
    status: form.status || 'active'
  }
}

async function saveMaterial() {
  if (!materialForm.value.materialName) { ElMessage.warning('请输入材料名称'); return }
  try {
    const payload = toIngredientPayload(materialForm.value)
    const res = materialEditing.value
      ? await request.put(`/ingredients/${materialForm.value.ingredientId || materialForm.value.id}`, payload, { params: { storeId: currentStoreId.value } })
      : await request.post('/ingredients', payload)
    const d = res.data || res
    if (d.code === 200 || d.code === 0 || d.ingredientId) {
      ElMessage.success('保存成功'); materialDialogVisible.value = false; await fetchMaterials(); await fetchOverview()
    } else ElMessage.error(d.message || d.msg || '保存失败')
  } catch (e) { ElMessage.error('保存失败') }
}

async function deleteMaterial(row) {
  try {
    await request.delete(`/ingredients/${row.ingredientId || row.id}`, { params: { storeId: currentStoreId.value } })
    ElMessage.success('已删除'); await fetchMaterials(); await fetchOverview()
  } catch (e) { ElMessage.error('删除失败，原料可能已被业务单据引用') }
}

async function batchDeleteMaterial() {
  if (materialSelections.value.length === 0) return
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${materialSelections.value.length} 项?`, '提示', { type:'warning' })
    for (const material of materialSelections.value) {
      await request.delete(`/ingredients/${material.ingredientId || material.id}`, { params: { storeId: currentStoreId.value } })
    }
    ElMessage.success('批量删除成功'); await fetchMaterials(); await fetchOverview()
  } catch (e) { if (e !== 'cancel') ElMessage.error('批量删除未完成，请检查原料引用关系') }
}

/* ========== 入库 ========== */
const stockInVisible = ref(false)
const stockInForm = ref({
  ingredientId: '', materialName: '', category: '', specification: '', stock: 0, inTime: '',
  remark: '', supplierAccount: '', supplierName: '', storeId: null
})

function openStockInDialog() {
  stockInForm.value = {
    ingredientId: '', materialName: '', category: '', specification: '', stock: 0,
    inTime: new Date().toISOString().slice(0,19).replace('T',' '),
    remark: '', supplierAccount: '', supplierName: '', storeId: currentStoreId.value
  }
  stockInVisible.value = true
}

function onStockInMaterialChange(ingredientId) {
  const material = materialList.value.find(item => item.ingredientId === ingredientId)
  if (!material) return
  stockInForm.value.materialName = material.materialName
  stockInForm.value.category = material.category
  stockInForm.value.specification = material.specification
  stockInForm.value.supplierAccount = material.supplierAccount
  stockInForm.value.supplierName = material.supplierName
}

function onInSupplierChange(acc) {
  const s = supplierOptions.value.find(x => x.supplierAccount === acc)
  if (s) stockInForm.value.supplierName = s.supplierName
}

async function submitStockIn() {
  if (!stockInForm.value.ingredientId) { ElMessage.warning('请选择材料'); return }
  if (!stockInForm.value.stock || stockInForm.value.stock <= 0) { ElMessage.warning('请填写入库数量'); return }
  try {
    const res = await request.post('/inventory/in', {
      storeId: String(currentStoreId.value),
      ingredientId: stockInForm.value.ingredientId,
      ingredientName: stockInForm.value.materialName,
      quantity: stockInForm.value.stock,
      operator: userStore.userInfo?.name || userStore.userInfo?.username || '系统用户',
      referenceType: 'manual_stock_in',
      referenceId: `IN-${Date.now()}`,
      notes: stockInForm.value.remark
    })
    const d = res.data || res
    if (d.code === 200 || d.code === 0 || d.ingredientId) {
      ElMessage.success('入库登记成功')
      stockInVisible.value = false
      await fetchOverview()
      await fetchMaterials()
    } else {
      ElMessage.error(d.message || d.msg || '入库失败')
    }
  } catch (e) { ElMessage.error('入库失败') }
}

/* ========== 出库 ========== */
const stockOutVisible = ref(false)
const stockOutForm = ref({ ingredientId: '', quantity: 0, operator: '', reason: '' })

function openStockOutDialog() {
  stockOutForm.value = { ingredientId: '', quantity: 0, operator: '', reason: '' }
  stockOutVisible.value = true
}

async function submitStockOut() {
  if (!stockOutForm.value.ingredientId) { ElMessage.warning('请选择原料'); return }
  if (!stockOutForm.value.quantity || stockOutForm.value.quantity <= 0) { ElMessage.warning('请填写出库数量'); return }
  try {
    const res = await request.post('/inventory/out', {
      storeId: String(currentStoreId.value),
      ingredientId: stockOutForm.value.ingredientId,
      quantity: stockOutForm.value.quantity,
      operator: stockOutForm.value.operator,
      reason: stockOutForm.value.reason
    })
    const d = res.data || res
    if (d.code === 200 || d.code === 0) {
      ElMessage.success('出库成功'); stockOutVisible.value = false; fetchOverview()
    } else {
      ElMessage.error(d.message || d.msg || '出库失败')
    }
  } catch (e) { ElMessage.error('出库失败') }
}

onMounted(() => {
  fetchCategories()
  fetchSuppliers().then(() => fetchMaterials())
  fetchOverview()
  fetchLoopStats()
})
</script>

<style scoped>
.page { width:100%; }
.loop-strip { display:flex; align-items:stretch; gap:8px; padding:10px; margin-bottom:12px; background:#f8fafc; border:1px solid #dbe4ee; border-radius:8px; }
.loop-stat { min-width:112px; padding:8px 10px; background:#ffffff; border:1px solid #dbe4ee; border-radius:6px; }
.loop-stat span { display:block; color:#64748b; font-size:12px; }
.loop-stat strong { display:block; margin-top:2px; color:#1e3a5f; font-size:22px; line-height:1.2; }
.loop-stat.is-ready strong { color:#166534; }
.loop-stat.is-warning strong { color:#b45309; }
.loop-copy { flex:1; display:flex; align-items:center; color:#475569; font-size:13px; line-height:1.5; }
.page-header { display:flex; align-items:center; gap:12px; margin-bottom:12px; }
.page-header h2 { font-size:18px; font-weight:600; margin:0; }
.page-desc { font-size:13px; color:#64748b; margin:0; }
.toolbar { display:flex; justify-content:space-between; align-items:center; margin-bottom:12px; gap:10px; flex-wrap:wrap; }
.toolbar-left, .toolbar-right { display:flex; gap:8px; align-items:center; flex-wrap:wrap; }
.search-box { width:220px; }
.sel-box { width:150px; }
.main-tabs { margin-top:4px; }
.data-table { width:100%; }
</style>
