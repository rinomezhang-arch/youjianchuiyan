<template>
  <svg
    class="site-icon"
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    :stroke-width="strokeWidth"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
    focusable="false"
  >
    <template v-if="name === 'pin'">
      <path d="M20 10c0 5-8 12-8 12s-8-7-8-12a8 8 0 0 1 16 0Z" />
      <circle cx="12" cy="10" r="2.75" />
    </template>

    <template v-else-if="name === 'clock'">
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7.5V12l3 1.75" />
    </template>

    <template v-else-if="name === 'phone'">
      <path d="M6.2 3.5h3l1.5 3.8-2 1.4a12 12 0 0 0 6.6 6.6l1.4-2 3.8 1.5v3a1.7 1.7 0 0 1-1.9 1.7A16.8 16.8 0 0 1 4.5 5.4 1.7 1.7 0 0 1 6.2 3.5Z" />
    </template>

    <template v-else-if="name === 'arrow-right'">
      <path d="M4.5 12h15" />
      <path d="m13.5 6 6 6-6 6" />
    </template>

    <template v-else-if="name === 'chevron-down'">
      <path d="m6 9.5 6 6 6-6" />
    </template>

    <template v-else-if="name === 'chevron-right'">
      <path d="m9.5 6 6 6-6 6" />
    </template>

    <template v-else-if="name === 'search'">
      <circle cx="11" cy="11" r="6.5" />
      <path d="m20 20-4.4-4.4" />
    </template>

    <template v-else-if="name === 'basket'">
      <path d="M3.5 8.5h17l-1.6 10a2 2 0 0 1-2 1.7H7.1a2 2 0 0 1-2-1.7Z" />
      <path d="m8.5 8.5 3-5 3 5" />
    </template>

    <template v-else-if="name === 'calendar'">
      <rect x="3.5" y="5" width="17" height="16" rx="1.5" />
      <path d="M3.5 10h17M8 3v4M16 3v4" />
    </template>

    <template v-else-if="name === 'users'">
      <circle cx="9" cy="8.5" r="3.5" />
      <path d="M2.8 20a6.2 6.2 0 0 1 12.4 0" />
      <path d="M16.5 5.4a3.5 3.5 0 0 1 0 6.7M17.5 14.3a6.2 6.2 0 0 1 3.7 5.7" />
    </template>

    <template v-else-if="name === 'check'">
      <path d="m4.5 12.5 5 5 10-11" />
    </template>

    <template v-else-if="name === 'close'">
      <path d="m6 6 12 12M18 6 6 18" />
    </template>

    <template v-else-if="name === 'plus'">
      <path d="M12 5v14M5 12h14" />
    </template>

    <template v-else-if="name === 'minus'">
      <path d="M5 12h14" />
    </template>
  </svg>
</template>

<script setup>
/**
 * 对客页面的图标。
 *
 * 为什么专门做这个：原来页面里用的是 📍🕐 这类 emoji。
 * emoji 在每个系统上都是另一套彩色字形，粗细、基线、颜色全不受控——
 * 一行文字里混一个彩色小图，再讲究的排版也立刻掉价。
 * 换成单色描边 SVG：颜色跟着 currentColor 走，粗细统一，基线可控。
 *
 * 尺寸默认 16，与 13~15px 正文并排时视觉重量正好；
 * 描边 1.5 而不是 2，避免小尺寸下显得笨重。
 */
defineProps({
  name: { type: String, required: true },
  size: { type: [Number, String], default: 16 },
  strokeWidth: { type: [Number, String], default: 1.5 }
})
</script>

<style scoped>
.site-icon {
  flex-shrink: 0;
  /* 与中文基线对齐：图标视觉中心比文字中心略高，压下来一点才不显得飘 */
  vertical-align: -0.18em;
}
</style>
