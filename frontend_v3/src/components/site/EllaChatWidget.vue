<template>
  <div class="ella-widget" :style="{ bottom: bottomOffset + 'px' }">
    <Transition name="ella-panel">
      <div v-if="open" class="ella-panel">
        <div class="ella-header" @dblclick="minimize">
          <div class="ella-header-left">
            <div class="ella-avatar">E</div>
            <div>
              <div class="ella-title">Ella</div>
              <div class="ella-subtitle">又见炊烟 · 在线客服 · Online Assistant</div>
            </div>
          </div>
          <button class="ella-close" @click="open = false">×</button>
        </div>

        <div class="ella-messages" ref="msgListRef">
          <div v-for="m in messages" :key="m.id" class="ella-msg-row" :class="m.role">
            <div class="ella-bubble" :class="m.role">
              <span v-if="m.loading" class="ella-typing"><i></i><i></i><i></i></span>
              <span v-else>{{ m.content }}</span>
            </div>
          </div>
        </div>

        <div class="ella-input-area">
          <button
            v-if="voiceSupported"
            class="ella-voice"
            :class="{ listening: isListening }"
            @click="toggleVoice"
            :title="isListening ? '正在听，点击停止' : '语音输入'"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/><path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
            </svg>
          </button>
          <textarea
            ref="inputRef"
            v-model="inputText"
            class="ella-input"
            :placeholder="isListening ? '正在听你说…' : '问问菜品/套餐/预定… · Ask about dishes, packages, booking…'"
            rows="1"
            @input="autoGrow"
            @keydown.enter.exact.prevent="send"
            @keydown.enter.shift.exact="() => {}"
          ></textarea>
          <button class="ella-send" :disabled="!inputText.trim()" @click="send">发送</button>
        </div>
      </div>
    </Transition>

    <button class="ella-trigger" :class="{ active: open }" @click="toggle">
      <span v-if="!open">Ella</span>
      <span v-else>×</span>
    </button>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'

// 移动端底部有 tab bar 时，用这个把悬浮气泡/面板往上抬，避免被 tab bar 挡住
const props = defineProps({
  bottomOffset: { type: Number, default: 24 }
})

const open = ref(false)
const messages = ref([])

// 每条消息给稳定唯一 id，不再靠数组下标/"最后一个"定位——允许上一句还没回完、
// 用户已经发下一句这种并发场景，用 push/pop 在多轮并发时会互相踩到彼此的占位消息。
let msgSeq = 0
function pushMsg(msg) {
  const id = ++msgSeq
  messages.value.push({ id, ...msg })
  return id
}
function findMsg(id) {
  return messages.value.find(m => m.id === id)
}
function removeMsg(id) {
  const idx = messages.value.findIndex(m => m.id === id)
  if (idx !== -1) messages.value.splice(idx, 1)
}

const inputText = ref('')
const msgListRef = ref(null)
const inputRef = ref(null)

// 语音输入：用浏览器原生 SpeechRecognition，安卓 Chrome/大部分场景能用，iOS 微信内置浏览器
// 不支持——这里做运行时能力检测，不支持就不显示麦克风按钮，不会报错也不会影响正常打字。
const SpeechRecognitionCtor = window.SpeechRecognition || window.webkitSpeechRecognition
const voiceSupported = ref(!!SpeechRecognitionCtor)
const isListening = ref(false)
let recognizer = null

function toggleVoice() {
  if (isListening.value) {
    recognizer?.stop()
    return
  }
  if (!SpeechRecognitionCtor) return
  recognizer = new SpeechRecognitionCtor()
  recognizer.lang = 'zh-CN'
  recognizer.interimResults = true
  recognizer.continuous = false

  recognizer.onstart = () => { isListening.value = true }
  recognizer.onerror = () => { isListening.value = false }
  recognizer.onend = () => { isListening.value = false }
  recognizer.onresult = (event) => {
    let text = ''
    for (let i = 0; i < event.results.length; i++) {
      text += event.results[i][0].transcript
    }
    inputText.value = text
    nextTick(autoGrow)
  }
  recognizer.start()
}

function minimize() {
  open.value = false
}

// 输入框随内容自适应高度：先重置成一行的高度再按内容撑开，不然只会越长越高、删字不会缩回去
function autoGrow() {
  const el = inputRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 120) + 'px'
}

function toggle() {
  open.value = !open.value
  if (open.value && messages.value.length === 0) {
    pushMsg({
      role: 'assistant',
      content: '你好呀，我是Ella～想了解菜品、宴会套餐，或者直接帮你预定，都可以问我 😊 Hi, I\'m Ella — ask me about dishes, banquet packages, or let me help you book a table.'
    })
  }
  scrollToBottom()
}

function scrollToBottom() {
  nextTick(() => {
    if (msgListRef.value) msgListRef.value.scrollTop = msgListRef.value.scrollHeight
  })
}

// 不再用全局 loading 挡住输入框——允许上一句还没回完，用户就发下一句，
// 每一句各自的请求/占位气泡/打字机效果都靠自己的 msgId 独立追踪，互不干扰。
async function send() {
  const text = inputText.value.trim()
  if (!text) return
  inputText.value = ''
  nextTick(autoGrow)
  pushMsg({ role: 'user', content: text })
  scrollToBottom()

  const history = messages.value
    .filter(m => !m.loading)
    .map(m => ({ role: m.role, content: m.content }))

  const loadingId = pushMsg({ role: 'assistant', content: '', loading: true })
  scrollToBottom()

  try {
    const res = await fetch('/api/public/agent/ella/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message: text, history })
    })
    const data = await res.json()
    removeMsg(loadingId)
    const reply = (data.data && data.data.reply) || data.message || '暂时联系不上，请稍后再试~'
    await typeOutReply(reply)
  } catch (e) {
    removeMsg(loadingId)
    pushMsg({ role: 'assistant', content: '网络好像不太好，请稍后再试一次~' })
  }
  scrollToBottom()
}

// 一个字一个字"打"出来，模仿真人打字——比一次性甩一大段文字出来看着更自然、不那么累眼睛。
// 按标点稍微停顿一下，节奏更像人在打字而不是机械匀速。
//
// 之前这里卡顿的真实原因：push 进数组之前先存了本地变量 msg，之后一直改这个"旧引用"的
// .content——Vue 3 的响应式是在 push 那一刻把数组元素包成 Proxy 的，本地这个 msg 变量
// 拿到的还是包装前的原始对象，改它 Vue 完全感知不到，所以逐字变化不会触发重绘，
// 憋到某次别的响应式操作顺带把整个数组重新 diff 一遍，才会"一股脑"全部冒出来。
// 改成按 id 查找（而不是数组下标）去改，即使多轮打字机动画同时在跑、期间数组被
// removeMsg/pushMsg 增删导致下标漂移，也不会改错消息。
// 另外把逐字滚动改成节流（每 4 个字才滚一次），避免每个字都强制触发一次布局重排。
function typeOutReply(fullText) {
  return new Promise(resolve => {
    const id = pushMsg({ role: 'assistant', content: '' })
    let i = 0
    const punctuationPause = new Set(['，', '。', '！', '？', '、', ',', '.', '!', '?'])
    function step() {
      const msg = findMsg(id)
      if (!msg || i >= fullText.length) {
        scrollToBottom()
        resolve()
        return
      }
      const ch = fullText[i]
      msg.content += ch
      i++
      if (i % 4 === 0) scrollToBottom()
      const delay = punctuationPause.has(ch) ? 140 : 28
      setTimeout(step, delay)
    }
    step()
  })
}
</script>

<style scoped>
.ella-widget {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 999;
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", sans-serif;
}

.ella-trigger {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #1F3A2E;
  color: #fff;
  border: 2px solid #B8935A;
  cursor: pointer;
  font-size: 14px;
  font-weight: 700;
  box-shadow: 0 4px 16px rgba(31, 58, 46, 0.35);
  transition: transform 0.2s;
}
.ella-trigger:hover { transform: scale(1.06); }
.ella-trigger.active { background: #B8935A; border-color: #1F3A2E; }

/* 桌面端按微信 PC 聊天窗口的比例来，比一般小气泡式客服窗口更宽更高，
   看起来更像"真的在聊天"而不是一个小提示框。手机端要另做一套贴近微信手机聊天界面
   的全屏/近全屏样式——这次先不做，下面 @media 只是权宜的窄屏适配，记得后续跟进补上真正的移动端版本。 */
.ella-panel {
  position: absolute;
  right: 0;
  bottom: 68px;
  width: 420px;
  max-width: calc(100vw - 48px);
  height: 600px;
  max-height: calc(100vh - 140px);
  background: #FAF7F0;
  border-radius: 12px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid rgba(184, 147, 90, 0.3);
}

.ella-header {
  background: #1F3A2E;
  padding: 14px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.ella-header-left { display: flex; align-items: center; gap: 10px; }
.ella-avatar {
  width: 32px; height: 32px; border-radius: 50%;
  background: #B8935A; color: #fff; display: flex; align-items: center; justify-content: center;
  font-weight: 700; font-size: 14px; flex-shrink: 0;
}
.ella-title { color: #fff; font-size: 14px; font-weight: 700; }
.ella-subtitle { color: rgba(255,255,255,0.65); font-size: 10.5px; margin-top: 2px; }
.ella-close { background: none; border: none; color: #fff; font-size: 20px; cursor: pointer; line-height: 1; }

.ella-messages {
  flex: 1;
  overflow-y: auto;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.ella-msg-row { display: flex; }
.ella-msg-row.user { justify-content: flex-end; }
.ella-bubble {
  max-width: 82%;
  padding: 9px 13px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
}
.ella-bubble.assistant { background: #fff; color: #2A2A28; border-radius: 4px 12px 12px 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); }
.ella-bubble.user { background: #1F3A2E; color: #fff; border-radius: 12px 4px 12px 12px; }

.ella-typing { display: inline-flex; gap: 3px; }
.ella-typing i { width: 5px; height: 5px; border-radius: 50%; background: #B8935A; animation: ella-bounce 1.3s infinite ease-in-out; }
.ella-typing i:nth-child(2) { animation-delay: 0.15s; }
.ella-typing i:nth-child(3) { animation-delay: 0.3s; }
@keyframes ella-bounce { 0%, 80%, 100% { opacity: 0.3; transform: scale(0.7); } 40% { opacity: 1; transform: scale(1); } }

.ella-input-area {
  display: flex;
  gap: 8px;
  padding: 10px;
  border-top: 1px solid rgba(184, 147, 90, 0.2);
  background: #fff;
  align-items: flex-end;
}
.ella-voice {
  flex-shrink: 0;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 1px solid #E3DBC8;
  background: #fff;
  color: #8A8478;
  display: flex;
  align-items: center;
  justify-content: center;
}
.ella-voice.listening {
  background: #C0392B;
  border-color: #C0392B;
  color: #fff;
  animation: ella-pulse 1.2s infinite;
}
@keyframes ella-pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(192, 57, 43, 0.4); }
  50% { box-shadow: 0 0 0 6px rgba(192, 57, 43, 0); }
}
.ella-input {
  flex: 1;
  box-sizing: border-box;
  border: 1px solid #DDD3B8;
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 13px;
  line-height: 1.4;
  font-family: inherit;
  outline: none;
  resize: none;
  overflow-y: auto;
  max-height: 120px;
}
.ella-input:focus { border-color: #B8935A; }
.ella-send {
  background: #B8935A;
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 0 16px;
  font-size: 13px;
  cursor: pointer;
}
.ella-send:disabled { opacity: 0.5; cursor: not-allowed; }

.ella-panel-enter-active, .ella-panel-leave-active { transition: all 0.25s ease; }
.ella-panel-enter-from, .ella-panel-leave-to { opacity: 0; transform: translateY(16px) scale(0.96); }

@media (max-width: 480px) {
  .ella-widget { right: 16px; bottom: 16px; }
  .ella-panel { width: calc(100vw - 32px); right: -8px; }
}
</style>
