<template>
  <div class="help-page">
    <div class="page-header">
      <h2 class="page-title">帮助与日志</h2>
      <p class="page-subtitle">Help Center · Commands, Knowledge & Logs</p>
    </div>

    <el-tabs v-model="activeTab" class="help-tabs">
      <!-- 命令参考 -->
      <el-tab-pane label="命令参考" name="commands">
        <div class="tab-content">
          <div class="command-section">
            <h3 class="section-title">数据库命令</h3>
            <div class="command-list">
              <div v-for="cmd in dbCommands" :key="cmd.name" class="command-item">
                <div class="command-header">
                  <span class="command-name">{{ cmd.name }}</span>
                  <el-tag size="small" :type="cmd.type">{{ cmd.category }}</el-tag>
                </div>
                <div class="command-code">{{ cmd.command }}</div>
                <div class="command-desc">{{ cmd.description }}</div>
              </div>
            </div>
          </div>

          <div class="command-section">
            <h3 class="section-title">服务管理</h3>
            <div class="command-list">
              <div v-for="cmd in serviceCommands" :key="cmd.name" class="command-item">
                <div class="command-header">
                  <span class="command-name">{{ cmd.name }}</span>
                  <el-tag size="small" :type="cmd.type">{{ cmd.category }}</el-tag>
                </div>
                <div class="command-code">{{ cmd.command }}</div>
                <div class="command-desc">{{ cmd.description }}</div>
              </div>
            </div>
          </div>

          <div class="command-section">
            <h3 class="section-title">系统工具</h3>
            <div class="command-list">
              <div v-for="cmd in systemCommands" :key="cmd.name" class="command-item">
                <div class="command-header">
                  <span class="command-name">{{ cmd.name }}</span>
                  <el-tag size="small" :type="cmd.type">{{ cmd.category }}</el-tag>
                </div>
                <div class="command-code">{{ cmd.command }}</div>
                <div class="command-desc">{{ cmd.description }}</div>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- 知识库 -->
      <el-tab-pane label="知识库" name="knowledge">
        <div class="tab-content">
          <el-input
            v-model="faqSearch"
            placeholder="搜索常见问题..."
            prefix-icon="Search"
            clearable
            style="margin-bottom: 20px"
          />
          <div class="faq-list">
            <div v-for="faq in filteredFaqs" :key="faq.q" class="faq-item">
              <div class="faq-question" @click="faq.expanded = !faq.expanded">
                <span class="faq-icon">{{ faq.expanded ? '▼' : '▶' }}</span>
                <span class="faq-text">{{ faq.q }}</span>
              </div>
              <div v-if="faq.expanded" class="faq-answer">
                <div v-html="faq.a"></div>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- 操作日志 -->
      <el-tab-pane label="操作日志" name="logs">
        <div class="tab-content">
          <div class="log-toolbar">
            <el-date-picker
              v-model="logDateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              style="width: 280px"
            />
            <el-select v-model="logType" placeholder="操作类型" style="width: 140px">
              <el-option label="全部" value="" />
              <el-option label="登录" value="login" />
              <el-option label="数据修改" value="edit" />
              <el-option label="删除" value="delete" />
              <el-option label="导出" value="export" />
            </el-select>
            <el-input
              v-model="logSearch"
              placeholder="搜索操作内容..."
              clearable
              style="width: 200px"
            />
          </div>

          <el-table :data="filteredLogs" border stripe style="width: 100%" max-height="500">
            <el-table-column prop="time" label="时间" width="180" />
            <el-table-column prop="user" label="操作人" width="100" />
            <el-table-column prop="type" label="类型" width="100">
              <template #default="{ row }">
                <el-tag :type="getLogType(row.type)" size="small">{{ row.typeLabel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="action" label="操作内容" />
            <el-table-column prop="ip" label="IP地址" width="140" />
          </el-table>

          <div class="log-pagination">
            <el-pagination
              v-model:current-page="logPage"
              :page-size="20"
              :total="logs.length"
              layout="total, prev, pager, next"
            />
          </div>
        </div>
      </el-tab-pane>

      <!-- 开发配置 -->
      <el-tab-pane label="开发配置" name="dev">
        <div class="tab-content">
          <div class="dev-section">
            <h3 class="section-title">开发过程时间线</h3>
            <div class="timeline">
              <div v-for="phase in devTimeline" :key="phase.title" class="timeline-item">
                <div class="timeline-dot"></div>
                <div class="timeline-content">
                  <div class="timeline-header">
                    <h4>{{ phase.title }}</h4>
                    <el-tag size="small" type="info">{{ phase.date }}</el-tag>
                  </div>
                  <div class="timeline-body">
                    <p>{{ phase.description }}</p>
                    <ul v-if="phase.features && phase.features.length">
                      <li v-for="feature in phase.features" :key="feature">{{ feature }}</li>
                    </ul>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="dev-section">
            <h3 class="section-title">技术栈</h3>
            <div class="tech-stack">
              <div v-for="tech in techStack" :key="tech.name" class="tech-item">
                <div class="tech-icon" :style="{ background: tech.bgColor }">
                  <span>{{ tech.emoji }}</span>
                </div>
                <div class="tech-info">
                  <h4>{{ tech.name }}</h4>
                  <p>{{ tech.version }}</p>
                </div>
              </div>
            </div>
          </div>

          <div class="dev-section">
            <h3 class="section-title">项目统计</h3>
            <div class="stats-grid">
              <div class="stat-card">
                <div class="stat-value">{{ projectStats.vueFiles }}</div>
                <div class="stat-label">Vue组件</div>
              </div>
              <div class="stat-card">
                <div class="stat-value">{{ projectStats.javaFiles }}</div>
                <div class="stat-label">Java类</div>
              </div>
              <div class="stat-card">
                <div class="stat-value">{{ projectStats.dbTables }}</div>
                <div class="stat-label">数据库表</div>
              </div>
              <div class="stat-card">
                <div class="stat-value">{{ projectStats.apiEndpoints }}</div>
                <div class="stat-label">API接口</div>
              </div>
              <div class="stat-card">
                <div class="stat-value">{{ projectStats.codeLines }}</div>
                <div class="stat-label">代码行数</div>
              </div>
              <div class="stat-card">
                <div class="stat-value">{{ projectStats.checkupItems }}</div>
                <div class="stat-label">体检检测项</div>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'

const activeTab = ref('commands')

// 命令参考数据
const dbCommands = ref([
  { name: '数据库备份', command: 'mysqldump -u root -p youjianchuiyan > backup_$(date +%Y%m%d).sql', description: '完整备份数据库到SQL文件', type: 'success', category: '备份' },
  { name: '数据库恢复', command: 'mysql -u root -p youjianchuiyan < backup_20260731.sql', description: '从SQL文件恢复数据库', type: 'warning', category: '恢复' },
  { name: '表结构查看', command: 'mysql -u root -p -e "SHOW CREATE TABLE booking_master;"', description: '查看指定表的创建语句', type: '', category: '查询' },
  { name: '数据库优化', command: 'mysqlcheck -u root -p --optimize youjianchuiyan', description: '优化所有表，释放空间', type: 'info', category: '维护' },
])

const serviceCommands = ref([
  { name: '重启后端服务', command: 'systemctl restart youjianchuiyan-backend', description: '重启Spring Boot后端应用', type: 'warning', category: '服务' },
  { name: '重启前端服务', command: 'systemctl restart youjianchuiyan-frontend', description: '重启Vue前端开发服务器', type: 'warning', category: '服务' },
  { name: '重启Nginx', command: 'systemctl reload nginx', description: '重新加载Nginx配置', type: 'info', category: '服务' },
  { name: '查看服务状态', command: 'systemctl status youjianchuiyan-*', description: '查看所有相关服务状态', type: '', category: '监控' },
])

const systemCommands = ref([
  { name: '查看系统日志', command: 'journalctl -u youjianchuiyan-backend -f', description: '实时查看后端服务日志', type: '', category: '日志' },
  { name: '清理旧日志', command: 'find /var/log/youjianchuiyan -name "*.log" -mtime +30 -delete', description: '删除30天前的日志文件', type: 'danger', category: '清理' },
  { name: '查看磁盘使用', command: 'df -h /mnt/cos/', description: '查看COS挂载点磁盘使用情况', type: 'info', category: '监控' },
  { name: '同步到COS', command: 'rsync -avz /opt/youjianchuiyan/ /mnt/cos/backup/', description: '同步项目文件到COS备份', type: 'success', category: '备份' },
])

// 知识库数据
const faqSearch = ref('')
const faqs = reactive([
  { q: '如何添加新用户？', a: '进入 <b>系统设置 → 权限管理 → 用户列表</b>，点击右上角"添加用户"按钮，填写用户信息并分配角色权限。', expanded: false },
  { q: '如何修改营业时间？', a: '进入 <b>系统设置 → 系统配置 → 营业时间配置</b>，使用日期选择器设置午餐和晚餐时段，选择营业日后点击保存。', expanded: false },
  { q: '打印机不出纸怎么办？', a: '1. 检查打印机电源和连接线<br>2. 检查打印机是否有纸<br>3. 进入 <b>系统设置 → 系统配置 → 打印配置</b>，点击"测试打印"<br>4. 如仍无法打印，联系技术支持', expanded: false },
  { q: '如何备份数据库？', a: '进入 <b>系统设置 → 系统配置 → 备份配置</b>，点击"立即备份"按钮手动备份。系统默认每天凌晨2点自动备份，备份文件保存在 <code>/mnt/cos/backups/</code> 目录。', expanded: false },
  { q: '如何查看操作日志？', a: '进入 <b>系统设置 → 帮助与日志 → 操作日志</b>，可以按日期范围和操作类型筛选，查看所有用户的操作记录。', expanded: false },
  { q: '系统卡顿如何优化？', a: '1. 进入 <b>系统设置 → 系统体检</b>，运行系统检查<br>2. 查看服务器资源使用情况（CPU、内存、磁盘）<br>3. 清理过期日志和临时文件<br>4. 优化数据库表<br>5. 如问题持续，考虑升级服务器配置', expanded: false },
])

const filteredFaqs = computed(() => {
  if (!faqSearch.value) return faqs
  return faqs.filter(f => f.q.includes(faqSearch.value) || f.a.includes(faqSearch.value))
})

// 操作日志数据
const logDateRange = ref([])
const logType = ref('')
const logSearch = ref('')
const logPage = ref(1)

const logs = ref([
  { time: '2026-07-31 09:15:23', user: '张三', type: 'login', typeLabel: '登录', action: '用户登录系统', ip: '100.70.171.0' },
  { time: '2026-07-31 09:20:45', user: '李四', type: 'edit', typeLabel: '数据修改', action: '修改了菜品"招牌红烧肉"的价格', ip: '100.70.171.15' },
  { time: '2026-07-31 10:05:12', user: '王五', type: 'edit', typeLabel: '数据修改', action: '新增了预订记录 #2026073101', ip: '100.70.171.22' },
  { time: '2026-07-31 11:30:00', user: '系统', type: 'export', typeLabel: '导出', action: '自动生成每日营业报表', ip: 'localhost' },
  { time: '2026-07-31 14:22:33', user: '赵六', type: 'delete', typeLabel: '删除', action: '删除了过期菜单项 5 条', ip: '100.70.171.18' },
  { time: '2026-07-31 15:45:18', user: '张三', type: 'edit', typeLabel: '数据修改', action: '更新了供应商"安徽农产品公司"的联系信息', ip: '100.70.171.0' },
  { time: '2026-07-31 16:30:00', user: '系统', type: 'export', typeLabel: '导出', action: '自动备份数据库 (256 MB)', ip: 'localhost' },
])

const filteredLogs = computed(() => {
  let result = logs.value
  if (logType.value) {
    result = result.filter(l => l.type === logType.value)
  }
  if (logSearch.value) {
    result = result.filter(l => l.action.includes(logSearch.value))
  }
  return result
})

function getLogType(type) {
  const map = { login: '', edit: 'warning', delete: 'danger', export: 'success' }
  return map[type] || 'info'
}

// 开发配置数据
const devTimeline = ref([
  {
    title: '第一阶段：项目启动',
    date: '2025年下半年',
    description: '又见炊烟私房菜宴会预定系统立项。确定技术栈为 Spring Boot 3 + Vue 3 + MySQL 8，采用前后端分离架构。',
    features: [
      '数据库设计与建表：booking_master、dish_master、customer_master 等核心表',
      '门店多租户架构：store_id 字段实现宁国店、宣城店数据隔离',
      '基础CRUD：预订管理、客户管理、菜品管理'
    ]
  },
  {
    title: '第二阶段：功能扩展',
    date: '2026年1-3月',
    description: '系统功能快速扩展，覆盖餐饮业务全流程。',
    features: [
      '桌台管理：TableBoard 可视化桌台看板，拖拽布局',
      '菜单系统：MenuHub 菜单中心，支持零点、宴会、节日菜单',
      '采购仓储：SupplyChain 采购入库、领用出库、库存管理',
      '人事行政：HR 模块，员工档案、考勤排班、薪资管理'
    ]
  },
  {
    title: '第三阶段：深度打磨',
    date: '2026年4-6月',
    description: '系统深度优化，增加数据分析和移动端支持。',
    features: [
      '财务模块：营收核算、对账管理、财务报表',
      '营销会员：会员管理、营销活动、积分体系',
      '工程管理：设备维护、装修管理、能耗安全',
      '数据大屏：经营分析看板，实时数据展示',
      'iPad点菜端：IpadMenu 美团式布局，拖拽点菜'
    ]
  },
  {
    title: '第四阶段：系统完善',
    date: '2026年7月',
    description: '系统全面重构，提升稳定性和可维护性。',
    features: [
      '套餐管理：SetMenu 宴会套餐，三栏编辑布局，价格联动',
      '菜单管理重组：11个子模块（菜库编辑、成本配方、调价管理、沽清管控等）',
      '总经办：GMOffice 总经理驾驶舱',
      '系统体检：全量扫描4780+检测项，数据库/后端/前端全覆盖',
      '前后端数据库字段对齐扫描',
      '账单管理：BillManage 结账清算',
      'UI升级：控制台淡蓝灰主题，统计卡片重新设计'
    ]
  }
])

const techStack = ref([
  { name: 'Vue 3', version: 'v3.4.21', emoji: '🟢', bgColor: 'rgba(66, 184, 131, 0.1)' },
  { name: 'Element Plus', version: 'v2.7.0', emoji: '🔵', bgColor: 'rgba(64, 158, 255, 0.1)' },
  { name: 'Spring Boot', version: 'v3.2.0', emoji: '🍃', bgColor: 'rgba(76, 175, 80, 0.1)' },
  { name: 'MySQL', version: 'v8.0.34', emoji: '🐬', bgColor: 'rgba(0, 120, 215, 0.1)' },
  { name: 'Redis', version: 'v7.0.12', emoji: '🔴', bgColor: 'rgba(220, 53, 69, 0.1)' },
  { name: 'Nginx', version: 'v1.24.0', emoji: '🟩', bgColor: 'rgba(0, 150, 136, 0.1)' },
])

const projectStats = reactive({
  vueFiles: 85,
  javaFiles: 120,
  dbTables: 45,
  apiEndpoints: 180,
  codeLines: '45,000+',
  checkupItems: '4,780+'
})
</script>

<style scoped>
.help-page {
  max-width: 1200px;
}

.page-header {
  margin-bottom: 28px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text);
  margin: 0 0 4px 0;
}

.page-subtitle {
  font-size: 13px;
  color: var(--color-text-muted);
  margin: 0;
}

.help-tabs {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px;
}

.tab-content {
  margin-top: 20px;
}

.command-section {
  margin-bottom: 32px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 16px 0;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--color-border-light);
}

.command-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.command-item {
  background: var(--color-bg-alt);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 16px;
}

.command-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.command-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
}

.command-code {
  background: #2D4A3E;
  color: #10b981;
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  font-family: 'Courier New', monospace;
  font-size: 13px;
  margin-bottom: 8px;
  overflow-x: auto;
}

.command-desc {
  font-size: 12px;
  color: var(--color-text-muted);
}

.faq-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.faq-item {
  background: var(--color-bg-alt);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.faq-question {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  cursor: pointer;
  transition: background 0.2s;
}

.faq-question:hover {
  background: var(--color-border-light);
}

.faq-icon {
  font-size: 12px;
  color: var(--color-text-muted);
}

.faq-text {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text);
}

.faq-answer {
  padding: 16px;
  background: white;
  border-top: 1px solid var(--color-border-light);
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.log-toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.log-pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.timeline {
  position: relative;
  padding-left: 32px;
}

.timeline::before {
  content: '';
  position: absolute;
  left: 11px;
  top: 8px;
  bottom: 0;
  width: 2px;
  background: linear-gradient(180deg, #2D4A3E, #e0e0e0);
}

.timeline-item {
  position: relative;
  margin-bottom: 28px;
}

.timeline-dot {
  position: absolute;
  left: -26px;
  top: 8px;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #2D4A3E;
  border: 3px solid #fff;
  box-shadow: 0 0 0 2px #2D4A3E;
}

.timeline-content {
  background: var(--color-bg-alt);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 20px;
}

.timeline-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.timeline-header h4 {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0;
}

.timeline-body p {
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.7;
  margin: 0 0 10px 0;
}

.timeline-body ul {
  margin: 0;
  padding-left: 20px;
}

.timeline-body li {
  font-size: 12px;
  color: var(--color-text-muted);
  line-height: 1.8;
}

.tech-stack {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.tech-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  background: var(--color-bg-alt);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.tech-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.tech-info h4 {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 2px 0;
}

.tech-info p {
  font-size: 11px;
  color: var(--color-text-muted);
  margin: 0;
  font-family: monospace;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 16px;
}

.stat-card {
  background: var(--color-bg-alt);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 20px;
  text-align: center;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-primary);
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  color: var(--color-text-muted);
}

@media (max-width: 768px) {
  .log-toolbar {
    flex-direction: column;
  }
  
  .log-toolbar > * {
    width: 100% !important;
  }
  
  .tech-stack {
    grid-template-columns: 1fr;
  }
  
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
