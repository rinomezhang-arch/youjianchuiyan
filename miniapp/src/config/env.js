/**
 * 环境配置
 * 部署前请至少替换：
 *   - API_BASE_URL_PROD（你的SpringBoot后端域名）
 *   - manifest.json 里的 mp-weixin.appid
 *   - WECHAT_APP_ID（小程序原始ID，一般跟AppID对应）
 *   - 订阅消息模板ID（tmplIds）：需要到微信公众平台-订阅消息里申请
 *   - 微信支付：mchId / 证书 / notifyURL 都是后端处理，前端直接调 uni.requestPayment 接收后端的支付参数即可
 */

export const APP_ENV = (() => {
  // #ifdef H5
  if (location && location.hostname === 'localhost') return 'dev'
  if (location && /youjianchuiyan\.com$/.test(location.hostname)) return 'prod'
  return 'dev'
  // #endif
  // #ifdef MP-WEIXIN
  return 'prod'   // 小程序端默认指向生产；调试时手动改成 dev
  // #endif
  return 'dev'
})()

/* ========= 后端 API ========= */
export const API_BASE_URL = APP_ENV === 'prod'
  ? 'https://youjianchuiyan.com/api'
  : 'http://localhost:8080/api'      // 本地 SpringBoot 默认端口

/* ========= 静态资源（菜品/套餐图片由后端返回 imageUrl；此处放通用占位） ========= */
export const STATIC_BASE = APP_ENV === 'prod'
  ? 'https://youjianchuiyan.com/static'
  : 'https://youjianchuiyan.com/static'

export const IMG_PLACEHOLDER = `${STATIC_BASE}/placeholder-dish.png`

/* ========= 微信配置（请替换成自己的） ========= */
export const WECHAT_APP_ID = 'YOUR_WECHAT_APPID_HERE'       // 小程序/公众号 AppID

/**
 * 订阅消息模板ID数组
 * 需要在微信公众平台 -> 订阅消息 -> 我的模板里申请并复制
 * 示例：tmpl_order_book_success 是"预订成功通知"，字段建议：
 *   thing6  门店
 *   date3   时间
 *   thing10 人数/包厢
 *   thing2  备注
 */
export const SUBSCRIBE_TMPL_IDS = {
  BOOK_SUCCESS: 'TMPL_ID_BOOK_SUCCESS_PLACEHOLDER',   // 预订成功
  BOOK_REMIND:  'TMPL_ID_BOOK_REMIND_PLACEHOLDER',    // 到店前提醒
  PAY_SUCCESS:  'TMPL_ID_PAY_SUCCESS_PLACEHOLDER',    // 支付成功
}

/* ========= 门店 ========= */
export const DEFAULT_STORE_ID = 1   // 默认宁国店，ID=1；宣城店 ID=2

/* ========= 存储 Key ========= */
export const STORAGE_KEYS = {
  TOKEN:      'yjcy_token',
  USER:       'yjcy_user',
  OPENID:     'yjcy_openid',
  STORES:     'yjcy_stores',
  STORE_ID:   'yjcy_store_id',
  CART:       'yjcy_cart_'          // 后接 storeId
}
