<template>
  <view class="feedback-page">
    <!-- 反馈类型 -->
    <view class="card">
      <view class="title">🙋 反馈类型</view>
      <view class="types">
        <view
          v-for="t in types"
          :key="t.key"
          class="type-card"
          :class="{active: form.type===t.key}">
          <text class="emoji">{{ t.emoji }}</text>
          <text class="label" @tap="form.type=t.key">{{ t.label }}</text>
        </view>
      </view>
    </view>

    <view class="card">
      <view class="title">📝 请详细描述</view>
      <textarea
        v-model="form.content"
        class="textarea"
        placeholder="遇到的问题、建议或投诉都可以写哦～写越详细我们处理越快"
        maxlength="1000"/>
      <view class="count muted">{{ form.content.length }}/1000</view>

      <view class="title sm">📷 图片 / 截图 （最多 6 张）</view>
      <view class="upload-row">
        <view v-for="(img,i) in form.images" :key="i" class="upload-item">
          <image class="img" :src="img" mode="aspectFill" @tap="preview(i)"/>
          <view class="del" @tap="form.images.splice(i,1)">×</view>
        </view>
        <view v-if="form.images.length<6" class="uploader" @tap="pickImages">
          <text class="plus">+</text>
          <text class="muted sm">上传图片</text>
        </view>
      </view>
    </view>

    <view class="card">
      <view class="title">📞 联系方式（可选，方便回复）</view>
      <view class="row">
        <text class="label">手机号</text>
        <input v-model="form.phone" class="input" type="number" maxlength="11" placeholder="请输入您的手机号"/>
      </view>
      <view class="row" v-if="isMp">
        <text class="label">微信授权</text>
        <button class="mini-btn gold-outline" open-type="getPhoneNumber" @getphonenumber="onGetPhone">一键带入</button>
      </view>
    </view>

    <view class="tips-card">
      <view class="tip-head">工作时间</view>
      <view class="tip">· 工作日 10:00-21:00 · 周末正常</view>
      <view class="tip">· 如需紧急协助请直接致电 <text class="tel" @tap="call">400-0000-000</text></view>
    </view>

    <view class="gap"></view>
    <view class="footer">
      <button class="gold-btn big" :loading="submitting" :disabled="!canSubmit" @tap="submit">
        提交反馈
      </button>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref, computed } from 'vue'
import commentApi from '@/api/comment'
import uploadApi  from '@/api/upload'
import { fetchPhoneNumber, toast } from '@/api/wx'

const isMp = (() => {
  // #ifdef MP-WEIXIN
  return true
  // #endif
  return false
})()

const types = [
  { key: 'FOOD',     emoji: '🍲', label: '菜品建议' },
  { key: 'SERVICE',  emoji: '💁', label: '服务体验' },
  { key: 'ENV',      emoji: '🏞️', label: '环境问题' },
  { key: 'PAYMENT',  emoji: '💳', label: '支付/订单' },
  { key: 'SUGGEST',  emoji: '💡', label: '其他建议' },
  { key: 'COMPLAINT',emoji: '🚨', label: '投诉' }
]
const submitting = ref(false)
const form = reactive({
  type:    'FOOD',
  content: '',
  images:  [],
  phone:   '',
  contact: ''
})
const canSubmit = computed(() => form.content.trim().length >= 8)

async function pickImages(){
  const left = 6 - form.images.length
  if (left <= 0) return
  try {
    const list = await uploadApi.chooseAndUpload({ count: left })
    form.images.push(...list)
  } catch(e){}
}
function preview(i){ uni.previewImage({ urls: form.images, current: form.images[i] }) }
function onGetPhone(e) {
  fetchPhoneNumber(e.detail.code)
    .then(r => { form.phone = r.phoneNumber || r.phone; toast('已带入') })
    .catch(() => toast('授权失败，请手动输入'))
}
function call(){ uni.makePhoneCall({ phoneNumber: '4000000000' }) }

async function submit(){
  if (!canSubmit.value) return toast('请至少写8个字的描述')
  submitting.value = true
  try {
    await commentApi.submitFeedback({
      ...form,
      contact: form.phone || ''
    })
    await uni.showModal({
      title: '反馈已提交',
      content: '我们会尽快处理，给您回电或微信回复。感谢您的监督！',
      showCancel: false
    })
    uni.navigateBack()
  } catch(e) { toast(e?.message || '提交失败，请稍后再试')
  } finally { submitting.value = false }
}
</script>

<style lang="scss">
@import '@/uni.scss';
.feedback-page{background:$page-bg; min-height:100vh; padding-bottom:180rpx}
.card{@include card; margin:20rpx}
.title{font-size:30rpx; font-weight:600; color:$ink-black; margin-bottom:20rpx;
  &.sm{margin-top:28rpx; margin-bottom:16rpx}
}
.types{@include row-wrap; gap:16rpx;
  .type-card{@include column; align-items:center; justify-content:center;
    width: calc((100% - 48rpx) / 3); padding:28rpx 0; background:#fffaf3;
    border:2rpx solid transparent; border-radius:14rpx;
    .emoji{font-size:48rpx} .label{font-size:24rpx; color:$ink-black; margin-top:8rpx}
    &.active{border-color:$brand-gold; background:linear-gradient(180deg,#fff8e9,#fff);
      box-shadow:0 6rpx 18rpx rgba(201,168,108,.22)}
  }
}
.textarea{width:100%; min-height:260rpx; background:#fafafa; border-radius:12rpx; padding:20rpx; font-size:28rpx; box-sizing:border-box}
.count{text-align:right; margin-top:8rpx}
.upload-row{@include row-wrap; gap:16rpx;
  .upload-item{width:200rpx; height:200rpx; position:relative;
    .img{width:100%; height:100%; border-radius:12rpx}
    .del{position:absolute; right:-10rpx; top:-10rpx; width:40rpx; height:40rpx; background:#000a; color:#fff; border-radius:50%; text-align:center; line-height:40rpx; font-size:24rpx}
  }
  .uploader{width:200rpx; height:200rpx; border:2rpx dashed $brand-gold; border-radius:12rpx;
    @include column; align-items:center; justify-content:center; background:#fffaf0;
    .plus{color:$brand-gold; font-size:60rpx; line-height:60rpx}
    .sm{margin-top:6rpx}
  }
}
.row{@include row-between; padding:14rpx 0; border-top:2rpx dashed #eee; &:first-child{border-top:0}
  .label{font-size:28rpx; color:$ink-black; min-width:160rpx}
  .input{flex:1; padding:10rpx 16rpx; background:#fafafa; border-radius:8rpx; font-size:28rpx}
}
.mini-btn{font-size:22rpx; padding:0 16rpx; line-height:52rpx; height:52rpx; margin-left:16rpx;}
.gold-outline{border:2rpx solid $brand-gold; color:$brand-gold; background:#fff;}
.tips-card{margin:20rpx; padding:24rpx; background:#fffef5; border:2rpx dashed rgba(201,168,108,0.40); border-radius:14rpx;
  .tip-head{font-weight:600; color:$brand-deep-gold; margin-bottom:12rpx}
  .tip{color:$text-muted; font-size:24rpx; margin-top:6rpx}
  .tel{color:$brand-gold; text-decoration:underline; margin-left:6rpx}
}
.gap{height:40rpx}
.footer{@include footer-bar;}
</style>
