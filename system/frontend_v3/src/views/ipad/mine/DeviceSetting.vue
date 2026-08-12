<template>
  <div class="ipad-page">
    <div class="page-top">
      <button class="back-link" @click="router.push('/ipad/home')">← 返回桌台</button>
      <h1 class="page-title">设备设置 · Settings</h1>
    </div>
    <div class="settings-content">
      <div class="setting-group">
        <h3>设备信息 · Device</h3>
        <div class="setting-row"><span>设备SN</span><span class="setting-value">{{ ipad.deviceSn }}</span></div>
        <div class="setting-row"><span>门店</span><span class="setting-value">{{ ipad.storeName }}</span></div>
        <div class="setting-row"><span>员工</span><span class="setting-value">{{ ipad.staffName }}</span></div>
      </div>
      <div class="setting-group">
        <h3>打印配置 · Printer</h3>
        <div class="setting-row"><span>打印端口</span><span class="setting-value">{{ ipad.printConfig.print_port }}</span></div>
        <div class="setting-row"><span>打印宽度</span><span class="setting-value">{{ ipad.printConfig.print_width }}mm</span></div>
        <div class="setting-row"><span>模板</span><span class="setting-value">{{ ipad.printConfig.print_template_code }}</span></div>
      </div>
      <div class="setting-group danger">
        <button class="logout-btn" @click="handleLogout">退出登录 · Logout</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useIpadStore } from '@/store/ipad'

const router = useRouter()
const ipad = useIpadStore()

function handleLogout() {
  ipad.logout()
  router.push('/ipad/login')
}
</script>

<style scoped>
.ipad-page { width: 100%; height: 100%; display: flex; flex-direction: column; background: var(--color-bg); }
.page-top { padding: 16px 24px; background: var(--color-card); border-bottom: 1px solid var(--color-border); display: flex; align-items: center; gap: 16px; flex-shrink: 0; }
.back-link { border: none; background: none; color: var(--color-text-muted); font-size: 13px; cursor: pointer; }
.page-title { font-size: 18px; font-weight: 700; color: var(--color-text); letter-spacing: 2px; }
.settings-content { flex: 1; overflow-y: auto; padding: 24px; max-width: 500px; margin: 0 auto; width: 100%; }
.setting-group { background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-lg); padding: 20px; margin-bottom: 16px; }
.setting-group h3 { font-size: 16px; font-weight: 700; color: var(--color-text); margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px solid var(--color-border-light); }
.setting-row { display: flex; justify-content: space-between; padding: 8px 0; font-size: 14px; color: var(--color-text-secondary); }
.setting-value { font-weight: 600; color: var(--color-text); }
.danger { border-color: rgba(194,85,85,0.2); }
.logout-btn { width: 100%; padding: 14px; border: 1px solid var(--color-danger); border-radius: var(--radius-md); background: transparent; color: var(--color-danger); font-size: 15px; font-weight: 600; cursor: pointer; }
.logout-btn:hover { background: var(--color-danger); color: white; }
</style>
