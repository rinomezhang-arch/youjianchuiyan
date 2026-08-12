<template>
  <div class="config-page">
    <div class="page-header">
      <h2 class="page-title">系统配置</h2>
      <p class="page-subtitle">System Configuration · 营业参数与系统设置</p>
    </div>

    <!-- 营业时间配置 -->
    <div class="config-section">
      <h3 class="section-title">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/>
          <polyline points="12 6 12 12 16 14"/>
        </svg>
        营业时间配置
      </h3>
      <div class="config-form">
        <div class="form-row">
          <label class="form-label">午餐时段</label>
          <div class="time-picker-group">
            <el-time-picker
              v-model="businessHours.lunchStart"
              placeholder="开始时间"
              format="HH:mm"
              value-format="HH:mm"
            />
            <span class="time-separator">至</span>
            <el-time-picker
              v-model="businessHours.lunchEnd"
              placeholder="结束时间"
              format="HH:mm"
              value-format="HH:mm"
            />
          </div>
        </div>
        <div class="form-row">
          <label class="form-label">晚餐时段</label>
          <div class="time-picker-group">
            <el-time-picker
              v-model="businessHours.dinnerStart"
              placeholder="开始时间"
              format="HH:mm"
              value-format="HH:mm"
            />
            <span class="time-separator">至</span>
            <el-time-picker
              v-model="businessHours.dinnerEnd"
              placeholder="结束时间"
              format="HH:mm"
              value-format="HH:mm"
            />
          </div>
        </div>
        <div class="form-row">
          <label class="form-label">营业日</label>
          <el-checkbox-group v-model="businessHours.workingDays">
            <el-checkbox v-for="day in weekDays" :key="day.value" :label="day.value" :value="day.value">{{ day.label }}</el-checkbox>
          </el-checkbox-group>
        </div>
        <div class="form-actions">
          <el-button type="primary" @click="saveBusinessHours">保存营业时间</el-button>
        </div>
      </div>
    </div>

    <!-- 打印配置 -->
    <div class="config-section">
      <h3 class="section-title">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="6 9 6 2 18 2 18 9"/>
          <path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"/>
          <rect x="6" y="14" width="12" height="8"/>
        </svg>
        打印配置
      </h3>
      <div class="config-form">
        <div class="form-row">
          <label class="form-label">默认打印机</label>
          <el-select v-model="printConfig.defaultPrinter" style="width: 280px">
            <el-option label="EPSON TM-T88V (前厅)" value="epson-front" />
            <el-option label="EPSON TM-T88V (后厨)" value="epson-kitchen" />
            <el-option label="佳博 GP-L80160I" value="gainscha" />
          </el-select>
        </div>
        <div class="form-row">
          <label class="form-label">打印份数</label>
          <el-input-number v-model="printConfig.copies" :min="1" :max="5" />
        </div>
        <div class="form-row">
          <label class="form-label">打印模板</label>
          <el-select v-model="printConfig.template" style="width: 280px">
            <el-option label="标准小票模板" value="standard" />
            <el-option label="宴会菜单模板" value="banquet" />
            <el-option label="简约模板" value="simple" />
          </el-select>
        </div>
        <div class="form-row">
          <label class="form-label">打印选项</label>
          <div class="checkbox-group">
            <el-checkbox v-model="printConfig.autoPrint">自动打印订单</el-checkbox>
            <el-checkbox v-model="printConfig.printKitchen">后厨分单打印</el-checkbox>
            <el-checkbox v-model="printConfig.printReceipt">结账小票打印</el-checkbox>
            <el-checkbox v-model="printConfig.showLogo">小票显示Logo</el-checkbox>
          </div>
        </div>
        <div class="form-row">
          <label class="form-label">模板预览</label>
          <div class="template-preview">
            <div class="preview-receipt">
              <div class="receipt-header">
                <div class="receipt-logo">又见炊烟私房菜</div>
                <div class="receipt-sub">Youjianchuiyan Private Kitchen</div>
              </div>
              <div class="receipt-divider"></div>
              <div class="receipt-body">
                <div class="receipt-row"><span>桌号：A08</span><span>人数：4</span></div>
                <div class="receipt-row"><span>时间：{{ new Date().toLocaleString('zh-CN') }}</span></div>
                <div class="receipt-divider"></div>
                <div class="receipt-item"><span>招牌红烧肉</span><span>¥88.00</span></div>
                <div class="receipt-item"><span>清炒时蔬</span><span>¥38.00</span></div>
                <div class="receipt-item"><span>西湖醋鱼</span><span>¥128.00</span></div>
                <div class="receipt-divider"></div>
                <div class="receipt-total"><span>合计</span><span>¥254.00</span></div>
              </div>
              <div class="receipt-footer">
                <div>谢谢惠顾，欢迎再来！</div>
              </div>
            </div>
          </div>
        </div>
        <div class="form-actions">
          <el-button type="primary" @click="savePrintConfig">保存打印配置</el-button>
          <el-button @click="testPrint">测试打印</el-button>
        </div>
      </div>
    </div>

    <!-- 备份配置 -->
    <div class="config-section">
      <h3 class="section-title">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
          <polyline points="7 10 12 15 17 10"/>
          <line x1="12" y1="15" x2="12" y2="3"/>
        </svg>
        备份配置
      </h3>
      <div class="config-form">
        <div class="form-row">
          <label class="form-label">自动备份</label>
          <el-switch v-model="backupConfig.autoBackup" />
        </div>
        <div class="form-row">
          <label class="form-label">备份时间</label>
          <el-time-picker
            v-model="backupConfig.backupTime"
            placeholder="选择备份时间"
            format="HH:mm"
            value-format="HH:mm"
          />
        </div>
        <div class="form-row">
          <label class="form-label">备份路径</label>
          <el-input v-model="backupConfig.backupPath" style="width: 400px" />
        </div>
        <div class="form-row">
          <label class="form-label">保留天数</label>
          <el-input-number v-model="backupConfig.retentionDays" :min="7" :max="365" />
        </div>
        <div class="backup-history">
          <div class="history-header">
            <h4>最近备份记录</h4>
          </div>
          <el-table :data="backupHistory" stripe size="small" style="width: 100%">
            <el-table-column prop="time" label="备份时间" width="180" />
            <el-table-column prop="size" label="大小" width="100" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === '成功' ? 'success' : 'danger'" size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="path" label="备份路径" />
          </el-table>
        </div>
        <div class="form-actions">
          <el-button type="primary" @click="saveBackupConfig">保存备份配置</el-button>
          <el-button type="warning" @click="manualBackup">立即备份</el-button>
        </div>
      </div>
    </div>

    <!-- 系统参数 -->
    <div class="config-section">
      <h3 class="section-title">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="3"/>
          <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>
        </svg>
        系统参数
      </h3>
      <div class="config-form">
        <div class="form-row">
          <label class="form-label">节假日配置</label>
          <div class="holidays-list">
            <div v-for="holiday in systemParams.holidays" :key="holiday.name" class="holiday-item">
              <div class="holiday-info">
                <span class="holiday-name">{{ holiday.name }}</span>
                <span class="holiday-date">{{ holiday.date }}</span>
              </div>
              <el-switch v-model="holiday.enabled" />
            </div>
            <el-button size="small" @click="addHoliday" style="margin-top:8px">添加节假日</el-button>
          </div>
        </div>
        <div class="form-row">
          <label class="form-label">桌台预警时间</label>
          <div class="inline-group">
            <el-input-number v-model="systemParams.warningMinutes" :min="5" :max="120" />
            <span class="unit-text">分钟（超时提醒）</span>
          </div>
        </div>
        <div class="form-row">
          <label class="form-label">默认服务费</label>
          <div class="inline-group">
            <el-input-number v-model="systemParams.serviceFee" :min="0" :max="20" :precision="1" />
            <span class="unit-text">%</span>
          </div>
        </div>
        <div class="form-row">
          <label class="form-label">订单超时自动关闭</label>
          <div class="inline-group">
            <el-input-number v-model="systemParams.orderTimeout" :min="10" :max="180" />
            <span class="unit-text">分钟</span>
          </div>
        </div>
        <div class="form-actions">
          <el-button type="primary" @click="saveSystemParams">保存系统参数</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

const weekDays = [
  { label: '周一', value: 1 },
  { label: '周二', value: 2 },
  { label: '周三', value: 3 },
  { label: '周四', value: 4 },
  { label: '周五', value: 5 },
  { label: '周六', value: 6 },
  { label: '周日', value: 0 },
]

const businessHours = reactive({
  lunchStart: '10:30',
  lunchEnd: '14:00',
  dinnerStart: '17:00',
  dinnerEnd: '21:30',
  workingDays: [1, 2, 3, 4, 5, 6, 0]
})

const printConfig = reactive({
  defaultPrinter: 'epson-front',
  copies: 1,
  template: 'standard',
  autoPrint: true,
  printKitchen: true,
  printReceipt: true,
  showLogo: true
})

const backupConfig = reactive({
  autoBackup: true,
  backupTime: '02:00',
  backupPath: '/mnt/cos/backups/youjianchuiyan/',
  retentionDays: 30
})

const backupHistory = ref([
  { time: '2026-07-31 02:00:00', size: '256 MB', status: '成功', path: '/mnt/cos/backups/youjianchuiyan/2026-07-31.sql' },
  { time: '2026-07-30 02:00:00', size: '254 MB', status: '成功', path: '/mnt/cos/backups/youjianchuiyan/2026-07-30.sql' },
  { time: '2026-07-29 02:00:00', size: '253 MB', status: '成功', path: '/mnt/cos/backups/youjianchuiyan/2026-07-29.sql' },
  { time: '2026-07-28 02:00:00', size: '251 MB', status: '成功', path: '/mnt/cos/backups/youjianchuiyan/2026-07-28.sql' },
])

const systemParams = reactive({
  holidays: [
    { name: '元旦', date: '2026-01-01', enabled: true },
    { name: '春节', date: '2026-02-17 ~ 02-23', enabled: true },
    { name: '清明节', date: '2026-04-05', enabled: true },
    { name: '劳动节', date: '2026-05-01 ~ 05-05', enabled: true },
    { name: '端午节', date: '2026-05-31', enabled: true },
    { name: '中秋节', date: '2026-09-25', enabled: true },
    { name: '国庆节', date: '2026-10-01 ~ 10-07', enabled: true },
  ],
  warningMinutes: 90,
  serviceFee: 10,
  orderTimeout: 60
})

function saveBusinessHours() {
  ElMessage.success('营业时间配置已保存')
}

function savePrintConfig() {
  ElMessage.success('打印配置已保存')
}

function testPrint() {
  ElMessage.info('正在发送测试打印...')
  setTimeout(() => ElMessage.success('测试打印已发送'), 1000)
}

function saveBackupConfig() {
  ElMessage.success('备份配置已保存')
}

function manualBackup() {
  ElMessage.info('正在执行手动备份...')
  setTimeout(() => {
    backupHistory.value.unshift({
      time: new Date().toLocaleString('zh-CN'),
      size: '257 MB',
      status: '成功',
      path: `/mnt/cos/backups/youjianchuiyan/${new Date().toISOString().slice(0, 10)}-manual.sql`
    })
    ElMessage.success('手动备份完成')
  }, 2000)
}

function saveSystemParams() {
  ElMessage.success('系统参数已保存')
}

function addHoliday() {
  ElMessage.info('添加节假日功能待实现')
}
</script>

<style scoped>
.config-page {
  max-width: 1000px;
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

.config-section {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 24px;
  margin-bottom: 24px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 20px 0;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--color-border-light);
}

.config-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-row {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.form-label {
  width: 140px;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  padding-top: 8px;
  flex-shrink: 0;
}

.time-picker-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.time-separator {
  font-size: 13px;
  color: var(--color-text-muted);
}

.checkbox-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.inline-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.unit-text {
  font-size: 13px;
  color: var(--color-text-muted);
}

.form-actions {
  display: flex;
  gap: 12px;
  padding-top: 8px;
  border-top: 1px solid var(--color-border-light);
  padding-left: 156px;
}

.template-preview {
  background: #f5f5f0;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 20px;
  display: flex;
  justify-content: center;
}

.preview-receipt {
  width: 280px;
  background: white;
  padding: 20px;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.receipt-header {
  text-align: center;
  margin-bottom: 12px;
}

.receipt-logo {
  font-size: 14px;
  font-weight: 700;
  color: #2D4A3E;
}

.receipt-sub {
  font-size: 10px;
  color: #999;
  margin-top: 2px;
}

.receipt-divider {
  border-top: 1px dashed #ddd;
  margin: 8px 0;
}

.receipt-body {
  font-size: 12px;
}

.receipt-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
  color: #666;
}

.receipt-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
  color: #333;
}

.receipt-total {
  display: flex;
  justify-content: space-between;
  font-weight: 700;
  font-size: 14px;
  color: #2D4A3E;
  margin-top: 4px;
}

.receipt-footer {
  text-align: center;
  font-size: 11px;
  color: #999;
  margin-top: 12px;
}

.backup-history {
  margin-top: 12px;
}

.history-header h4 {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 12px 0;
}

.holidays-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.holiday-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: var(--color-bg-alt);
  border-radius: var(--radius-sm);
}

.holiday-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.holiday-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text);
  min-width: 60px;
}

.holiday-date {
  font-size: 12px;
  color: var(--color-text-muted);
  font-family: monospace;
}

@media (max-width: 768px) {
  .form-row {
    flex-direction: column;
  }
  
  .form-label {
    width: auto;
    padding-top: 0;
  }
  
  .form-actions {
    padding-left: 0;
  }
}
</style>
