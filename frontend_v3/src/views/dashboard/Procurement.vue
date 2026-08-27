<template>
  <div class="page">
    <div class="page-header">
      <h2>采购管理 · Procurement Management</h2>
      <p class="page-desc">采购申请 · 提交 · 审核 · 入库</p>
    </div>

    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-date-picker v-model="dateRange" type="daterange" start-placeholder="日期范围" size="small" style="width:240px" value-format="YYYY-MM-DD" />
        <el-input v-model="keyword" placeholder="搜索原料" size="small" class="search-box" clearable />
        <el-button size="small" @click="fetchData">查询</el-button>
      </div>
      <div class="toolbar-right">
        <el-button type="primary" @click="openNew">+ 录入</el-button>
        <el-button size="small" @click="handleExport">导出</el-button>
      </div>
    </div>

    <!-- 采购记录表格 -->
    <el-table :data="list" stripe v-loading="loading" max-height="calc(100vh - 200px)" @row-click="openEdit">
      <el-table-column label="系统单号" width="130">
        <template #default="{ row }"><span class="mono">CIN-{{ row.purchaseId }}</span></template>
      </el-table-column>
      <el-table-column label="原料" width="120">
        <template #default="{ row }">{{ ingredientName(row.ingredientId) }}</template>
      </el-table-column>
      <el-table-column label="供应商" width="120">
        <template #default="{ row }">{{ supplierName(row.supplierId) }}</template>
      </el-table-column>
      <el-table-column label="数量" width="90">
        <template #default="{ row }">{{ row.quantity }}</template>
      </el-table-column>
      <el-table-column label="单价" width="90">
        <template #default="{ row }">¥{{ row.unitPrice }}</template>
      </el-table-column>
      <el-table-column label="金额" width="100">
        <template #default="{ row }"><span class="mono">¥{{ row.totalAmount }}</span></template>
      </el-table-column>
      <el-table-column label="日期" width="110">
        <template #default="{ row }">{{ formatDate(row.purchaseDate) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.status==='pending'" type="warning" size="small">待审核</el-tag>
          <el-tag v-else-if="row.status==='approved'" type="success" size="small">已审核</el-tag>
          <el-tag v-else-if="row.status==='draft'" type="info" size="small">草稿</el-tag>
          <el-tag v-else size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link size="small" type="primary" @click.stop="openEdit(row)">查看</el-button>
          <el-button v-if="row.status==='pending'" link size="small" type="success" @click.stop="handleAudit(row)">审核</el-button>
          <el-button v-if="row.status==='pending'" link size="small" type="danger" @click.stop="handleDelete(row)">取消</el-button>
          <el-button v-if="row.status==='approved'" link size="small" type="warning" @click.stop="openReceive(row)">验收入库</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- ============================================ -->
    <!-- 采购进仓单弹窗（录入 / 编辑）                  -->
    <!-- ============================================ -->
    <el-dialog v-model="showDialog" :title="dialogTitle" width="850px" top="2vh" :close-on-click-modal="false" destroy-on-close>
      <div class="pd-body">
        <div class="pd-topbar">
          <span class="pd-tag" :class="form.status">{{ form.status === 'pending' ? '待审核' : form.status === 'approved' ? '已审核' : '' }}</span>
          <div class="pd-btns">
            <el-button size="small" @click="resetForm">清空</el-button>
            <el-button size="small" type="primary" :disabled="!canSave" @click="savePurchase">提交申请</el-button>
            <el-button size="small" type="success" :disabled="form.status!=='pending'" @click="doAudit">审核通过</el-button>
            <el-button size="small" @click="handlePrint">🖨 打印</el-button>
          </div>
        </div>

        <div class="pd-row">
          <div class="pd-field"><label>系统单号</label><span class="pd-val mono">{{ form.orderNo || '(自动生成)' }}</span></div>
          <div class="pd-field"><label>制单日期</label><el-date-picker v-model="form.orderDate" type="date" size="small" value-format="YYYY-MM-DD" :disabled="form.status==='approved'" /></div>
          <div class="pd-field"><label>入库仓库</label><el-select v-model="form.warehouse" size="small" :disabled="form.status==='approved'"><el-option label="原料仓库" value="原料仓库" /><el-option label="冷库" value="冷库" /></el-select></div>
        </div>

        <div class="pd-row">
          <div class="pd-field"><label>供方名称</label><el-select v-model="form.supplierId" filterable size="small" placeholder="选择" style="width:100%" :disabled="form.status==='approved'" @change="onSuppChange">
            <el-option v-for="s in suppliers" :key="s.supplierId" :label="s.supplierName" :value="s.supplierId" />
          </el-select></div>
          <div class="pd-field"><label>联系人</label><el-input v-model="form.contactPerson" size="small" :disabled="form.status==='approved'" /></div>
          <div class="pd-field"><label>联系电话</label><el-input v-model="form.contactPhone" size="small" :disabled="form.status==='approved'" /></div>
          <div class="pd-field"><label>经手人</label><el-select v-model="form.handlerId" filterable size="small" placeholder="选人" :disabled="form.status==='approved'">
            <el-option v-for="s in staffList" :key="s.staffId" :label="s.staffName" :value="s.staffId" />
          </el-select></div>
        </div>

        <div class="pd-row">
          <div class="pd-field"><label>备注</label><el-input v-model="form.remark" size="small" :disabled="form.status==='approved'" /></div>
        </div>

        <div class="pd-section">产品明细</div>
        <div class="pd-table-wrap">
          <el-table :data="form.items" border stripe size="small" max-height="180" empty-text="点击「+ 选择原料」添加">
            <el-table-column type="index" width="40" />
            <el-table-column label="编码" width="90"><template #default="{ row }"><span class="mono">{{ row.ingredientId }}</span></template></el-table-column>
            <el-table-column label="原料" min-width="170">
              <template #default="{ row, $index }">
                <el-autocomplete
                  v-model="row.inputText"
                  value-key="ingredientId"
                  :fetch-suggestions="(q, cb) => searchIngredients(q, $index, cb)"
                  :disabled="form.status==='approved'"
                  placeholder="输入编码/名称/拼音..."
                  size="small"
                  style="width:100%"
                  trigger-on-focus
                  @select="(item) => onIngSelect($index, item)"
                  @keyup.enter="onEnterIngredient($index, $event)"
                  @keyup.enter.native="onEnterIngredient($index, $event)"
                >
                  <template #default="{ item }">
                    <div class="ing-suggest">
                      <span class="ing-suggest-code">{{ item.ingredientId }}</span>
                      <span class="ing-suggest-name">{{ item.ingredientName }}</span>
                      <span class="ing-suggest-cat">{{ item.ingredientCategory }}</span>
                      <span class="ing-suggest-stock">库存:{{ item.currentStock }}{{ item.purchaseUnit }}</span>
                    </div>
                  </template>
                </el-autocomplete>
              </template>
            </el-table-column>
            <el-table-column label="单位" width="60"><template #default="{ row }">{{ row.unit }}</template></el-table-column>
            <el-table-column label="数量" width="105">
              <template #default="{ row, $index }">
                <el-input-number v-model="row.quantity" :min="0" :precision="3" size="small" controls-position="right" style="width:90px" :disabled="form.status==='approved'" @change="() => calc($index)" @keyup.enter="onQtyEnter($index)" />
              </template>
            </el-table-column>
            <el-table-column label="单价" width="105">
              <template #default="{ row, $index }">
                <el-input-number v-model="row.price" :min="0" :precision="2" size="small" controls-position="right" style="width:90px" :disabled="form.status==='approved'" @change="() => calc($index)" />
              </template>
            </el-table-column>
            <el-table-column label="金额" width="105">
              <template #default="{ row }"><span class="mono">{{ (row.amount||0).toFixed(2) }}</span></template>
            </el-table-column>
            <el-table-column label="备注" min-width="100">
              <template #default="{ row }"><el-input v-model="row.note" size="small" :disabled="form.status==='approved'" /></template>
            </el-table-column>
            <el-table-column label="" width="45" fixed="right">
              <template #default="{ $index }">
                <el-button link size="small" type="danger" :disabled="form.status==='approved'" @click="removeItem($index)">✕</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div style="margin:6px 0; display:flex; gap:8px">
          <el-button size="small" type="primary" plain :disabled="form.status==='approved'" @click="addRow">+ 新增行</el-button>
          <el-button size="small" type="success" plain :disabled="form.status==='approved'" @click="openAddIngredient">+ 新建原料档案</el-button>
        </div>

        <div class="pd-footer-summary">
          <span>合计数量：<strong>{{ sumQty.toFixed(3) }}</strong></span>
          <span>合计金额：<strong>¥{{ sumAmount.toFixed(2) }}</strong></span>
          <span>大写：{{ sumChinese }}</span>
        </div>
        <div class="pd-footer-double">
          <div class="pd-footer-sign-left">
            <span>制单人：</span><u class="pd-underline">{{ operator }}</u>
          </div>
          <div class="pd-footer-sign-right">
            <span>仓库签收：</span><u class="pd-underline">&emsp;&emsp;&emsp;&emsp;</u>
          </div>
        </div>
        <div class="pd-footer-double" style="margin-top:4px">
          <div class="pd-footer-sign-left">
            <span>部门审批：</span><u class="pd-underline">&emsp;&emsp;&emsp;&emsp;</u>
          </div>
          <div class="pd-footer-sign-right">
            <span>财务确认：</span><u class="pd-underline">&emsp;&emsp;&emsp;&emsp;</u>
          </div>
        </div>
        <div class="pd-footer-user">
          <span>制单时间：{{ now }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="showDialog=false">关闭</el-button>
        <el-button type="primary" :disabled="!canSave" @click="savePurchase">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 添加新原料弹窗 -->
    <el-dialog v-model="showAddIngredient" title="添加新原料" width="480px">
      <el-form :model="newIng" label-width="80px" size="small">
        <el-form-item label="原料编码" required>
          <el-input v-model="newIng.ingredientId" placeholder="如 YL006" />
        </el-form-item>
        <el-form-item label="原料名称" required>
          <el-input v-model="newIng.ingredientName" placeholder="如 鲜鸡蛋" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="newIng.ingredientCategory" filterable placeholder="选择或输入" allow-create style="width:100%">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="采购单位">
          <el-input v-model="newIng.purchaseUnit" placeholder="如 斤、只、箱" />
        </el-form-item>
        <el-form-item label="基准单价">
          <el-input-number v-model="newIng.avgPrice" :min="0" :precision="2" controls-position="right" />
        </el-form-item>
        <el-form-item label="预警阈值">
          <el-input-number v-model="newIng.warningThreshold" :min="0" :precision="3" controls-position="right" />
        </el-form-item>
        <el-form-item label="供应商">
          <el-select v-model="newIng.primarySupplierId" filterable placeholder="选择" style="width:100%">
            <el-option v-for="s in suppliers" :key="s.supplierId" :label="s.supplierName" :value="s.supplierId" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddIngredient=false">取消</el-button>
        <el-button type="primary" @click="doAddIngredient">确认添加</el-button>
      </template>
    </el-dialog>

    <!-- 选原料弹窗 -->
    <el-dialog v-model="showProductPicker" title="选择原料" width="550px">
      <el-input v-model="pickerSearch" placeholder="搜索原料" size="small" style="margin-bottom:10px" clearable @input="filterPicker" />
      <el-table :data="pickerList" border stripe size="small" max-height="300" @row-click="pickProduct">
        <el-table-column prop="ingredientId" label="编码" width="90" />
        <el-table-column prop="ingredientName" label="名称" width="140" />
        <el-table-column prop="ingredientCategory" label="分类" width="80" />
        <el-table-column prop="purchaseUnit" label="单位" width="60" />
        <el-table-column prop="currentStock" label="库存" width="80" />
      </el-table>
      <template #footer><el-button @click="showProductPicker=false">取消</el-button></template>
    </el-dialog>

    <!-- 入库验收弹窗 -->
    <el-dialog v-model="showReceiveDialog" title="验收入库" width="500px">
      <el-form v-if="receiveForm" :model="receiveForm" label-width="90px">
        <el-form-item label="原料">
          <el-input :model-value="receiveForm.ingredientName" disabled />
        </el-form-item>
        <el-form-item label="供应商">
          <el-input :model-value="supplierName(receiveForm.supplierId)" disabled />
        </el-form-item>
        <el-form-item label="申请数量">
          <el-input :model-value="receiveForm.orderQuantity" disabled />
        </el-form-item>
        <el-form-item label="实收数量" required>
          <el-input-number v-model="receiveForm.actualQuantity" :min="0" :precision="3" style="width:100%" />
        </el-form-item>
        <el-form-item label="实收单价" required>
          <el-input-number v-model="receiveForm.unitPrice" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="送货人">
          <el-input v-model="receiveForm.deliveryPerson" placeholder="选填" />
        </el-form-item>
        <el-form-item label="验收人">
          <el-input :model-value="operator" disabled />
        </el-form-item>
        <el-form-item label="质量状态">
          <el-select v-model="receiveForm.qualityStatus" style="width:100%">
            <el-option label="合格" value="QUALIFIED" />
            <el-option label="部分合格" value="PARTIAL" />
            <el-option label="不合格" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="receiveForm.remark" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showReceiveDialog = false">取消</el-button>
        <el-button type="primary" :loading="receiving" @click="confirmReceive">确认入库</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getIngredients, createIngredient, getSuppliers, getStaffList,
  getPurchaseRecords, getPurchaseRecord, createPurchase, updatePurchase, auditPurchase, deletePurchase
} from '@/api/booking'
import { useUserStore } from '@/store/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import request from '@/utils/request'
import { pinyin } from 'pinyin-pro'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const operator = computed(() => userStore.userInfo?.staffName || '系统管理员')
const currentStoreId = computed(() => userStore.currentStore?.storeId || userStore.stores?.[0]?.storeId || 1)
const now = ref(dayjs().format('YYYY-MM-DD HH:mm:ss'))

// 列表
const loading = ref(false); const list = ref([]); const suppliers = ref([]); const staffList = ref([])
const ingredients = ref([]); const keyword = ref(''); const dateRange = ref([])

function supplierName(id) { const s = suppliers.value.find(x => x.supplierId === id); return s?.supplierName || '-' }
// 后端 toDTO 从不回填 ingredientName，列表这里按 ingredientId 反查已加载的原料清单
function ingredientName(id) { const i = ingredients.value.find(x => x.ingredientId === id); return i?.ingredientName || id || '-' }
function formatDate(date) { if (!date) return '-'; return String(date).slice(0, 10) }

// 弹窗
const showDialog = ref(false); const showProductPicker = ref(false); const showAddIngredient = ref(false)
const pickerSearch = ref(''); const pickerList = ref([])
const editingId = ref(null)

const categories = ref(['海鲜水产','肉类','禽类','粮油','蔬菜','调味品','干货','冻品','饮品','其他']) 
const newIng = ref({ ingredientId: '', ingredientName: '', ingredientCategory: '', purchaseUnit: '', avgPrice: 0, warningThreshold: 0, primarySupplierId: null })

const emptyForm = () => ({
  orderNo: '', supplierId: null, contactPerson: '', contactPhone: '', handlerId: null,
  warehouse: '原料仓库', remark: '', orderDate: dayjs().format('YYYY-MM-DD'), status: 'pending',
  items: [],
})

const form = reactive(emptyForm())
const dialogTitle = computed(() => editingId.value ? '采购申请单 - 编辑' : '采购申请单 - 录入')

const sumQty = computed(() => form.items.reduce((s, i) => s + (i.quantity || 0), 0))
const sumAmount = computed(() => form.items.reduce((s, i) => s + (i.amount || 0), 0))
const sumChinese = computed(() => numToChinese(sumAmount.value))

const canSave = computed(() => {
  if (form.status === 'approved') return false
  if (form.items.length === 0) return false
  // 至少有一行选了原料
  return form.items.some(i => i.ingredientId)
})

function numToChinese(n) {
  if (!n || n === 0) return '零元整'
  const d = '零壹贰叁肆伍陆柒捌玖', u = ['', '拾', '佰', '仟', '万', '拾', '佰', '仟', '亿']
  const p = n.toFixed(2).split('.'); let r = ''; let x = parseInt(p[0]); let pos = 0; let zf = false
  while (x > 0) { const t = x % 10; if (t === 0) { if (!zf && pos > 0) { r = '零' + r; zf = true } } else { r = d[t] + (pos % 4 === 0 && pos > 0 ? '万' : '') + r; zf = false } x = Math.floor(x / 10); pos++ }
  if (!r) r = '零'
  r += '元'; const dec = parseInt(p[1])
  if (dec === 0) r += '整'; else r += d[Math.floor(dec / 10)] + '角' + d[dec % 10] + '分'
  return r
}

function resetForm() { Object.assign(form, emptyForm()); editingId.value = null; form.status = 'pending' }

function onSuppChange(id) { const s = suppliers.value.find(x => x.supplierId === id); if (s) { form.contactPerson = s.contactPerson || ''; form.contactPhone = s.contactPhone || '' } }

function onIngSelect(idx, item) {
  const row = form.items[idx]
  if (!row) return
  row.ingredientId = item.ingredientId
  row.unit = item.purchaseUnit || ''
  row.name = item.ingredientName
  row.inputText = item.ingredientName
  row.price = item.avgPrice || 0
  calc(idx)
  // 自动聚焦数量输入框
  nextTick(() => {
    const inputs = document.querySelectorAll('.pd-table-wrap .el-input-number input')
    if (inputs) {
      const qtyInputs = document.querySelectorAll('.pd-table-wrap .el-input-number input')
      for (let i = 0; i < qtyInputs.length; i++) {
        const rowIdx = Math.floor(i / 1) // 每个row有1个el-input-number
        if (i === idx) { qtyInputs[i]?.focus(); qtyInputs[i]?.select(); break }
      }
    }
  })
}

// 原料列回车：如果已匹配则聚焦数量，否则判断是否需要新建
function onEnterIngredient(idx, event) {
  const row = form.items[idx]
  if (!row) return
  // 如果已经匹配到原料 -> 新建下一行，聚焦新行的原料输入
  if (row.ingredientId) {
    const isLast = idx === form.items.length - 1
    if (isLast) addRow()
    // 聚焦下一行（刚新增或已有的下一行）的原料输入
    nextTick(() => {
      const nextIdx = isLast ? form.items.length - 1 : idx + 1
      const table = event.target?.closest('table')
      if (table) {
        const rows = table.querySelectorAll('tbody tr')
        const tr = rows[nextIdx]
        if (tr) {
          const inp = tr.querySelector('.el-autocomplete input') || tr.querySelector('input')
          if (inp) { inp.focus(); inp.select() }
        }
      }
    })
    return
  }
  // 没匹配到 -> 搜索看有没有结果
  const query = (row.inputText || '').trim()
  if (!query) return
  searchIngredients(query, idx, (results) => {
    if (results.length === 0) {
      // 无匹配，提示添加
      ElMessageBox.confirm(`未找到原料「${query}」，是否新建？`, '提示', {
        confirmButtonText: '新建原料',
        cancelButtonText: '取消',
        type: 'info'
      }).then(() => {
        newIng.value = { ingredientId: '', ingredientName: query, ingredientCategory: '', purchaseUnit: '', avgPrice: 0, warningThreshold: 0, primarySupplierId: form.supplierId || null }
        showAddIngredient.value = true
      }).catch(() => {})
    } else if (results.length === 1) {
      // 唯一匹配，直接选中
      onIngSelect(idx, results[0])
    }
  })
}

function calc(idx) { const item = form.items[idx]; if (item) item.amount = (item.quantity || 0) * (item.price || 0) }

function onQtyEnter(idx) {
  // 数量列回车 -> 新增下一行
  if (idx === form.items.length - 1) {
    addRow()
  }
}

function removeItem(idx) { form.items.splice(idx, 1) }

function pickProduct(row) {
  form.items.push({ ingredientId: row.ingredientId, inputText: row.ingredientName, name: row.ingredientName, unit: row.purchaseUnit || '', quantity: 1, price: row.avgPrice || 0, amount: 0, note: '' })
  calc(form.items.length - 1)
  showProductPicker.value = false
}

function filterPicker() { const q = pickerSearch.value.trim().toLowerCase(); if (!q) pickerList.value = [...ingredients.value]; else pickerList.value = ingredients.value.filter(i => i.ingredientName?.toLowerCase().includes(q) || i.ingredientId?.toLowerCase().includes(q)) }

// 拼音模糊搜索 - 支持编码/名称/拼音全拼/拼音首字母
// 第三个可选参数 idx 用于回车搜索
function searchIngredients(query, idxOrCb, cbOrUndef) {
  const cb = typeof idxOrCb === 'function' ? idxOrCb : cbOrUndef
  if (!cb) return
  if (!query || query.trim() === '') { cb(ingredients.value.slice(0, 20)); return }
  const q = query.trim().toLowerCase()
  const scored = ingredients.value.map(i => {
    let score = -1
    const id = i.ingredientId?.toLowerCase() || ''
    const name = i.ingredientName || ''
    const nameLower = name.toLowerCase()
    const py = pinyin(name, { toneType: 'none' }).toLowerCase()
    const pyLower = py.replace(/\s/g, '')
    // 拼音首字母
    let pyFirst = ''
    py.split(' ').forEach(w => { if (w) pyFirst += w[0] })

    if (id === q) score = 100
    else if (id.includes(q)) score = 80
    else if (name === q) score = 60
    else if (nameLower.includes(q)) score = 50
    else if (pyLower === q) score = 70
    else if (pyLower.includes(q)) score = 40
    else if (pyFirst === q) score = 35
    else if (pyFirst.includes(q)) score = 25
    return { ...i, _score: score }
  })
  .filter(x => x._score >= 0)
  .sort((a, b) => b._score - a._score)
  .slice(0, 15)
  cb(scored)
}

function addRow() {
  form.items.push({ ingredientId: '', inputText: '', name: '', unit: '', quantity: 1, price: 0, amount: 0, note: '' })
  // 自动聚焦新行的原料输入
  nextTick(() => {
    const idx = form.items.length - 1
    const table = document.querySelector('.pd-table-wrap table')
    if (table) {
      const rows = table.querySelectorAll('tbody tr')
      if (rows[idx]) {
        const inp = rows[idx].querySelector('.el-autocomplete input') || rows[idx].querySelector('input')
        if (inp) { inp.focus() }
      }
    }
  })
}

function openAddIngredient() {
  newIng.value = { ingredientId: '', ingredientName: '', ingredientCategory: '', purchaseUnit: '', avgPrice: 0, warningThreshold: 0, primarySupplierId: null }
  showAddIngredient.value = true
}

async function doAddIngredient() {
  if (!newIng.value.ingredientId || !newIng.value.ingredientName) { ElMessage.warning('原料编码和名称为必填'); return }
  try {
    const payload = {
      ingredient_id: newIng.value.ingredientId,
      ingredient_name: newIng.value.ingredientName,
      ingredient_category: newIng.value.ingredientCategory,
      purchase_unit: newIng.value.purchaseUnit,
      usage_unit: newIng.value.purchaseUnit,
      primary_supplier_id: newIng.value.primarySupplierId,
      avg_price: newIng.value.avgPrice,
      warning_threshold: newIng.value.warningThreshold,
    }
    const res = await createIngredient(payload)
    if (res.code === 200) {
      ElMessage.success('原料添加成功')
      showAddIngredient.value = false
      // 刷新原料列表
      const r = await getIngredients({})
      if (r.code === 200) { ingredients.value = r.data || []; pickerList.value = [...ingredients.value] }
      // 自动添加到明细
      form.items.push({ ingredientId: newIng.value.ingredientId, name: newIng.value.ingredientName, unit: newIng.value.purchaseUnit || '', quantity: 1, price: newIng.value.avgPrice || 0, amount: 0, note: '' })
      calc(form.items.length - 1)
    }
  } catch (e) { console.error(e) }
}

// 操作
function openNew() { resetForm(); form.orderDate = dayjs().format('YYYY-MM-DD'); now.value = dayjs().format('YYYY-MM-DD HH:mm:ss'); showDialog.value = true }

async function openEdit(row) {
  now.value = dayjs().format('YYYY-MM-DD HH:mm:ss')
  try {
    const res = await getPurchaseRecord(row.purchaseId)
    if (res.code === 200) {
      const d = res.data
      Object.assign(form, {
        orderNo: 'CIN-' + d.purchaseId, supplierId: d.supplierId, orderDate: d.purchaseDate || dayjs().format('YYYY-MM-DD'),
        status: d.status || 'pending', remark: d.notes || '', warehouse: '原料仓库',
        // 采购单没有联系人/经手人这几个字段（数据库和DTO都没有），不编造
        contactPerson: '', contactPhone: '', handlerId: null,
        items: [{ ingredientId: d.ingredientId, name: ingredientName(d.ingredientId), unit: '', quantity: d.quantity || 0, price: d.unitPrice || 0, amount: d.totalAmount || 0, note: '' }]
      })
      editingId.value = d.purchaseId
      onSuppChange(d.supplierId)
      showDialog.value = true
    }
  } catch (e) { console.error(e) }
}

async function savePurchase() {
  if (form.items.length === 0) { ElMessage.warning('请添加产品明细'); return }
  const validItems = form.items.filter(i => i.ingredientId)
  if (validItems.length === 0) { ElMessage.warning('请至少选择一种原料'); return }
  try {
    let savedIds = []
    for (const item of validItems) {
      const qty = item.quantity || 0
      const price = item.price || 0
      const data = {
        storeId: String(currentStoreId.value),
        ingredientId: item.ingredientId,
        supplierId: form.supplierId != null ? String(form.supplierId) : null,
        purchaseDate: form.orderDate,
        quantity: qty,
        unitPrice: price,
        totalAmount: qty * price,
        notes: form.remark
      }
      let res
      if (editingId.value && savedIds.length === 0) {
        res = await updatePurchase(editingId.value, data)
      } else {
        res = await createPurchase(data)
      }
      if (res.code === 200 && res.data?.purchaseId) savedIds.push(res.data.purchaseId)
    }
    if (savedIds.length > 0) {
      ElMessage.success(`保存成功 (${savedIds.length}条)`)
      editingId.value = savedIds[0]
      form.orderNo = 'CIN-' + savedIds[0]
      fetchData()
    }
  } catch (e) { console.error(e) }
}

async function doAudit() {
  if (!editingId.value) { ElMessage.warning('请先保存'); return }
  const res = await auditPurchase(editingId.value)
  if (res.code === 200) { ElMessage.success('审核通过，可在列表中"验收入库"确认收货后计入库存'); form.status = 'approved'; fetchData() }
}

async function handleAudit(row) {
  try { await ElMessageBox.confirm('确认审核通过该采购单？审核后可在列表中"验收入库"确认收货入库存。', '审核', { type: 'warning' }); await auditPurchase(row.purchaseId); ElMessage.success('审核通过'); fetchData() } catch (e) { }
}

async function handleDelete(row) {
  try { await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' }); await deletePurchase(row.purchaseId); ElMessage.success('已删除'); fetchData() } catch (e) { }
}

// ── 入库验收 ──
const showReceiveDialog = ref(false)
const receiveForm = ref(null)
const receiving = ref(false)
let receivingSourceRow = null

function openReceive(row) {
  receivingSourceRow = row
  receiveForm.value = {
    ingredientId: row.ingredientId,
    ingredientName: ingredientName(row.ingredientId),
    supplierId: row.supplierId,
    orderQuantity: row.quantity,
    actualQuantity: row.quantity,
    unitPrice: row.unitPrice,
    deliveryPerson: '',
    qualityStatus: 'QUALIFIED',
    remark: '',
  }
  showReceiveDialog.value = true
}

async function confirmReceive() {
  if (!receiveForm.value.actualQuantity || receiveForm.value.actualQuantity <= 0) {
    ElMessage.warning('请输入实收数量')
    return
  }
  receiving.value = true
  try {
    const f = receiveForm.value
    await request.post('/kitchen-supply/goods-receipts', {
      receipt: {
        storeId: currentStoreId.value,
        orderNo: 'PUR-' + receivingSourceRow.purchaseId,
        supplierId: f.supplierId,
        supplierName: supplierName(f.supplierId),
        status: 'ACCEPTED',
        deliveryPerson: f.deliveryPerson,
        warehouseKeeperName: operator.value,
        remark: f.remark,
      },
      items: [{
        ingredientId: f.ingredientId,
        ingredientName: f.ingredientName,
        orderQuantity: f.orderQuantity,
        actualQuantity: f.actualQuantity,
        unitPrice: f.unitPrice,
        amount: f.actualQuantity * f.unitPrice,
        qualityStatus: f.qualityStatus,
        remark: f.remark,
      }]
    })
    ElMessage.success('入库成功，库存已更新')
    showReceiveDialog.value = false
    fetchData()
  } catch (e) {
    console.error('入库失败', e)
    ElMessage.error(e.response?.data?.message || '入库失败')
  } finally {
    receiving.value = false
  }
}

function handleExport() { ElMessage.info('导出功能') }

function handlePrint() {
  const el = document.querySelector('.pd-body')?.parentElement
  if (!el) { ElMessage.info('请先打开申请单'); return }
  const w = window.open('', '_blank', 'width=800,height=600')
  if (!w) { ElMessage.warning('请允许弹出窗口'); return }
  const title = '采购申请单_' + (form.orderNo || '新建')
  const tableEl = document.querySelector('.pd-body .el-table__body-wrapper table')
  let rowsHtml = ''
  if (tableEl) {
    const theadHtml = tableEl.querySelector('thead')?.outerHTML || ''
    const tbodyHtml = tableEl.querySelector('tbody')?.outerHTML || ''
    rowsHtml = theadHtml + tbodyHtml
  }
  const content =
'<html><head><meta charset="utf-8"><title>'+title+'</title>'+
'<style>'+
'body{font-family:SimSun,serif;padding:30px;font-size:14px;max-width:750px;margin:0 auto}'+
'h2{text-align:center;margin-bottom:4px}'+
'h3{text-align:right;font-weight:normal;font-size:12px;margin:0 0 8px}'+
'.info{display:flex;flex-wrap:wrap;gap:8px 20px;margin:12px 0;padding:8px 0;border-top:2px solid #000;border-bottom:1px solid #000}'+
'.info span{min-width:140px}'+
'table{width:100%;border-collapse:collapse;margin:12px 0}'+
'th,td{border:1px solid #000;padding:4px 6px;font-size:12px;text-align:center}'+
'th{background:#f0f0f0}'+
'.sign-area{display:flex;justify-content:space-between;margin-top:30px}'+
'.sign-col{text-align:center;min-width:140px}'+
'.sign-col u{display:inline-block;min-width:100px;border-bottom:1px solid #000}'+
'.sign-label{font-size:12px;margin-bottom:24px}'+
'@media print{body{padding:10px}}'+
'</style></head><body>'+
'<h2>采购申请单</h2>'+
'<h3>编号: '+ (form.orderNo || '(草稿)') +' &nbsp; 状态: '+ (form.status==="approved"?"已审核":"待审核") +'</h3>'+
'<div class="info">'+
'<span>制单日期: '+form.orderDate+'</span>'+
'<span>入库仓库: '+form.warehouse+'</span>'+
'<span>供方名称: '+ (supplierName(form.supplierId)||'-') +'</span>'+
'<span>联系人: '+ (form.contactPerson||'-') +'</span>'+
'<span>电话: '+ (form.contactPhone||'-') +'</span>'+
'<span>经手人: '+ (staffList.value.find(s=>s.staffId===form.handlerId)?.staffName||'-') +'</span>'+
'</div>'+
'<table>'+ (rowsHtml||'<tr><td colspan="7">无明细</td></tr>') +'</table>'+
'<div style="display:flex;justify-content:flex-end;gap:24px;font-weight:bold;margin-top:8px">'+
'<span>合计数量: '+ sumQty.value.toFixed(3) +'</span>'+
'<span>合计金额: ¥'+ sumAmount.value.toFixed(2) +'</span>'+
'<span>大写: '+ sumChinese.value +'</span>'+
'</div>'+
'<div class="sign-area">'+
'<div class="sign-col"><div class="sign-label">制单人</div><br><u>&emsp;'+ operator.value +'&emsp;</u></div>'+
'<div class="sign-col"><div class="sign-label">仓库签收</div><br><u>&emsp;&emsp;&emsp;&emsp;</u></div>'+
'</div>'+
'<div class="sign-area">'+
'<div class="sign-col"><div class="sign-label">部门审批</div><br><u>&emsp;&emsp;&emsp;&emsp;</u></div>'+
'<div class="sign-col"><div class="sign-label">财务确认</div><br><u>&emsp;&emsp;&emsp;&emsp;</u></div>'+
'</div>'+
'<div style="text-align:right;margin-top:16px;color:#999;font-size:11px">打印时间: '+ now.value +'</div>'+
'<script>window.onload=function(){window.print()}<\/script>'+
'</body></html>'
  w.document.write(content)
  w.document.close()
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getPurchaseRecords({})
    if (res.code === 200) list.value = res.data || []
  } catch (e) { console.error(e) } finally { loading.value = false }
}

onMounted(async () => {
  fetchData()
  const [r1, r2, r3] = await Promise.all([
    getIngredients({}), getSuppliers({}), getStaffList({})
  ])
  if (r1.code === 200) { ingredients.value = r1.data || []; pickerList.value = [...ingredients.value] }
  if (r2.code === 200) suppliers.value = r2.data || []
  if (r3.code === 200) staffList.value = r3.data || []
  
  if (route.query.action === 'new') {
    nextTick(() => {
      openNew()
    })
  }
})

watch(() => route.query.action, (newVal) => {
  if (newVal === 'new') {
    nextTick(() => {
      openNew()
    })
  }
})
</script>

<style scoped>
.page { width:100%; }
.page-header { display:flex; align-items:center; gap:12px; margin-bottom:12px; }
.page-header h2 { font-size:18px; font-weight:600; margin:0; }
.page-desc { font-size:13px; color:#64748b; margin:0; }
.toolbar { display:flex; justify-content:space-between; align-items:center; margin-bottom:12px; }
.toolbar-left, .toolbar-right { display:flex; gap:8px; align-items:center; }
.search-box { width:200px; }
.mono { font-family:'Courier New',monospace; }
:deep(.el-table) { width:100%; }

.pd-body { font-size:13px; }
.pd-topbar { display:flex; justify-content:space-between; align-items:center; margin-bottom:10px; padding-bottom:8px; border-bottom:1px solid #e5e7eb; }
.pd-tag { padding:2px 10px; border-radius:3px; font-size:12px; font-weight:600; }
.pd-tag.pending { background:#fff7e6; color:#d46b08; border:1px solid #ffd591; }
.pd-tag.approved { background:#f6ffed; color:#389e0d; border:1px solid #b7eb8f; }
.pd-btns { display:flex; gap:4px; }
.pd-row { display:flex; gap:10px; margin-bottom:8px; }
.pd-field { flex:1; min-width:0; display:flex; align-items:center; gap:4px; }
.pd-field label { font-size:12px; color:#6b7280; white-space:nowrap; width:60px; text-align:right; flex-shrink:0; }
.pd-field .pd-val { color:#111827; }
.pd-section { font-weight:600; font-size:13px; margin:10px 0 4px; color:#374151; }
.pd-table-wrap { border:1px solid #e5e7eb; border-radius:4px; }
.pd-footer-summary { display:flex; gap:20px; margin-top:10px; padding:8px 0; border-top:1px solid #e5e7eb; }
.pd-footer-user { font-size:12px; color:#9ca3af; margin-top:4px; }
.pd-footer-double { display:flex; justify-content:space-between; margin-top:8px; padding-top:8px; border-top:1px dashed #ccc; font-size:13px; }
.pd-footer-sign-left, .pd-footer-sign-right { min-width:200px; }
.pd-underline { text-underline-offset:4px; }

:deep(.el-input__wrapper) { border-radius:2px; box-shadow:0 0 0 1px #d9d9d9 inset !important; }
:deep(.el-select) { width:100%; }
:deep(.el-date-editor) { width:100%; }
:deep(.el-table th.el-table__cell) { background:#fafafa; color:#374151; font-weight:600; font-size:12px; }
:deep(.el-input-number--small .el-input-number__increase),
:deep(.el-input-number--small .el-input-number__decrease) { display:none; }

.ing-suggest { display:flex; align-items:center; gap:8px; font-size:12px; line-height:28px; }
.ing-suggest-code { color:#999; width:55px; font-family:monospace; }
.ing-suggest-name { font-weight:600; min-width:70px; }
.ing-suggest-cat { color:#6b7280; font-size:11px; }
.ing-suggest-stock { margin-left:auto; color:#9ca3af; font-size:11px; }
</style>
