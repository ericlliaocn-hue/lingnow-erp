import type { CustomerOrderStatus, ShopProduct } from '@/types/shop'

/**
 * 商城话术清洗工具：把后端返回的 ERP 内部标签转成面向消费者的友好文案。
 * 收口原本散落在 ProductListView / OrderCreateView / OrderDetailView 的重复实现。
 */

/** ERP 属性/标签文案映射，把"商品款式/商品定制/商品衣钩"等内部叫法转成商城话术 */
const LABEL_REWRITES: Array<{ test: RegExp; label: string }> = [
  { test: /定制/, label: '搭配' },
  { test: /款式/, label: '类型' },
  { test: /衣钩/, label: '挂钩' }
]

/**
 * 清洗单个属性/标签文本：
 * - 去掉 ERP 前缀"商品"（如"商品款式"→"款式"）
 * - 应用 LABEL_REWRITES 改写
 * - 去空白
 */
export function displayShopLabel(value?: string): string {
  const text = String(value || '').trim().replace(/^商品/, '')
  if (!text) return ''
  for (const rule of LABEL_REWRITES) {
    if (rule.test.test(text)) return rule.label
  }
  return text
}

/** 价格展示：0 或缺失显示"询价"，否则保留两位小数 */
export function priceLabel(value?: number): string {
  const price = Number(value || 0)
  return price > 0 ? `￥${price.toFixed(2)}` : '询价'
}

/** 订单状态文案 */
export function statusText(status: CustomerOrderStatus): string {
  const map: Record<CustomerOrderStatus, string> = {
    PENDING: '待确认',
    CONFIRMED: '已确认',
    CANCELLED: '已取消'
  }
  return map[status] || status
}

/** 订单状态对应的 pill 样式类 */
export function statusClass(status: CustomerOrderStatus): string {
  const map: Record<CustomerOrderStatus, string> = {
    PENDING: 'pill warning',
    CONFIRMED: 'pill success',
    CANCELLED: 'pill info'
  }
  return map[status] || 'pill'
}

/** 商品副标题：规格优先，否则按单位兜底 */
export function productSubtitle(item: ShopProduct): string {
  if (item.spec) return item.spec
  return item.unitName ? `${item.unitName}计价` : '多种尺寸可选'
}

/** 从商品的 attributeText/unitName/spec 派生最多 3 个展示标签（清洗后） */
export function productTags(item: ShopProduct): string[] {
  const values = [item.attributeText, item.unitName, item.spec ? '多规格' : '', item.imageUrl ? '实物图' : '']
    .filter(Boolean)
    .flatMap(value => String(value).split(/[\/,，]/))
    .map(value => displayShopLabel(value))
    .filter(Boolean)
  const tags: string[] = []
  values.forEach((value) => {
    if (!tags.includes(value)) tags.push(value)
  })
  return tags.length ? tags.slice(0, 3) : ['可咨询']
}

/** 商城用的展示名：优先昵称，缺失时给化名，不直出 ERP 的 customerName 字段名 */
export function displayName(user?: { nickname?: string; customerName?: string } | null): string {
  if (!user) return '荣时会员'
  return user.nickname || user.customerName || '荣时会员'
}

/** 头像首字：取昵称/客户名首字，缺失取品牌字"荣" */
export function avatarText(user?: { nickname?: string; customerName?: string } | null): string {
  if (!user) return '荣'
  return (user.nickname || user.customerName || '荣').slice(0, 1)
}
