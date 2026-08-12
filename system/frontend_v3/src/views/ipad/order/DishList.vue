<template>
  <div class="ipad-page">
    <div class="page-top">
      <button class="back-link" @click="router.back()">← 返回点餐</button>
      <h1 class="page-title">已点菜品 · Ordered</h1>
    </div>
    <div class="cart-list">
      <div v-for="item in ipad.cartItems" :key="item.dish_id" class="cart-item">
        <div class="item-info">
          <div class="item-name">{{ item.dish_name }}</div>
          <div class="item-price">¥{{ Number(item.sale_price || item.unit_price).toFixed(0) }}</div>
        </div>
        <div class="item-qty">
          <button @click="ipad.updateCartQty(item.dish_id, item.dish_quantity - 1)">−</button>
          <span>{{ item.dish_quantity }}</span>
          <button @click="ipad.updateCartQty(item.dish_id, item.dish_quantity + 1)">+</button>
        </div>
      </div>
      <div v-if="!ipad.cartItems.length" class="empty-state">
        <p>还没有点菜</p>
        <p class="empty-en">No dishes ordered yet</p>
      </div>
    </div>
    <div class="cart-footer" v-if="ipad.cartItems.length">
      <div class="cart-summary">
        <span>合计 · Total</span>
        <span class="cart-total">¥{{ ipad.cartTotal.toFixed(2) }}</span>
      </div>
      <div class="cart-actions">
        <button class="btn-clear" @click="ipad.clearCart()">清空</button>
        <button class="btn-submit" @click="submitToKitchen">提交后厨 · Submit</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useIpadStore } from '@/store/ipad'
import { ipadOrderSendKitchen } from '@/api/ipad'
import { ElMessage } from 'element-plus'
import { fallbackOrThrow, errorMessage } from '@/utils/fallback'

const router = useRouter()
const ipad = useIpadStore()

async function submitToKitchen() {
  if (!ipad.currentBooking?.booking_id) { ElMessage.warning('请先开台'); return }
  try {
    const res = await ipadOrderSendKitchen(ipad.currentBooking.booking_id)
    if (res.code === 200) { ElMessage.success('已提交后厨'); router.back() }
    else ElMessage.error(res.msg || '提交失败')
  } catch (error) {
    try {
      fallbackOrThrow(error, () => router.back())
    } catch (productionError) {
      ElMessage.error(errorMessage(productionError, '提交后厨失败'))
    }
  }
}
</script>

<style scoped>
.ipad-page { width: 100%; height: 100%; display: flex; flex-direction: column; background: var(--color-bg); }
.page-top { padding: 16px 24px; background: var(--color-card); border-bottom: 1px solid var(--color-border); display: flex; align-items: center; gap: 16px; flex-shrink: 0; }
.back-link { border: none; background: none; color: var(--color-text-muted); font-size: 13px; cursor: pointer; }
.page-title { font-size: 18px; font-weight: 700; color: var(--color-text); letter-spacing: 2px; }
.cart-list { flex: 1; overflow-y: auto; padding: 20px 24px; display: flex; flex-direction: column; gap: 10px; }
.cart-item { display: flex; align-items: center; justify-content: space-between; padding: 16px 20px; background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-lg); }
.item-name { font-size: 16px; font-weight: 600; color: var(--color-text); }
.item-price { font-size: 14px; color: var(--color-accent-dark); font-weight: 600; margin-top: 2px; }
.item-qty { display: flex; align-items: center; gap: 12px; }
.item-qty button { width: 32px; height: 32px; border-radius: 50%; border: 1px solid var(--color-border); background: var(--color-card); font-size: 18px; cursor: pointer; }
.item-qty button:hover { background: var(--color-primary); color: white; border-color: var(--color-primary); }
.item-qty span { font-size: 18px; font-weight: 700; min-width: 32px; text-align: center; }
.empty-state { text-align: center; padding: 60px; color: var(--color-text-muted); }
.empty-en { font-size: 12px; margin-top: 4px; }
.cart-footer { padding: 16px 24px; background: var(--color-card); border-top: 1px solid var(--color-border); flex-shrink: 0; }
.cart-summary { display: flex; justify-content: space-between; margin-bottom: 12px; font-size: 15px; color: var(--color-text-secondary); }
.cart-total { font-size: 24px; font-weight: 700; color: var(--color-accent-dark); }
.cart-actions { display: flex; gap: 12px; }
.btn-clear { flex: 1; padding: 12px; border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-card); font-size: 14px; cursor: pointer; }
.btn-submit { flex: 2; padding: 12px; border: none; border-radius: var(--radius-md); background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light)); color: white; font-size: 14px; font-weight: 700; cursor: pointer; letter-spacing: 1px; }
.btn-submit:hover { transform: translateY(-1px); box-shadow: 0 4px 14px rgba(45,74,62,0.3); }
</style>
