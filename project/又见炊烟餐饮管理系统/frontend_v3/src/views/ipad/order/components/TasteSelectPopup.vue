<template>
  <Transition name="modal">
    <div v-if="visible" class="modal-overlay" @click.self="emit('close')">
      <div class="taste-modal">
        <div class="modal-header">
          <h3>口味偏好 · Flavor</h3>
          <button class="close-btn" @click="emit('close')">✕</button>
        </div>
        <div class="modal-body">
          <div class="section">
            <h4>辣度 · Spicy</h4>
            <div class="option-row">
              <button v-for="(label, i) in ['不辣','微辣','中辣','辣','特辣']" :key="i"
                :class="['opt-btn', { active: spicy === i }]" @click="spicy = i">
                {{ '🌶'.repeat(i) }} {{ label }}
              </button>
            </div>
          </div>
          <div class="section">
            <h4>熟度 · Doneness</h4>
            <div class="option-row">
              <button v-for="d in ['默认','三分','五分','七分','全熟']" :key="d"
                :class="['opt-btn', { active: doneness === d }]" @click="doneness = d">{{ d }}</button>
            </div>
          </div>
          <div class="section">
            <h4>忌口 · Avoid</h4>
            <div class="option-row">
              <button v-for="a in ['葱','姜','蒜','香菜','辣椒','无']" :key="a"
                :class="['opt-btn sm', { active: avoids.includes(a) }]" @click="toggleAvoid(a)">{{ a }}</button>
            </div>
          </div>
          <div class="custom-note">
            <input v-model="customNote" placeholder="其他备注..." />
          </div>
          <button class="confirm-btn" @click="handleConfirm">确认 · OK</button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({ visible: Boolean })
const emit = defineEmits(['close', 'confirm'])

const spicy = ref(0)
const doneness = ref('默认')
const avoids = ref([])
const customNote = ref('')

watch(() => props.visible, (v) => { if (v) { spicy.value = 0; doneness.value = '默认'; avoids.value = []; customNote.value = '' } })

function toggleAvoid(v) {
  const idx = avoids.value.indexOf(v)
  if (idx > -1) avoids.value.splice(idx, 1)
  else avoids.value.push(v)
}

function handleConfirm() {
  const note = [doneness.value !== '默认' ? doneness.value : '', avoids.value.length ? `忌口:${avoids.value.join('、')}` : '', customNote.value].filter(Boolean).join('；')
  emit('confirm', { spicy: spicy.value, doneness: doneness.value, avoids: avoids.value, note })
}
</script>

<style scoped>
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.3); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.taste-modal { width: 420px; background: var(--color-card); border-radius: var(--radius-lg); box-shadow: var(--shadow-xl); overflow: hidden; }
.modal-header { padding: 16px 20px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--color-border); }
.modal-header h3 { font-size: 16px; font-weight: 700; }
.close-btn { background: none; border: none; font-size: 20px; cursor: pointer; color: var(--color-text-muted); }
.modal-body { padding: 20px; }
.section { margin-bottom: 16px; }
.section h4 { font-size: 14px; font-weight: 600; color: var(--color-text-secondary); margin-bottom: 8px; }
.option-row { display: flex; flex-wrap: wrap; gap: 6px; }
.opt-btn { padding: 6px 12px; border: 1px solid var(--color-border); border-radius: 20px; background: var(--color-card); font-size: 13px; cursor: pointer; white-space: nowrap; color: var(--color-text); }
.opt-btn:hover { border-color: var(--color-primary); }
.opt-btn.active { background: var(--color-primary); color: white; border-color: var(--color-primary); }
.opt-btn.sm { padding: 4px 10px; font-size: 12px; }
.custom-note input { width: 100%; padding: 10px 14px; border: 1px solid var(--color-border); border-radius: var(--radius-md); font-size: 14px; margin-bottom: 16px; }
.custom-note input:focus { border-color: var(--color-primary); outline: none; }
.confirm-btn { width: 100%; padding: 14px; border: none; border-radius: var(--radius-md); background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light)); color: white; font-size: 16px; font-weight: 700; cursor: pointer; }

.modal-enter-active, .modal-leave-active { transition: all 0.25s; }
.modal-enter-from { opacity: 0; transform: scale(0.95); }
.modal-leave-to { opacity: 0; transform: scale(0.95); }
</style>
