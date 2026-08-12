<template>
  <div class="welcome">
    <div class="welcome-bg"></div>
    <div class="welcome-content">
      <!-- Logo -->
      <div class="welcome-logo">
        <svg width="72" height="72" viewBox="0 0 72 72" fill="none">
          <circle cx="36" cy="38" r="22" stroke="#D4AF37" stroke-width="1.5"/>
          <!-- 炊具/锅 -->
          <path d="M22 42c0-4 6-8 14-8s14 4 14 8" stroke="#D4AF37" stroke-width="1.5" stroke-linecap="round"/>
          <path d="M20 42h32" stroke="#D4AF37" stroke-width="1.5" stroke-linecap="round"/>
          <!-- 蒸汽 -->
          <path d="M30 34c0-3 2-5 2-8" stroke="#D4AF37" stroke-width="1.2" stroke-linecap="round" opacity="0.8"/>
          <path d="M36 32c0-3 2-5 2-8" stroke="#D4AF37" stroke-width="1.2" stroke-linecap="round" opacity="0.9"/>
          <path d="M42 34c0-3 2-5 2-8" stroke="#D4AF37" stroke-width="1.2" stroke-linecap="round" opacity="0.7"/>
          <!-- 底座 -->
          <path d="M28 48h16" stroke="#D4AF37" stroke-width="1.5" stroke-linecap="round"/>
          <path d="M30 48v3M42 48v3" stroke="#D4AF37" stroke-width="1.2" stroke-linecap="round"/>
        </svg>
      </div>
      
      <!-- 标题 -->
      <h1 class="welcome-title">又见炊烟</h1>
      <p class="welcome-subtitle">私厨 · 宴会 · 臻选</p>
      <div class="welcome-divider"></div>
      <p class="welcome-slogan">人间烟火味 · 最抚凡人心</p>

      <!-- 入口卡片 -->
      <div class="welcome-cards">
        <div class="welcome-card" @click="goTo('/ipad/home')">
          <div class="card-icon-wrap" style="background: rgba(180, 160, 100, 0.15);">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#D4AF37" stroke-width="2" stroke-linecap="round">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
              <circle cx="9" cy="7" r="4"/>
              <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
              <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
            </svg>
          </div>
          <h3>客户点菜</h3>
          <p>自助扫码 · 浏览菜单 · 一键下单</p>
          <span class="card-arrow">→</span>
        </div>

        <div class="welcome-card" @click="goTo('/dashboard/home')">
          <div class="card-icon-wrap" style="background: rgba(100, 160, 140, 0.15);">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#6BA58C" stroke-width="2" stroke-linecap="round">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10"/>
            </svg>
          </div>
          <h3>后台管理</h3>
          <p>门店管理 · 数据报表 · 系统配置</p>
          <span class="card-arrow">→</span>
        </div>
      </div>
    </div>

    <!-- 底部 -->
    <div class="welcome-footer">
      <p>
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#7A8C85" stroke-width="2" style="vertical-align: middle; margin-right: 2px;">
          <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10"/>
        </svg>
        苏公网安备32132302010492号
      </p>
      <p>© 2026 又见炊烟餐饮管理系统</p>
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
  background: linear-gradient(180deg, #1E352B 0%, #233D32 40%, #1A3028 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.welcome-bg {
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse at 50% 30%, rgba(212, 175, 55, 0.04) 0%, transparent 70%);
  pointer-events: none;
}

.welcome-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  z-index: 1;
  padding: 40px 20px;
}

.welcome-logo {
  margin-bottom: 24px;
  opacity: 0.9;
}

.welcome-title {
  font-size: 38px;
  font-weight: 700;
  color: #FFFFFF;
  letter-spacing: 6px;
  margin: 0 0 10px 0;
  font-family: 'STSong', 'SimSun', 'Noto Serif SC', serif;
}

.welcome-subtitle {
  font-size: 13px;
  color: #BFA878;
  letter-spacing: 6px;
  margin: 0 0 12px 0;
  font-weight: 400;
}

.welcome-divider {
  width: 60px;
  height: 1px;
  background: linear-gradient(90deg, transparent, #D4AF37, transparent);
  margin: 0 0 12px 0;
}

.welcome-slogan {
  font-size: 12px;
  color: #8A9A92;
  letter-spacing: 2px;
  margin: 0 0 48px 0;
}

.welcome-cards {
  display: flex;
  gap: 24px;
}

.welcome-card {
  width: 240px;
  height: 160px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  padding: 24px;
  position: relative;
  cursor: pointer;
  transition: all 0.35s ease;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
}

.welcome-card:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(212, 175, 55, 0.3);
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.3);
}

.card-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}

.welcome-card h3 {
  font-size: 16px;
  font-weight: 700;
  color: #FFFFFF;
  margin: 0 0 8px 0;
}

.welcome-card p {
  font-size: 12px;
  color: #7A8C85;
  margin: 0;
  line-height: 1.6;
}

.card-arrow {
  position: absolute;
  bottom: 20px;
  left: 24px;
  color: #D4AF37;
  font-size: 18px;
  transition: all 0.3s;
}

.welcome-card:hover .card-arrow {
  transform: translateX(6px);
}

.welcome-footer {
  position: absolute;
  bottom: 24px;
  text-align: center;
  z-index: 1;
}

.welcome-footer p {
  font-size: 11px;
  color: #5A6C64;
  margin: 2px 0;
}

@media (max-width: 560px) {
  .welcome-cards {
    flex-direction: column;
  }
  
  .welcome-card {
    width: 260px;
    height: 140px;
  }
}
</style>
