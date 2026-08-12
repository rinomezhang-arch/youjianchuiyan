<template>
  <Transition name="modal">
    <div v-if="visible" class="modal-overlay" @click.self="$emit('close')">
      <div class="search-modal">
        <div class="modal-header">
          <h3>全局搜索 · Global Search</h3>
          <button class="close-btn" @click="$emit('close')">✕</button>
        </div>
        <div class="search-box">
          <input v-model="keyword" placeholder="搜索菜品、桌台、订单..." @input="doSearch" ref="inputRef" />
        </div>
        <div class="search-results" v-if="results.length">
          <div
            v-for="item in results"
            :key="item.dish_id || item.table_id || item.booking_id"
            class="result-item"
            @click="selectItem(item)"
          >
            <span class="result-type">{{ itemType(item) }}</span>
            <span class="result-name">{{ item.dish_name || item.table_name || item.customer_name || item.booking_id }}</span>
            <span v-if="item.sale_price" class="result-price">¥{{ Number(item.sale_price).toFixed(0) }}</span>
          </div>
        </div>
        <div v-else-if="keyword && !searching" class="no-results">无匹配结果</div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { ipadDishSearch } from '@/api/ipad'

const props = defineProps({ visible: Boolean })
const emit = defineEmits(['close', 'select'])

const keyword = ref('')
const results = ref([])
const searching = ref(false)
const inputRef = ref(null)

watch(() => props.visible, async (v) => {
  if (v) { await nextTick(); inputRef.value?.focus() }
  else { keyword.value = ''; results.value = [] }
})

let timer = null
function doSearch() {
  clearTimeout(timer)
  if (!keyword.value.trim()) { results.value = []; return }
  timer = setTimeout(async () => {
    searching.value = true
    try {
      const res = await ipadDishSearch(keyword.value)
      results.value = res.code === 200 ? (res.data || []) : []
    } catch {
      results.value = []
    }
    searching.value = false
  }, 300)
}

function itemType(item) {
  if (item.dish_name) return '菜品'
  if (item.table_name) return '桌台'
  if (item.booking_id) return '订单'
  return '其他'
}

function selectItem(item) { emit('select', item); emit('close') }
</script>

<style scoped>
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.3); display: flex; align-items: flex-start; justify-content: center; padding-top: 80px; z-index: 1000; }
.search-modal { width: 500px; max-height: 70vh; background: var(--color-card); border-radius: var(--radius-lg); box-shadow: var(--shadow-xl); display: flex; flex-direction: column; overflow: hidden; }
.modal-header { padding: 16px 20px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--color-border); }
.modal-header h3 { font-size: 16px; font-weight: 700; color: var(--color-text); }
.close-btn { background: none; border: none; font-size: 20px; cursor: pointer; color: var(--color-text-muted); }
.search-box { padding: 12px 20px; }
.search-box input { width: 100%; padding: 12px 16px; border: 1px solid var(--color-border); border-radius: var(--radius-md); font-size: 15px; background: var(--color-bg); }
.search-box input:focus { border-color: var(--color-primary); outline: none; }
.search-results { flex: 1; overflow-y: auto; padding: 0 20px 12px; }
.result-item { display: flex; align-items: center; gap: 12px; padding: 12px 0; border-bottom: 1px solid var(--color-border-light); cursor: pointer; }
.result-item:hover { color: var(--color-primary); }
.result-type { font-size: 11px; padding: 2px 6px; border-radius: 3px; background: var(--color-bg-alt); color: var(--color-text-muted); }
.result-name { flex: 1; font-size: 15px; font-weight: 500; }
.result-price { font-size: 14px; font-weight: 700; color: var(--color-accent-dark); }
.no-results { padding: 24px; text-align: center; color: var(--color-text-muted); font-size: 14px; }

.modal-enter-active, .modal-leave-active { transition: opacity 0.25s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
</style>
