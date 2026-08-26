import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi, me } from '@/api/shop'
import type { ShopLoginVO } from '@/types/shop'

const TOKEN_KEY = 'token-sales-h5'
const USER_KEY = 'sales-h5-user'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: readUser(),
    userLoaded: Boolean(localStorage.getItem(USER_KEY))
  }),
  actions: {
    async login(username: string, password: string) {
      const data = await loginApi({ username, password })
      this.setSession(data)
      return data
    },
    async fetchMe() {
      const data = await me()
      this.setSession(data)
      return data
    },
    async logout() {
      await logoutApi().catch(() => undefined)
      this.clear()
    },
    setSession(data: ShopLoginVO) {
      this.token = data.token
      this.user = data
      this.userLoaded = true
      localStorage.setItem(TOKEN_KEY, data.token)
      localStorage.setItem(USER_KEY, JSON.stringify(data))
    },
    clear() {
      this.token = ''
      this.user = null
      this.userLoaded = false
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      localStorage.removeItem('sales-customer-id')
      localStorage.removeItem('sales-customer-name')
    }
  }
})

function readUser(): ShopLoginVO | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as ShopLoginVO
  } catch (err) {
    localStorage.removeItem(USER_KEY)
    return null
  }
}
