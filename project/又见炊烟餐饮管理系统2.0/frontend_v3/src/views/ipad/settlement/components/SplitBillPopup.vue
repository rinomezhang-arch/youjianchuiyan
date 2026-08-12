<template>
  <Transition name="modal">
    <div v-if="visible" class="modal-overlay" @click.self="$emit('close')">
      <div class="modal-box">
        <div class="modal-header">
          <h3>分账 · Split Bill</h3>
          <span class="modal-sub">总金额 ¥{{ totalAmount.toFixed(2) }} · {{ personCount }}人</span>
        </div>
        <div class="modal-body">
          <div class="split-options">
            <button :class="['split-mode', { active: mode === 'equal' }]" @click="setEqual">
              平摊 · Equal
            </button>
            <button :class="['split-mode', { active: mode === 'custom' }]" @click="mode = 'custom'">
              自定义 · Custom
            </button>
            <button :class="['split-mode', { active: mode === 'dishes' }]" @click="mode = 'dishes'">
              按菜分 · By Dish
            </button>
          </div>

          <!-- 平摊模式 -->
          <div v-if="mode === 'equal'" class="equal-result">
            <div v-for="(p, i) in persons" :key="i" class="person-row">
              <span class="person-index">客人{{ i + 1 }}</span>
              <span class="person-amount">¥{{ equalAmount.toFixed(2) }}</span>
            </div>
          </div>

          <!-- 自定义金额 -->
          <div v-if="mode === 'custom'" class="custom-amounts">
            <div v-for="(p, i) in persons" :key="i" class="person-row editable">
              <span class="person-index">客人{{ i + 1 }}</span>
              <input v-model.number="persons[i]" type="number" class="person-input" placeholder="0.00" />
              <span class="person-leftover" v-if="i === persons.length - 1 && customRemainder !== 0">
                {{ customRemainder > 0 ? '余' : '超' }} ¥{{ Math.abs(customRemainder).toFixed(2) }}
              </span>
            </div>
          </div>

          <!-- 按菜分 -->
          <div v-if="mode === 'dishes'" class="dishes-split">
            <div class="dish-split-header">将菜品分配给各人：</div>
            <div v-for="(d, di) in dishList" :key="di" class="dish-split-row">
              <span class="ds-name">{{ d.dish_name }}</span>
              <span class="ds-price">¥{{ Number(d.subtotal || d.sale_price * d.dish_quantity).toFixed(0) }}</span>
              <select v-model="dishAssign[di]" class="ds-select">
                <option :value="null">—</option>
                <option v-for="(p, pi) in persons" :key="pi" :value="pi">客人{{ pi + 1 }}</option>
              </select>
            </div>
            <div class="dishes-summary">
              <div v-for="(p, pi) in persons" :key="pi" class="ds-person-total">
                客人{{ pi + 1 }}: ¥{{ dishPersonTotal(pi).toFixed(2) }}
              </div>
            </div>
          </div>

          <!-- 人员数量 -->
          <div class="person-count-row">
            <button @click="removePerson" :disabled="persons.length <= 2">−</button>
            <span>{{ persons.length }}人</span>
            <button @click="addPerson">+</button>
          </div>
        </div>
        <div class="modal-actions">
          <button class="btn-cancel" @click="$emit('close')">取消</button>
          <button class="btn-confirm" @click="confirmSplit" :disabled="!isValid">确认分账</button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  visible: Boolean,
  totalAmount: { type: Number, default: 0 },
  dishList: { type: Array, default: () => [] },
})
const emit = defineEmits(['close', 'done'])

const mode = ref('equal')
const persons = ref([0, 0])
const dishAssign = ref([])

watch(() => props.dishList, () => {
  dishAssign.value = props.dishList.map(() => null)
}, { immediate: true })

const personCount = computed(() => persons.value.length)

const equalAmount = computed(() =>
  props.totalAmount / persons.value.length
)

const customSum = computed(() =>
  persons.value.reduce((s, v) => s + (Number(v) || 0), 0)
)

const customRemainder = computed(() =>
  props.totalAmount - customSum.value
)

const isValid = computed(() => {
  if (mode.value === 'equal') return true
  if (mode.value === 'custom') return Math.abs(customRemainder.value) < 0.01
  if (mode.value === 'dishes') return dishAssign.value.every(a => a !== null)
  return false
})

function setEqual() {
  mode.value = 'equal'
  persons.value = persons.value.map(() => equalAmount.value)
}

function addPerson() {
  if (mode.value === 'equal') persons.value.push(0)
  else persons.value.push(0)
}

function removePerson() {
  if (persons.value.length > 2) persons.value.pop()
}

function dishPersonTotal(pi) {
  return props.dishList.reduce((sum, d, di) => {
    if (dishAssign.value[di] === pi) {
      return sum + Number(d.subtotal || d.sale_price * d.dish_quantity)
    }
    return sum
  }, 0)
}

function confirmSplit() {
  if (!isValid.value) {
    ElMessage.warning('请完成分账配置')
    return
  }
  let result
  if (mode.value === 'equal') {
    result = persons.value.map((_, i) => ({
      person: i + 1, amount: equalAmount.value, share: 'equal'
    }))
  } else if (mode.value === 'custom') {
    result = persons.value.map((v, i) => ({
      person: i + 1, amount: Number(v) || 0, share: 'custom'
    }))
  } else {
    result = persons.value.map((_, pi) => ({
      person: pi + 1,
      amount: dishPersonTotal(pi),
      share: 'dishes',
      dishes: props.dishList
        .filter((_, di) => dishAssign.value[di] === pi)
        .map(d => d.dish_name)
    }))
  }
  ElMessage.success('分账完成')
  emit('done', result)
}
</script>

<style scoped>
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 300; }
.modal-box { background: var(--color-card); border-radius: var(--radius-xl); width: 480px; max-width: 90vw; max-height: 80vh; overflow-y: auto; box-shadow: var(--shadow-xl); }
.modal-header { padding: 20px 24px 0; }
.modal-header h3 { font-size: 18px; font-weight: 700; color: var(--color-text); letter-spacing: 1px; }
.modal-sub { font-size: 12px; color: var(--color-text-muted); display: block; margin-top: 4px; }
.modal-body { padding: 16px 24px; }

.split-options { display: flex; gap: 8px; margin-bottom: 16px; }
.split-mode { flex: 1; padding: 10px; border: 2px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-card); font-size: 13px; cursor: pointer; transition: all 0.15s; }
.split-mode.active { border-color: var(--color-primary); background: rgba(45,74,62,0.05); font-weight: 600; }

.equal-result, .custom-amounts { display: flex; flex-direction: column; gap: 8px; margin-bottom: 16px; }
.person-row { display: flex; align-items: center; justify-content: space-between; padding: 8px 12px; background: var(--color-bg-alt); border-radius: var(--radius-md); }
.person-row.editable { gap: 10px; }
.person-index { font-size: 14px; font-weight: 500; color: var(--color-text); }
.person-amount { font-size: 16px; font-weight: 700; color: var(--color-accent-dark); }
.person-input { width: 100px; padding: 6px 10px; border: 1px solid var(--color-border); border-radius: var(--radius-sm); font-size: 14px; text-align: right; }
.person-leftover { font-size: 12px; color: var(--color-danger); }

.dishes-split { margin-bottom: 16px; }
.dish-split-header { font-size: 13px; color: var(--color-text-muted); margin-bottom: 8px; }
.dish-split-row { display: flex; align-items: center; gap: 8px; padding: 6px 8px; border-bottom: 1px solid var(--color-border-light); }
.ds-name { flex: 1; font-size: 13px; color: var(--color-text); }
.ds-price { font-size: 13px; font-weight: 600; color: var(--color-text-secondary); }
.ds-select { padding: 4px 8px; border: 1px solid var(--color-border); border-radius: var(--radius-sm); font-size: 13px; background: var(--color-card); }
.dishes-summary { margin-top: 10px; padding: 10px; background: var(--color-bg-alt); border-radius: var(--radius-md); }
.ds-person-total { font-size: 14px; font-weight: 600; color: var(--color-text); padding: 4px 0; }

.person-count-row { display: flex; align-items: center; gap: 16px; justify-content: center; margin-top: 8px; }
.person-count-row button { width: 32px; height: 32px; border-radius: 50%; border: 1px solid var(--color-border); background: var(--color-card); font-size: 18px; cursor: pointer; }
.person-count-row button:disabled { opacity: 0.3; cursor: not-allowed; }
.person-count-row span { font-size: 16px; font-weight: 700; min-width: 30px; text-align: center; }

.modal-actions { display: flex; gap: 10px; padding: 16px 24px 20px; justify-content: flex-end; }
.btn-cancel { padding: 12px 24px; border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-card); font-size: 14px; cursor: pointer; }
.btn-confirm { padding: 12px 24px; border: none; border-radius: var(--radius-md); background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light)); color: white; font-size: 14px; font-weight: 700; cursor: pointer; }
.btn-confirm:disabled { opacity: 0.4; cursor: not-allowed; }

.modal-enter-active, .modal-leave-active { transition: opacity 0.2s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
</style>
