import axios, { type AxiosRequestConfig } from 'axios'
import JSONBig from 'json-bigint'
import router from '@/router'
import { useAuthStore } from '@/stores/auth'

const service = axios.create({
  baseURL: '/admin-api',
  timeout: 12000,
  transformResponse: [
    function (data) {
      try {
        return JSONBig({ storeAsString: true }).parse(data, (key, value) => {
          if (key === 'status' && typeof value === 'boolean') {
            return value ? 1 : 0
          }
          return value
        })
      } catch (err) {
        return data
      }
    }
  ]
})

service.interceptors.request.use((config) => {
  const token = localStorage.getItem('token-sales-h5')
  if (token) {
    if (!config.headers) {
      config.headers = new axios.AxiosHeaders()
    }
    if (typeof config.headers.set === 'function') {
      config.headers.set('token-admin', token)
      config.headers.set('satoken', token)
      config.headers.set('Authorization', token)
    } else {
      config.headers['token-admin'] = token
      config.headers['satoken'] = token
      config.headers['Authorization'] = token
    }
  }
  return config
})

service.interceptors.response.use(
  (response) => {
    const res = response.data
    if (response.request.responseType === 'blob' || response.config.responseType === 'blob') {
      return res
    }
    if (res.code !== 200) {
      if (res.code === 401) {
        useAuthStore().clear()
        router.replace('/login')
      }
      return Promise.reject(new Error(res.message || res.msg || '请求失败'))
    }
    return res.data
  },
  (error) => {
    if (error.response?.status === 401) {
      useAuthStore().clear()
      router.replace('/login')
    }
    return Promise.reject(error)
  }
)

const request = <T = unknown>(config: AxiosRequestConfig): Promise<T> => service(config) as Promise<T>

export default request
