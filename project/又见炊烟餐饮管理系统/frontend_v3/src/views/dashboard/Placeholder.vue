<template>
  <div class="empty-state">
    <div class="empty-icon">🚧</div>
    <h3>{{ title }}</h3>
    <p style="color: var(--color-text-muted); margin-top: 8px;">{{ desc }}</p>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, default: '功能开发中' },
  desc: { type: String, default: '即将上线，敬请期待' }
})
</script>
