import request from '@/utils/request'

export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

export function getStoreList() {
  return request({
    url: '/stores',
    method: 'get'
  })
}
