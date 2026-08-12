<template>
  <div class="system-checkup-page">
    <div class="page-header">
      <h2 class="page-title">系统体检 · System Checkup</h2>
      <p class="page-subtitle">系统健康检查 · 性能诊断 · 异常排查</p>
    </div>

    <div class="placeholder-content">
      <div class="placeholder-icon">
        <svg width="80" height="80" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1">
          <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
        </svg>
      </div>
      <h3>系统体检功能即将上线</h3>
      <p>该模块正在规划中，将提供系统健康诊断、性能监控和异常排查功能</p>
      <div class="planned-features">
        <div class="feature-item">
          <span class="feature-icon">🔍</span>
          <span>数据库健康检查</span>
        </div>
        <div class="feature-item">
          <span class="feature-icon">⚡</span>
          <span>API响应时间监控</span>
        </div>
        <div class="feature-item">
          <span class="feature-icon">💾</span>
          <span>磁盘空间预警</span>
        </div>
        <div class="feature-item">
          <span class="feature-icon">🔒</span>
          <span>安全漏洞扫描</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.system-checkup-page { max-width: 1400px; margin: 0 auto; }
.page-header { margin-bottom: 32px; }
.page-title { font-size: 22px; font-weight: 700; color: var(--color-text); margin-bottom: 4px; }
.page-subtitle { font-size: 13px; color: var(--color-text-muted); }
.placeholder-content { background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-lg); padding: 60px 40px; text-align: center; }
.placeholder-icon { color: var(--color-border); margin-bottom: 24px; }
.placeholder-content h3 { font-size: 20px; font-weight: 600; color: var(--color-text); margin-bottom: 8px; }
.placeholder-content p { font-size: 14px; color: var(--color-text-muted); margin-bottom: 32px; }
.planned-features { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; max-width: 400px; margin: 0 auto; }
.feature-item { display: flex; align-items: center; gap: 10px; padding: 14px 20px; background: var(--color-bg-alt); border-radius: var(--radius-md); font-size: 14px; color: var(--color-text-secondary); }
.feature-icon { font-size: 18px; }
</style>
