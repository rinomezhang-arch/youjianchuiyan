import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
      imports: ['vue', 'vue-router', 'pinia']
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dirs: ['src/components']
    })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: '0.0.0.0',
    allowedHosts: ['.vercel.run', 'localhost'],
    port: 5173,
    watch: {
      usePolling: false,
      ignored: ['**/public/dish/**']
    },
    proxy: {
      '/api/hr': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/api/dilong': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        configure: proxy => {
          // 浏览器请求与 Vite 同源；转发到后端时移除外层 Origin，避免后端误判为跨域。
          proxy.on('proxyReq', proxyReq => proxyReq.removeHeader('origin'))
        }
      },
      '/menu-api': {
        target: 'http://localhost:3001',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    assetsDir: 'assets'
  }
})
