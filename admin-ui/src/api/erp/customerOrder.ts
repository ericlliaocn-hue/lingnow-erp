import request from '@/utils/request'
import type { PageResult } from '@/api/types'

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
  customerId: string
  customerName?: string
  accountId: string
  accountName?: string
  status: CustomerOrderStatus
  orderTime?: string
  totalQty?: number
  totalAmount?: number
  receiverName?: string
  receiverPhone?: string
  receiverAddress?: string
  remark?: string
  billId?: string
  billNo?: string
  confirmTime?: string
  confirmBy?: string
  cancelTime?: string
  cancelBy?: string
  cancelReason?: string
  createTime?: string
  items?: CustomerOrderItem[]
}

export interface CustomerOrderQuery {
  current: number
  size: number
  orderNo?: string
  customerName?: string
  status?: CustomerOrderStatus | ''
}

export interface CustomerOrderConfirmPayload {
  warehouseId: string
  employeeId?: string
  employeeName?: string
  accountId?: string
  paidAmount?: number
  paymentMethod?: string
  remark?: string
}

export function listCustomerOrder(params: CustomerOrderQuery) {
  return request<PageResult<CustomerOrder>>({ url: '/erp/customer-order/list', method: 'get', params })
}

export function getCustomerOrder(id: string) {
  return request<CustomerOrder>({ url: `/erp/customer-order/${id}`, method: 'get' })
}

export function confirmCustomerOrder(id: string, data: CustomerOrderConfirmPayload) {
  return request<string>({ url: `/erp/customer-order/${id}/confirm`, method: 'post', data })
}

export function cancelCustomerOrder(id: string, reason?: string) {
  return request({ url: `/erp/customer-order/${id}/cancel`, method: 'put', data: { reason } })
}

export function printCustomerOrder(id: string) {
  return request<CustomerOrder>({ url: `/erp/customer-order/print/${id}`, method: 'get' })
}
