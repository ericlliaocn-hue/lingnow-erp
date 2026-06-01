import request from '@/utils/request'
import type { PageResult } from '@/api/types'

export type BillModule = 'sale' | 'purchase'

export interface BillItem {
  id?: string
  productId: string
  productCode?: string
  productName?: string
  spec?: string
  unitId?: string
  unitName?: string
  warehouseId?: string
  qty: number
  price: number
  amount?: number
  discountRate?: number
  discountAmount?: number
  finalAmount?: number
  remark?: string
}

export interface ErpBill {
  id?: string
  billNo?: string
  billType?: string
  billDate: string
  partnerId: string
  partnerName?: string
  warehouseId: string
  warehouseName?: string
  accountId?: string
  accountName?: string
  totalQty?: number
  totalAmount?: number
  discountAmount?: number
  otherAmount?: number
  payableAmount?: number
  paidAmount?: number
  debtAmount?: number
  auditStatus?: number
  paymentStatus?: string
  remark?: string
  items: BillItem[]
}

export interface BillQuery {
  current: number
  size: number
  billNo?: string
  partnerId?: string
  auditStatus?: number
  paymentStatus?: string
}

export function listBill(module: BillModule, params: BillQuery) {
  return request<PageResult<ErpBill>>({ url: `/erp/${module}/list`, method: 'get', params })
}

export function nextBillNo(module: BillModule) {
  return request<string>({ url: `/erp/${module}/nextNo`, method: 'get' })
}

export function getBill(module: BillModule, id: string) {
  return request<ErpBill>({ url: `/erp/${module}/${id}`, method: 'get' })
}

export function addBill(module: BillModule, data: ErpBill) {
  return request({ url: `/erp/${module}`, method: 'post', data })
}

export function updateBill(module: BillModule, data: ErpBill) {
  return request({ url: `/erp/${module}`, method: 'put', data })
}

export function deleteBill(module: BillModule, ids: string | string[]) {
  return request({ url: `/erp/${module}/${Array.isArray(ids) ? ids.join(',') : ids}`, method: 'delete' })
}

export function auditBill(module: BillModule, id: string) {
  return request({ url: `/erp/${module}/audit/${id}`, method: 'put' })
}

export function unauditBill(module: BillModule, id: string) {
  return request({ url: `/erp/${module}/unaudit/${id}`, method: 'put' })
}
