import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import router from '@/router'
import JSONBig from 'json-bigint'

// 是否正在显示重新登录弹窗
let isReloginShow = false

const service = axios.create({
    baseURL: '/admin-api', // 匹配后端 server.servlet.context-path: /admin-api
    timeout: 5000,
    transformResponse: [
        function (data) {
            try {
                // 使用 json-bigint 处理大整数，并将 boolean status 转换为 0/1
                return JSONBig({ storeAsString: true }).parse(data, (key, value) => {
                    if (key === 'status' && typeof value === 'boolean') {
                        return value ? 1 : 0
                    }
                    return value
                })
            } catch (err) {
                // 如果解析失败，返回原数据
                return data
            }
        }
    ]
})

// 请求拦截器
service.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token-admin')
        if (token) {
            // Check if headers object exists, if not create it
            if (!config.headers) {
                config.headers = new axios.AxiosHeaders()
            }
            // Use set method if available (Axios 1.x), otherwise direct assignment
            if (typeof config.headers.set === 'function') {
                config.headers.set('token-admin', token)
                config.headers.set('satoken-admin', token) // 兼容 AUTHENTICATION.md 描述
                config.headers.set('satoken', token)       // 兼容默认 Sa-Token
                config.headers.set('Authorization', token) // 兼容标准头
            } else {
                config.headers['token-admin'] = token
                config.headers['satoken-admin'] = token
                config.headers['satoken'] = token
                config.headers['Authorization'] = token
            }
        }
        return config
    },
    (error) => {
        console.error('请求拦截器错误:', error)
        return Promise.reject(error)
    }
)

// 处理登录过期/未登录的统一方法
const handleUnauthorized = () => {
    // 如果已经在登录页，不显示弹窗
    if (router.currentRoute.value.path.startsWith('/login')) {
        return
    }

    if (isReloginShow) {
        return
    }
    isReloginShow = true
    
    ElMessageBox.confirm('登录状态已过期，您可以继续留在该页面，或者重新登录', '系统提示', {
        confirmButtonText: '重新登录',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        isReloginShow = false
        // 清除本地存储
        localStorage.removeItem('token-admin')
        localStorage.removeItem('userInfo')
        localStorage.removeItem('permissions')
        // 使用 window.location.href 强制刷新跳转，确保清除 Pinia 状态并避免路由守卫死循环
        window.location.href = `/login?redirect=${encodeURIComponent(location.pathname + location.search)}`
    }).catch(() => {
        isReloginShow = false
    })
}

// 响应拦截器
service.interceptors.response.use(
    (response) => {
        const res = response.data
        // 二进制数据则直接返回
        if (response.request.responseType === 'blob' || response.config.responseType === 'blob') {
            return res
        }

        if (res.code !== 200) {
            // 处理 401 未登录/登录过期
            if (res.code === 401) {
                // 如果是登录接口报 401，直接提示错误，不弹出重新登录框
                if (response.config.url?.includes('/auth/login')) {
                    ElMessage.error(res.message || res.msg || '登录失败')
                    return Promise.reject(new Error(res.message || res.msg || '登录失败'))
                }

                handleUnauthorized()
                return Promise.reject(new Error(res.message || res.msg || '未登录'))
            }
            
            // 其他错误提示
            const errorMessage = res.message || res.msg || 'Error'
            ElMessage.error(errorMessage)
            return Promise.reject(new Error(errorMessage))
        }
        return res.data
    },
    (error) => {
        console.error('API 错误:', error)
        
        // 处理 HTTP 401 状态码
        if (error.response?.status === 401) {
            // 如果是登录接口报 401，直接提示错误，不弹出重新登录框
            if (error.config?.url?.includes('/auth/login')) {
                const msg = error.response?.data?.msg || '登录失败'
                ElMessage.error(msg)
                return Promise.reject(error)
            }
            handleUnauthorized()
            return Promise.reject(error)
        }
        
        // 其他错误提示
        const message = error.response?.data?.msg || error.message || '请求失败'
        ElMessage.error(message)
        return Promise.reject(error)
    }
)

const request = <T = any>(config: AxiosRequestConfig): Promise<T> => {
    return service(config) as any
}

export default request
