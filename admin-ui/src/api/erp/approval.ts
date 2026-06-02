import request from '@/utils/request'
import type { PageResult } from '@/api/types'

export type ApprovalBizType =
  | 'SALE'
  | 'SALE_RETURN'
  | 'PURCHASE'
  | 'PURCHASE_RETURN'
  | 'STOCK_CHECK'
  | 'RECEIPT'
  | 'PAYMENT'
  | 'INCOME'
  | 'EXPENSE'

export type ApprovalStatus = 'NONE' | 'PENDING' | 'APPROVED' | 'REJECTED' | 'REVOKED'

export interface ApprovalTask {
  taskId?: string
  instanceId?: string
  bizType: ApprovalBizType
  bizId: string
  bizName: string
  billNo: string
  approvalStatus: ApprovalStatus
  nodeCode?: string
  nodeName?: string
  flowStatus?: string
  amount?: string
  submitBy?: string
  submitTime?: string
  createTime?: string
  actionUrl?: string
}

export interface ApprovalHistory {
  id: string
  taskId: string
  instanceId: string
  nodeName?: string
  targetNodeName?: string
  approver?: string
  skipType?: string
  flowStatus?: string
  message?: string
  createTime?: string
  updateTime?: string
}

export interface ApprovalQuery {
  current: number
  size: number
  bizType?: string
  billNo?: string
  approvalStatus?: string
}

export function listApprovalTodo(params: ApprovalQuery) {
  return request<PageResult<ApprovalTask>>({ url: '/erp/approval/todo/list', method: 'get', params })
}

export function listApprovalDone(params: ApprovalQuery) {
  return request<PageResult<ApprovalTask>>({ url: '/erp/approval/done/list', method: 'get', params })
}

export function listApprovalMine(params: ApprovalQuery) {
  return request<PageResult<ApprovalTask>>({ url: '/erp/approval/mine/list', method: 'get', params })
}

export function submitApproval(bizType: ApprovalBizType, bizId: string) {
  return request({ url: '/erp/approval/submit', method: 'post', data: { bizType, bizId } })
}

export function passApproval(taskId: string, comment?: string) {
  return request({ url: '/erp/approval/pass', method: 'post', data: { taskId, comment } })
}

export function rejectApproval(taskId: string, comment?: string) {
  return request({ url: '/erp/approval/reject', method: 'post', data: { taskId, comment } })
}

export function revokeApproval(bizType: ApprovalBizType, bizId: string) {
  return request({ url: '/erp/approval/revoke', method: 'post', data: { bizType, bizId } })
}

export function transferApproval(taskId: string, transferUserId: string, comment?: string) {
  return request({ url: '/erp/approval/transfer', method: 'post', data: { taskId, transferUserId, comment } })
}

export function approvalHistory(bizType: ApprovalBizType, bizId: string) {
  return request<ApprovalHistory[]>({ url: '/erp/approval/history', method: 'get', params: { bizType, bizId } })
}

export const approvalStatusText: Record<string, string> = {
  NONE: '未提交',
  PENDING: '审批中',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  REVOKED: '已撤回'
}

export const approvalStatusTag: Record<string, 'info' | 'warning' | 'success' | 'danger'> = {
  NONE: 'info',
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
  REVOKED: 'info'
}

export const approvalBizTypeOptions = [
  { label: '销售单', value: 'SALE' },
  { label: '销售退货单', value: 'SALE_RETURN' },
  { label: '进货单', value: 'PURCHASE' },
  { label: '进货退货单', value: 'PURCHASE_RETURN' },
  { label: '库存盘点', value: 'STOCK_CHECK' },
  { label: '收款单', value: 'RECEIPT' },
  { label: '付款单', value: 'PAYMENT' },
  { label: '其他收入', value: 'INCOME' },
  { label: '其他支出', value: 'EXPENSE' }
]
