// TR-MARKETING-H5-UI-39 测试专用 vitest 配置（放在 tests/marketing/ 内遵守 allowed_paths）。
// vitest/@vue/test-utils/happy-dom 通过目录联接复用既有 --no-save 安装，package.json 零改动。
import { fileURLToPath, URL } from 'node:url'
import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vitest/config'

export default defineConfig({
  root: fileURLToPath(new URL('../..', import.meta.url)),
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('../../src', import.meta.url))
    }
  },
  test: {
    environment: 'happy-dom',
    include: ['tests/marketing/**/*.test.mjs'],
    setupFiles: ['./tests/marketing/setup.dom.mjs'],
    testTimeout: 30000
  }
})
