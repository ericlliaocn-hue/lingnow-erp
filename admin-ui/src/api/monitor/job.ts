import request from '@/utils/request'

export interface JobVO {
  jobId?: string
  jobName: string
  jobGroup: string
  invokeTarget: string
  cronExpression: string
  misfirePolicy: string
  concurrent: string
  status: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface JobQueryBO {
  current: number
  size: number
  jobName?: string
  jobGroup?: string
  status?: number | string
}

export interface JobLogVO {
  jobLogId?: string
  jobId?: string
  jobName: string
  jobGroup: string
  invokeTarget: string
  jobMessage?: string
  status: number
  exceptionInfo?: string
  startTime?: string
  endTime?: string
  durationMs?: number
}

export interface JobLogQueryBO {
  current: number
  size: number
  jobId?: string
  jobName?: string
  jobGroup?: string
  status?: number | string
}

export function listJob(params: JobQueryBO) {
  return request<any>({
    url: '/monitor/job/list',
    method: 'get',
    params
  })
}

export function getJob(jobId: string) {
  return request<JobVO>({
    url: `/monitor/job/${jobId}`,
    method: 'get'
  })
}

export function addJob(data: JobVO) {
  return request({
    url: '/monitor/job',
    method: 'post',
    data
  })
}

export function updateJob(data: JobVO) {
  return request({
    url: '/monitor/job',
    method: 'put',
    data
  })
}

export function deleteJob(jobIds: string | string[]) {
  return request({
    url: `/monitor/job/${Array.isArray(jobIds) ? jobIds.join(',') : jobIds}`,
    method: 'delete'
  })
}

export function changeJobStatus(jobId: string, status: number) {
  return request({
    url: '/monitor/job/changeStatus',
    method: 'put',
    data: { jobId, status }
  })
}

export function runJob(jobId: string) {
  return request({
    url: `/monitor/job/run/${jobId}`,
    method: 'post'
  })
}

export function listJobLog(params: JobLogQueryBO) {
  return request<any>({
    url: '/monitor/job/log/list',
    method: 'get',
    params
  })
}

export function deleteJobLog(jobLogIds: string | string[]) {
  return request({
    url: `/monitor/job/log/${Array.isArray(jobLogIds) ? jobLogIds.join(',') : jobLogIds}`,
    method: 'delete'
  })
}

export function cleanJobLog() {
  return request({
    url: '/monitor/job/log/clean',
    method: 'delete'
  })
}
