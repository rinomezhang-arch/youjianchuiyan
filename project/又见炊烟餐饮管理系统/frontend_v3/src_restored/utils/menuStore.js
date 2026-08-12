/**
 * 菜单数据存储 (纯内存，不落盘到浏览器)
 * 数据从后端 API 获取，内存缓存仅用于当前会话
 */

// 内存缓存（页面刷新即清空）
let _memDishes = null
let _memCatOrder = null
let _memOrders = {}
let _memPackages = null

function memGet(key) {
  if (key === 'dishes') return _memDishes
  if (key === 'catOrder') return _memCatOrder
  if (key === 'packages') return _memPackages
  return null
}

function memSet(key, val) {
  if (key === 'dishes') _memDishes = val
  else if (key === 'catOrder') _memCatOrder = val
  else if (key === 'packages') _memPackages = val
}

function memGetOrders() { return _memOrders }
function memSetOrders(v) { _memOrders = v }

// ===== 菜品库 =====
export function getDishes() {
  return memGet('dishes') || []
}

export function saveDishes(dishes) {
  memSet('dishes', dishes)
}

export function addDish(dish) {
  const dishes = getDishes()
  dishes.push(dish)
  saveDishes(dishes)
}

export function updateDishById(id, data) {
  const dishes = getDishes()
  const idx = dishes.findIndex(d => d.id === id)
  if (idx >= 0) {
    dishes[idx] = { ...dishes[idx], ...data }
    saveDishes(dishes)
    return true
  }
  return false
}

export function deleteDishById(id) {
  const dishes = getDishes().filter(d => d.id !== id)
  saveDishes(dishes)
}

// ===== 分类 =====
export function getCategories() {
  const dishes = getDishes()
  const cats = [...new Set(dishes.map(d => d.category).filter(Boolean))]
  return cats.sort()
}

export function getCatOrderMap() {
  return memGet('catOrder') || {}
}

export function saveCatOrderMap(order) {
  memSet('catOrder', order)
}

// ===== 点菜订单（内存） =====
export function getOrders() {
  return memGetOrders()
}

export function saveOrders(orders) {
  memSetOrders(orders)
}

export function getOrderKey(date, period, tableName) {
  return `${date}_${period}_${tableName}`
}

export function getTableOrders(date, period, tableName) {
  const orders = getOrders()
  const key = getOrderKey(date, period, tableName)
  return orders[key] || []
}

export function addDishToOrder(date, period, tableName, dishCode, qty = 1) {
  const orders = getOrders()
  const key = getOrderKey(date, period, tableName)
  if (!orders[key]) orders[key] = []
  const existing = orders[key].find(d => d.dishCode === dishCode)
  if (existing) {
    existing.qty += qty
  } else {
    orders[key].push({ dishCode, qty, remark: '' })
  }
  saveOrders(orders)
}

export function removeDishFromOrder(date, period, tableName, dishCode) {
  const orders = getOrders()
  const key = getOrderKey(date, period, tableName)
  if (orders[key]) {
    orders[key] = orders[key].filter(d => d.dishCode !== dishCode)
    if (orders[key].length === 0) delete orders[key]
    saveOrders(orders)
  }
}

export function updateDishQty(date, period, tableName, dishCode, delta) {
  const orders = getOrders()
  const key = getOrderKey(date, period, tableName)
  if (!orders[key]) return
  const item = orders[key].find(d => d.dishCode === dishCode)
  if (!item) return
  item.qty += delta
  if (item.qty <= 0) {
    orders[key] = orders[key].filter(d => d.dishCode !== dishCode)
    if (orders[key].length === 0) delete orders[key]
  }
  saveOrders(orders)
}

export function updateDishRemark(date, period, tableName, dishCode, remark) {
  const orders = getOrders()
  const key = getOrderKey(date, period, tableName)
  if (!orders[key]) return
  const item = orders[key].find(d => d.dishCode === dishCode)
  if (item) {
    item.remark = remark
    saveOrders(orders)
  }
}

export function clearTableOrders(date, period, tableName) {
  const orders = getOrders()
  const key = getOrderKey(date, period, tableName)
  delete orders[key]
  saveOrders(orders)
}

// ===== 套餐 =====
export function getPackages() {
  return memGet('packages') || []
}

export function savePackages(packages) {
  memSet('packages', packages)
}

// ===== 菜单状态检查 =====
export function hasMenuOrder(date, period, tableNames) {
  const orders = getOrders()
  return tableNames.some(name => {
    const key = getOrderKey(date, period, name)
    return orders[key] && orders[key].length > 0
  })
}

// ===== 初始化示例数据 =====
export function initSampleDishes() {
  if (getDishes().length > 0) return
  const samples = [
    { id: 'D001', name: '油淋酱椒大黄鱼', category: '海鲜', price: 168, unit: '条', spicy: 2, cookingTime: 35, mainIngredient: '大黄鱼-酱椒', festiveName: '福寿有余笑颜开' },
    { id: 'D002', name: '翡翠鱼片羹', category: '汤羹', price: 88, unit: '份', spicy: 0, cookingTime: 25, mainIngredient: '草鱼片-菠菜汁', festiveName: '翡翠玉液润心田' },
    { id: 'D003', name: '秘制红烧肉', category: '肉类', price: 78, unit: '份', spicy: 0, cookingTime: 45, mainIngredient: '五花肉', festiveName: '红袍加身步步高' },
    { id: 'D004', name: '蒜蓉粉丝蒸扇贝', category: '海鲜', price: 128, unit: '份', spicy: 1, cookingTime: 15, mainIngredient: '扇贝-粉丝', festiveName: '金贝满堂喜团圆' },
    { id: 'D005', name: '椒盐牛蛙', category: '小炒', price: 68, unit: '份', spicy: 2, cookingTime: 12, mainIngredient: '牛蛙', festiveName: '金蟾纳福乐逍遥' },
    { id: 'D006', name: '银芽鸡丝', category: '小炒', price: 48, unit: '份', spicy: 0, cookingTime: 10, mainIngredient: '鸡胸肉-豆芽', festiveName: '银丝织锦福绵长' },
    { id: 'D007', name: '牡丹虾仁', category: '海鲜', price: 138, unit: '份', spicy: 0, cookingTime: 15, mainIngredient: '虾仁-青花', festiveName: '国色天香迎贵客' },
    { id: 'D008', name: '佛跳墙', category: '汤羹', price: 288, unit: '盅', spicy: 0, cookingTime: 120, mainIngredient: '鲍鱼-海参-花菇', festiveName: '佛光普照福满堂' },
    { id: 'D009', name: '金陵盐水鸭', category: '凉菜', price: 58, unit: '份', spicy: 0, cookingTime: 0, mainIngredient: '鸭', festiveName: '金玉良缘好相逢' },
    { id: 'D010', name: '蟹粉豆腐', category: '小炒', price: 88, unit: '份', spicy: 0, cookingTime: 12, mainIngredient: '蟹粉-嫩豆腐', festiveName: '金玉满堂合家欢' },
    { id: 'D011', name: '避风塘炒蟹', category: '海鲜', price: 198, unit: '只', spicy: 2, cookingTime: 20, mainIngredient: '膏蟹-蒜酥', festiveName: '横行霸道财气旺' },
    { id: 'D012', name: '铁板黑椒牛柳', category: '肉类', price: 98, unit: '份', spicy: 1, cookingTime: 15, mainIngredient: '牛里脊-黑椒', festiveName: '铁板定乾坤' },
    { id: 'D013', name: '白灼西兰花', category: '素菜', price: 32, unit: '份', spicy: 0, cookingTime: 8, mainIngredient: '西兰花', festiveName: '翠绿常青步步春' },
    { id: 'D014', name: '松仁玉米', category: '素菜', price: 38, unit: '份', spicy: 0, cookingTime: 8, mainIngredient: '玉米粒-松仁', festiveName: '金玉满仓五谷丰' },
    { id: 'D015', name: '杨枝甘露', category: '甜品', price: 28, unit: '位', spicy: 0, cookingTime: 5, mainIngredient: '芒果-西米', festiveName: '甘露润心甜如蜜' }
  ]
  saveDishes(samples)
  saveCatOrderMap({})
}

// 在模块加载时初始化
if (typeof window !== 'undefined') {
  initSampleDishes()
}
