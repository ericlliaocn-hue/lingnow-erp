import { defineStore } from 'pinia'
import type { ShopProduct } from '@/types/shop'

export interface CartItem {
  key: string
  productId: string
  productCode?: string
  productName: string
  productImageUrl?: string
  spec?: string
  price: number
  qty: number
}

const CART_KEY = 'rs-shop-cart'

export const useCartStore = defineStore('cart', {
  state: () => ({
    items: readCart()
  }),
  getters: {
    totalQty: (state) => state.items.reduce((sum, item) => sum + Number(item.qty || 0), 0),
    totalAmount: (state) => state.items.reduce((sum, item) => sum + Number(item.qty || 0) * Number(item.price || 0), 0)
  },
  actions: {
    addProduct(product: ShopProduct, qty = 1) {
      const productId = String(product.id)
      const current = this.items.find(item => item.productId === productId)
      if (current) {
        current.qty += qty
      } else {
        this.items.push({
          key: `${Date.now()}-${productId}`,
          productId,
          productCode: product.code,
          productName: product.name,
          productImageUrl: product.imageUrl,
          spec: product.spec,
          price: Number(product.salePrice || 0),
          qty
        })
      }
      this.persist()
    },
    updateQty(key: string, qty: number) {
      const item = this.items.find(record => record.key === key)
      if (!item) return
      item.qty = Math.max(1, Number(qty || 1))
      this.persist()
    },
    remove(key: string) {
      this.items = this.items.filter(item => item.key !== key)
      this.persist()
    },
    clear() {
      this.items = []
      this.persist()
    },
    persist() {
      localStorage.setItem(CART_KEY, JSON.stringify(this.items))
    }
  }
})

function readCart(): CartItem[] {
  const raw = localStorage.getItem(CART_KEY)
  if (!raw) return []
  try {
    const records = JSON.parse(raw) as CartItem[]
    return Array.isArray(records) ? records.filter(item => item.productId && item.productName) : []
  } catch (err) {
    localStorage.removeItem(CART_KEY)
    return []
  }
}
