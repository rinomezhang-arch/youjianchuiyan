/**
 * 购物车 Store：菜单页加购菜品用
 * 数据同时持久化到 storage，不同门店相互隔离
 */
import { defineStore } from 'pinia'
import storage from '@/utils/storage'
import { STORAGE_KEYS } from '@/config/env'
import { useAppStore } from './app'

const cartKey = (sid) => `${STORAGE_KEYS.CART}${sid}`

export const useCartStore = defineStore('cart', {
  state: () => ({
    items: []   // [{ id, name, image, price, count, categoryId }]
  }),
  getters: {
    totalCount: (s) => s.items.reduce((n, i) => n + (i.count || 0), 0),
    totalFen:   (s) => s.items.reduce((n, i) => n + (i.count || 0) * (Number(i.priceFen) || Number(i.price) * 100 || 0), 0),
    isEmpty:    (s) => s.items.length === 0,
  },
  actions: {
    /** 每次读最新 storeId 从 storage 拉 */
    hydrate() {
      const sid = useAppStore().currentStoreId || 1
      this.items = storage.get(cartKey(sid), []) || []
    },
    /** 写入 storage（每次增删改都自动调） */
    persist() {
      const sid = useAppStore().currentStoreId || 1
      storage.set(cartKey(sid), this.items)
    },
    findIndex(id) { return this.items.findIndex(i => i.id === id) },
    add(dish, delta = 1) {
      if (!dish || !dish.id) return
      const idx = this.findIndex(dish.id)
      if (idx >= 0) {
        const after = (this.items[idx].count || 0) + delta
        if (after <= 0) this.items.splice(idx, 1)
        else this.items[idx].count = after
      } else if (delta > 0) {
        this.items.push({
          id:         dish.id,
          name:       dish.name,
          image:      dish.image || dish.imageUrl || '',
          price:      dish.price,        // 元（后端返回展示用）
          priceFen:   dish.priceFen != null ? dish.priceFen : Math.round(Number(dish.price || 0) * 100),
          categoryId: dish.categoryId || dish.dishCategoryId,
          count:      delta,
          specs:      dish.specs || dish.unit || ''
        })
      }
      this.persist()
    },
    remove(id) {
      const idx = this.findIndex(id)
      if (idx >= 0) { this.items.splice(idx, 1); this.persist() }
    },
    clear() {
      this.items = []
      this.persist()
    }
  }
})
