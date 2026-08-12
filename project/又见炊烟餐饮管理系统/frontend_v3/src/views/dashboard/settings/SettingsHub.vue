<template>
  <div class="settings-hub">
    <aside class="settings-nav">
      <div class="nav-header">
        <h3 class="nav-title">系统设置</h3>
        <p class="nav-subtitle">System Settings</p>
      </div>
      <div class="nav-menu">
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: $route.path === item.path }"
        >
          <span class="nav-icon">{{ item.icon }}</span>
          <span class="nav-text">{{ item.label }}</span>
          <span v-if="$route.path === item.path" class="nav-indicator"></span>
        </router-link>
      </div>
    </aside>
    <main class="settings-content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script setup>
const menuItems = [
  { path: '/dashboard/settings/info', icon: '🖥️', label: '系统信息与运行状态' },
  { path: '/dashboard/settings/permission', icon: '🔐', label: '权限管理' },
  { path: '/dashboard/settings/org', icon: '🏪', label: '门店与组织' },
  { path: '/dashboard/settings/config', icon: '⚙️', label: '系统配置' },
  { path: '/dashboard/settings/help', icon: '📖', label: '帮助与日志' },
  { path: '/dashboard/settings/checkup', icon: '🩺', label: '系统体检' }
]
</script>

<style scoped>
.settings-hub {
  display: flex;
  gap: 24px;
  height: calc(100vh - 164px);
  max-width: 1600px;
  margin: 0 auto;
}

.settings-nav {
  width: 220px;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px 0;
  flex-shrink: 0;
  overflow-y: auto;
}

.nav-header {
  padding: 0 20px 16px;
  border-bottom: 1px solid var(--color-border-light);
  margin-bottom: 12px;
}

.nav-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text);
  margin: 0 0 2px 0;
}

.nav-subtitle {
  font-size: 11px;
  color: var(--color-text-muted);
  margin: 0;
  letter-spacing: 0.5px;
}

.nav-menu {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 0 12px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: var(--radius-md);
  text-decoration: none;
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s;
  position: relative;
}

.nav-item:hover {
  background: var(--color-bg-alt);
  color: var(--color-text);
}

.nav-item.active {
  background: rgba(45, 74, 62, 0.08);
  color: var(--color-primary);
  font-weight: 600;
}

.nav-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 24px;
  background: var(--color-primary);
  border-radius: 0 2px 2px 0;
}

.nav-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.nav-text {
  flex: 1;
}

.nav-indicator {
  width: 6px;
  height: 6px;
  background: var(--color-primary);
  border-radius: 50%;
}

.settings-content {
  flex: 1;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 28px;
  overflow-y: auto;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 1024px) {
  .settings-hub {
    flex-direction: column;
    height: auto;
  }
  
  .settings-nav {
    width: 100%;
    height: auto;
  }
  
  .nav-menu {
    flex-direction: row;
    flex-wrap: wrap;
    gap: 8px;
  }
  
  .nav-item {
    flex: 1;
    min-width: 140px;
    justify-content: center;
  }
  
  .nav-item.active::before {
    left: 50%;
    top: auto;
    bottom: 0;
    transform: translateX(-50%);
    width: 24px;
    height: 3px;
    border-radius: 2px 2px 0 0;
  }
  
  .settings-content {
    min-height: 500px;
  }
}
</style>
