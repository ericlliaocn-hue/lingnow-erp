import request from '@/utils/request'
import type {
  AddressParseResult,
  AddressRegionOption,
  CustomerOrder,
  OrderSubmitPayload,
  PageResult,
  ShopAddress,
  ShopAddressPayload,
  ShopAttribute,
  ShopCategory,
  ShopCustomer,
  ShopLoginVO,
  ShopProduct
} from '@/types/shop'

export function login(data: { username: string; password: string }) {
  return request<ShopLoginVO>({ url: '/auth/login', method: 'post', data })
}

export function logout() {
  return request<void>({ url: '/auth/logout', method: 'post' })
}

export function me() {
  return request<ShopLoginVO>({ url: '/sales-h5/me', method: 'get' })
}

export function listCustomers(keyword?: string) {
  return request<ShopCustomer[]>({ url: '/sales-h5/customers', method: 'get', params: { keyword } })
}

export function listCategories() {
  return request<ShopCategory[]>({ url: '/sales-h5/categories', method: 'get' })
}

export function listProducts(params: { current: number; size: number; keyword?: string; categoryId?: string }) {
  return request<PageResult<ShopProduct>>({ url: '/sales-h5/products', method: 'get', params })
}

export function getProduct(id: string) {
  return request<ShopProduct>({ url: `/sales-h5/products/${id}`, method: 'get' })
}

export function listAttributes() {
  return request<ShopAttribute[]>({ url: '/sales-h5/attributes', method: 'get' })
}

export function listAddressRegions(parentCode?: string) {
  return request<AddressRegionOption[]>({
    url: '/sales-h5/address/regions',
    method: 'get',
    params: { parentCode }
  })
}

export function searchAddressRegions(keyword: string, limit = 20) {
  return request<AddressRegionOption[]>({
    url: '/sales-h5/address/search',
    method: 'get',
    params: { keyword, limit }
  })
}

export function parseAddress(rawText: string) {
  return request<AddressParseResult>({
    url: '/sales-h5/address/parse',
    method: 'post',
    data: { rawText }
  })
}

export function listAddresses(keyword?: string) {
  const customerId = selectedCustomerId()
  return request<ShopAddress[]>({
    url: `/sales-h5/customers/${customerId}/addresses`,
    method: 'get',
    params: { keyword }
  })
}

export function getAddress(id: string) {
  return request<ShopAddress>({ url: `/sales-h5/customers/${selectedCustomerId()}/addresses/${id}`, method: 'get' })
}

export function createAddress(data: ShopAddressPayload) {
  return request<ShopAddress>({ url: `/sales-h5/customers/${selectedCustomerId()}/addresses`, method: 'post', data })
}

export function updateAddress(id: string, data: ShopAddressPayload) {
  return request<ShopAddress>({ url: `/sales-h5/customers/${selectedCustomerId()}/addresses/${id}`, method: 'put', data })
}

export function setDefaultAddress(id: string) {
  return request<ShopAddress>({ url: `/sales-h5/customers/${selectedCustomerId()}/addresses/${id}/default`, method: 'post' })
}

export function uploadImage(file: File) {
  const data = new FormData()
  data.append('file', file)
  return request<string>({
    url: '/sales-h5/file/upload',
    method: 'post',
    data,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function submitOrder(data: OrderSubmitPayload) {
  return request<string>({ url: '/sales-h5/orders', method: 'post', data })
}

export function listOrders(params: { current: number; size: number }) {
  return request<PageResult<CustomerOrder>>({ url: '/sales-h5/orders', method: 'get', params })
}

export function getOrder(id: string) {
  return request<CustomerOrder>({ url: `/sales-h5/orders/${id}`, method: 'get' })
}

function selectedCustomerId() {
  const customerId = localStorage.getItem('sales-customer-id') || ''
  if (!customerId) throw new Error('请先选择客户')
  return customerId
}
