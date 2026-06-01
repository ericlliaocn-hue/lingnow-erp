import request from '@/utils/request'
import type { PageResult } from '@/api/types'

export interface ErpProduct {
  id?: string
  code: string
  name: string
  spec?: string
  categoryId?: string
  categoryName?: string
  brandId?: string
  brandName?: string
  unitId?: string
  unitName?: string
  attributeText?: string
  barcode?: string
  location?: string
  purchasePrice?: number
  salePrice?: number
  retailPrice?: number
  minStock?: number
  maxStock?: number
  imageUrl?: string
  sortOrder?: number
  status?: number
  remark?: string
}

export interface ErpProductQuery {
  current: number
  size: number
  code?: string
  name?: string
  barcode?: string
  categoryId?: string
  brandId?: string
  status?: number
}

export function listProduct(params: ErpProductQuery) {
  return request<PageResult<ErpProduct>>({ url: '/erp/product/list', method: 'get', params })
}

export function productOptions(params: Partial<ErpProductQuery> = {}) {
  return request<ErpProduct[]>({ url: '/erp/product/options', method: 'get', params })
}

export function getProduct(id: string) {
  return request<ErpProduct>({ url: `/erp/product/${id}`, method: 'get' })
}

export function addProduct(data: ErpProduct) {
  return request({ url: '/erp/product', method: 'post', data })
}

export function updateProduct(data: ErpProduct) {
  return request({ url: '/erp/product', method: 'put', data })
}

export function deleteProduct(ids: string | string[]) {
  return request({ url: `/erp/product/${Array.isArray(ids) ? ids.join(',') : ids}`, method: 'delete' })
}
