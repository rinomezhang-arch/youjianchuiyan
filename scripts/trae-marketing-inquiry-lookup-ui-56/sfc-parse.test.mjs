// TR-MARKETING-INQUIRY-LOOKUP-UI-56 SFC 解析与隐私纪律测试（MOCKED_CONTRACT，零网络）。
// 运行：node sfc-parse.test.mjs
// 覆盖：SFC 可解析可编译、路由深链接注册、隐私纪律（手机号不落存储/控制台）、页面文案口径。
import assert from 'node:assert/strict'
import { createRequire } from 'node:module'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const require = createRequire(import.meta.url)
const { parse, compileScript, compileTemplate } = require('@vue/compiler-sfc')

const here = fileURLToPath(new URL('.', import.meta.url))
const sfcPath = fileURLToPath(new URL('../../frontend_v3/src/views/site/MarketingInquiryLookup.vue', import.meta.url))
const routerPath = fileURLToPath(new URL('../../frontend_v3/src/router/index.js', import.meta.url))
const apiPath = fileURLToPath(new URL('../../frontend_v3/src/api/marketing.js', import.meta.url))

let pass = 0
let fail = 0
function check(name, fn) {
  try {
    fn()
    pass++
    console.log(`PASS | ${name}`)
  } catch (e) {
    fail++
    console.log(`FAIL | ${name} | ${e.message}`)
  }
}

const source = readFileSync(sfcPath, 'utf8')
const routerSource = readFileSync(routerPath, 'utf8')
const apiSource = readFileSync(apiPath, 'utf8')

check('S1 SFC 解析无错误', () => {
  const { descriptor, errors } = parse(source, { filename: 'MarketingInquiryLookup.vue' })
  assert.deepEqual(errors, [])
  assert.ok(descriptor.template, '缺少 template')
  assert.ok(descriptor.scriptSetup, '缺少 <script setup>')
  assert.ok(descriptor.styles.length >= 1, '缺少 style')
})

check('S2 script setup 编译通过（setup 返回包含查询/复制/视图状态）', () => {
  const { descriptor } = parse(source, { filename: 'MarketingInquiryLookup.vue' })
  let content = ''
  try {
    ;({ content } = compileScript(descriptor, { id: 'lk-test' }))
  } catch (e) {
    throw new Error('compileScript 抛错: ' + e.message)
  }
  for (const token of ['onSubmit', 'copyInquiryNo', 'lookupBookingInquiry', 'normalizeInquiryLookup', 'isValidLookupPhone']) {
    assert.ok(content.includes(token), `setup 编译产物缺 ${token}`)
  }
})

check('S3 模板编译通过且引用状态视图 data-testid', () => {
  const { descriptor } = parse(source, { filename: 'MarketingInquiryLookup.vue' })
  const result = compileTemplate({
    id: 'lk-test',
    filename: 'MarketingInquiryLookup.vue',
    source: descriptor.template.content,
    compilerOptions: { mode: 'module' }
  })
  assert.equal(result.errors.length, 0)
  for (const tid of ['state-loading', 'state-empty', 'state-error', 'state-result', 'inquiry-no', 'phone-input', 'lookup-btn', 'copy-inquiry-no', 'retry-btn']) {
    assert.ok(result.code.includes(tid), `模板缺 data-testid=${tid}`)
  }
})

check('S4 路由注册深链接 /h5/inquiry/:inquiryNo（history 模式，刷新与直开可用）', () => {
  assert.ok(routerSource.includes("path: '/h5/inquiry/:inquiryNo'"))
  assert.ok(routerSource.includes('MarketingInquiryLookup'))
  assert.ok(routerSource.includes('createWebHistory'))
})

check('S5 契约层：lookup 为 POST /public/booking-inquiry/lookup，请求体仅两键', () => {
  assert.ok(apiSource.includes("url: '/public/booking-inquiry/lookup'"))
  assert.ok(apiSource.includes("inquiryNo: String(inquiryNo ?? '').trim()"))
  assert.ok(apiSource.includes("phone: String(phone ?? '').trim()"))
})

check('P1 隐私：页面代码不写 localStorage/sessionStorage，手机号不进任何存储', () => {
  assert.ok(!source.includes('localStorage'), '页面不得使用 localStorage')
  assert.ok(!source.includes('sessionStorage'), '页面不得使用 sessionStorage')
  assert.ok(!source.includes('document.cookie'), '页面不得写 cookie')
})

check('P2 隐私：页面不打印手机号/请求体到控制台', () => {
  assert.ok(!/console\.(log|info|debug|warn)\(/.test(source), '页面不得使用 console 输出')
})

check('P3 统一口径：空结果文案不区分查无/手机号不符/非法输入（只看用户可见模板）', () => {
  const template = parse(source, { filename: 'x.vue' }).descriptor.template.content
  assert.ok(template.includes('未找到匹配的咨询记录'))
  for (const banned of ['手机号不符', '不存在该咨询', '格式不正确', '编号无效']) {
    assert.ok(!template.includes(banned), `模板空结果文案不得出现「${banned}」区分失败原因`)
  }
})

check('P4 独立错误态：系统故障文案与空结果不同，且保留输入（无清空逻辑）', () => {
  assert.ok(source.includes('系统繁忙，请稍后重试'))
  assert.ok(!/phone\.value\s*=\s*['"]{2}/.test(source), '不得清空手机号输入')
})

check('P5 视觉纪律：无渐变/玻璃拟态/emoji 图标，金色仅按钮与强调', () => {
  assert.ok(!source.includes('linear-gradient'), '不得使用渐变')
  assert.ok(!source.includes('backdrop-filter'), '不得使用玻璃拟态')
})

check('P6 白名单展示：模板不含备注/操作人/门店经营数据展示位', () => {
  const template = parse(source, { filename: 'x.vue' }).descriptor.template.content
  for (const banned of ['备注', '操作人', '客单价', '预算', '转化率']) {
    assert.ok(!template.includes(banned), `模板不得出现「${banned}」展示位`)
  }
})

console.log(`\nSUMMARY sfc-parse: ${pass} passed, ${fail} failed, ${pass + fail} total`)
process.exit(fail === 0 ? 0 : 2)
