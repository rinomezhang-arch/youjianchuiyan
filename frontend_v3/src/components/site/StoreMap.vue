<template>
  <div class="store-map">
    <div v-if="!amapKey" class="map-placeholder">
      <p class="map-placeholder-cn">地图密钥配置中，敬请期待</p>
      <p class="map-placeholder-en">Interactive map key pending configuration</p>
      <div class="map-placeholder-list">
        <div v-for="s in stores" :key="s.storeId" class="map-placeholder-item">
          <strong>{{ s.storeName }}</strong>
          <span>{{ s.address }}</span>
          <a :href="amapUrl(s)" target="_blank" rel="noopener">高德地图 →</a>
        </div>
      </div>
    </div>
    <div v-else ref="mapEl" class="map-canvas"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  stores: { type: Array, required: true } // [{ storeId, storeName, address }]
})

const amapKey = import.meta.env.VITE_AMAP_KEY
const mapEl = ref(null)
let mapInstance = null

function amapUrl(store) {
  return `https://uri.amap.com/search?keyword=${encodeURIComponent(store.address)}`
}

function loadAMapScript() {
  return new Promise((resolve, reject) => {
    if (window.AMap) return resolve(window.AMap)
    const script = document.createElement('script')
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${amapKey}`
    script.onload = () => resolve(window.AMap)
    script.onerror = reject
    document.head.appendChild(script)
  })
}

async function initMap() {
  if (!amapKey || !props.stores.length) return
  try {
    const AMap = await loadAMapScript()
    mapInstance = new AMap.Map(mapEl.value, {
      zoom: 10,
      scrollWheel: true,
      mapStyle: 'amap://styles/whitesmoke'
    })
    const geocoder = new AMap.Geocoder()
    const points = []
    for (const store of props.stores) {
      geocoder.getLocation(store.address, (status, result) => {
        if (status === 'complete' && result.geocodes.length) {
          const loc = result.geocodes[0].location
          points.push(loc)
          const marker = new AMap.Marker({
            position: loc,
            map: mapInstance,
            label: {
              content: `<div class="amap-store-label">${store.storeName}</div>`,
              direction: 'top'
            }
          })
          marker.on('click', () => window.open(amapUrl(store), '_blank'))
          if (points.length === props.stores.length) {
            mapInstance.setFitView()
          }
        }
      })
    }
  } catch (e) {
    // 地图脚本加载失败（网络/Key 问题），静默降级——placeholder 分支已经给了地址兜底
  }
}

onMounted(initMap)
onBeforeUnmount(() => {
  if (mapInstance) mapInstance.destroy()
})
</script>

<style scoped>
.store-map { width: 100%; height: 480px; border-radius: 8px; overflow: hidden; }
.map-canvas { width: 100%; height: 100%; }

.map-placeholder {
  width: 100%; height: 100%; background: #FAF7F0; border: 1px solid #EDE7D9; border-radius: 8px;
  display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 32px;
}
.map-placeholder-cn { font-size: 15px; color: #1F3A2E; font-weight: 600; margin: 0 0 4px; }
.map-placeholder-en { font-size: 11px; color: #9C8F6E; margin: 0 0 28px; letter-spacing: 0.3px; }
.map-placeholder-list { display: flex; gap: 40px; flex-wrap: wrap; justify-content: center; }
.map-placeholder-item { display: flex; flex-direction: column; gap: 6px; text-align: center; }
.map-placeholder-item strong { color: #1F3A2E; font-size: 15px; }
.map-placeholder-item span { color: #7A7A72; font-size: 13px; }
.map-placeholder-item a { color: #B8935A; font-size: 12px; font-weight: 600; }
</style>
