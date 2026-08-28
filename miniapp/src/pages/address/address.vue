<template>
  <view class="address-page">
    <view v-if="selectMode" class="tips-bar">
      💡 请选择一个收货地址；或新增后自动使用
    </view>

    <view v-for="a in list" :key="a.id" class="addr-card" @tap="pickAndBack(a)">
      <view class="main">
        <view class="row1">
          <text class="name">{{ a.name }}</text>
          <text class="phone">{{ a.phone }}</text>
          <text v-if="a.isDefault" class="def">默认</text>
        </view>
        <view class="detail">{{ a.detail }}</view>
        <view class="tags muted" v-if="a.tag">标签：{{ a.tag }}</view>
      </view>
      <view class="actions">
        <text class="act" @tap.stop="edit(a)">✏️ 修改</text>
        <text class="act" v-if="!a.isDefault" @tap.stop="setDefault(a)">⭐ 设默认</text>
        <text class="act danger" @tap.stop="remove(a)">🗑️ 删除</text>
      </view>
    </view>

    <view v-if="loaded && !list.length" class="empty">
      <image class="icon" src="https://img.icons8.com/ios/200/C9A86C/address.png" mode="aspectFit" />
      <view class="text">还没有地址，先新增一个吧～</view>
    </view>

    <view class="gap"></view>
    <view class="footer">
      <button class="gold-btn big" @tap="showEdit = true; editing = null">＋ 新增收货地址</button>
    </view>

    <!-- 新增/编辑 Sheet -->
    <view v-if="showEdit" class="mask" @tap="showEdit = false">
      <view class="sheet" @tap.stop>
        <view class="sheet-title">
          <text>{{ editing ? '修改地址' : '新增地址' }}</text>
          <text class="close" @tap="showEdit = false">×</text>
        </view>
        <scroll-view scroll-y class="sheet-body">
          <view class="card no-margin">
            <view class="row"><text class="label">收货人</text>
              <input v-model="form.name" class="input" placeholder="您的称呼"/>
            </view>
            <view class="row"><text class="label">手机号</text>
              <input v-model="form.phone" class="input" type="number" maxlength="11" placeholder="请输入手机号"/>
              <button v-if="isMp" class="mini-btn gold-outline"
                      open-type="getPhoneNumber" @getphonenumber="onGetPhone">一键授权</button>
            </view>
            <view class="row" @tap="openRegion = true">
              <text class="label">所在区域</text>
              <view class="input picker">{{ regionText || '省 / 市 / 区' }}</view>
            </view>
            <picker
              v-if="openRegion"
              mode="region"
              :value="form.region"
              @cancel="openRegion=false"
              @change="onRegionChange">
              <!-- 透明一层，由上一行点击触发 -->
              <view style="position:absolute; inset:0; opacity:0; pointer-events:none"></view>
            </picker>
            <view class="row column">
              <text class="label">详细地址</text>
              <textarea v-model="form.detail" class="textarea" placeholder="街道、门牌号、楼栋、单元、楼层房号等" rows="2"/>
            </view>
            <view class="row tag-row">
              <text class="label">地址标签</text>
              <view class="chips">
                <text
                  v-for="t in tags"
                  :key="t"
                  class="chip"
                  :class="{active: form.tag===t}"
                  @tap="form.tag = form.tag===t ? '' : t">{{ t }}</text>
              </view>
            </view>
            <view class="row">
              <text class="label">设为默认</text>
              <switch :checked="form.isDefault" color="#C9A86C" @change="form.isDefault = $event.detail.value"/>
            </view>
          </view>
          <view style="height:40rpx"></view>
          <button class="gold-btn big" :loading="saving" :disabled="!canSave" @tap="save">
            {{ editing ? '保存修改' : '确认新增' }}
          </button>
        </scroll-view>
      </view>
    </view>
    <!-- 真正的 region picker 用可见层 -->
    <view style="position:fixed; bottom:0; left:0; width:0; height:0; overflow:hidden">
      <picker
        mode="region"
        :value="form.region"
        @change="onRegionChange">
        <view ref="regionPickerRef" style="width:0;height:0"></view>
      </picker>
    </view>
  </view>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { useAppStore } from '@/store/app'
import { toast } from '@/api/wx'

const isMp = (() => {
  // #ifdef MP-WEIXIN
  return true
  // #endif
  return false
})()

const appStore = useAppStore()
const list = ref([])
const loaded = ref(false)
const selectMode = ref(false)
const eventChannel = ref(null)

onLoad(q => {
  if (q.select === '1') selectMode.value = true
  // uni.navigateTo 会把 events 注入 eventChannel
  const pages = getCurrentPages()
  const pg = pages[pages.length - 1]
  eventChannel.value = pg.getOpenerEventChannel && pg.getOpenerEventChannel()
})
onShow(load)

/* 地址数据：优先走后端 API；后端没实现时，兜底用本地 Storage，保证能用 */
const STORAGE_KEY = 'yjcy_address_list'

async function load() {
  try {
    // 如果你后端实现了 GET /addresses，把下面注释解开即可
    // const { data } = await http.get('/addresses')
    // list.value = data
    throw new Error('fallback') // 占位
  } catch (e) {
    const local = uni.getStorageSync(STORAGE_KEY) || []
    list.value = local
    loaded.value = true
  }
}
function persist() { uni.setStorageSync(STORAGE_KEY, list.value) }

async function remove(a) {
  const r = await uni.showModal({ title:'确认删除？', content:a.detail })
  if (!r.confirm) return
  list.value = list.value.filter(x => x.id !== a.id)
  persist(); toast('已删除')
}
function setDefault(a) {
  list.value.forEach(x => x.isDefault = (x.id === a.id))
  persist(); toast('已设为默认')
  pickAndBack(a)
}

/* --- 新增 / 编辑 --- */
const showEdit = ref(false)
const editing = ref(null)
const openRegion = ref(false)
const tags = ['家','公司','学校','酒店','父母家','其他']
const defaultForm = () => ({
  id: null, name: '', phone: '', region: [], detail: '', tag: '', isDefault: false
})
const form = reactive(defaultForm())

function edit(a) {
  editing.value = a
  Object.assign(form, JSON.parse(JSON.stringify(a)))
  showEdit.value = true
}

function onRegionChange(e) {
  form.region = e.detail.value
  openRegion.value = false
}
const regionText = computed(() => form.region ? form.region.join(' / ') : '')

const canSave = computed(() =>
  form.name.trim().length >= 1 &&
  /^1\d{10}$/.test(form.phone) &&
  form.region && form.region.length === 3 &&
  form.detail.trim().length >= 5
)

function onGetPhone(e){
  import('@/api/wx').then(m => m.fetchPhoneNumber(e.detail.code))
    .then(r => { form.phone = r.phoneNumber || r.phone; toast('已带入') })
    .catch(() => toast('授权失败，请手动输入'))
}

const saving = ref(false)
async function save() {
  if (!canSave.value) return toast('请补全完整信息')
  saving.value = true
  try {
    // 默认地址：把其他的取消
    if (form.isDefault) list.value.forEach(x => x.isDefault = false)
    if (editing.value) {
      Object.assign(editing.value, JSON.parse(JSON.stringify({
        ...form,
        regionText: form.region.join(' / ')
      })))
      // 同步替换列表里对应记录
      list.value = list.value.map(x => x.id === editing.value.id ? editing.value : x)
    } else {
      const id = Date.now()
      const record = {
        id,
        ...JSON.parse(JSON.stringify(form)),
        regionText: form.region.join(' / ')
      }
      if (list.value.length === 0) record.isDefault = true
      list.value = [record, ...list.value]
    }
    persist()
    showEdit.value = false
    toast(editing.value ? '修改成功' : '新增成功')
    // 如果是选中模式：新增后自动使用新记录
    if (selectMode.value && !editing.value) {
      const last = list.value[0]
      if (last) pickAndBack(last)
    }
  } finally { saving.value = false }
}

/* --- 选中并回传给 checkout 页 --- */
function pickAndBack(a) {
  if (!selectMode.value) return
  if (eventChannel.value && eventChannel.value.emit) {
    eventChannel.value.emit('choose', a)
    uni.navigateBack()
  } else {
    // fallback：storage
    uni.setStorageSync('selected_address', a)
    uni.navigateBack()
  }
}
</script>

<style lang="scss">
@import '@/uni.scss';
.address-page{background:$page-bg; min-height:100vh; padding-bottom:180rpx}
.tips-bar{padding:20rpx 32rpx; background:linear-gradient(90deg, #FFF7E2, #FFEDBE); color:$brand-deep-gold; font-size:26rpx}
.addr-card{@include card; margin:20rpx; }
.addr-card .main{padding-bottom:20rpx; border-bottom:2rpx dashed #eee}
.addr-card .row1{@include row-start;
  .name{font-size:32rpx; font-weight:700; color:$ink-black; margin-right:20rpx}
  .phone{font-size:28rpx; color:$ink-black}
  .def{margin-left:16rpx; padding:4rpx 14rpx; background:$brand-gold; color:#fff; font-size:20rpx; border-radius:20rpx}
}
.addr-card .detail{margin-top:12rpx; font-size:28rpx; color:$ink-black; line-height:1.5}
.addr-card .tags{margin-top:10rpx; font-size:22rpx}
.addr-card .actions{@include row-end; padding-top:16rpx;
  .act{font-size:24rpx; color:$brand-deep-gold; padding:0 20rpx; border-right:2rpx solid #eee;
    &:last-child{border-right:0}
    &.danger{color:#ff5e52}
  }
}
.empty{@include column; align-items:center; padding:140rpx 40rpx; color:$text-muted;
  .icon{width:180rpx; height:180rpx; opacity:.55}
  .text{font-size:28rpx; margin:30rpx 0}
}
.gap{height:40rpx}
.footer{@include footer-bar;}

.mask{position:fixed; inset:0; background:rgba(0,0,0,.5); z-index:99; @include column; justify-content:flex-end}
.sheet{background:#fff; border-top-left-radius:24rpx; border-top-right-radius:24rpx; max-height:88vh; @include column;
  .sheet-title{@include row-between; padding:28rpx 32rpx; font-size:32rpx; font-weight:600; color:$ink-black;
    .close{font-size:40rpx; color:#bbb; padding:0 20rpx}
  }
  .sheet-body{padding:0 24rpx 48rpx; flex:1}
}
.card.no-margin{margin:0}
.row{@include row-start; align-items:center; padding:14rpx 0; border-top:2rpx dashed #eee; &:first-child{border-top:0}
  &.column{align-items:flex-start; flex-direction:column;
    .label{margin-bottom:14rpx}
    .textarea{width:100%;}
  }
  .label{font-size:28rpx; color:$ink-black; min-width:160rpx}
  .input{flex:1; padding:10rpx 16rpx; background:#fafafa; border-radius:8rpx; font-size:28rpx;
    &.picker{color:$ink-black}
  }
}
.mini-btn{font-size:22rpx; padding:0 16rpx; line-height:52rpx; height:52rpx; margin-left:16rpx;}
.gold-outline{border:2rpx solid $brand-gold; color:$brand-gold; background:#fff;}
.textarea{background:#fafafa; border-radius:8rpx; padding:16rpx; font-size:28rpx; min-height:120rpx; box-sizing:border-box}
.tag-row{.chips{flex:1; @include row-wrap;}}
.chip{@include chip;}
</style>
