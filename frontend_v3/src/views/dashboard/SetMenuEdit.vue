<template>
  <div class="set-menu-edit-page">
    <!-- 顶部操作栏 -->
    <div class="edit-header">
      <div class="header-left">
        <el-button @click="goBack" class="back-btn">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="15 18 9 12 15 6"/>
          </svg>
          返回
        </el-button>
        <h2 class="page-title">{{ isEdit ? '编辑套餐' : '新建套餐' }} · {{ isEdit ? 'Edit Package' : 'New Package' }}</h2>
      </div>
      <div class="header-right">
        <el-button @click="resetForm">重置</el-button>
        <el-button type="primary" @click="savePackage" :loading="saving">
          {{ isEdit ? '保存修改' : '创建套餐' }}
        </el-button>
      </div>
    </div>

    <!-- 三栏布局 -->
    <div class="three-column-layout">
      <!-- 左栏：套餐信息 -->
      <div class="col-left">
        <div class="col-title">套餐信息 · Package Info</div>
        <div class="form-section">
          <!-- 套餐编号 -->
          <div class="form-group">
            <label class="form-label">套餐编号</label>
            <el-input v-model="form.packageId" readonly class="locked-input" />
          </div>

          <!-- 录入时间 -->
          <div class="form-group">
            <label class="form-label">录入时间</label>
            <el-input v-model="form.createdAt" readonly class="locked-input" placeholder="打开即生成" />
          </div>

          <!-- 套餐名称 -->
          <div class="form-group" :class="{ 'field-warning': auditErrors.name }">
            <label class="form-label">套餐名称 <span class="required">*</span></label>
            <el-input v-model="form.packageName" placeholder="请输入套餐名称" maxlength="50" show-word-limit />
          </div>

          <!-- 英文名 -->
          <div class="form-group">
            <label class="form-label">英文名</label>
            <el-input v-model="form.englishName" placeholder="English name (optional)" />
          </div>

          <!-- 使用地点 -->
          <div class="form-group" :class="{ 'field-warning': auditErrors.location }">
            <label class="form-label">使用地点 <span class="required">*</span></label>
            <el-checkbox-group v-model="form.location">
              <el-checkbox label="宁国">宁国</el-checkbox>
              <el-checkbox label="宣城">宣城</el-checkbox>
            </el-checkbox-group>
          </div>

          <!-- 宴会分类 -->
          <div class="form-group" :class="{ 'field-warning': auditErrors.category }">
            <label class="form-label">宴会分类 <span class="required">*</span></label>
            <el-select v-model="form.category" placeholder="选择分类" class="full-width">
              <el-option v-for="cat in categories" :key="cat" :label="cat" :value="cat" />
            </el-select>
          </div>

          <!-- 人数（紧挨着价格信息之上） -->
          <div class="form-group">
            <label class="form-label">建议人数</label>
            <div class="guest-range">
              <el-input-number v-model="form.minGuests" :min="1" :max="form.maxGuests" controls-position="right" @change="onMinChange" />
              <span class="range-sep">至</span>
              <el-input-number v-model="form.maxGuests" :min="form.minGuests" :max="30" controls-position="right" @change="onMaxChange" />
            </div>
          </div>

          <!-- 价格区（人数的下一块，紧挨着） -->
          <div class="price-section">
            <div class="section-divider">价格信息 · Pricing</div>

            <div class="form-group">
              <label class="form-label">售价 (¥)</label>
              <el-input-number
                v-model="form.packageTotalPrice"
                :precision="2"
                :min="0"
                :max="999999"
                controls-position="right"
                class="full-width"
              />
              <span class="form-hint">售价请手动填入</span>
            </div>

            <div class="form-group">
              <label class="form-label">原价 (¥)</label>
              <el-input-number
                :model-value="form.originalPrice"
                :precision="2"
                :min="0"
                :max="999999"
                controls-position="right"
                class="full-width"
                readonly
              />
              <span class="form-hint">根据菜品总价自动计算</span>
            </div>

            <div class="form-group">
              <label class="form-label">折扣 (%)</label>
              <el-input-number
                :model-value="computedDiscount"
                :precision="1"
                :min="0"
                :max="200"
                controls-position="right"
                class="full-width"
                readonly
                :class="{ 'premium-discount': computedDiscount > 100 }"
              />
              <span class="form-hint">自动计算 = 售价 / 原价 × 100</span>
            </div>

            <div class="form-group">
              <label class="form-label">人均 (¥)</label>
              <el-input :model-value="perCapita.toFixed(2)" readonly class="locked-input" />
              <span class="form-hint">按人数配置最大值计算</span>
            </div>

            <div class="form-group" v-if="computedDiscount > 100">
              <div class="premium-badge">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
                </svg>
                溢价套餐 ({{ (computedDiscount - 100).toFixed(1) }}% 溢价)
              </div>
            </div>
          </div>

          <!-- 适用时间 -->
          <div class="form-group">
            <label class="form-label">适用时间</label>
            <div class="locked-field" @dblclick="unlockTime" :class="{ unlocked: timeUnlocked }">
              <el-date-picker
                v-if="timeUnlocked"
                v-model="form.validDate"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                class="full-width"
              />
              <div v-else class="lock-hint">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                  <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                </svg>
                <span>长期有效（双击解锁设置时间范围）</span>
              </div>
            </div>
          </div>

          <!-- 制作人（只读显示：打开即抓取当前登录用户） -->
          <div class="form-group">
            <label class="form-label">制作人</label>
            <el-input v-model="form.createdBy" readonly class="locked-input" placeholder="打开即抓取登录用户" />
          </div>

          <!-- 套餐图片 -->
          <div class="form-group">
            <label class="form-label">套餐图片（最多3张，从菜品图选）</label>
            <div class="image-picker">
              <div
                v-for="(img, idx) in form.images"
                :key="idx"
                class="image-thumb"
                @click="removeImage(idx)"
              >
                <img :src="img" />
                <div class="remove-overlay">×</div>
              </div>
              <div
                v-if="form.images.length < 3"
                class="image-add-btn"
                @click="openImagePicker"
              >
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="12" y1="5" x2="12" y2="19"/>
                  <line x1="5" y1="12" x2="19" y2="12"/>
                </svg>
                <span>添加图片</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 中栏：菜品库 -->
      <div class="col-mid">
        <div class="col-title">菜品库 · Dish Library</div>
        
        <!-- 搜索 -->
        <div class="search-bar">
          <el-input
            v-model="dishSearch"
            placeholder="搜索菜品名称 / 分类..."
            clearable
            :prefix-icon="Search"
          />
        </div>

        <!-- 分类tabs -->
        <div class="category-tabs">
          <div
            v-for="cat in dishCategories"
            :key="cat"
            :class="['cat-tab', { active: activeCategory === cat }]"
            @click="activeCategory = cat"
          >
            {{ cat }}
          </div>
        </div>

        <!-- 菜品卡片网格 -->
        <div class="dish-grid" v-loading="dishesLoading">
          <div
            v-for="dish in filteredDishes"
            :key="dish.dishId"
            class="dish-card"
            draggable="true"
            @dragstart="onDishDragStart(dish, $event)"
            @dblclick="previewDishImage(dish)"
          >
            <div class="dish-card-image">
              <img v-if="dish.imageUrl || dish.image" :src="dish.imageUrl || dish.image" :alt="dish.dishName" />
              <div v-else class="dish-card-placeholder">
                <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <rect x="3" y="3" width="18" height="18" rx="2"/>
                  <circle cx="8.5" cy="8.5" r="1.5"/>
                  <polyline points="21 15 16 10 5 21"/>
                </svg>
              </div>
            </div>
            <div class="dish-card-body">
              <div class="dish-card-name">{{ dish.dishName }}</div>
              <div class="dish-card-meta">
                <span class="dish-card-category">{{ dish.categoryName || dish.category }}</span>
                <span class="dish-card-price">¥{{ (dish.salePrice || dish.price || 0).toFixed(0) }}</span>
              </div>
            </div>
            <div class="dish-card-add" @click.stop="addDishToMenu(dish)">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" y1="5" x2="12" y2="19"/>
                <line x1="5" y1="12" x2="19" y2="12"/>
              </svg>
            </div>
          </div>

          <div v-if="filteredDishes.length === 0 && !dishesLoading" class="no-dishes">
            暂无菜品数据
          </div>
        </div>
      </div>

      <!-- 右栏：菜单内容 -->
      <div class="col-right" @dragover.prevent @drop="onDropToMenu">
        <div class="col-title">
          <span>菜单内容 · Menu Content</span>
          <span class="col-title-actions">
            <span class="dish-count">{{ menuDishes.length }}道菜</span>
            <el-button
              size="small"
              type="primary"
              plain
              :disabled="form.images.length === 0"
              @click="openMenuImageViewer"
              class="menu-preview-btn"
            >
              图片预览
            </el-button>
          </span>
        </div>

        <div class="menu-summary">
          <div class="summary-item">
            <span class="summary-label">菜品总价</span>
            <span class="summary-value">¥{{ totalDishPrice.toFixed(2) }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">套餐售价</span>
            <span class="summary-value price-highlight">¥{{ form.packageTotalPrice.toFixed(2) }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">折扣率</span>
            <span class="summary-value" :class="{ 'premium-text': form.discount > 100 }">
              {{ form.discount.toFixed(1) }}%
            </span>
          </div>
        </div>

        <div class="menu-dish-list">
          <div
            v-for="(dish, idx) in menuDishes"
            :key="idx"
            class="menu-dish-item"
            @dblclick="previewDishImage(dish)"
          >
            <span class="menu-dish-idx">{{ idx + 1 }}</span>
            <img v-if="dish.imageUrl || dish.image" :src="dish.imageUrl || dish.image" class="menu-dish-thumb" />
            <div v-else class="menu-dish-thumb-placeholder">菜</div>
            
            <div class="menu-dish-info">
              <div class="menu-dish-name">{{ dish.dishName }}</div>
              <div class="menu-dish-note" v-if="dish.note">{{ dish.note }}</div>
              <div class="menu-dish-price">¥{{ (dish.salePrice || dish.price || 0).toFixed(0) }}</div>
            </div>

            <div class="menu-dish-actions">
              <el-button text size="small" @click="editDishNote(dish, idx)" title="编辑备注">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                </svg>
              </el-button>
              <el-button text size="small" type="danger" @click="removeDishFromMenu(idx)" title="删除">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18"/>
                  <line x1="6" y1="6" x2="18" y2="18"/>
                </svg>
              </el-button>
            </div>
          </div>

          <div v-if="menuDishes.length === 0" class="empty-menu-hint">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1">
              <line x1="16.5" y1="9.4" x2="7.5" y2="4.21"/>
              <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/>
            </svg>
            <p>从左侧菜品库拖拽或点击添加菜品</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 审计报告弹窗 -->
    <el-dialog v-model="showAuditReport" title="数据审计报告" width="500px">
      <div class="audit-report">
        <div v-if="auditWarnings.length === 0 && auditErrorsList.length === 0" class="audit-success">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#4A7C59" stroke-width="2">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
            <polyline points="22 4 12 14.01 9 11.01"/>
          </svg>
          <p>所有数据检查通过，可以保存</p>
        </div>
        
        <div v-if="auditErrorsList.length > 0" class="audit-section">
          <h4 class="audit-section-title error">❌ 必填项缺失</h4>
          <div v-for="(err, idx) in auditErrorsList" :key="idx" class="audit-item error">
            {{ err }}
          </div>
        </div>

        <div v-if="auditWarnings.length > 0" class="audit-section">
          <h4 class="audit-section-title warning">⚠️ 建议完善</h4>
          <div v-for="(warn, idx) in auditWarnings" :key="idx" class="audit-item warning">
            {{ warn }}
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showAuditReport = false">取消</el-button>
        <el-button type="primary" @click="confirmSave" :disabled="auditErrorsList.length > 0">
          确认保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 图片选择器弹窗 -->
    <el-dialog v-model="showImagePicker" title="选择套餐图片（从菜品图片中选择）" width="700px">
      <div class="image-picker-grid">
        <div
          v-for="dish in dishesWithImages"
          :key="dish.dishId"
          class="picker-image-item"
          :class="{ selected: form.images.includes(dish.imageUrl || dish.image) }"
          @click="toggleImageSelection(dish)"
        >
          <img :src="dish.imageUrl || dish.image" :alt="dish.dishName" />
          <div class="picker-image-label">{{ dish.dishName }}</div>
          <div class="picker-check" v-if="form.images.includes(dish.imageUrl || dish.image)">✓</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showImagePicker = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 大图预览 -->
    <el-dialog v-model="showImagePreview" width="600px" top="5vh">
      <img :src="previewImageUrl" style="width:100%;border-radius:8px" />
    </el-dialog>

    <!-- 菜单图片预览（套餐图片多图轮播） -->
    <el-dialog v-model="showMenuImageViewer" title="套餐图片预览" width="720px" top="8vh" align-center>
      <div class="menu-image-viewer">
        <div class="viewer-main">
          <img :src="form.images[menuViewerIdx]" class="viewer-main-img" />
          <div v-if="form.images.length > 1" class="viewer-nav viewer-prev" @click="prevMenuImage" title="上一张">‹</div>
          <div v-if="form.images.length > 1" class="viewer-nav viewer-next" @click="nextMenuImage" title="下一张">›</div>
        </div>
        <div class="viewer-index" v-if="form.images.length > 1">
          {{ menuViewerIdx + 1 }} / {{ form.images.length }}
        </div>
        <div class="viewer-thumbs" v-if="form.images.length > 1">
          <div
            v-for="(src, i) in form.images"
            :key="i"
            :class="['thumb-item', { active: i === menuViewerIdx }]"
            @click="menuViewerIdx = i"
          >
            <img :src="src" />
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showMenuImageViewer = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 备注编辑弹窗 -->
    <el-dialog v-model="showNoteEditor" title="编辑菜品备注" width="400px">
      <el-input
        v-model="noteText"
        type="textarea"
        :rows="3"
        placeholder="输入备注信息（如：少辣、加量等）"
      />
      <template #footer>
        <el-button @click="showNoteEditor = false">取消</el-button>
        <el-button type="primary" @click="saveNote">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getPackageDetail, createPackage, updatePackage } from '@/api/package'
import { getDishesWithRecipe } from '@/api/booking'
import { getStaffList } from '@/api/booking'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isEdit = computed(() => !!route.query.id)
const saving = ref(false)
const dishesLoading = ref(false)

// 表单数据
const form = reactive({
  packageId: '',
  packageName: '',
  englishName: '',
  location: ['宁国'],
  category: '商务宴',
  minGuests: 8,
  maxGuests: 10,
  validDate: null,
  createdAt: '',
  createdBy: '',
  packageTotalPrice: 0,
  originalPrice: 0,
  discount: 100,
  images: [],
  dishes: []
})

// 解锁状态
const timeUnlocked = ref(false)
const makerUnlocked = ref(false)

// 菜品库
const allDishes = ref([])
const dishSearch = ref('')
const activeCategory = ref('全部')
const dishCategories = ['全部', '凉菜', '热菜', '汤羹', '主食', '点心', '水果']

// 菜单内容
const menuDishes = ref([])

// 员工列表
const staffList = ref([])

// 弹窗
const showAuditReport = ref(false)
const showImagePicker = ref(false)
const showImagePreview = ref(false)
const showNoteEditor = ref(false)
const previewImageUrl = ref('')
const noteText = ref('')
const editingDishIdx = ref(-1)
const showMenuImageViewer = ref(false)
const menuViewerIdx = ref(0)

// 审计
const auditErrors = reactive({
  name: false,
  location: false,
  category: false
})
const auditWarnings = ref([])
const auditErrorsList = ref([])

const categories = ['婚宴', '寿宴', '商务宴', '谢师宴', '满月宴', '团圆宴']

// 计算属性
const filteredDishes = computed(() => {
  let result = allDishes.value
  if (activeCategory.value !== '全部') {
    result = result.filter(d => (d.categoryName || d.category || '') === activeCategory.value)
  }
  if (dishSearch.value) {
    const q = dishSearch.value.toLowerCase()
    result = result.filter(d =>
      (d.dishName || '').toLowerCase().includes(q) ||
      (d.categoryName || d.category || '').toLowerCase().includes(q)
    )
  }
  return result
})

const dishesWithImages = computed(() => {
  return allDishes.value.filter(d => d.imageUrl || d.image)
})

const totalDishPrice = computed(() => {
  return menuDishes.value.reduce((sum, d) => sum + (d.salePrice || d.price || 0) * (d.quantity || 1), 0)
})

const perCapita = computed(() => {
  const guests = form.maxGuests || 0
  return guests > 0 ? form.packageTotalPrice / guests : 0
})

// 折扣：按售价/原价×100 自动计算（只读）；如果没有原价则为100%
const computedDiscount = computed(() => {
  const selling = Number(form.packageTotalPrice || 0)
  const original = Number(totalDishPrice.value || 0)
  if (original <= 0) return 100
  return Math.round((selling / original) * 1000) / 10
})

// 监听菜品总价变化，自动更新原价（原价=菜品总价）
watch(totalDishPrice, (newVal) => {
  form.originalPrice = newVal
  // 原价变化后，重新计算折扣（同步到 form.discount 用于后端持久化）
  form.discount = computedDiscount.value
})

// 监听售价变化，重新计算折扣并同步到 form.discount（用于持久化）
watch(() => form.packageTotalPrice, () => {
  form.discount = computedDiscount.value
})

// 监听最大人数变化不影响其他，人均已 computed 自动计算

// 人数联动
function onMinChange(val) {
  if (val > form.maxGuests) {
    form.maxGuests = val
  }
}

function onMaxChange(val) {
  if (val < form.minGuests) {
    form.minGuests = val
  }
}

// 解锁时间
function unlockTime() {
  timeUnlocked.value = true
}

// 解锁制作人
function unlockMaker() {
  makerUnlocked.value = true
}

// 生成套餐编号预览格式：TC + 日期（后端保存时覆盖为真实体系序列号）
function generatePackageId() {
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  // 前端预览占位：后端保存时会按真实体系序列号覆盖
  return `TC${y}${m}${d}***`
}

// 生成当前时间字符串（yyyy-MM-dd HH:mm:ss）
function formatNow() {
  const n = new Date()
  const pad = v => String(v).padStart(2, '0')
  return `${n.getFullYear()}-${pad(n.getMonth()+1)}-${pad(n.getDate())} ${pad(n.getHours())}:${pad(n.getMinutes())}:${pad(n.getSeconds())}`
}

// 加载菜品数据
async function loadDishes() {
  dishesLoading.value = true
  try {
    const res = await getDishesWithRecipe()
    if (res.data) {
      allDishes.value = res.data.map(d => ({
        dishId: d.dishId || d.id,
        dishName: d.dishName || d.name,
        salePrice: d.salePrice || d.price || 0,
        price: d.salePrice || d.price || 0,
        categoryName: d.categoryName || d.category || '其他',
        category: d.categoryName || d.category || '其他',
        imageUrl: d.imageUrl || d.image || '',
        image: d.imageUrl || d.image || ''
      }))
    }
  } catch (e) {
    console.error('加载菜品失败', e)
  } finally {
    dishesLoading.value = false
  }
}

// 加载员工列表（只显示宁国店）
async function loadStaff() {
  try {
    const res = await getStaffList({ storeId: 1 })
    if (res.data) {
      staffList.value = res.data
    }
  } catch (e) {
    console.error('加载员工列表失败', e)
  }
}

// 加载套餐详情（编辑模式）
async function loadPackage() {
  if (!isEdit.value) {
    // 新建套餐：打开即生成编号、录入时间，并从当前登录用户抓取制作人
    form.packageId = generatePackageId()
    form.createdAt = formatNow()
    // 从 userStore 取制作人（优先 username，其次 staffName/name）
    const info = userStore.userInfo || {}
    form.createdBy = info.username || info.staffName || info.name || ''
    return
  }
  try {
    const res = await getPackageDetail(route.query.id)
    if (res.data) {
      const pkg = res.data
      Object.assign(form, {
        packageId: pkg.packageId,
        packageName: pkg.packageName,
        englishName: pkg.englishName || '',
        location: pkg.location ? pkg.location.split(',') : ['宁国'],
        category: pkg.occasionType || pkg.category || '商务宴',
        minGuests: pkg.minGuests || pkg.suggestGuests || 8,
        maxGuests: pkg.maxGuests || pkg.suggestGuests || 10,
        packageTotalPrice: pkg.packageTotalPrice || 0,
        originalPrice: pkg.originalPrice || pkg.packageTotalPrice || 0,
        discount: pkg.discount != null ? pkg.discount : 100,
        createdBy: pkg.creator || pkg.createdBy || '',
        createdAt: pkg.createdAt || '',
        images: pkg.images ? pkg.images.split(',') : []
      })
      menuDishes.value = pkg.dishes || []
    }
  } catch (e) {
    console.error('加载套餐详情失败', e)
    ElMessage.error('加载套餐详情失败')
  }
}

// 添加菜品到菜单
function addDishToMenu(dish) {
  const exists = menuDishes.value.find(d => d.dishId === dish.dishId)
  if (exists) {
    ElMessage.warning('该菜品已在菜单中')
    return
  }
  menuDishes.value.push({ ...dish, quantity: 1, note: '' })
}

// 拖拽开始
function onDishDragStart(dish, e) {
  e.dataTransfer.effectAllowed = 'copy'
  e.dataTransfer.setData('application/json', JSON.stringify(dish))
}

// 拖拽到菜单
function onDropToMenu(e) {
  try {
    const dish = JSON.parse(e.dataTransfer.getData('application/json'))
    addDishToMenu(dish)
  } catch (err) {
    console.error('拖拽失败', err)
  }
}

// 从菜单移除
function removeDishFromMenu(idx) {
  menuDishes.value.splice(idx, 1)
}

// 编辑备注
function editDishNote(dish, idx) {
  editingDishIdx.value = idx
  noteText.value = dish.note || ''
  showNoteEditor.value = true
}

// 保存备注
function saveNote() {
  if (editingDishIdx.value >= 0 && editingDishIdx.value < menuDishes.value.length) {
    menuDishes.value[editingDishIdx.value].note = noteText.value
  }
  showNoteEditor.value = false
}

// 预览图片
function previewDishImage(dish) {
  const url = dish.imageUrl || dish.image
  if (url) {
    previewImageUrl.value = url
    showImagePreview.value = true
  }
}

// 打开菜单图片预览（套餐选择的多张图片轮播）
function openMenuImageViewer() {
  if (form.images.length === 0) {
    ElMessage.warning('还没有选择套餐图片')
    return
  }
  menuViewerIdx.value = 0
  showMenuImageViewer.value = true
}

function prevMenuImage() {
  if (menuViewerIdx.value > 0) {
    menuViewerIdx.value--
  } else {
    menuViewerIdx.value = form.images.length - 1
  }
}

function nextMenuImage() {
  if (menuViewerIdx.value < form.images.length - 1) {
    menuViewerIdx.value++
  } else {
    menuViewerIdx.value = 0
  }
}

// 打开图片选择器
function openImagePicker() {
  showImagePicker.value = true
}

// 切换图片选择
function toggleImageSelection(dish) {
  const url = dish.imageUrl || dish.image
  const idx = form.images.indexOf(url)
  if (idx >= 0) {
    form.images.splice(idx, 1)
  } else if (form.images.length < 3) {
    form.images.push(url)
  } else {
    ElMessage.warning('最多选择3张图片')
  }
}

// 移除图片
function removeImage(idx) {
  form.images.splice(idx, 1)
}

// 数据审计
function runAudit() {
  auditErrors.name = !form.packageName
  auditErrors.location = !form.location || form.location.length === 0
  auditErrors.category = !form.category

  auditErrorsList.value = []
  auditWarnings.value = []

  if (auditErrors.name) auditErrorsList.value.push('套餐名称不能为空')
  if (auditErrors.location) auditErrorsList.value.push('请选择使用地点')
  if (auditErrors.category) auditErrorsList.value.push('请选择宴会分类')
  if (menuDishes.value.length === 0) auditWarnings.value.push('菜单中没有菜品')
  if (!form.englishName) auditWarnings.value.push('建议填写英文名称')
  if (form.minGuests === form.maxGuests) auditWarnings.value.push('建议设置人数范围而非固定值')
}

// 保存套餐
function savePackage() {
  runAudit()
  showAuditReport.value = true
}

// 确认保存
async function confirmSave() {
  if (auditErrorsList.value.length > 0) {
    ElMessage.error('请修正必填项后再保存')
    return
  }

  saving.value = true
  try {
    const payload = {
      ...form,
      location: form.location.join(','),
      occasionType: form.category,
      category: form.category,
      minGuests: form.minGuests,
      maxGuests: form.maxGuests,
      suggestGuests: form.maxGuests,
      // 制作人字段统一映射为 creator，接收端 DTO 字段
      creator: form.createdBy,
      createdAt: form.createdAt,
      images: form.images.join(','),
      dishes: menuDishes.value.map(d => ({
        dishId: d.dishId,
        dishName: d.dishName,
        quantity: d.quantity || 1,
        price: d.salePrice || d.price || 0,
        note: d.note || ''
      }))
    }

    let res
    if (isEdit.value) {
      res = await updatePackage(form.packageId, payload)
    } else {
      res = await createPackage(payload)
    }

    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '套餐已更新' : '套餐已创建')
      showAuditReport.value = false
      router.push('/dashboard/set-menu')
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch (e) {
    console.error('保存失败', e)
    ElMessage.error('保存失败，请重试')
  } finally {
    saving.value = false
  }
}

// 重置表单
function resetForm() {
  form.packageId = generatePackageId()
  form.packageName = ''
  form.englishName = ''
  form.location = ['宁国']
  form.category = '商务宴'
  form.minGuests = 8
  form.maxGuests = 10
  form.validDate = null
  form.createdAt = formatNow()
  // 制作人重置为当前登录用户
  const info = userStore.userInfo || {}
  form.createdBy = info.username || info.staffName || info.name || ''
  form.packageTotalPrice = 0
  form.originalPrice = 0
  form.discount = 100
  form.images = []
  menuDishes.value = []
  timeUnlocked.value = false
  makerUnlocked.value = false
}

// 返回
function goBack() {
  router.push('/dashboard/set-menu')
}

// 初始化
onMounted(() => {
  loadDishes()
  loadStaff()
  loadPackage()
})
</script>

<style scoped>
.set-menu-edit-page {
  height: calc(100vh - 140px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.edit-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: var(--color-card);
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
}

.page-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text);
  margin: 0;
}

.header-right {
  display: flex;
  gap: 10px;
}

/* 三栏布局 */
.three-column-layout {
  flex: 1;
  display: grid;
  grid-template-columns: 360px 1fr 380px;
  gap: 0;
  overflow: hidden;
}

.col-left,
.col-mid,
.col-right {
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--color-border);
  overflow: hidden;
}

.col-right {
  border-right: none;
}

.col-title {
  padding: 14px 20px;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
  background: var(--color-bg-alt);
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}

/* 左栏 */
.form-section {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.form-group {
  margin-bottom: 16px;
}

.form-label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-secondary);
  margin-bottom: 6px;
}

.required {
  color: var(--color-danger);
}

.full-width {
  width: 100%;
}

.locked-input :deep(.el-input__wrapper) {
  background: var(--color-bg-alt);
  cursor: not-allowed;
}

.guest-range {
  display: flex;
  align-items: center;
  gap: 12px;
}

.range-sep {
  color: var(--color-text-muted);
  font-size: 13px;
}

.locked-field {
  cursor: pointer;
  transition: all 0.2s;
}

.locked-field.unlocked {
  cursor: default;
}

.lock-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: var(--color-bg-alt);
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text-muted);
  font-size: 12px;
  transition: all 0.2s;
}

.lock-hint:hover {
  border-color: var(--color-accent);
  color: var(--color-accent-dark);
}

.lock-hint svg {
  flex-shrink: 0;
  opacity: 0.6;
}

.price-section {
  margin-top: 8px;
}

.section-divider {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-accent-dark);
  padding: 12px 0 16px;
  border-top: 1px solid var(--color-border-light);
  margin-bottom: 16px;
}

.form-hint {
  display: block;
  font-size: 11px;
  color: var(--color-text-muted);
  margin-top: 4px;
}

.premium-discount :deep(.el-input__wrapper) {
  background: rgba(194, 85, 85, 0.08);
  border-color: var(--color-danger);
}

.premium-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: linear-gradient(135deg, #C25555, #D46A6A);
  color: #fff;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 600;
}

.image-picker {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.image-thumb {
  width: 72px;
  height: 72px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  position: relative;
  cursor: pointer;
}

.image-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.image-thumb:hover .remove-overlay {
  opacity: 1;
}

.image-add-btn {
  width: 72px;
  height: 72px;
  border: 2px dashed var(--color-border);
  border-radius: var(--radius-sm);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  cursor: pointer;
  color: var(--color-text-muted);
  font-size: 11px;
  transition: all 0.2s;
}

.image-add-btn:hover {
  border-color: var(--color-accent);
  color: var(--color-accent-dark);
}

.field-warning :deep(.el-input__wrapper),
.field-warning :deep(.el-select__wrapper) {
  animation: shake 0.5s;
  border-color: var(--color-warning);
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-4px); }
  75% { transform: translateX(4px); }
}

/* 中栏 */
.search-bar {
  padding: 12px 16px;
  border-bottom: 1px solid var(--color-border-light);
  flex-shrink: 0;
}

.category-tabs {
  display: flex;
  gap: 8px;
  padding: 10px 16px;
  border-bottom: 1px solid var(--color-border-light);
  overflow-x: auto;
  flex-shrink: 0;
}

.cat-tab {
  padding: 6px 14px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-secondary);
  background: var(--color-bg-alt);
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.cat-tab:hover {
  background: rgba(196, 163, 90, 0.1);
  color: var(--color-accent-dark);
}

.cat-tab.active {
  background: var(--color-primary);
  color: #fff;
}

.dish-grid {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 12px;
  align-content: start;
}

.dish-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: grab;
  transition: var(--transition);
  position: relative;
}

.dish-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
  border-color: var(--color-accent);
}

.dish-card:active {
  cursor: grabbing;
}

.dish-card-image {
  height: 180px;
  background: var(--color-bg-alt);
  overflow: hidden;
}

.dish-card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.dish-card:hover .dish-card-image img {
  transform: scale(1.05);
}

.dish-card-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-border);
}

.dish-card-body {
  padding: 10px 12px;
}

.dish-card-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 4px;
}

.dish-card-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dish-card-category {
  font-size: 11px;
  color: var(--color-text-muted);
  padding: 2px 6px;
  background: var(--color-bg-alt);
  border-radius: 4px;
}

.dish-card-price {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-accent-dark);
}

.dish-card-add {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #4A90D9;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0;
  transition: all 0.2s;
  font-size: 18px;
  font-weight: 700;
  line-height: 1;
}

.dish-card:hover .dish-card-add {
  opacity: 1;
}

.dish-card-add:hover {
  background: #357ABD;
  transform: scale(1.1);
}

.no-dishes {
  grid-column: 1 / -1;
  text-align: center;
  padding: 40px;
  color: var(--color-text-muted);
}

/* 右栏 */
.menu-summary {
  padding: 12px 16px;
  background: var(--color-bg-alt);
  border-bottom: 1px solid var(--color-border-light);
  display: flex;
  gap: 16px;
  flex-shrink: 0;
}

.summary-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.summary-label {
  font-size: 11px;
  color: var(--color-text-muted);
}

.summary-value {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text);
}

.summary-value.price-highlight {
  color: var(--color-primary);
}

.premium-text {
  color: var(--color-danger) !important;
}

.menu-dish-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.menu-dish-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  margin-bottom: 6px;
  background: var(--color-card);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-sm);
  transition: all 0.25s;
  cursor: pointer;
}

.menu-dish-item:hover {
  transform: translateX(4px);
  background: rgba(196, 163, 90, 0.06);
  border-color: var(--color-accent);
}

.menu-dish-idx {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.menu-dish-thumb {
  width: 38px;
  height: 38px;
  border-radius: 6px;
  object-fit: cover;
  flex-shrink: 0;
}

.menu-dish-thumb-placeholder {
  width: 38px;
  height: 38px;
  border-radius: 6px;
  background: var(--color-bg-alt);
  color: var(--color-text-muted);
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.menu-dish-info {
  flex: 1;
  min-width: 0;
}

.menu-dish-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text);
}

.menu-dish-note {
  font-size: 11px;
  color: var(--color-accent-dark);
  margin-top: 2px;
}

.menu-dish-price {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-top: 2px;
}

.menu-dish-actions {
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.2s;
}

.menu-dish-item:hover .menu-dish-actions {
  opacity: 1;
}

.empty-menu-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: var(--color-text-muted);
  gap: 12px;
}

.empty-menu-hint svg {
  opacity: 0.3;
}

.empty-menu-hint p {
  font-size: 13px;
}

.dish-count {
  float: right;
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-muted);
}

/* 审计报告 */
.audit-report {
  padding: 8px 0;
}

.audit-success {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 24px;
  color: #4A7C59;
}

.audit-success p {
  font-size: 15px;
  font-weight: 500;
}

.audit-section {
  margin-bottom: 20px;
}

.audit-section-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 10px;
}

.audit-section-title.error {
  color: var(--color-danger);
}

.audit-section-title.warning {
  color: var(--color-warning);
}

.audit-item {
  padding: 8px 12px;
  margin-bottom: 6px;
  border-radius: var(--radius-sm);
  font-size: 13px;
}

.audit-item.error {
  background: rgba(194, 85, 85, 0.08);
  color: var(--color-danger);
}

.audit-item.warning {
  background: rgba(212, 168, 83, 0.08);
  color: var(--color-accent-dark);
}

/* 图片选择器 */
.image-picker-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
  max-height: 400px;
  overflow-y: auto;
}

.picker-image-item {
  position: relative;
  border-radius: var(--radius-sm);
  overflow: hidden;
  cursor: pointer;
  aspect-ratio: 1;
  border: 2px solid transparent;
  transition: all 0.2s;
}

.picker-image-item:hover {
  border-color: var(--color-accent);
}

.picker-image-item.selected {
  border-color: var(--color-primary);
}

.picker-image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.picker-image-label {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 4px;
  background: linear-gradient(transparent, rgba(0,0,0,0.7));
  color: #fff;
  font-size: 10px;
  text-align: center;
}

.picker-check {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

@media (max-width: 1400px) {
  .three-column-layout {
    grid-template-columns: 320px 1fr 340px;
  }
}

@media (max-width: 1200px) {
  .three-column-layout {
    grid-template-columns: 300px 1fr 300px;
  }
}

/* 右栏标题 actions 布局 */
.col-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.col-title-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.menu-preview-btn {
  margin: 0 !important;
}

/* 套餐图片多图预览弹窗 */
.menu-image-viewer {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}

.viewer-main {
  position: relative;
  width: 100%;
  max-height: 520px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
  border-radius: 8px;
  overflow: hidden;
}

.viewer-main-img {
  max-width: 100%;
  max-height: 520px;
  object-fit: contain;
  display: block;
}

.viewer-nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 40px;
  height: 60px;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  font-size: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  user-select: none;
  transition: background 0.15s ease;
}

.viewer-nav:hover {
  background: rgba(0, 0, 0, 0.65);
}

.viewer-prev { left: 0; border-radius: 0 4px 4px 0; }
.viewer-next { right: 0; border-radius: 4px 0 0 4px; }

.viewer-index {
  font-size: 13px;
  color: var(--color-text-muted);
}

.viewer-thumbs {
  display: flex;
  gap: 8px;
  justify-content: center;
  flex-wrap: wrap;
}

.thumb-item {
  width: 72px;
  height: 72px;
  border-radius: 6px;
  overflow: hidden;
  border: 2px solid transparent;
  cursor: pointer;
  background: #f3f3f3;
  flex-shrink: 0;
}

.thumb-item.active {
  border-color: var(--color-primary, #4A7C59);
}

.thumb-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
</style>

