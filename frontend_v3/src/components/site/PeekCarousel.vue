<template>
  <div class="peek-carousel">
    <div class="peek-track-wrap" ref="trackWrapEl">
      <div class="peek-track" :style="{ transform: `translateX(${offset}px)` }">
        <div
          v-for="(item, i) in displayItems"
          :key="i"
          class="peek-card"
          :class="{ active: i === activeIndex + 1 }"
          :style="cardStyle"
          @click="goToDisplay(i)"
        >
          <img :src="item.img" :alt="item.alt || ''" />
        </div>
      </div>
    </div>
    <div class="peek-controls">
      <button class="peek-arrow" @click="prev" aria-label="上一张">‹</button>
      <span class="peek-dots">
        <i v-for="(item, i) in items" :key="i" :class="{ on: i === activeIndex }" @click="goTo(i)"></i>
      </span>
      <button class="peek-arrow" @click="next" aria-label="下一张">›</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  items: { type: Array, required: true },
  cardWidth: { type: Number, default: 520 }
})

const activeIndex = ref(0)
const trackWrapEl = ref(null)
const wrapWidth = ref(1200)
let resizeObserver = null

onMounted(() => {
  if (trackWrapEl.value) {
    wrapWidth.value = trackWrapEl.value.clientWidth
    resizeObserver = new ResizeObserver((entries) => {
      wrapWidth.value = entries[0].contentRect.width
    })
    resizeObserver.observe(trackWrapEl.value)
  }
})
onUnmounted(() => resizeObserver && resizeObserver.disconnect())

const cardStyle = computed(() => ({ width: props.cardWidth + 'px' }))

// 首尾各克隆一张实际图片垫在轨道两端，这样第一张/最后一张打开也能在左右两侧看到真实的"预览"，不是空白
const displayItems = computed(() => {
  const n = props.items.length
  if (n === 0) return []
  return [props.items[n - 1], ...props.items, props.items[0]]
})

// 用轨道容器自身的实测宽度居中，而不是猜 window 宽度减一个固定 padding——
// 猜的宽度和容器实际渲染宽度对不上时，左侧预览卡会被 overflow:hidden 直接裁掉，等于"永远看不见"
const gap = 24
const slotWidth = computed(() => props.cardWidth + gap)
const offset = computed(() => {
  const center = wrapWidth.value / 2
  return center - slotWidth.value * (activeIndex.value + 1) - props.cardWidth / 2
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
function goToDisplay(displayIndex) {
  const n = props.items.length
  if (displayIndex === 0) return prev()
  if (displayIndex === n + 1) return next()
  activeIndex.value = displayIndex - 1
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

.peek-controls { display: flex; align-items: center; justify-content: center; gap: 20px; margin-top: 8px; }
.peek-arrow {
  width: 40px; height: 40px; border-radius: 50%; border: 1px solid #DDD3B8; background: #fff;
  color: #1F3A2E; font-size: 20px; cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: all 0.2s;
}
.peek-arrow:hover { background: #1F3A2E; color: #fff; border-color: #1F3A2E; }
.peek-dots { display: flex; align-items: center; gap: 8px; }
.peek-dots i {
  display: block; width: 7px; height: 7px; border-radius: 50%; background: #DDD3B8;
  cursor: pointer; transition: all 0.25s ease;
}
.peek-dots i.on { width: 20px; border-radius: 4px; background: #B8935A; }

@media (max-width: 720px) {
  .peek-card { height: 240px; }
}
</style>
