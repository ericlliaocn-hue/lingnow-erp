import request from '@/utils/request'
import type {
  CustomerOrder,
  OrderSubmitPayload,
  PageResult,
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
