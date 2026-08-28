/**
 * 菜品/分类
 * 后端契约参考：DishesController
 *   GET  /dishes              分页列表（参数 categoryId, keyword, status=1, storeId）
 *   GET  /dishes/categories   分类列表
 *   GET  /dishes/search?q=    搜索
 *   GET  /dishes/{id}         菜品详情
 */
import http from '@/utils/request'

export function fetchCategories(params = {}) {
  return http.get('/dishes/categories', { status: 1, ...params }, { skipToken: true })
}

export function fetchDishes(params = {}) {
  return http.get('/dishes', { page: 1, size: 200, status: 1, ...params }, { skipToken: true })
}

export function fetchDishDetail(id) {
  return http.get(`/dishes/${id}`, {}, { skipToken: true })
}

export function searchDishes(q, params = {}) {
  return http.get('/dishes/search', { q, ...params }, { skipToken: true })
}

/**
 * 获取首页展示的"招牌菜"（后端推荐列表；如没有接口就按销量倒序取前N个）
 * 如果你的后端有专门接口，把 URL 改成对应路径
 */
export function fetchFeaturedDishes(storeId, limit = 6) {
  // 这里直接复用 /dishes，带推荐标识参数；实际后端支持时调整
  return http.get('/dishes', { featured: 1, size: limit, status: 1 }, { skipToken: true })
}

export default { fetchCategories, fetchDishes, fetchDishDetail, searchDishes, fetchFeaturedDishes }
