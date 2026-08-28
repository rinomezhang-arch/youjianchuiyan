<template>
  <view class="comment-page">
    <!-- 订单摘要 -->
    <view class="card order-sum">
      <view class="title">📦 订单信息</view>
      <view v-if="order.id" class="rows">
        <view class="row"><text>订单号</text><text>{{ order.orderNo || order.id }}</text></view>
        <view class="row"><text>下单时间</text><text>{{ order.createdAt }}</text></view>
        <view class="row"><text>金额</text><text class="price">¥{{ ((order.payableFen||order.totalFen)/100).toFixed(2) }}</text></view>
      </view>
      <view v-if="(order.items||[]).length" class="items">
        <view v-for="it in order.items.slice(0,3)" :key="it.id||it.dishId" class="item">
          <image v-if="it.image" class="t" :src="it.image" mode="aspectFill"/>
          <text class="n">{{ it.name }}</text>
        </view>
      </view>
    </view>

    <!-- 总评分 -->
    <view class="card rating-block">
      <view class="line">
        <text class="label">整体评分</text>
        <stars v-model="form.rating" size="lg" />
        <text class="tag-r">{{ ratingText }}</text>
      </view>
      <view class="line">
        <text class="label">口味</text>
        <stars v-model="form.taste" />
      </view>
      <view class="line">
        <text class="label">环境</text>
        <stars v-model="form.env" />
      </view>
      <view class="line">
        <text class="label">服务</text>
        <stars v-model="form.service" />
      </view>
    </view>

    <!-- 标签 -->
    <view class="card">
      <view class="title">✨ 快速标签</view>
      <view class="chips">
        <text
          v-for="t in tags"
          :key="t"
          class="chip"
          :class="{active: form.tags.includes(t)}"
          @tap="toggleTag(t)">{{ t }}</text>
      </view>
    </view>

    <!-- 文字评价 -->
    <view class="card">
      <view class="title">📝 写下你的感受</view>
      <textarea
        v-model="form.text"
        class="textarea"
        placeholder="菜品口味如何？有什么建议想对大厨说？～"
        maxlength="500"/>
      <view class="count muted">{{ form.text.length }}/500</view>

      <view class="title sm">📷 晒图（最多 9 张）</view>
      <view class="upload-row">
        <view v-for="(img,i) in form.images" :key="i" class="upload-item">
          <image class="img" :src="img" mode="aspectFill" @tap="preview(i)"/>
          <view class="del" @tap="form.images.splice(i,1)">×</view>
        </view>
        <view v-if="form.images.length<9" class="uploader" @tap="pickImages">
          <text class="plus">+</text>
          <text class="muted sm">上传图片</text>
        </view>
      </view>
    </view>

    <view class="gap"></view>
    <view class="footer">
      <view class="left">
        <text class="chk" :class="{on: form.anonymous}" @tap="form.anonymous = !form.anonymous"></text>
        <text class="muted sm">匿名评价</text>
      </view>
      <button class="gold-btn big" :loading="submitting" :disabled="!canSubmit" @tap="submit">提交评价</button>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, computed, onMounted, defineComponent, h } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import orderApi from '@/api/order'
import commentApi from '@/api/comment'
import uploadApi  from '@/api/upload'
import { toast } from '@/api/wx'

const orderId = ref(null)
const order = ref({ items: [] })
const submitting = ref(false)
const form = reactive({
  rating: 5,
  taste: 5, env: 5, service: 5,
  tags: [],
  text: '',
  images: [],
  anonymous: false
})

const tags = ['味道很棒','分量很足','上菜快','环境雅致','服务贴心','性价比高','适合宴请','干净卫生','食材新鲜','下次还来']

const canSubmit = computed(() => form.rating > 0 && (form.text.trim().length >= 5 || form.images.length > 0))

const ratingText = computed(() => ({
  5:'⭐⭐⭐⭐⭐ 非常满意', 4:'⭐⭐⭐⭐ 挺满意',
  3:'⭐⭐⭐ 一般般',    2:'⭐⭐ 不满意',  1:'⭐ 很差'
})[form.rating] || '')

onLoad(q => { orderId.value = q.orderId })
onMounted(async () => {
  // 拉订单信息
  const r = await orderApi.fetchOrderDetail(orderId.value)
  order.value = r?.data?.order || r?.data || r || {}
  // 之前写过评价就读取回来（允许重写）
  try {
    const c = await commentApi.fetchOrderComment(orderId.value)
    if (c?.data?.id) Object.assign(form, c.data, { tags: c.data.tags || [] })
  } catch(e){}
})

function toggleTag(t){
  const i = form.tags.indexOf(t)
  if (i>=0) form.tags.splice(i,1); else form.tags.push(t)
}

async function pickImages(){
  const left = 9 - form.images.length
  if (left <= 0) return
  try {
    const list = await uploadApi.chooseAndUpload({ count: left })
    form.images.push(...list)
  } catch(e){}
}
function preview(i){
  uni.previewImage({ urls: form.images, current: form.images[i] })
}

async function submit(){
  if (!canSubmit.value) {
    return toast('请给出评分并填写至少5字评价或上传图片')
  }
  submitting.value = true
  try {
    await commentApi.submitComment({
      orderId: orderId.value,
      ...form
    })
    uni.showModal({
      title: '评价提交成功',
      content: '感谢您的宝贵意见，期待下次光临～',
      showCancel: false,
      success: () => uni.navigateBack()
    })
  } catch(e){ toast(e?.message || '提交失败，请稍后再试')
  } finally { submitting.value = false }
}

// 本地星星组件（避免外部依赖，H5/小程序通用）
const stars = defineComponent({
  name: 'Stars',
  props: { modelValue: {type:Number,default:0}, size: {type:String,default:'md'} },
  emits: ['update:modelValue'],
  setup(p, {emit}) {
    const click = (i) => emit('update:modelValue', i)
    return () => {
      const sizeCls = p.size === 'lg' ? {width:'48rpx', height:'48rpx', fontSize:'44rpx', marginRight:'12rpx'} : {width:'36rpx', height:'36rpx', fontSize:'32rpx', marginRight:'6rpx'}
      const arr = [1,2,3,4,5].map(i =>
        h('text', {
          key:i,
          onClick: ()=> click(i),
          style: {
            ...sizeCls,
            color: i <= p.modelValue ? '#F5A623' : '#DDD',
            display:'inline-block', lineHeight: sizeCls.height
          }
        }, '★')
      )
      return h('view', {style:{display:'inline-flex'}}, arr)
    }
  }
})
</script>

<style lang="scss">
@import '@/uni.scss';
.comment-page{background:$page-bg; min-height:100vh; padding-bottom:160rpx}
.card{@include card; margin:20rpx}
.title{font-size:30rpx; font-weight:600; color:$ink-black; margin-bottom:20rpx;
  &.sm{margin-top:30rpx; margin-bottom:16rpx}
}
.rows{.row{@include row-between; padding:8rpx 0; color:$text-muted; font-size:26rpx;
  .price{color:$brand-gold; font-weight:600}
}}
.items{display:flex; gap:12rpx; margin-top:16rpx; padding-top:16rpx; border-top:2rpx dashed #eee;
  .item{display:flex; flex-direction:column; align-items:center;
    .t{width:96rpx; height:96rpx; border-radius:10rpx; background:#eee}
    .n{font-size:22rpx; color:$ink-black; margin-top:8rpx; max-width:120rpx; text-align:center}
  }
}
.rating-block .line{@include row-between; padding:16rpx 0; border-top:2rpx dashed #eee; &:first-child{border-top:0}
  .label{font-size:28rpx; color:$ink-black}
  .tag-r{font-size:24rpx; color:$brand-gold; margin-left:16rpx}
}
.chips{@include row-wrap}
.chip{@include chip;}
.textarea{width:100%; min-height:220rpx; background:#fafafa; border-radius:12rpx; padding:20rpx; font-size:28rpx; box-sizing:border-box}
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
.gap{height:40rpx}
.footer{@include footer-bar;
  .left{@include row-center;
    .chk{width:36rpx; height:36rpx; border:2rpx solid #ccc; border-radius:6rpx; margin-right:10rpx;
      &.on{background:$brand-gold; border-color:$brand-gold; position:relative;
        &:after{content:'✓'; position:absolute; left:6rpx; top:0; color:#fff; font-size:26rpx; line-height:32rpx}
      }
    }
    .sm{font-size:24rpx}
  }
}
</style>
