<template>
  <div class="ipad-page">
    <div class="page-top">
      <button class="back-link" @click="router.back()">← 返回账单</button>
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
          :class="['pay-method', { active: selected === m.type }]"
          @click="selectMethod(m)">
          <span class="method-icon">{{ m.icon }}</span>
          <span class="method-name">{{ m.name }}</span>
          <span class="method-desc">{{ m.desc }}</span>
        </button>
      </div>

      <!-- 现金支付：输入收款并计算找零 -->
      <div v-if="selected === 'cash'" class="cash-section">
        <div class="cash-input-row">
          <label>实收现金 · Received</label>
          <div class="cash-input-group">
            <span class="cash-prefix">¥</span>
            <input v-model.number="cashReceived" type="number" placeholder="输入实收金额" class="cash-input" @input="calcChange" />
          </div>
        </div>
        <div class="cash-shortcuts">
          <button v-for="a in cashShortcuts" :key="a" @click="cashReceived = a; calcChange()">¥{{ a }}</button>
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
          <input v-model="creditAccount" placeholder="输入挂账单位名称" class="credit-input" />
        </div>
        <div class="form-row">
          <label>挂账金额</label>
          <span class="credit-amount">¥{{ payAmount.toFixed(2) }}</span>
        </div>
        <div class="credit-note">挂账需经理审核</div>
      </div>

      <!-- 混合支付 -->
      <div class="split-pay-toggle" @click="splitPay = !splitPay">
        <span :class="['split-check', { on: splitPay }]">✓</span>
        <span>混合支付 · Split Payment</span>
      </div>

      <div v-if="splitPay" class="split-section">
        <div class="split-methods">
          <div v-for="(s, i) in splitMethods" :key="i" class="split-row">
            <select v-model="s.type" class="split-select">
              <option v-for="m in methods" :key="m.type" :value="m.type">{{ m.name }}</option>
            </select>
            <input v-model.number="s.amount" type="number" placeholder="金额" class="split-amount" />
            <button v-if="splitMethods.length > 1" class="split-remove" @click="splitMethods.splice(i, 1)">×</button>
          </div>
        </div>
        <button class="add-split" @click="splitMethods.push({ type: 'wechat', amount: 0 })">+ 添加支付方式</button>
        <div class="split-total">
          合计：¥{{ splitTotal }} / ¥{{ payAmount.toFixed(2) }}
          <span v-if="splitTotal !== payAmount" :class="splitTotal > payAmount ? 'over' : 'under'">
            {{ splitTotal > payAmount ? '超出' : '差' }}¥{{ Math.abs(splitTotal - payAmount).toFixed(2) }}
          </span>
        </div>
      </div>

      <!-- 支付确认按钮 -->
      <button class="btn-confirm" @click="confirmPay" :disabled="paying || !canPay">
        {{ paying ? '支付中...' : `确认支付 · Confirm Pay ¥${payAmount.toFixed(0)}` }}
      </button>
    </div>

    <!-- 找零弹窗 -->
    <Transition name="modal">
      <div v-if="showChangeModal" class="modal-overlay" @click.self="completePay">
        <div class="change-modal">
          <div class="change-icon">💵</div>
          <div class="change-title">找零 · Change</div>
          <div class="change-price">¥{{ changeAmount.toFixed(2) }}</div>
          <button class="change-done" @click="completePay">完成 · Done</button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useIpadStore } from '@/store/ipad'
import { ipadSettlementPay, ipadBillDetail } from '@/api/ipad'
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
  if (splitPay.value) return Math.abs(splitTotal.value - payAmount.value) < 0.01
  if (selected.value === 'cash') return cashReceived.value >= payAmount.value
  if (selected.value === 'credit') return creditAccount.value.length > 0
  return true
})

const splitTotal = computed(() =>
  splitMethods.value.reduce((s, m) => s + Number(m.amount || 0), 0)
)

function selectMethod(m) {
  selected.value = m.type
  splitPay.value = false
  cashReceived.value = 0
  changeAmount.value = 0
}

function calcChange() {
  changeAmount.value = Math.max(0, (cashReceived.value || 0) - payAmount.value)
}

async function confirmPay() {
  if (splitPay.value && Math.abs(splitTotal.value - payAmount.value) > 0.01) {
    ElMessage.warning('混合支付合计需等于应付金额')
    return
  }

  paying.value = true
  try {
    let payData
    if (splitPay.value) {
      payData = {
        booking_id: route.params.bookingId,
        pay_type: 'split',
        pay_details: splitMethods.value,
      }
    } else {
      payData = {
        booking_id: route.params.bookingId,
        pay_type: selected.value,
        pay_amount: selected.value === 'cash' ? cashReceived.value : payAmount.value,
        credit_account: selected.value === 'credit' ? creditAccount.value : undefined,
      }
    }

    const res = await ipadSettlementPay(payData)
    if (res.code === 200) {
      if (selected.value === 'cash' && changeAmount.value > 0) {
        showChangeModal.value = true
        return
      }
      completePay()
    } else {
      ElMessage.error(res.msg || '支付失败')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '支付失败，请检查网络后重试')
  } finally {
    paying.value = false
  }
}

function completePay() {
  showChangeModal.value = false
  ElMessage.success('支付成功')
  ipad.clearCart()
  router.push('/ipad/home')
}

onMounted(async () => {
  try {
    // Read discount from session
    const discountStr = sessionStorage.getItem('ipad_discount')
    if (discountStr) {
      const disc = JSON.parse(discountStr)
      payAmount.value = disc.final_amount || ipad.cartTotal
    } else {
      const res = await ipadBillDetail(route.params.bookingId)
      if (res.code === 200) payAmount.value = res.data.final_amount || res.data.total_amount || 0
    }
  } catch (error) {
    payAmount.value = 0
    ElMessage.error(error.response?.data?.message || '账单加载失败，暂不能收款')
  }
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
.split-pay-toggle { display: flex; align-items: center; gap: 10px; padding: 10px 0; cursor: pointer; margin-bottom: 8px; }
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
