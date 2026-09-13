<template>
  <div class="mk-h5" :class="`is-${state}`">
    <!-- 加载骨架：与成功态同形，避免数据到达时整页跳动 -->
    <main v-if="state === 'loading'" class="mk-card" aria-busy="true">
      <div class="sk sk-store"></div>
      <div class="sk sk-title"></div>
      <div class="sk sk-date"></div>
      <div class="sk sk-hero"></div>
      <div class="sk sk-line"></div>
      <div class="sk sk-line"></div>
      <p class="mk-loading-text">正在打开活动…</p>
    </main>

    <!-- 未找到：草稿/未审批/跨店/错误链接 统一口径，不区分试探 -->
    <main v-else-if="state === 'not_found'" class="mk-card mk-state-card">
      <div class="mk-state-mark" aria-hidden="true">·</div>
      <h1 class="mk-state-title">活动不存在或已下架</h1>
      <p class="mk-state-text">请通过门店分享的最新链接打开，或直接联系门店了解活动。</p>
      <a class="mk-state-link" href="/stores">查看又见炊烟门店</a>
    </main>

    <!-- 暂停：只停止公开展示与咨询，不渲染活动内容，不给咨询入口 -->
    <main v-else-if="state === 'paused'" class="mk-card mk-state-card">
      <div class="mk-state-mark" aria-hidden="true">‖</div>
      <h1 class="mk-state-title">活动已暂停</h1>
      <p class="mk-state-text">本次活动暂时停止接受咨询，恢复时间请以门店通知为准。</p>
      <a class="mk-state-link" href="/stores">联系门店</a>
    </main>

    <!-- 过期：不显示咨询按钮与表单 -->
    <main v-else-if="state === 'expired'" class="mk-card mk-state-card">
      <div class="mk-state-mark" aria-hidden="true">—</div>
      <h1 class="mk-state-title">活动已结束</h1>
      <p class="mk-state-text">本次活动有效期已过，欢迎查看门店当前的其他活动与菜品。</p>
      <a class="mk-state-link" href="/stores">查看门店信息</a>
    </main>

    <!-- 网络错误：可重试，不把空数据渲染成"暂无优惠" -->
    <main v-else-if="state === 'network_error'" class="mk-card mk-state-card">
      <div class="mk-state-mark" aria-hidden="true">!</div>
      <h1 class="mk-state-title">网络不太顺畅</h1>
      <p class="mk-state-text">活动内容暂时没有取到，请检查网络后重试。</p>
      <button type="button" class="mk-retry" @click="load">重新加载</button>
    </main>

    <!-- 成功：唯一可咨询状态 -->
    <template v-else-if="state === 'success' && activity">
      <main class="mk-card mk-hero-card">
        <p class="mk-store">{{ activity.storeName || '又见炊烟' }}</p>
        <h1 class="mk-title">{{ activity.publicTitle || activity.title }}</h1>
        <p class="mk-validity">
          <span class="mk-validity-dot" aria-hidden="true"></span>
          有效期 {{ formatDate(activity.validFrom) }} 至 {{ formatDate(activity.validTo) }}
        </p>
        <img
          class="mk-hero-img"
          :src="hero"
          width="358"
          height="220"
          alt="又见炊烟门店实景"
          loading="eager"
          decoding="async"
        />
        <!-- 首屏唯一主按钮：文案强制单行，宽度随内容但不越界 -->
        <button ref="heroCtaRef" type="button" class="mk-cta" @click="openForm">咨询档期</button>
      </main>

      <section class="mk-card mk-section">
        <h2 class="mk-h2">活动内容</h2>
        <p class="mk-p">{{ activity.publicSummary || activity.summary || '' }}</p>
        <p v-for="(p, i) in contentParagraphs" :key="i" class="mk-p">{{ p }}</p>
      </section>

      <section v-if="packages.length" class="mk-card mk-section">
        <h2 class="mk-h2">适用套餐或菜品</h2>
        <ul class="mk-list">
          <li v-for="(item, i) in packages" :key="i">{{ item }}</li>
        </ul>
      </section>

      <section v-if="rules.length" class="mk-card mk-section">
        <h2 class="mk-h2">使用规则</h2>
        <ul class="mk-list">
          <li v-for="(item, i) in rules" :key="i">{{ item }}</li>
        </ul>
      </section>

      <section class="mk-card mk-section">
        <h2 class="mk-h2">门店信息</h2>
        <p class="mk-p mk-contact">{{ activity.storeAddress || '地址请致电门店确认' }}</p>
        <p class="mk-p" v-if="activity.storePhone">
          联系电话：
          <a class="mk-tel" :href="`tel:${activity.storePhone}`">{{ activity.storePhone }}</a>
        </p>
        <!-- 第二次同名行动按钮：首屏主按钮滚出视口后才出现（R1 评审项2：初始视口只允许一个 CTA） -->
        <button v-if="showBottomCta" type="button" class="mk-cta mk-cta--second" @click="openForm">咨询档期</button>
      </section>

      <!-- 咨询表单：只提交 sourceCode 与客人字段，storeId 不进请求体 -->
      <section ref="formSection" class="mk-card mk-section mk-form-card" :class="{ 'is-open': formOpen }">
        <div v-if="inquiryState === 'success'" class="mk-inquiry-success">
          <h2 class="mk-h2">咨询已提交</h2>
          <p class="mk-p">您的咨询编号是</p>
          <p class="mk-inquiry-no">{{ inquiryNo }}</p>
          <p class="mk-p">门店会按您留的日期与电话联系确认。请记下编号，便于后续查询。</p>
          <!-- 查询入口只认后端返回的 lookupUrl，前端不自行拼接任何门店地址 -->
          <a class="mk-state-link" :href="lookupUrl">查询入口</a>
        </div>

        <div v-else class="mk-inquiry-form-wrap">
          <h2 class="mk-h2">咨询档期</h2>
          <form class="mk-form" @submit.prevent="submitInquiry">
            <label class="mk-field">
              <span class="mk-label">称呼</span>
              <input v-model.trim="form.customerName" class="mk-input" type="text" maxlength="20"
                     placeholder="您怎么称呼" autocomplete="name" />
              <em v-if="fieldErrors.customerName" class="mk-err">{{ fieldErrors.customerName }}</em>
            </label>
            <label class="mk-field">
              <span class="mk-label">手机号</span>
              <input v-model.trim="form.phone" class="mk-input" type="tel" maxlength="11"
                     placeholder="用于门店与您确认" inputmode="numeric" autocomplete="tel" />
              <em v-if="fieldErrors.phone" class="mk-err">{{ fieldErrors.phone }}</em>
            </label>
            <label class="mk-field">
              <span class="mk-label">期望到店日期</span>
              <input v-model="form.expectedDate" class="mk-input" type="date" />
              <em v-if="fieldErrors.expectedDate" class="mk-err">{{ fieldErrors.expectedDate }}</em>
            </label>
            <label class="mk-field">
              <span class="mk-label">用餐人数</span>
              <input v-model.number="form.partySize" class="mk-input" type="number" min="1" max="99"
                     step="1" inputmode="numeric" placeholder="1-99" />
              <em v-if="fieldErrors.partySize" class="mk-err">{{ fieldErrors.partySize }}</em>
            </label>
            <label class="mk-field">
              <span class="mk-label">备注（选填）</span>
              <textarea v-model.trim="form.remark" class="mk-input mk-textarea" rows="3"
                        maxlength="200" placeholder="包厢、布置等需求"></textarea>
            </label>

            <p v-if="submitError" class="mk-submit-err" role="alert">{{ submitError }}</p>
            <button type="submit" class="mk-cta mk-cta--submit" :disabled="submitting">
              {{ submitting ? '提交中…' : '提交咨询' }}
            </button>
            <p class="mk-idem-note">提交失败再次点击会沿用同一请求，不会产生重复咨询。</p>
          </form>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import {
  getPublicMarketingActivity,
  recordMarketingEvent,
  submitPublicInquiry,
  readPublicActivity,
  resolveHeroAsset,
  validateInquiryForm,
  buildInquiryPayload,
  newRequestId
} from '@/api/marketing'

const route = useRoute()

// 六态：loading / success / not_found / paused / expired / network_error
const state = ref('loading')
const activity = ref(null)

const formOpen = ref(false)
const formSection = ref(null)
const form = ref({ customerName: '', phone: '', expectedDate: '', partySize: null, remark: '' })
const fieldErrors = ref({})
const submitting = ref(false)
const submitError = ref('')
const inquiryState = ref('form') // form | success
const inquiryNo = ref('')
// 查询入口只来自后端咨询响应的 lookupUrl；没有后端给的地址就不显示任何链接（R1 评审项3）。
const lookupUrl = ref('')

// 幂等键：一次提交意图生成一个，失败重试沿用；成功后才换新的。
let inquiryRequestId = ''

// 底部第二次同名按钮：初始视口只允许一个 CTA（R1 评审项2）。
// 首屏主按钮完全滚出视口顶部后才显示；用 scroll + getBoundingClientRect，
// 不依赖 IntersectionObserver（happy-dom 等无排版环境矩形恒为 0 → 保持隐藏，行为确定）。
const showBottomCta = ref(false)
const heroCtaRef = ref(null)

function onScrollReveal() {
  const el = heroCtaRef.value
  if (!el) return
  const r = el.getBoundingClientRect()
  // 主按钮完全离开视口（滚到上方或下方之外）才亮出底部同名入口
  showBottomCta.value = r.bottom < 0 || r.top > window.innerHeight
}

const hero = computed(() => (activity.value ? resolveHeroAsset(activity.value) : ''))

const contentJson = computed(() => {
  const raw = activity.value?.contentJson ?? activity.value?.content_json
  if (!raw) return {}
  if (typeof raw === 'object') return raw
  try { return JSON.parse(raw) } catch { return {} }
})
const contentParagraphs = computed(() => arrOf(contentJson.value.content))
const packages = computed(() => arrOf(contentJson.value.packages))
const rules = computed(() => arrOf(contentJson.value.rules))

function arrOf(v) {
  if (Array.isArray(v)) return v.map((x) => (typeof x === 'string' ? x : x?.name || x?.title || '')).filter(Boolean)
  return []
}

function formatDate(d) {
  if (!d) return '待定'
  const dt = new Date(d)
  if (Number.isNaN(dt.getTime())) return String(d).slice(0, 10)
  return `${dt.getFullYear()}年${dt.getMonth() + 1}月${dt.getDate()}日`
}

async function load() {
  state.value = 'loading'
  const slug = String(route.params.publicSlug || '')
  try {
    const res = await getPublicMarketingActivity(slug)
    const verdict = readPublicActivity(res, new Date())
    if (verdict.state === 'success') {
      activity.value = verdict.data
      state.value = 'success'
      document.title = `${verdict.data.publicTitle || verdict.data.title || '活动详情'} · 又见炊烟`
      // 首屏渲染后立即核对一次滚动位置（例如刷新时恢复在页面中部，则底部入口立即可用）。
      nextTick(onScrollReveal)
      trackView(verdict.data)
    } else {
      activity.value = verdict.data || null
      state.value = verdict.state // paused / expired / not_found
    }
  } catch (e) {
    // HTTP 404 = 后端统一口径的"不存在/不可公开"；断网/超时（无 response）才是网络错误。
    // 业务非 200 的其他状态保守落到网络错误页，绝不渲染空"成功页"。
    if (e?.response?.status === 404) {
      state.value = 'not_found'
    } else {
      state.value = 'network_error'
    }
  }
}

/** 脱敏浏览事件：visitor_key 只发 SHA-256 哈希；埋点失败不打扰客人。 */
async function trackView(snap) {
  try {
    let visitorKey = ''
    let raw = localStorage.getItem('mk_visitor_key')
    if (!raw) {
      raw = newRequestId()
      localStorage.setItem('mk_visitor_key', raw)
    }
    if (globalThis.crypto?.subtle) {
      const buf = await globalThis.crypto.subtle.digest('SHA-256', new TextEncoder().encode(raw))
      visitorKey = Array.from(new Uint8Array(buf)).map((b) => b.toString(16).padStart(2, '0')).join('')
    }
    await recordMarketingEvent({
      publicationId: snap.publicationId ?? snap.publication_id ?? 0,
      sourceCode: snap.sourceCode ?? snap.source_code ?? '',
      eventType: 'view',
      visitorKey,
      requestId: newRequestId()
    })
  } catch {
    /* 埋点不是浏览前提，静默 */
  }
}

function openForm() {
  formOpen.value = true
  requestAnimationFrame(() => {
    formSection.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
}

async function submitInquiry() {
  if (!activity.value) return
  fieldErrors.value = validateInquiryForm(form.value)
  if (Object.keys(fieldErrors.value).length) return

  if (!inquiryRequestId) inquiryRequestId = newRequestId()
  const sourceCode = activity.value.sourceCode ?? activity.value.source_code ?? ''
  // buildInquiryPayload 显式剔除 storeId：门店由服务端用 sourceCode 反查。
  const payload = buildInquiryPayload(form.value, sourceCode, inquiryRequestId)

  submitting.value = true
  submitError.value = ''
  try {
    const res = await submitPublicInquiry(payload)
    const data = res?.data ?? {}
    const no = data.inquiryNo ?? data.inquiry_no
    const lookup = data.lookupUrl ?? data.lookup_url
    // 成功双门槛（R1 评审项3）：编号与查询入口必须同时来自后端响应，
    // 缺任一字段一律不当成功，前端也绝不自行拼接门店地址（防假成功分叉）。
    if (!no || !lookup) throw new Error('响应缺少咨询编号或查询入口')
    inquiryNo.value = String(no)
    lookupUrl.value = String(lookup)
    inquiryState.value = 'success'
    formOpen.value = true
    inquiryRequestId = ''
  } catch (e) {
    // 留在原地、保留全部输入；同一 requestId 供重试幂等。
    submitError.value = e?.response
      ? `提交未成功（${e.response.status}），请稍后重试，不会产生重复咨询。`
      : (e?.message?.includes('响应缺少')
          ? '服务未返回完整的咨询回执（编号或查询入口），本次不按成功处理，请稍后重试。'
          : '网络异常，提交未完成，请点击重试（同一请求不会重复登记）。')
  } finally {
    submitting.value = false
  }
}

onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScrollReveal)
})

onMounted(() => {
  window.addEventListener('scroll', onScrollReveal, { passive: true })
  load()
})
</script>

<style scoped>
/*
  客人 H5：深绿为体，金色只用于主按钮与有效期；不用渐变、玻璃拟态、自动轮播。
  390px 视口下零横向滚动：固定 390 宽度的元素一律不出现，图片 max-width:100%。
*/
.mk-h5 {
  min-height: 100vh;
  background: #f5f2ea; /* 柔白米底，纯色无渐变 */
  padding: 12px 12px 28px;
  box-sizing: border-box;
  overflow-x: hidden;
  color: #24382f;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.mk-card {
  max-width: 480px;
  margin: 0 auto 12px;
  background: #fffdf8;
  border: 1px solid #e6dfd0;
  border-radius: 12px;
  padding: 16px;
  box-sizing: border-box;
}

.mk-store {
  margin: 0 0 6px;
  font-size: 13px;
  letter-spacing: 0.08em;
  color: #5c7268;
}

.mk-title {
  margin: 0 0 10px;
  font-size: 23px;
  line-height: 1.35;
  font-weight: 700;
  color: #2d4a3e; /* 品牌深绿 */
}

.mk-validity {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 14px;
  font-size: 13px;
  color: #9a7b2e; /* 克制金色，仅用于有效期提示 */
}
.mk-validity-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #c4a35a;
  flex: 0 0 auto;
}

.mk-hero-img {
  display: block;
  width: 100%;
  height: auto;
  border-radius: 8px;
  background: #ece6d8;
  animation: mk-rise 0.45s ease both; /* 轻微进入，受 reduced-motion 约束 */
}

/* 主按钮：金色仅出现在此；white-space:nowrap 保证文案永远单行 */
.mk-cta {
  display: block;
  width: 100%;
  margin-top: 14px;
  padding: 13px 18px;
  border: none;
  border-radius: 8px;
  background: #c4a35a;
  color: #fffdf8;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0.12em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: clip;
  cursor: pointer;
  transition: transform 0.12s ease, background 0.12s ease;
}
.mk-cta:active { transform: scale(0.985); background: #b39247; }
.mk-cta:disabled { opacity: 0.6; cursor: default; }
.mk-cta--second { margin-top: 16px; }
.mk-cta--submit { font-size: 16px; padding: 12px; }

.mk-section { animation: mk-rise 0.5s ease both; }
.mk-h2 {
  margin: 0 0 10px;
  font-size: 16px;
  font-weight: 700;
  color: #2d4a3e;
  padding-left: 9px;
  border-left: 3px solid #c4a35a;
}
.mk-p { margin: 0 0 8px; font-size: 14px; line-height: 1.7; color: #3b4f45; }
.mk-list { margin: 0; padding-left: 18px; font-size: 14px; line-height: 1.8; color: #3b4f45; }
.mk-contact { word-break: break-word; }
.mk-tel { color: #2d4a3e; font-weight: 600; text-decoration: none; border-bottom: 1px solid #c4a35a; }

/* 表单 */
.mk-form-card { display: none; }
.mk-form-card.is-open { display: block; }
.mk-form { display: flex; flex-direction: column; gap: 12px; }
.mk-field { display: flex; flex-direction: column; gap: 5px; }
.mk-label { font-size: 13px; color: #5c7268; }
.mk-input {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 12px;
  border: 1px solid #d8d0bf;
  border-radius: 8px;
  background: #fffefa;
  font-size: 15px;
  color: #24382f;
}
.mk-input:focus { outline: 2px solid rgba(45, 74, 62, 0.35); border-color: #2d4a3e; }
.mk-textarea { resize: vertical; }
.mk-err { font-size: 12px; color: #b04a3a; font-style: normal; }
.mk-submit-err {
  margin: 0;
  padding: 9px 11px;
  background: #f7ece9;
  border: 1px solid #e0bdb4;
  border-radius: 8px;
  font-size: 13px;
  color: #9a3f31;
}
.mk-idem-note { margin: 0; font-size: 12px; color: #8a8372; text-align: center; }

.mk-inquiry-no {
  margin: 6px 0 10px;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: #2d4a3e;
  word-break: break-all;
}

/* 状态页（未找到/暂停/过期/网络错误） */
.mk-state-card { text-align: center; padding: 44px 22px; }
.mk-state-mark {
  width: 46px; height: 46px; margin: 0 auto 14px;
  display: flex; align-items: center; justify-content: center;
  border: 1.5px solid #b9c7bf; border-radius: 50%;
  font-size: 22px; color: #5c7268;
}
.mk-state-title { margin: 0 0 10px; font-size: 19px; color: #2d4a3e; }
.mk-state-text { margin: 0 0 18px; font-size: 14px; line-height: 1.7; color: #5c7268; }
.mk-state-link {
  display: inline-block;
  padding: 9px 18px;
  border: 1px solid #2d4a3e;
  border-radius: 8px;
  color: #2d4a3e;
  font-size: 14px;
  text-decoration: none;
  white-space: nowrap;
}
.mk-retry {
  padding: 9px 22px;
  border: none;
  border-radius: 8px;
  background: #2d4a3e;
  color: #fffdf8;
  font-size: 14px;
  cursor: pointer;
  white-space: nowrap;
}
.mk-retry:active { transform: scale(0.98); }

/* 骨架 */
.sk { background: #ece6d8; border-radius: 6px; margin-bottom: 12px; overflow: hidden; }
.sk-store { width: 32%; height: 12px; }
.sk-title { width: 72%; height: 24px; }
.sk-date { width: 56%; height: 13px; }
.sk-hero { width: 100%; height: 200px; border-radius: 8px; }
.sk-line { width: 100%; height: 12px; }
.mk-loading-text { margin: 4px 0 0; font-size: 12px; color: #8a8372; }

@keyframes mk-rise {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
@media (prefers-reduced-motion: reduce) {
  .mk-hero-img, .mk-section { animation: none; }
  .mk-cta, .mk-retry { transition: none; }
}
</style>
