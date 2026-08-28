<template>
  <view class="webview-page">
    <!-- 兼容：H5 用 iframe，小程序用 uni 内置 web-view -->
    <!-- #ifdef H5 -->
    <iframe
      class="frame"
      :src="url"
      frameborder="0"
      @load="loaded = true"/>
    <!-- #endif -->
    <!-- #ifdef MP-WEIXIN || APP-PLUS -->
    <web-view :src="url" @load="loaded = true" @error="onError"></web-view>
    <!-- #endif -->
    <view v-if="!loaded" class="loading muted">加载中…</view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

const url = ref('')
const loaded = ref(false)

onLoad((q = {}) => {
  try {
    url.value = decodeURIComponent(q.url || '')
  } catch(e) {
    url.value = q.url || ''
  }
  // 设置导航栏标题
  if (q.title) {
    try { uni.setNavigationBarTitle({ title: decodeURIComponent(q.title) }) } catch(_) {}
  } else {
    uni.setNavigationBarTitle({ title: '详情' })
  }
})
function onError(e){
  uni.showToast({ title: '页面加载失败', icon:'none' })
}
</script>

<style lang="scss">
@import '@/uni.scss';
.webview-page{width:100vw; min-height:100vh; background:#fff}
.frame{width:100%; height:calc(100vh - var(--status-bar-height, 44px)); border:0}
.loading{position:fixed; inset:0; @include column; align-items:center; justify-content:center; background:#fff}
</style>
