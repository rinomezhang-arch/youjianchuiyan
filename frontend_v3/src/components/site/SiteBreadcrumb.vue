<template>
  <div class="site-breadcrumb">
    <div class="crumb-inner">
      <template v-for="(item, i) in items" :key="i">
        <a v-if="item.to" class="crumb-link" @click="$router.push(item.to)">
          {{ item.label }}<span v-if="item.en" class="crumb-en"> · {{ item.en }}</span>
        </a>
        <span v-else class="crumb-current">
          {{ item.label }}<span v-if="item.en" class="crumb-en"> · {{ item.en }}</span>
        </span>
        <span v-if="i < items.length - 1" class="crumb-sep">›</span>
      </template>
    </div>
  </div>
</template>

<script setup>
defineProps({
  // [{ label: '首页', en: 'Home', to: '/' }, { label: '门店选择', en: 'Restaurants', to: '/stores' }, { label: '宁国店' }]
  items: { type: Array, required: true }
})
</script>

<style scoped>
.site-breadcrumb {
  background: #FAF7F0;
  border-bottom: 1px solid #EDE7D9;
  padding: 100px 40px 18px;
}
.crumb-inner {
  max-width: 1200px; margin: 0 auto;
  display: flex; align-items: center; gap: 10px;
  font-size: 13px; color: #7A7A72;
}
.crumb-link { cursor: pointer; color: #7A7A72; }
.crumb-link:hover { color: #B8935A; }
.crumb-current { color: #1F3A2E; font-weight: 600; }
.crumb-sep { color: #C9BFA0; }
.crumb-en { font-size: 11px; color: #9C8F6E; letter-spacing: 0.3px; }
</style>
