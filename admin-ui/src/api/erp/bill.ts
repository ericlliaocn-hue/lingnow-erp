import request from '@/utils/request'
import type { PageResult } from '@/api/types'
import type { ApprovalStatus } from './approval'

export type BillModule = 'sale' | 'sale-return' | 'purchase' | 'purchase-return' | 'production'

export interface BillItem {
  id?: string
  productId: string
  productCode?: string
  productName?: string
  productImageUrl?: string
  logoImageUrl?: string
  spec?: string
  attributeText?: string
  categoryLevel1Id?: string
  categoryLevel1Name?: string
  categoryLevel2Id?: string
  categoryLevel2Name?: string
  optionAttributeIds?: string
  optionAttributeText?: string
  attributeSelections?: Record<string, string>
  availableAttributeIds?: string
  availableAttributeText?: string
  productOptionsRequestId?: number
  unitId?: string
  unitName?: string
  warehouseId?: string
  qty: number
  purchasePrice?: number
  basePrice?: number
  price: number
  amount?: number
  discountRate?: number
  discountAmount?: number
  finalAmount?: number
  remark?: string
  optionProducts?: any[]
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
  employeeId?: string
  employeeName?: string
  receiverName?: string
  receiverPhone?: string
  receiverAddress?: string
  totalQty?: number
  totalAmount?: number
  discountAmount?: number
  otherAmount?: number
  payableAmount?: number
  paidAmount?: number
  paymentMethod?: string
  debtAmount?: number
  auditStatus?: number
  approvalStatus?: ApprovalStatus
  approvalInstanceId?: string
  approvalSubmitBy?: string
  approvalSubmitTime?: string
  approvalFinishTime?: string
  paymentStatus?: string
  productionProgress?: string
  trackingNo?: string
  productionUserId?: string
  productionUserName?: string
  remark?: string
  items: BillItem[]
}

export interface BillQuery {
  current: number
  size: number
  billNo?: string
  employeeId?: string
  partnerId?: string
  auditStatus?: number
  paymentStatus?: string
  beginDate?: string
  endDate?: string
}

export interface ProductionUpdatePayload {
  productionProgress?: string
  trackingNo?: string
  productionUserName?: string
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

export function updateProductionBill(id: string, data: ProductionUpdatePayload) {
  return request({ url: `/erp/production/${id}`, method: 'put', data })
}

export function deleteBill(module: BillModule, ids: string | string[]) {
  return request({ url: `/erp/${module}/${Array.isArray(ids) ? ids.join(',') : ids}`, method: 'delete' })
}

export function copyBill(module: BillModule, id: string) {
  return request<string>({ url: `/erp/${module}/copy/${id}`, method: 'post' })
}

export function exportBill(module: BillModule, params: BillQuery) {
  return request<Blob>({ url: `/erp/${module}/export`, method: 'get', params, responseType: 'blob' })
}

export function printBill(module: BillModule, id: string) {
  return request<Record<string, any>>({ url: `/erp/${module}/print/${id}`, method: 'get' })
}

export function auditBill(module: BillModule, id: string) {
  return request({ url: `/erp/${module}/audit/${id}`, method: 'put' })
}

export function unauditBill(module: BillModule, id: string) {
  return request({ url: `/erp/${module}/unaudit/${id}`, method: 'put' })
}
