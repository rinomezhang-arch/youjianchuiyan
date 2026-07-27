import request from '@/utils/request'

export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

export function getStoreList() {
  return fetch('/api/stores').then(res => res.json())
}
