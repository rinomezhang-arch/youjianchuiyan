<template>
  <div class="store-org-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">门店与组织 · Store & Organization</h1>
        <p class="page-desc">多门店管理 · 部门架构 · 岗位编制 · 组织层级总览</p>
      </div>
    </div>

    <!-- 门店卡片 -->
    <div class="section">
      <h3 class="section-title">🏪 门店列表</h3>
      <div class="store-grid">
        <div class="store-card" v-for="store in stores" :key="store.id">
          <div class="store-top">
            <div class="store-icon">{{ store.id === 1 ? '🏪' : '🏬' }}</div>
            <div class="store-meta">
              <div class="store-name">{{ store.name }}</div>
              <div class="store-addr">{{ store.address }}</div>
            </div>
            <el-tag :type="store.status === 'open' ? 'success' : 'info'" size="small" effect="plain">
              {{ store.status === 'open' ? '营业中' : '筹备中' }}
            </el-tag>
          </div>
          <div class="store-details">
            <div class="detail-row"><span>联系电话</span><span>{{ store.phone }}</span></div>
            <div class="detail-row"><span>营业时间</span><span>{{ store.businessHours }}</span></div>
            <div class="detail-row"><span>桌台数量</span><span>{{ store.tableCount }} 桌</span></div>
            <div class="detail-row"><span>员工人数</span><span>{{ store.staffCount }} 人</span></div>
          </div>
        </div>
      </div>
    </div>

    <!-- 部门架构 -->
    <div class="section">
      <h3 class="section-title">👥 部门架构</h3>
      <div class="dept-grid">
        <div class="dept-card" v-for="dept in departments" :key="dept.name">
          <div class="dept-header">
            <div class="dept-icon">{{ dept.icon }}</div>
            <div class="dept-meta">
              <div class="dept-name">{{ dept.name }}</div>
              <div class="dept-count">{{ dept.count }} 人 · {{ dept.positions.length }} 个岗位</div>
            </div>
          </div>
          <div class="dept-positions">
            <span v-for="p in dept.positions" :key="p" class="pos-tag">{{ p }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 组织层级 -->
    <div class="section">
      <h3 class="section-title">🏛️ 组织层级</h3>
      <div class="org-tree">
        <div class="tree-level">
          <div class="tree-node root">
            <div class="node-icon">🏠</div>
            <div class="node-label">又见炊烟私房菜</div>
          </div>
          <div class="tree-arrow">↓</div>
          <div class="tree-children">
            <div class="tree-node branch" v-for="store in stores" :key="store.id">
              <div class="node-icon">{{ store.id === 1 ? '🏪' : '🏬' }}</div>
              <div class="node-label">{{ store.name }}</div>
              <div class="node-sub">店长：--</div>
            </div>
          </div>
          <div class="tree-arrow">↓</div>
          <div class="tree-children dept-level">
            <div class="tree-node leaf" v-for="dept in departments" :key="dept.name">
              <div class="node-icon">{{ dept.icon }}</div>
              <div class="node-label">{{ dept.name }}</div>
              <div class="node-sub">{{ dept.count }} 人</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const stores = ref([])
const departments = ref([])

async function fetchStores() {
  try {
    const res = await request.get('/api/stores')
    const data = res.data || res
    stores.value = Array.isArray(data) ? data : data?.list || []
  } catch (e) {
    console.error('获取门店列表失败', e)
    stores.value = []
  }
}

async function fetchDepartments() {
  try {
    const res = await request.get('/api/hr/departments')
    const data = res.data || res
    departments.value = Array.isArray(data) ? data : data?.list || []
  } catch (e) {
    console.error('获取部门列表失败', e)
    departments.value = []
  }
}

onMounted(() => {
  loading.value = true
  Promise.all([fetchStores(), fetchDepartments()]).finally(() => { loading.value = false })
})
</script>

<style scoped>
.store-org-page { max-width: 1200px; margin: 0 auto; }

.page-header { margin-bottom: 24px; }
.page-title { font-size: 22px; font-weight: 700; color: #1a1a1a; margin-bottom: 4px; }
.page-desc { font-size: 13px; color: #8a8a8a; }

.section { margin-bottom: 32px; }
.section-title { font-size: 15px; font-weight: 600; color: #1a1a1a; margin-bottom: 14px; }

.store-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px; }
.store-card {
  background: #fff; border: 1px solid #e8e8e8; border-radius: 10px;
  padding: 20px; transition: all 0.2s;
}
.store-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.06); }
.store-top { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; }
.store-icon { font-size: 28px; width: 46px; height: 46px; display: flex; align-items: center; justify-content: center; background: #f5f5f5; border-radius: 10px; }
.store-meta { flex: 1; }
.store-name { font-size: 16px; font-weight: 600; color: #1a1a1a; }
.store-addr { font-size: 12px; color: #999; margin-top: 2px; }
.store-details { display: grid; grid-template-columns: repeat(2, 1fr); gap: 6px; }
.detail-row { display: flex; justify-content: space-between; font-size: 12px; padding: 6px 0; border-bottom: 1px solid #f5f5f5; }
.detail-row span:first-child { color: #999; }
.detail-row span:last-child { color: #333; font-weight: 500; }

.dept-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.dept-card {
  background: #fff; border: 1px solid #e8e8e8; border-radius: 10px;
  padding: 18px; text-align: center; transition: all 0.2s;
}
.dept-card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.04); }
.dept-icon { font-size: 28px; margin-bottom: 6px; }
.dept-name { font-size: 14px; font-weight: 600; color: #333; }
.dept-count { font-size: 11px; color: #aaa; margin: 4px 0 10px; }
.dept-positions { display: flex; flex-wrap: wrap; gap: 4px; justify-content: center; }
.pos-tag { font-size: 10px; padding: 2px 8px; background: #f5f5f5; border-radius: 10px; color: #888; }

.org-tree { padding: 24px; background: #fff; border: 1px solid #e8e8e8; border-radius: 10px; }
.tree-level { display: flex; flex-direction: column; align-items: center; gap: 6px; }
.tree-node { display: flex; flex-direction: column; align-items: center; gap: 4px; padding: 14px 20px; background: #f9fafb; border: 1px solid #e8e8e8; border-radius: 8px; min-width: 120px; }
.tree-node.root { border-color: #2D4A3E; border-width: 2px; }
.tree-node.branch { border-color: #4A7C59; }
.tree-node.leaf { padding: 10px 14px; min-width: 100px; }
.node-icon { font-size: 22px; }
.node-label { font-size: 13px; font-weight: 600; color: #333; }
.node-sub { font-size: 11px; color: #aaa; }
.tree-arrow { font-size: 18px; color: #ccc; }
.tree-children { display: flex; gap: 10px; flex-wrap: wrap; justify-content: center; }

@media (max-width: 900px) {
  .store-grid, .dept-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 600px) {
  .store-grid, .dept-grid { grid-template-columns: 1fr; }
}
</style>
