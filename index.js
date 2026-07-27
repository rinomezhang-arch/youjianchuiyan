import { createRouter, createWebHistory } from 'vue-router'
import Login from '@/views/Login.vue'
import StoreSelect from '@/views/StoreSelect.vue'
import Dashboard from '@/views/Dashboard.vue'

const routes = [
  { path: '/login', name: 'Login', component: Login, meta: { title: '登录' } },
  { path: '/store-select', name: 'StoreSelect', component: StoreSelect, meta: { title: '选择门店' } },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: Dashboard,
    redirect: '/dashboard/home',
    children: [
      { path: 'home', name: 'Home', component: () => import('@/views/dashboard/Home.vue'), meta: { requiresAuth: true, title: '工作台' } },
      { path: 'table-board', name: 'TableBoard', component: () => import('@/views/dashboard/TableBoard.vue'), meta: { requiresAuth: true, title: '桌台看板' } },
      { path: 'bookings', name: 'Bookings', component: () => import('@/views/dashboard/Bookings.vue'), meta: { requiresAuth: true, title: '预订管理' } },
      { path: 'menu', name: 'Menu', component: () => import('@/views/dashboard/MenuHub.vue'), meta: { requiresAuth: true, title: '点菜系统' } },
      { path: 'menu-banquet', name: 'MenuBanquet', component: () => import('@/views/dashboard/MenuBanquet.vue'), meta: { requiresAuth: true, title: '宴会菜单' } },
      { path: 'menu-alacarte', name: 'MenuAlacarte', component: () => import('@/views/dashboard/MenuAlacarte.vue'), meta: { requiresAuth: true, title: '零点菜单' } },
      { path: 'menu-soldout', name: 'MenuSoldout', component: () => import('@/views/dashboard/MenuSoldout.vue'), meta: { requiresAuth: true, title: '沽清内容' } },
      { path: 'menu-festive', name: 'MenuFestive', component: () => import('@/views/dashboard/MenuFestive.vue'), meta: { requiresAuth: true, title: '节日菜单' } },
      { path: 'menu-full', name: 'MenuFull', component: () => import('@/views/dashboard/MenuFull.vue'), meta: { requiresAuth: true, title: '总菜单' } },
      { path: 'menu-detail', name: 'MenuDetail', component: () => import('@/views/dashboard/MenuDetail.vue'), meta: { requiresAuth: true, title: '菜单详情' } },
      { path: 'front-office', name: 'FrontOffice', component: () => import('@/views/dashboard/FrontOffice.vue'), meta: { requiresAuth: true, title: '前厅运营' } },
      { path: 'ai-assistant', name: 'AiAssistant', component: () => import('@/views/dashboard/AiAssistant.vue'), meta: { requiresAuth: true, title: 'AI助手' } },
      { path: 'floor-project', name: 'FloorProject', component: () => import('@/views/dashboard/FloorProject.vue'), meta: { requiresAuth: true, title: '楼面工程' } },
      { path: 'front-desk', name: 'FrontDesk', component: () => import('@/views/dashboard/FrontDesk.vue'), meta: { requiresAuth: true, title: '前台' } },
      { path: 'kitchen', name: 'Kitchen', component: () => import('@/views/dashboard/Kitchen.vue'), meta: { requiresAuth: true, title: '厨房出品' } },
      { path: 'art-design', name: 'ArtDesign', component: () => import('@/views/dashboard/ArtDesign.vue'), meta: { requiresAuth: true, title: '美工' } },
      { path: 'table-layout', name: 'TableLayout', component: () => import('@/views/dashboard/TableLayout.vue'), meta: { requiresAuth: true, title: '台型' } },
      { path: 'production', name: 'Production', component: () => import('@/views/dashboard/Production.vue'), meta: { requiresAuth: true, title: '出品' } },
      { path: 'packages', name: 'Packages', component: () => import('@/views/dashboard/Packages.vue'), meta: { requiresAuth: true, title: '套餐管理' } },
      { path: 'supply-chain', name: 'SupplyChain', component: () => import('@/views/dashboard/SupplyChain.vue'), meta: { requiresAuth: true, title: '采购仓储' } },
      { path: 'inventory', name: 'Inventory', component: () => import('@/views/dashboard/Inventory.vue'), meta: { requiresAuth: true, title: '库存管理' } },
      { path: 'procurement', name: 'Procurement', component: () => import('@/views/dashboard/Procurement.vue'), meta: { requiresAuth: true, title: '采购管理' } },
      { path: 'receipt', name: 'Receipt', component: () => import('@/views/dashboard/Receipt.vue'), meta: { requiresAuth: true, title: '入库验收' } },
      { path: 'issue', name: 'Issue', component: () => import('@/views/dashboard/Issue.vue'), meta: { requiresAuth: true, title: '领用出库' } },
      { path: 'supplier-reconciliation', name: 'SupplierReconciliation', component: () => import('@/views/dashboard/SupplierReconciliation.vue'), meta: { requiresAuth: true, title: '供应商对账' } },
      { path: 'stock-take', name: 'StockTake', component: () => import('@/views/dashboard/StockTake.vue'), meta: { requiresAuth: true, title: '盘点' } },
      { path: 'finance', name: 'Finance', component: () => import('@/views/dashboard/Finance.vue'), meta: { requiresAuth: true, title: '财务管理' } },
      { path: 'finance/dish-cost', name: 'FinanceDishCost', component: () => import('@/views/dashboard/DishCost.vue'), meta: { requiresAuth: true, title: '菜品成本管理' } },
      { path: 'finance/cost-analysis', name: 'FinanceCost', component: () => import('@/views/dashboard/Cost.vue'), meta: { requiresAuth: true, title: '成本分析' } },
      { path: 'hr-admin', name: 'HRAdmin', component: () => import('@/views/dashboard/HRAdmin.vue'), meta: { requiresAuth: true, title: '人事行政' } },
      { path: 'staff', name: 'Staff', component: () => import('@/views/dashboard/Staff.vue'), meta: { requiresAuth: true, title: '人事管理' } },
      { path: 'training', name: 'Training', component: () => import('@/views/dashboard/Training.vue'), meta: { requiresAuth: true, title: '培训管理' } },
      { path: 'license', name: 'License', component: () => import('@/views/dashboard/License.vue'), meta: { requiresAuth: true, title: '证照管理' } },
      { path: 'security', name: 'Security', component: () => import('@/views/dashboard/Security.vue'), meta: { requiresAuth: true, title: '安保保洁' } },
      { path: 'assets', name: 'Assets', component: () => import('@/views/dashboard/Assets.vue'), meta: { requiresAuth: true, title: '行政资产' } },
      { path: 'attendance', name: 'Attendance', component: () => import('@/views/dashboard/Attendance.vue'), meta: { requiresAuth: true, title: '考勤管理' } },
      { path: 'schedule', name: 'Schedule', component: () => import('@/views/dashboard/Schedule.vue'), meta: { requiresAuth: true, title: '排班管理' } },
      { path: 'leave', name: 'Leave', component: () => import('@/views/dashboard/Leave.vue'), meta: { requiresAuth: true, title: '请假管理' } },
      { path: 'dish-cost-analysis', name: 'DishCostAnalysis', component: () => import('@/views/dashboard/DishCostAnalysis.vue'), meta: { requiresAuth: true, title: '菜品成本分析' } },
      { path: 'suppliers', name: 'Suppliers', component: () => import('@/views/dashboard/Suppliers.vue'), meta: { requiresAuth: true, title: '供应商管理' } },
      { path: 'reports', name: 'Reports', component: () => import('@/views/dashboard/Reports.vue'), meta: { requiresAuth: true, title: '数据报表' } },
      { path: 'settings', name: 'Settings', component: () => import('@/views/dashboard/Settings.vue'), meta: { requiresAuth: true, title: '系统设置' } },
      { path: 'customers', name: 'Customers', component: () => import('@/views/dashboard/Customers.vue'), meta: { requiresAuth: true, title: '客户管理' } },
      { path: 'marketing', name: 'Marketing', component: () => import('@/views/dashboard/Marketing.vue'), meta: { requiresAuth: true, title: '营销会员' } },
      { path: 'data-screen', name: 'DataScreen', component: () => import('@/views/dashboard/DataScreen.vue'), meta: { requiresAuth: true, title: '数据大屏' } },
      { path: 'change-logs', name: 'ChangeLogs', component: () => import('@/views/dashboard/ChangeLogView.vue'), meta: { requiresAuth: true, title: '改动日志' } },
      { path: 'engineering', name: 'Engineering', component: () => import('@/views/dashboard/Engineering.vue'), meta: { requiresAuth: true, title: '工程管理' } },
      { path: 'decoration', name: 'Decoration', component: () => import('@/views/dashboard/Decoration.vue'), meta: { requiresAuth: true, title: '装修管理' } },
      { path: 'maintenance', name: 'Maintenance', component: () => import('@/views/dashboard/Maintenance.vue'), meta: { requiresAuth: true, title: '设备维护' } },
      { path: 'energy', name: 'Energy', component: () => import('@/views/dashboard/Energy.vue'), meta: { requiresAuth: true, title: '能耗管理' } },
      { path: 'safety', name: 'Safety', component: () => import('@/views/dashboard/Safety.vue'), meta: { requiresAuth: true, title: '安全管理' } },
      { path: 'guest-analysis', name: 'GuestAnalysis', component: () => import('@/views/dashboard/GuestAnalysis.vue'), meta: { requiresAuth: true, title: '客人分析' } },
      { path: 'staff-performance', name: 'StaffPerformance', component: () => import('@/views/dashboard/StaffPerformance.vue'), meta: { requiresAuth: true, title: '员工绩效' } },
      { path: 'table-utilization', name: 'TableUtilization', component: () => import('@/views/dashboard/TableUtilization.vue'), meta: { requiresAuth: true, title: '桌台利用率' } },
      { path: 'report-print', name: 'ReportPrint', component: () => import('@/views/dashboard/ReportPrint.vue'), meta: { requiresAuth: true, title: '报表打印' } },
      { path: 'perm-manager', name: 'PermManager', component: () => import('@/views/dashboard/PermManager.vue'), meta: { requiresAuth: true, title: '权限管理' } },
      { path: 'menu-manager', name: 'MenuManager', component: () => import('@/views/dashboard/MenuManager.vue'), meta: { requiresAuth: true, title: '菜单管理' } },
      { path: 'attendance-calendar', name: 'AttendanceCalendar', component: () => import('@/views/dashboard/AttendanceCalendar.vue'), meta: { requiresAuth: true, title: '考勤日历' } },
      { path: 'staff-profile/:id?', name: 'StaffProfile', component: () => import('@/views/dashboard/StaffProfile.vue'), meta: { requiresAuth: true, title: '员工档案' } },
      { path: 'payroll', name: 'Payroll', component: () => import('@/views/dashboard/Payroll.vue'), meta: { requiresAuth: true, title: '工资管理' } },
      { path: 'self-service', name: 'SelfService', component: () => import('@/views/dashboard/SelfService.vue'), meta: { requiresAuth: true, title: '自助登记' } },
      { path: 'review-queue', name: 'ReviewQueue', component: () => import('@/views/dashboard/ReviewQueue.vue'), meta: { requiresAuth: true, title: '审核队列' } },
      { path: 'hr-analytics', name: 'HRAnalytics', component: () => import('@/views/dashboard/HRAnalytics.vue'), meta: { requiresAuth: true, title: 'HR数据' } },
      { path: 'attendance-print', name: 'AttendancePrint', component: () => import('@/views/dashboard/AttendancePrint.vue'), meta: { requiresAuth: true, title: '考勤报表' } },
      { path: 'export-panel', name: 'ExportPanel', component: () => import('@/views/dashboard/ExportPanel.vue'), meta: { requiresAuth: true, title: '数据导出' } },
      { path: 'audit-log', name: 'AuditLog', component: () => import('@/views/dashboard/AuditLog.vue'), meta: { requiresAuth: true, title: '审计日志' } },
      { path: 'dict-manager', name: 'DictManager', component: () => import('@/views/dashboard/DictManager.vue'), meta: { requiresAuth: true, title: '数据字典' } },
      { path: 'admin', name: 'Admin', component: () => import('@/views/dashboard/Admin.vue'), meta: { requiresAuth: true, title: '后台管理' } },
      { path: 'category-sort', name: 'CategorySort', component: () => import('@/views/dashboard/CategorySort.vue'), meta: { requiresAuth: true, title: '菜品分类排序' } },
      { path: 'menu-sort', name: 'MenuSort', component: () => import('@/views/dashboard/MenuSort.vue'), meta: { requiresAuth: true, title: '菜单排序管理' } },
      { path: 'ipad', name: 'IpadMenu', component: () => import('@/views/dashboard/IpadMenu.vue'), meta: { requiresAuth: true, title: 'iPad点菜' } }
    ]
  },
  { path: '/', redirect: '/store-select' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  if (to.meta.requiresAuth) {
    next()
  } else {
    next()
  }
})

export default router
