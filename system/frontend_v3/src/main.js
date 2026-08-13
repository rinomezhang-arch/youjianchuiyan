import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import App from './App.vue'
import router from './router'
import i18n from './i18n'
import '@fontsource/noto-sans-sc/400.css'
import '@fontsource/noto-sans-sc/500.css'
import '@fontsource/noto-sans-sc/600.css'
import '@fontsource/noto-sans-sc/700.css'
import './assets/styles/global.css'

const app = createApp(App)
const pinia = createPinia()

app.use(ElementPlus, { locale: zhCn })
app.use(pinia)
app.use(router)
app.use(i18n)
app.mount('#app')
