<template>
  <view class="stepper" :class="{lg: size==='lg', sm: size==='sm', disabled}">
    <view
      class="btn minus"
      :class="{disabled: value <= min || loading || disabled}"
      @tap="change(-1)">
      <text>−</text>
    </view>
    <view v-if="inputMode" class="num-wrap">
      <input class="num" type="number" :value="String(value)" @blur="onBlur" />
    </view>
    <text v-else class="num">{{ value }}</text>
    <view
      class="btn plus"
      :class="{disabled: value >= max || loading || disabled}"
      @tap="change(1)">
      <text>+</text>
    </view>
  </view>
</template>

<script setup>
const props = defineProps({
  value:      { type: Number, default: 1 },
  min:        { type: Number, default: 1 },
  max:        { type: Number, default: 99 },
  step:       { type: Number, default: 1 },
  size:       { type: String, default: 'md' },   // sm / md / lg
  disabled:   { type: Boolean, default: false },
  loading:    { type: Boolean, default: false },
  inputMode:  { type: Boolean, default: false }
})
const emit = defineEmits(['update:value', 'change'])

function change(delta) {
  if (props.disabled || props.loading) return
  const next = Math.max(props.min, Math.min(props.max, props.value + delta * props.step))
  if (next === props.value) {
    // 触底时给个小反馈
    uni.vibrateShort && uni.vibrateShort({ type: 'light' })
    return
  }
  emit('update:value', next)
  emit('change', next, delta)
}
function onBlur(e) {
  let v = parseInt(e.detail.value, 10)
  if (isNaN(v)) v = props.min
  v = Math.max(props.min, Math.min(props.max, v))
  emit('update:value', v)
  emit('change', v)
}
</script>

<style lang="scss">
@import '@/uni.scss';
.stepper{@include row-center; background:#fff; border:2rpx solid #eee; border-radius:40rpx; overflow:hidden;
  .btn{width:52rpx; height:52rpx; @include column; align-items:center; justify-content:center; color:$ink-black; font-size:36rpx;
    text{line-height:48rpx}
    &.disabled{color:#ccc}
  }
  .btn.minus{background:#fafafa}
  .btn.plus{background:rgba(201,168,108,0.10); color:$brand-gold; font-weight:700}
  .num{min-width:64rpx; text-align:center; font-size:28rpx; color:$ink-black; font-weight:600; padding:0 12rpx}
  .num-wrap{min-width:80rpx; input.num{text-align:center}}
  &.sm .btn{width:44rpx;height:44rpx;font-size:30rpx}
  &.sm .num{font-size:24rpx; min-width:50rpx}
  &.lg .btn{width:68rpx;height:68rpx;font-size:44rpx;border-radius:20rpx}
  &.lg .num{font-size:34rpx; min-width:96rpx}
  &.disabled{opacity:.55}
}
</style>
