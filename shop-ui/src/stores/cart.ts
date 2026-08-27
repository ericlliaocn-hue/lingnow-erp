import { defineStore } from 'pinia'
import type { ShopProduct } from '@/types/shop'

export interface CartItem {
  key: string
  productId: string
  productCode?: string
  productName: string
  productImageUrl?: string
  spec?: string
  basePrice: number
  attributeExtraAmount: number
  price: number
  optionAttributeIds: string
  optionAttributeText: string
  optionAttributeQuantityJson: string
  optionQuantities: Record<string, number>
  logoImageUrl?: string
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
    addConfigured(product: ShopProduct, configuration: {
      optionAttributeIds?: string
      optionAttributeText?: string
      optionAttributeQuantityJson?: string
      optionQuantities?: Record<string, number>
      attributeExtraAmount?: number
      logoImageUrl?: string
    }, qty = 1) {
      const productId = String(product.id)
      const optionAttributeIds = configuration.optionAttributeIds || ''
      const optionQuantities = configuration.optionQuantities || {}
      const optionAttributeQuantityJson = configuration.optionAttributeQuantityJson || JSON.stringify(optionQuantities)
      const signature = `${productId}|${optionAttributeQuantityJson}|${configuration.logoImageUrl || ''}`
      const current = this.items.find(item => item.key === signature)
      if (current) {
        current.qty += qty
        current.attributeExtraAmount += Number(configuration.attributeExtraAmount || 0)
        current.optionQuantities = mergeOptionQuantities(current.optionQuantities, optionQuantities)
        current.optionAttributeQuantityJson = JSON.stringify(current.optionQuantities)
        current.optionAttributeText = mergeOptionAttributeText(
          current.optionAttributeText,
          current.optionAttributeIds,
          current.optionQuantities
        )
        current.price = effectivePrice(current.basePrice, current.qty, current.attributeExtraAmount)
      } else {
        const basePrice = Number(product.salePrice || 0)
        const attributeExtraAmount = Number(configuration.attributeExtraAmount || 0)
        this.items.push({
          key: signature,
          productId,
          productCode: product.code,
          productName: product.name,
          productImageUrl: product.imageUrl,
          spec: product.spec,
          basePrice,
          attributeExtraAmount,
          price: effectivePrice(basePrice, qty, attributeExtraAmount),
          optionAttributeIds,
          optionAttributeText: configuration.optionAttributeText || '',
          optionAttributeQuantityJson,
          optionQuantities,
          logoImageUrl: configuration.logoImageUrl,
          qty
        })
      }
      this.persist()
    },
    updateQty(key: string, qty: number) {
      const item = this.items.find(record => record.key === key)
      if (!item) return
      item.qty = Math.max(1, Number(qty || 1))
      item.price = effectivePrice(item.basePrice, item.qty, item.attributeExtraAmount)
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
    return Array.isArray(records) ? records.filter(item => item.productId && item.productName).map(item => ({
      ...item,
      basePrice: Number(item.basePrice ?? item.price ?? 0),
      attributeExtraAmount: item.optionAttributeQuantityJson
        ? Number(item.attributeExtraAmount || 0)
        : Number(item.attributeExtraAmount || 0) * Number(item.qty || 1),
      optionAttributeIds: item.optionAttributeIds || '',
      optionAttributeText: item.optionAttributeText || '',
      optionAttributeQuantityJson: item.optionAttributeQuantityJson || '',
      optionQuantities: item.optionQuantities || (item.optionAttributeQuantityJson
        ? parseOptionQuantities(item.optionAttributeQuantityJson)
        : legacyOptionQuantities(item.optionAttributeIds, item.qty))
    })) : []
  } catch (err) {
    localStorage.removeItem(CART_KEY)
    return []
  }
}

function effectivePrice(basePrice: number, qty: number, optionTotal: number) {
  return (Number(basePrice || 0) * Number(qty || 0) + Number(optionTotal || 0)) / Math.max(1, Number(qty || 1))
}

function parseOptionQuantities(value?: string) {
  if (!value) return {}
  try {
    return JSON.parse(value) as Record<string, number>
  } catch {
    return {}
  }
}

function legacyOptionQuantities(ids?: string, qty?: number) {
  return Object.fromEntries((ids || '').split(',').map(id => id.trim()).filter(Boolean).map(id => [id, Number(qty || 1)]))
}

function mergeOptionQuantities(current: Record<string, number>, added: Record<string, number>) {
  const result = { ...(current || {}) }
  Object.entries(added || {}).forEach(([id, qty]) => {
    result[id] = Number(result[id] || 0) + Number(qty || 0)
  })
  return result
}

function mergeOptionAttributeText(text: string, optionIds: string, quantities: Record<string, number>) {
  const ids = (optionIds || '').split(',').map(id => id.trim()).filter(Boolean)
  return (text || '').split('/').map((part, index) => {
    const label = part.trim().replace(/\s*[×x]\s*\d+(?:\.\d+)?\s*$/, '')
    const id = ids[index]
    const qty = id ? Number(quantities[id] || 0) : 0
    return qty > 0 ? `${label} × ${qty}` : label
  }).filter(Boolean).join(' / ')
}
