export interface PageResult<T> {
  current: number
  size: number
  total: number
  records: T[]
}

export interface ShopLoginVO {
  token: string
  accountId: string
  customerId: string
  username: string
  nickname?: string
  customerName?: string
}

export interface ShopRegisterPayload {
  name: string
  phone: string
  password: string
  confirmPassword: string
}

export interface ShopProduct {
  id: string
  code?: string
  name: string
  spec?: string
  imageUrl?: string
  attributeIds?: string
  attributeText?: string
  salePrice?: number
  unitName?: string
}

export interface ShopAttribute {
  id: string
  name: string
  parentId?: string
  extraAmount?: number
  sortOrder?: number
  status?: number
}

export type CustomerOrderStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED'

export interface CustomerOrderItem {
  id?: string
  productId: string
  productCode?: string
  productName?: string
  productImageUrl?: string
  logoImageUrl?: string
  spec?: string
  attributeText?: string
  optionAttributeIds?: string
  optionAttributeText?: string
  qty: number
  price: number
  amount: number
  remark?: string
}

export interface CustomerOrder {
  id: string
  orderNo: string
  customerName?: string
  accountName?: string
  status: CustomerOrderStatus
  orderTime?: string
  totalQty?: number
  totalAmount?: number
  receiverName?: string
  receiverPhone?: string
  receiverAddress?: string
  remark?: string
  billNo?: string
  confirmTime?: string
  cancelTime?: string
  cancelReason?: string
  items?: CustomerOrderItem[]
}

export interface OrderSubmitItem {
  productId: string
  optionAttributeIds?: string
  logoImageUrl?: string
  qty: number
  remark?: string
}

export interface OrderSubmitPayload {
  receiverName?: string
  receiverPhone?: string
  receiverAddress?: string
  remark?: string
  items: OrderSubmitItem[]
}
