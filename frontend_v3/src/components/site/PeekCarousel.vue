<template>
  <div class="peek-carousel">
    <div class="peek-track-wrap">
      <div class="peek-track" :style="{ transform: `translateX(${offset}px)` }">
        <div
          v-for="(item, i) in items"
          :key="i"
          class="peek-card"
          :class="{ active: i === activeIndex }"
          :style="cardStyle"
          @click="goTo(i)"
        >
          <img :src="item.img" :alt="item.alt || ''" />
        </div>
      </div>
    </div>
    <div class="peek-controls">
      <button class="peek-arrow" @click="prev" aria-label="上一张">‹</button>
      <span class="peek-counter">{{ activeIndex + 1 }} / {{ items.length }}</span>
      <button class="peek-arrow" @click="next" aria-label="下一张">›</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  items: { type: Array, required: true },
  cardWidth: { type: Number, default: 520 },
  sideWidth: { type: Number, default: 340 }
})

const activeIndex = ref(0)
const viewportWidth = ref(1200)

function updateViewport() {
  viewportWidth.value = Math.min(window.innerWidth - 64, 1200)
}
onMounted(() => {
  updateViewport()
  window.addEventListener('resize', updateViewport)
})
onUnmounted(() => window.removeEventListener('resize', updateViewport))

const cardStyle = computed(() => ({ width: props.cardWidth + 'px' }))

// 每张卡片占位宽度统一按 cardWidth+gap 计算，居中卡片放大靠 CSS transform，不改变布局宽度
const gap = 24
const slotWidth = computed(() => props.cardWidth + gap)
const offset = computed(() => {
  const center = viewportWidth.value / 2
  return center - slotWidth.value * activeIndex.value - props.cardWidth / 2
})

function next() {
  activeIndex.value = (activeIndex.value + 1) % props.items.length
}
function prev() {
  activeIndex.value = (activeIndex.value - 1 + props.items.length) % props.items.length
}
function goTo(i) {
  activeIndex.value = i
}
</script>

<style scoped>
.peek-carousel { width: 100%; }
.peek-track-wrap { overflow: hidden; padding: 20px 0; }
.peek-track { display: flex; gap: 24px; transition: transform 0.5s cubic-bezier(0.4, 0, 0.2, 1); }
.peek-card {
  flex-shrink: 0; height: 340px; border-radius: 8px; overflow: hidden; cursor: pointer;
  opacity: 0.55; transform: scale(0.82); transition: opacity 0.5s ease, transform 0.5s ease;
  box-shadow: 0 8px 30px rgba(0,0,0,0.1);
}
.peek-card.active { opacity: 1; transform: scale(1); box-shadow: 0 16px 50px rgba(0,0,0,0.18); }
.peek-card img { width: 100%; height: 100%; object-fit: cover; display: block; }

.peek-controls { display: flex; align-items: center; justify-content: center; gap: 24px; margin-top: 8px; }
.peek-arrow {
  width: 40px; height: 40px; border-radius: 50%; border: 1px solid #DDD3B8; background: #fff;
  color: #1F3A2E; font-size: 20px; cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: all 0.2s;
}
.peek-arrow:hover { background: #1F3A2E; color: #fff; border-color: #1F3A2E; }
.peek-counter { font-size: 13px; color: #7A7A72; letter-spacing: 1px; min-width: 48px; text-align: center; }

@media (max-width: 720px) {
  .peek-card { height: 240px; }
}
</style>
