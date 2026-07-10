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
  ShopLoginVO,
  ShopProduct,
  ShopRegisterPayload
} from '@/types/shop'

export function login(data: { username: string; password: string }) {
  return request<ShopLoginVO>({ url: '/auth/login', method: 'post', data })
}

export function register(data: ShopRegisterPayload) {
  return request<ShopLoginVO>({ url: '/auth/register', method: 'post', data })
}

export function logout() {
  return request<void>({ url: '/auth/logout', method: 'post' })
}

export function me() {
  return request<ShopLoginVO>({ url: '/auth/me', method: 'get' })
}

export function listProducts(params: { current: number; size: number; keyword?: string }) {
  return request<PageResult<ShopProduct>>({ url: '/products', method: 'get', params })
}

export function getProduct(id: string) {
  return request<ShopProduct>({ url: `/products/${id}`, method: 'get' })
}

export function listAttributes() {
  return request<ShopAttribute[]>({ url: '/attributes', method: 'get' })
}

export function listAddressRegions(parentCode?: string) {
  return request<AddressRegionOption[]>({
    url: '/address/regions',
    method: 'get',
    params: { parentCode }
  })
}

export function searchAddressRegions(keyword: string, limit = 20) {
  return request<AddressRegionOption[]>({
    url: '/address/search',
    method: 'get',
    params: { keyword, limit }
  })
}

export function parseAddress(rawText: string) {
  return request<AddressParseResult>({
    url: '/address/parse',
    method: 'post',
    data: { rawText }
  })
}

export function listAddresses(keyword?: string) {
  return request<ShopAddress[]>({
    url: '/addresses',
    method: 'get',
    params: { keyword }
  })
}

export function getAddress(id: string) {
  return request<ShopAddress>({ url: `/addresses/${id}`, method: 'get' })
}

export function createAddress(data: ShopAddressPayload) {
  return request<ShopAddress>({ url: '/addresses', method: 'post', data })
}

export function updateAddress(id: string, data: ShopAddressPayload) {
  return request<ShopAddress>({ url: `/addresses/${id}`, method: 'put', data })
}

export function setDefaultAddress(id: string) {
  return request<ShopAddress>({ url: `/addresses/${id}/default`, method: 'post' })
}

export function uploadImage(file: File) {
  const data = new FormData()
  data.append('file', file)
  return request<string>({
    url: '/file/upload',
    method: 'post',
    data,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function submitOrder(data: OrderSubmitPayload) {
  return request<string>({ url: '/orders', method: 'post', data })
}

export function listOrders(params: { current: number; size: number }) {
  return request<PageResult<CustomerOrder>>({ url: '/orders', method: 'get', params })
}

export function getOrder(id: string) {
  return request<CustomerOrder>({ url: `/orders/${id}`, method: 'get' })
}
