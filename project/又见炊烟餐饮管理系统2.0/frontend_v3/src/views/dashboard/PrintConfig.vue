<template>
  <div class="print-config-page">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">打印配置 · Print Config</h2>
        <p class="page-subtitle">打印机管理 · 打印模板 · 出票规则</p>
      </div>
      <div class="page-header-right">
        <el-button type="primary" @click="openAddPrinter">+ 添加打印机</el-button>
      </div>
    </div>

    <!-- 打印机列表 -->
    <div class="printer-section">
      <h3 class="section-title">打印机管理 · Printers</h3>
      <div class="printer-grid">
        <div v-for="printer in printers" :key="printer.id" class="printer-card" :class="{ offline: !printer.online }">
          <div class="printer-header">
            <div class="printer-icon">
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <polyline points="6 9 6 2 18 2 18 9"/>
                <path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"/>
                <rect x="6" y="14" width="12" height="8"/>
              </svg>
            </div>
            <div class="printer-status" :class="{ online: printer.online }">
              {{ printer.online ? '在线' : '离线' }}
            </div>
          </div>
          <div class="printer-name">{{ printer.name }}</div>
          <div class="printer-info">
            <div class="info-row">
              <span class="info-label">类型：</span>
              <span>{{ printer.type }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">位置：</span>
              <span>{{ printer.location }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">IP：</span>
              <span class="ip-addr">{{ printer.ip }}</span>
            </div>
          </div>
          <div class="printer-actions">
            <el-button text size="small" @click="testPrint(printer)">测试</el-button>
            <el-button text size="small" @click="editPrinter(printer)">编辑</el-button>
            <el-button text size="small" type="danger" @click="deletePrinter(printer)">删除</el-button>
          </div>
        </div>

        <div class="printer-add-card" @click="openAddPrinter">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19"/>
            <line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
          <span>添加打印机</span>
        </div>
      </div>
    </div>

    <!-- 打印规则配置 -->
    <div class="rules-section">
      <h3 class="section-title">出票规则 · Print Rules</h3>
      <div class="rules-table-wrapper">
        <el-table :data="printRules" stripe>
          <el-table-column prop="name" label="规则名称" min-width="150" />
          <el-table-column prop="trigger" label="触发条件" width="150">
            <template #default="{ row }">
              <el-tag size="small">{{ row.trigger }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="printer" label="目标打印机" width="150" />
          <el-table-column prop="copies" label="份数" width="80" />
          <el-table-column label="启用" width="100">
            <template #default="{ row }">
              <el-switch v-model="row.enabled" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button text size="small" @click="editRule(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 添加/编辑打印机弹窗 -->
    <el-dialog v-model="showPrinterDialog" :title="editingPrinter ? '编辑打印机' : '添加打印机'" width="500px">
      <el-form :model="printerForm" label-width="100px">
        <el-form-item label="打印机名称" required>
          <el-input v-model="printerForm.name" placeholder="如：后厨打印机" />
        </el-form-item>
        <el-form-item label="打印机类型" required>
          <el-select v-model="printerForm.type" class="full-width">
            <el-option label="网口打印机" value="network" />
            <el-option label="USB打印机" value="usb" />
            <el-option label="蓝牙打印机" value="bluetooth" />
          </el-select>
        </el-form-item>
        <el-form-item label="IP地址" v-if="printerForm.type === 'network'">
          <el-input v-model="printerForm.ip" placeholder="192.168.1.xxx" />
        </el-form-item>
        <el-form-item label="打印位置">
          <el-select v-model="printerForm.location" class="full-width">
            <el-option label="后厨" value="后厨" />
            <el-option label="吧台" value="吧台" />
            <el-option label="前台" value="前台" />
            <el-option label="备餐间" value="备餐间" />
          </el-select>
        </el-form-item>
        <el-form-item label="纸张宽度">
          <el-select v-model="printerForm.paperWidth" class="full-width">
            <el-option label="58mm" value="58" />
            <el-option label="80mm" value="80" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPrinterDialog = false">取消</el-button>
        <el-button type="primary" @click="savePrinter">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const printers = ref([
  { id: 1, name: '后厨主打印机', type: '网口打印机', location: '后厨', ip: '192.168.1.100', online: true },
  { id: 2, name: '吧台打印机', type: '网口打印机', location: '吧台', ip: '192.168.1.101', online: true },
  { id: 3, name: '前台打印机', type: 'USB打印机', location: '前台', ip: '-', online: true }
])

const printRules = ref([
  { id: 1, name: '新订单自动打印', trigger: '下单时', printer: '后厨主打印机', copies: 1, enabled: true },
  { id: 2, name: '加菜打印', trigger: '加菜时', printer: '后厨主打印机', copies: 1, enabled: true },
  { id: 3, name: '结账单打印', trigger: '结账时', printer: '前台打印机', copies: 2, enabled: true },
  { id: 4, name: '酒水单打印', trigger: '点酒水时', printer: '吧台打印机', copies: 1, enabled: true }
])

const showPrinterDialog = ref(false)
const editingPrinter = ref(false)
const printerForm = ref({ id: '', name: '', type: 'network', ip: '', location: '', paperWidth: '80' })

function openAddPrinter() {
  editingPrinter.value = false
  printerForm.value = { id: '', name: '', type: 'network', ip: '', location: '', paperWidth: '80' }
  showPrinterDialog.value = true
}

function editPrinter(printer) {
  editingPrinter.value = true
  printerForm.value = { ...printer }
  showPrinterDialog.value = true
}

function savePrinter() {
  if (!printerForm.value.name) { ElMessage.warning('请输入打印机名称'); return }
  if (editingPrinter.value) {
    const idx = printers.value.findIndex(p => p.id === printerForm.value.id)
    if (idx >= 0) printers.value[idx] = { ...printerForm.value }
  } else {
    printers.value.push({ ...printerForm.value, id: Date.now(), online: false })
  }
  ElMessage.success('保存成功')
  showPrinterDialog.value = false
}

async function deletePrinter(printer) {
  try {
    await ElMessageBox.confirm(`确定删除打印机"${printer.name}"？`, '确认删除', { type: 'warning' })
    printers.value = printers.value.filter(p => p.id !== printer.id)
    ElMessage.success('已删除')
  } catch (e) { /* cancel */ }
}

function testPrint(printer) {
  ElMessage.success(`已向"${printer.name}"发送测试打印`)
}

function editRule(rule) {
  ElMessage.info('编辑规则功能开发中')
}

onMounted(() => {})
</script>

<style scoped>
.print-config-page { max-width: 1400px; margin: 0 auto; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; flex-wrap: wrap; gap: 12px; }
.page-title { font-size: 22px; font-weight: 700; color: var(--color-text); margin-bottom: 4px; }
.page-subtitle { font-size: 13px; color: var(--color-text-muted); }
.section-title { font-size: 16px; font-weight: 600; color: var(--color-text); margin-bottom: 16px; }
.printer-section { margin-bottom: 32px; }
.printer-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; }
.printer-card { background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-lg); padding: 20px; transition: var(--transition); }
.printer-card:hover { box-shadow: var(--shadow-md); }
.printer-card.offline { opacity: 0.6; }
.printer-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px; }
.printer-icon { color: var(--color-primary); }
.printer-status { padding: 4px 10px; border-radius: 12px; font-size: 11px; font-weight: 600; background: var(--color-danger); color: #fff; }
.printer-status.online { background: var(--color-success); }
.printer-name { font-size: 16px; font-weight: 600; color: var(--color-text); margin-bottom: 12px; }
.printer-info { display: flex; flex-direction: column; gap: 6px; margin-bottom: 16px; }
.info-row { display: flex; font-size: 13px; color: var(--color-text-secondary); }
.info-label { color: var(--color-text-muted); min-width: 60px; }
.ip-addr { font-family: var(--font-family-sans); font-size: 12px; }
.printer-actions { display: flex; gap: 8px; padding-top: 12px; border-top: 1px solid var(--color-border-light); }
.printer-add-card { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; padding: 40px; border: 2px dashed var(--color-border); border-radius: var(--radius-lg); cursor: pointer; color: var(--color-text-muted); font-size: 14px; transition: all 0.2s; }
.printer-add-card:hover { border-color: var(--color-accent); color: var(--color-accent-dark); }
.rules-section { background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-lg); padding: 20px; }
.rules-table-wrapper { overflow: hidden; }
.full-width { width: 100%; }
</style>
