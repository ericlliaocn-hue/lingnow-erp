import request from '@/utils/request'
import type { PageResult } from '@/api/types'

export interface PostVO {
  postId: string
  postCode: string
  postName: string
  postSort: number
  status: number
  createTime: string
  remark?: string
}

export interface PostQuery {
  current: number
  size: number
  postCode?: string
  postName?: string
  status?: number
  deptId?: string
}

export interface PostForm {
  postId?: string
  postCode: string
  postName: string
  postSort: number
  status: number
  remark?: string
}

// 查询岗位列表
export function listPost(params: PostQuery) {
  return request<PageResult<PostVO>>({
    url: '/system/post/list',
    method: 'get',
    params
  })
}

// 查询岗位详细
export function getPost(postId: string) {
  return request<PostVO>({
    url: `/system/post/${postId}`,
    method: 'get'
  })
}

// 新增岗位
export function addPost(data: PostForm) {
  return request({
    url: '/system/post',
    method: 'post',
    data
  })
}

// 修改岗位
export function updatePost(data: PostForm) {
  return request({
    url: '/system/post',
    method: 'put',
    data
  })
}

// 删除岗位
export function delPost(postId: string) {
  return request({
    url: `/system/post/${postId}`,
    method: 'delete'
  })
}
