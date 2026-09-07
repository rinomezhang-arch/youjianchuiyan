<template>
  <div class="page">
    <div class="page-header">
      <button class="back-btn" @click="$router.push('/dashboard')">← 返回</button>
      <div>
        <h2>每日菜单 · H5生成器</h2>
        <p class="page-desc">粘贴文字 → 实时预览 → 一键下载 H5 页面</p>
      </div>
      <div class="header-actions">
        <input type="date" v-model="menuDate" class="date-input" />
        <input type="text" v-model="storeName" placeholder="门店名称" class="store-input" />
        <button class="btn-reset" @click="resetToSample">加载示例</button>
      </div>
    </div>

    <div class="workspace">
      <!-- 左侧：文本输入 -->
      <div class="input-panel">
        <div class="panel-header">
          <span>📝 文本输入</span>
          <span class="hint">支持：特别介绍: / 急推: / 沽清: 等分段</span>
        </div>
        <textarea
          ref="textareaRef"
          v-model="rawText"
          class="text-input"
          placeholder="在此粘贴每日菜单文字..."
          @input="onTextChange"
        ></textarea>
        <div class="panel-footer">
          <span class="char-count">{{ rawText.length }} 字符</span>
          <button class="btn-generate" @click="generate">🚀 生成 H5</button>
        </div>
      </div>

      <!-- 右侧：实时预览 -->
      <div class="preview-panel">
        <div class="panel-header">
          <span>📱 H5 预览</span>
          <span class="hint">{{ parsedSections.length }} 个分段 · {{ totalItems }} 道菜</span>
        </div>
        <div class="preview-phone">
          <div class="phone-frame">
            <div class="phone-screen">
              <div class="h5-header">
                <div class="h5-brand">YOUJIANCHUIYAN · 每日菜单</div>
                <div class="h5-title">又见炊烟</div>
                <div class="h5-date">{{ formattedDate }}</div>
              </div>
              <div class="h5-body">
                <template v-if="parsedSections.length === 0">
                  <div class="empty">在左侧输入文字...</div>
                </template>
                <template v-for="(s, idx) in parsedSections" :key="idx">
                  <div class="h5-section" :class="`section-${s.style}`">
                    <div class="h5-section-header">
                      <span class="h5-section-title">{{ s.section }}</span>
                      <span v-if="s.labelEn" class="h5-badge">{{ s.labelEn }}</span>
                    </div>
                    <ul class="h5-dish-list">
                      <li v-for="(item, i) in s.items" :key="i" class="h5-dish" :class="{soldout: s.style === 'soldout'}">
                        {{ item }}
                      </li>
                    </ul>
                  </div>
                </template>
              </div>
              <div class="h5-footer">
                <div class="h5-store">{{ storeName }}</div>
                <div>匠心之作 · 时令为先</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 生成成功弹窗 -->
    <div v-if="showResult" class="modal-mask" @click.self="showResult = false">
      <div class="modal">
        <h3>✅ H5 已生成</h3>
        <p class="modal-desc">共 {{ parsedSections.length }} 个分段，{{ totalItems }} 道菜</p>
        <div class="modal-actions">
          <button class="btn-primary" @click="downloadGenerated">📥 下载 HTML 文件</button>
          <button class="btn-secondary" @click="showResult = false">继续编辑</button>
        </div>
        <div class="modal-tip">下载后可直接微信发送，或上传 COS 分享链接</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { parseDailyMenu, generateH5Html, downloadH5, getDefaultSample } from '@/utils/dailyMenuParser'

const STORAGE_KEY = 'daily-menu-editor-content'
const STORE_KEY = 'daily-menu-editor-store'
const DATE_KEY = 'daily-menu-editor-date'

const rawText = ref('')
const storeName = ref('又见炊烟')
const menuDate = ref('')
const showResult = ref(false)
const textareaRef = ref(null)

const parsedSections = computed(() => parseDailyMenu(rawText.value))
const totalItems = computed(() => parsedSections.value.reduce((sum, s) => sum + s.items.length, 0))
const formattedDate = computed(() => {
  if (!menuDate.value) return ''
  const d = new Date(menuDate.value)
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 周${'日一二三四五六'[d.getDay()]}`
})

function onTextChange() {
  localStorage.setItem(STORAGE_KEY, rawText.value)
}

function resetToSample() {
  rawText.value = getDefaultSample()
  localStorage.setItem(STORAGE_KEY, rawText.value)
}

function generate() {
  if (parsedSections.value.length === 0) {
    alert('请先输入菜单文字')
    return
  }
  showResult.value = true
}

function downloadGenerated() {
  const html = generateH5Html(parsedSections.value, {
    date: formattedDate.value,
    storeName: storeName.value
  })
  const filename = `daily-menu-${menuDate.value || new Date().toISOString().slice(0, 10)}.html`
  downloadH5(html, filename)
}

// 初始化
onMounted(() => {
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved) {
    rawText.value = saved
  } else {
    rawText.value = getDefaultSample()
  }
  const savedStore = localStorage.getItem(STORE_KEY)
  if (savedStore) storeName.value = savedStore
  else {
    const storeId = new URLSearchParams(window.location.search).get('storeId') || '1'
    storeName.value = storeId === '1' ? '又见炊烟·宁国店' : storeId === '2' ? '又见炊烟·宣城店' : '又见炊烟'
  }
  menuDate.value = localStorage.getItem(DATE_KEY) || new Date().toISOString().slice(0, 10)
})

watch(storeName, (v) => localStorage.setItem(STORE_KEY, v))
watch(menuDate, (v) => localStorage.setItem(DATE_KEY, v))
</script>

<style scoped>
.page { padding: 20px; }
.page-header {
  display: flex; align-items: center; gap: 16px;
  margin-bottom: 20px; flex-wrap: wrap;
}
.page-header h2 { font-size: 18px; font-weight: 600; margin: 0; }
.page-desc { font-size: 13px; color: #7A7A72; margin: 2px 0 0; }
.back-btn {
  padding: 6px 14px; border: 1px solid #DDD3B8;
  background: #fff; color: #333;
  border-radius: 2px; cursor: pointer; font-size: 13px;
}
.back-btn:hover { background: #1a3a2a; color: #fff; border-color: #1a3a2a; }

.header-actions { display: flex; gap: 10px; margin-left: auto; }
.date-input, .store-input {
  padding: 6px 10px; border: 1px solid #DDD3B8;
  border-radius: 2px; font-size: 13px; height: 32px;
}
.store-input { width: 160px; }
.btn-reset {
  padding: 6px 14px; border: 1px solid #C4A35A;
  background: #fff; color: #C4A35A;
  border-radius: 2px; cursor: pointer; font-size: 13px;
}
.btn-reset:hover { background: #C4A35A; color: #fff; }

.workspace {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  min-height: calc(100vh - 160px);
}

.input-panel, .preview-panel {
  background: #fff; border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);
  display: flex; flex-direction: column; overflow: hidden;
}

.panel-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 16px; border-bottom: 1px solid #F0EAD8;
  font-size: 14px; font-weight: 600;
}
.panel-header .hint { font-size: 12px; color: #999; font-weight: normal; }

.text-input {
  flex: 1; padding: 16px; border: none;
  resize: none; font-family: inherit; font-size: 14px;
  line-height: 1.8; color: #333; outline: none;
  min-height: 400px;
}
.text-input::placeholder { color: #BBB; }

.panel-footer {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 16px; border-top: 1px solid #F0EAD8;
  background: #FAF7F0;
}
.char-count { font-size: 12px; color: #999; }
.btn-generate {
  padding: 8px 20px; border: none;
  background: linear-gradient(135deg, #C4A35A 0%, #A8873F 100%);
  color: #fff; border-radius: 4px; cursor: pointer;
  font-size: 14px; font-weight: 600; letter-spacing: 1px;
}
.btn-generate:hover { opacity: 0.9; }

/* 预览区 */
.preview-phone {
  flex: 1; display: flex; justify-content: center;
  align-items: flex-start; padding: 20px;
  overflow-y: auto;
  background: #E8E0D0;
}
.phone-frame {
  width: 340px; background: #000;
  border-radius: 28px; padding: 10px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.2);
}
.phone-screen {
  background: #F5F0E8; border-radius: 22px;
  overflow: hidden; min-height: 600px;
  max-height: 680px; overflow-y: auto;
}

/* H5 预览样式 */
.h5-header {
  background: linear-gradient(135deg, #1a3a2a 0%, #2d5a3d 100%);
  color: #F5F0E8; padding: 24px 16px 20px;
  text-align: center; position: relative;
}
.h5-header::after {
  content: ''; position: absolute;
  bottom: 0; left: 0; right: 0; height: 3px;
  background: linear-gradient(90deg, transparent, #C4A35A 50%, transparent);
}
.h5-brand { font-size: 10px; letter-spacing: 3px; color: #C4A35A; margin-bottom: 4px; }
.h5-title { font-size: 18px; font-weight: 700; letter-spacing: 2px; }
.h5-date { font-size: 11px; color: rgba(245,240,232,0.75); margin-top: 6px; }

.h5-body { padding: 12px; }
.h5-section {
  background: #fff; border-radius: 10px;
  margin-bottom: 12px; overflow: hidden;
  box-shadow: 0 1px 8px rgba(26,58,42,0.05);
}
.h5-section-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 14px; border-bottom: 1px solid #F0EAD8;
}
.h5-section-title { font-size: 14px; font-weight: 700; letter-spacing: 1px; }
.h5-badge { font-size: 10px; padding: 1px 8px; border-radius: 8px; }

.section-feature { border-top: 3px solid #C4A35A; }
.section-feature .h5-section-title { color: #C4A35A; }
.section-feature .h5-badge { background: #C4A35A; color: #fff; }

.section-urgent { border-top: 3px solid #8B2020; }
.section-urgent .h5-section-title { color: #8B2020; }
.section-urgent .h5-badge { background: #8B2020; color: #fff; }

.section-soldout { border-top: 3px solid #999; opacity: 0.85; }
.section-soldout .h5-section-title { color: #666; }
.section-soldout .h5-badge { background: #999; color: #fff; }
.section-soldout .h5-dish-list { color: #888; }

.section-new { border-top: 3px solid #1a3a2a; }
.section-new .h5-section-title { color: #1a3a2a; }
.section-new .h5-badge { background: #1a3a2a; color: #fff; }

.section-promo { border-top: 3px solid #C4A35A; }
.section-promo .h5-section-title { color: #B87333; }
.section-promo .h5-badge { background: #B87333; color: #fff; }

.section-default { border-top: 3px solid #1a3a2a; }
.section-default .h5-section-title { color: #1a3a2a; }

.h5-dish-list { list-style: none; padding: 6px 0; }
.h5-dish {
  padding: 6px 14px; font-size: 13px; line-height: 1.5;
  border-bottom: 1px solid #F7F3E8; display: flex; align-items: flex-start;
}
.h5-dish:last-child { border-bottom: none; }
.h5-dish::before {
  content: '·'; color: #C4A35A; font-size: 18px;
  margin-right: 8px; line-height: 1;
}
.section-soldout .h5-dish::before { color: #999; content: '×'; }
.section-urgent .h5-dish::before { color: #8B2020; content: '★'; }
.h5-dish.soldout { text-decoration: line-through; text-decoration-color: #999; }
.h5-dish.soldout::before { text-decoration: none; }

.h5-footer {
  text-align: center; padding: 16px 12px;
  font-size: 11px; color: #999; line-height: 1.6;
}
.h5-store { font-size: 12px; color: #1a3a2a; font-weight: 600; letter-spacing: 1px; margin-bottom: 2px; }

.empty { text-align: center; color: #BBB; padding: 60px 20px; font-size: 14px; }

/* 弹窗 */
.modal-mask {
  position: fixed; inset: 0; background: rgba(0,0,0,0.5);
  display: flex; align-items: center; justify-content: center; z-index: 9999;
}
.modal {
  background: #fff; border-radius: 12px; padding: 32px;
  width: 360px; text-align: center;
}
.modal h3 { margin: 0 0 12px; }
.modal-desc { color: #7A7A72; margin: 0 0 20px; font-size: 14px; }
.modal-actions { display: flex; gap: 10px; justify-content: center; }
.btn-primary {
  padding: 10px 24px; border: none;
  background: linear-gradient(135deg, #C4A35A 0%, #A8873F 100%);
  color: #fff; border-radius: 4px; cursor: pointer; font-size: 14px;
}
.btn-secondary {
  padding: 10px 24px; border: 1px solid #DDD3B8;
  background: #fff; color: #666;
  border-radius: 4px; cursor: pointer; font-size: 14px;
}
.modal-tip { margin-top: 16px; font-size: 12px; color: #BBB; }

@media (max-width: 960px) {
  .workspace { grid-template-columns: 1fr; }
  .header-actions { margin-left: 0; }
}
</style>
