<template>
  <div class="welcome">
    <div class="bg-gradient"></div>
    <div class="bg-pattern"></div>

    <div class="content">
      <div class="logo-wrap">
        <img src="@/assets/images/logo.png" alt="又见炊烟" />
      </div>

      <h1 class="title">又见炊烟</h1>
      <p class="subtitle">私厨 · 宴会 · 臻选</p>

      <div class="actions">
        <div class="action-card" @click="goTo('/ipad/home')">
          <div class="action-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
              <circle cx="9" cy="7" r="4"/>
              <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
              <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
            </svg>
          </div>
          <div class="action-text">
            <h3>客户点菜</h3>
            <p>扫码 · 浏览 · 下单</p>
          </div>
          <span class="action-arrow">→</span>
        </div>

        <div class="action-card" @click="goTo('/login')">
          <div class="action-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10"/>
            </svg>
          </div>
          <div class="action-text">
            <h3>后台管理</h3>
            <p>门店 · 数据 · 配置</p>
          </div>
          <span class="action-arrow">→</span>
        </div>
      </div>
    </div>

    <div class="footer">
      <p>© 2026 又见炊烟</p>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

function goTo(path) {
  router.push(path)
}
</script>

<style scoped>
.welcome {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: linear-gradient(160deg, #2D4A3E 0%, #1D3A2E 40%, #0F2419 100%);
}

.bg-gradient {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 20% 20%, rgba(196, 163, 90, 0.25) 0%, transparent 45%),
    radial-gradient(ellipse at 80% 80%, rgba(196, 163, 90, 0.15) 0%, transparent 40%),
    radial-gradient(ellipse at 50% 50%, rgba(61, 90, 78, 0.3) 0%, transparent 60%);
}

.bg-pattern {
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(rgba(196, 163, 90, 0.08) 1px, transparent 1px);
  background-size: 24px 24px;
}

.content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px;
}

.logo-wrap {
  margin-bottom: 28px;
}

.logo-wrap img {
  width: 80px;
  height: 80px;
  object-fit: contain;
  filter: drop-shadow(0 4px 20px rgba(196, 163, 90, 0.3));
}

.title {
  font-size: 40px;
  font-weight: 600;
  color: #FAF8F5;
  letter-spacing: 6px;
  margin: 0 0 8px 0;
}

.subtitle {
  font-size: 13px;
  color: rgba(196, 163, 90, 0.8);
  letter-spacing: 4px;
  margin: 0 0 56px 0;
}

.actions {
  display: flex;
  gap: 20px;
}

.action-card {
  width: 240px;
  padding: 28px 24px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(196, 163, 90, 0.25);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
}

.action-card:hover {
  background: rgba(196, 163, 90, 0.1);
  border-color: rgba(196, 163, 90, 0.5);
  transform: translateY(-2px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.action-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: rgba(196, 163, 90, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #C4A35A;
  flex-shrink: 0;
}

.action-text h3 {
  font-size: 15px;
  font-weight: 600;
  color: #FAF8F5;
  margin: 0 0 4px 0;
  letter-spacing: 1px;
}

.action-text p {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.55);
  margin: 0;
  letter-spacing: 0.5px;
}

.action-arrow {
  margin-left: auto;
  color: rgba(255, 255, 255, 0.35);
  font-size: 16px;
  transition: all 0.3s ease;
}

.action-card:hover .action-arrow {
  color: #C4A35A;
  transform: translateX(4px);
}

.footer {
  position: absolute;
  bottom: 28px;
  z-index: 1;
}

.footer p {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.3);
  margin: 0;
  letter-spacing: 1px;
}

@media (max-width: 600px) {
  .actions {
    flex-direction: column;
  }

  .action-card {
    width: 260px;
  }

  .title {
    font-size: 32px;
    letter-spacing: 4px;
  }
}
</style>
