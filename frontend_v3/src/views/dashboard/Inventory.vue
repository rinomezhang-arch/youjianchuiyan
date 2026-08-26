<template>
  <div class="page">
    <div class="page-header">
      <h2>库存管理 · Inventory Management</h2>
      <p class="page-desc">原料库存 · 材料种类/信息 · 出入库 · 预警</p>
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
            <el-button type="primary" @click="openCategoryDialog()">+ 新增种类</el-button>
            <el-button type="danger" :disabled="categorySelections.length===0" @click="batchDeleteCategory">批量删除</el-button>
          </div>
        </div>
        <el-table :data="categoryList" stripe class="data-table" v-loading="categoryLoading" @selection-change="s=>categorySelections=s">
          <el-table-column type="selection" width="50" />
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="categoryName" label="材料种类" min-width="200" />
          <el-table-column prop="storeId" label="门店ID" width="80" />
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button link size="small" type="primary" @click="openCategoryDialog(row)">编辑</el-button>
              <el-popconfirm title="确认删除?" @confirm="deleteCategory(row)">
                <template #reference><el-button link size="small" type="danger">删除</el-button></template>
              </el-popconfirm>
            </template>
          </el-table-column>
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
    <!-- 材料种类 新增/编辑 弹窗                       -->
    <!-- ============================================ -->
    <el-dialog v-model="categoryDialogVisible" :title="categoryEditing?'编辑材料种类':'新增材料种类'" width="500px" destroy-on-close>
      <el-form :model="categoryForm" label-width="90px">
        <el-form-item label="种类名称" required>
          <el-input v-model="categoryForm.categoryName" placeholder="如：蔬菜类、肉类、粮油" />
        </el-form-item>
        <el-form-item label="门店ID">
          <el-input v-model="categoryForm.storeId" placeholder="可选，留空为全局" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialogVisible=false">取消</el-button>
        <el-button type="primary" @click="saveCategory">保存</el-button>
      </template>
    </el-dialog>

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
    <el-dialog v-model="stockInVisible" title="入库登记" width="560px" destroy-on-close>
      <el-form :model="stockInForm" label-width="90px">
        <el-form-item label="材料" required>
          <el-select v-model="stockInForm.ingredientId" filterable placeholder="选择原料" style="width:100%">
            <el-option v-for="m in overviewList" :key="m.ingredientId" :label="m.ingredientName + ' (库存:' + m.currentStock + ')'" :value="m.ingredientId" />
          </el-select>
        </el-form-item>
        <el-form-item label="入库数量" required><el-input-number v-model="stockInForm.quantity" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="经手人"><el-input v-model="stockInForm.operator" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="stockInForm.notes" type="textarea" :rows="2" /></el-form-item>
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

/* ========== 库存总览 ========== */
const overviewLoading = ref(false)
const overviewList = ref([])
const overviewKeyword = ref('')
const overviewCategory = ref('')

async function fetchOverview() {
  overviewLoading.value = true
  try {
    const res = await request.get('/api/inventory/summary', { params: { storeId: currentStoreId.value } })
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
  } catch (e) { console.error(e); ElMessage.error('加载库存数据失败') } finally { overviewLoading.value = false }
}

async function fetchWarnings() {
  try {
    const res = await request.get('/api/inventory/alerts', { params: { storeId: currentStoreId.value } })
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
const categorySelections = ref([])
const categoryDialogVisible = ref(false)
const categoryEditing = ref(false)
const categoryForm = ref({ id: null, categoryName: '', storeId: null })
const categoryOptions = ref([])

async function fetchCategories() {
  categoryLoading.value = true
  try {
    const res = await request.get('/api/purchase/material-category/page', { params: { storeId: currentStoreId.value, page: 1, limit: 500 } })
    const d = res.data || res
    let list = d.data || []
    if (categorySearch.value) {
      list = list.filter(c => (c.categoryName||'').includes(categorySearch.value))
    }
    categoryList.value = list
    categoryOptions.value = list.map(c => ({ categoryId: c.id, categoryName: c.categoryName }))
  } catch (e) { console.error(e) } finally { categoryLoading.value = false }
}

function openCategoryDialog(row) {
  if (row) {
    categoryEditing.value = true
    categoryForm.value = { id: row.id, categoryName: row.categoryName, storeId: row.storeId }
  } else {
    categoryEditing.value = false
    categoryForm.value = { id: null, categoryName: '', storeId: currentStoreId.value }
  }
  categoryDialogVisible.value = true
}

async function saveCategory() {
  if (!categoryForm.value.categoryName) { ElMessage.warning('请输入种类名称'); return }
  try {
    const url = categoryEditing.value ? '/api/purchase/material-category/update' : '/api/purchase/material-category/save'
    const method = categoryEditing.value ? 'post' : 'post'
    const res = await request[method](url, categoryForm.value)
    const d = res.data || res
    if (d.code === 0) { ElMessage.success('保存成功'); categoryDialogVisible.value = false; fetchCategories() }
    else ElMessage.error(d.msg || '保存失败')
  } catch (e) { ElMessage.error('保存失败') }
}

async function deleteCategory(row) {
  try {
    const res = await request.post('/api/purchase/material-category/delete', [row.id])
    const d = res.data || res
    if (d.code === 0) { ElMessage.success('已删除'); fetchCategories() }
  } catch (e) { ElMessage.error('删除失败') }
}

async function batchDeleteCategory() {
  if (categorySelections.value.length === 0) return
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${categorySelections.value.length} 项?`, '提示', { type:'warning' })
    const ids = categorySelections.value.map(c => c.id)
    await request.post('/api/purchase/material-category/delete', ids)
    ElMessage.success('批量删除成功'); fetchCategories()
  } catch (e) { if (e !== 'cancel') ElMessage.error('批量删除失败') }
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
    const res = await request.get('/api/purchase/supplier/page', { params: { storeId: currentStoreId.value, page: 1, limit: 500 } })
    const d = res.data || res
    supplierOptions.value = d.data || []
  } catch (e) { console.error(e) }
}

async function fetchMaterials() {
  materialLoading.value = true
  try {
    const params = { storeId: currentStoreId.value, page: 1, limit: 500 }
    const res = await request.get('/api/purchase/material-info/page', { params })
    const d = res.data || res
    let list = d.data || []
    if (materialSearch.value) {
      const kw = materialSearch.value.toLowerCase()
      list = list.filter(m => (m.materialName||'').toLowerCase().includes(kw))
    }
    if (materialCategoryFilter.value) {
      list = list.filter(m => (m.category||'') === materialCategoryFilter.value)
    }
    if (materialSupplierFilter.value) {
      list = list.filter(m => (m.supplierAccount||'') === materialSupplierFilter.value)
    }
    materialList.value = list
  } catch (e) { console.error(e) } finally { materialLoading.value = false }
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

async function saveMaterial() {
  if (!materialForm.value.materialName) { ElMessage.warning('请输入材料名称'); return }
  try {
    const url = materialEditing.value ? '/api/purchase/material-info/update' : '/api/purchase/material-info/save'
    const res = await request.post(url, materialForm.value)
    const d = res.data || res
    if (d.code === 0) { ElMessage.success('保存成功'); materialDialogVisible.value = false; fetchMaterials() }
    else ElMessage.error(d.msg || '保存失败')
  } catch (e) { ElMessage.error('保存失败') }
}

async function deleteMaterial(row) {
  try {
    const res = await request.post('/api/purchase/material-info/delete', [row.id])
    const d = res.data || res
    if (d.code === 0) { ElMessage.success('已删除'); fetchMaterials() }
  } catch (e) { ElMessage.error('删除失败') }
}

async function batchDeleteMaterial() {
  if (materialSelections.value.length === 0) return
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${materialSelections.value.length} 项?`, '提示', { type:'warning' })
    const ids = materialSelections.value.map(m => m.id)
    await request.post('/api/purchase/material-info/delete', ids)
    ElMessage.success('批量删除成功'); fetchMaterials()
  } catch (e) { if (e !== 'cancel') ElMessage.error('批量删除失败') }
}

/* ========== 入库 ========== */
const stockInVisible = ref(false)
const stockInForm = ref({
  ingredientId: '', quantity: 0, operator: '', notes: ''
})

function openStockInDialog() {
  stockInForm.value = { ingredientId: '', quantity: 0, operator: '', notes: '' }
  stockInVisible.value = true
}

async function submitStockIn() {
  if (!stockInForm.value.ingredientId) { ElMessage.warning('请选择原料'); return }
  if (!stockInForm.value.quantity || stockInForm.value.quantity <= 0) { ElMessage.warning('请填写入库数量'); return }
  try {
    // 对齐真实存在的 /api/inventory/in（此前调用的 /api/purchase/purchase-in/save 后端根本不存在，
    // 与"出库"走的 /api/inventory/out 不是同一套接口，同一张台账出库能记账、入库记不了账）
    const res = await request.post('/api/inventory/in', {
      storeId: String(currentStoreId.value),
      ingredientId: stockInForm.value.ingredientId,
      quantity: stockInForm.value.quantity,
      operator: stockInForm.value.operator,
      notes: stockInForm.value.notes
    })
    const d = res.data || res
    if (d.code === 200 || d.code === 0) {
      ElMessage.success('入库登记成功')
      stockInVisible.value = false
      fetchOverview()
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
    const res = await request.post('/api/inventory/out', {
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
})
</script>

<style scoped>
.page { width:100%; }
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
