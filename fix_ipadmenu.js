const fs = require('fs');

const content = `
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
        <button class="btn-back" @click="goBack">返回</button>
      </div>
    </div>
    <div class="menu-tabs">
      <button 
        v-for="tab in menuTabs" 
        :key="tab.value"
        :class="['tab-btn', { active: currentMenuType === tab.value }]"
        @click="switchMenu(tab.value)"
      >
        <span v-if="tab.icon">{{ tab.icon }}</span>
        <span>{{ tab.label }}</span>
        <span class="tab-en">{{ tab.en }}</span>
      </button>
    </div>
    <div class="main-content">
      <div class="category-sidebar">
        <div class="search-box">
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
            <div class="dish-image">
              <img :src="dish.image_url || '/images/default-dish.png'" :alt="dish.dish_name" />
              <span v-if="dish.is_specialty === 1" class="badge-specialty">招牌</span>
              <span v-if="dish.is_seasonal === 1" class="badge-seasonal">时令</span>
            </div>
            <div class="dish-info">
              <div class="dish-name">{{ dish.dish_name }}</div>
              <div class="dish-name-en">{{ dish.dish_name_en }}</div>
              <div class="dish-price">¥{{ formatPrice(dish.sale_price) }}</div>
            </div>
            <button class="btn-add" @click.stop="addToOrder(dish)">
              <span>+</span>
            </button>
          </div>
        </div>
      </div>
    </div>
    <div class="order-bar">
      <div class="order-left" @click="showOrderDetail">
        <span class="order-icon">🛒</span>
        <span class="order-label">查看点菜单</span>
        <span class="order-count">{{ orderCount }}道菜</span>
      </div>
      <div class="order-right">
        <span class="order-total">¥{{ formatPrice(orderTotal) }}</span>
        <button class="btn-submit" @click="submitOrder">提交后厨</button>
      </div>
    </div>
    <div v-if="showDetail" class="detail-modal" @click="closeDetail">
      <div class="detail-content" @click.stop>
        <button class="close-btn" @click="closeDetail">×</button>
        <div class="detail-image">
          <img :src="currentDish.image_url || '/images/default-dish.png'" :alt="currentDish.dish_name" />
        </div>
        <div class="detail-info">
          <div class="detail-name">{{ currentDish.dish_name }}</div>
          <div class="detail-name-en">{{ currentDish.dish_name_en }}</div>
          <div class="detail-price">¥{{ formatPrice(currentDish.sale_price) }}</div>
          <div v-if="currentDish.cooking_method" class="detail-item">烹饪方式: {{ currentDish.cooking_method }}</div>
          <div v-if="currentDish.taste" class="detail-item">口味: {{ currentDish.taste }}</div>
          <div v-if="currentDish.spicy_level" class="detail-item">辣度: {{ getSpicyLevel(currentDish.spicy_level) }}</div>
        </div>
        <div class="detail-actions">
          <div class="quantity-control">
            <button @click="decreaseQty" class="qty-btn">-</button>
            <span class="qty-value">{{ detailQty }}</span>
            <button @click="increaseQty" class="qty-btn">+</button>
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
              <div v-if="item.dish_note" class="item-note">{{ item.dish_note }}</div>
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const currentTable = ref('A06')
const currentMenuType = ref('alacarte')
const categories = ref([])
const selectedCategory = ref('')
const dishList = ref([])
const searchKeyword = ref('')
const orderItems = ref([])
const showDetail = ref(false)
const showOrder = ref(false)
const currentDish = ref({})
const detailQty = ref(1)
const detailNote = ref('')
const currentLang = ref('zh')

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
    const res = await fetch(\`/api/ipad/dish/category?menu_type=\${currentMenuType.value}\`, {
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
    let url = \`/api/ipad/dish/list?menu_type=\${currentMenuType.value}\`
    if (selectedCategory.value) {
      url += \`&category_id=\${selectedCategory.value}\`
    }
    if (searchKeyword.value) {
      url += \`&keyword=\${encodeURIComponent(searchKeyword.value)}\`
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
        booking_id: currentTable.value,
        dish_id: dish.dish_id,
        quantity: 1
      })
    })
    const json = await res.json()
    if (json.code === 200) {
      ElMessage.success(\`已添加：\${dish.dish_name}\`)
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
    const res = await fetch(\`/api/ipad/order/current?table_id=\${currentTable.value}\`, {
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
    const res = await fetch(\`/api/ipad/order/dish/remove\`, {
      method: 'DELETE',
      headers: { 
        'Content-Type': 'application/json',
        'X-Store-Id': '1',
        'X-Staff-Id': '1',
        'X-Device-Sn': 'test001',
        'X-Client-Type': 'ipad'
      },
      body: JSON.stringify({ dish_booking_id: item.dish_booking_id || item.id })
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
        booking_id: currentTable.value,
        dish_id: currentDish.value.dish_id,
        quantity: detailQty.value,
        dish_note: detailNote.value
      })
    })
    const json = await res.json()
    if (json.code === 200) {
      ElMessage.success(\`已添加：\${currentDish.value.dish_name} × \${detailQty.value}\`)
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

const submitOrder = async () => {
  if (orderItems.value.length === 0) {
    ElMessage.warning('订单为空')
    return
  }
  try {
    const res = await fetch('/api/ipad/order/send-kitchen', {
      method: 'POST',
      headers: { 
        'Content-Type': 'application/json',
        'X-Store-Id': '1',
        'X-Staff-Id': '1',
        'X-Device-Sn': 'test001',
        'X-Client-Type': 'ipad'
      },
      body: JSON.stringify({
        booking_id: currentTable.value
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

onMounted(() => {
  fetchCategories()
  fetchDishes()
  fetchOrder()
})
</script>

<style scoped>
.ipad-menu {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #F5F3EF;
  font-family: '字酷堂字体', sans-serif;
}

.ipad-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: #2D4A3E;
  color: white;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  width: 44px;
  height: 44px;
  background: #D4A853;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: bold;
  color: #2D4A3E;
}

.brand-name {
  font-size: 18px;
  font-weight: bold;
}

.brand-en {
  font-size: 12px;
  opacity: 0.8;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.table-badge {
  background: rgba(255,255,255,0.2);
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 13px;
}

.lang-switch {
  cursor: pointer;
  font-size: 13px;
}

.btn-back {
  background: transparent;
  border: 1px solid rgba(255,255,255,0.3);
  color: white;
  padding: 6px 12px;
  border-radius: 6px;
  cursor: pointer;
}

.menu-tabs {
  display: flex;
  background: #2D4A3E;
  padding: 0 20px 12px;
  gap: 8px;
}

.tab-btn {
  flex: 1;
  padding: 10px 16px;
  border: none;
  border-radius: 8px;
  background: rgba(255,255,255,0.1);
  color: rgba(255,255,255,0.8);
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.tab-btn.active {
  background: #D4A853;
  color: #2D4A3E;
}

.tab-en {
  font-size: 10px;
  opacity: 0.8;
}

.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.category-sidebar {
  width: 160px;
  background: #EDEAE5;
  padding: 12px;
  display: flex;
  flex-direction: column;
}

.search-box {
  margin-bottom: 12px;
}

.search-input {
  width: 100%;
  height: 32px;
  padding: 0 10px;
  border: 1px solid #D4C8B8;
  border-radius: 6px;
  font-size: 13px;
}

.category-list {
  flex: 1;
  overflow-y: auto;
}

.category-item {
  padding: 10px 12px;
  margin-bottom: 4px;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: #3A4A3E;
  transition: all 0.2s;
}

.category-item.active {
  background: #4A7C59;
  color: white;
}

.cat-count {
  font-size: 12px;
  opacity: 0.7;
}

.dish-area {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.dish-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.dish-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  position: relative;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  cursor: pointer;
}

.dish-image {
  width: 100%;
  padding-top: 80%;
  position: relative;
  background: #F5F3EF;
}

.dish-image img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.badge-specialty, .badge-seasonal {
  position: absolute;
  top: 6px;
  padding: 2px 6px;
  font-size: 10px;
  border-radius: 4px;
}

.badge-specialty {
  left: 6px;
  background: #D4A853;
  color: #2D4A3E;
}

.badge-seasonal {
  right: 6px;
  background: #4A7C59;
  color: white;
}

.dish-info {
  padding: 8px;
}

.dish-name {
  font-size: 13px;
  font-weight: bold;
  color: #2D4A3E;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dish-name-en {
  font-size: 10px;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dish-price {
  font-size: 14px;
  font-weight: bold;
  color: #D4A853;
  margin-top: 4px;
}

.btn-add {
  position: absolute;
  bottom: 8px;
  right: 8px;
  width: 28px;
  height: 28px;
  background: #4A7C59;
  color: white;
  border: none;
  border-radius: 50%;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.order-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: white;
  box-shadow: 0 -2px 8px rgba(0,0,0,0.06);
}

.order-left {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.order-icon {
  font-size: 18px;
}

.order-label {
  font-size: 14px;
  color: #3A4A3E;
}

.order-count {
  background: #D4A853;
  color: #2D4A3E;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: bold;
}

.order-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.order-total {
  font-size: 20px;
  font-weight: bold;
  color: #D4A853;
}

.btn-submit {
  background: #4A7C59;
  color: white;
  padding: 10px 24px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
}

.detail-modal, .order-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.detail-content, .order-content {
  background: white;
  border-radius: 16px;
  width: 90%;
  max-width: 400px;
  max-height: 80vh;
  overflow-y: auto;
  position: relative;
}

.close-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 32px;
  height: 32px;
  background: #eee;
  border: none;
  border-radius: 50%;
  font-size: 20px;
  cursor: pointer;
  z-index: 10;
}

.detail-image {
  width: 100%;
  padding-top: 70%;
  position: relative;
}

.detail-image img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-info {
  padding: 20px;
}

.detail-name {
  font-size: 22px;
  font-weight: bold;
  color: #2D4A3E;
}

.detail-name-en {
  font-size: 13px;
  color: #999;
  margin-bottom: 12px;
}

.detail-price {
  font-size: 28px;
  font-weight: bold;
  color: #D4A853;
  margin-bottom: 16px;
}

.detail-item {
  font-size: 13px;
  color: #666;
  margin-bottom: 4px;
}

.detail-actions {
  padding: 20px;
  border-top: 1px solid #eee;
}

.quantity-control {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  margin-bottom: 16px;
}

.qty-btn {
  width: 40px;
  height: 40px;
  background: #EDEAE5;
  border: none;
  border-radius: 8px;
  font-size: 20px;
  cursor: pointer;
}

.qty-value {
  font-size: 24px;
  font-weight: bold;
}

.detail-note {
  width: 100%;
  height: 48px;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 13px;
  margin-bottom: 16px;
}

.btn-add-detail {
  width: 100%;
  background: #4A7C59;
  color: white;
  padding: 14px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  cursor: pointer;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #eee;
}

.order-header h3 {
  margin: 0;
  font-size: 18px;
}

.order-items {
  padding: 16px;
}

.order-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}

.item-name {
  font-size: 15px;
  font-weight: bold;
}

.item-note {
  font-size: 12px;
  color: #999;
}

.item-right {
  text-align: right;
}

.item-price {
  font-size: 13px;
  color: #666;
}

.item-subtotal {
  font-size: 15px;
  font-weight: bold;
  color: #D4A853;
}

.btn-remove {
  background: #FF6B6B;
  color: white;
  border: none;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  margin-top: 4px;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-top: 1px solid #eee;
}

.order-summary {
  display: flex;
  gap: 16px;
  font-size: 14px;
}

.btn-submit-order {
  background: #4A7C59;
  color: white;
  padding: 12px 32px;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  cursor: pointer;
}
</style>
`.trim();

fs.writeFileSync('/home/ubuntu/canyin/frontend_v3/src/views/dashboard/IpadMenu.vue', content, 'utf8');
console.log('File written successfully');
