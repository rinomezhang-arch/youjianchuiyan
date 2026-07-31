<template>
  <div class="help-page">
    <div class="page-header">
      <h2 class="page-title">帮助文件</h2>
      <p class="page-subtitle">Help Center · 系统使用与开发文档</p>
    </div>

    <div class="help-grid">
      <div class="help-card" @click="goTo('/dashboard/help/dev-process')">
        <div class="help-icon" style="background: rgba(45,74,62,0.08)">
          <svg viewBox="0 0 24 24" fill="none" stroke="#2D4A3E" stroke-width="2">
            <polyline points="16 18 22 12 16 6"/>
            <polyline points="8 6 2 12 8 18"/>
            <line x1="12" y1="2" x2="12" y2="22"/>
          </svg>
        </div>
        <div class="help-body">
          <h3 class="help-title">开发过程</h3>
          <p class="help-desc">项目开发历程、架构演进、技术决策记录</p>
          <span class="help-arrow">→</span>
        </div>
      </div>

      <div class="help-card" @click="goTo('/dashboard/help/dev-process')">
        <div class="help-icon" style="background: rgba(74,124,89,0.08)">
          <svg viewBox="0 0 24 24" fill="none" stroke="#4A7C59" stroke-width="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <polyline points="14 2 14 8 20 8"/>
            <line x1="16" y1="13" x2="8" y2="13"/>
            <line x1="16" y1="17" x2="8" y2="17"/>
          </svg>
        </div>
        <div class="help-body">
          <h3 class="help-title">使用手册</h3>
          <p class="help-desc">系统操作指南、功能说明、常见问题</p>
          <span class="help-arrow">→</span>
        </div>
      </div>

      <div class="help-card" @click="goTo('/dashboard/help/dev-process')">
        <div class="help-icon" style="background: rgba(196,163,90,0.08)">
          <svg viewBox="0 0 24 24" fill="none" stroke="#C4A35A" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/>
            <line x1="12" y1="17" x2="12.01" y2="17"/>
          </svg>
        </div>
        <div class="help-body">
          <h3 class="help-title">数据库文档</h3>
          <p class="help-desc">数据表结构、字段说明、关联关系</p>
          <span class="help-arrow">→</span>
        </div>
      </div>

      <div class="help-card" @click="goTo('/dashboard/help/dev-process')">
        <div class="help-icon" style="background: rgba(107,140,158,0.08)">
          <svg viewBox="0 0 24 24" fill="none" stroke="#6B8C9E" stroke-width="2">
            <path d="M12 20h9"/>
            <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/>
          </svg>
        </div>
        <div class="help-body">
          <h3 class="help-title">更新日志</h3>
          <p class="help-desc">版本更新记录、功能变更、Bug修复</p>
          <span class="help-arrow">→</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
function goTo(path) { router.push(path) }
</script>

<style scoped>
.help-page { max-width: 1000px; margin: 0 auto; }
.page-header { margin-bottom: 28px; }
.page-title { font-size: 22px; font-weight: 700; color: #1a1a1a; margin-bottom: 4px; }
.page-subtitle { font-size: 13px; color: #8a8a8a; }

.help-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
.help-card {
  background: #fff; border: 1px solid #e8e8e8; border-radius: 10px;
  padding: 24px; display: flex; align-items: flex-start; gap: 16px;
  cursor: pointer; transition: all 0.25s;
}
.help-card:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(0,0,0,0.08); border-color: #2D4A3E; }
.help-icon {
  width: 52px; height: 52px; border-radius: 14px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.help-icon svg { width: 24px; height: 24px; }
.help-body { flex: 1; }
.help-title { font-size: 15px; font-weight: 600; color: #1a1a1a; margin: 0 0 6px 0; }
.help-desc { font-size: 13px; color: #8a8a8a; margin: 0; line-height: 1.5; }
.help-arrow { color: #ccc; font-size: 18px; position: absolute; right: 20px; top: 50%; transform: translateY(-50%); }
.help-card { position: relative; }
.help-card:hover .help-arrow { color: #2D4A3E; transform: translate(4px, -50%); }

@media (max-width: 640px) { .help-grid { grid-template-columns: 1fr; } }
</style>
