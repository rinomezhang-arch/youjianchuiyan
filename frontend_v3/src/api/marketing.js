import request from '@/utils/request'

export function getMarketingOverview() {
  return request({ url: '/marketing/overview', method: 'get' })
}

export function getMemberTiers() {
  return request({ url: '/marketing/member-tiers', method: 'get' })
}

export function getActiveActivities() {
  return request({ url: '/marketing/activities', method: 'get' })
}

export function getPlatformStats() {
  return request({ url: '/marketing/platform-stats', method: 'get' })
}
