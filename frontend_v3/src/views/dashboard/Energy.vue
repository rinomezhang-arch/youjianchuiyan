<template>
  <div class="energy-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">能耗管理 · Energy Management</h2>
        <p class="page-subtitle">Electricity, water and gas consumption tracking</p>
      </div>
      <button class="btn-primary" @click="showAddDialog = true">+ 录入读数</button>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon" style="background:rgba(212,168,83,0.08)">
          <svg viewBox="0 0 24 24" fill="none" stroke="#D4A853" stroke-width="2">
            <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">本月用电 · Electricity</div>
          <div class="stat-value" style="color:#D4A853">{{ currentMonth.electric }} <span class="unit">kWh</span></div>
          <div class="stat-sub">较上月 {{ currentMonth.electricChange > 0 ? '+' : '' }}{{ currentMonth.electricChange }}%</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:rgba(91,123,138,0.08)">
          <svg viewBox="0 0 24 24" fill="none" stroke="#5B7B8A" stroke-width="2">
            <path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">本月用水 · Water</div>
          <div class="stat-value" style="color:#5B7B8A">{{ currentMonth.water }} <span class="unit">吨</span></div>
          <div class="stat-sub">较上月 {{ currentMonth.waterChange > 0 ? '+' : '' }}{{ currentMonth.waterChange }}%</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:rgba(192,57,43,0.08)">
          <svg viewBox="0 0 24 24" fill="none" stroke="#C0392B" stroke-width="2">
            <path d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">本月用气 · Gas</div>
          <div class="stat-value" style="color:#C0392B">{{ currentMonth.gas }} <span class="unit">m³</span></div>
          <div class="stat-sub">较上月 {{ currentMonth.gasChange > 0 ? '+' : '' }}{{ currentMonth.gasChange }}%</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:rgba(45,74,62,0.08)">
          <svg viewBox="0 0 24 24" fill="none" stroke="#2D4A3E" stroke-width="2">
            <line x1="12" y1="1" x2="12" y2="23"/>
            <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">本月费用 · Cost</div>
          <div class="stat-value" style="color:#2D4A3E">¥{{ currentMonth.cost.toLocaleString() }}</div>
          <div class="stat-sub">预算 ¥{{ budget.toLocaleString() }} · 剩余 ¥{{ (budget - currentMonth.cost).toLocaleString() }}</div>
        </div>
      </div>
    </div>

    <!-- 月度趋势图 -->
    <div class="chart-card">
      <h3 class="section-title">月度能耗趋势 · Monthly Energy Trend</h3>
      <div class="chart-area">
        <div class="y-axis">
          <span v-for="v in yLabels" :key="v">{{ v }}</span>
        </div>
        <div class="chart-body">
          <div class="grid-lines">
            <div v-for="i in 5" :key="i" class="grid-line"></div>
          </div>
          <div class="bars-container">
            <div v-for="(item, i) in monthlyData" :key="i" class="bar-group">
              <div class="bars">
                <div class="bar electric" :style="{ height: (item.electric / maxElectric * 100) + '%' }" :title="'电: ' + item.electric + 'kWh'"></div>
                <div class="bar water" :style="{ height: (item.water / maxWater * 100) + '%' }" :title="'水: ' + item.water + 't'"></div>
                <div class="bar gas" :style="{ height: (item.gas / maxGas * 100) + '%' }" :title="'气: ' + item.gas + 'm³'"></div>
              </div>
              <span class="bar-label">{{ item.month }}</span>
            </div>
          </div>
        </div>
      </div>
      <div class="chart-legend">
        <span class="legend-item"><span class="legend-dot electric"></span> 用电 kWh</span>
        <span class="legend-item"><span class="legend-dot water"></span> 用水 吨</span>
        <span class="legend-item"><span class="legend-dot gas"></span> 用气 m³</span>
      </div>
    </div>

    <!-- 读数记录 -->
    <div class="records-card">
      <h3 class="section-title">读数记录 · Meter Readings</h3>
      <table class="data-table">
        <thead>
          <tr>
            <th>日期</th>
            <th>电表读数</th>
            <th>水表读数</th>
            <th>气表读数</th>
            <th>日用电</th>
            <th>日用水</th>
            <th>日费用</th>
            <th>录入人</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in readings" :key="r.id">
            <td>{{ r.date }}</td>
            <td>{{ r.electricMeter }}</td>
            <td>{{ r.waterMeter }}</td>
            <td>{{ r.gasMeter }}</td>
            <td>{{ r.dailyElectric }} kWh</td>
            <td>{{ r.dailyWater }} t</td>
            <td>¥{{ r.dailyCost }}</td>
            <td>{{ r.recorder }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 录入弹窗 -->
    <div v-if="showAddDialog" class="dialog-overlay" @click.self="showAddDialog = false">
      <div class="dialog-box">
        <h3>录入能耗读数</h3>
        <div class="form-grid">
          <div class="form-item">
            <label>日期</label>
            <input v-model="form.date" type="date" />
          </div>
          <div class="form-item">
            <label>电表读数 (kWh)</label>
            <input v-model.number="form.electricMeter" type="number" />
          </div>
          <div class="form-item">
            <label>水表读数 (吨)</label>
            <input v-model.number="form.waterMeter" type="number" />
          </div>
          <div class="form-item">
            <label>气表读数 (m³)</label>
            <input v-model.number="form.gasMeter" type="number" />
          </div>
          <div class="form-item">
            <label>录入人</label>
            <input v-model="form.recorder" placeholder="姓名" />
          </div>
        </div>
        <div class="dialog-actions">
          <button class="btn-cancel" @click="showAddDialog = false">取消</button>
          <button class="btn-primary" @click="saveReading">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const showAddDialog = ref(false)
const budget = 15000

const form = ref({ date: '', electricMeter: 0, waterMeter: 0, gasMeter: 0, recorder: '' })

const currentMonth = ref({
  electric: 4200, electricChange: 2.4,
  water: 68, waterChange: -1.2,
  gas: 320, gasChange: 5.1,
  cost: 12800,
})

const monthlyData = ref([
  { month: '1月', electric: 3800, water: 55, gas: 280 },
  { month: '2月', electric: 3200, water: 48, gas: 250 },
  { month: '3月', electric: 3600, water: 52, gas: 270 },
  { month: '4月', electric: 4100, water: 58, gas: 290 },
  { month: '5月', electric: 4500, water: 62, gas: 310 },
  { month: '6月', electric: 4200, water: 68, gas: 320 },
])

const readings = ref([
  { id: 1, date: '2026-07-08', electricMeter: 125800, waterMeter: 8650, gasMeter: 3280, dailyElectric: 145, dailyWater: 2.3, dailyCost: 420, recorder: '王芳' },
  { id: 2, date: '2026-07-07', electricMeter: 125655, waterMeter: 8647.7, gasMeter: 3270, dailyElectric: 138, dailyWater: 2.1, dailyCost: 398, recorder: '王芳' },
  { id: 3, date: '2026-07-06', electricMeter: 125517, waterMeter: 8645.6, gasMeter: 3260, dailyElectric: 152, dailyWater: 2.5, dailyCost: 445, recorder: '李强' },
  { id: 4, date: '2026-07-05', electricMeter: 125365, waterMeter: 8643.1, gasMeter: 3250, dailyElectric: 140, dailyWater: 2.2, dailyCost: 410, recorder: '李强' },
])

const maxElectric = computed(() => Math.max(...monthlyData.value.map(d => d.electric)) * 1.2)
const maxWater = computed(() => Math.max(...monthlyData.value.map(d => d.water)) * 1.2)
const maxGas = computed(() => Math.max(...monthlyData.value.map(d => d.gas)) * 1.2)
const yLabels = computed(() => {
  const max = maxElectric.value
  return [Math.round(max), Math.round(max * 0.75), Math.round(max * 0.5), Math.round(max * 0.25), 0]
})

const saveReading = () => {
  if (!form.value.date || !form.value.recorder) return
  const prev = readings.value[0]
  const dailyElectric = prev ? form.value.electricMeter - prev.electricMeter : 0
  const dailyWater = prev ? +(form.value.waterMeter - prev.waterMeter).toFixed(1) : 0
  const dailyCost = Math.round(dailyElectric * 0.85 + dailyWater * 5.5 + 50)
  readings.value.unshift({
    id: Date.now(),
    ...form.value,
    dailyElectric, dailyWater, dailyCost,
  })
  showAddDialog.value = false
  form.value = { date: '', electricMeter: 0, waterMeter: 0, gasMeter: 0, recorder: '' }
}
</script>

<style scoped>
.energy-page { padding: 24px 32px; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; flex-wrap: wrap; gap: 12px; }
.page-title { font-size: 22px; font-weight: 700; color: #1a2f23; margin: 0; }
.page-subtitle { font-size: 13px; color: #8a9a8e; margin: 4px 0 0 0; }

.btn-primary {
  background: #2D4A3E; color: #fff; border: none; padding: 8px 20px;
  border-radius: 6px; font-size: 13px; cursor: pointer; font-weight: 500;
}
.btn-primary:hover { background: #3a5f50; }

.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 20px; }
.stat-card {
  background: #fff; border-radius: 8px; padding: 18px 20px;
  border: 1px solid #e8ece9; display: flex; align-items: flex-start; gap: 14px;
}
.stat-icon {
  width: 44px; height: 44px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.stat-icon svg { width: 22px; height: 22px; }
.stat-content { flex: 1; }
.stat-label { font-size: 12px; color: #8a9a8e; margin-bottom: 4px; }
.stat-value { font-size: 26px; font-weight: 700; line-height: 1.2; }
.stat-value .unit { font-size: 14px; font-weight: 400; }
.stat-sub { font-size: 11px; color: #a0b0a5; margin-top: 4px; }

.chart-card {
  background: #fff; border-radius: 8px; padding: 20px;
  border: 1px solid #e8ece9; margin-bottom: 20px;
}
.section-title { font-size: 15px; font-weight: 600; color: #1a2f23; margin: 0 0 16px 0; }

.chart-area { display: flex; height: 220px; }
.y-axis {
  display: flex; flex-direction: column; justify-content: space-between;
  padding-right: 10px; font-size: 11px; color: #8a9a8e; min-width: 40px;
}
.chart-body { flex: 1; position: relative; }
.grid-lines { position: absolute; inset: 0; display: flex; flex-direction: column; justify-content: space-between; }
.grid-line { border-bottom: 1px dashed #e8ece9; }
.bars-container {
  position: relative; display: flex; align-items: flex-end;
  height: 100%; padding: 0 10px; gap: 8px; z-index: 1;
}
.bar-group { flex: 1; display: flex; flex-direction: column; align-items: center; height: 100%; justify-content: flex-end; }
.bars { display: flex; gap: 3px; align-items: flex-end; height: 180px; width: 100%; justify-content: center; }
.bar { width: 16px; border-radius: 3px 3px 0 0; min-height: 2px; transition: height 0.3s; }
.bar.electric { background: linear-gradient(180deg, #D4A853, #e8c97a); }
.bar.water { background: linear-gradient(180deg, #5B7B8A, #7a9baa); }
.bar.gas { background: linear-gradient(180deg, #C0392B, #d4756b); }
.bar-label { font-size: 11px; color: #8a9a8e; margin-top: 6px; }

.chart-legend { display: flex; gap: 20px; justify-content: center; margin-top: 14px; }
.legend-item { font-size: 12px; color: #6a7a6e; display: flex; align-items: center; gap: 5px; }
.legend-dot { width: 12px; height: 12px; border-radius: 3px; }
.legend-dot.electric { background: #D4A853; }
.legend-dot.water { background: #5B7B8A; }
.legend-dot.gas { background: #C0392B; }

.records-card { background: #fff; border-radius: 8px; padding: 20px; border: 1px solid #e8ece9; overflow-x: auto; }
.data-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.data-table th {
  text-align: left; padding: 10px 12px; font-weight: 600; color: #6a7a6e;
  border-bottom: 2px solid #e8ece9; font-size: 12px; white-space: nowrap;
}
.data-table td { padding: 10px 12px; border-bottom: 1px solid #f0f2f0; color: #3a4a3e; }

.dialog-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.4);
  display: flex; align-items: center; justify-content: center; z-index: 1000;
}
.dialog-box {
  background: #fff; border-radius: 12px; padding: 28px; width: 480px; max-width: 90vw;
  box-shadow: 0 20px 60px rgba(0,0,0,0.15);
}
.dialog-box h3 { font-size: 18px; color: #1a2f23; margin: 0 0 20px 0; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.form-item { display: flex; flex-direction: column; gap: 4px; }
.form-item label { font-size: 12px; color: #6a7a6e; font-weight: 500; }
.form-item input {
  padding: 8px 10px; border: 1px solid #d0d8d2; border-radius: 6px;
  font-size: 13px; color: #3a4a3e; outline: none;
}
.form-item input:focus { border-color: #2D4A3E; }
.dialog-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; }
.btn-cancel {
  padding: 8px 20px; border-radius: 6px; font-size: 13px; cursor: pointer;
  border: 1px solid #d0d8d2; background: #fff; color: #6a7a6e;
}
</style>
