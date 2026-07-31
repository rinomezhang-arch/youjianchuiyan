<template>
  <div class="settings-page">
    <div class="page-header">
      <h2 class="page-title">系统设置总览</h2>
      <p class="page-subtitle">System Settings & Configuration</p>
    </div>

    <div class="settings-grid">
      <div class="setting-card">
        <div class="card-header">
          <div class="card-icon" style="background: rgba(45,74,62,0.06)">
            <svg viewBox="0 0 24 24" fill="none" stroke="#2D4A3E" stroke-width="2">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
              <circle cx="9" cy="7" r="4"/>
              <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
              <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
            </svg>
          </div>
          <div class="card-title-area">
            <h3 class="card-title">基础配置</h3>
            <p class="card-desc">门店信息与基础数据维护</p>
          </div>
        </div>
        <div class="card-content">
          <div class="config-grid">
            <div class="config-item" @click="goTo('settings')">
              <div class="config-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="#4A7C59" stroke-width="2">
                  <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                  <circle cx="9" cy="7" r="4"/>
                </svg>
              </div>
              <div class="config-info">
                <div class="config-name">门店信息</div>
                <div class="config-status enabled">已启用</div>
              </div>
            </div>
            <div class="config-item" @click="goTo('settings')">
              <div class="config-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="#4A7C59" stroke-width="2">
                  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10"/>
                  <path d="M9 12l2 2 4-4"/>
                </svg>
              </div>
              <div class="config-info">
                <div class="config-name">菜品分类</div>
                <div class="config-status enabled">已启用</div>
              </div>
            </div>
            <div class="config-item" @click="goTo('settings')">
              <div class="config-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="#4A7C59" stroke-width="2">
                  <rect x="3" y="3" width="18" height="18" rx="2"/>
                  <path d="M9 9h6"/>
                  <path d="M9 15h6"/>
                </svg>
              </div>
              <div class="config-info">
                <div class="config-name">单位规格</div>
                <div class="config-status enabled">已启用</div>
              </div>
            </div>
            <div class="config-item" @click="goTo('settings')">
              <div class="config-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="#4A7C59" stroke-width="2">
                  <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/>
                  <polyline points="3.27 6.96 12 12.01 20.73 6.96"/>
                </svg>
              </div>
              <div class="config-info">
                <div class="config-name">合作商家</div>
                <div class="config-status enabled">已启用</div>
              </div>
            </div>
            <div class="config-item" @click="goTo('settings')">
              <div class="config-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="#4A7C59" stroke-width="2">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                  <polyline points="7 10 12 15 17 10"/>
                  <line x1="12" y1="15" x2="12" y2="3"/>
                </svg>
              </div>
              <div class="config-info">
                <div class="config-name">仓储档案</div>
                <div class="config-status enabled">已启用</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="setting-card">
        <div class="card-header">
          <div class="card-icon" style="background: rgba(74,124,89,0.06)">
            <svg viewBox="0 0 24 24" fill="none" stroke="#4A7C59" stroke-width="2">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10"/>
              <path d="M9 12l2 2 4-4"/>
            </svg>
          </div>
          <div class="card-title-area">
            <h3 class="card-title">权限配置</h3>
            <p class="card-desc">角色管理与权限分配</p>
          </div>
        </div>
        <div class="card-content">
          <div class="role-section">
            <h4 class="section-subtitle">角色列表</h4>
            <div class="role-list">
              <div class="role-item" v-for="role in roles" :key="role.name">
                <div class="role-icon" :style="{ background: role.bgColor }">
                  <svg viewBox="0 0 24 24" fill="none" :stroke="role.color" stroke-width="2">
                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                    <circle cx="9" cy="7" r="4"/>
                  </svg>
                </div>
                <div class="role-info">
                  <div class="role-name">{{ role.name }}</div>
                  <div class="role-count">{{ role.count }} 人</div>
                </div>
                <div class="role-actions">
                  <button class="action-btn">编辑</button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="setting-card">
        <div class="card-header">
          <div class="card-icon" style="background: rgba(196,163,90,0.06)">
            <svg viewBox="0 0 24 24" fill="none" stroke="#C4A35A" stroke-width="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <polyline points="14 2 14 8 20 8"/>
              <line x1="16" y1="13" x2="8" y2="13"/>
              <line x1="16" y1="17" x2="8" y2="17"/>
              <polyline points="10 9 9 9 8 9"/>
            </svg>
          </div>
          <div class="card-title-area">
            <h3 class="card-title">打印配置</h3>
            <p class="card-desc">打印模板与打印机设置</p>
          </div>
        </div>
        <div class="card-content">
          <div class="print-section">
            <div class="print-item">
              <div class="print-header">
                <div class="print-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="#2D4A3E" stroke-width="2">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                    <polyline points="14 2 14 8 20 8"/>
                  </svg>
                </div>
                <div class="print-info">
                  <div class="print-name">前厅小票</div>
                  <div class="print-status online">已启用</div>
                </div>
              </div>
              <div class="print-detail">默认打印机：EPSON TM-T88V</div>
            </div>
            <div class="print-item">
              <div class="print-header">
                <div class="print-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="#4A7C59" stroke-width="2">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                    <polyline points="14 2 14 8 20 8"/>
                  </svg>
                </div>
                <div class="print-info">
                  <div class="print-name">后厨分单</div>
                  <div class="print-status online">已启用</div>
                </div>
              </div>
              <div class="print-detail">默认打印机：EPSON TM-T88V (后厨)</div>
            </div>
            <div class="print-item">
              <div class="print-header">
                <div class="print-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="#C4A35A" stroke-width="2">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                    <polyline points="14 2 14 8 20 8"/>
                  </svg>
                </div>
                <div class="print-info">
                  <div class="print-name">结账单打印</div>
                  <div class="print-status online">已启用</div>
                </div>
              </div>
              <div class="print-detail">默认打印机：EPSON TM-T88V</div>
            </div>
          </div>
        </div>
      </div>

      <div class="setting-card">
        <div class="card-header">
          <div class="card-icon" style="background: rgba(91,123,138,0.06)">
            <svg viewBox="0 0 24 24" fill="none" stroke="#5B7B8A" stroke-width="2">
              <rect x="3" y="3" width="18" height="18" rx="2"/>
              <circle cx="8.5" cy="8.5" r="1.5"/>
              <circle cx="15.5" cy="8.5" r="1.5"/>
              <path d="M8 15h8"/>
            </svg>
          </div>
          <div class="card-title-area">
            <h3 class="card-title">硬件对接</h3>
            <p class="card-desc">设备管理与接口配置</p>
          </div>
        </div>
        <div class="card-content">
          <div class="hardware-grid">
            <div class="hardware-item" v-for="device in devices" :key="device.name">
              <div class="hardware-icon" :class="device.status">
                <svg viewBox="0 0 24 24" fill="none" :stroke="device.color" stroke-width="2">
                  <rect x="3" y="3" width="18" height="18" rx="2"/>
                </svg>
              </div>
              <div class="hardware-info">
                <div class="hardware-name">{{ device.name }}</div>
                <div class="hardware-status" :class="device.status">
                  {{ device.statusText }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="setting-card full-width">
        <div class="card-header">
          <div class="card-icon" style="background: rgba(45,74,62,0.06)">
            <svg viewBox="0 0 24 24" fill="none" stroke="#2D4A3E" stroke-width="2">
              <path d="M21 12a9 9 0 0 0-9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/>
              <path d="M3 3v5h5"/>
              <path d="M3 12a9 9 0 0 0 9 9 9.75 9.75 0 0 0 6.74-2.74L21 16"/>
              <path d="M16 21h5v-5"/>
            </svg>
          </div>
          <div class="card-title-area">
            <h3 class="card-title">系统管理</h3>
            <p class="card-desc">数据维护与系统参数</p>
          </div>
        </div>
        <div class="card-content">
          <div class="system-grid">
            <div class="system-item">
              <div class="system-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="#2D4A3E" stroke-width="2">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                  <polyline points="7 10 12 15 17 10"/>
                  <line x1="12" y1="15" x2="12" y2="3"/>
                </svg>
              </div>
              <div class="system-info">
                <div class="system-name">数据备份</div>
                <div class="system-detail">自动备份：每日 02:00</div>
                <div class="system-action">
                  <button class="btn-small">立即备份</button>
                </div>
              </div>
            </div>
            <div class="system-item">
              <div class="system-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="#4A7C59" stroke-width="2">
                  <circle cx="12" cy="12" r="10"/>
                  <polyline points="12 6 12 12 16 14"/>
                </svg>
              </div>
              <div class="system-info">
                <div class="system-name">日志查询</div>
                <div class="system-detail">最近7天日志：1,256 条</div>
                <div class="system-action">
                  <button class="btn-small">查看日志</button>
                </div>
              </div>
            </div>
            <div class="system-item">
              <div class="system-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="#C4A35A" stroke-width="2">
                  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10"/>
                  <path d="M9 9l2 2 4-4"/>
                  <path d="M9 15l2 2 4-4"/>
                </svg>
              </div>
              <div class="system-info">
                <div class="system-name">系统参数</div>
                <div class="system-detail">营业时间、节假日配置</div>
                <div class="system-action">
                  <button class="btn-small">编辑参数</button>
                </div>
              </div>
            </div>
            <div class="system-item">
              <div class="system-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="#5B7B8A" stroke-width="2">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                  <polyline points="7 10 12 15 17 10"/>
                  <line x1="12" y1="15" x2="12" y2="3"/>
                </svg>
              </div>
              <div class="system-info">
                <div class="system-name">系统更新</div>
                <div class="system-detail">当前版本：v3.2.1</div>
                <div class="system-action">
                  <button class="btn-small">检查更新</button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 帮助文件 -->
      <div class="setting-card">
        <div class="card-header">
          <div class="card-icon" style="background: rgba(91,123,138,0.06)">
            <svg viewBox="0 0 24 24" fill="none" stroke="#5B7B8A" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/>
              <line x1="12" y1="17" x2="12.01" y2="17"/>
            </svg>
          </div>
          <div class="card-title-area">
            <h3 class="card-title">帮助文件</h3>
            <p class="card-desc">使用手册 · 开发文档 · 更新日志</p>
          </div>
        </div>
        <div class="card-content">
          <div class="config-grid">
            <div class="config-item" @click="goTo('help/dev-process')">
              <div class="config-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="#2D4A3E" stroke-width="2">
                  <polyline points="16 18 22 12 16 6"/>
                  <polyline points="8 6 2 12 8 18"/>
                </svg>
              </div>
              <div class="config-info">
                <div class="config-name">开发过程</div>
                <div class="config-status enabled">项目历程</div>
              </div>
            </div>
            <div class="config-item" @click="goTo('help')">
              <div class="config-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="#4A7C59" stroke-width="2">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                  <polyline points="14 2 14 8 20 8"/>
                </svg>
              </div>
              <div class="config-info">
                <div class="config-name">使用手册</div>
                <div class="config-status">操作指南</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const roles = [
  { name: '超级管理员', count: 1, color: '#2D4A3E', bgColor: 'rgba(45,74,62,0.06)' },
  { name: '店长', count: 2, color: '#4A7C59', bgColor: 'rgba(74,124,89,0.06)' },
  { name: '前厅主管', count: 3, color: '#C4A35A', bgColor: 'rgba(196,163,90,0.06)' },
  { name: '后厨主管', count: 2, color: '#5B7B8A', bgColor: 'rgba(91,123,138,0.06)' },
  { name: '收银员', count: 4, color: '#8B9A8C', bgColor: 'rgba(139,154,140,0.06)' }
]

const devices = [
  { name: '收银机', status: 'online', statusText: '在线', color: '#2D4A3E' },
  { name: '扫码枪', status: 'online', statusText: '在线', color: '#4A7C59' },
  { name: '后厨打印机', status: 'online', statusText: '在线', color: '#C4A35A' },
  { name: '扫码桌牌', status: 'partial', statusText: '部分在线', color: '#C4A35A' }
]

function goTo(path) {
  router.push(`/dashboard/${path}`)
}
</script>

<style scoped>
.settings-page {
  max-width: 1400px;
  margin: 0 auto;
}

.settings-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.setting-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.setting-card.full-width {
  grid-column: span 2;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border-light);
  background: var(--color-bg-alt);
}

.card-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-icon svg {
  width: 22px;
  height: 22px;
}

.card-title-area {
  flex: 1;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 2px;
}

.card-desc {
  font-size: 12px;
  color: var(--color-text-muted);
}

.card-content {
  padding: 16px 20px;
}

.config-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.config-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  background: var(--color-bg-alt);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: var(--transition);
}

.config-item:hover {
  background: var(--color-bg-side);
}

.config-icon {
  width: 36px;
  height: 36px;
  opacity: 0.6;
}

.config-info {
  flex: 1;
}

.config-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text);
  margin-bottom: 2px;
}

.config-detail {
  font-size: 11px;
  color: var(--color-text-muted);
}

.section-subtitle {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text);
  margin-bottom: 12px;
}

.role-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.role-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  background: var(--color-bg-alt);
  border-radius: var(--radius-sm);
}

.role-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.role-icon svg {
  width: 18px;
  height: 18px;
}

.role-info {
  flex: 1;
}

.role-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text);
  margin-bottom: 2px;
}

.role-count {
  font-size: 11px;
  color: var(--color-text-muted);
}

.role-actions .action-btn {
  padding: 4px 12px;
  font-size: 12px;
  color: var(--color-primary);
  background: rgba(45, 74, 62, 0.06);
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.print-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.print-item {
  padding: 14px;
  background: var(--color-bg-alt);
  border-radius: var(--radius-sm);
}

.print-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.print-icon {
  width: 32px;
  height: 32px;
  opacity: 0.6;
}

.print-info {
  flex: 1;
  margin-left: 10px;
}

.print-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text);
}

.print-status {
  font-size: 12px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 4px;
}

.print-status.online {
  color: #4A7C59;
  background: rgba(74, 124, 89, 0.08);
}

.print-detail {
  font-size: 11px;
  color: var(--color-text-muted);
}

.hardware-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.hardware-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px;
  background: var(--color-bg-alt);
  border-radius: var(--radius-sm);
}

.hardware-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
}

.hardware-icon.online {
  background: rgba(74, 124, 89, 0.08);
}

.hardware-icon.partial {
  background: rgba(196, 163, 90, 0.08);
}

.hardware-icon svg {
  width: 18px;
  height: 18px;
}

.hardware-info {
  text-align: center;
}

.hardware-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text);
  margin-bottom: 4px;
}

.hardware-status {
  font-size: 11px;
  font-weight: 500;
}

.hardware-status.online {
  color: #4A7C59;
}

.hardware-status.partial {
  color: #C4A35A;
}

.system-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.system-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  background: var(--color-bg-alt);
  border-radius: var(--radius-md);
}

.system-icon {
  width: 48px;
  height: 48px;
  opacity: 0.6;
  margin-bottom: 12px;
}

.system-info {
  text-align: center;
}

.system-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 4px;
}

.system-detail {
  font-size: 11px;
  color: var(--color-text-muted);
  margin-bottom: 12px;
}

.system-action .btn-small {
  padding: 6px 16px;
  font-size: 12px;
  color: var(--color-primary);
  background: rgba(45, 74, 62, 0.06);
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: var(--transition);
}

.system-action .btn-small:hover {
  background: rgba(45, 74, 62, 0.12);
}

@media (max-width: 1200px) {
  .settings-grid {
    grid-template-columns: 1fr;
  }
  .setting-card.full-width {
    grid-column: span 1;
  }
  .system-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .config-grid {
    grid-template-columns: 1fr;
  }
  .hardware-grid {
    grid-template-columns: 1fr;
  }
  .system-grid {
    grid-template-columns: 1fr;
  }
}
</style>