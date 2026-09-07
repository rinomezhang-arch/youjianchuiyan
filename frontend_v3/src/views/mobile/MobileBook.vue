<template>
  <div class="m-book">
    <div v-if="!submitted" class="book-form">
      <div class="form-head">
        <div class="form-title">立即预定</div>
        <div class="form-sub">Reserve a Table · 填好信息，门店会尽快联系您确认</div>
      </div>

      <div class="field">
        <label>选择门店</label>
        <div class="store-pick">
          <button v-for="s in stores" :key="s.store_id" :class="{ active: form.storeId === s.store_id }" @click="form.storeId = s.store_id">
            {{ s.store_short_name || s.store_name }}
          </button>
        </div>
      </div>

      <div class="field-row">
        <div class="field">
          <label>用餐日期</label>
          <input type="date" v-model="form.preferredDate" :min="today" />
        </div>
        <div class="field">
          <label>用餐时间</label>
          <select v-model="form.preferredTime">
            <option value="">不限</option>
            <option v-for="t in timeSlots" :key="t" :value="t">{{ t }}</option>
          </select>
        </div>
      </div>

      <div class="field">
        <label>用餐人数</label>
        <div class="stepper">
          <button @click="adjustGuests(-1)">−</button>
          <input
            class="stepper-input"
            type="number"
            inputmode="numeric"
            pattern="[0-9]*"
            v-model.number="form.guestCount"
            @blur="clampGuests"
          />
          <span class="stepper-unit">位</span>
          <button @click="adjustGuests(1)">＋</button>
        </div>
      </div>

      <div class="field">
        <label>包厢偏好</label>
        <div class="chip-pick">
          <button v-for="opt in roomOptions" :key="opt" :class="{ active: form.roomPref === opt }" @click="form.roomPref = opt">{{ opt }}</button>
        </div>
      </div>

      <div class="field-row">
        <div class="field">
          <label>您的姓名</label>
          <input v-model="form.customerName" placeholder="怎么称呼您" />
        </div>
        <div class="field">
          <label>手机号</label>
          <input v-model="form.customerPhone" type="tel" placeholder="11位手机号" maxlength="11" />
        </div>
      </div>

      <div class="field">
        <label>备注</label>
        <textarea v-model="form.remark" rows="2" placeholder="有什么特别需求，比如忌口、生日惊喜、想吃的菜…"></textarea>
      </div>

      <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>

      <button class="submit-btn" :disabled="submitting" @click="submit">
        {{ submitting ? '提交中…' : '提交预定 · Submit' }}
      </button>
      <p class="submit-hint">提交后门店会主动打电话跟您确认，请保持手机畅通</p>
    </div>

    <div v-else class="book-success">
      <div class="success-icon">✓</div>
      <div class="success-title">预定信息已提交</div>
      <div class="success-sub">Booking request received</div>
      <p class="success-detail">
        {{ pickedStoreName }} · {{ form.preferredDate }}{{ form.preferredTime ? ' ' + form.preferredTime : '' }} · {{ form.guestCount }}位
      </p>
      <p class="success-note">门店会尽快联系 {{ form.customerPhone }} 确认，也可以直接拨打门店电话确认。</p>
      <a v-if="pickedStorePhone" class="success-call" :href="`tel:${pickedStorePhone}`">拨打门店电话</a>
      <button class="success-back" @click="resetForm">再提交一个预定</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import request from '@/utils/request'

const route = useRoute()
const stores = ref([])
const submitting = ref(false)
const submitted = ref(false)
const errorMsg = ref('')

const today = new Date().toISOString().slice(0, 10)
const timeSlots = ['11:00', '11:30', '12:00', '12:30', '17:00', '17:30', '18:00', '18:30', '19:00', '19:30']
const roomOptions = ['不限', '包间', '大厅']

const form = ref({
  storeId: 1,
  preferredDate: today,
  preferredTime: '',
  guestCount: 2,
  roomPref: '不限',
  customerName: '',
  customerPhone: '',
  remark: ''
})

const pickedStoreName = computed(() => {
  const s = stores.value.find(s => s.store_id === form.value.storeId)
  return s ? s.store_name : ''
})
const pickedStorePhone = computed(() => {
  const s = stores.value.find(s => s.store_id === form.value.storeId)
  return s ? s.phone : ''
})

function adjustGuests(delta) {
  const next = form.value.guestCount + delta
  if (next >= 1 && next <= 50) form.value.guestCount = next
}

function clampGuests() {
  let v = Number(form.value.guestCount)
  if (!Number.isFinite(v) || v < 1) v = 1
  if (v > 50) v = 50
  form.value.guestCount = Math.round(v)
}

async function loadStores() {
  try {
    const res = await request.get('/api/public/stores')
    stores.value = res.data || []
    const q = Number(route.query.storeId)
    if (q && stores.value.some(s => s.store_id === q)) form.value.storeId = q
    else if (stores.value.length) form.value.storeId = stores.value[0].store_id
  } catch (e) {
    stores.value = []
  }
}

function resetForm() {
  submitted.value = false
  form.value = {
    storeId: form.value.storeId,
    preferredDate: today,
    preferredTime: '',
    guestCount: 2,
    roomPref: '不限',
    customerName: '',
    customerPhone: '',
    remark: ''
  }
}

async function submit() {
  errorMsg.value = ''
  if (!form.value.customerName.trim()) { errorMsg.value = '请填写姓名'; return }
  if (!/^1[3-9]\d{9}$/.test(form.value.customerPhone)) { errorMsg.value = '请填写正确的11位手机号'; return }

  submitting.value = true
  try {
    let remark = form.value.remark.trim()
    if (form.value.roomPref !== '不限') remark = `[包厢偏好:${form.value.roomPref}] ${remark}`
    if (route.query.dish) remark = `[感兴趣的菜:${route.query.dish}] ${remark}`
    remark = (remark + ' [移动端预定]').trim()

    await request.post('/api/public/booking-inquiry', {
      customerName: form.value.customerName.trim(),
      customerPhone: form.value.customerPhone.trim(),
      storeId: form.value.storeId,
      preferredDate: form.value.preferredDate,
      preferredTime: form.value.preferredTime,
      guestCount: form.value.guestCount,
      remark
    })
    submitted.value = true
  } catch (e) {
    errorMsg.value = e.message || '提交失败，请稍后重试，或直接拨打门店电话'
  } finally {
    submitting.value = false
  }
}

onMounted(loadStores)
</script>

<style scoped>
.m-book {
  --forest: #1F3A2E;
  --gold: #B8935A;
  --ivory: #FAF7F0;
  --ink: #2A2A28;
  --muted: #8A8478;
  min-height: 100%;
  background: var(--ivory);
  padding: 18px 16px 30px;
}

.form-head { margin-bottom: 18px; }
.form-title { font-size: 20px; font-weight: 700; color: var(--forest); }
.form-sub { font-size: 11.5px; color: var(--muted); margin-top: 4px; }

.field { margin-bottom: 16px; }
.field-row { display: flex; gap: 12px; }
.field-row .field { flex: 1; }
.field label { display: block; font-size: 12.5px; color: var(--muted); margin-bottom: 6px; font-weight: 600; }
.field input[type="text"], .field input[type="tel"], .field input[type="date"], .field select, .field textarea {
  width: 100%; box-sizing: border-box; border: 1px solid #E3DBC8; border-radius: 8px;
  padding: 10px 12px; font-size: 14px; background: #fff; outline: none; font-family: inherit;
}
.field textarea { resize: none; }
.field input:focus, .field select:focus, .field textarea:focus { border-color: var(--gold); }

.store-pick, .chip-pick { display: flex; gap: 8px; flex-wrap: wrap; }
.store-pick button, .chip-pick button {
  background: #fff; border: 1px solid #E3DBC8; color: var(--muted);
  padding: 9px 16px; border-radius: 8px; font-size: 13px;
}
.store-pick button.active, .chip-pick button.active { background: var(--forest); border-color: var(--forest); color: #fff; font-weight: 600; }

.stepper { display: flex; align-items: center; gap: 10px; background: #fff; border: 1px solid #E3DBC8; border-radius: 8px; padding: 6px 12px; width: fit-content; }
.stepper button { flex-shrink: 0; width: 28px; height: 28px; border-radius: 50%; border: 1px solid var(--forest); background: #fff; color: var(--forest); font-size: 16px; line-height: 1; }
.stepper-input {
  width: 40px; border: none; outline: none; font-size: 15px; font-weight: 700;
  text-align: center; color: var(--ink); background: none; -moz-appearance: textfield;
}
.stepper-input::-webkit-outer-spin-button, .stepper-input::-webkit-inner-spin-button { -webkit-appearance: none; margin: 0; }
.stepper-unit { font-size: 13px; color: var(--muted); }

.error-msg { color: #C0392B; font-size: 12.5px; margin: -4px 0 12px; }

.submit-btn {
  width: 100%; background: var(--gold); color: #fff; border: none;
  padding: 15px 0; border-radius: 26px; font-size: 15px; font-weight: 700; letter-spacing: 0.5px;
}
.submit-btn:disabled { opacity: 0.6; }
.submit-hint { text-align: center; font-size: 11px; color: var(--muted); margin-top: 10px; }

.book-success { padding: 60px 24px 30px; text-align: center; }
.success-icon {
  width: 60px; height: 60px; border-radius: 50%; background: var(--forest); color: #fff;
  font-size: 28px; display: flex; align-items: center; justify-content: center; margin: 0 auto 16px;
}
.success-title { font-size: 18px; font-weight: 700; color: var(--forest); }
.success-sub { font-size: 11px; color: var(--muted); margin-top: 3px; }
.success-detail { font-size: 13.5px; color: var(--ink); margin: 16px 0 6px; font-weight: 600; }
.success-note { font-size: 12px; color: var(--muted); line-height: 1.6; margin: 0 0 20px; }
.success-call {
  display: inline-block; background: var(--forest); color: #fff; text-decoration: none;
  padding: 11px 26px; border-radius: 22px; font-size: 13.5px; margin-bottom: 14px;
}
.success-back { display: block; width: 100%; background: none; border: 1px solid #E3DBC8; color: var(--muted); padding: 11px 0; border-radius: 22px; font-size: 13px; }
</style>
