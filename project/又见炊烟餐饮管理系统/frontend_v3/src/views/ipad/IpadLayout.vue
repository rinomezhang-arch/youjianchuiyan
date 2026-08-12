<template>
  <div class="ipad-layout">
    <router-view />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useIpadStore } from '@/store/ipad'

const router = useRouter()
const route = useRoute()
const ipad = useIpadStore()

onMounted(() => {
  // iPad 端 auth 检查：需要登录的页面，未登录跳登录
  if (route.meta.requiresAuth && !ipad.isLoggedIn) {
    router.push('/ipad/login')
  }
})
</script>

<style scoped>
.ipad-layout {
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background: var(--color-bg);
  font-family: var(--font-family);
}
</style>

<style>
/* iPad 全局：隐藏滚动条、触控优化 */
.ipad-layout * {
  -webkit-tap-highlight-color: transparent;
  -webkit-touch-callout: none;
}
.ipad-layout ::-webkit-scrollbar {
  width: 4px;
  height: 4px;
}
.ipad-layout ::-webkit-scrollbar-thumb {
  background: rgba(196, 163, 90, 0.3);
  border-radius: 2px;
}
</style>
