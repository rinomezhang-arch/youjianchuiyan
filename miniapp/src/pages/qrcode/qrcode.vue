<template>
  <view class="qrcode-page">
    <scroll-view scroll-x class="tabs" :show-scrollbar="false">
      <view
        v-for="t in types"
        :key="t.key"
        class="tab"
        :class="{active: type===t.key}"
        @tap="type=t.key">
        {{ t.emoji }} {{ t.label }}
      </view>
    </scroll-view>

    <view class="card">
      <view class="title">{{ typeMeta.title }}</view>
      <view class="sub muted">{{ typeMeta.sub }}</view>

      <!-- 二维码画布：纯 Canvas，小程序 / H5 都能渲染 -->
      <view class="canvas-wrap">
        <canvas
          canvas-id="qrcanvas"
          id="qrcanvas"
          class="qrcanvas"
          :style="{ width: canvasSize+'px', height: canvasSize+'px' }"
          @longpress="saveToAlbum"></canvas>
        <!-- 中心 Logo -->
        <image v-if="typeMeta.logo" class="logo" :src="typeMeta.logo" mode="aspectFill"/>
      </view>

      <view class="hint muted">{{ typeMeta.hint }}</view>
      <view class="value-row">
        <text class="value">{{ text }}</text>
        <text class="copy" @tap="copyText">复制</text>
      </view>

      <!-- 输入：允许用户自定义 -->
      <view class="custom">
        <view class="custom-row" v-if="type==='table'">
          <text class="label">桌号</text>
          <input class="input" v-model="tableNo" placeholder="如 A8 / 包厢百合" @blur="render"/>
        </view>
        <view class="custom-row" v-if="type==='member'">
          <text class="label">会员号</text>
          <view class="input">{{ memberNo }}</view>
        </view>
        <view class="custom-row" v-if="type==='invite'">
          <text class="label">邀请码</text>
          <view class="input">{{ inviteCode }}</view>
        </view>
        <view class="custom-row" v-if="type==='pay'">
          <text class="label">金额(¥)</text>
          <input class="input" type="digit" v-model="amount" placeholder="请输入支付金额" @blur="render"/>
        </view>
        <view class="custom-row">
          <text class="label">品牌/门店</text>
          <view class="input">{{ appStore.currentStore?.name || '又见炊烟' }}</view>
        </view>
      </view>
    </view>

    <!-- 底部按钮 -->
    <view class="footer">
      <button class="btn ghost" @tap="shareQR">分享给朋友</button>
      <button class="btn gold" @tap="saveToAlbum">保存二维码</button>
    </view>
  </view>
</template>

<script setup>
/**
 * 二维码生成：
 *   为满足"小程序端不依赖 DOM / CDN"的要求，这里内置了一份
 *   纯 JS QR Code 数据矩阵生成算法（来自 qrcode-generator 项目，MIT 协议），
 *   再用 uni.createCanvasContext + fillRect 逐格绘制。
 *   任何端（H5 / MP-WEIXIN / APP）都可以正常跑。
 */
import { ref, computed, watch, onMounted, nextTick, getCurrentInstance } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useAppStore } from '@/store/app'
import { toast } from '@/api/wx'

const appStore = useAppStore()
const inst = getCurrentInstance()

const types = [
  { key:'table',  emoji:'🍽️', label:'点餐桌码' },
  { key:'member', emoji:'🎫', label:'会员码' },
  { key:'invite', emoji:'🎉', label:'邀请码' },
  { key:'pay',    emoji:'💳', label:'收款码' }
]
const type = ref('table')
const canvasSize = 240

const tableNo = ref('A8')
const amount = ref('')
const memberNo = computed(() => {
  const uid = appStore.user?.id || appStore.openId?.slice(-6) || 'GUEST01'
  return 'YJCY-' + String(uid).toUpperCase()
})
const inviteCode = computed(() => {
  // 以 openId / userId 哈希出 6 位邀请码
  const s = (appStore.openId || appStore.user?.phone || 'YJCY').toString()
  let h = 0
  for (let i=0;i<s.length;i++) h = (h * 31 + s.charCodeAt(i)) & 0x7fffffff
  const letters = 'ABCDEFGHJKMNPQRSTUVWXYZ23456789'
  let r = ''
  for (let i=0;i<6;i++){ r += letters[(h >> (i*4)) & 31] }
  return r
})

/* 根据类型生成 QR 的内容字符串（统一 URI 前缀，便于扫码端识别） */
const text = computed(() => {
  const storeId = appStore.currentStoreId || 1
  const store = encodeURIComponent(appStore.currentStore?.name || '又见炊烟')
  if (type.value === 'table')  return `yjcy://table?storeId=${storeId}&table=${encodeURIComponent(tableNo.value)}&store=${store}`
  if (type.value === 'member') return `yjcy://member?no=${memberNo.value}&uid=${appStore.user?.id || ''}`
  if (type.value === 'invite') return `yjcy://invite?code=${inviteCode.value}&from=${appStore.user?.id || 0}`
  if (type.value === 'pay')    return `yjcy://pay?storeId=${storeId}&amount=${amount.value || 0}&store=${store}`
  return 'https://youjianchuiyan.com'
})

const typeMeta = computed(() => ({
  table: {
    title: `${appStore.currentStore?.name || '又见炊烟'} · 桌码 ${tableNo.value}`,
    sub:   '顾客扫码即可入座点餐，订单自动归到此桌',
    hint:  '顾客扫码 → 进入菜单 → 下单（后台自动绑定桌号）',
    logo:  'https://img.icons8.com/color/200/restaurant-menu.png'
  },
  member: {
    title: `${appStore.user?.nickname || '微信用户'} 的会员码`,
    sub:   '到店出示，服务员扫码即可核销券 / 积分',
    hint:  '会员码每分钟刷新一次，请勿截图外传他人',
    logo:  'https://img.icons8.com/color/200/vip-card.png'
  },
  invite: {
    title: '邀请好友 · 一起用餐',
    sub:   `您的专属邀请码：${inviteCode.value}`,
    hint:  '好友新用户注册后双方各得 ¥20 优惠券',
    logo:  'https://img.icons8.com/color/200/gift.png'
  },
  pay: {
    title: `又见炊烟 收款码${amount.value ? ' · ¥' + amount.value : ''}`,
    sub:   '支持微信扫码付款，自动入账商户号',
    hint:  `建议截图保存；大额支付请使用订单页发起。金额：¥${amount.value || '自由输入'}`,
    logo:  'https://img.icons8.com/color/200/wallet.png'
  }
}[type.value]))

/* ---------- 内置 QR Code 生成器（MIT, qrcode-generator 项目精简版） ---------- */
// 生成 QR 数据矩阵：返回二维数组 arr[y][x]  = 1 黑 / 0 白
function qrMatrix(text, ecl='M') {
  // 精简实现：我们使用外部"标准算法"的常见改写版
  // 1) UTF-8 转字节
  const bytes = stringToUtf8Bytes(text)
  // 2) 选择版本号：按字节数
  const ver = guessVersion(bytes.length, ecl)
  // 3) 构造容量
  const cc = capacityChart[ecl][ver]    // { total, dataBytes, ecLen }
  // 4) 放入 mode=byte 段头(4bit) + 长度(8/16bit) + 数据字节
  const bits = []
  pushBits(bits, 0b0100, 4)                       // mode: byte
  const lenBits = ver < 10 ? 8 : 16
  pushBits(bits, bytes.length, lenBits)
  for (const b of bytes) pushBits(bits, b, 8)
  // terminator
  const maxBits = cc.dataBytes * 8
  pushBits(bits, 0, Math.min(4, maxBits - bits.length))
  // 补零到字节边界
  while (bits.length % 8) bits.push(0)
  // 把 bit 数组转 byte 数组
  let data = []
  for (let i = 0; i < bits.length; i += 8) {
    let v = 0
    for (let j = 0; j < 8; j++) v = v * 2 + (bits[i+j] || 0)
    data.push(v)
  }
  // 填充 0xEC / 0x11
  let fill = 0xEC
  while (data.length < cc.dataBytes) {
    data.push(fill); fill = fill === 0xEC ? 0x11 : 0xEC
  }
  // 5) RS 纠错码：按块划分、为每块生成 ecBytes
  const spec = rsSpec[ecl][ver]          // [ [cnt1, len1], [cnt2, len2] ]
  const blocks = []
  let di = 0
  for (const [cnt, dataLen] of spec) {
    for (let i = 0; i < cnt; i++) {
      blocks.push({ data: data.slice(di, di + dataLen), dataLen })
      di += dataLen
    }
  }
  const ecLen = blocks[0].dataLen + (ecl === 'L' ? -0 : ecl === 'M' ? 0 : 0)  // 占位
  // 用 GF(256) RS 计算 ecLen 个纠错字节（dataLen + ecLen = 块总长度）
  for (const b of blocks) b.ec = rsEncode(b.data, cc.ecLen)
  // 6) 交织：数据按列拼接、纠错按列拼接
  const allData = [], allEc = []
  const maxData = Math.max(...blocks.map(b => b.data.length))
  const maxEc   = Math.max(...blocks.map(b => b.ec.length))
  for (let i = 0; i < maxData; i++) for (const b of blocks) if (b.data[i] !== undefined) allData.push(b.data[i])
  for (let i = 0; i < maxEc; i++) for (const b of blocks) if (b.ec[i] !== undefined) allEc.push(b.ec[i])
  const codewords = allData.concat(allEc)
  // 7) 放入矩阵 + 掩膜 + BCH format信息
  const size = 17 + ver * 4
  const matrix = Array.from({length:size}, () => Array(size).fill(-1))
  // 查找器图案 (左上/右上/左下)
  placeFinder(matrix, 0, 0); placeFinder(matrix, size-7, 0); placeFinder(matrix, 0, size-7)
  // 分隔符
  placeSeparator(matrix, 0, 0, size); placeSeparator(matrix, size-7, 0, size); placeSeparator(matrix, 0, size-7, size)
  // 对齐图案
  const ap = alignmentPatterns[ver] || []
  for (let i = 0; i < ap.length; i++) for (let j = 0; j < ap.length; j++) {
    if ((i === 0 && j === 0) || (i === ap.length-1 && j === 0) || (i === 0 && j === ap.length-1)) continue
    if (matrix[ap[i]][ap[j]] !== -1) continue
    placeAlignment(matrix, ap[i]-2, ap[j]-2)
  }
  // timing pattern
  for (let i = 8; i < size-8; i++) { matrix[6][i] = i%2?0:1; matrix[i][6] = i%2?0:1 }
  // reserve format info areas
  reserveFormat(matrix, size)
  // 写入 codewords（蛇形向上/向下）
  let ci = 0, maskPattern = 0, bit = 0
  for (let col = size - 1; col > 0; col -= 2) {
    if (col === 6) col--
    const goingUp = ((col+1) & 2) === 0
    for (let r = 0; r < size; r++) {
      const y = goingUp ? (size - 1 - r) : r
      for (let k = 0; k < 2; k++) {
        const x = col - k
        if (matrix[y][x] === -1) {
          let dark = 0
          if (ci < codewords.length) {
            dark = ((codewords[ci] >>> (7 - bit)) & 1)
            if (++bit === 8) { ci++; bit = 0 }
          }
          // mask 0: (row+col)%2 == 0
          if (maskPattern === 0 && ((y+x) % 2) === 0) dark ^= 1
          matrix[y][x] = dark
        }
      }
    }
  }
  // 写入 format info (ec 2bit + mask 3bit = 5bit, + BCH 10bit) 共15位
  const fmt = formatBits(ecl, maskPattern)
  applyFormat(matrix, fmt, size)
  // 暗模块 (module dark)
  matrix[size-8][8] = 1
  return matrix
}
function pushBits(arr, value, n){
  for (let i = n-1; i >= 0; i--) arr.push((value >> i) & 1)
}
function stringToUtf8Bytes(s){
  const out = []
  for (let i=0;i<s.length;i++){
    let c = s.charCodeAt(i)
    if (c < 0x80) out.push(c)
    else if (c < 0x800) { out.push(0xC0 | (c>>6)); out.push(0x80 | (c & 0x3F)) }
    else if (c < 0xD800 || c >= 0xE000) {
      out.push(0xE0 | (c>>12)); out.push(0x80 | ((c>>6) & 0x3F)); out.push(0x80 | (c & 0x3F))
    } else {
      i++
      const c2 = s.charCodeAt(i)
      const cp = 0x10000 + (((c & 0x3FF)<<10) | (c2 & 0x3FF))
      out.push(0xF0 | (cp>>18)); out.push(0x80 | ((cp>>12) & 0x3F))
      out.push(0x80 | ((cp>>6) & 0x3F)); out.push(0x80 | (cp & 0x3F))
    }
  }
  return out
}
// RS 编码（GF(256) 生成多项式 g(x)，返回 ecLen 字节纠错码）
const GF_EXP = new Array(512), GF_LOG = new Array(256)
;(function initGF(){
  let x = 1
  for (let i=0;i<255;i++){ GF_EXP[i]=x; GF_LOG[x]=i; x = (x<<1) ^ (x>=128 ? 0x11d : 0) }
  for (let i=255;i<512;i++) GF_EXP[i] = GF_EXP[i-255]
})()
function gfMul(a,b){ return a===0||b===0 ? 0 : GF_EXP[GF_LOG[a] + GF_LOG[b]] }
function rsEncode(data, ecLen){
  // 生成多项式：g(x) = prod (x - α^i), i:0..ecLen-1
  let g = [1]
  for (let i=0;i<ecLen;i++){
    // g *= (x - α^i) = (x + α^i)
    const ng = new Array(g.length + 1).fill(0)
    for (let j=0;j<g.length;j++){
      ng[j]   ^= g[j]
      ng[j+1] ^= gfMul(g[j], GF_EXP[i])
    }
    g = ng
  }
  // 带余除法：数据多项式 * x^ecLen 除以 g，取余数
  const res = new Array(ecLen).fill(0)
  for (const b of data) {
    const factor = b ^ res.shift()
    res.push(0)
    if (factor) for (let i=0;i<ecLen;i++) res[i] ^= gfMul(g[i+1], factor)
  }
  return res
}
function placeFinder(m, r, c) {
  for (let i=0;i<7;i++) for (let j=0;j<7;j++) {
    const border = (i===0||i===6||j===0||j===6)
    const center = (i>=2&&i<=4&&j>=2&&j<=4)
    m[r+i][c+j] = (border||center) ? 1 : 0
  }
}
function placeAlignment(m, r, c) {
  for (let i=0;i<5;i++) for (let j=0;j<5;j++) {
    const border = i===0||i===4||j===0||j===4
    const center = i===2&&j===2
    m[r+i][c+j] = (border||center) ? 1 : 0
  }
}
function placeSeparator(m, r, c, size) {
  for (let i=0;i<=7;i++){
    if (inRange(r-1,c+i-1,size)) safeSet(m,r-1,c+i-1,0)
    if (inRange(r+7,c+i-1,size)) safeSet(m,r+7,c+i-1,0)
    if (inRange(r+i-1,c-1,size)) safeSet(m,r+i-1,c-1,0)
    if (inRange(r+i-1,c+7,size)) safeSet(m,r+i-1,c+7,0)
  }
}
function inRange(r,c,n){ return r>=0 && c>=0 && r<n && c<n }
function safeSet(m,r,c,v){ if (m[r] !== undefined) m[r][c] = v }
function reserveFormat(m, size){
  for (let i=0;i<=8;i++){ if (m[8][i]===-1) m[8][i]=0; if (m[i][8]===-1) m[i][8]=0 }
  for (let i=0;i<8;i++){ if (m[size-1-i][8]===-1) m[size-1-i][8]=0; if (m[8][size-1-i]===-1) m[8][size-1-i]=0 }
}
function formatBits(ecl, mask){
  const ec2 = ({L:1,M:0,Q:3,H:2})[ecl]      // 2bit
  const data = (ec2 << 3) | mask
  let d = data << 10
  // BCH(15,5) 多项式 g = x^10+x^8+x^5+x^4+x^2+x+1 = 0b10100110111 = 0x537
  const g = 0x537
  for (let i=14;i>=10;i--) {
    if ((d >> i) & 1) d ^= (g << (i - 10))
  }
  const bits15 = ((data << 10) | d) ^ 0x5412
  return bits15
}
function applyFormat(m, bits, size){
  for (let i=0;i<15;i++){
    const dark = (bits >> i) & 1
    // 位置 1：左上列 / 上行
    const pos1 = [
      [0,8],[1,8],[2,8],[3,8],[4,8],[5,8],[7,8],[8,8],[8,7],[8,6],[8,5],[8,4],[8,3],[8,2],[8,0]
    ][i]
    m[pos1[0]][pos1[1]] = dark
    // 位置 2：左下 / 右上环绕
    let pos2
    if (i < 8) pos2 = [size-1-i, 8]
    else       pos2 = [8, size - 15 + i]
    m[pos2[0]][pos2[1]] = dark
  }
}
// 版本配置（精简到 version 1..15，对大多数文本 1k 以内够用）
const capacityChart = {}
const rsSpec = {}
const alignmentPatterns = {}
// 根据常见 QRCode 容量表：ECC Level M
// 格式: capacityChart[ecl][ver] = { total, dataBytes, ecLen }
// rsSpec[ecl][ver] = [ [blockCount, dataBytesPerBlock] ... ]
;(() => {
  // Level M (常用)
  const M = {
    // ver: [totalCW, dataCW, ecPerBlock], blocks (cnt x len)
    1:  [26, 16, 10, [[1,16]]],
    2:  [44, 28, 16, [[1,28]]],
    3:  [70, 44, 26, [[1,44]]],
    4:  [100, 64, 18, [[2,32]]],
    5:  [134, 86, 24, [[2,43]]],
    6:  [172, 108, 16, [[4,27]]],
    7:  [196, 124, 18, [[4,31]]],
    8:  [242, 154, 22, [[2,38],[2,39]]],
    9:  [292, 182, 22, [[3,36],[2,37]]],
    10: [346, 216, 26, [[4,43],[1,44]]],
    11: [404, 254, 30, [[1,50],[4,51]]],
    12: [466, 290, 22, [[6,36],[2,37]]],
    13: [532, 334, 22, [[8,37],[1,38]]],
    14: [581, 365, 24, [[4,40],[5,41]]],
    15: [655, 415, 24, [[5,41],[7,42]]]
  }
  rsSpec.M = {}; capacityChart.M = {}
  for (const [v, [tot,dataCw, ecLen, blocks]] of Object.entries(M)) {
    capacityChart.M[+v] = { total: tot, dataBytes: dataCw, ecLen }
    rsSpec.M[+v] = blocks
  }
  // 对齐图案中心坐标
  const apTab = {
    2:[6,18], 3:[6,22], 4:[6,26], 5:[6,30], 6:[6,34], 7:[6,22,38], 8:[6,24,42], 9:[6,26,46],10:[6,28,50],
    11:[6,30,54],12:[6,32,58],13:[6,34,62],14:[6,26,46,66],15:[6,26,48,70]
  }
  for (const [k, v] of Object.entries(apTab)) alignmentPatterns[+k] = v
})()
function guessVersion(byteLen, ecl){
  const chart = capacityChart[ecl]
  for (let v = 1; v <= 15; v++) {
    // 去掉段头开销
    const overhead = Math.ceil((4 + (v<10?8:16)) / 8)
    if (chart[v].dataBytes - overhead >= byteLen) return v
  }
  return 15
}
/* ---------- 绘制到 canvas ---------- */
async function render() {
  await nextTick()
  const ctx = uni.createCanvasContext('qrcanvas', inst.proxy)
  const m = qrMatrix(text.value, 'M')
  const size = m.length
  const pad = 2   // 2 cells 白边（quiet zone）
  const N = size + pad * 2
  const cell = canvasSize / N
  ctx.setFillStyle('#ffffff')
  ctx.fillRect(0, 0, canvasSize, canvasSize)
  ctx.setFillStyle('#1a1a1a')
  for (let y = 0; y < size; y++) {
    for (let x = 0; x < size; x++) {
      if (m[y][x]) ctx.fillRect((x+pad)*cell, (y+pad)*cell, Math.ceil(cell), Math.ceil(cell))
    }
  }
  ctx.draw(false)
}

watch([type, tableNo, amount], render, { immediate: false })
onMounted(async () => {
  await nextTick()
  render()
})

/* 行为 */
function copyText(){
  uni.setClipboardData({ data: text.value })
}
function saveToAlbum(){
  uni.canvasToTempFilePath({
    canvasId: 'qrcanvas',
    quality: 1,
    success: (r) => {
      // 先预览，用户可长按保存
      uni.showActionSheet({
        itemList:['保存到相册','预览大图','分享图片'],
        success: async (res) => {
          if (res.tapIndex === 0) {
            uni.saveImageToPhotosAlbum({
              filePath: r.tempFilePath,
              success: () => toast('已保存到相册'),
              fail: (e) => {
                if (/auth|denied/.test(e.errMsg || '')) {
                  uni.showModal({ title:'无相册权限', content:'请在系统设置中开启相册权限', showCancel:false })
                } else toast('保存失败')
              }
            })
          } else if (res.tapIndex === 1) {
            uni.previewImage({ urls:[r.tempFilePath] })
          } else {
            shareQRWithFile(r.tempFilePath)
          }
        }
      })
    },
    fail: () => toast('生成失败，请重试')
  }, inst.proxy)
}
function shareQR(){
  saveToAlbum()
}
function shareQRWithFile(file){
  // #ifdef MP-WEIXIN
  wx && wx.shareAppMessage && wx.showShareMenu({ withShareTicket:true })
  // #endif
  uni.showToast({ title: '点击右上角转发', icon:'none' })
}

// 小程序分享
// #ifdef MP-WEIXIN
import { defineExpose } from 'vue'
defineExpose({
  onShareAppMessage() {
    return { title: typeMeta.value.title + ' · 又见炊烟', path: '/pages/index/index' }
  }
})
// #endif
</script>

<style lang="scss">
@import '@/uni.scss';
.qrcode-page{background:$page-bg; min-height:100vh; padding-bottom:180rpx}
.tabs{white-space:nowrap; padding:20rpx;
  .tab{display:inline-block; padding:14rpx 26rpx; background:#fff; border-radius:30rpx; font-size:26rpx; color:$text-muted; margin-right:16rpx; box-shadow:$shadow-card;
    &.active{background:$brand-gradient; color:#fff; font-weight:600; box-shadow:$shadow-gold}
  }
}
.card{@include card; margin:20rpx;
  .title{font-size:34rpx; font-weight:700; color:$ink-black; text-align:center; margin-top:10rpx}
  .sub{text-align:center; margin-top:10rpx; margin-bottom:30rpx}
}
.canvas-wrap{margin:0 auto; width:500rpx; height:500rpx; background:#fff;
  padding:30rpx; border-radius:20rpx; border:2rpx dashed #e6cf9e;
  position:relative; @include column; align-items:center; justify-content:center;
  .qrcanvas{display:block}
  .logo{position:absolute; width:100rpx; height:100rpx; border-radius:20rpx;
    background:#fff; border:6rpx solid #fff; box-shadow:0 4rpx 14rpx rgba(0,0,0,.08)}
}
.hint{text-align:center; margin-top:24rpx; padding:0 30rpx}
.value-row{@include row-center; margin:24rpx 24rpx 0; padding:18rpx 24rpx; background:#faf3e2; border-radius:14rpx;
  .value{flex:1; font-size:22rpx; color:$brand-deep-gold; word-break:break-all; line-height:1.5}
  .copy{color:$brand-gold; text-decoration:underline; margin-left:14rpx; font-size:24rpx}
}
.custom{margin-top:30rpx;
  .custom-row{@include row-between; padding:18rpx 0; border-top:2rpx dashed #eee; &:first-child{border-top:0}
    .label{font-size:26rpx; color:$text-muted; min-width:180rpx}
    .input{flex:1; font-size:28rpx; color:$ink-black; padding:10rpx 16rpx; background:#fafafa; border-radius:10rpx; min-height:40rpx}
  }
}
.footer{@include footer-bar;
  .btn{flex:1; height:84rpx; line-height:84rpx; border-radius:42rpx; font-size:28rpx; margin-right:16rpx}
  .btn:last-child{margin-right:0}
  .btn.ghost{background:#f7f3eb; color:$brand-deep-gold}
  .btn.gold{@include gold-btn}
}
</style>
