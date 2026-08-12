<template>
  <div class="ipad-page">
    <div class="page-top">
      <button class="back-link" @click="router.back()">← 返回点餐</button>
      <h1 class="page-title">账单 · Bill</h1>
      <span class="page-status" v-if="bill">{{ bill.booking_id }}</span>
    </div>

    <!-- 有账单数据 -->
    <div class="bill-content" v-if="bill">
      <!-- 客户信息区 -->
      <div class="section customer-section">
        <div class="section-title">
          <span>客户信息 · Customer</span>
          <button v-if="!bill.customer_id" class="link-btn" @click="showBindMember = true">绑定会员</button>
        </div>
        <div class="customer-row">
          <div class="cust-info">
            <span class="cust-name">{{ bill.customer_name || '散客' }}</span>
            <span class="cust-phone" v-if="bill.customer_phone">{{ bill.customer_phone }}</span>
          </div>
          <span class="cust-people">{{ bill.guest_count }}人</span>
        </div>
      </div>

      <!-- 菜品区 -->
      <div class="section dish-section">
        <div class="section-title">已点菜品 · Dishes</div>
        <div class="dish-list">
          <div v-for="(d, i) in bill.dish_list" :key="i" class="dish-row">
            <span class="dish-index">{{ i + 1 }}</span>
            <span class="dish-name">{{ d.dish_name }}</span>
            <span class="dish-spec" v-if="d.dish_spec || d.taste">{{ d.dish_spec || d.taste }}</span>
            <span class="dish-qty">×{{ d.dish_quantity }}</span>
            <span class="dish-price">¥{{ Number(d.subtotal || d.sale_price * d.dish_quantity).toFixed(0) }}</span>
          </div>
        </div>
        <button class="link-btn back-dishes" @click="router.push(`/ipad/order/${bill.booking_id}`)">
          + 继续加菜
        </button>
      </div>

      <!-- 优惠区 -->
      <div class="section discount-section">
        <div class="section-title">优惠 · Discount</div>
        <div class="discount-options">
          <div class="discount-row">
            <span class="discount-label">手动折扣</span>
            <div class="discount-control">
              <button @click="manualDiscount = Math.max(0, manualDiscount - 1)">−</button>
              <span>{{ manualDiscount }}%</span>
              <button @click="manualDiscount = Math.min(20, manualDiscount + 1)">+</button>
            </div>
            <span class="discount-amount" v-if="manualDiscount > 0">-¥{{ manualDiscountAmount.toFixed(0) }}</span>
          </div>
          <div class="coupon-row">
            <span class="discount-label">优惠券 · Coupon</span>
            <button class="link-btn" @click="showCouponPicker = true" v-if="!selectedCoupon">
              选择优惠券
            </button>
            <span class="coupon-name" v-else>
              {{ selectedCoupon.coupon_name }} -¥{{ selectedCoupon.coupon_amount }}
              <button class="remove-coupon" @click="selectedCoupon = null">×</button>
            </span>
          </div>
        </div>
      </div>

      <!-- 金额汇总 -->
      <div class="section summary-section">
        <div class="sum-row"><span>菜品合计</span><span>¥{{ dishTotal.toFixed(2) }}</span></div>
        <div class="sum-row" v-if="manualDiscount > 0"><span>手动折扣 {{ manualDiscount }}%</span><span class="neg">-¥{{ manualDiscountAmount.toFixed(2) }}</span></div>
        <div class="sum-row" v-if="selectedCoupon"><span>优惠券 · {{ selectedCoupon.coupon_name }}</span><span class="neg">-¥{{ selectedCoupon.coupon_amount }}</span></div>
        <div class="sum-row total"><span>应付金额 · Total</span><span class="total-price">¥{{ finalAmount.toFixed(2) }}</span></div>
      </div>

      <!-- 操作按钮 -->
      <div class="bill-actions">
        <button class="btn-print" @click="handlePrint">打印小票 · Print</button>
        <button class="btn-pay" @click="goPay">去支付 · Pay ¥{{ finalAmount.toFixed(0) }}</button>
      </div>
    </div>

    <div v-else class="loading-state">加载中...</div>

    <!-- 绑定会员弹窗 -->
    <Transition name="modal">
      <div v-if="showBindMember" class="modal-overlay" @click.self="showBindMember = false">
        <div class="modal-box">
          <div class="modal-header"><h3>绑定会员 · Bind Member</h3></div>
          <div class="modal-body">
            <div class="form-row">
              <label>手机号 · Phone</label>
              <input v-model="memberPhone" placeholder="输入手机号查询" class="search-input" />
              <button class="search-btn" @click="searchMember">查询</button>
            </div>
            <div v-if="foundMember" class="found-member">
              <span class="found-name">{{ foundMember.customer_name }}</span>
              <span class="found-points">{{ foundMember.total_points || 0 }}分</span>
              <button class="bind-btn" @click="bindMember">确认绑定</button>
            </div>
            <div v-if="memberSearched && !foundMember" class="no-member">
              未找到会员，<button class="link-btn" @click="createMember">快速创建</button>
            </div>
          </div>
          <div class="modal-actions">
            <button class="btn-cancel" @click="showBindMember = false">取消</button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- 优惠券选择弹窗 -->
    <Transition name="modal">
      <div v-if="showCouponPicker" class="modal-overlay" @click.self="showCouponPicker = false">
        <div class="modal-box">
          <div class="modal-header"><h3>选择优惠券 · Coupons</h3></div>
          <div class="modal-body coupon-list-body">
            <div v-if="coupons.length" class="coupon-list">
              <div v-for="c in coupons" :key="c.coupon_id" class="coupon-card" @click="selectCoupon(c)">
                <span class="cpn-name">{{ c.coupon_name }}</span>
                <span class="cpn-amount">-¥{{ c.coupon_amount }}</span>
                <span class="cpn-expire" v-if="c.expire_date">至 {{ c.expire_date }}</span>
              </div>
            </div>
            <div v-else class="no-data">暂无可用优惠券</div>
          </div>
          <div class="modal-actions">
            <button class="btn-cancel" @click="showCouponPicker = false">取消</button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useIpadStore } from '@/store/ipad'
import { ipadBillDetail, ipadMemberSearch, ipadCouponAvailable } from '@/api/ipad'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const ipad = useIpadStore()

const bill = ref(null)
const manualDiscount = ref(0)
const selectedCoupon = ref(null)
const showBindMember = ref(false)
const showCouponPicker = ref(false)
const memberPhone = ref('')
const foundMember = ref(null)
const memberSearched = ref(false)
const coupons = ref([])

const dishTotal = computed(() =>
  bill.value?.dish_list?.reduce((s, d) => s + Number(d.subtotal || d.sale_price * d.dish_quantity), 0) || 0
)

const manualDiscountAmount = computed(() =>
  manualDiscount.value > 0 ? dishTotal.value * (manualDiscount.value / 100) : 0
)

const finalAmount = computed(() => {
  let total = dishTotal.value
  if (manualDiscount.value > 0) total -= manualDiscountAmount.value
  if (selectedCoupon.value) total -= selectedCoupon.value.coupon_amount
  return Math.max(0, total)
})

function goPay() {
  // Store discount info
  sessionStorage.setItem('ipad_discount', JSON.stringify({
    manual_discount: manualDiscount.value,
    manual_discount_amount: manualDiscountAmount.value,
    coupon_id: selectedCoupon.value?.coupon_id,
    coupon_amount: selectedCoupon.value?.coupon_amount || 0,
    final_amount: finalAmount.value
  }))
  router.push(`/ipad/pay/${bill.value.booking_id}`)
}

async function searchMember() {
  if (!memberPhone.value) return
  memberSearched.value = true
  try {
    const res = await ipadMemberSearch(memberPhone.value)
    if (res.code === 200 && res.data) foundMember.value = res.data
    else foundMember.value = null
  } catch {
    // Mock
    foundMember.value = { customer_id: 'M001', customer_name: '测试会员', total_points: 1200 }
  }
}

function bindMember() {
  if (foundMember.value) {
    bill.value.customer_id = foundMember.value.customer_id
    bill.value.customer_name = foundMember.value.customer_name
    showBindMember.value = false
    ElMessage.success('会员已绑定')
    loadCoupons()
  }
}

function createMember() {
  router.push('/ipad/member')
}

async function loadCoupons() {
  try {
    const res = await ipadCouponAvailable({ customer_id: bill.value?.customer_id, booking_amount: dishTotal.value })
    if (res.code === 200) coupons.value = res.data || []
  } catch {
    coupons.value = [
      { coupon_id: 'C01', coupon_name: '满200减20', coupon_amount: 20, expire_date: '2026-12-31' },
      { coupon_id: 'C02', coupon_name: '满500减50', coupon_amount: 50, expire_date: '2026-12-31' },
    ]
  }
}

function selectCoupon(c) {
  selectedCoupon.value = c
  showCouponPicker.value = false
}

function handlePrint() {
  ElMessage.info('打印小票')
}

onMounted(async () => {
  const bookingId = route.params.bookingId
  try {
    const res = await ipadBillDetail(bookingId)
    if (res.code === 200) bill.value = res.data
  } catch {
    bill.value = {
      booking_id: bookingId,
      customer_name: '散客',
      guest_count: ipad.currentBooking?.guest_count || 0,
      dish_list: ipad.cartItems.map(i => ({
        dish_name: i.dish_name, dish_quantity: i.dish_quantity,
        sale_price: i.sale_price || i.unit_price,
        subtotal: (i.sale_price || i.unit_price) * i.dish_quantity
      })),
      total_amount: ipad.cartTotal,
      final_amount: ipad.cartTotal
    }
  }
  loadCoupons()
})
</script>

<style scoped>
.ipad-page { width: 100%; height: 100%; display: flex; flex-direction: column; background: var(--color-bg); }
.page-top { padding: 16px 24px; background: var(--color-card); border-bottom: 1px solid var(--color-border); display: flex; align-items: center; gap: 16px; flex-shrink: 0; }
.back-link { border: none; background: none; color: var(--color-text-muted); font-size: 13px; cursor: pointer; }
.page-title { font-size: 18px; font-weight: 700; color: var(--color-text); letter-spacing: 2px; }
.page-status { margin-left: auto; font-size: 12px; color: var(--color-text-muted); background: var(--color-bg-alt); padding: 4px 10px; border-radius: 4px; }

.bill-content { flex: 1; overflow-y: auto; padding: 16px 24px; max-width: 640px; margin: 0 auto; width: 100%; }

.section { background: var(--color-card); border-radius: var(--radius-lg); padding: 16px 20px; margin-bottom: 14px; border: 1px solid var(--color-border); }
.section-title { display: flex; align-items: center; justify-content: space-between; font-size: 14px; font-weight: 700; color: var(--color-text); letter-spacing: 1px; margin-bottom: 12px; padding-bottom: 10px; border-bottom: 1px solid var(--color-border-light); }

.link-btn { border: none; background: none; color: var(--color-primary); font-size: 13px; cursor: pointer; font-family: var(--font-family); }
.link-btn:hover { text-decoration: underline; }
.back-dishes { display: block; margin-top: 10px; }

.customer-row { display: flex; align-items: center; justify-content: space-between; }
.cust-info { display: flex; flex-direction: column; gap: 2px; }
.cust-name { font-size: 16px; font-weight: 600; color: var(--color-text); }
.cust-phone { font-size: 12px; color: var(--color-text-muted); }
.cust-people { font-size: 14px; color: var(--color-text-secondary); }

.dish-list { max-height: 280px; overflow-y: auto; }
.dish-row { display: flex; align-items: center; padding: 8px 0; border-bottom: 1px solid var(--color-border-light); gap: 10px; }
.dish-index { width: 20px; font-size: 12px; color: var(--color-text-muted); }
.dish-name { flex: 1; font-size: 14px; color: var(--color-text); }
.dish-spec { font-size: 11px; color: var(--color-text-muted); padding: 1px 6px; background: var(--color-bg-alt); border-radius: 3px; }
.dish-qty { width: 40px; text-align: center; font-size: 13px; color: var(--color-text-secondary); }
.dish-price { width: 60px; text-align: right; font-size: 14px; font-weight: 600; color: var(--color-text); }

.discount-row, .coupon-row { display: flex; align-items: center; justify-content: space-between; padding: 6px 0; }
.discount-label { font-size: 14px; color: var(--color-text-secondary); }
.discount-control { display: flex; align-items: center; gap: 12px; }
.discount-control button { width: 28px; height: 28px; border-radius: 50%; border: 1px solid var(--color-border); background: var(--color-card); font-size: 16px; cursor: pointer; }
.discount-control span { font-size: 15px; font-weight: 600; min-width: 40px; text-align: center; }
.discount-amount { font-size: 14px; color: var(--color-danger); font-weight: 600; }
.coupon-name { font-size: 13px; color: var(--color-success); font-weight: 600; display: flex; align-items: center; gap: 6px; }
.remove-coupon { border: none; background: none; color: var(--color-text-muted); font-size: 16px; cursor: pointer; }

.summary-section > div { display: flex; justify-content: space-between; padding: 6px 0; font-size: 14px; color: var(--color-text-secondary); }
.sum-row .neg { color: var(--color-danger); }
.sum-row.total { padding-top: 10px; border-top: 1px solid var(--color-border); font-size: 15px; font-weight: 600; color: var(--color-text); margin-top: 4px; }
.total-price { font-size: 28px; font-weight: 700; color: var(--color-accent-dark); }

.bill-actions { display: flex; gap: 12px; padding: 0 0 24px; }
.btn-print { flex: 1; padding: 16px; border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-card); font-size: 15px; cursor: pointer; }
.btn-pay { flex: 2; padding: 16px; border: none; border-radius: var(--radius-md); background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light)); color: white; font-size: 16px; font-weight: 700; cursor: pointer; letter-spacing: 1px; }
.btn-pay:hover { transform: translateY(-1px); box-shadow: 0 4px 14px rgba(45,74,62,0.3); }

.loading-state { flex: 1; display: flex; align-items: center; justify-content: center; color: var(--color-text-muted); font-size: 16px; }

/* 弹窗共用 */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 300; }
.modal-box { background: var(--color-card); border-radius: var(--radius-xl); width: 440px; max-width: 90vw; max-height: 80vh; overflow-y: auto; box-shadow: var(--shadow-xl); }
.modal-header { padding: 20px 24px 0; }
.modal-header h3 { font-size: 18px; font-weight: 700; color: var(--color-text); letter-spacing: 1px; }
.modal-body { padding: 16px 24px; }
.modal-actions { display: flex; gap: 10px; padding: 16px 24px 20px; justify-content: flex-end; }
.btn-cancel { padding: 12px 24px; border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-card); font-size: 14px; cursor: pointer; }

.form-row { margin-bottom: 12px; }
.form-row label { display: block; font-size: 13px; color: var(--color-text-secondary); margin-bottom: 6px; }
.search-input { width: 200px; padding: 8px 12px; border: 1px solid var(--color-border); border-radius: var(--radius-md); font-size: 14px; outline: none; margin-right: 8px; }
.search-input:focus { border-color: var(--color-primary); }
.search-btn { padding: 8px 16px; border: none; border-radius: var(--radius-md); background: var(--color-primary); color: white; cursor: pointer; }
.found-member { margin-top: 12px; padding: 12px; background: var(--color-bg-alt); border-radius: var(--radius-md); display: flex; align-items: center; gap: 10px; }
.found-name { font-size: 14px; font-weight: 600; color: var(--color-text); flex: 1; }
.found-points { font-size: 12px; color: var(--color-primary); }
.bind-btn { padding: 6px 14px; border: none; border-radius: var(--radius-md); background: var(--color-primary); color: white; font-size: 13px; cursor: pointer; }
.no-member { padding: 12px; text-align: center; color: var(--color-text-muted); font-size: 13px; }

.coupon-list-body { max-height: 300px; overflow-y: auto; }
.coupon-list { display: flex; flex-direction: column; gap: 8px; }
.coupon-card { padding: 12px 14px; border: 2px solid var(--color-border); border-radius: var(--radius-md); cursor: pointer; transition: all 0.15s; display: flex; align-items: center; gap: 12px; }
.coupon-card:hover { border-color: var(--color-primary); }
.cpn-name { font-size: 14px; font-weight: 600; color: var(--color-text); flex: 1; }
.cpn-amount { font-size: 16px; font-weight: 700; color: var(--color-danger); }
.cpn-expire { font-size: 11px; color: var(--color-text-muted); }

.no-data { text-align: center; padding: 24px; color: var(--color-text-muted); font-size: 14px; }

.modal-enter-active, .modal-leave-active { transition: opacity 0.2s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
</style>
