// happy-dom 环境下的 storage 兜底。
// Node 22+ 自带实验性 localStorage 全局 getter（未提供 --localstorage-file 时返回 undefined），
// 它会遮蔽 happy-dom 注入的 window.localStorage，导致业务代码里的裸 localStorage 全是 undefined。
// 这里无条件覆写为内存实现，保证测试与被测代码走的是同一个 storage 对象。

function createMemoryStorage() {
  const map = new Map()
  return {
    getItem: (key) => (map.has(String(key)) ? map.get(String(key)) : null),
    setItem: (key, value) => {
      map.set(String(key), String(value))
    },
    removeItem: (key) => {
      map.delete(String(key))
    },
    clear: () => {
      map.clear()
    },
    key: (index) => Array.from(map.keys())[index] ?? null,
    get length() {
      return map.size
    }
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
