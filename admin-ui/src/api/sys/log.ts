import request from '@/utils/request'

export interface OperLogVO {
  operId: string
  title: string
  businessType: number
  method: string
  requestMethod: string
  operatorType: number
  operName: string
  deptName: string
  operUrl: string
  operIp: string
  operLocation: string
  operParam: string
  jsonResult: string
  status: number
  errorMsg: string
  operTime: string
  costTime: number
}

export interface OperLogQueryBO {
  current: number
  size: number
  title?: string
  operName?: string
  businessType?: number | string
  status?: number | string
  startTime?: string
  endTime?: string
}

export interface LoginLogVO {
  infoId: string
  userName: string
  ipaddr: string
  loginLocation: string
  browser: string
  os: string
  status: number
  msg: string
  loginTime: string
}

export interface LoginLogQueryBO {
  current: number
  size: number
  userName?: string
  ipaddr?: string
  status?: number | string
  startTime?: string
  endTime?: string
}

export interface ErrorLogVO {
  id: string
  traceId: string
  userId: string
  userName: string
  requestMethod: string
  requestUrl: string
  requestParams: string
  ip: string
  errorMsg: string
  errorStack: string
  createTime: string
}

export interface ErrorLogQueryBO {
  current: number
  size: number
  traceId?: string
  userName?: string
  requestUrl?: string
}

export interface SlowSqlLogVO {
  id: string
  traceId: string
  userId: string
  userName: string
  executionTime: number
  sqlStatement: string
  createTime: string
}

export interface SlowSqlLogQueryBO {
  current: number
  size: number
  traceId?: string
  userName?: string
  minExecutionTime?: number
}

export function getOperLogList(params: OperLogQueryBO) {
  return request<any>({
    url: '/sys/log/oper/list',
    method: 'get',
    params
  })
}

export function getLoginLogList(params: LoginLogQueryBO) {
  return request<any>({
    url: '/sys/log/login/list',
    method: 'get',
    params
  })
}

export function getErrorLogList(params: ErrorLogQueryBO) {
  return request<any>({
    url: '/sys/log/error/list',
    method: 'get',
    params
  })
}

export function getSlowSqlLogList(params: SlowSqlLogQueryBO) {
  return request<any>({
    url: '/sys/log/slowSql/list',
    method: 'get',
    params
  })
}
