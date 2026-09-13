// TR-MARKETING-H5-UI-39 纯契约测试（无 DOM、无网络；MOCKED_CONTRACT 的形状定义层）。
import { describe, it, expect } from 'vitest'
import {
  columnOfStatus,
  isPubliclyVisible,
  readPublicActivity,
  buildInquiryPayload,
  validateInquiryForm,
  validateActivityForm,
  normalizeActivity,
  resolveHeroAsset
} from '@/api/marketing'

const DAY = 86400000
const around = (deltaFrom, deltaTo = deltaFrom + DAY) => ({
  validFrom: new Date(Date.now() + deltaFrom).toISOString(),
  validTo: new Date(Date.now() + deltaTo).toISOString()
})

describe('状态机 → 工作台四栏', () => {
  it('draft/changes_requested→草稿，pending/approved→待审批，published→已发布，其余→已结束', () => {
    expect(columnOfStatus('draft')).toBe('draft')
    expect(columnOfStatus('changes_requested')).toBe('draft')
    expect(columnOfStatus('pending_approval')).toBe('approval')
    expect(columnOfStatus('approved')).toBe('approval')
    expect(columnOfStatus('published')).toBe('published')
    for (const s of ['paused', 'expired', 'cancelled']) expect(columnOfStatus(s)).toBe('ended')
  })
})

describe('公开可见性（唯一权威：status=published + 有效期内）', () => {
  it('草稿/待审批/已批准/暂停一律不可见', () => {
    for (const status of ['draft', 'pending_approval', 'approved', 'paused', 'cancelled']) {
      expect(isPubliclyVisible({ status, ...around(0) })).toBe(false)
    }
  })
  it('published 但过期/未开始不可见，有效期内可见', () => {
    expect(isPubliclyVisible({ status: 'published', ...around(-2 * DAY, -DAY) })).toBe(false)
    expect(isPubliclyVisible({ status: 'published', ...around(DAY, 2 * DAY) })).toBe(false)
    expect(isPubliclyVisible({ status: 'published', ...around(-DAY, DAY) })).toBe(true)
  })
  it('readPublicActivity 把合同响应分成六态且暂停/过期保留状态原因', () => {
    const v = around(-DAY, DAY)
    expect(readPublicActivity({ code: 200, data: { status: 'published', ...v } }).state).toBe('success')
    expect(readPublicActivity({ data: { status: 'paused' } }).state).toBe('paused')
    expect(readPublicActivity({ data: { status: 'expired' } }).state).toBe('expired')
    expect(readPublicActivity({ data: { status: 'published', ...around(-2 * DAY, -DAY) } }).state).toBe('expired')
    expect(readPublicActivity({ data: { status: 'draft' } }).state).toBe('not_found')
    expect(readPublicActivity(null).state).toBe('not_found')
  })
})

describe('咨询提交体：只信 sourceCode，绝不带 storeId', () => {
  it('即使表单混入 storeId/store_id 也被剔除', () => {
    const payload = buildInquiryPayload(
      { customerName: '王女士', phone: '13800000001', expectedDate: '2026-10-01', partySize: 4, remark: '包厢', storeId: 99, store_id: 99 },
      'SRC-OK-1', 'REQ-1'
    )
    expect(payload).toEqual({
      sourceCode: 'SRC-OK-1',
      requestId: 'REQ-1',
      customerName: '王女士',
      phone: '13800000001',
      expectedDate: '2026-10-01',
      partySize: 4,
      remark: '包厢'
    })
    expect('storeId' in payload).toBe(false)
    expect('store_id' in payload).toBe(false)
  })
  it('表单校验：手机号/日期/人数', () => {
    expect(Object.keys(validateInquiryForm({ customerName: 'a', phone: '13800000001', expectedDate: '2026-10-01', partySize: 2 }))).toHaveLength(0)
    expect(validateInquiryForm({ customerName: '', phone: '123', expectedDate: '', partySize: 0 }).phone).toBeTruthy()
  })
})

describe('活动表单：门店必选、不得默认 1', () => {
  it('空表单必须报 storeId；storeId 传 0/未给都不合法', () => {
    const errs = validateActivityForm({})
    expect(errs.storeId).toBe('必须选择门店')
    expect(validateActivityForm({ storeId: 2, activityName: 'a', activityCode: 'c', validFrom: '2026-10-01', validTo: '2026-10-02', publicTitle: 't' }).storeId).toBeUndefined()
  })
  it('结束早于开始报错', () => {
    const errs = validateActivityForm({ storeId: 2, activityName: 'a', activityCode: 'c', validFrom: '2026-10-02', validTo: '2026-10-01', publicTitle: 't' })
    expect(errs.validRange).toBeTruthy()
  })
})

describe('规范化与真实照片', () => {
  it('snake_case 后端行可规范化', () => {
    const a = normalizeActivity({
      id: 7, activity_name: '中秋', store_id: 2, store_name: '欢乐巷店', status: 'published',
      document_version: 3, row_version: 9, valid_from: '2026-10-01', valid_to: '2026-10-08',
      latest_publication: { publication_id: 77, version: 3, channel: 'h5', public_slug: 'slug-x', source_code: 'SRC-X', status: 'published' }
    })
    expect(a.activityName).toBe('中秋')
    expect(a.storeId).toBe(2)
    expect(a.documentVersion).toBe(3)
    expect(a.publicSlug).toBe('slug-x')
    expect(a.channel).toBe('h5')
  })
  it('hero 只引用既有 /site-photos 真实资产，外部 URL 被拒并按门店兜底', () => {
    expect(resolveHeroAsset({ heroAssetUrl: '/site-photos/storefront-dusk.jpg', storeId: 1 }))
      .toBe('/site-photos/storefront-dusk.jpg')
    expect(resolveHeroAsset({ heroAssetUrl: 'https://evil.example/x.jpg', storeId: 1 }))
      .toBe('/site-photos/storefront-entrance.jpg')
    expect(resolveHeroAsset({ storeId: 2 })).toBe('/site-photos/storefront-dusk.jpg')
  })
})
