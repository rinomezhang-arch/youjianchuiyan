<template>
  <div class="biz-page" :class="{ hasContent, noContent: !hasContent }">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">{{ title }}</h2>
        <p class="page-desc">{{ subtitle }}</p>
      </div>
      <div class="header-right">
        <slot name="actions" />
      </div>
    </div>

    <div v-if="!hasContent" class="empty-state">
      <div class="empty-text-block">
        <span class="empty-title-text">{{ emptyTitle }}</span>
        <span class="empty-desc-text">{{ emptyDesc }}</span>
      </div>
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
.biz-page {
  padding: 24px 32px;
  height: 100%;
  min-height: calc(100vh - 120px);
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 24px;
  gap: 16px;
  flex-wrap: wrap;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a2f23;
  margin: 0;
  letter-spacing: 0.5px;
}

.page-desc {
  font-size: 13px;
  color: #8a9a8e;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 80px 24px;
  text-align: center;
  background: var(--color-card, #fff);
  border: 1px solid var(--color-border, #e8ece9);
  border-radius: 14px;
}

.empty-text-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
}

.empty-title-text {
  font-size: 18px;
  font-weight: 600;
  color: #5a6a5e;
}

.empty-desc-text {
  font-size: 13px;
  color: #a0b0a5;
}
</style>
