<template>
  <nav class="site-breadcrumb" aria-label="面包屑">
    <div class="crumb-inner">
      <template v-for="(item, i) in items" :key="i">
        <a v-if="item.to" class="crumb-link" @click="$router.push(item.to)">{{ item.label }}</a>
        <span v-else class="crumb-current" aria-current="page">{{ item.label }}</span>
        <SiteIcon v-if="i < items.length - 1" name="chevron-right" :size="12" class="crumb-sep" />
      </template>
    </div>
  </nav>
</template>

<script setup>
import SiteIcon from '@/components/site/SiteIcon.vue'

defineProps({
  /**
   * [{ label: '首页', to: '/' }, { label: '门店', to: '/stores' }, { label: '宁国店' }]
   *
   * 仍然接受调用方传 en，但不再渲染。面包屑本来就是"你在哪儿"的一行小字，
   * 每一节后面再挂一串英文，读的人得先跳过噪点才能找到自己的位置。
   * 保留这个字段只是为了不逼着所有页面同一次改完。
   */
  items: { type: Array, required: true }
})
</script>

<style scoped>
.site-breadcrumb {
  background: var(--site-paper);
  border-bottom: 1px solid var(--site-line);
  /* 顶部留白要压过固定导航的高度，否则第一行字会被导航吃掉 */
  padding: 104px var(--site-gutter) 16px;
}
.crumb-inner {
  max-width: var(--site-max);
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--site-fs-small);
  color: var(--site-ink-3);
}
.crumb-link {
  cursor: pointer;
  color: var(--site-ink-3);
  transition: color var(--site-dur) var(--site-ease);
}
.crumb-link:hover { color: var(--site-pine); }
.crumb-current { color: var(--site-ink); }
.crumb-sep { color: var(--site-line-strong); }

@media (max-width: 960px) {
  .site-breadcrumb { padding-top: 78px; }
}
</style>
