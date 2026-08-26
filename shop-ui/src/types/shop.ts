export interface PageResult<T> {
  current: number
  size: number
  total: number
  records: T[]
}

export interface ShopLoginVO {
  token: string
  username: string
  nickname?: string
  permissions?: string[]
}

export interface ShopCustomer {
  id: string
  code?: string
  name: string
  contact?: string
  phone?: string
  address?: string
}

export interface ShopCategory {
  id: string
  code?: string
  name: string
  parentId?: string
  attributeIds?: string
  sortOrder?: number
  status?: number
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
  categoryId?: string
  categoryName?: string
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

export interface AddressRegionOption {
  code: string
  name: string
  level?: number
  leaf?: boolean
  path?: string[]
  pathNames?: string[]
}

export interface AddressParseResult {
  contactName?: string
  phone?: string
  province?: string
  city?: string
  district?: string
  street?: string
  village?: string
  regionPath?: string[]
  regionPathNames?: string[]
  contactCandidates?: string[]
  detailAddress?: string
  normalizedAddress?: string
  confidence?: number
  warnings?: string[]
}

export interface ShopAddress {
  id: string
  receiverName: string
  receiverPhone: string
  provinceCode?: string
  provinceName?: string
  cityCode?: string
  cityName?: string
  districtCode?: string
  districtName?: string
  streetCode?: string
  streetName?: string
  villageCode?: string
  villageName?: string
  regionPath?: string[]
  regionPathNames?: string[]
  detailAddress: string
  fullAddress?: string
  addressLabel?: string
  defaultFlag?: boolean
  createTime?: string
  updateTime?: string
}

export interface ShopAddressPayload {
  receiverName: string
  receiverPhone: string
  regionPath?: string[]
  regionPathNames?: string[]
  detailAddress: string
  addressLabel?: string
  defaultFlag?: boolean
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
  customerId: string
  addressId?: string
  receiverName?: string
  receiverPhone?: string
  receiverAddress?: string
  remark?: string
  items: OrderSubmitItem[]
}
