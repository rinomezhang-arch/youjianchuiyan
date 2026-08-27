<template>
  <footer class="footer">
    <div class="footer-inner">
      <div class="footer-col footer-brand-col">
        <div class="brand">
          <img src="/logo.png" alt="又见炊烟私房菜 Logo" class="brand-mark" />
          <div class="brand-text">
            <span class="brand-cn">又见炊烟私房菜</span>
            <span class="brand-en">YOUJIANCHUIYAN · PRIVATE KITCHEN</span>
          </div>
        </div>
        <p class="footer-tagline">私房手艺 · 本地时令食材 · 二十余年</p>
        <p class="footer-tagline-en">Home-style Craft · Local, Seasonal Ingredients · Since 2003</p>
      </div>
      <div class="footer-col">
        <h4>门店地址 <span class="h4-en">Our Restaurants</span></h4>
        <div v-for="s in stores" :key="s.storeId" class="footer-store">
          <a class="footer-store-name" @click="router.push(`/stores/${s.storeId}`)">{{ s.storeName }}</a>
          <p class="footer-store-addr">{{ s.address }}</p>
          <p class="footer-store-phone">{{ s.phone }}</p>
        </div>
      </div>
      <div class="footer-col">
        <h4>快速入口 <span class="h4-en">Quick Links</span></h4>
        <p><a @click="router.push('/menu')">臻选菜品 <span class="link-en">Dishes</span></a></p>
        <p><a @click="router.push('/packages')">宴会套餐 <span class="link-en">Banquets</span></a></p>
        <p><a @click="router.push('/guide')">皖南攻略 <span class="link-en">Travel Guide</span></a></p>
        <p><a @click="router.push('/self-service')">加入我们 <span class="link-en">Careers</span></a></p>
        <p><a @click="router.push('/login')">员工登录 <span class="link-en">Staff Login</span></a></p>
      </div>
    </div>
    <div class="footer-bottom">
      <p>© {{ year }} 又见炊烟私房菜 · Youjianchuiyan Private Kitchen. All rights reserved.</p>
    </div>
  </footer>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()
const stores = ref([])
const year = new Date().getFullYear()

onMounted(async () => {
  try {
    const res = await request.get('/api/public/stores')
    stores.value = (res.data || []).map(s => ({
      storeId: s.store_id ?? s.storeId,
      storeName: s.store_name ?? s.storeName,
      address: s.address,
      phone: s.phone
    }))
  } catch (e) {
    stores.value = []
  }
})
</script>

<style scoped>
.footer { background: #16241C; padding: 68px 40px 28px; }
.footer-inner {
  max-width: 1240px; margin: 0 auto;
  display: grid; grid-template-columns: 1.8fr 1fr 1fr; gap: 64px;
  padding-bottom: 40px; border-bottom: 1px solid rgba(255,255,255,0.1);
}
.footer-brand-col .brand { display: flex; align-items: center; gap: 14px; margin-bottom: 18px; }
.brand-mark { width: 40px; height: 40px; object-fit: contain; flex-shrink: 0; }
.brand-text { display: flex; flex-direction: column; line-height: 1.3; }
.brand-cn { font-size: 19px; font-weight: 700; color: #fff; letter-spacing: 1.5px; }
.brand-en { font-size: 9.5px; color: #D4B483; letter-spacing: 2px; margin-top: 4px; }
.footer-tagline { color: rgba(255,255,255,0.55); font-size: 13px; margin: 0 0 4px; }
.footer-tagline-en { color: rgba(255,255,255,0.32); font-size: 11px; margin: 0; letter-spacing: 0.3px; }

.footer-col h4 { color: #fff; font-size: 14px; margin: 0 0 20px; letter-spacing: 1px; display: flex; flex-direction: column; gap: 4px; }
.h4-en { font-size: 9.5px; color: rgba(255,255,255,0.4); letter-spacing: 1.5px; font-weight: 400; text-transform: uppercase; }
.footer-col p { color: rgba(255,255,255,0.6); font-size: 13px; margin: 0 0 13px; }
.footer-col a { cursor: pointer; display: inline-flex; align-items: baseline; gap: 8px; }
.footer-col a:hover { color: #D4B483; }
.link-en { font-size: 10px; color: rgba(255,255,255,0.35); letter-spacing: 0.3px; }

.footer-store { margin-bottom: 20px; }
.footer-store-name { display: block; color: #D4B483; font-size: 14px; font-weight: 700; margin-bottom: 6px; cursor: pointer; }
.footer-store-addr { font-size: 12.5px; color: rgba(255,255,255,0.55); margin: 0 0 3px; line-height: 1.6; }
.footer-store-phone { font-size: 12.5px; color: rgba(255,255,255,0.45); margin: 0; }

.footer-bottom { max-width: 1240px; margin: 0 auto; padding-top: 24px; text-align: center; }
.footer-bottom p { color: rgba(255,255,255,0.35); font-size: 12px; margin: 0; }

@media (max-width: 960px) {
  .footer-inner { grid-template-columns: 1fr; gap: 36px; }
}
</style>
