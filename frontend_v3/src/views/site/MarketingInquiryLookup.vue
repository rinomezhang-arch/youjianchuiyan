<template>
  <div class="lk-h5">
    <section class="lk-card lk-head">
      <p class="lk-eyebrow">又见炊烟 · 宴会咨询</p>
      <h1 class="lk-title">咨询进度回查</h1>
      <div class="lk-no-row">
        <span class="lk-no-label">咨询编号</span>
        <span class="lk-no" data-testid="inquiry-no">{{ inquiryNo || '—' }}</span>
        <button type="button" class="lk-copy" data-testid="copy-inquiry-no" @click="copyInquiryNo">
          {{ copied ? '已复制' : '复制' }}
        </button>
      </div>
      <p class="lk-hint">输入提交咨询时使用的手机号，即可查询该编号的处理进度。无需登录。</p>
    </section>

    <section class="lk-card">
      <form class="lk-form" @submit.prevent="onSubmit">
        <label class="lk-field">
          <span class="lk-label">手机号</span>
          <input
            v-model.trim="phone"
            class="lk-input"
            type="tel"
            inputmode="numeric"
            maxlength="11"
            autocomplete="off"
            name="lookup-phone"
            placeholder="请填写 11 位手机号"
            data-testid="phone-input"
          />
        </label>
        <button type="submit" class="lk-cta" :disabled="loading" data-testid="lookup-btn">
          {{ loading ? '查询中…' : '查询进度' }}
        </button>
      </form>
    </section>

    <!-- 查询中 -->
    <section v-if="view === 'loading'" class="lk-card lk-state-card" data-testid="state-loading">
      <div class="sk sk-line" style="width: 58%"></div>
      <div class="sk sk-line"></div>
      <div class="sk sk-line" style="width: 40%"></div>
      <p class="lk-loading-text">正在查询…</p>
    </section>

    <!-- 统一不泄露空结果：查无 / 号码对不上 / 非法输入 共用同一文案，不区分原因 -->
    <section v-else-if="view === 'empty'" class="lk-card lk-state-card" data-testid="state-empty">
      <div class="lk-state-mark">？</div>
      <h2 class="lk-state-title">未找到匹配的咨询记录</h2>
      <p class="lk-state-text">请核对链接中的咨询编号与当时填写的手机号后重试。</p>
    </section>

    <!-- 网络/系统故障：与空结果不同的错误态；输入保留，可重试 -->
    <section v-else-if="view === 'error'" class="lk-card lk-state-card" data-testid="state-error">
      <div class="lk-state-mark">！</div>
      <h2 class="lk-state-title">系统繁忙，请稍后重试</h2>
      <p class="lk-state-text">查询服务暂时不可用，您填写的内容已保留。</p>
      <button type="button" class="lk-retry" data-testid="retry-btn" @click="onSubmit">重试</button>
    </section>

    <!-- 成功：仅白名单字段（咨询号/状态/期望日期/人数/提交时间/已转预订的预订号） -->
    <section v-else-if="view === 'result'" class="lk-card lk-result" data-testid="state-result">
      <h2 class="lk-h2">查询结果</h2>
      <dl class="lk-rows">
        <div class="lk-row"><dt>咨询编号</dt><dd>{{ result.inquiryNo }}</dd></div>
        <div class="lk-row">
          <dt>当前状态</dt>
          <dd><span class="lk-status" :class="'lk-status--' + result.statusKind">{{ result.statusText }}</span></dd>
        </div>
        <div class="lk-row"><dt>期望到店日期</dt><dd>{{ result.expectedDate || '—' }}</dd></div>
        <div class="lk-row"><dt>人数</dt><dd>{{ result.partySize != null ? result.partySize + ' 人' : '—' }}</dd></div>
        <div class="lk-row"><dt>提交时间</dt><dd>{{ result.createdAt || '—' }}</dd></div>
        <div v-if="result.bookingId" class="lk-row"><dt>预订编号</dt><dd>{{ result.bookingId }}</dd></div>
      </dl>
      <p class="lk-footnote">如需修改咨询内容或加急处理，请致电门店。</p>
    </section>

    <p class="lk-footer">又见炊烟私房菜 · 仅可凭本人手机号查询</p>
  </div>
</template>

<script setup>
// TR-MARKETING-INQUIRY-LOOKUP-UI-56 客人自助回查 H5（公开免登录）。
// ⚠️ MOCKED_CONTRACT：后端 TL55 未 reviewed；相关接口/浏览器证据均为合同 mock，
// 真实闭环待 TL55 reviewed 后另做联调。
// 隐私纪律：手机号只存在于输入框与 POST 请求体；不写 URL/query/存储/控制台；
// 查无、手机号不符、非法输入共用同一不泄露结果；系统故障使用独立错误态并保留输入。
import { computed, onBeforeUnmount, ref } from 'vue'
import { useRoute } from 'vue-router'
import { isValidLookupPhone, lookupBookingInquiry, normalizeInquiryLookup } from '@/api/marketing'

const route = useRoute()
const inquiryNo = computed(() => String(route.params.inquiryNo ?? '').trim())

const phone = ref('')
const loading = ref(false)
const view = ref('idle') // idle | loading | result | empty | error
const result = ref(null)
const copied = ref(false)
let copyTimer = null

async function onSubmit() {
  if (loading.value) return // 重复点击期间只发一次请求
  // 非法输入不发请求，与查无/手机号不符共用同一不泄露结果
  if (!inquiryNo.value || !isValidLookupPhone(phone.value)) {
    result.value = null
    view.value = 'empty'
    return
  }
  loading.value = true
  view.value = 'loading'
  try {
    const res = await lookupBookingInquiry(inquiryNo.value, phone.value)
    const normalized = normalizeInquiryLookup(res?.data ?? null)
    if (normalized) {
      result.value = normalized
      view.value = 'result'
    } else {
      result.value = null
      view.value = 'empty'
    }
  } catch {
    // 网络/系统故障：独立错误态；不区分 HTTP 5xx 与断网，输入保留
    result.value = null
    view.value = 'error'
  } finally {
    loading.value = false
  }
}

async function copyInquiryNo() {
  const text = inquiryNo.value
  if (!text) return
  let ok = false
  try {
    await navigator.clipboard.writeText(text)
    ok = true
  } catch {
    try {
      const ta = document.createElement('textarea')
      ta.value = text
      ta.setAttribute('readonly', '')
      ta.style.position = 'fixed'
      ta.style.opacity = '0'
      document.body.appendChild(ta)
      ta.select()
      ok = document.execCommand('copy')
      ta.remove()
    } catch {
      ok = false
    }
  }
  copied.value = ok
  if (ok) {
    if (copyTimer) clearTimeout(copyTimer)
    copyTimer = setTimeout(() => { copied.value = false }, 1600)
  }
}

onBeforeUnmount(() => {
  if (copyTimer) clearTimeout(copyTimer)
})
</script>

<style scoped>
/*
  客人回查 H5：沿用营销 H5 视觉——柔白米底、深绿为体、金色只出现在主按钮与强调点。
  390px 视口零横向滚动：卡片 max-width + box-sizing，无固定宽元素，overflow-x 兜底。
*/
.lk-h5 {
  min-height: 100vh;
  background: #f5f2ea;
  padding: 14px 12px 28px;
  box-sizing: border-box;
  overflow-x: hidden;
  color: #24382f;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.lk-card {
  max-width: 480px;
  margin: 0 auto 12px;
  background: #fffdf8;
  border: 1px solid #e6dfd0;
  border-radius: 12px;
  padding: 16px;
  box-sizing: border-box;
}

.lk-eyebrow {
  margin: 0 0 6px;
  font-size: 12px;
  letter-spacing: 0.14em;
  color: #5c7268;
}

.lk-title {
  margin: 0 0 12px;
  font-size: 23px;
  line-height: 1.35;
  font-weight: 700;
  color: #2d4a3e;
}

.lk-no-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #f7f4ec;
  border: 1px solid #e6dfd0;
  border-radius: 8px;
  box-sizing: border-box;
}

.lk-no-label { font-size: 12px; color: #5c7268; flex: 0 0 auto; }

.lk-no {
  flex: 1 1 auto;
  min-width: 0;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: #2d4a3e;
  word-break: break-all;
}

.lk-copy {
  flex: 0 0 auto;
  padding: 6px 12px;
  border: 1px solid #c4a35a;
  border-radius: 8px;
  background: #fffdf8;
  color: #9a7b2e;
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
}
.lk-copy:active { transform: scale(0.97); }

.lk-hint { margin: 10px 0 0; font-size: 13px; line-height: 1.7; color: #5c7268; }

.lk-form { display: flex; flex-direction: column; gap: 12px; }
.lk-field { display: flex; flex-direction: column; gap: 5px; }
.lk-label { font-size: 13px; color: #5c7268; }

.lk-input {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 12px;
  border: 1px solid #d8d0bf;
  border-radius: 8px;
  background: #fffefa;
  font-size: 16px;
  color: #24382f;
  letter-spacing: 0.04em;
}
.lk-input:focus { outline: 2px solid rgba(45, 74, 62, 0.35); border-color: #2d4a3e; }

.lk-cta {
  display: block;
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: 8px;
  background: #c4a35a;
  color: #fffdf8;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.12em;
  white-space: nowrap;
  cursor: pointer;
  transition: transform 0.12s ease, background 0.12s ease;
}
.lk-cta:active { transform: scale(0.985); background: #b39247; }
.lk-cta:disabled { opacity: 0.6; cursor: default; }

.lk-h2 {
  margin: 0 0 10px;
  font-size: 16px;
  font-weight: 700;
  color: #2d4a3e;
  padding-left: 9px;
  border-left: 3px solid #c4a35a;
}

.lk-rows { margin: 0; }
.lk-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding: 9px 0;
  border-bottom: 1px dashed #e6dfd0;
}
.lk-row:last-child { border-bottom: none; }
.lk-row dt { flex: 0 0 auto; width: 6.5em; font-size: 13px; color: #5c7268; }
.lk-row dd {
  flex: 1 1 auto;
  min-width: 0;
  margin: 0;
  font-size: 15px;
  color: #24382f;
  word-break: break-all;
  text-align: right;
}

.lk-status { font-weight: 700; }
.lk-status--pending { color: #9a7b2e; }
.lk-status--converted { color: #2d4a3e; }
.lk-status--rejected { color: #b04a3a; }
.lk-status--unknown { color: #5c7268; }

.lk-footnote { margin: 10px 0 0; font-size: 12px; color: #8a8372; }

/* 状态页（空结果/系统繁忙） */
.lk-state-card { text-align: center; padding: 40px 22px; }
.lk-state-mark {
  width: 46px;
  height: 46px;
  margin: 0 auto 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1.5px solid #b9c7bf;
  border-radius: 50%;
  font-size: 20px;
  color: #5c7268;
}
.lk-state-title { margin: 0 0 10px; font-size: 19px; color: #2d4a3e; }
.lk-state-text { margin: 0 0 18px; font-size: 14px; line-height: 1.7; color: #5c7268; }

.lk-retry {
  padding: 9px 22px;
  border: none;
  border-radius: 8px;
  background: #2d4a3e;
  color: #fffdf8;
  font-size: 14px;
  cursor: pointer;
  white-space: nowrap;
}
.lk-retry:active { transform: scale(0.98); }

/* 骨架 */
.sk { background: #ece6d8; border-radius: 6px; margin-bottom: 12px; overflow: hidden; }
.sk-line { width: 100%; height: 12px; }
.lk-loading-text { margin: 4px 0 0; font-size: 12px; color: #8a8372; text-align: center; }

.lk-footer { margin: 18px 0 0; font-size: 12px; color: #8a8372; text-align: center; }

@media (prefers-reduced-motion: reduce) {
  .lk-cta, .lk-retry, .lk-copy { transition: none; }
}
</style>
