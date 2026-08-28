<template>
  <view class="book-page">
    <!-- 门店切换条 -->
    <view class="store-strip">
      <picker v-if="appStore.stores.length" mode="selector" :range="appStore.stores" range-key="name" @change="onStoreChange">
        <view class="store-strip-inner">
          <text class="s-label">预订门店</text>
          <text class="s-value">{{ (appStore.currentStore && appStore.currentStore.name) || '选择门店' }} ▾</text>
        </view>
      </picker>
      <view v-else class="store-strip-inner">
        <text class="s-label">门店</text>
        <text class="s-value">{{ currentStoreName }}</text>
      </view>
    </view>

    <view class="form-card">
      <view class="card-title">
        <text class="bar"></text><text>预订信息</text>
      </view>

      <!-- 日期 -->
      <picker mode="date" :value="form.bookingDate" :start="minDate" :end="maxDate" @change="e => form.bookingDate = e.detail.value">
        <view class="field">
          <text class="f-label">到店日期</text>
          <text class="f-value link">{{ form.bookingDate || '请选择日期' }}</text>
        </view>
      </picker>

      <!-- 时段 -->
      <picker mode="selector" :range="timeSlots" :value="timeSlotIndex" @change="e => selectTimeSlot(Number(e.detail.value))">
        <view class="field">
          <text class="f-label">到店时间</text>
          <text class="f-value link">{{ form.bookingTime || '请选择时间' }}</text>
        </view>
      </picker>

      <!-- 人数 -->
      <view class="field">
        <text class="f-label">用餐人数</text>
        <view class="stepper-lg">
          <text class="minus" @tap="form.guestCount = Math.max(1, form.guestCount - 1)">−</text>
          <text class="num">{{ form.guestCount }} 位</text>
          <text class="plus"  @tap="form.guestCount = Math.min(60, form.guestCount + 1)">＋</text>
        </view>
      </view>

      <!-- 包厢 -->
      <view class="field">
        <text class="f-label">包厢选择</text>
        <view class="chip-row">
          <text
            v-for="opt in roomOptions"
            :key="opt.value"
            :class="['chip', {active: form.roomType === opt.value}]"
            @tap="form.roomType = opt.value"
          >{{ opt.label }}</text>
        </view>
      </view>
    </view>

    <view class="form-card">
      <view class="card-title"><text class="bar"></text><text>联系信息</text></view>

      <view class="field">
        <text class="f-label">您的称呼</text>
        <input class="f-input" v-model="form.customerName" placeholder="如：张先生" maxlength="20" />
      </view>

      <view class="field">
        <text class="f-label">手机号码</text>
        <view class="f-input-wrap">
          <input
            class="f-input"
            v-model="form.customerPhone"
            type="number" maxlength="11"
            placeholder="请输入11位手机号"
          />
          <!-- #ifdef MP-WEIXIN -->
          <button
            class="phone-btn"
            size="mini"
            type="warn"
            open-type="getPhoneNumber"
            @getphonenumber="onGetPhone"
            plain
          >一键获取</button>
          <!-- #endif -->
        </view>
      </view>

      <view class="field">
        <text class="f-label">备注（选填）</text>
        <textarea
          class="f-textarea"
          v-model="form.remark"
          placeholder="例如：靠窗位置、对某种食材过敏、生日布置等"
          maxlength="100"
        ></textarea>
      </view>
    </view>

    <view class="form-card" v-if="appStore.currentStore">
      <view class="card-title"><text class="bar"></text><text>预订须知</text></view>
      <view class="tips">
        <text>· 预订提交成功后，门店将通过电话与您确认</text>
        <text>· 如需取消，请提前 2 小时致电门店</text>
        <text>· 包厢需支付定金 200 元（支持小程序微信支付）</text>
        <text>· 高峰时段（周末/晚市 17:30-19:30）建议提前预订</text>
      </view>
    </view>

    <view style="height: 180rpx"></view>

    <!-- 底部提交 -->
    <view class="submit-bar">
      <view class="total">
        <text class="t-label">预估</text>
        <text class="t-val">¥{{ estimatePrice }}</text>
      </view>
      <view :class="['submit-btn', submitting && 'disabled']" @tap="submit">
        {{ submitting ? '提交中…' : '提交预订' }}
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useAppStore } from '@/store/app'
import { createBooking } from '@/api/booking'
import { requestSubscribe, wechatGetPhoneFromEvent } from '@/api/wx'
import { todayStr, addDays, isMobile, isEmpty, formatDate } from '@/utils/util'

const appStore = useAppStore()

const currentStoreName = computed(() => appStore.currentStore?.name || '又见炊烟·宁国店')
const minDate = todayStr()
const maxDate = addDays(todayStr(), 60)

const timeSlots = [
  '10:30','11:00','11:30','12:00','12:30','13:00',
  '14:00','14:30','15:00',
  '17:00','17:30','18:00','18:30','19:00','19:30','20:00','20:30'
]
const timeSlotIndex = ref(0)

const roomOptions = [
  { label: '大厅',   value: null },
  { label: '小包厢（6-8人）', value: 'SMALL' },
  { label: '中包厢（10-12人）', value: 'MID' },
  { label: '大包厢（14-20人）', value: 'BIG' },
  { label: '宴会厅（20人以上）', value: 'HALL' }
]

const form = reactive({
  bookingDate:  todayStr(),
  bookingTime:  timeSlots[0],
  guestCount:   4,
  roomType:     null,
  customerName: appStore.userInfo?.nickname || '',
  customerPhone: appStore.userInfo?.phone || appStore.phoneNumber || '',
  remark: ''
})

function selectTimeSlot(i) {
  timeSlotIndex.value = i
  form.bookingTime = timeSlots[i]
}
function onStoreChange(e) {
  const store = appStore.stores[e.detail.value]
  if (store) appStore.setCurrentStoreId(store.id)
}

async function onGetPhone(e) {
  try {
    const phone = await wechatGetPhoneFromEvent(e)
    if (phone) {
      form.customerPhone = phone
      uni.showToast({ title: '已获取手机号', icon: 'success' })
    }
  } catch (err) {
    uni.showToast({ title: err.message || '授权失败', icon: 'none' })
  }
}

const estimatePrice = computed(() => {
  const perCapita = 120   // 又见炊烟人均消费
  const roomFee = form.roomType === 'SMALL' ? 0
                : form.roomType === 'MID' ? 200
                : form.roomType === 'BIG' ? 300
                : form.roomType === 'HALL' ? 1000 : 0
  return (form.guestCount * perCapita + roomFee).toFixed(2)
})

const submitting = ref(false)
async function submit() {
  if (submitting.value) return
  if (isEmpty(form.customerName)) return uni.showToast({ title: '请输入您的称呼', icon: 'none' })
  if (!isMobile(form.customerPhone)) return uni.showToast({ title: '请输入正确的手机号', icon: 'none' })

  submitting.value = true
  try {
    // 1) 先请求订阅消息授权（预订成功+到店前提醒），失败不阻塞
    try { await requestSubscribe('BOOK_SUCCESS', 'BOOK_REMIND') } catch {}

    // 2) 调后端接口
    const res = await createBooking({
      bookingDate: form.bookingDate,
      bookingTime: form.bookingTime,
      guestCount:  form.guestCount,
      roomType:    form.roomType,
      customerName:form.customerName,
      customerPhone: form.customerPhone,
      remark:      form.remark
    })

    uni.showToast({ title: '预订成功！门店稍后致电确认', icon: 'success', duration: 2000 })
    setTimeout(() => {
      uni.redirectTo({
        url: `/pages/bookingList/bookingList?newId=${(res && res.id) ? res.id : ''}`
      }).catch(() => {
        uni.switchTab({ url: '/pages/me/me' })
      })
    }, 1500)
  } catch (e) {
    // request.js 已经 toast 了，这里只做兜底
    console.warn('预订提交失败', e)
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
@import '@/uni.scss';
.book-page { padding: $spacing-md; background: $brand-bg; min-height: 100vh; box-sizing: border-box; }

/* 门店切换条 */
.store-strip {
  background: linear-gradient(135deg, #FFFBF0, #FFF4DC);
  border: 1rpx solid $brand-gold-light; border-radius: $radius-md;
  padding: $spacing-md; margin-bottom: $spacing-md;
}
.store-strip-inner { display:flex; align-items:center; justify-content:space-between; }
.store-strip .s-label { color: $brand-gold-dark; font-weight: 600; }
.store-strip .s-value { color: $text-1; font-weight: 600; font-size: $font-md; }

/* 表单卡片 */
.form-card {
  background: #fff; border-radius: $radius-lg; padding: $spacing-md;
  box-shadow: $shadow-card; margin-bottom: $spacing-md;
}
.card-title { display:flex; align-items:center; gap: 14rpx; margin-bottom: $spacing-sm; }
.card-title .bar { width: 8rpx; height: 30rpx; background: linear-gradient(180deg, #E6D4A8, #C9A86C); border-radius: 4rpx; }
.card-title text:last-child { font-size: $font-md; font-weight: 700; color: $text-1; }

.field {
  display: flex; align-items: center; padding: 22rpx 0;
  border-bottom: 1rpx solid #F4F0E6;
}
.field:last-of-type { border-bottom: 0; }
.f-label { width: 180rpx; color: $text-2; font-size: $font-sm; flex-shrink: 0; }
.f-value { flex: 1; color: $text-1; font-size: $font-md; }
.f-value.link { color: $brand-gold-dark; }
.f-input {
  flex: 1; height: 60rpx; font-size: $font-md; color: $text-1; text-align: right;
}
.f-input-wrap { flex: 1; display:flex; align-items:center; gap: $spacing-xs; }
.f-input-wrap .f-input { height: 72rpx; text-align: left; }
.phone-btn {
  transform: scale(0.82); border-color: $brand-gold !important; color: $brand-gold-dark !important;
  font-size: $font-xs !important;
}
.f-textarea {
  flex: 1; width: 100%; min-height: 140rpx; font-size: $font-sm;
  padding: 16rpx; background: #FAF7F2; border-radius: $radius-sm; box-sizing: border-box; color: $text-1;
}

/* 步进器 */
.stepper-lg {
  display:flex; align-items:center; gap: $spacing-md;
  border: 1rpx solid #EFE9D8; border-radius: $radius-pill; padding: 6rpx 8rpx;
}
.stepper-lg .minus, .stepper-lg .plus {
  width: 52rpx; height: 52rpx; line-height: 48rpx; text-align:center; border-radius: 50%;
  background: #F7F0DE; color: $brand-gold-dark; font-size: 32rpx;
}
.stepper-lg .plus { background: linear-gradient(135deg, #E6D4A8, #C9A86C); color:#fff; }
.stepper-lg .num { min-width: 120rpx; text-align:center; font-weight: 700; color: $text-1; font-size: $font-md; }

/* 包厢 chips */
.chip-row { display:flex; flex-wrap: wrap; gap: 12rpx; flex: 1; justify-content: flex-end; }
.chip {
  padding: 10rpx 20rpx; border-radius: $radius-pill; font-size: $font-xs;
  background: #F7F3E7; color: $text-2; border: 1rpx solid transparent;
}
.chip.active {
  background: linear-gradient(135deg, #FFF3D6, #FFE3A8);
  border-color: $brand-gold; color: $brand-gold-dark; font-weight: 600;
}

.tips { color: $text-3; font-size: $font-xs; line-height: 1.8; }

/* 底部提交栏 */
.submit-bar {
  position: fixed; left: 0; right: 0; bottom: 0;
  padding: $spacing-sm $spacing-md calc(env(safe-area-inset-bottom) + 16rpx);
  background: #fff; box-shadow: 0 -4rpx 20rpx rgba(0,0,0,0.06);
  display:flex; align-items: center; gap: $spacing-md;
}
.total { flex: 1; display:flex; align-items: baseline; gap: 8rpx; }
.t-label { color: $text-3; font-size: $font-xs; }
.t-val { color: $brand-gold-dark; font-weight: 800; font-size: $font-xl; }
.submit-btn {
  flex: 1.6; height: 92rpx; line-height: 92rpx; text-align: center; color:#fff;
  background: linear-gradient(135deg, #E6D4A8, #C9A86C); border-radius: $radius-pill;
  box-shadow: $shadow-gold; font-weight: 700; font-size: $font-md; letter-spacing: 2rpx;
}
.submit-btn.disabled { opacity: 0.6; }
</style>
