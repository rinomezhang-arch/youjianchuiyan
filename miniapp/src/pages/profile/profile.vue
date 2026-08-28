<template>
  <view class="profile-page">
    <view class="avatar-row">
      <view class="avatar-wrap" @tap="chooseAvatar">
        <image class="avatar" :src="form.avatar || defaultAvatar" mode="aspectFill"/>
        <view class="camera">📷</view>
      </view>
      <view class="tip muted">点击头像更换图片（最多 2MB）</view>
    </view>

    <view class="group">
      <view class="item">
        <text class="label">昵称</text>
        <!-- #ifdef MP-WEIXIN -->
        <button
          class="nickname-btn"
          v-if="!form.nickname"
          open-type="chooseAvatar"
          @chooseavatar="onChooseAvatar">
          <!-- 用 placeholder + input 双通道 -->
        </button>
        <input
          v-model="form.nickname"
          class="input"
          type="nickname"
          placeholder="请输入昵称"
          maxlength="20"/>
        <!-- #endif -->
        <!-- #ifndef MP-WEIXIN -->
        <input v-model="form.nickname" class="input" placeholder="请输入昵称" maxlength="20"/>
        <!-- #endif -->
      </view>
      <view class="item">
        <text class="label">性别</text>
        <view class="genders">
          <text
            v-for="g in genders"
            :key="g.v"
            class="gender"
            :class="{active: form.gender === g.v}"
            @tap="form.gender = g.v">
            {{ g.label }}
          </text>
        </view>
      </view>
      <view class="item" @tap="openBirthday">
        <text class="label">生日</text>
        <picker mode="date" :value="form.birthday" @change="onBirthday">
          <view class="input picker">{{ form.birthday || '请选择生日（可选）' }}</view>
        </picker>
      </view>
      <view class="item">
        <text class="label">手机号</text>
        <view class="input">{{ user?.phone || '未绑定' }}</view>
        <button
          v-if="isMp && !user?.phone"
          class="mini-btn gold-outline"
          open-type="getPhoneNumber"
          @getphonenumber="onGetPhone">绑定</button>
      </view>
      <view class="item">
        <text class="label">常去门店</text>
        <view class="input picker" @tap="pickStore">{{ appStore.currentStore?.name || '选择常去门店' }}</view>
      </view>
    </view>

    <view class="group">
      <view class="item column">
        <text class="label">个人简介</text>
        <textarea v-model="form.bio" class="textarea" maxlength="100" placeholder="一句话介绍自己 / 口味偏好 / 忌口"/>
        <text class="count muted">{{ form.bio.length }}/100</text>
      </view>
    </view>

    <view class="gap"></view>
    <view class="footer">
      <button class="gold-btn big" :loading="saving" :disabled="!canSave" @tap="save">保存资料</button>
    </view>
  </view>
</template>

<script setup>
import { reactive, computed, onMounted } from 'vue'
import { toast, fetchPhoneNumber } from '@/api/wx'
import uploadApi from '@/api/upload'
import userApi from '@/api/user'
import { useAppStore } from '@/store/app'

const appStore = useAppStore()
const user = computed(() => appStore.user)
const defaultAvatar = 'https://img.icons8.com/ios/200/C9A86C/user-male-circle.png'
const isMp = (() => {
  // #ifdef MP-WEIXIN
  return true
  // #endif
  return false
})()

const genders = [{v:0, label:'保密'},{v:1, label:'先生'},{v:2, label:'女士'}]
const form = reactive({
  avatar: '',
  nickname: '',
  gender: 0,
  birthday: '',
  bio: ''
})

onMounted(() => {
  if (user.value) {
    Object.assign(form, {
      avatar: user.value.avatar || '',
      nickname: user.value.nickname || '',
      gender: typeof user.value.gender === 'number' ? user.value.gender : 0,
      birthday: user.value.birthday || '',
      bio: user.value.bio || ''
    })
  }
})

const canSave = computed(() => form.nickname.trim().length >= 1)

/* 头像 */
async function chooseAvatar(){
  try {
    const urls = await uploadApi.chooseAndUpload({ count: 1 })
    if (urls?.[0]) form.avatar = urls[0]
  } catch(e){}
}
function onChooseAvatar(e) {
  // 2.27+ 小程序同时回传 avatarUrl（本地临时路径），上传一下
  const tempPath = e?.detail?.avatarUrl
  if (tempPath) {
    uploadApi.uploadFile(tempPath).then(url => { form.avatar = url })
      .catch(() => { form.avatar = tempPath })
  }
}
function onBirthday(e){ form.birthday = e.detail.value }
function openBirthday() { /* picker 外层已绑定点击 */ }
async function onGetPhone(e){
  try {
    const r = await fetchPhoneNumber(e.detail.code)
    const phone = r?.phoneNumber || r?.phone
    if (phone) {
      // 同步到 app store
      appStore.updateUser({ phone })
      toast('手机号绑定成功')
    }
  } catch(e2) { toast('绑定失败') }
}
function pickStore(){ uni.navigateTo({ url:'/pages/stores/stores' }) }

/* 保存 */
const saving = ref(false)
async function save(){
  if (!canSave.value) return toast('请填写昵称')
  saving.value = true
  try {
    const payload = { ...form }
    // 兼容后端两种：有 /user/profile 接口就走它，没有就直接存本地
    const r = await userApi.updateProfile(payload).catch(() => null)
    appStore.updateUser(payload)
    uni.showModal({
      title:'保存成功',
      content: '资料已更新～',
      showCancel:false,
      success: () => uni.navigateBack()
    })
  } catch(e) {
    // 兜底：直接更新 Pinia 并返回
    appStore.updateUser(form)
    toast('已保存')
    setTimeout(() => uni.navigateBack(), 500)
  } finally {
    saving.value = false
  }
}
import { ref } from 'vue'
</script>

<style lang="scss">
@import '@/uni.scss';
.profile-page{background:$page-bg; min-height:100vh; padding:20rpx 0 180rpx}
.avatar-row{padding:40rpx 0; @include column; align-items:center;
  .avatar-wrap{width:200rpx; height:200rpx; border-radius:50%; position:relative;
    .avatar{width:100%; height:100%; border-radius:50%; background:#eee; box-shadow:0 12rpx 30rpx rgba(201,168,108,.3); border:6rpx solid #fff}
    .camera{position:absolute; right:0; bottom:0; width:60rpx; height:60rpx; border-radius:50%; background:$brand-gold; color:#fff; text-align:center; line-height:60rpx; font-size:30rpx; border:4rpx solid #fff}
  }
  .tip{margin-top:16rpx}
}
.group{background:#fff; margin:0 20rpx 20rpx; border-radius:20rpx; overflow:hidden; box-shadow:$shadow-card}
.item{@include row-center; padding:28rpx 28rpx; border-top:2rpx solid #f5f0e2; &:first-child{border-top:0; align-items:center}
  &.column{align-items:flex-start; flex-direction:column;
    .label{margin-bottom:18rpx} .textarea{width:100%;}
  }
  .label{min-width:180rpx; font-size:28rpx; color:$ink-black; font-weight:500}
  .input{flex:1; padding:10rpx 16rpx; background:#fafafa; border-radius:10rpx; font-size:28rpx; color:$ink-black;
    &.picker{color:$ink-black}
  }
  .mini-btn{font-size:22rpx; padding:0 16rpx; line-height:52rpx; height:52rpx; margin-left:16rpx;}
  .gold-outline{border:2rpx solid $brand-gold; color:$brand-gold; background:#fff}
  .count{text-align:right; margin-top:8rpx}
}
.nickname-btn{height:60rpx; margin:0; padding:0; background:transparent; line-height:60rpx; font-size:24rpx; color:$brand-gold; border:0; &::after{border:0}}
.genders{@include row-wrap; flex:1;
  .gender{padding:10rpx 24rpx; background:#fafafa; border:2rpx solid #eee; border-radius:30rpx; margin-right:14rpx; font-size:26rpx; color:$text-muted;
    &.active{background:rgba(201,168,108,0.12); color:$brand-deep-gold; border-color:$brand-gold}
  }
}
.textarea{background:#fafafa; border-radius:10rpx; padding:16rpx; font-size:28rpx; min-height:160rpx; box-sizing:border-box}
.gap{height:40rpx}
.footer{@include footer-bar;}
</style>
