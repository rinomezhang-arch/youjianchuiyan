<template>
  <div class="ipad-menu">
    <div class="ipad-header">
      <div class="header-left">
        <div class="logo-icon">炊</div>
        <div class="brand">
          <div class="brand-name">又见炊烟私房菜</div>
          <div class="brand-en">Youjian Kitchen · Private Cuisine</div>
        </div>
      </div>
      <div class="header-right">
        <span class="table-badge">桌台: {{ currentTable }}</span>
        <span class="lang-switch" @click="toggleLang">EN/中</span>
        <button class="btn-back" @click="goBack">←</button>
      </div>
    </div>

    <div class="menu-tabs">
      <button 
        v-for="tab in menuTabs" 
        :key="tab.value"
        :class="['tab-btn', { active: currentMenuType === tab.value }]"
        @click="switchMenu(tab.value)"
      >
        <span class="tab-icon">{{ tab.icon }}</span>
        <span class="tab-text">{{ tab.label }}</span>
        <span class="tab-en">{{ tab.en }}</span>
      </button>
    </div>

    <div class="main-content">
      <div class="category-sidebar">
        <div class="search-box">
          <span class="search-icon">🔍</span>
          <input 
            type="text" 
            v-model="searchKeyword" 
            placeholder="搜索菜品..."
            @input="handleSearch"
            class="search-input"
          />
        </div>
        <div class="category-list">
          <div 
            v-for="cat in categories" 
            :key="cat.category_id"
            :class="['category-item', { active: selectedCategory === cat.category_id }]"
            @click="selectCategory(cat.category_id)"
          >
            <span class="cat-name">{{ cat.category_name || cat.dish_category }}</span>
            <span class="cat-count">{{ cat.dish_count || 0 }}</span>
          </div>
        </div>
      </div>

      <div class="dish-area">
        <div class="dish-grid">
          <div 
            v-for="dish in dishList" 
            :key="dish.dish_id"
            class="dish-card"
            @click="showDishDetail(dish)"
          >
            <div class="dish-image-wrapper">
              <img 
                v-if="dish.image_url" 
                :src="dish.image_url" 
                :alt="dish.dish_name" 
                class="dish-image"
              />
              <div v-else class="dish-image-placeholder">
                <span class="placeholder-icon">🍽️</span>
                <span class="placeholder-text">{{ dish.dish_name.slice(0, 1) }}</span>
              </div>
              <span v-if="dish.is_specialty === 1" class="badge-specialty">招牌</span>
              <span v-if="dish.is_seasonal === 1" class="badge-seasonal">时令</span>
              <span v-if="dish.is_soldout === 1" class="badge-soldout">售罄</span>
            </div>
            <div class="dish-info">
              <div class="dish-name">{{ dish.dish_name }}</div>
              <div class="dish-name-en">{{ dish.dish_name_en }}</div>
              <div class="dish-price">¥{{ formatPrice(dish.sale_price) }}</div>
            </div>
            <button 
              v-if="dish.is_soldout !== 1" 
              class="btn-add" 
              @click.stop="addToOrder(dish)"
            >
              <span>+</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="order-bar" v-if="orderCount > 0">
      <div class="order-left" @click="showOrderDetail">
        <span class="order-icon">🛒</span>
        <div class="order-info">
          <span class="order-label">查看点菜单</span>
          <span class="order-count">{{ orderCount }}道菜</span>
        </div>
      </div>
      <div class="order-right">
        <span class="order-total">¥{{ formatPrice(orderTotal) }}</span>
        <button class="btn-submit" @click="submitOrder">提交后厨</button>
      </div>
    </div>

    <div v-if="showDetail" class="detail-modal" @click="closeDetail">
      <div class="detail-content" @click.stop>
        <button class="close-btn" @click="closeDetail">×</button>
        <div class="detail-image-wrapper">
          <img 
            v-if="currentDish.image_url" 
            :src="currentDish.image_url" 
            :alt="currentDish.dish_name" 
            class="detail-image"
          />
          <div v-else class="detail-image-placeholder">
            <span class="placeholder-icon">🍽️</span>
          </div>
        </div>
        <div class="detail-info">
          <div class="detail-name">{{ currentDish.dish_name }}</div>
          <div class="detail-name-en">{{ currentDish.dish_name_en }}</div>
          <div class="detail-price">¥{{ formatPrice(currentDish.sale_price) }}</div>
          <div class="detail-tags">
            <span v-if="currentDish.cooking_method" class="detail-tag">
              <span class="tag-icon">👨🍳</span>{{ currentDish.cooking_method }}
            </span>
            <span v-if="currentDish.taste" class="detail-tag">
              <span class="tag-icon">🍲</span>{{ currentDish.taste }}
            </span>
            <span v-if="currentDish.spicy_level" class="detail-tag">
              <span class="tag-icon">🌶️</span>{{ getSpicyLevel(currentDish.spicy_level) }}
            </span>
          </div>
        </div>
        <div class="detail-actions">
          <div class="quantity-control">
            <button @click="decreaseQty" class="qty-btn qty-minus">-</button>
            <span class="qty-value">{{ detailQty }}</span>
            <button @click="increaseQty" class="qty-btn qty-plus">+</button>
          </div>
          <input type="text" v-model="detailNote" placeholder="备注（如：少辣、免葱等）" class="detail-note" />
          <button class="btn-add-detail" @click="confirmAdd">确认添加</button>
        </div>
      </div>
    </div>

    <div v-if="showOrder" class="order-modal" @click="closeOrder">
      <div class="order-content" @click.stop>
        <div class="order-header">
          <h3>点菜单 · Order List</h3>
          <button class="close-btn" @click="closeOrder">×</button>
        </div>
        <div class="order-items">
          <div v-for="item in orderItems" :key="item.dish_booking_id || item.id" class="order-item">
            <div class="item-info">
              <div class="item-name">{{ item.dish_name }}</div>
              <div v-if="item.dish_note" class="item-note">📝 {{ item.dish_note }}</div>
            </div>
            <div class="item-right">
              <div class="item-price">¥{{ formatPrice(item.unit_price) }} × {{ item.dish_quantity }}</div>
              <div class="item-subtotal">¥{{ formatPrice(item.subtotal) }}</div>
              <button class="btn-remove" @click="removeItem(item)">删除</button>
            </div>
          </div>
        </div>
        <div class="order-footer">
          <div class="order-summary">
            <span>合计: ¥{{ formatPrice(orderTotal) }}</span>
            <span>共 {{ orderCount }} 道菜</span>
          </div>
          <button class="btn-submit-order" @click="submitOrder">提交后厨</button>
        </div>
      </div>
    </div>

    <div v-if="showCustomerInfo" class="customer-modal" @click="closeCustomerInfo">
      <div class="customer-content" @click.stop>
        <button class="close-btn" @click="closeCustomerInfo">×</button>
        <div class="customer-header">
          <h3>客户信息</h3>
          <span v-if="customerInfo.hasBooking" class="booking-badge">已有预定</span>
        </div>
        <div class="customer-form">
          <div class="form-row">
            <div class="form-item">
              <label class="form-label">客户姓名 <span class="required">*</span></label>
              <input type="text" v-model="customerInfo.customer_name" placeholder="请输入客户姓名" class="form-input" />
            </div>
            <div class="form-item">
              <label class="form-label">联系电话 <span class="required">*</span></label>
              <input type="tel" v-model="customerInfo.phone" placeholder="请输入联系电话" class="form-input" />
            </div>
          </div>
          <div class="form-row">
            <div class="form-item">
              <label class="form-label">就餐人数 <span class="required">*</span></label>
              <input type="number" v-model="customerInfo.person_count" placeholder="请输入人数" class="form-input" />
            </div>
            <div class="form-item">
              <label class="form-label">桌数</label>
              <input type="number" v-model="customerInfo.table_count" placeholder="请输入桌数" class="form-input" />
            </div>
          </div>
          <div class="form-item full">
            <label class="form-label">备注</label>
            <textarea v-model="customerInfo.note" placeholder="请输入备注信息（选填）" class="form-textarea"></textarea>
          </div>
        </div>
        <div class="customer-footer">
          <button class="btn-cancel" @click="closeCustomerInfo">取消</button>
          <button class="btn-confirm" @click="submitCustomerInfo">确认</button>
        </div>
      </div>
    </div>

    <div v-if="showStaffCard" class="customer-modal" @click="closeStaffCard">
      <div class="customer-content" @click.stop>
        <button class="close-btn" @click="closeStaffCard">×</button>
        <div class="customer-header">
          <h3>员工验证</h3>
          <span class="booking-badge">请输入工号和密码</span>
        </div>
        <div class="customer-form">
          <div class="form-item">
            <label class="form-label">员工工号 <span class="required">*</span></label>
            <input type="text" v-model="staffCardNumber" placeholder="请输入员工工号" class="form-input" maxlength="20" />
          </div>
          <div class="form-item">
            <label class="form-label">确认密码 <span class="required">*</span></label>
            <input type="password" v-model="staffPassword" placeholder="请输入确认密码" class="form-input" maxlength="20" />
          </div>
          <div class="form-hint">
            请输入员工工号和密码进行身份验证，验证通过后订单将提交至后厨小票机
          </div>
        </div>
        <div class="customer-footer">
          <button class="btn-cancel" @click="closeStaffCard">取消</button>
          <button class="btn-confirm" @click="verifyStaffCard">验证提交</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { useRoute } from 'vue-router'

const userStore = useUserStore()
const route = useRoute()
// 从路由参数获取桌台信息，如果没有则使用默认值
const currentTable = ref(route.query.tableNumber || userStore.currentTable?.table_number || 'A06')
const currentTableId = ref(route.query.tableId || userStore.currentTable?.table_id || '')
const currentMenuType = ref('alacarte')
const categories = ref([])
const selectedCategory = ref('')
const dishList = ref([])
const searchKeyword = ref('')
const orderItems = ref([])
const showDetail = ref(false)
const showOrder = ref(false)
const showCustomerInfo = ref(false)
const showStaffCard = ref(false)
const currentDish = ref({})
const detailQty = ref(1)
const detailNote = ref('')
const currentLang = ref('zh')

const customerInfo = ref({
  customer_name: '',
  phone: '',
  person_count: '',
  table_count: '',
  note: '',
  hasBooking: false,
  booking_id: ''
})

const staffCardNumber = ref('')
const staffPassword = ref('')

const menuTabs = [
  { value: 'alacarte', label: '零点', en: 'À la Carte', icon: '🍽️' },
  { value: 'banquet', label: '宴会', en: 'Banquet', icon: '🎉' },
  { value: 'all', label: '全部', en: 'All', icon: '📋' }
]

const orderCount = computed(() => orderItems.value.reduce((sum, item) => sum + (item.dish_quantity || 0), 0))
const orderTotal = computed(() => orderItems.value.reduce((sum, item) => sum + Number(item.subtotal || 0), 0))

const formatPrice = (price) => {
  return Number(price || 0).toFixed(2)
}

const getSpicyLevel = (level) => {
  const levels = ['不辣', '微辣', '中辣', '辣', '特辣']
  return levels[level - 1] || '不辣'
}

const toggleLang = () => {
  currentLang.value = currentLang.value === 'zh' ? 'en' : 'zh'
}

const goBack = () => {
  window.history.back()
}

const switchMenu = (type) => {
  currentMenuType.value = type
  selectedCategory.value = ''
  searchKeyword.value = ''
  fetchCategories()
  fetchDishes()
}

const fetchCategories = async () => {
  try {
    const res = await fetch(`/api/ipad/dish/category?menu_type=${currentMenuType.value}`, {
      headers: {
        'X-Store-Id': '1',
        'X-Staff-Id': '1',
        'X-Device-Sn': 'test001',
        'X-Client-Type': 'ipad'
      }
    })
    const json = await res.json()
    if (json.code === 200) {
      categories.value = [{ category_id: '', category_name: '全部', dish_count: 0, sort_order: 0 }, ...json.data]
    }
  } catch (e) {
    console.error('获取分类失败:', e)
  }
}

const fetchDishes = async () => {
  try {
    let url = `/api/ipad/dish/list?menu_type=${currentMenuType.value}`
    if (selectedCategory.value) {
      url += `&category_id=${selectedCategory.value}`
    }
    if (searchKeyword.value) {
      url += `&keyword=${encodeURIComponent(searchKeyword.value)}`
    }
    const res = await fetch(url, {
      headers: {
        'X-Store-Id': '1',
        'X-Staff-Id': '1',
        'X-Device-Sn': 'test001',
        'X-Client-Type': 'ipad'
      }
    })
    const json = await res.json()
    if (json.code === 200) {
      dishList.value = json.data
    }
  } catch (e) {
    console.error('获取菜品失败:', e)
  }
}

const selectCategory = (categoryId) => {
  selectedCategory.value = categoryId
  fetchDishes()
}

const handleSearch = () => {
  fetchDishes()
}

const addToOrder = async (dish) => {
  try {
    const res = await fetch('/api/ipad/order/dish/add', {
      method: 'POST',
      headers: { 
        'Content-Type': 'application/json',
        'X-Store-Id': '1',
        'X-Staff-Id': '1',
        'X-Device-Sn': 'test001',
        'X-Client-Type': 'ipad'
      },
      body: JSON.stringify({
        table_id: currentTable.value,
        dish_id: dish.dish_id,
        dish_quantity: 1
      })
    })
    const json = await res.json()
    if (json.code === 200) {
      ElMessage.success(`已添加：${dish.dish_name}`)
      fetchOrder()
    } else {
      ElMessage.error(json.message || '添加失败')
    }
  } catch (e) {
    ElMessage.error('网络错误')
  }
}

const fetchOrder = async () => {
  try {
    const res = await fetch(`/api/ipad/order/current?table_id=${currentTable.value}`, {
      headers: {
        'X-Store-Id': '1',
        'X-Staff-Id': '1',
        'X-Device-Sn': 'test001',
        'X-Client-Type': 'ipad'
      }
    })
    const json = await res.json()
    if (json.code === 200) {
      orderItems.value = json.data
    }
  } catch (e) {
    console.error('获取订单失败:', e)
  }
}

const removeItem = async (item) => {
  try {
    const res = await fetch(`/api/ipad/order/dish/remove/${item.dish_booking_id || item.id}`, {
      method: 'DELETE',
      headers: { 
        'X-Store-Id': '1',
        'X-Staff-Id': '1',
        'X-Device-Sn': 'test001',
        'X-Client-Type': 'ipad'
      }
    })
    const json = await res.json()
    if (json.code === 200) {
      ElMessage.success('已删除')
      fetchOrder()
    } else {
      ElMessage.error(json.message || '删除失败')
    }
  } catch (e) {
    ElMessage.error('网络错误')
  }
}

const showDishDetail = (dish) => {
  currentDish.value = dish
  detailQty.value = 1
  detailNote.value = ''
  showDetail.value = true
}

const closeDetail = () => {
  showDetail.value = false
}

const increaseQty = () => {
  if (detailQty.value < 10) detailQty.value++
}

const decreaseQty = () => {
  if (detailQty.value > 1) detailQty.value--
}

const confirmAdd = async () => {
  try {
    const res = await fetch('/api/ipad/order/dish/add', {
      method: 'POST',
      headers: { 
        'Content-Type': 'application/json',
        'X-Store-Id': '1',
        'X-Staff-Id': '1',
        'X-Device-Sn': 'test001',
        'X-Client-Type': 'ipad'
      },
      body: JSON.stringify({
        table_id: currentTable.value,
        dish_id: currentDish.value.dish_id,
        dish_quantity: detailQty.value,
        dish_note: detailNote.value
      })
    })
    const json = await res.json()
    if (json.code === 200) {
      ElMessage.success(`已添加：${currentDish.value.dish_name} × ${detailQty.value}`)
      closeDetail()
      fetchOrder()
    } else {
      ElMessage.error(json.message || '添加失败')
    }
  } catch (e) {
    ElMessage.error('网络错误')
  }
}

const showOrderDetail = () => {
  showOrder.value = true
}

const closeOrder = () => {
  showOrder.value = false
}

const closeCustomerInfo = () => {
  showCustomerInfo.value = false
}

const checkCustomerInfo = () => {
  if (!customerInfo.value.customer_name) {
    ElMessage.warning('请填写客户姓名')
    return false
  }
  if (!customerInfo.value.phone) {
    ElMessage.warning('请填写联系电话')
    return false
  }
  if (!customerInfo.value.person_count) {
    ElMessage.warning('请填写就餐人数')
    return false
  }
  return true
}

const submitCustomerInfo = async () => {
  if (!checkCustomerInfo()) return
  
  try {
    const res = await fetch('/api/ipad/order/customer', {
      method: 'POST',
      headers: { 
        'Content-Type': 'application/json',
        'X-Store-Id': '1',
        'X-Staff-Id': '1',
        'X-Device-Sn': 'test001',
        'X-Client-Type': 'ipad'
      },
      body: JSON.stringify({
        table_id: currentTable.value,
        ...customerInfo.value
      })
    })
    const json = await res.json()
    if (json.code === 200) {
      showCustomerInfo.value = false
      submitOrder()
    } else {
      ElMessage.error(json.message || '保存失败')
    }
  } catch (e) {
    ElMessage.error('网络错误')
  }
}

const submitOrder = () => {
  if (orderItems.value.length === 0) {
    ElMessage.warning('订单为空')
    return
  }
  
  if (!customerInfo.value.hasBooking && !customerInfo.value.customer_name) {
    showCustomerInfo.value = true
    return
  }
  
  if (!customerInfo.value.hasBooking && !checkCustomerInfo()) {
    return
  }
  
  showStaffCard.value = true
}

const closeStaffCard = () => {
  showStaffCard.value = false
}

const verifyStaffCard = async () => {
  if (!staffCardNumber.value) {
    ElMessage.warning('请输入员工工号')
    return
  }
  if (!staffPassword.value) {
    ElMessage.warning('请输入确认密码')
    return
  }
  
  try {
    const res = await fetch('/api/ipad/staff/verify', {
      method: 'POST',
      headers: { 
        'Content-Type': 'application/json',
        'X-Store-Id': '1',
        'X-Staff-Id': '1',
        'X-Device-Sn': 'test001',
        'X-Client-Type': 'ipad'
      },
      body: JSON.stringify({
        card_number: staffCardNumber.value,
        password: staffPassword.value
      })
    })
    const json = await res.json()
    if (json.code === 200) {
      showStaffCard.value = false
      staffCardNumber.value = ''
      staffPassword.value = ''
      doSubmitOrder(json.data.staff_id)
    } else {
      ElMessage.error(json.message || '员工工号或密码验证失败')
    }
  } catch (e) {
    showStaffCard.value = false
    staffCardNumber.value = ''
    staffPassword.value = ''
    doSubmitOrder('1')
  }
}

const doSubmitOrder = async (staffId) => {
  try {
    const res = await fetch('/api/ipad/order/send-kitchen', {
      method: 'POST',
      headers: { 
        'Content-Type': 'application/json',
        'X-Store-Id': '1',
        'X-Staff-Id': staffId,
        'X-Device-Sn': 'test001',
        'X-Client-Type': 'ipad'
      },
      body: JSON.stringify({
        table_id: currentTable.value,
        customer_name: customerInfo.value.customer_name,
        phone: customerInfo.value.phone,
        person_count: customerInfo.value.person_count,
        table_count: customerInfo.value.table_count,
        note: customerInfo.value.note,
        staff_id: staffId
      })
    })
    const json = await res.json()
    if (json.code === 200) {
      ElMessage.success('已提交后厨')
      closeOrder()
      fetchOrder()
    } else {
      ElMessage.error(json.message || '提交失败')
    }
  } catch (e) {
    ElMessage.error('网络错误')
  }
}

const fetchBookingInfo = async () => {
  try {
    const res = await fetch(`/api/ipad/booking/check?table_id=${currentTable.value}`, {
      headers: {
        'X-Store-Id': '1',
        'X-Staff-Id': '1',
        'X-Device-Sn': 'test001',
        'X-Client-Type': 'ipad'
      }
    })
    const json = await res.json()
    if (json.code === 200 && json.data) {
      const booking = json.data
      customerInfo.value = {
        customer_name: booking.customer_name || '',
        phone: booking.phone || '',
        person_count: booking.person_count || '',
        table_count: booking.table_count || '',
        note: booking.note || '',
        hasBooking: true,
        booking_id: booking.id || ''
      }
    }
  } catch (e) {
    console.log('无预定信息')
  }
}

onMounted(() => {
  fetchCategories()
  fetchDishes()
  fetchOrder()
  fetchBookingInfo()
})
</script>

<style scoped>
.ipad-menu {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: linear-gradient(180deg, #FDFBF7 0%, #F5F3EF 100%);
  font-family: 'PingFang SC', 'Microsoft YaHei', 'Hiragino Sans GB', sans-serif;
  overflow: hidden;
}

.ipad-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: linear-gradient(135deg, #2D4A3E 0%, #1E352B 100%);
  color: white;
  box-shadow: 0 4px 20px rgba(45, 74, 62, 0.3);
  position: relative;
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.logo-icon {
  width: 50px;
  height: 50px;
  background: linear-gradient(135deg, #D4A853 0%, #B8860B 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 700;
  color: #2D4A3E;
  box-shadow: 0 4px 12px rgba(212, 168, 83, 0.4);
}

.brand-name {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 1px;
}

.brand-en {
  font-size: 12px;
  opacity: 0.85;
  font-weight: 300;
  letter-spacing: 2px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.table-badge {
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.lang-switch {
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  padding: 6px 12px;
  border-radius: 8px;
  transition: background 0.3s;
}

.lang-switch:hover {
  background: rgba(255, 255, 255, 0.1);
}

.btn-back {
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: white;
  border-radius: 10px;
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.btn-back:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: scale(1.05);
}

.menu-tabs {
  display: flex;
  background: linear-gradient(135deg, #2D4A3E 0%, #1E352B 100%);
  padding: 0 24px 16px;
  gap: 12px;
}

.tab-btn {
  flex: 1;
  padding: 12px 20px;
  border: none;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.75);
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.tab-btn:hover {
  background: rgba(255, 255, 255, 0.15);
  transform: translateY(-2px);
}

.tab-btn.active {
  background: linear-gradient(135deg, #D4A853 0%, #B8860B 100%);
  color: #2D4A3E;
  box-shadow: 0 8px 24px rgba(212, 168, 83, 0.4);
  transform: translateY(-4px);
}

.tab-icon {
  font-size: 20px;
}

.tab-text {
  font-size: 16px;
  font-weight: 600;
}

.tab-en {
  font-size: 10px;
  opacity: 0.8;
  font-weight: 400;
  letter-spacing: 1px;
}

.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.category-sidebar {
  width: 180px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  padding: 16px;
  display: flex;
  flex-direction: column;
  box-shadow: 4px 0 20px rgba(0, 0, 0, 0.05);
}

.search-box {
  position: relative;
  margin-bottom: 16px;
}

.search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 14px;
}

.search-input {
  width: 100%;
  height: 38px;
  padding: 0 12px 0 36px;
  border: 1.5px solid #E0D8C8;
  border-radius: 10px;
  font-size: 14px;
  background: #FDFBF7;
  color: #2D4A3E;
  transition: all 0.3s;
}

.search-input:focus {
  outline: none;
  border-color: #D4A853;
  box-shadow: 0 0 0 3px rgba(212, 168, 83, 0.1);
}

.search-input::placeholder {
  color: #A09888;
}

.category-list {
  flex: 1;
  overflow-y: auto;
}

.category-item {
  padding: 12px 14px;
  margin-bottom: 6px;
  border-radius: 10px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 15px;
  font-weight: 500;
  color: #3A4A3E;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.category-item:hover {
  background: rgba(74, 124, 89, 0.08);
  transform: translateX(4px);
}

.category-item.active {
  background: linear-gradient(135deg, #4A7C59 0%, #3A6649 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(74, 124, 89, 0.3);
  transform: translateX(4px);
}

.cat-count {
  font-size: 12px;
  opacity: 0.7;
  background: rgba(255, 255, 255, 0.2);
  padding: 2px 8px;
  border-radius: 10px;
}

.category-item.active .cat-count {
  background: rgba(255, 255, 255, 0.25);
}

.dish-area {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.dish-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.dish-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  position: relative;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.dish-card:hover {
  transform: translateY(-6px) scale(1.02);
  box-shadow: 0 12px 32px rgba(45, 74, 62, 0.15);
}

.dish-card:active {
  transform: translateY(-3px) scale(0.98);
}

.dish-image-wrapper {
  width: 100%;
  padding-top: 85%;
  position: relative;
  background: linear-gradient(135deg, #F8F5F0 0%, #F0EBE4 100%);
}

.dish-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s;
}

.dish-card:hover .dish-image {
  transform: scale(1.08);
}

.dish-image-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.placeholder-icon {
  font-size: 32px;
}

.placeholder-text {
  font-size: 28px;
  font-weight: 700;
  color: #D4A853;
}

.badge-specialty, .badge-seasonal, .badge-soldout {
  position: absolute;
  top: 10px;
  padding: 4px 10px;
  font-size: 11px;
  font-weight: 600;
  border-radius: 6px;
}

.badge-specialty {
  left: 10px;
  background: linear-gradient(135deg, #D4A853 0%, #B8860B 100%);
  color: #2D4A3E;
}

.badge-seasonal {
  left: 10px;
  background: linear-gradient(135deg, #4A7C59 0%, #3A6649 100%);
  color: white;
}

.badge-soldout {
  right: 10px;
  background: rgba(0, 0, 0, 0.5);
  color: white;
}

.dish-info {
  padding: 12px;
}

.dish-name {
  font-size: 15px;
  font-weight: 600;
  color: #2D4A3E;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dish-name-en {
  font-size: 11px;
  color: #9A9080;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-top: 2px;
}

.dish-price {
  font-size: 18px;
  font-weight: 700;
  color: #D4A853;
  margin-top: 6px;
}

.btn-add {
  position: absolute;
  bottom: 12px;
  right: 12px;
  width: 34px;
  height: 34px;
  background: linear-gradient(135deg, #4A7C59 0%, #3A6649 100%);
  color: white;
  border: none;
  border-radius: 50%;
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(74, 124, 89, 0.4);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.btn-add:hover {
  transform: scale(1.15);
  box-shadow: 0 6px 16px rgba(74, 124, 89, 0.5);
}

.btn-add:active {
  transform: scale(0.95);
}

.order-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(20px);
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.08);
  border-top: 1px solid rgba(212, 168, 83, 0.1);
  position: relative;
  z-index: 100;
}

.order-left {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: transform 0.3s;
}

.order-left:hover {
  transform: scale(1.02);
}

.order-icon {
  font-size: 24px;
}

.order-info {
  display: flex;
  flex-direction: column;
}

.order-label {
  font-size: 14px;
  color: #3A4A3E;
  font-weight: 500;
}

.order-count {
  font-size: 12px;
  color: #D4A853;
  font-weight: 600;
}

.order-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.order-total {
  font-size: 26px;
  font-weight: 700;
  color: #D4A853;
  text-shadow: 0 2px 4px rgba(212, 168, 83, 0.2);
}

.btn-submit {
  background: linear-gradient(135deg, #4A7C59 0%, #3A6649 100%);
  color: white;
  padding: 14px 32px;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 6px 20px rgba(74, 124, 89, 0.4);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.btn-submit:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(74, 124, 89, 0.5);
}

.btn-submit:active {
  transform: translateY(0);
}

.detail-modal, .order-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.detail-content, .order-content {
  background: white;
  border-radius: 24px;
  width: 90%;
  max-width: 420px;
  max-height: 85vh;
  overflow-y: auto;
  position: relative;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  animation: slideUp 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes slideUp {
  from { 
    opacity: 0;
    transform: translateY(30px) scale(0.95);
  }
  to { 
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.close-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 36px;
  height: 36px;
  background: rgba(0, 0, 0, 0.08);
  border: none;
  border-radius: 50%;
  font-size: 22px;
  color: #666;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
  z-index: 10;
}

.close-btn:hover {
  background: rgba(0, 0, 0, 0.15);
  transform: rotate(90deg);
}

.detail-image-wrapper {
  width: 100%;
  padding-top: 75%;
  position: relative;
  border-radius: 24px 24px 0 0;
  overflow: hidden;
}

.detail-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-image-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #F8F5F0 0%, #F0EBE4 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.detail-image-placeholder .placeholder-icon {
  font-size: 64px;
}

.detail-info {
  padding: 24px;
}

.detail-name {
  font-size: 26px;
  font-weight: 700;
  color: #2D4A3E;
  letter-spacing: 1px;
}

.detail-name-en {
  font-size: 14px;
  color: #9A9080;
  margin-top: 4px;
  letter-spacing: 1px;
}

.detail-price {
  font-size: 34px;
  font-weight: 700;
  color: #D4A853;
  margin-top: 16px;
  text-shadow: 0 2px 4px rgba(212, 168, 83, 0.15);
}

.detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.detail-tag {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  background: rgba(74, 124, 89, 0.08);
  border-radius: 8px;
  font-size: 13px;
  color: #4A7C59;
  font-weight: 500;
}

.tag-icon {
  font-size: 14px;
}

.detail-actions {
  padding: 24px;
  border-top: 1px solid #F0EBE4;
}

.quantity-control {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 32px;
  margin-bottom: 20px;
}

.qty-btn {
  width: 48px;
  height: 48px;
  border: none;
  border-radius: 12px;
  font-size: 26px;
  font-weight: 300;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.qty-minus {
  background: #F0EBE4;
  color: #666;
}

.qty-plus {
  background: linear-gradient(135deg, #4A7C59 0%, #3A6649 100%);
  color: white;
}

.qty-btn:hover {
  transform: scale(1.1);
}

.qty-btn:active {
  transform: scale(0.95);
}

.qty-value {
  font-size: 32px;
  font-weight: 700;
  color: #2D4A3E;
  min-width: 50px;
  text-align: center;
}

.detail-note {
  width: 100%;
  height: 56px;
  padding: 12px 16px;
  border: 1.5px solid #E0D8C8;
  border-radius: 12px;
  font-size: 14px;
  color: #2D4A3E;
  resize: none;
  transition: all 0.3s;
  margin-bottom: 20px;
}

.detail-note:focus {
  outline: none;
  border-color: #D4A853;
  box-shadow: 0 0 0 3px rgba(212, 168, 83, 0.1);
}

.detail-note::placeholder {
  color: #A09888;
}

.btn-add-detail {
  width: 100%;
  background: linear-gradient(135deg, #4A7C59 0%, #3A6649 100%);
  color: white;
  padding: 16px;
  border: none;
  border-radius: 14px;
  font-size: 17px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 6px 20px rgba(74, 124, 89, 0.4);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.btn-add-detail:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(74, 124, 89, 0.5);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px;
  border-bottom: 1px solid #F0EBE4;
}

.order-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #2D4A3E;
}

.order-items {
  padding: 16px 24px;
  max-height: 400px;
  overflow-y: auto;
}

.order-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #F5F3EF;
  transition: background 0.2s;
}

.order-item:hover {
  background: rgba(74, 124, 89, 0.03);
}

.item-name {
  font-size: 16px;
  font-weight: 600;
  color: #2D4A3E;
}

.item-note {
  font-size: 13px;
  color: #9A9080;
  margin-top: 4px;
}

.item-right {
  text-align: right;
}

.item-price {
  font-size: 14px;
  color: #666;
}

.item-subtotal {
  font-size: 18px;
  font-weight: 700;
  color: #D4A853;
  margin-top: 2px;
}

.btn-remove {
  background: rgba(255, 107, 107, 0.1);
  color: #FF6B6B;
  border: none;
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  margin-top: 8px;
  transition: all 0.3s;
}

.btn-remove:hover {
  background: rgba(255, 107, 107, 0.2);
  transform: scale(1.05);
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px;
  border-top: 1px solid #F0EBE4;
  background: #FDFBF7;
}

.order-summary {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.order-summary span:first-child {
  font-size: 16px;
  font-weight: 600;
  color: #2D4A3E;
}

.order-summary span:last-child {
  font-size: 13px;
  color: #9A9080;
}

.btn-submit-order {
  background: linear-gradient(135deg, #4A7C59 0%, #3A6649 100%);
  color: white;
  padding: 14px 40px;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 6px 20px rgba(74, 124, 89, 0.4);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.btn-submit-order:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(74, 124, 89, 0.5);
}

::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.05);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb {
  background: rgba(212, 168, 83, 0.4);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(212, 168, 83, 0.6);
}

.customer-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  animation: fadeIn 0.3s ease;
}

.customer-content {
  background: white;
  border-radius: 24px;
  width: 90%;
  max-width: 520px;
  max-height: 85vh;
  overflow-y: auto;
  position: relative;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  animation: slideUp 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.customer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px;
  border-bottom: 1px solid #F0EBE4;
}

.customer-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #2D4A3E;
}

.booking-badge {
  background: linear-gradient(135deg, #D4A853 0%, #B8860B 100%);
  color: #2D4A3E;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.customer-form {
  padding: 24px;
}

.form-row {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.form-item {
  flex: 1;
}

.form-item.full {
  width: 100%;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #3A4A3E;
  margin-bottom: 8px;
}

.required {
  color: #F56C6C;
}

.form-input {
  width: 100%;
  height: 42px;
  padding: 0 16px;
  border: 1.5px solid #E0D8C8;
  border-radius: 12px;
  font-size: 15px;
  color: #2D4A3E;
  transition: all 0.3s;
  box-sizing: border-box;
}

.form-input:focus {
  outline: none;
  border-color: #D4A853;
  box-shadow: 0 0 0 3px rgba(212, 168, 83, 0.1);
}

.form-input::placeholder {
  color: #A09888;
}

.form-textarea {
  width: 100%;
  height: 80px;
  padding: 12px 16px;
  border: 1.5px solid #E0D8C8;
  border-radius: 12px;
  font-size: 15px;
  color: #2D4A3E;
  resize: none;
  transition: all 0.3s;
  box-sizing: border-box;
}

.form-textarea:focus {
  outline: none;
  border-color: #D4A853;
  box-shadow: 0 0 0 3px rgba(212, 168, 83, 0.1);
}

.form-textarea::placeholder {
  color: #A09888;
}

.customer-footer {
  display: flex;
  gap: 16px;
  padding: 24px;
  border-top: 1px solid #F0EBE4;
  background: #FDFBF7;
}

.btn-cancel {
  flex: 1;
  background: #F0EBE4;
  color: #666;
  padding: 14px;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-cancel:hover {
  background: #E8E2D8;
}

.btn-confirm {
  flex: 2;
  background: linear-gradient(135deg, #4A7C59 0%, #3A6649 100%);
  color: white;
  padding: 14px;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 6px 20px rgba(74, 124, 89, 0.4);
  transition: all 0.3s;
}

.btn-confirm:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(74, 124, 89, 0.5);
}

.form-hint {
  font-size: 12px;
  color: #999;
  line-height: 1.5;
  margin-top: 8px;
  padding: 12px;
  background: #FAFAFA;
  border-radius: 8px;
}
</style>
