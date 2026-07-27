import request from '@/api/http'

export function generateBanquetSuggestion(data) {
  return request({
    url: '/ai/banquet/suggest',
    method: 'post',
    data
  })
}

export function generateDishRecommendation(data) {
  return request({
    url: '/ai/dish/recommend',
    method: 'post',
    data
  })
}

export function generateCopywriting(data) {
  return request({
    url: '/ai/copy/generate',
    method: 'post',
    data
  })
}
