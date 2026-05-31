export interface PageResult<T> {
  current: number
  size: number
  total: number
  pages: number
  records: T[]
}

export interface Result<T = any> {
  code: number
  msg: string
  data: T
}
