import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, logout as logoutApi, type UserDetail } from '@/api/sys/user'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token-admin') || '')
  const userInfo = ref<UserDetail | null>(JSON.parse(localStorage.getItem('userInfo') || 'null'))

  const login = async (loginForm: any) => {
    const res = await loginApi(loginForm)
    // request.ts 拦截器已经返回了 res.data，所以这里 res 就是实际的数据对象
    if (res && res.token) {
        token.value = res.token
        localStorage.setItem('token-admin', res.token)
        userInfo.value = res
        localStorage.setItem('userInfo', JSON.stringify(res))
        localStorage.setItem('permissions', JSON.stringify(res.permissions || []))
    }
    return res
  }

  const logout = async () => {
    try {
      await logoutApi()
    } catch (e) {
      console.error(e)
    }
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token-admin')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('permissions')
  }

  return {
    token,
    userInfo,
    login,
    logout
  }
})
