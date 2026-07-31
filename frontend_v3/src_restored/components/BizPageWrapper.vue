<template>
  <div class="biz-page" :class="{ hasContent, noContent: !hasContent }">
    <div class="page-header">
      <h2>{{ title }}</h2>
      <p class="page-desc">{{ subtitle }}</p>
      <el-tag v-if="hasContent" type="success" effect="dark" round>🟢 有内容</el-tag>
      <el-tag v-else type="info" effect="plain" round>⚪ 暂无内容</el-tag>
    </div>

    <div v-if="!hasContent" class="empty-state">
      <span class="empty-icon">{{ icon }}</span>
      <h3>{{ emptyTitle }}</h3>
      <p>{{ emptyDesc }}</p>
    </div>

    <slot v-else />
  </div>
</template>

<script setup>
defineProps({
  title: String,
  subtitle: String,
  icon: String,
  emptyTitle: { type: String, default: '暂无数据' },
  emptyDesc: { type: String, default: '此模块暂无内容' },
  hasContent: { type: Boolean, default: false }
})
</script>

<style scoped>
.biz-page { padding: 24px; height: 100%; transition: 0.3s; }
.biz-page.hasContent {
  background: linear-gradient(135deg, rgba(124,58,237,0.03), rgba(124,58,237,0.01));
}
.biz-page.noContent {
  background: #f8f9fa;
  filter: grayscale(0.3);
  opacity: 0.85;
}

.page-header {
  display: flex; align-items: center; gap: 12px; margin-bottom: 20px;
}
.page-header h2 { font-size: 22px; margin: 0; }
.page-desc { font-size: 13px; color: #9ca3af; margin: 0; flex: 1; }

.empty-state {
  text-align: center; padding: 80px 20px; color: #94a3b8;
}
.empty-icon { font-size: 64px; display: block; margin-bottom: 16px; opacity: 0.5; }
.empty-state h3 { font-size: 18px; color: #64748b; margin: 0 0 8px; }
.empty-state p { font-size: 13px; }
</style>
