<template>
  <div class="ipad-page">
    <div class="page-top">
      <button class="back-link" :disabled="paymentLocked" @click="router.back()">← 返回账单</button>
      <h1 class="page-title">支付 · Payment</h1>
    </div>

    <div class="pay-content">
      <!-- 金额展示 -->
      <div class="pay-amount">
        <span class="pay-label">应付金额 · Total</span>
        <span class="pay-price">¥{{ payAmount.toFixed(2) }}</span>
      </div>

      <!-- 已收定金 -->
      <div v-if="deposit" class="deposit-info">
        <span>已收定金 {{ deposit.deposit_type === 'cash' ? '现金' : '转账' }}</span>
        <span class="deposit-amount">¥{{ deposit.deposit_amount }}</span>
        <span class="deposit-left">还需：¥{{ Math.max(0, payAmount - deposit.deposit_amount).toFixed(2) }}</span>
      </div>

      <!-- 支付方式 -->
      <div class="pay-methods">
        <button v-for="m in methods" :key="m.type"
          :disabled="paymentLocked"
          :class="['pay-method', { active: selected === m.type }]"
          @click="selectMethod(m)">
          <span class="method-icon">{{ m.icon }}</span>
          <span class="method-name">{{ m.name }}</span>
          <span class="method-desc">{{ m.desc }}</span>
        </button>
      </div>

      <!-- 收款账户：本店启用账户白名单，必须选择后才能收款 -->
      <div class="account-section">
        <div class="account-head">
          <span class="account-title">收款账户 · Account</span>
          <button v-if="accountsState === 'error'" class="account-retry" type="button" :disabled="paymentLocked" @click="loadAccounts">重新加载</button>
        </div>

        <div v-if="accountsState === 'loading'" class="account-state">正在加载本店收款账户…</div>

        <div v-else-if="accountsState === 'error'" class="account-state account-error">
          收款账户加载失败，暂不能收款{{ accountError ? `：${accountError}` : '' }}。请检查网络后点“重新加载”。
        </div>

        <div v-else-if="accountsState === 'empty'" class="account-state account-error">
          本店暂无可用收款账户。请先在后台为本店添加并启用收款账户后再收款。
        </div>

        <div v-else class="account-list">
          <button v-for="a in accounts" :key="a.account_id" type="button"
            :disabled="paymentLocked"
            :class="['account-item', { active: selectedAccountId === a.account_id }]"
            @click="selectedAccountId = a.account_id">
            <span :class="['account-radio', { on: selectedAccountId === a.account_id }]">●</span>
            <span class="account-name">{{ a.account_name }}</span>
            <span class="account-type">{{ accountTypeLabel(a.account_type) }}</span>
          </button>
        </div>
      </div>

      <!-- 现金支付：输入收款并计算找零 -->
      <div v-if="selected === 'cash'" class="cash-section">
        <div class="cash-input-row">
          <label>实收现金 · Received</label>
          <div class="cash-input-group">
            <span class="cash-prefix">¥</span>
            <input v-model.number="cashReceived" :disabled="paymentLocked" type="number" placeholder="输入实收金额" class="cash-input" @input="calcChange" />
          </div>
        </div>
        <div class="cash-shortcuts">
          <button v-for="a in cashShortcuts" :key="a" :disabled="paymentLocked" @click="cashReceived = a; calcChange()">¥{{ a }}</button>
        </div>
        <div v-if="changeAmount > 0" class="change-row">
          <span>找零 · Change</span>
          <span class="change-amount">¥{{ changeAmount.toFixed(2) }}</span>
        </div>
      </div>

      <!-- 挂账信息 -->
      <div v-if="selected === 'credit'" class="credit-section">
        <div class="form-row">
          <label>挂账单位/客户</label>
          <input v-model="creditAccount" :disabled="paymentLocked" placeholder="输入挂账单位名称" class="credit-input" />
        </div>
        <div class="form-row">
          <label>挂账金额</label>
          <span class="credit-amount">¥{{ payAmount.toFixed(2) }}</span>
        </div>
        <div class="credit-note">挂账需经理审核</div>
      </div>

      <!-- 混合支付 -->
      <button type="button" class="split-pay-toggle" :disabled="paymentLocked" @click="splitPay = !splitPay">
        <span :class="['split-check', { on: splitPay }]">✓</span>
        <span>混合支付 · Split Payment</span>
      </button>

      <div v-if="splitPay" class="split-section">
        <div class="split-methods">
          <div v-for="(s, i) in splitMethods" :key="i" class="split-row">
            <select v-model="s.type" :disabled="paymentLocked" class="split-select">
              <option v-for="m in methods" :key="m.type" :value="m.type">{{ m.name }}</option>
            </select>
            <input v-model.number="s.amount" :disabled="paymentLocked" type="number" placeholder="金额" class="split-amount" />
            <button v-if="splitMethods.length > 1" class="split-remove" :disabled="paymentLocked" @click="splitMethods.splice(i, 1)">×</button>
          </div>
        </div>
        <button class="add-split" :disabled="paymentLocked" @click="splitMethods.push({ type: 'wechat', amount: 0 })">+ 添加支付方式</button>
        <div class="split-total">
          合计：¥{{ splitTotal }} / ¥{{ payAmount.toFixed(2) }}
          <span v-if="splitTotal !== payAmount" :class="splitTotal > payAmount ? 'over' : 'under'">
            {{ splitTotal > payAmount ? '超出' : '差' }}¥{{ Math.abs(splitTotal - payAmount).toFixed(2) }}
          </span>
        </div>
      </div>

      <!-- 支付确认按钮 -->
      <div v-if="awaitingConfirmation" class="payment-pending" role="status">本笔支付结果待确认，请重试确认；将使用首次提交的账户和金额。</div>
      <button class="btn-confirm" @click="confirmPay" :disabled="paying || !canPay">
        {{ paying ? '支付中...' : awaitingConfirmation ? `重试确认原支付 ¥${pendingPayment.amount.toFixed(2)}` : `确认支付 · Confirm Pay ¥${payAmount.toFixed(0)}` }}
      </button>
    </div>

    <!-- 找零弹窗 -->
    <Transition name="modal">
      <div v-if="showChangeModal" class="modal-overlay" @click.self="completePay">
        <div class="change-modal">
          <div class="change-icon">💵</div>
          <div class="change-title">找零 · Change</div>
          <div class="change-price">¥{{ confirmedPayment.change.toFixed(2) }}</div>
          <button class="change-done" @click="completePay">完成 · Done</button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useIpadStore } from '@/store/ipad'
import { ipadSettlementPay, ipadSettlementAccounts, ipadBillDetail } from '@/api/ipad'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const ipad = useIpadStore()

const payAmount = ref(0)
const deposit = ref(null)
const selected = ref('wechat')
const paying = ref(false)
const cashReceived = ref(0)
const changeAmount = ref(0)
const splitPay = ref(false)
const splitMethods = ref([{ type: 'wechat', amount: 0 }])
const showChangeModal = ref(false)
const creditAccount = ref('')
const paymentKey = ref(crypto.randomUUID())
const confirmedPayment = ref(null)
const pendingPayment = ref(null)
const awaitingConfirmation = computed(() => !!pendingPayment.value && !paying.value && !showChangeModal.value)
const paymentLocked = computed(() => paying.value || showChangeModal.value || !!pendingPayment.value)
let alive = true
let contextGeneration = 0
let activePayment = null
// 同一组件内切回原门店/预订时，未知结果仍只能使用原 key 与原载荷重试。
const pendingByContext = new Map()
function paymentContextKey() {
  return JSON.stringify([ipad.storeId, route.params.bookingId])
}

function isCurrentPayment(request) {
  return alive && activePayment === request && request.generation === contextGeneration
    && request.storeId === ipad.storeId && request.bookingId === route.params.bookingId
}

function invalidatePaymentContext() {
  contextGeneration += 1
  activePayment = null
  paying.value = false
  showChangeModal.value = false
  confirmedPayment.value = null
  pendingPayment.value = pendingByContext.get(paymentContextKey()) || null
  paymentKey.value = pendingPayment.value?.key || crypto.randomUUID()
}

function freezePayment(value) {
  if (value && typeof value === 'object') {
    Object.values(value).forEach(freezePayment)
    Object.freeze(value)
  }
  return value
}

// 收款账户：进入页面按设备认证门店现拉，只保留后端白名单三字段。
// 每次加载先清空——切店或重新进入绝不能复用上一家门店的账户选择。
const accounts = ref([])
const selectedAccountId = ref(null)
const accountsState = ref('loading') // loading | ready | empty | error
const accountError = ref('')
let accountsRequest = 0

const ACCOUNT_TYPE_LABELS = {
  cash: '现金', wechat: '微信', alipay: '支付宝', card: '银行卡', credit: '挂账',
}
function accountTypeLabel(type) {
  return ACCOUNT_TYPE_LABELS[type] || type || '其他'
}

async function loadAccounts() {
  const request = ++accountsRequest
  const requestStore = ipad.storeId
  const isCurrent = () => request === accountsRequest && requestStore === ipad.storeId
  // 先复位：列表与已选账户一律清空，避免切店/重入时短暂渲染旧店账户。
  accounts.value = []
  selectedAccountId.value = null
  accountsState.value = 'loading'
  accountError.value = ''
  try {
    const res = await ipadSettlementAccounts()
    if (!isCurrent()) return
    if (res.code !== 200 || !Array.isArray(res.data)) throw new Error('账户列表返回格式不正确')
    // 白名单收口：只取三字段，后端多给的字段一律不进渲染数据。
    const rows = res.data
      .filter(a => a && a.account_id !== null && a.account_id !== undefined && a.account_id !== '')
      .map(a => ({
        account_id: a.account_id,
        account_name: String(a.account_name ?? ''),
        account_type: String(a.account_type ?? ''),
      }))
    accounts.value = rows
    accountsState.value = rows.length ? 'ready' : 'empty'
  } catch (error) {
    if (!isCurrent()) return
    accounts.value = []
    selectedAccountId.value = null
    accountsState.value = 'error'
    accountError.value = error.response?.data?.message || error.message || '网络异常'
  }
}

const methods = [
  { type: 'wechat', name: '微信', icon: '💚', desc: 'WeChat Pay' },
  { type: 'alipay', name: '支付宝', icon: '🔵', desc: 'Alipay' },
  { type: 'cash', name: '现金', icon: '💵', desc: 'Cash' },
  { type: 'card', name: '银行卡', icon: '💳', desc: 'Bank Card' },
  { type: 'credit', name: '挂账', icon: '📋', desc: 'Corporate Credit' },
]

const cashShortcuts = computed(() => {
  const base = Math.ceil(payAmount.value / 100) * 100
  return [base, base + 100, base + 200, base + 300].filter(v => v >= payAmount.value)
})

const canPay = computed(() => {
  if (!alive || showChangeModal.value) return false
  if (pendingPayment.value) return true
  // 账户是硬前提：空列表/加载失败/未选账户一律不能发起支付。
  if (accountsState.value !== 'ready' || selectedAccountId.value == null) return false
  if (splitPay.value) return Math.abs(splitTotal.value - payAmount.value) < 0.01
  if (selected.value === 'cash') return cashReceived.value >= payAmount.value
  if (selected.value === 'credit') return creditAccount.value.length > 0
  return true
})

const splitTotal = computed(() =>
  splitMethods.value.reduce((s, m) => s + Number(m.amount || 0), 0)
)

function selectMethod(m) {
  if (paymentLocked.value || !alive) return
  selected.value = m.type
  splitPay.value = false
  cashReceived.value = 0
  changeAmount.value = 0
}

function calcChange() {
  changeAmount.value = Math.max(0, (cashReceived.value || 0) - payAmount.value)
}

async function confirmPay() {
  // 重复点击：按钮已 disabled，这里再加一道函数级重入守卫，杜绝并发双发。
  if (!alive || paying.value || showChangeModal.value) return
  if (!pendingPayment.value) {
    if (accountsState.value !== 'ready' || selectedAccountId.value == null) {
      ElMessage.warning('请先选择收款账户')
      return
    }
    if (splitPay.value && Math.abs(splitTotal.value - payAmount.value) > 0.01) {
      ElMessage.warning('混合支付合计需等于应付金额')
      return
    }
    // account_id 对现金/微信/支付宝/银行卡/挂账/混合都必带；同一幂等键供重复点击与失败重试复用。
    let payData
    if (splitPay.value) {
      payData = {
        booking_id: route.params.bookingId,
        pay_type: 'split',
        account_id: selectedAccountId.value,
        pay_details: splitMethods.value,
      }
    } else {
      payData = {
        booking_id: route.params.bookingId,
        pay_type: selected.value,
        account_id: selectedAccountId.value,
        pay_amount: selected.value === 'cash' ? cashReceived.value : payAmount.value,
        credit_account: selected.value === 'credit' ? creditAccount.value : undefined,
      }
    }

    // 与响应式输入彻底分离，金额、支付方式及混合支付明细均以提交时为准。
    pendingPayment.value = freezePayment(JSON.parse(JSON.stringify({
      key: paymentKey.value,
      payData,
      method: splitPay.value ? 'split' : selected.value,
      amount: payAmount.value,
      received: cashReceived.value,
      change: Math.max(0, (cashReceived.value || 0) - payAmount.value),
    })))
    pendingByContext.set(paymentContextKey(), pendingPayment.value)
  }
  // 未知结果只能原样重试首份载荷，不能为改金额重新发 key。
  const snapshot = pendingPayment.value
  const request = {
    generation: contextGeneration, storeId: ipad.storeId,
    bookingId: route.params.bookingId, contextKey: paymentContextKey(),
  }
  activePayment = request
  paying.value = true
  try {
    const res = await ipadSettlementPay(snapshot.payData, snapshot.key)
    if (!isCurrentPayment(request)) return
    if (res.code === 200) {
      pendingByContext.delete(request.contextKey)
      confirmedPayment.value = snapshot
      if (snapshot.method === 'cash' && snapshot.change > 0) {
        showChangeModal.value = true
        return
      }
      completePay()
    } else {
      // 仅明确的账户无效业务拒绝已知零写入，可释放快照后重选。
      // 其余服务失败仍可能已记账，保留首份快照与 key。
      const bizMsg = res.message || res.msg || '支付失败'
      ElMessage.error(bizMsg)
      if (/收款账户.*(?:不存在|不属于当前门店|已停用)/.test(bizMsg)) {
        pendingByContext.delete(request.contextKey)
        pendingPayment.value = null
        loadAccounts()
      }
    }
  } catch (error) {
    if (!isCurrentPayment(request)) return
    // 网络错误不是零写入证明，保留首份快照与 key，只允许原样重试。
    const msg = error.response?.data?.message || '支付失败，请检查网络后重试'
    ElMessage.error(msg)
  } finally {
    if (isCurrentPayment(request)) {
      paying.value = false
      if (!confirmedPayment.value) activePayment = null
    }
  }
}

function completePay() {
  if (!confirmedPayment.value || !isCurrentPayment(activePayment)) return
  activePayment = null
  confirmedPayment.value = null
  pendingPayment.value = null
  paying.value = false
  showChangeModal.value = false
  ElMessage.success('支付成功')
  // 成功后才清理：换新幂等键、清空账户选择，避免下一单复用。
  paymentKey.value = crypto.randomUUID()
  selectedAccountId.value = null
  ipad.clearCart()
  router.push('/ipad/home')
}

onMounted(() => {
  loadAccounts()
  // 账单金额沿用原逻辑（会话折扣或服务端快照），与账户加载互不阻塞。
  try {
    // Read discount from session
    const discountStr = sessionStorage.getItem('ipad_discount')
    if (discountStr) {
      const disc = JSON.parse(discountStr)
      payAmount.value = disc.final_amount || ipad.cartTotal
    } else {
      ipadBillDetail(route.params.bookingId).then(res => {
        if (res.code === 200) payAmount.value = res.data.final_amount || res.data.total_amount || 0
      }).catch(error => {
        payAmount.value = 0
        ElMessage.error(error.response?.data?.message || '账单加载失败，暂不能收款')
      })
    }
  } catch (error) {
    payAmount.value = 0
    ElMessage.error(error.response?.data?.message || '账单加载失败，暂不能收款')
  }
})

// 设备切店：重新拉当前门店账户，旧店列表与选择不复用。
watch(() => [ipad.storeId, route.params.bookingId], () => {
  invalidatePaymentContext()
  loadAccounts()
}, { flush: 'sync' })
onBeforeUnmount(() => {
  alive = false
  invalidatePaymentContext()
  accountsRequest += 1
})
</script>

<style scoped>
.ipad-page { width: 100%; height: 100%; display: flex; flex-direction: column; background: var(--color-bg); }
.page-top { padding: 16px 24px; background: var(--color-card); border-bottom: 1px solid var(--color-border); display: flex; align-items: center; gap: 16px; flex-shrink: 0; }
.back-link { border: none; background: none; color: var(--color-text-muted); font-size: 13px; cursor: pointer; }
.page-title { font-size: 18px; font-weight: 700; color: var(--color-text); letter-spacing: 2px; }
.pay-content { flex: 1; overflow-y: auto; padding: 24px; max-width: 520px; margin: 0 auto; width: 100%; }

.pay-amount { text-align: center; margin-bottom: 20px; }
.pay-label { display: block; font-size: 14px; color: var(--color-text-muted); margin-bottom: 6px; }
.pay-price { font-size: 44px; font-weight: 700; color: var(--color-accent-dark); }

.deposit-info { display: flex; align-items: center; gap: 10px; padding: 10px 14px; background: rgba(74,124,89,0.05); border-radius: var(--radius-md); margin-bottom: 16px; font-size: 13px; color: var(--color-text-secondary); }
.deposit-amount { font-weight: 600; color: var(--color-primary); }
.deposit-left { color: var(--color-accent-dark); font-weight: 600; margin-left: auto; }

.pay-methods { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 20px; }
.pay-method {
  display: flex; flex-direction: column; align-items: center; gap: 4px;
  padding: 14px 12px; border: 2px solid var(--color-border);
  border-radius: var(--radius-lg); background: var(--color-card);
  cursor: pointer; transition: all 0.2s;
}
.pay-method.active { border-color: var(--color-primary); background: rgba(45,74,62,0.04); }
.pay-method:hover { border-color: var(--color-primary); }
.method-icon { font-size: 24px; }
.method-name { font-size: 14px; font-weight: 600; color: var(--color-text); }
.method-desc { font-size: 11px; color: var(--color-text-muted); }

/* 收款账户 */
.account-section { border: 1px solid var(--color-border); border-radius: var(--radius-lg); background: var(--color-card); padding: 12px 14px; margin-bottom: 20px; }
.account-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.account-title { font-size: 14px; font-weight: 600; color: var(--color-text); }
.account-retry { border: 1px solid var(--color-primary); color: var(--color-primary); background: none; border-radius: var(--radius-sm); padding: 4px 12px; font-size: 12px; cursor: pointer; }
.account-state { font-size: 13px; color: var(--color-text-muted); padding: 8px 2px; }
.account-state.account-error { color: var(--color-warning, #b8860b); line-height: 1.6; }
.account-list { display: flex; flex-direction: column; gap: 8px; max-height: 220px; overflow-y: auto; }
.account-item { display: flex; align-items: center; gap: 10px; width: 100%; text-align: left; padding: 10px 12px; border: 1.5px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-bg-alt); cursor: pointer; transition: border-color 0.15s; }
.account-item.active { border-color: var(--color-primary); background: rgba(45,74,62,0.05); }
.account-radio { font-size: 10px; color: transparent; border: 2px solid var(--color-border); border-radius: 50%; width: 18px; height: 18px; display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }
.account-radio.on { color: #fff; border-color: var(--color-primary); background: var(--color-primary); }
.account-name { flex: 1; font-size: 14px; font-weight: 600; color: var(--color-text); min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.account-type { font-size: 11px; color: var(--color-text-muted); border: 1px solid var(--color-border); border-radius: var(--radius-sm); padding: 2px 8px; flex-shrink: 0; }
@media (max-width: 480px) {
  .pay-content { padding: 16px; }
  .account-list { max-height: 180px; }
}

/* 现金 */
.cash-section { background: var(--color-bg-alt); border-radius: var(--radius-lg); padding: 16px; margin-bottom: 16px; }
.cash-input-row { display: flex; align-items: center; justify-content: space-between; }
.cash-input-row label { font-size: 14px; color: var(--color-text-secondary); }
.cash-input-group { display: flex; align-items: center; gap: 4px; }
.cash-prefix { font-size: 20px; font-weight: 700; color: var(--color-text); }
.cash-input { width: 120px; padding: 8px 12px; border: 1px solid var(--color-border); border-radius: var(--radius-md); font-size: 18px; font-weight: 600; text-align: right; outline: none; }
.cash-input:focus { border-color: var(--color-primary); }
.cash-shortcuts { display: flex; gap: 8px; margin-top: 10px; }
.cash-shortcuts button { padding: 6px 14px; border: 1px solid var(--color-border); border-radius: var(--radius-sm); background: var(--color-card); font-size: 13px; cursor: pointer; transition: all 0.15s; }
.cash-shortcuts button:hover { border-color: var(--color-primary); background: rgba(45,74,62,0.04); }
.change-row { display: flex; justify-content: space-between; align-items: center; margin-top: 12px; padding-top: 10px; border-top: 1px solid var(--color-border); font-size: 15px; color: var(--color-text-secondary); }
.change-amount { font-size: 28px; font-weight: 700; color: var(--color-success); }

/* 挂账 */
.credit-section { background: var(--color-bg-alt); border-radius: var(--radius-lg); padding: 16px; margin-bottom: 16px; }
.form-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.form-row label { font-size: 14px; color: var(--color-text-secondary); }
.credit-input { width: 180px; padding: 8px 12px; border: 1px solid var(--color-border); border-radius: var(--radius-md); font-size: 14px; outline: none; }
.credit-input:focus { border-color: var(--color-primary); }
.credit-amount { font-size: 20px; font-weight: 700; color: var(--color-accent-dark); }
.credit-note { font-size: 12px; color: var(--color-warning); margin-top: 8px; }

/* 混合支付 */
.split-pay-toggle { display: flex; align-items: center; gap: 10px; padding: 10px 0; cursor: pointer; margin-bottom: 8px; border: none; background: none; color: inherit; font: inherit; }
.payment-pending { margin: 12px 0; color: var(--color-warning, #b8860b); font-size: 13px; line-height: 1.6; }
.split-check { width: 22px; height: 22px; border-radius: 6px; border: 2px solid var(--color-border); display: flex; align-items: center; justify-content: center; font-size: 12px; color: transparent; transition: all 0.2s; }
.split-check.on { border-color: var(--color-primary); background: var(--color-primary); color: white; }
.split-section { background: var(--color-bg-alt); border-radius: var(--radius-lg); padding: 14px; margin-bottom: 16px; }
.split-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.split-select { padding: 8px 12px; border: 1px solid var(--color-border); border-radius: var(--radius-md); font-size: 14px; flex: 1; background: var(--color-card); }
.split-amount { width: 100px; padding: 8px 12px; border: 1px solid var(--color-border); border-radius: var(--radius-md); font-size: 14px; text-align: right; }
.split-remove { border: none; background: none; font-size: 18px; color: var(--color-text-muted); cursor: pointer; }
.add-split { display: block; border: none; background: none; color: var(--color-primary); font-size: 13px; cursor: pointer; padding: 4px 0; }
.split-total { font-size: 13px; color: var(--color-text-secondary); margin-top: 6px; }
.split-total .over { color: var(--color-danger); }
.split-total .under { color: var(--color-warning); }

.btn-confirm {
  width: 100%; padding: 18px; border: none; border-radius: var(--radius-md);
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light));
  color: white; font-size: 18px; font-weight: 700; cursor: pointer; letter-spacing: 1px;
  margin-top: 8px;
}
.btn-confirm:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-confirm:not(:disabled):hover { transform: translateY(-1px); box-shadow: 0 4px 14px rgba(45,74,62,0.3); }

/* 找零弹窗 */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 300; }
.change-modal {
  background: var(--color-card); border-radius: var(--radius-xl);
  width: 320px; padding: 32px; text-align: center; box-shadow: var(--shadow-xl);
}
.change-icon { font-size: 48px; margin-bottom: 12px; }
.change-title { font-size: 16px; font-weight: 600; color: var(--color-text-secondary); margin-bottom: 8px; }
.change-price { font-size: 40px; font-weight: 700; color: var(--color-success); margin-bottom: 24px; }
.change-done { width: 100%; padding: 14px; border: none; border-radius: var(--radius-md); background: var(--color-primary); color: white; font-size: 16px; font-weight: 700; cursor: pointer; }
.change-done:hover { background: var(--color-primary-dark); }

.modal-enter-active, .modal-leave-active { transition: opacity 0.2s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
</style>
