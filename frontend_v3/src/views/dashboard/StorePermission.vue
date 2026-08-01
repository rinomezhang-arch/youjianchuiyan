<template>
  <div class="store-permission-page">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">门店权限 · Store Permission</h2>
        <p class="page-subtitle">门店功能权限 · 数据权限 · 操作权限配置</p>
      </div>
      <div class="page-header-right">
        <el-button type="primary" @click="saveAll">保存权限配置</el-button>
      </div>
    </div>

    <!-- 门店选择 -->
    <div class="store-selector">
      <div
        v-for="store in stores"
        :key="store.id"
        :class="['store-tab', { active: activeStore === store.id }]"
        @click="activeStore = store.id"
      >
        <span class="store-name">{{ store.name }}</span>
        <span class="store-badge">{{ store.permissionCount }}项</span>
      </div>
    </div>

    <!-- 权限配置面板 -->
    <div class="permission-panels">
      <div v-for="panel in permissionPanels" :key="panel.key" class="permission-panel">
        <div class="panel-header">
          <h4 class="panel-title">{{ panel.icon }} {{ panel.title }}</h4>
          <el-switch v-model="panel.enabled" size="small" />
        </div>
        <div class="panel-body" v-if="panel.enabled">
          <div v-for="item in panel.items" :key="item.key" class="permission-item">
            <div class="perm-info">
              <span class="perm-name">{{ item.name }}</span>
              <span class="perm-desc">{{ item.desc }}</span>
            </div>
            <el-switch v-model="item.enabled" size="small" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const activeStore = ref(1)

const stores = ref([
  { id: 1, name: '宁国店', permissionCount: 24 },
  { id: 2, name: '宣城店', permissionCount: 18 }
])

const permissionPanels = ref([
  {
    key: 'front', icon: '🏪', title: '前厅功能', enabled: true,
    items: [
      { key: 'booking', name: '预订管理', desc: '查看/创建/修改预订', enabled: true },
      { key: 'table', name: '桌台管理', desc: '开台/换台/清台', enabled: true },
      { key: 'cashier', name: '收银结算', desc: '结账/退款/挂单', enabled: true },
      { key: 'vip', name: 'VIP管理', desc: '会员/积分/充值', enabled: false }
    ]
  },
  {
    key: 'kitchen', icon: '🍳', title: '厨房功能', enabled: true,
    items: [
      { key: 'order', name: '点菜系统', desc: '下单/加菜/退菜', enabled: true },
      { key: 'package', name: '套餐管理', desc: '套餐编辑/定价', enabled: true },
      { key: 'soldout', name: '沽清管控', desc: '菜品沽清设置', enabled: false }
    ]
  },
  {
    key: 'finance', icon: '💰', title: '财务数据', enabled: true,
    items: [
      { key: 'report', name: '营收报表', desc: '查看日/月营收数据', enabled: true },
      { key: 'cost', name: '成本分析', desc: '菜品成本/毛利分析', enabled: true },
      { key: 'export', name: '数据导出', desc: '报表导出功能', enabled: false }
    ]
  },
  {
    key: 'hr', icon: '👥', title: '人事行政', enabled: false,
    items: [
      { key: 'staff', name: '员工管理', desc: '员工档案/入离职', enabled: true },
      { key: 'attendance', name: '考勤排班', desc: '考勤/排班/请假', enabled: true },
      { key: 'payroll', name: '工资管理', desc: '薪资核算/发放', enabled: false }
    ]
  }
])

function saveAll() {
  ElMessage.success('权限配置已保存')
}

onMounted(() => {})
</script>

<style scoped>
.store-permission-page { max-width: 1400px; margin: 0 auto; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; flex-wrap: wrap; gap: 12px; }
.page-title { font-size: 22px; font-weight: 700; color: var(--color-text); margin-bottom: 4px; }
.page-subtitle { font-size: 13px; color: var(--color-text-muted); }
.store-selector { display: flex; gap: 12px; margin-bottom: 24px; }
.store-tab { display: flex; align-items: center; gap: 10px; padding: 14px 24px; background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-md); cursor: pointer; transition: var(--transition); }
.store-tab:hover { border-color: var(--color-accent); }
.store-tab.active { background: var(--color-primary); color: #fff; border-color: var(--color-primary); }
.store-name { font-size: 15px; font-weight: 600; }
.store-badge { padding: 2px 10px; border-radius: 10px; font-size: 11px; background: rgba(0,0,0,0.1); }
.store-tab.active .store-badge { background: rgba(255,255,255,0.2); }
.permission-panels { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
.permission-panel { background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-lg); overflow: hidden; }
.panel-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; background: var(--color-bg-alt); border-bottom: 1px solid var(--color-border-light); }
.panel-title { font-size: 15px; font-weight: 600; color: var(--color-text); margin: 0; }
.panel-body { padding: 12px; }
.permission-item { display: flex; justify-content: space-between; align-items: center; padding: 12px; border-radius: var(--radius-sm); transition: var(--transition); }
.permission-item:hover { background: rgba(196, 163, 90, 0.06); }
.perm-info { display: flex; flex-direction: column; gap: 2px; }
.perm-name { font-size: 14px; font-weight: 500; color: var(--color-text); }
.perm-desc { font-size: 12px; color: var(--color-text-muted); }
</style>
