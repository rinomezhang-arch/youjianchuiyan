<template>
  <div class="ai-float">
    <!-- 浮动聊天面板 -->
    <Transition name="panel-anim">
      <div v-if="showChat" class="chat-panel" :style="panelStyle" @mousedown="startDrag">
        <!-- 头部 -->
        <div class="panel-header" @dblclick="minimize">
          <div class="header-left">
            <div class="avatar-bot">
              <img src="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'%3E%3Cpath d='M50 10 C30 10 20 25 20 40 C20 55 30 65 40 70 L40 85 L60 85 L60 70 C70 65 80 55 80 40 C80 25 70 10 50 10 Z' fill='white' opacity='0.95'/%3E%3Cpath d='M35 30 Q30 20 35 15' stroke='white' stroke-width='2' fill='none' opacity='0.7'/%3E%3Cpath d='M45 25 Q42 18 45 12' stroke='white' stroke-width='2' fill='none' opacity='0.7'/%3E%3C/svg%3E" alt="logo" class="logo-img" />
            </div>
            <div class="header-info">
              <span class="title">炊小助</span>
              <span class="subtitle">又见炊烟 · AI 智能助理</span>
            </div>
          </div>
          <div class="header-right">
            <select v-model="currentModel" class="model-select" @mousedown.stop>
              <option value="">智能路由</option>
              <optgroup label=" 对话">
                <option v-for="m in chatModels" :key="m.id" :value="m.id">{{ m.name }}</option>
              </optgroup>
              <optgroup label=" 识图">
                <option v-for="m in visionModels" :key="m.id" :value="m.id">{{ m.name }}</option>
              </optgroup>
            </select>
          </div>
        </div>

        <!-- 消息区 -->
        <div class="messages" ref="msgList">
          <div v-for="(msg, idx) in messages" :key="idx" class="msg-row" :class="msg.role">
            <div class="msg-avatar" :class="msg.role">
              <template v-if="msg.role === 'assistant'">
                <img src="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'%3E%3Cpath d='M50 10 C30 10 20 25 20 40 C20 55 30 65 40 70 L40 85 L60 85 L60 70 C70 65 80 55 80 40 C80 25 70 10 50 10 Z' fill='white' opacity='0.95'/%3E%3Cpath d='M35 30 Q30 20 35 15' stroke='white' stroke-width='2' fill='none' opacity='0.7'/%3E%3Cpath d='M45 25 Q42 18 45 12' stroke='white' stroke-width='2' fill='none' opacity='0.7'/%3E%3C/svg%3E" alt="AI" class="avatar-svg" />
              </template>
              <template v-else>
                <span class="emoji-avatar">🐻</span>
              </template>
            </div>
            <div class="msg-content">
              <div class="msg-sender">{{ msg.role === 'assistant' ? '炊小助' : senderName }}</div>
              <div class="msg-bubble" :class="msg.role">
                <img v-if="msg.image" :src="msg.image" class="msg-image" />
                <div v-if="msg.loading" class="typing">
                  <span></span><span></span><span></span>
                </div>
                <div v-else class="msg-text" v-html="formatText(msg.content)"></div>
              </div>
            </div>
          </div>
        </div>

        <!-- 图片预览 -->
        <div v-if="pendingImage" class="image-preview">
          <img :src="pendingImage" />
          <span>图片已就绪</span>
          <button @click="clearImage">×</button>
        </div>

        <!-- 输入区 -->
        <div class="input-area">
          <div class="input-tools">
            <label class="icon-btn" title="图片">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/>
              </svg>
              <input type="file" accept="image/*" hidden @change="onImageUpload" />
            </label>
            <label class="icon-btn" title="文件">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"/><polyline points="13 2 13 9 20 9"/>
              </svg>
              <input type="file" hidden @change="onFileUpload" />
            </label>
            <button class="icon-btn" :class="{ active: isRecording }" @click="toggleRecording" title="语音">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/><path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
              </svg>
            </button>
            <button class="icon-btn" @click="refreshChat" title="刷新对话">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="23 4 23 10 17 10"/>
                <polyline points="1 20 1 14 7 14"/>
                <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
              </svg>
            </button>
          </div>
          <textarea
            ref="inputRef"
            v-model="inputText"
            class="text-input"
            placeholder="输入消息..."
            @keydown.enter.exact.prevent="sendMessage"
            @keydown.enter.shift.exact.prevent="inputText += '\n'"
            @paste="onPaste"
            :disabled="loading"
            rows="1"
          ></textarea>
          <button class="send-btn" @click="sendMessage" :disabled="loading || (!inputText.trim() && !pendingImage)">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/>
            </svg>
          </button>
        </div>
      </div>
    </Transition>

    <!-- 底部触发按钮 -->
    <button class="ai-trigger" @click="toggleChat" :class="{ active: showChat }">
      <img v-if="!showChat" src="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'%3E%3Cpath d='M50 10 C30 10 20 25 20 40 C20 55 30 65 40 70 L40 85 L60 85 L60 70 C70 65 80 55 80 40 C80 25 70 10 50 10 Z' fill='white' opacity='0.95'/%3E%3Cpath d='M35 30 Q30 20 35 15' stroke='white' stroke-width='2.5' fill='none' opacity='0.8'/%3E%3Cpath d='M45 25 Q42 18 45 12' stroke='white' stroke-width='2.5' fill='none' opacity='0.8'/%3E%3C/svg%3E" alt="logo" class="trigger-logo" />
      <svg v-else width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
        <polyline points="6 9 12 15 18 9"/>
      </svg>
    </button>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'

const API_BASE = '/api/ai'

const showChat = ref(false)
const messages = ref([])
const inputText = ref('')
const loading = ref(false)
const isRecording = ref(false)
const voiceChatActive = ref(false)
const msgList = ref(null)
const inputRef = ref(null)
const pendingImage = ref(null)
const pendingFile = ref(null)
const currentModel = ref('')
const allModels = ref([])
const senderName = ref('我')

// 拖拽相关
const panelX = ref(window.innerWidth - 880)
const panelY = ref(80)
const dragging = ref(false)
const dragOffset = ref({ x: 0, y: 0 })

const chatModels = computed(() => allModels.value.filter(m => m.type === 'chat'))
const visionModels = computed(() => allModels.value.filter(m => m.type === 'vision'))

const panelStyle = computed(() => ({
  left: panelX.value + 'px',
  top: panelY.value + 'px',
}))

function toggleChat() {
  showChat.value = !showChat.value
  if (showChat.value) {
    nextTick(() => {
      inputRef.value?.focus()
      scrollToBottom()
    })
  }
}

function minimize() {
  showChat.value = false
}

async function refreshChat() {
  // 刷新 = 新话题，清空后端历史
  try {
    await fetch(`${API_BASE}/history/clear`, { method: 'POST', credentials: 'include' })
  } catch (e) {
    console.error('清空历史失败:', e)
  }
  messages.value = []
  messages.value.push({
    role: 'assistant',
    content: "",
  })
  scrollToBottom()
}

function startDrag(e) {
  // 只允许在页眉区域拖动
  if (!e.target.closest('.chat-header')) return
  dragging.value = true
  dragOffset.value = {
    x: e.clientX - panelX.value,
    y: e.clientY - panelY.value
  }
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
  e.preventDefault()
}

function onDrag(e) {
  if (!dragging.value) return
  panelX.value = Math.max(0, Math.min(window.innerWidth - 840, e.clientX - dragOffset.value.x))
  panelY.value = Math.max(0, Math.min(window.innerHeight - 200, e.clientY - dragOffset.value.y))
}

function stopDrag() {
  dragging.value = false
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
}

watch(showChat, async (newVal) => {
  if (newVal) {
    // 打开对话框时加载历史记录
    await loadChatHistory()
    if (messages.value.length === 0) {
      messages.value.push({
        role: 'assistant',
    content: "",
      })
    }
    scrollToBottom()
  }
})

async function loadChatHistory() {
  try {
    const res = await fetch(`${API_BASE}/history`, { credentials: 'include' })
    const data = await res.json()
    if (data.code === 200 && data.data && data.data.length > 0) {
      messages.value = data.data.map(msg => ({
        role: msg.role,
        content: msg.content,
        image: msg.image || null
      }))
    }
  } catch (e) {
    console.error('加载历史记录失败:', e)
  }
}

async function loadModels() {
  try {
    const res = await fetch(`${API_BASE}/models`, { credentials: 'include' })
    const data = await res.json()
    if (data.code === 200) {
      allModels.value = data.data || []
      if (data.defaultModel) currentModel.value = data.defaultModel
    }
  } catch (e) {
    console.error('加载模型失败:', e)
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (msgList.value) {
      msgList.value.scrollTop = msgList.value.scrollHeight
    }
  })
}

function formatText(text) {
  if (!text) return ''
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
}

function onImageUpload(e) {
  const file = e.target.files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = (event) => {
    pendingImage.value = event.target.result
    if (!showChat.value) showChat.value = true
    scrollToBottom()
  }
  reader.readAsDataURL(file)
  e.target.value = ''
}

function onFileUpload(e) {
  const file = e.target.files?.[0]
  if (!file) return
  pendingFile.value = file
  if (!showChat.value) showChat.value = true
  messages.value.push({ role: 'user', content: `[文件] ${file.name}` })
  scrollToBottom()
  messages.value.push({ role: 'assistant', content: '文件上传功能需要后端支持，请使用文字或图片。' })
  scrollToBottom()
  e.target.value = ''
}

function onPaste(e) {
  const items = e.clipboardData?.items
  if (!items) return
  for (const item of items) {
    if (item.type.startsWith('image/')) {
      e.preventDefault()
      const file = item.getAsFile()
      if (file) {
        const reader = new FileReader()
        reader.onload = (event) => {
          pendingImage.value = event.target.result
          if (!showChat.value) showChat.value = true
          scrollToBottom()
        }
        reader.readAsDataURL(file)
      }
      break
    }
  }
}

function clearImage() {
  pendingImage.value = null
}

let mediaRecorder = null
let audioChunks = []

async function toggleRecording() {
  if (isRecording.value) {
    mediaRecorder?.stop()
    isRecording.value = false
    return
  }
  
  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    mediaRecorder = new MediaRecorder(stream)
    audioChunks = []
    
    mediaRecorder.ondataavailable = (e) => {
      if (e.data.size > 0) audioChunks.push(e.data)
    }
    
    mediaRecorder.onstop = () => {
      stream.getTracks().forEach(t => t.stop())
      if (audioChunks.length > 0) {
        if (!showChat.value) showChat.value = true
        messages.value.push({ role: 'user', content: '[语音] ' })
        scrollToBottom()
        messages.value.push({ role: 'assistant', content: '语音转文字功能需要后端支持，请使用文字或图片。' })
        scrollToBottom()
      }
    }
    
    mediaRecorder.start()
    isRecording.value = true
  } catch (e) {
    isRecording.value = false
    messages.value.push({ role: 'assistant', content: '麦克风未授权，无法录音。' })
    if (!showChat.value) showChat.value = true
  }
}

function toggleVoiceChat() {
  voiceChatActive.value = !voiceChatActive.value
  if (voiceChatActive.value) {
    messages.value.push({ role: 'assistant', content: '语音聊天模式已开启（需要后端支持）。' })
    scrollToBottom()
  } else {
    messages.value.push({ role: 'assistant', content: '语音聊天模式已关闭。' })
    scrollToBottom()
  }
}

async function sendMessage() {
  const text = inputText.value.trim()
  if ((!text && !pendingImage.value) || loading.value) return

  const hasImage = !!pendingImage.value
  messages.value.push({
    role: 'user',
    content: text || (hasImage ? '请分析这张图片' : ''),
    image: pendingImage.value
  })

  inputText.value = ''
  const imageData = pendingImage.value
  pendingImage.value = null
  scrollToBottom()

  loading.value = true
  messages.value.push({ role: 'assistant', content: '', loading: true })
  scrollToBottom()

  try {
    const url = hasImage ? `${API_BASE}/chat-with-image` : `${API_BASE}/chat`
    const body = { message: text || '请分析图片' }

    if (hasImage) {
      body.image = imageData.replace(/^data:image\/\w+;base64,/, '')
    }

    if (currentModel.value) {
      body.model = currentModel.value
    }

    const res = await fetch(url, {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    })

    const data = await res.json()
    messages.value.pop()
    messages.value.push({
      role: 'assistant',
      content: (data.data || '服务异常，请稍后重试').trim()
    })
  } catch (e) {
    messages.value.pop()
    messages.value.push({ role: 'assistant', content: '网络异常，请检查连接。' })
  }

  loading.value = false
  scrollToBottom()
  nextTick(() => inputRef.value?.focus())
}

onMounted(() => {
  loadModels()
})
</script>

<style scoped>
.ai-float {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 9999;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* ===== 触发按钮 ===== */
.ai-trigger {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: linear-gradient(135deg, #7B61FF, #9B7FFF);
  color: white;
  border: none;
  cursor: pointer;
  box-shadow: 0 4px 20px rgba(123, 97, 255, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.ai-trigger:hover {
  transform: scale(1.08) translateY(-2px);
  box-shadow: 0 8px 28px rgba(123, 97, 255, 0.45);
}

.ai-trigger.active {
  background: linear-gradient(135deg, #6B51EF, #8B6FEF);
  transform: scale(0.95);
}

.trigger-logo {
  width: 34px;
  height: 34px;
}

/* ===== 聊天面板 ===== */
.chat-panel {
  position: fixed;
  width: 840px;
  max-width: calc(100vw - 48px);
  height: 680px;
  max-height: calc(100vh - 120px);
  background: #FAFBFF;
  border-radius: 16px;
  box-shadow: 0 8px 40px rgba(123, 97, 255, 0.15), 0 2px 8px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid rgba(123, 97, 255, 0.1);
  cursor: default;
  user-select: none;
}

/* ===== 头部 ===== */
.panel-header {
  background: linear-gradient(135deg, #7B61FF 0%, #9B7FFF 100%);
  padding: 12px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  cursor: grab;
}

.panel-header:active {
  cursor: grabbing;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar-bot {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-img {
  width: 22px;
  height: 22px;
}

.header-info {
  display: flex;
  flex-direction: column;
}

.title {
  font-size: 14px;
  font-weight: 600;
  color: white;
  line-height: 1.2;
}

.subtitle {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.75);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.model-select {
  background: rgba(255, 255, 255, 0.15);
  color: white;
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: 6px;
  padding: 4px 8px;
  font-size: 11px;
  outline: none;
  cursor: pointer;
  backdrop-filter: blur(4px);
  transition: background 0.2s;
}

.model-select:hover {
  background: rgba(255, 255, 255, 0.25);
}

.model-select option,
.model-select optgroup {
  color: #333;
  background: white;
}

/* ===== 消息区 ===== */
.messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 200px;
}

.messages::-webkit-scrollbar {
  width: 4px;
}

.messages::-webkit-scrollbar-track {
  background: transparent;
}

.messages::-webkit-scrollbar-thumb {
  background: rgba(123, 97, 255, 0.2);
  border-radius: 10px;
}

.msg-row {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.msg-row.user {
  flex-direction: row-reverse;
}

.msg-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.msg-avatar.assistant {
  background: linear-gradient(135deg, #7B61FF, #9B7FFF);
}

.msg-avatar.user {
  background: linear-gradient(135deg, #FF8C61, #FFB088);
}

.avatar-svg {
  width: 20px;
  height: 20px;
}

.emoji-avatar {
  font-size: 18px;
  line-height: 1;
}

.msg-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.msg-row.user .msg-content {
  align-items: flex-end;
}

.msg-sender {
  font-size: 11px;
  font-weight: 500;
  color: #999;
  margin-bottom: 2px;
}

.msg-row:not(.user) .msg-sender {
  padding-left: 2px;
}

.msg-row.user .msg-sender {
  padding-right: 2px;
  text-align: right;
}

.msg-bubble {
  position: relative;
  max-width: 75%;
}

.msg-bubble.assistant {
  background: white;
  border-radius: 4px 12px 12px 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.msg-bubble.user {
  background: linear-gradient(135deg, #7B61FF, #9B7FFF);
  border-radius: 12px 4px 12px 12px;
}

.msg-image {
  max-width: 200px;
  max-height: 150px;
  border-radius: 6px;
  margin-bottom: 4px;
  object-fit: cover;
}

.msg-text {
  padding: 10px 12px;
  font-size: 13px;
  line-height: 1.6;
  word-break: break-word;
  user-select: text;
  -webkit-user-select: text;
  cursor: text;
}

.msg-bubble.assistant .msg-text {
  color: #333;
}

.msg-bubble.user .msg-text {
  color: white;
}

.typing {
  display: flex;
  gap: 4px;
  padding: 12px 14px;
}

.typing span {
  width: 6px;
  height: 6px;
  background: #7B61FF;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out;
}

.typing span:nth-child(2) { animation-delay: 0.2s; }
.typing span:nth-child(3) { animation-delay: 0.4s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0.5); opacity: 0.3; }
  40% { transform: scale(1); opacity: 1; }
}

/* ===== 图片预览 ===== */
.image-preview {
  padding: 4px 12px;
  background: #F8F7FF;
  border-top: 1px solid rgba(123, 97, 255, 0.08);
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.image-preview img {
  width: 28px;
  height: 28px;
  border-radius: 4px;
  object-fit: cover;
}

.image-preview span {
  font-size: 11px;
  color: #666;
}

.image-preview button {
  margin-left: auto;
  background: none;
  border: none;
  font-size: 14px;
  color: #999;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
}

.image-preview button:hover {
  background: rgba(0, 0, 0, 0.05);
}

/* ===== 输入区 ===== */
.input-area {
  padding: 8px 12px;
  background: white;
  border-top: 1px solid rgba(123, 97, 255, 0.08);
  display: flex;
  gap: 8px;
  align-items: flex-end;
  flex-shrink: 0;
}

.input-tools {
  display: flex;
  gap: 2px;
  align-items: center;
}

.icon-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #666;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.icon-btn:hover {
  background: rgba(123, 97, 255, 0.08);
  color: #7B61FF;
}

.icon-btn.active {
  background: rgba(123, 97, 255, 0.15);
  color: #7B61FF;
}

.text-input {
  flex: 1;
  border: 1.5px solid #E8E4FF;
  border-radius: 10px;
  padding: 8px 12px;
  font-size: 13px;
  font-family: inherit;
  outline: none;
  background: #FAFBFF;
  resize: none;
  max-height: 100px;
  line-height: 1.5;
  transition: border-color 0.2s, box-shadow 0.2s;
  color: #333;
}

.text-input:focus {
  border-color: #7B61FF;
  box-shadow: 0 0 0 3px rgba(123, 97, 255, 0.1);
}

.text-input::placeholder {
  color: #BBB;
}

.send-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #7B61FF, #9B7FFF);
  color: white;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s;
  box-shadow: 0 2px 6px rgba(123, 97, 255, 0.25);
}

.send-btn:hover:not(:disabled) {
  transform: scale(1.05);
  box-shadow: 0 4px 10px rgba(123, 97, 255, 0.35);
}

.send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  transform: none;
}

/* ===== 工具栏 ===== */
.toolbar {
  padding: 6px 12px;
  background: white;
  border-top: 1px solid rgba(123, 97, 255, 0.06);
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

.tool-btn {
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #666;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  position: relative;
}

.tool-btn:hover {
  background: #F0EDFF;
  color: #7B61FF;
}

.tool-btn.active {
  background: #7B61FF;
  color: white;
}

.tool-btn input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

/* ===== 动画 ===== */
.panel-anim-enter-active {
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

.panel-anim-leave-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.panel-anim-enter-from {
  opacity: 0;
  transform: translateY(30px) scale(0.95);
}

.panel-anim-leave-to {
  opacity: 0;
  transform: translateY(20px) scale(0.97);
}
</style>

async function loadGreeting() {
  try {
    const res = await fetch('/api/chat/greeting')
    const data = await res.json()
    if (data.code === 200 && data.greeting) {
      messages.value.push({
        role: 'assistant',
        content: data.greeting
      })
      currentUserName.value = data.name || '朋友'
      currentPermLevel.value = data.permLevel || 0
    } else {
      messages.value.push({
        role: 'assistant',
        content: '你好！我是炊小助，又见炊烟的AI助理。有什么需要？'
      })
    }
  } catch {
    messages.value.push({
      role: 'assistant',
      content: '你好！我是炊小助，又见炊烟的AI助理。有什么需要？'
    })
  }
}
