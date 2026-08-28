/**
 * 用户（账号密码/手机号验证码登录，获取个人信息等）
 * 后端契约参考：AuthController
 *   POST /auth/login          {username, password} 后台用户登录
 *   POST /auth/wx-login       {code, phoneCode?}   小程序专用
 *   POST /auth/me             获取我的信息
 *   POST /auth/phone-login    手机号+短信验证码
 *   POST /auth/sms            发送短信验证码
 */
import http from '@/utils/request'

/** 管理员/前台账号登录（管理后台用，小程序用户一般不用） */
export function pwdLogin(username, password) {
  return http.post('/auth/login', { username, password }, { skipToken: true })
}

/** 手机号 + 短信验证码登录（H5/公众号用） */
export function smsLogin(phone, code) {
  return http.post('/auth/phone-login', { phone, code }, { skipToken: true })
}

/** 发送短信验证码 */
export function sendSms(phone, scene = 'login') {
  return http.post('/auth/sms', { phone, scene }, { skipToken: true })
}

/** 当前登录用户信息 */
export function fetchMe() {
  return http.post('/auth/me', {}, {})
}

export default { pwdLogin, smsLogin, sendSms, fetchMe }
