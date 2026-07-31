<template>
  <div class="ipad-page">
    <div class="page-top">
      <button class="back-link" @click="router.push('/ipad/home')">← 返回桌台</button>
      <h1 class="page-title">套餐 · Packages</h1>
    </div>
    <div class="pkg-grid">
      <div v-for="pkg in packages" :key="pkg.package_id" class="pkg-card">
        <div class="pkg-header">
          <h3>{{ pkg.package_name }}</h3>
          <div class="pkg-price">¥{{ Number(pkg.package_price).toFixed(0) }}</div>
        </div>
        <div class="pkg-dishes">
          <div v-for="d in pkg.dish_list" :key="d.dish_id" class="pkg-dish">
            <span>{{ d.dish_name }}</span>
            <span class="dish-qty">×{{ d.dish_quantity }}</span>
          </div>
        </div>
        <button class="pkg-btn" @click="addPackage(pkg)">选择套餐</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useIpadStore } from '@/store/ipad'
import { ipadPackageList } from '@/api/ipad'
import { ElMessage } from 'element-plus'

const router = useRouter()
const ipad = useIpadStore()
const packages = ref([])

function addPackage(pkg) {
  pkg.dish_list?.forEach(d => {
    ipad.addToCart({ dish_id: d.dish_id, dish_name: d.dish_name, sale_price: d.unit_price || 0, unit_price: d.unit_price || 0, dish_quantity: d.dish_quantity })
  })
  ElMessage.success('套餐已加入')
}

onMounted(async () => {
  try {
    const res = await ipadPackageList()
    if (res.code === 200) packages.value = res.data || []
  } catch {
    packages.value = [
      { package_id: 'P001', package_name: '迎春接福宴', package_price: 988, dish_list: [{ dish_id: 'D001', dish_name: '红烧肉', dish_quantity: 1 }, { dish_id: 'D002', dish_name: '清蒸鲈鱼', dish_quantity: 1 }] },
      { package_id: 'P002', package_name: '阖家团圆宴', package_price: 1288, dish_list: [{ dish_id: 'D003', dish_name: '蒜蓉粉丝蒸扇贝', dish_quantity: 1 }] },
    ]
  }
})
</script>

<style scoped>
.ipad-page { width: 100%; height: 100%; display: flex; flex-direction: column; background: var(--color-bg); }
.page-top { padding: 16px 24px; background: var(--color-card); border-bottom: 1px solid var(--color-border); display: flex; align-items: center; gap: 16px; flex-shrink: 0; }
.back-link { border: none; background: none; color: var(--color-text-muted); font-size: 13px; cursor: pointer; }
.page-title { font-size: 18px; font-weight: 700; color: var(--color-text); letter-spacing: 2px; }
.pkg-grid { flex: 1; overflow-y: auto; padding: 24px; display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px; align-content: start; }
.pkg-card { background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-lg); padding: 20px; }
.pkg-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid var(--color-border-light); }
.pkg-header h3 { font-size: 18px; font-weight: 700; color: var(--color-text); }
.pkg-price { font-size: 24px; font-weight: 700; color: var(--color-accent-dark); }
.pkg-dishes { margin-bottom: 16px; }
.pkg-dish { display: flex; justify-content: space-between; padding: 6px 0; font-size: 14px; color: var(--color-text-secondary); }
.dish-qty { color: var(--color-text-muted); }
.pkg-btn { width: 100%; padding: 12px; border: 1px solid var(--color-primary); border-radius: var(--radius-md); background: transparent; color: var(--color-primary); font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.2s; }
.pkg-btn:hover { background: var(--color-primary); color: white; }
</style>
