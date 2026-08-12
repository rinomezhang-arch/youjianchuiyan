import request from '@/utils/request'

export const listBanquetNotices = (params) => request({ url: '/banquet-notices', method: 'get', params })
export const getBanquetNotice = (id, params) => request({ url: `/banquet-notices/${id}`, method: 'get', params })
export const createBanquetNotice = (data) => request({ url: '/banquet-notices', method: 'post', data })
export const updateBanquetNotice = (id, data) => request({ url: `/banquet-notices/${id}`, method: 'put', data })
export const copyBanquetNotice = (id, params) => request({ url: `/banquet-notices/${id}/copy`, method: 'post', params })
export const transitionBanquetNotice = (id, data) => request({ url: `/banquet-notices/${id}/transition`, method: 'post', data })
export const attachBanquetNoticeScan = (id, data) => request({ url: `/banquet-notices/${id}/scan`, method: 'post', data })
