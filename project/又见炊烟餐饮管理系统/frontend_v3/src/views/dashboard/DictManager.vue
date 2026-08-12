<template>
  <div class="dict-manager">
    <div class="page-header">
      <h2 class="page-title">
        <span class="title-cn">数据字典管理</span>
        <span class="title-en">· Data Dictionary</span>
      </h2>
      <div class="page-actions">
        <el-select v-model="filterStoreId" placeholder="门店筛选" clearable @change="loadData" style="width: 160px">
          <el-option label="全部门店" :value="0" />
          <el-option v-for="s in storeList" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
        <el-button type="primary" @click="showAddDict = true">
          新建字典类型
        </el-button>
      </div>
    </div>

    <!-- 字典类型卡片 -->
    <div class="dict-grid">
      <div
        v-for="dict in dictTypes"
        :key="dict.dict_id"
        :class="['dict-card', { expanded: expandedDict === dict.dict_id }]"
      >
        <div class="dict-card-header" @click="toggleDict(dict.dict_id)">
          <div class="dict-card-info">
            <div class="dict-card-code">{{ dict.dict_code }}</div>
            <div class="dict-card-name">{{ dict.dict_name }}</div>
          </div>
          <div class="dict-card-meta">
            <el-tag size="small" v-if="dict.store_id === 0" type="info">全局</el-tag>
            <el-tag size="small" v-else :type="dict.store_id === currentStoreId ? 'success' : 'warning'">
              {{ getStoreName(dict.store_id) }}
            </el-tag>
            <span class="dict-card-count">{{ getItemCount(dict.dict_id) }} 项</span>
          </div>
        </div>

        <div v-if="expandedDict === dict.dict_id" class="dict-card-body">
          <div class="dict-item-header">
            <span class="dict-item-title">字典项</span>
            <el-button size="small" type="primary" plain @click="openAddItem(dict)">
              + 添加条目
            </el-button>
          </div>
          <el-table :data="getDictItems(dict.dict_id)" stripe size="small" max-height="300">
            <el-table-column prop="item_value" label="编码" width="100" />
            <el-table-column prop="item_label" label="名称" width="160" />
            <el-table-column prop="store_id" label="门店" width="100">
              <template #default="{ row }">
                <el-tag size="small" v-if="row.store_id === 0" type="info">全局</el-tag>
                <span v-else>{{ getStoreName(row.store_id) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="sort_order" label="排序" width="70" />
            <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="editItem(row)">编辑</el-button>
                <el-button link type="danger" size="small" @click="deleteItem(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>

    <!-- 新增/编辑字典类型对话框 -->
    <el-dialog v-model="showAddDict" :title="editingDict ? '编辑字典类型' : '新建字典类型'" width="520px">
      <el-form :model="dictForm" label-width="100px">
        <el-form-item label="字典编码">
          <el-input v-model="dictForm.dict_code" placeholder="如: dish_category" />
        </el-form-item>
        <el-form-item label="字典名称">
          <el-input v-model="dictForm.dict_name" placeholder="如: 菜品分类" />
        </el-form-item>
        <el-form-item label="门店">
          <el-select v-model="dictForm.store_id" placeholder="选择门店">
            <el-option label="全局（所有门店共享）" :value="0" />
            <el-option v-for="s in storeList" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="dictForm.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDict = false">取消</el-button>
        <el-button type="primary" @click="saveDict">保存</el-button>
      </template>
    </el-dialog>

    <!-- 新增/编辑字典项对话框 -->
    <el-dialog v-model="showAddItem" :title="editingItemId ? '编辑字典项' : '新建字典项'" width="520px">
      <el-form :model="itemForm" label-width="100px">
        <el-form-item label="所属字典">
          <el-input :value="currentDictName" disabled />
        </el-form-item>
        <el-form-item label="条目编码">
          <el-input v-model="itemForm.item_value" placeholder="如: 01" />
        </el-form-item>
        <el-form-item label="条目名称">
          <el-input v-model="itemForm.item_label" placeholder="如: 热菜" />
        </el-form-item>
        <el-form-item label="门店">
          <el-select v-model="itemForm.store_id" placeholder="选择门店">
            <el-option label="全局（所有门店共享）" :value="0" />
            <el-option v-for="s in storeList" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="itemForm.sort_order" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="itemForm.remark" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddItem = false">取消</el-button>
        <el-button type="primary" @click="saveItem">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const userStore = useUserStore()
const currentStoreId = computed(() => userStore.storeId)

const storeList = ref([])
const filterStoreId = ref(null)
const expandedDict = ref(null)
const dictTypes = ref([])
const allDictItems = ref([])

const showAddDict = ref(false)
const showAddItem = ref(false)
const editingDict = ref(null)
const editingItemId = ref(null)
const currentDictName = ref('')

const dictForm = ref({ dict_code: '', dict_name: '', store_id: 0, description: '' })
const itemForm = ref({ item_value: '', item_label: '', store_id: 0, sort_order: 0, remark: '' })
const currentAddDictId = ref(null)

async function loadData() {
  try {
    const storeId = filterStoreId.value
    let url = '/sys-dict?'
    if (storeId) url += `store_id=${storeId}`
    const res = await request({ url, method: 'get' })
    if (res.code === 200 && res.data) {
      dictTypes.value = res.data.dicts || []
      allDictItems.value = res.data.items || []
    }
  } catch {
    ElMessage.error('加载字典数据失败')
  }
}

async function loadStores() {
  try {
    const res = await request({ url: '/stores', method: 'get' })
    if (res.code === 200 && res.data?.length) {
      storeList.value = res.data
    }
  } catch {}
}

function getStoreName(sid) {
  if (sid === 0) return '全局'
  const s = storeList.value.find(x => x.id === sid)
  return s ? s.name : `门店${sid}`
}

function getDictItems(dictId) {
  return allDictItems.value.filter(i => i.dict_id === dictId)
}

function getItemCount(dictId) {
  return getDictItems(dictId).length
}

function toggleDict(dictId) {
  expandedDict.value = expandedDict.value === dictId ? null : dictId
}

function openAddItem(dict) {
  currentAddDictId.value = dict.dict_id
  currentDictName.value = `${dict.dict_code} (${dict.dict_name})`
  editingItemId.value = null
  itemForm.value = { item_value: '', item_label: '', store_id: dict.store_id, sort_order: 0, remark: '' }
  showAddItem.value = true
}

function editItem(row) {
  currentDictName.value = `字典ID: ${row.dict_id}`
  editingItemId.value = row.item_id
  itemForm.value = {
    item_value: row.item_value,
    item_label: row.item_label,
    store_id: row.store_id || 0,
    sort_order: row.sort_order || 0,
    remark: row.remark || ''
  }
  showAddItem.value = true
}

async function deleteItem(row) {
  try {
    await ElMessageBox.confirm(`确定删除 "${row.item_label}"？`, '确认删除', { type: 'warning' })
    const res = await request({ url: `/sys-dict/item/${row.item_id}`, method: 'delete' })
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadData()
    }
  } catch {}
}

async function saveDict() {
  try {
    const data = { ...dictForm.value }
    const method = editingDict.value ? 'put' : 'post'
    const url = editingDict.value ? `/sys-dict/${editingDict.value.dict_id}` : '/sys-dict'
    const res = await request({ url, method, data })
    if (res.code === 200) {
      ElMessage.success(editingDict.value ? '更新成功' : '创建成功')
      showAddDict.value = false
      editingDict.value = null
      dictForm.value = { dict_code: '', dict_name: '', store_id: 0, description: '' }
      loadData()
    }
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

async function saveItem() {
  try {
    const data = { ...itemForm.value, dict_id: currentAddDictId.value }
    const method = editingItemId.value ? 'put' : 'post'
    const url = editingItemId.value ? `/sys-dict/item/${editingItemId.value}` : '/sys-dict/item'
    const res = await request({ url, method, data })
    if (res.code === 200) {
      ElMessage.success(editingItemId.value ? '更新成功' : '创建成功')
      showAddItem.value = false
      editingItemId.value = null
      loadData()
    }
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

onMounted(() => {
  loadStores()
  loadData()
})
</script>

<style scoped>
.dict-manager { padding: 0; }

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-title { margin: 0; display: flex; align-items: baseline; gap: 8px; }
.title-cn { font-size: 20px; font-weight: 700; color: var(--color-text); }
.title-en { font-size: 13px; color: var(--color-text-muted); }

.page-actions { display: flex; align-items: center; gap: 12px; }

.dict-grid { display: flex; flex-direction: column; gap: 12px; }

.dict-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  overflow: hidden;
  transition: all 0.25s;
}

.dict-card:hover { box-shadow: 0 2px 12px rgba(0,0,0,0.06); }

.dict-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  cursor: pointer;
  user-select: none;
  transition: background 0.2s;
}

.dict-card-header:hover { background: rgba(45, 74, 62, 0.04); }

.dict-card-info { display: flex; align-items: center; gap: 12px; }
.dict-card-code { font-family: monospace; font-size: 13px; font-weight: 600; color: var(--color-primary); background: rgba(45,74,62,0.06); padding: 2px 10px; border-radius: 4px; }
.dict-card-name { font-size: 15px; font-weight: 600; color: var(--color-text); }

.dict-card-meta { display: flex; align-items: center; gap: 12px; }
.dict-card-count { font-size: 12px; color: var(--color-text-muted); }
.dict-card-arrow { transition: transform 0.25s; color: var(--color-text-muted); }
.dict-card-arrow.rotated { transform: rotate(180deg); }

.dict-card-body { padding: 0 20px 16px; border-top: 1px solid var(--color-border); }

.dict-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
}
.dict-item-title { font-size: 13px; font-weight: 600; color: var(--color-text-secondary); }
</style>
