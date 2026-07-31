<template>
  <div class="hr-analytics">
    <!-- 统计卡片行 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: rgba(45,74,62,0.1);">
            <el-icon :size="32" color="#2D4A3E"><UserFilled /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">总人数</div>
            <div class="stat-value">{{ analytics.totalEmployees }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: rgba(45,74,62,0.1);">
            <el-icon :size="32" color="#2D4A3E"><Calendar /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">本月出勤率</div>
            <div class="stat-value">{{ analytics.attendanceRate }}%</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: rgba(245,108,108,0.1);">
            <el-icon :size="32" color="#F56C6C"><WarningFilled /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">异常人数</div>
            <div class="stat-value" style="color: #F56C6C;">{{ analytics.abnormalCount }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background: rgba(230,162,60,0.1);">
            <el-icon :size="32" color="#E6A23C"><Clock /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-label">请假人数</div>
            <div class="stat-value" style="color: #E6A23C;">{{ analytics.leaveCount }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <span class="chart-title">出勤率趋势（近12个月）</span>
          </template>
          <div ref="attendanceTrendRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <span class="chart-title">部门出勤对比</span>
          </template>
          <div ref="deptCompareRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <span class="chart-title">异常分布</span>
          </template>
          <div ref="abnormalPieRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <span class="chart-title">员工状态分布</span>
          </template>
          <div ref="statusPieRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { UserFilled, Calendar, WarningFilled, Clock } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import request from '@/utils/request'

const analytics = reactive({
  totalEmployees: 0,
  attendanceRate: 0,
  abnormalCount: 0,
  leaveCount: 0,
  attendanceTrend: [],
  deptComparison: [],
  abnormalDistribution: [],
  statusDistribution: []
})

const attendanceTrendRef = ref(null)
const deptCompareRef = ref(null)
const abnormalPieRef = ref(null)
const statusPieRef = ref(null)

let trendChart = null
let deptChart = null
let abnormalChart = null
let statusChart = null

const themeColors = {
  primary: '#2D4A3E',
  primaryLight: '#3D6A5A',
  primaryLighter: '#5A8A7A',
  danger: '#F56C6C',
  warning: '#E6A23C',
  success: '#67C23A',
  info: '#909399'
}

async function fetchData() {
  try {
    const res = await request.get('/api/hr/analytics')
    const data = res.data || res
    analytics.totalEmployees = data.totalEmployees || 0
    analytics.attendanceRate = data.attendanceRate || 0
    analytics.abnormalCount = data.abnormalCount || 0
    analytics.leaveCount = data.leaveCount || 0
    analytics.attendanceTrend = data.attendanceTrend || []
    analytics.deptComparison = data.deptComparison || []
    analytics.abnormalDistribution = data.abnormalDistribution || []
    analytics.statusDistribution = data.statusDistribution || []
    await nextTick()
    renderCharts()
  } catch (error) {
    console.error('获取HR分析数据失败:', error)
  }
}

function renderCharts() {
  renderAttendanceTrend()
  renderDeptComparison()
  renderAbnormalPie()
  renderStatusPie()
}

function renderAttendanceTrend() {
  if (!attendanceTrendRef.value) return
  if (trendChart) trendChart.dispose()
  trendChart = echarts.init(attendanceTrendRef.value)

  const months = analytics.attendanceTrend.map(item => {
    const parts = item.month.split('-')
    return parts[1] + '月'
  })
  const rates = analytics.attendanceTrend.map(item => item.rate)

  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: '{b}<br/>出勤率: {c}%'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: months,
      boundaryGap: false,
      axisLine: { lineStyle: { color: '#999' } },
      axisLabel: { color: '#666' }
    },
    yAxis: {
      type: 'value',
      name: '出勤率(%)',
      min: 85,
      max: 100,
      axisLabel: {
        formatter: '{value}%',
        color: '#666'
      },
      splitLine: {
        lineStyle: { color: '#eee', type: 'dashed' }
      }
    },
    series: [
      {
        type: 'line',
        data: rates,
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: {
          color: themeColors.primary,
          width: 3
        },
        itemStyle: {
          color: themeColors.primary
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(45,74,62,0.3)' },
            { offset: 1, color: 'rgba(45,74,62,0.02)' }
          ])
        },
        markLine: {
          silent: true,
          data: [
            {
              yAxis: 95,
              lineStyle: { color: '#67C23A', type: 'dashed' },
              label: { formatter: '达标线 95%', color: '#67C23A' }
            }
          ]
        }
      }
    ]
  }
  trendChart.setOption(option)
}

function renderDeptComparison() {
  if (!deptCompareRef.value) return
  if (deptChart) deptChart.dispose()
  deptChart = echarts.init(deptCompareRef.value)

  const departments = analytics.deptComparison.map(item => item.department)
  const rates = analytics.deptComparison.map(item => item.rate)

  const option = {
    tooltip: {
      trigger: 'axis',
      formatter: function(params) {
        const item = analytics.deptComparison[params[0].dataIndex]
        return `${item.department}<br/>出勤率: ${item.rate}%<br/>人数: ${item.count}人`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: departments,
      axisLabel: {
        color: '#666',
        rotate: departments.length > 6 ? 30 : 0
      },
      axisLine: { lineStyle: { color: '#999' } }
    },
    yAxis: {
      type: 'value',
      name: '出勤率(%)',
      min: 85,
      max: 100,
      axisLabel: {
        formatter: '{value}%',
        color: '#666'
      },
      splitLine: {
        lineStyle: { color: '#eee', type: 'dashed' }
      }
    },
    series: [
      {
        type: 'bar',
        data: rates,
        barWidth: '50%',
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: themeColors.primaryLight },
            { offset: 1, color: themeColors.primary }
          ]),
          borderRadius: [6, 6, 0, 0]
        },
        label: {
          show: true,
          position: 'top',
          formatter: '{c}%',
          color: '#666',
          fontSize: 12
        },
        markLine: {
          silent: true,
          data: [
            {
              yAxis: 95,
              lineStyle: { color: '#67C23A', type: 'dashed' },
              label: { formatter: '达标线', color: '#67C23A' }
            }
          ]
        }
      }
    ]
  }
  deptChart.setOption(option)
}

function renderAbnormalPie() {
  if (!abnormalPieRef.value) return
  if (abnormalChart) abnormalChart.dispose()
  abnormalChart = echarts.init(abnormalPieRef.value)

  const data = analytics.abnormalDistribution.map(item => ({
    name: item.name,
    value: item.value
  }))

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}人次 ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      textStyle: { color: '#666' }
    },
    color: ['#F56C6C', '#E6A23C', '#909399', '#67C23A'],
    series: [
      {
        type: 'pie',
        radius: ['45%', '75%'],
        center: ['40%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 6,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: '{b}\n{d}%',
          color: '#666'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 16,
            fontWeight: 'bold'
          }
        },
        data: data
      }
    ]
  }
  abnormalChart.setOption(option)
}

function renderStatusPie() {
  if (!statusPieRef.value) return
  if (statusChart) statusChart.dispose()
  statusChart = echarts.init(statusPieRef.value)

  const data = analytics.statusDistribution.map(item => ({
    name: item.name,
    value: item.value
  }))

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}人 ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      textStyle: { color: '#666' }
    },
    color: [themeColors.primary, '#909399', themeColors.warning],
    series: [
      {
        type: 'pie',
        radius: ['45%', '75%'],
        center: ['40%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 6,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: '{b}\n{d}%',
          color: '#666'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 16,
            fontWeight: 'bold'
          }
        },
        data: data
      }
    ]
  }
  statusChart.setOption(option)
}

function handleResize() {
  trendChart?.resize()
  deptChart?.resize()
  abnormalChart?.resize()
  statusChart?.resize()
}

onMounted(() => {
  fetchData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  deptChart?.dispose()
  abnormalChart?.dispose()
  statusChart?.dispose()
})
</script>

<style scoped>
.hr-analytics {
  padding: 20px;
}

.stat-cards {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 12px;
  border: none;
  transition: transform 0.3s;
}

.stat-card:hover {
  transform: translateY(-4px);
}

.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  padding: 20px;
  gap: 16px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 6px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #2D4A3E;
  line-height: 1;
}

.chart-row {
  margin-bottom: 20px;
}

.chart-card {
  border-radius: 12px;
  border: none;
  height: 100%;
}

.chart-card :deep(.el-card__header) {
  border-bottom: 1px solid #f0f0f0;
  padding: 16px 20px;
}

.chart-title {
  font-size: 16px;
  font-weight: 600;
  color: #2D4A3E;
}

.chart-container {
  width: 100%;
  height: 350px;
}

@media (max-width: 768px) {
  .hr-analytics {
    padding: 10px;
  }

  .stat-card :deep(.el-card__body) {
    padding: 12px;
    gap: 10px;
  }

  .stat-icon {
    width: 44px;
    height: 44px;
  }

  .stat-value {
    font-size: 22px;
  }

  .chart-container {
    height: 280px;
  }
}
</style>
