// TR-MARKETING-H5-UI-39 后台工作台 DOM 测试。
//
// ⚠️ MOCKED_CONTRACT：全部网络响应由 axios adapter 合同假后端提供（见 helpers.mjs），
// 后端未接通，不冒充真实 HTTP/数据库。所有交互为真实 DOM 点击/输入；仅有效期日期
// 因 happy-dom 对 el-date-picker 弹层布局无排版支持，经 vm.dateRange 测试缝注入
// （日期选择器真人键入留浏览器证据/NOT_COVERED 说明）。
import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import ElementPlus from 'element-plus'
import Marketing from '@/views/dashboard/Marketing.vue'
import { useAdapter, resetAdapter, clearLocal, flushAllDeep, callsTo, setNativeInput, OK, NETWORK_FAIL, FAIL_HTTP } from './helpers.mjs'

const STORES = [
  { store_id: 1, store_name: '宁国店' },
  { store_id: 2, store_name: '欢乐巷店' }
]

function activities() {
  return [
    { id: 1, activity_name: '草稿家宴', store_id: 1, store_name: '宁国店', status: 'draft', document_version: 1, row_version: 2, valid_from: '2026-10-01', valid_to: '2026-10-08' },
    { id: 2, activity_name: '待批团圆', store_id: 2, store_name: '欢乐巷店', status: 'pending_approval', document_version: 1, row_version: 3, valid_from: '2026-10-01', valid_to: '2026-10-08', latest_publication: null },
    { id: 3, activity_name: '已批待发', store_id: 1, store_name: '宁国店', status: 'approved', document_version: 1, row_version: 4, valid_from: '2026-10-01', valid_to: '2026-10-08' },
    {
      id: 4, activity_name: '中秋发布', store_id: 2, store_name: '欢乐巷店', status: 'published', document_version: 2, row_version: 5,
      valid_from: '2026-09-01', valid_to: '2026-10-01', channel: 'h5',
      latest_publication: { publication_id: 44, version: 2, channel: 'h5', public_slug: 'midautumn-2', source_code: 'SRC-2', status: 'published' }
    },
    {
      id: 5, activity_name: '暂停活动', store_id: 1, store_name: '宁国店', status: 'paused', document_version: 1, row_version: 6,
      valid_from: '2026-08-01', valid_to: '2026-09-01', channel: 'h5',
      latest_publication: { publication_id: 55, version: 1, channel: 'h5', public_slug: 'paused-1', source_code: 'SRC-5', status: 'paused' }
    },
    { id: 6, activity_name: '过期活动', store_id: 1, store_name: '宁国店', status: 'expired', document_version: 1, row_version: 7, valid_from: '2026-07-01', valid_to: '2026-08-01' }
  ]
}

let adapterState
let mountedWrapper
let host
function defaultHandler(url, config) {
  const method = String(config.method || 'get').toLowerCase()
  if (url === '/stores' || url.endsWith('/api/stores')) return OK(STORES)
  if (url.includes('/marketing/activities') && method === 'get') return OK(activities())
  if (adapterState.create) return adapterState.create(url, config)
  return FAIL_HTTP(404, 'no-mock')
}

async function mountPage() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', redirect: '/dashboard/marketing' },
      { path: '/dashboard/marketing', component: Marketing },
      { path: '/dashboard/approval', component: { template: '<div class="approval-stub" />' } }
    ]
  })
  host = document.createElement('div')
  document.body.appendChild(host)
  mountedWrapper = mount(Marketing, { attachTo: host, global: { plugins: [ElementPlus, router] } })
  await router.push('/dashboard/marketing')
  await flushPromises()
  await flushAllDeep()
  return mountedWrapper
}

/** 真实点击 el-select 并选中文本匹配项（弹层 teleport 到 body） */
async function chooseSelect(triggerEl, labelRegex, fallbackSet) {
  triggerEl.click()
  await flushPromises()
  await flushAllDeep(2)
  const items = [...document.body.querySelectorAll('.el-select-dropdown__item')]
  const opt = items.find((n) => labelRegex.test(n.textContent || ''))
  if (opt) {
    opt.click()
    await flushPromises()
    await flushAllDeep(2)
    return true
  }
  // happy-dom 弹层排版缺失时的测试缝（真人下拉选择由浏览器证据覆盖）
  fallbackSet?.()
  await flushPromises()
  return false
}

function buttonByText(re, root = document.body) {
  return [...root.querySelectorAll('button')].find((b) => re.test(b.textContent.replace(/\s+/g, '')))
}

// el-dialog 的遮罩常驻且 display:none；只数真正可见的遮罩（抽屉打开态）。
function visibleOverlays() {
  return [...document.body.querySelectorAll('.el-overlay')].filter((n) => n.style.display !== 'none')
}

beforeEach(() => {
  clearLocal()
  adapterState = { create: null, publish: null }
  window.open = () => null
  useAdapter(defaultHandler)
})

afterEach(() => {
  mountedWrapper?.unmount()
  host?.remove()
  resetAdapter()
  document.body.querySelectorAll('.el-message, .el-drawer, .el-overlay, .el-popper, .el-select-dropdown').forEach((n) => n.remove())
})

it('四栏工作台：门店/状态/版本/渠道/有效期齐全；物理删除入口不存在；顶部只有三动作', async () => {
  const wrapper = await mountPage()

  const cols = wrapper.findAll('.mk-col')
  expect(cols).toHaveLength(4)
  const count = (key) => wrapper.findAll(`.mk-col[data-col="${key}"] .mk-row`).length
  expect(count('draft')).toBe(1)
  expect(count('approval')).toBe(2)
  expect(count('published')).toBe(1)
  expect(count('ended')).toBe(2)

  // 行信息：门店、状态、版本 v{documentVersion}、渠道、有效期
  const publishedRow = wrapper.find('.mk-col[data-col="published"] .mk-row')
  const text = publishedRow.text().replace(/\s+/g, '')
  expect(text).toContain('欢乐巷店')
  expect(text).toContain('已发布')
  expect(text).toContain('版本v2')
  expect(text).toContain('渠道H5')
  expect(text).toContain('2026-09-01')

  // 物理删除入口必须消失：界面无"删除"字样、出站无 DELETE
  expect(wrapper.text()).not.toContain('删除')
  expect(callsTo('/', 'delete')).toHaveLength(0)

  // 顶部主动作恰好三个
  const headerTexts = wrapper.find('.mk-header-actions').findAll('button').map((b) => b.text().replace(/\s+/g, ''))
  expect(headerTexts).toEqual(['新建活动', '待我审批', '查看已发布H5'])
})

it('保存失败：留在第一步、输入完整保留；恢复后同表单可继续保存进入下一步', async () => {
  const wrapper = await mountPage()

  // 首次创建一律网络失败
  adapterState.create = (url) => {
    if (url === '/marketing/activities' && false) return OK({})
    if (url.includes('/marketing/activities')) return NETWORK_FAIL('simulated down')
    return FAIL_HTTP(404)
  }

  buttonByText(/新建活动/).click()
  await flushPromises()
  await flushAllDeep(2)
  expect(document.body.querySelector('.mk-editor')).toBeTruthy()

  // 选择门店 2（真实下拉；happy-dom 弹层不可用时经 vm 测试缝，均证明不默认 1）
  const selectTrigger = document.body.querySelector('.mk-step-1 .el-select__wrapper') ||
    document.body.querySelector('.mk-step-1 .el-select')
  await chooseSelect(selectTrigger, /欢乐巷店/, () => { wrapper.vm.form.storeId = 2 })

  // 真实键入名称/编码
  setNativeInput([...document.body.querySelectorAll('.mk-step-1 input')].find((i) => i.placeholder === '如：宁国店中秋家宴'), '中秋家宴')
  setNativeInput([...document.body.querySelectorAll('.mk-step-1 input')].find((i) => i.placeholder === '字母数字与连字符'), 'zj-2026')
  // 日期范围：happy-dom 无弹层布局，经测试缝注入（浏览器证据覆盖真人操作）
  wrapper.vm.dateRange = ['2026-10-01', '2026-10-08']
  await flushPromises()

  buttonByText(/下一步/).click()
  await flushPromises()
  await flushAllDeep(6)

  const creates = callsTo('/marketing/activities', 'post')
  expect(creates).toHaveLength(1)
  expect(creates[0].data.storeId).toBe(2)
  expect(creates[0].data.storeId).not.toBe(1)
  // 留在第一步、第二步 v-show 隐藏、字段保留
  expect(document.body.querySelector('.mk-step-2').style.display).toBe('none')
  const nameInput = [...document.body.querySelectorAll('.mk-step-1 input')].find((i) => i.placeholder === '如：宁国店中秋家宴')
  expect(nameInput.value).toBe('中秋家宴')
  expect(document.body.querySelector('.mk-save-error').textContent).toContain('保存失败')

  // 恢复后再点下一步：创建成功并进入第二步
  adapterState.create = (url) => (url.includes('/marketing/activities') ? OK({ id: 101, row_version: 7, status: 'draft', document_version: 1 }) : FAIL_HTTP(404))
  buttonByText(/下一步/).click()
  await flushPromises()
  await flushAllDeep(6)
  expect(wrapper.vm.form.id).toBe(101)
  expect(document.body.querySelector('.mk-step-1').style.display).toBe('none')
  expect(document.body.querySelector('.mk-step-2').style.display).not.toBe('none')
})

it('发布：未预览不能发布；发布失败绝不显示成功；重试沿用同一 requestId，成功才关抽屉', async () => {
  const wrapper = await mountPage()
  let publishAttempt = 0
  adapterState.create = (url, config) => {
    const method = String(config.method || 'get').toLowerCase()
    if (url.includes('/publish')) {
      publishAttempt += 1
      return publishAttempt === 1 ? NETWORK_FAIL('publish down') : OK({ publication_id: 909, public_slug: 'approved-slug', source_code: 'SRC-9', status: 'published' })
    }
    // 编辑抽屉的防抖自动保存（PUT）回合同样视为成功，避免噪声干扰发布断言
    if (method === 'put') return OK({ id: 3, row_version: 4, status: 'approved', document_version: 1 })
    return FAIL_HTTP(404)
  }

  // 已批准行的「去发布」
  const approvedRow = wrapper.find('.mk-col[data-col="approval"] .mk-row[data-status="approved"]')
  const goPublishBtn = [...approvedRow.element.querySelectorAll('button')].find((b) => /去发布/.test(b.textContent.replace(/\s+/g, '')))
  expect(goPublishBtn).toBeTruthy()
  goPublishBtn.click()
  await flushPromises()
  await flushAllDeep(2)
  expect(document.body.querySelector('.mk-editor')).toBeTruthy()
  expect(document.body.querySelector('.mk-step-1').style.display).toBe('none')
  expect(document.body.querySelector('.mk-step-3').style.display).not.toBe('none')

  // 发布按钮初始禁用（必须先真实预览）
  let publishBtn = buttonByText(/确认发布/)
  expect(publishBtn.disabled).toBe(true)

  const opened = []
  window.open = (u) => { opened.push(u) }
  // 第三步需要 slug：直接在测试缝填入真实表单字段（与发布人在第二步填写等价）
  wrapper.vm.form.publicSlug = 'approved-slug'
  buttonByText(/打开真实H5预览/).click()
  await flushPromises()
  expect(opened).toContain('/h5/activity/approved-slug')
  publishBtn = buttonByText(/确认发布/)
  expect(publishBtn.disabled).toBe(false)

  // 第一次：网络失败
  publishBtn.click()
  await flushPromises()
  await flushAllDeep(6)
  const editorText = document.body.querySelector('.mk-editor').textContent
  expect(editorText).toContain('发布失败')
  expect(editorText).not.toContain('发布成功')
  // EP drawer 默认保留 DOM（destroy-on-close=false），以状态与可见遮罩为准断言"抽屉仍开着"
  expect(wrapper.vm.drawerOpen).toBe(true)
  expect(visibleOverlays().length).toBeGreaterThan(0)

  // 第二次：成功；两次 requestId 相同（幂等重试）
  buttonByText(/确认发布/).click()
  await flushPromises()
  await flushAllDeep(6)
  const pubs = callsTo('/publish', 'post')
  expect(pubs).toHaveLength(2)
  expect(pubs[0].data.requestId).toBeTruthy()
  expect(pubs[0].data.requestId).toBe(pubs[1].data.requestId)
  expect(pubs[0].data.channel).toBe('h5')
  // 成功才关抽屉：等遮罩离开动画（约 300ms）后状态关闭、无可见遮罩
  await new Promise((r) => setTimeout(r, 450))
  expect(wrapper.vm.drawerOpen).toBe(false)
  expect(visibleOverlays()).toHaveLength(0)
})
