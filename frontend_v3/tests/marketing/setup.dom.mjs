// happy-dom 环境下的 storage 兜底（与 tests/auth/setup.dom.mjs 同款，Node22 实验全局遮蔽问题）。
function createMemoryStorage() {
  const map = new Map()
  return {
    getItem: (key) => (map.has(String(key)) ? map.get(String(key)) : null),
    setItem: (key, value) => { map.set(String(key), String(value)) },
    removeItem: (key) => { map.delete(String(key)) },
    clear: () => { map.clear() },
    key: (index) => Array.from(map.keys())[index] ?? null,
    get length() { return map.size }
  }
}
for (const name of ['localStorage', 'sessionStorage']) {
  const current = globalThis[name]
  if (current && typeof current.clear === 'function' && typeof current.setItem === 'function') continue
  Object.defineProperty(globalThis, name, {
    configurable: true,
    writable: true,
    value: createMemoryStorage()
  })
}

// Element Plus 弹层/响应式在 happy-dom 下需要的最小环境补齐（无布局，仅防构造期抛错）。
if (typeof globalThis.ResizeObserver === 'undefined') {
  class RO {
    observe() {}
    unobserve() {}
    disconnect() {}
  }
  globalThis.ResizeObserver = RO
  if (globalThis.window) globalThis.window.ResizeObserver = RO
}
if (typeof globalThis.matchMedia === 'undefined') {
  const mm = (query) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener() {},
    removeListener() {},
    addEventListener() {},
    removeEventListener() {},
    dispatchEvent() { return false }
  })
  globalThis.matchMedia = mm
  if (globalThis.window) globalThis.window.matchMedia = mm
}
if (typeof globalThis.requestAnimationFrame === 'undefined') {
  globalThis.requestAnimationFrame = (cb) => setTimeout(() => cb(Date.now()), 0)
  globalThis.cancelAnimationFrame = (id) => clearTimeout(id)
}
