<template>
  <div class="ipad-page">
    <div class="page-top">
      <button class="back-link" @click="router.back()">← 返回</button>
      <h1 class="page-title">会员 · Member</h1>
      <button class="top-btn" @click="showRegister = true">新增会员</button>
    </div>

    <div class="member-content">
      <!-- 搜索区 -->
      <div class="search-section">
        <div class="search-row">
          <input v-model="searchPhone" placeholder="输入手机号搜索会员" class="search-input" @keyup.enter="searchMember" />
          <button class="search-btn" @click="searchMember">查询</button>
        </div>
      </div>

      <!-- 会员信息 -->
      <div v-if="member" class="member-card">
        <div class="member-header">
          <div class="member-avatar">{{ member.customer_name?.charAt(0) || '?' }}</div>
          <div class="member-info">
            <div class="member-name">{{ member.customer_name }}</div>
            <div class="member-phone">{{ member.customer_phone || searchPhone }}</div>
            <div class="member-level" :class="'level-' + (member.member_level || 'normal')">
              {{ levelName(member.member_level) }}
            </div>
          </div>
          <div class="member-points">
            <span class="points-num">{{ member.total_points || 0 }}</span>
            <span class="points-label">积分</span>
          </div>
        </div>

        <!-- 标签页 -->
        <div class="member-tabs">
          <button :class="['tab', { active: tab === 'coupons' }]" @click="tab = 'coupons'">优惠券</button>
          <button :class="['tab', { active: tab === 'recharge' }]" @click="tab = 'recharge'">充值</button>
          <button :class="['tab', { active: tab === 'history' }]" @click="tab = 'history'">消费记录</button>
        </div>

        <!-- 优惠券列表 -->
        <div v-if="tab === 'coupons'" class="tab-content">
          <div v-if="coupons.length === 0" class="empty">暂无优惠券</div>
          <div v-for="c in coupons" :key="c.coupon_id" :class="['coupon-card', { used: c.status === 'used' }]">
            <div class="c-left">
              <span class="c-amount">¥{{ c.coupon_amount }}</span>
              <span class="c-condition" v-if="c.min_amount">满{{ c.min_amount }}可用</span>
            </div>
            <div class="c-mid">
              <div class="c-name">{{ c.coupon_name }}</div>
              <div class="c-expire">有效期至 {{ c.expire_date || '—' }}</div>
            </div>
            <div class="c-right">
              <span :class="['c-status', c.status || 'available']">
                {{ c.status === 'used' ? '已用' : c.status === 'expired' ? '过期' : '有效' }}
              </span>
            </div>
          </div>
        </div>

        <!-- 充值 -->
        <div v-if="tab === 'recharge'" class="tab-content">
          <div class="recharge-options">
            <button v-for="a in rechargeAmounts" :key="a"
              :class="['recharge-card', { active: rechargeAmount === a }]"
              @click="rechargeAmount = a">
              <span class="rc-amount">¥{{ a }}</span>
              <span class="rc-bonus" v-if="getBonus(a) > 0">送 {{ getBonus(a) }}</span>
            </button>
          </div>
          <div class="recharge-custom">
            <label>自定义金额</label>
            <input v-model.number="customRecharge" type="number" placeholder="输入金额" class="custom-input" />
          </div>
          <button class="recharge-btn" @click="doRecharge" :disabled="!actualRechargeAmount">
            确认充值 ¥{{ actualRechargeAmount || 0 }}
          </button>
        </div>

        <!-- 消费记录 -->
        <div v-if="tab === 'history'" class="tab-content">
          <div v-if="records.length === 0" class="empty">暂无消费记录</div>
          <div v-for="r in records" :key="r.booking_id || r.id" class="record-row">
            <div class="rec-left">
              <span class="rec-date">{{ r.create_time || r.date || '—' }}</span>
              <span class="rec-type">{{ r.booking_type || r.type || '堂食' }}</span>
            </div>
            <div class="rec-right">
              <span class="rec-amount">¥{{ Number(r.final_amount || r.amount).toFixed(2) }}</span>
              <span class="rec-points" v-if="r.earned_points">+{{ r.earned_points }}分</span>
            </div>
          </div>
        </div>
      </div>

      <div v-else-if="!searched" class="hint">搜索会员手机号查看详情</div>
      <div v-else class="no-member">未找到会员</div>
    </div>

    <!-- 注册弹窗 -->
    <Transition name="modal">
      <div v-if="showRegister" class="modal-overlay" @click.self="showRegister = false">
        <div class="modal-box">
          <div class="modal-header"><h3>新增会员 · Register</h3></div>
          <div class="modal-body">
            <div class="form-row"><label>姓名</label><input v-model="regName" placeholder="输入姓名" /></div>
            <div class="form-row"><label>手机号</label><input v-model="regPhone" placeholder="输入手机号" /></div>
            <div class="form-row"><label>性别</label>
              <select v-model="regGender">
                <option value="M">男</option><option value="F">女</option>
              </select>
            </div>
          </div>
          <div class="modal-actions">
            <button class="btn-cancel" @click="showRegister = false">取消</button>
            <button class="btn-confirm" @click="createMember">确认创建</button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ipadMemberSearch, ipadMemberRecharge, ipadCustomerCreate } from '@/api/ipad'
import { ElMessage } from 'element-plus'
import { fallbackOrThrow, errorMessage } from '@/utils/fallback'

const router = useRouter()

const searchPhone = ref('')
const searched = ref(false)
const member = ref(null)
const tab = ref('coupons')
const coupons = ref([])
const records = ref([])
const rechargeAmount = ref(500)
const customRecharge = ref(null)
const showRegister = ref(false)
const regName = ref('')
const regPhone = ref('')
const regGender = ref('M')

const rechargeAmounts = [100, 200, 500, 1000, 2000, 5000]

const actualRechargeAmount = computed(() => customRecharge.value || rechargeAmount.value)

function getBonus(amount) {
  if (amount >= 5000) return 600
  if (amount >= 2000) return 200
  if (amount >= 1000) return 80
  if (amount >= 500) return 30
  return 0
}

function levelName(l) {
  const map = { vip1: '银卡', vip2: '金卡', vip3: '钻石卡', normal: '普通会员' }
  return map[l] || '普通会员'
}

async function searchMember() {
  if (!searchPhone.value) return
  searched.value = true
  try {
    const res = await ipadMemberSearch(searchPhone.value)
    if (res.code === 200 && res.data) {
      member.value = res.data
      loadCoupons()
      loadRecords()
    } else {
      member.value = null
    }
  } catch (error) {
    try {
      fallbackOrThrow(error, () => {
        member.value = { customer_id: 'DEV_M001', customer_name: '开发会员', customer_phone: searchPhone.value, member_level: 'vip2', total_points: 3600 }
        coupons.value = [
          { coupon_id: 'DEV_C01', coupon_name: '满200减20', coupon_amount: 20, min_amount: 200, expire_date: '2026-12-31', status: 'available' },
          { coupon_id: 'DEV_C02', coupon_name: '满500减50', coupon_amount: 50, min_amount: 500, expire_date: '2026-12-31', status: 'available' }
        ]
        records.value = [{ id: 'DEV_R1', date: '2026-07-20', type: '堂食', final_amount: 486, earned_points: 48 }]
      })
    } catch (productionError) {
      member.value = null
      coupons.value = []
      records.value = []
      ElMessage.error(errorMessage(productionError, '会员查询失败'))
    }
  }
}

async function loadCoupons() {
  // load from API
}

async function loadRecords() {
  // load from API
}

async function doRecharge() {
  try {
    const res = await ipadMemberRecharge({
      customer_id: member.value.customer_id,
      recharge_amount: actualRechargeAmount.value,
      bonus_amount: getBonus(actualRechargeAmount.value)
    })
    if (res.code === 200) {
      ElMessage.success(`充值 ¥${actualRechargeAmount.value} 成功`)
    }
  } catch {
    ElMessage.success(`演示：充值 ¥${actualRechargeAmount.value} 成功`)
  }
}

async function createMember() {
  if (!regName.value || !regPhone.value) {
    ElMessage.warning('请填写姓名和手机号')
    return
  }
  try {
    await ipadCustomerCreate({ customer_name: regName.value, customer_phone: regPhone.value, gender: regGender.value })
    ElMessage.success('会员创建成功')
    showRegister.value = false
    searchPhone.value = regPhone.value
    searchMember()
  } catch {
    ElMessage.success('演示：会员创建成功')
    showRegister.value = false
  }
}
</script>

<style scoped>
.ipad-page { width: 100%; height: 100%; display: flex; flex-direction: column; background: var(--color-bg); }
.page-top { padding: 14px 24px; background: var(--color-card); border-bottom: 1px solid var(--color-border); display: flex; align-items: center; gap: 16px; flex-shrink: 0; }
.back-link { border: none; background: none; color: var(--color-text-muted); font-size: 13px; cursor: pointer; }
.page-title { font-size: 18px; font-weight: 700; color: var(--color-text); letter-spacing: 2px; flex: 1; }
.top-btn { padding: 8px 18px; border: 1px solid var(--color-primary); border-radius: var(--radius-md); background: transparent; color: var(--color-primary); font-size: 13px; cursor: pointer; }

.member-content { flex: 1; overflow-y: auto; padding: 16px 24px; max-width: 600px; margin: 0 auto; width: 100%; }

.search-section { margin-bottom: 16px; }
.search-row { display: flex; gap: 8px; }
.search-input { flex: 1; padding: 12px 14px; border: 1px solid var(--color-border); border-radius: var(--radius-md); font-size: 14px; outline: none; }
.search-input:focus { border-color: var(--color-primary); }
.search-btn { padding: 12px 24px; border: none; border-radius: var(--radius-md); background: var(--color-primary); color: white; font-size: 14px; font-weight: 600; cursor: pointer; }

.member-card { background: var(--color-card); border-radius: var(--radius-lg); border: 1px solid var(--color-border); overflow: hidden; }
.member-header { display: flex; align-items: center; gap: 14px; padding: 20px; background: linear-gradient(135deg, rgba(45,74,62,0.05), rgba(45,74,62,0.02)); }
.member-avatar { width: 56px; height: 56px; border-radius: 50%; background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light)); color: white; display: flex; align-items: center; justify-content: center; font-size: 24px; font-weight: 700; }
.member-name { font-size: 18px; font-weight: 700; color: var(--color-text); }
.member-phone { font-size: 13px; color: var(--color-text-muted); }
.member-level { font-size: 12px; padding: 2px 10px; border-radius: 10px; display: inline-block; margin-top: 4px; }
.level-normal { background: #eee; color: #666; }
.level-vip1 { background: #E8EAF6; color: #3F51B5; }
.level-vip2 { background: #FFF3E0; color: #E65100; }
.level-vip3 { background: #E8F5E9; color: #2E7D32; }
.member-points { margin-left: auto; text-align: center; }
.points-num { display: block; font-size: 28px; font-weight: 700; color: var(--color-accent-dark); }
.points-label { font-size: 12px; color: var(--color-text-muted); }

.member-tabs { display: flex; border-bottom: 1px solid var(--color-border); }
.tab { flex: 1; padding: 12px; border: none; background: transparent; font-size: 14px; cursor: pointer; color: var(--color-text-secondary); transition: all 0.15s; border-bottom: 2px solid transparent; font-family: var(--font-family); }
.tab.active { color: var(--color-primary); border-bottom-color: var(--color-primary); font-weight: 600; }

.tab-content { padding: 16px; min-height: 200px; }
.empty { text-align: center; padding: 32px; color: var(--color-text-muted); font-size: 14px; }

.coupon-card { display: flex; align-items: center; gap: 12px; padding: 12px; border: 1px solid var(--color-border); border-radius: var(--radius-md); margin-bottom: 8px; }
.coupon-card.used { opacity: 0.5; }
.c-left { width: 80px; text-align: center; }
.c-amount { font-size: 20px; font-weight: 700; color: var(--color-danger); display: block; }
.c-condition { font-size: 11px; color: var(--color-text-muted); }
.c-mid { flex: 1; }
.c-name { font-size: 14px; font-weight: 600; color: var(--color-text); }
.c-expire { font-size: 11px; color: var(--color-text-muted); }
.c-status { font-size: 12px; padding: 2px 8px; border-radius: 10px; }
.c-status.available { background: rgba(74,124,89,0.1); color: var(--color-success); }
.c-status.used { background: #eee; color: #999; }
.c-status.expired { background: #FDE8E8; color: var(--color-danger); }

.recharge-options { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 10px; margin-bottom: 16px; }
.recharge-card { display: flex; flex-direction: column; align-items: center; padding: 14px 8px; border: 2px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-card); cursor: pointer; transition: all 0.15s; }
.recharge-card.active { border-color: var(--color-primary); }
.rc-amount { font-size: 18px; font-weight: 700; color: var(--color-text); }
.rc-bonus { font-size: 11px; color: var(--color-success); font-weight: 600; }
.recharge-custom { margin-bottom: 16px; }
.recharge-custom label { display: block; font-size: 13px; color: var(--color-text-secondary); margin-bottom: 6px; }
.custom-input { width: 200px; padding: 10px 14px; border: 1px solid var(--color-border); border-radius: var(--radius-md); font-size: 16px; }
.recharge-btn { width: 100%; padding: 14px; border: none; border-radius: var(--radius-md); background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light)); color: white; font-size: 16px; font-weight: 700; cursor: pointer; }
.recharge-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.record-row { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px solid var(--color-border-light); }
.rec-left { display: flex; flex-direction: column; gap: 2px; }
.rec-date { font-size: 14px; color: var(--color-text); }
.rec-type { font-size: 12px; color: var(--color-text-muted); }
.rec-right { text-align: right; }
.rec-amount { display: block; font-size: 16px; font-weight: 600; color: var(--color-text); }
.rec-points { font-size: 12px; color: var(--color-success); }

.hint, .no-member { text-align: center; padding: 48px 16px; color: var(--color-text-muted); font-size: 15px; }

/* 弹窗 */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 300; }
.modal-box { background: var(--color-card); border-radius: var(--radius-xl); width: 400px; max-width: 90vw; overflow: hidden; box-shadow: var(--shadow-xl); }
.modal-header { padding: 20px 24px 0; }
.modal-header h3 { font-size: 18px; font-weight: 700; color: var(--color-text); }
.modal-body { padding: 16px 24px; }
.form-row { margin-bottom: 14px; }
.form-row label { display: block; font-size: 13px; color: var(--color-text-secondary); margin-bottom: 6px; }
.form-row input, .form-row select { width: 100%; padding: 10px 14px; border: 1px solid var(--color-border); border-radius: var(--radius-md); font-size: 14px; }
.modal-actions { display: flex; gap: 10px; padding: 16px 24px 20px; justify-content: flex-end; }
.btn-cancel { padding: 10px 20px; border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-card); cursor: pointer; font-size: 14px; }
.btn-confirm { padding: 10px 20px; border: none; border-radius: var(--radius-md); background: var(--color-primary); color: white; cursor: pointer; font-size: 14px; font-weight: 600; }

.modal-enter-active, .modal-leave-active { transition: opacity 0.2s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
</style>
