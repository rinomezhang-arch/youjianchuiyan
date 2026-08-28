<template>
  <view class="agreement-page">
    <view class="header">
      <view class="title">{{ type === 'privacy' ? '隐私政策' : '用户服务协议' }}</view>
      <view class="date muted">最近更新：2026-06-01 · 生效：2026-06-01</view>
    </view>
    <scroll-view scroll-y class="body">
      <view class="p">
        <text v-if="type === 'privacy'">欢迎使用「又见炊烟」小程序（以下简称"我们"）。本隐私政策将帮助您了解：</text>
        <text v-else>欢迎使用「又见炊烟」小程序（以下简称"本平台"）。在使用本平台服务前，请您务必仔细阅读以下条款：</text>
      </view>

      <view v-for="sec in content" :key="sec.t" class="sec">
        <view class="sec-title">{{ sec.t }}</view>
        <view class="sec-body">
          <view class="p" v-for="(p, i) in sec.ps" :key="i">{{ p }}</view>
        </view>
      </view>

      <view class="contact">
        <view class="sec-title">📮 联系我们</view>
        <view class="p">如果您对本协议有任何疑问、意见或建议，可通过以下方式联系我们：</view>
        <view class="p">运营主体：又见炊烟餐饮管理（宣城）有限公司</view>
        <view class="p">注册地址：宣城市宁国市宁城北路 128 号</view>
        <view class="p">客服电话：400-0000-000（工作日 9:00-21:00）</view>
        <view class="p">客服邮箱：privacy@youjianchuiyan.com</view>
      </view>

      <view style="height:80rpx"></view>
    </scroll-view>
    <view class="footer">
      <button class="gold-btn big" @tap="agree">我已阅读并同意</button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

const type = ref('service')
onLoad(q => { type.value = q.type || 'service' })

const content = computed(() => {
  if (type.value === 'privacy') {
    return [
      { t: '一、我们收集的信息', ps:[
        '1. 注册/登录：当您使用微信登录时，我们会获取您的微信头像、昵称与 openid 用于创建账号；',
        '2. 手机号：当您点击"一键授权手机号"或主动输入时，我们会将手机号与账号绑定，用于预订确认、取餐通知、营销信息（若您同意）送达；',
        '3. 预订/下单：为了履行订单，我们会收集用餐人数、日期时段、桌号包厢偏好、收货地址（外卖）、备注忌口等；',
        '4. 位置：仅在您授权后用于展示"附近门店"，并会以匿名方式用于客流热力分析；',
        '5. 设备/日志：为保证账号安全与反作弊，会收集设备型号、系统版本、IP、请求日志。'
      ]},
      { t: '二、我们如何使用信息', ps:[
        '· 用于完成预订、下单、支付、开票、售后等合同义务；',
        '· 用于向您发送预订成功、取餐提醒、订单状态变更等必要通知；',
        '· 在您明确授权后发送优惠券、活动邀请、节日营销；可在"设置-消息通知"关闭；',
        '· 以匿名/聚合方式进行经营分析、菜品推荐算法训练。'
      ]},
      { t: '三、信息共享与披露', ps:[
        '我们不会出售您的个人信息。仅在以下场景共享：',
        '· 第三方服务商：微信支付、短信通道、地图/导航、云存储等，仅为实现前述功能且签署 DPA；',
        '· 执法机关：在法律要求或行政/司法请求下提供；',
        '· 并购重组：主体变更时会随资产转移，并提前 30 日通知。'
      ]},
      { t: '四、您的权利', ps:[
        '您有权随时访问、更正、删除您的个人资料，可在"我的 → 设置 → 个人资料"中自助操作；',
        '有权撤回同意（如消息推送、位置）、注销账号（设置 → 退出登录 → 注销账号）；',
        '对上述操作有疑问的，通过下方联系方式发函，我们会在 15 个工作日内响应。'
      ]},
      { t: '五、信息存储与安全', ps:[
        '数据存储于中国境内的腾讯云服务器（上海地域），传输全程采用 HTTPS/TLS1.3；',
        '敏感字段（手机号、身份证）采用 AES-256 加密落库，员工按最小权限分级访问；',
        '我们会按法律要求的最短期限保留数据，到期后删除或匿名化。'
      ]},
      { t: '六、未成年人保护', ps:[
        '若您未满 14 周岁，需由监护人陪同阅读本政策并在其同意下使用我们的服务；',
        '如监护人发现我们未经同意收集了未成年人信息，请立即联系我们删除。'
      ]}
    ]
  }
  return [
    { t: '一、服务内容', ps:[
      '又见炊烟小程序为"又见炊烟"连锁门店提供：门店查询、在线预订桌位/包厢、扫码点餐、宴会套餐预约、会员优惠券、订单管理、外卖/自取等服务。'
    ]},
    { t: '二、账号注册与使用', ps:[
      '您应当使用真实信息完成注册，不得冒用他人身份；',
      '账号仅限本人使用，转借、出租导致的损失由您自行承担；',
      '我们有权对涉及刷单、套现、恶意投诉等违规行为暂停/封禁账号。'
    ]},
    { t: '三、预订与订单', ps:[
      '预订成功以"商家确认"状态为准。包间预订需在 30 分钟内支付定金，超时自动释放；',
      '如需取消/修改请至少提前 2 小时操作。定金类预订未到店且未提前取消的，定金不予退还；',
      '菜品价格以门店当日为准，小程序显示价格遇特殊食材浮动会电话确认。'
    ]},
    { t: '四、支付与发票', ps:[
      '平台支持微信支付（JSAPI/小程序支付），交易流水由微信支付与商户号共同存证；',
      '如需发票请在订单详情页申请，电子发票将在 3 个工作日内发送至您的邮箱；',
      '退款原路退回，到账时间一般 1-7 个工作日以银行为准。'
    ]},
    { t: '五、优惠券与活动', ps:[
      '优惠券有效期、使用门槛以券面为准，不可折现、不可兑换现金，不可叠加使用；',
      '通过不正当手段（脚本、多账号）套券的，平台有权撤销并冻结账号；',
      '活动最终解释权在法律允许范围内归又见炊烟所有。'
    ]},
    { t: '六、禁止行为', ps:[
      '禁止发布辱骂、歧视、不实评价；禁止在门店/小程序传播违法、违反公序良俗的内容；',
      '禁止以技术手段干扰小程序正常运行、爬虫批量获取数据。'
    ]},
    { t: '七、免责条款', ps:[
      '因系统维护、不可抗力、第三方服务故障导致服务中断的，平台会尽快恢复但不承担违约责任；',
      '菜品图片可能存在因拍摄光线引起的色差，请以实际出品为准。'
    ]},
    { t: '八、协议修改', ps:[
      '我们可能不定期修订本协议，重大变更会提前 7 日在小程序公告并弹窗征求同意；',
      '若您不同意新条款，应停止使用服务；继续使用视为同意修订版。'
    ]}
  ]
})

function agree(){
  // 如果是被登录页唤起，就回传同意
  try {
    const pages = getCurrentPages()
    const pg = pages[pages.length-1]
    const ch = pg.getOpenerEventChannel && pg.getOpenerEventChannel()
    if (ch && ch.emit) { ch.emit('agree', type.value) }
  } catch(e){}
  uni.navigateBack()
}
</script>

<style lang="scss">
@import '@/uni.scss';
.agreement-page{background:$page-bg; min-height:100vh; display:flex; flex-direction:column}
.header{background:linear-gradient(180deg,#fff,#fff8e6); padding:40rpx 32rpx 36rpx; border-bottom-left-radius:30rpx; border-bottom-right-radius:30rpx;
  .title{font-size:40rpx; font-weight:700; color:$ink-black}
  .date{margin-top:10rpx; font-size:24rpx}
}
.body{flex:1; padding:24rpx 32rpx;}
.sec{margin-bottom:32rpx;
  .sec-title{font-size:30rpx; font-weight:700; color:$brand-deep-gold; margin-bottom:14rpx}
  .sec-body{padding-left:4rpx}
}
.p{font-size:26rpx; color:$ink-black; line-height:1.8; margin-bottom:12rpx}
.contact{margin-top:40rpx; padding:28rpx; background:#fffaf0; border:2rpx dashed #e6cf9e; border-radius:16rpx}
.footer{@include footer-bar}
</style>
