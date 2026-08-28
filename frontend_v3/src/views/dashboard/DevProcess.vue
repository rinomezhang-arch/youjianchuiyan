<template>
  <div class="dev-page">
    <div class="page-header">
      <div class="back-row">
        <button class="back-btn" @click="goBack">← 返回帮助文件</button>
      </div>
      <h2 class="page-title">开发过程</h2>
      <p class="page-subtitle">Development Process · 项目从零到一的完整记录</p>
    </div>

    <div class="timeline">
      <div class="phase">
        <div class="phase-dot"></div>
        <div class="phase-content">
          <h3 class="phase-title">第一阶段：项目启动（2025年下半年）</h3>
          <div class="phase-body">
            <p>又见炊烟私房菜立项。确定技术栈为 Spring Boot 3 + Vue 3 + MySQL 8，采用前后端分离架构。</p>
            <ul>
              <li>数据库设计与建表：booking_master、dish_master、customer_master 等核心表</li>
              <li>门店多租户架构：store_id 字段实现宁国店、宣城店数据隔离</li>
              <li>基础CRUD：预订管理、客户管理、菜品管理</li>
            </ul>
          </div>
        </div>
      </div>

      <div class="phase">
        <div class="phase-dot"></div>
        <div class="phase-content">
          <h3 class="phase-title">第二阶段：功能扩展（2026年1-3月）</h3>
          <div class="phase-body">
            <ul>
              <li>桌台管理：TableBoard 可视化桌台看板，拖拽布局</li>
              <li>菜单系统：MenuHub 菜单中心，支持零点、宴会、节日菜单</li>
              <li>采购仓储：SupplyChain 采购入库、领用出库、库存管理</li>
              <li>人事行政：HR 模块，员工档案、考勤排班、薪资管理</li>
            </ul>
          </div>
        </div>
      </div>

      <div class="phase">
        <div class="phase-dot"></div>
        <div class="phase-content">
          <h3 class="phase-title">第三阶段：深度打磨（2026年4-6月）</h3>
          <div class="phase-body">
            <ul>
              <li>财务模块：营收核算、对账管理、财务报表</li>
              <li>营销会员：会员管理、营销活动、积分体系</li>
              <li>工程管理：设备维护、装修管理、能耗安全</li>
              <li>数据大屏：经营分析看板，实时数据展示</li>
              <li>iPad点菜端：IpadMenu 美团式布局，拖拽点菜</li>
            </ul>
          </div>
        </div>
      </div>

      <div class="phase">
        <div class="phase-dot"></div>
        <div class="phase-content">
          <h3 class="phase-title">第四阶段：系统完善（2026年7月）</h3>
          <div class="phase-body">
            <ul>
              <li>套餐管理：SetMenu 宴会套餐，三栏编辑布局，价格联动</li>
              <li>菜单管理重组：11个子模块（菜库编辑、成本配方、调价管理、沽清管控等）</li>
              <li>总经办：GMOffice 总经理驾驶舱</li>
              <li>系统体检：SystemCheckup 全量扫描4780+检测项</li>
              <li>账单管理：BillManage 结账清算</li>
              <li>UI升级：控制台淡蓝灰主题，统计卡片重新设计</li>
              <li>开发记录：DevProcess 时间线格式记录项目全过程</li>
            </ul>
          </div>
        </div>
      </div>

      <div class="phase">
        <div class="phase-dot"></div>
        <div class="phase-content">
          <h3 class="phase-title">第五阶段：系统工具模块整合（2026年7月31日）</h3>
          <div class="phase-body">
            <p>将零散的系统管理功能统一收纳到侧边栏"系统工具"模块，用户在一个入口找到所有后台管理功能。</p>
            <ul>
              <li>系统信息与运行状态：SystemDashboard 实时监控仪表盘，CPU/内存/磁盘/网络/服务状态</li>
              <li>权限管理：StorePermission 门店权限分配与角色管理</li>
              <li>门店与组织：StoreOrg 多门店管理 + 部门架构 + 组织层级树</li>
              <li>系统配置：SettingsHub 统一配置中心（信息/权限/组织/配置/帮助/体检）</li>
              <li>帮助与日志：HelpCenter 帮助文件 + DevProcess 开发记录</li>
              <li>系统体检：SystemCheckup 合并至系统工具</li>
              <li>账单管理 &amp; iPad点菜：保留在系统工具模块</li>
            </ul>
            <p>🔥 教训：修改前必须确认正确的工程目录！地龙改错了 F:\trae（旧工程），秋哥实际项目在 f:\solo\project\又见炊烟餐饮管理系统。发现后立即纠正，以正确目录重建所有改动。</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
function goBack() { router.push('/dashboard/help') }
</script>

<style scoped>
.dev-page { max-width: 800px; margin: 0 auto; }
.page-header { margin-bottom: 32px; }
.back-row { margin-bottom: 12px; }
.back-btn {
  background: none; border: none; color: #2D4A3E; font-size: 13px;
  cursor: pointer; padding: 4px 0;
}
.back-btn:hover { text-decoration: underline; }
.page-title { font-size: 24px; font-weight: 700; color: #1a1a1a; margin-bottom: 4px; }
.page-subtitle { font-size: 13px; color: #8a8a8a; }

.timeline { position: relative; padding-left: 32px; }
.timeline::before {
  content: ''; position: absolute; left: 11px; top: 8px; bottom: 0;
  width: 2px; background: linear-gradient(180deg, #2D4A3E, #e0e0e0);
}
.phase { position: relative; margin-bottom: 28px; }
.phase-dot {
  position: absolute; left: -26px; top: 8px;
  width: 14px; height: 14px; border-radius: 50%;
  background: #2D4A3E; border: 3px solid #fff;
  box-shadow: 0 0 0 2px #2D4A3E;
}
.phase-content { background: #fff; border: 1px solid #e8e8e8; border-radius: 10px; padding: 20px 24px; }
.phase-title { font-size: 16px; font-weight: 700; color: #1a1a1a; margin: 0 0 12px 0; }
.phase-body p { font-size: 14px; color: #555; line-height: 1.7; margin: 0 0 10px 0; }
.phase-body ul { margin: 0; padding-left: 20px; }
.phase-body li { font-size: 13px; color: #666; line-height: 1.8; }
</style>
