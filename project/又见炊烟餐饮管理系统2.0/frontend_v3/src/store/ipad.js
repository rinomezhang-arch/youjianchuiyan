import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/**
 * iPad 点餐子系统状态管理
 * 独立于 PC 端 user store，避免状态污染
 */
export const useIpadStore = defineStore('ipad', () => {
  // 设备信息
  const deviceSn = ref(localStorage.getItem('ipad_device_sn') || `IPAD_${Date.now()}`)
  const printConfig = ref({ print_port: 9100, print_width: 80, print_template_code: 'default' })

  // 员工/门店信息（iPad 登录后写入）
  const staffInfo = ref(JSON.parse(localStorage.getItem('ipad_staff') || 'null'))
  const storeInfo = ref(JSON.parse(localStorage.getItem('ipad_store') || 'null'))

  // 当前预订单（点餐临时状态）
  const currentBooking = ref(JSON.parse(sessionStorage.getItem('ipad_booking') || 'null'))
  const cartItems = ref(JSON.parse(sessionStorage.getItem('ipad_cart') || '[]'))

  // 计算属性
  const isLoggedIn = computed(() => !!staffInfo.value?.staff_id)
  const storeId = computed(() => storeInfo.value?.id || staffInfo.value?.store_id || 1)
  const storeName = computed(() => storeInfo.value?.store_name || staffInfo.value?.store_name || '')
  const staffId = computed(() => staffInfo.value?.staff_id || null)
  const staffName = computed(() => staffInfo.value?.staff_name || '')

  // 登录成功
  function setLogin(data) {
    staffInfo.value = {
      staff_id: data.staff_id,
      staff_name: data.staff_name,
      staff_phone: data.staff_phone,
      role_type: data.role_type,
      store_id: data.store_id
    }
    storeInfo.value = {
      id: data.store_id,
      store_name: data.store_name
    }
    // 设备配置
    if (data.device_sn) deviceSn.value = data.device_sn
    if (data.print_port) printConfig.value.print_port = data.print_port
    if (data.print_template_code) printConfig.value.print_template_code = data.print_template_code

    localStorage.setItem('ipad_staff', JSON.stringify(staffInfo.value))
    localStorage.setItem('ipad_store', JSON.stringify(storeInfo.value))
    localStorage.setItem('ipad_device_sn', deviceSn.value)
  }

  // 退出登录
  function logout() {
    staffInfo.value = null
    storeInfo.value = null
    currentBooking.value = null
    cartItems.value = []
    localStorage.removeItem('ipad_staff')
    localStorage.removeItem('ipad_store')
    sessionStorage.removeItem('ipad_booking')
    sessionStorage.removeItem('ipad_cart')
  }

  // 选择门店（登录前）
  function selectStore(store) {
    storeInfo.value = { id: store.id, store_name: store.store_name || store.name }
    localStorage.setItem('ipad_store', JSON.stringify(storeInfo.value))
  }

  // 开台成功
  function openTable(booking) {
    currentBooking.value = booking
    cartItems.value = []
    sessionStorage.setItem('ipad_booking', JSON.stringify(booking))
    sessionStorage.setItem('ipad_cart', '[]')
  }

  // 加菜
  function addToCart(dish) {
    const existing = cartItems.value.find(i => i.dish_id === dish.dish_id)
    if (existing) {
      existing.dish_quantity += 1
    } else {
      cartItems.value.push({ ...dish, dish_quantity: 1 })
    }
    sessionStorage.setItem('ipad_cart', JSON.stringify(cartItems.value))
  }

  // 改数量
  function updateCartQty(dishId, qty) {
    const item = cartItems.value.find(i => i.dish_id === dishId)
    if (item) {
      item.dish_quantity = qty
      if (item.dish_quantity <= 0) {
        removeFromCart(dishId)
        return
      }
    }
    sessionStorage.setItem('ipad_cart', JSON.stringify(cartItems.value))
  }

  // 删菜
  function removeFromCart(dishId) {
    cartItems.value = cartItems.value.filter(i => i.dish_id !== dishId)
    sessionStorage.setItem('ipad_cart', JSON.stringify(cartItems.value))
  }

  // 清空购物车
  function clearCart() {
    cartItems.value = []
    currentBooking.value = null
    sessionStorage.removeItem('ipad_cart')
    sessionStorage.removeItem('ipad_booking')
  }

  // 购物车总价
  const cartTotal = computed(() => {
    return cartItems.value.reduce((sum, i) => sum + (Number(i.sale_price || i.unit_price || 0) * i.dish_quantity), 0)
  })

  // 购物车总件数
  const cartCount = computed(() => {
    return cartItems.value.reduce((sum, i) => sum + i.dish_quantity, 0)
  })

  return {
    deviceSn, printConfig, staffInfo, storeInfo, currentBooking, cartItems,
    isLoggedIn, storeId, storeName, staffId, staffName, cartTotal, cartCount,
    setLogin, logout, selectStore, openTable, addToCart, updateCartQty, removeFromCart, clearCart
  }
})
