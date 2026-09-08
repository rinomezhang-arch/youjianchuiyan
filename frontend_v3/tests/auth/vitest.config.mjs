// TR-AUTH-SHELL-14 测试专用 vitest 配置。
// 放在 tests/auth/ 内以遵守任务 allowed_paths（不改 frontend_v3 根目录文件）。
// 测试工具 vitest/@vue/test-utils/happy-dom 全部 --no-save 安装，package.json 零改动。
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
    include: ['tests/auth/**/*.test.mjs'],
    setupFiles: ['./tests/auth/setup.dom.mjs'],
    testTimeout: 30000
  }
})
