<template>
  <div class="menu-page">
    <div class="page-header">
      <h2>菜品管理 · Menu Management</h2>
      <p class="page-desc">菜品信息 · 定价 · 吉庆名 · 成本卡</p>
    </div>

    <div class="toolbar">
      <el-select v-model="catFilter" placeholder="全部分类" clearable size="small" style="width:140px">
        <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
      </el-select>
      <el-input v-model="search" placeholder="搜索菜品" size="small" clearable style="width:180px" />
      <span style="flex:1" />
      <el-button type="primary" size="small" @click="openNew">+ 录入新菜品</el-button>
    </div>

    <!-- 菜品列表 -->
    <el-table :data="filtered" stripe size="small" max-height="calc(100vh - 260px)" row-key="dishId"
      @row-click="openEdit">
      <el-table-column label="图片" width="60">
        <template #default="{ row }">
          <img v-if="row.imageUrl" :src="row.imageUrl" class="dish-thumb" @error="e=>e.target.style.display='none'" />
          <span v-else class="no-img">🈚</span>
        </template>
      </el-table-column>
      <el-table-column prop="dishId" label="编号" width="85" />
      <el-table-column prop="dishName" label="名称" min-width="130" />
      <el-table-column prop="dishCategory" label="分类" width="85" />
      <el-table-column prop="mainIngredientType" label="主料类别" width="75" />
      <el-table-column prop="mainIngredient" label="主料" width="80" />
      <el-table-column label="辣度" width="55">
        <template #default="{ row }">{{ '🌶'.repeat(row.spicyLevel||0) || '-' }}</template>
      </el-table-column>
      <el-table-column label="成本" width="75">
        <template #default="{ row }">¥{{ (row.costPrice||0).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="售价" width="75">
        <template #default="{ row }">¥{{ (row.salePrice||0).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="成本率" width="70">
        <template #default="{ row }">{{ (row.costRate||0).toFixed(1) }}%</template>
      </el-table-column>
      <el-table-column label="成本卡" width="75" fixed="right">
        <template #default="{ row }">
          <el-button link size="small" type="primary" @click.stop="goRecipe(row)">🧾 配方</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 菜品录入/编辑弹窗 -->
    <el-dialog v-model="showDlg" :title="isEdit?'编辑菜品':'录入新菜品'" width="820px" destroy-on-close>
      <el-tabs v-model="tabActive">
        <el-tab-pane label="基本信息" name="base">
          <el-form :model="form" label-width="85px" size="small">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="菜品名称" required>
                  <el-input v-model="form.dishName" placeholder="如：油淋酱椒大黄鱼" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="菜品分类" required>
                  <el-select v-model="form.dishCategory" filterable allow-create placeholder="选择或输入分类" style="width:100%">
                    <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="英文名称">
                  <el-input v-model="form.englishName" placeholder="如：Yellow Croaker with Pepper" />
                </el-form-item>
              </el-col>
              <el-col :span="6">
                <el-form-item label="辣度">
                  <el-rate v-model="form.spicyLevel" :max="3" show-text :texts="['不辣','微辣','中辣','特辣']" />
                </el-form-item>
              </el-col>
              <el-col :span="6">
                <el-form-item label="出菜时长(分)">
                  <el-input-number v-model="form.cookingTime" :min="1" :max="120" controls-position="right" style="width:100%" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="主料类别">
                  <el-input v-model="form.mainIngredientType" placeholder="如：海鲜、肉类、禽类" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="主料">
                  <el-input v-model="form.mainIngredient" placeholder="如：大黄鱼-酱椒" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="售价(元)">
                  <el-input-number v-model="form.salePrice" :min="0" :precision="0" controls-position="right" style="width:100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="成本(元)">
                  <el-input v-model="form.costPrice" disabled placeholder="从成本卡计算" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="成本率">
                  <el-input :value="costRateStr" disabled />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="菜品图片">
              <el-input v-model="form.imageUrl" placeholder="图片URL地址，如 https://img.example.com/dish.jpg" />
            </el-form-item>
            <div v-if="form.imageUrl" style="margin-left:85px;margin-bottom:12px">
              <img :src="form.imageUrl" style="max-width:200px;max-height:150px;border-radius:6px;border:1px solid #e5e7eb" @error="e=>e.target.style.display='none'" />
            </div>
            <el-form-item label="菜肴介绍" style="margin-bottom:16px">
              <el-input
                v-model="form.dishIntro"
                type="textarea"
                :autosize="{ minRows: 3, maxRows: 6 }"
                placeholder="请输入菜肴介绍，如：选用深海野生大黄鱼，配以秘制酱椒，肉质鲜美，香辣开胃..."
                style="width:50%"
              />
            </el-form-item>
            <el-form-item label="抖音推荐" style="margin-bottom:8px">
              <el-input
                v-model="form.tiktokRecommend"
                type="textarea"
                :autosize="{ minRows: 2, maxRows: 4 }"
                placeholder="请输入抖音推荐语，如：深海野生大黄鱼，肉质鲜嫩，配秘制酱椒，一口回味无穷！"
                style="width:50%"
              />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="吉庆名" name="blessing">
          <el-form :model="form" label-width="85px" size="small">
            <el-form-item label="吉庆名">
              <el-input v-model="form.festiveName" placeholder="如：福寿有余笑颜开" />
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <el-button @click="showDlg=false">取消</el-button>
        <el-button type="primary" @click="doSave">保存菜品</el-button>
        <el-button type="success" v-if="isEdit" @click="goRecipeAfterSave">保存并编辑成本卡 →</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getDishes, createDish, updateDish } from '@/api/booking'

const router = useRouter()
const dishes = ref([])
const categories = ref([])
const search = ref('')
const catFilter = ref('')
const showDlg = ref(false)
const isEdit = ref(false)
const tabActive = ref('base')
const editingId = ref(null)

const form = ref(initForm())

const filtered = computed(() => {
  let arr = dishes.value
  const q = search.value.trim().toLowerCase()
  if (q) arr = arr.filter(d => d.dishName?.toLowerCase().includes(q) || d.dishId?.toLowerCase().includes(q))
  if (catFilter.value) arr = arr.filter(d => d.dishCategory === catFilter.value)
  return arr
})

const costRateStr = computed(() => {
  const c = parseFloat(form.value.costPrice) || 0
  const s = parseFloat(form.value.salePrice) || 0
  return s > 0 ? (c / s * 100).toFixed(1) + '%' : '-'
})

function initForm() {
  return {
    dishName: '', dishCategory: '', spicyLevel: 0, mainIngredientType: '', mainIngredient: '',
    englishName: '', costPrice: '0', salePrice: 0, cookingTime: 20, imageUrl: '',
    festiveName: '', dishIntro: '', tiktokRecommend: ''
  }
}

function openNew() {
  form.value = initForm()
  isEdit.value = false
  editingId.value = null
  tabActive.value = 'base'
  showDlg.value = true
}

async function openEdit(row) {
  form.value = {
    dishName: row.dishName || '', dishCategory: row.dishCategory || '', spicyLevel: row.spicyLevel || 0,
    mainIngredientType: row.mainIngredientType || '', mainIngredient: row.mainIngredient || '',
    englishName: row.englishName || '', costPrice: row.costPrice || '0', salePrice: row.salePrice || 0,
    cookingTime: row.cookingTime || 20, imageUrl: row.imageUrl || '',
    festiveName: row.festiveName || '', dishIntro: row.dishIntro || '', tiktokRecommend: row.tiktokRecommend || ''
  }
  isEdit.value = true
  editingId.value = row.dishId
  tabActive.value = 'base'
  showDlg.value = true
}

async function doSave() {
  if (!form.value.dishName || !form.value.dishCategory) { ElMessage.warning('名称和分类为必填'); return }
  try {
    const payload = { ...form.value }
    let res
    if (isEdit.value) {
      res = await updateDish(editingId.value, payload)
    } else {
      res = await createDish(payload)
    }
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '更新成功' : '新菜品录入成功')
      showDlg.value = false
      await fetchData()
      // 新建的菜品：弹窗提示跳成本卡
      if (!isEdit.value && res.data) {
        const dishId = res.data.dishId || (res.data.dishMaster?.dishId)
        if (dishId) {
          nextTick(() => {
            ElMessageBox.confirm('菜品已录入，是否立即编辑成本卡？', '提示', {
              confirmButtonText: '去配成本', cancelButtonText: '稍后', type: 'info'
            }).then(() => router.push({ path: '/dashboard/dish-cost', query: { dish: dishId } })).catch(() => {})
          })
        }
      }
    }
  } catch (e) { console.error(e) }
}

async function goRecipeAfterSave() {
  showDlg.value = false
  router.push({ path: '/dashboard/dish-cost', query: { dish: editingId.value } })
}

function goRecipe(row) { router.push({ path: '/dashboard/dish-cost', query: { dish: row.dishId } }) }

async function fetchData() {
  try {
    const res = await getDishes({})
    if (res.code === 200) {
      dishes.value = res.data || []
      categories.value = [...new Set(dishes.value.map(d => d.dishCategory).filter(Boolean))].sort()
    }
  } catch (e) { console.error(e) }
}

onMounted(fetchData)
</script>

<script>
import { ElMessageBox } from 'element-plus'
</script>

<style scoped>
.menu-page { padding: 16px; height: 100%; }
.page-header { margin-bottom: 10px; }
.page-header h2 { font-size: 20px; margin: 0; }
.page-desc { font-size: 12px; color: #9ca3af; margin: 2px 0 0; }
.toolbar { display: flex; gap: 10px; align-items: center; margin-bottom: 10px; }
.dish-thumb { width: 40px; height: 40px; object-fit: cover; border-radius: 4px; }
.no-img { color: #d1d5db; font-size: 18px; }
</style>
