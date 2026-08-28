/**
 * 活动 / 首页 Banners / 弹窗 / 公告
 * 后端契约参考（ActivityController 或 BannerController）：
 *   GET  /banners               首页 Banner（支持按门店过滤）
 *   GET  /activities            进行中活动列表
 *   GET  /popup                 首次进入首页的弹窗（新人券/周年庆/活动）
 *   GET  /notice                门店公告文本/跳转（预订须知、临时闭店通知、春节放假）
 */
import http from '@/utils/request'

export function fetchBanners(storeId) {
  return http.get('/banners', storeId ? { storeId } : {}, { skipToken: true })
}

export function fetchActivities(params = {}) {
  return http.get('/activities', params, { skipToken: true })
}

/**
 * 弹窗活动（一般用于首页展示）
 * 返回示例：
 *   { id: 1, image: 'https://...', type: 'LINK', target: '/pages/packages/packages' }
 */
export function fetchPopup(storeId) {
  return http.get('/popup', storeId ? { storeId } : {}, { skipToken: true, silent: true })
}

export function fetchNotice(storeId) {
  return http.get('/notice', storeId ? { storeId } : {}, { skipToken: true, silent: true })
}

export default { fetchBanners, fetchActivities, fetchPopup, fetchNotice }
