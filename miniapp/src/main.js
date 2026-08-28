import { createSSRApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'

export function createApp() {
  const app = createSSRApp(App)
  app.use(createPinia())

  // 全局方法挂载（Vue3 建议用 inject，这里保留快捷访问）
  app.config.globalProperties.$storeId = () => {
    const { appStore } = require('./store/app').useAppStore()
    return appStore.currentStoreId
  }

  return {
    app
  }
}
