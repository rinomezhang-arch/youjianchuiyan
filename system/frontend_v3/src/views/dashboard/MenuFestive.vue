<template>
  <EmptyState title="节日菜单 · Festive Menu" desc="特殊节日套餐,从 package_master 按 tag=festive 筛选" action-path="/dashboard/packages" action-label="查看套餐" :stats="stats" />
</template>
<script setup>
import { ref, onMounted } from 'vue'
import EmptyState from '@/components/EmptyState.vue'
import request from '@/utils/request'
const stats = ref({})
async function load() {
  try { const r = await request.get('/packages', { params: { tag: 'festive', size: 1 } }); stats.value = { '节日套餐数': r?.data?.total || 0 } } catch (e) {}
}
onMounted(load)
</script>
