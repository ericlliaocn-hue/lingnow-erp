import { defineStore } from 'pinia'
import { ref } from 'vue'
import { ElNotification } from 'element-plus'
import request from '@/utils/request'

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)
  const list = ref<any[]>([])
  const ws = ref<WebSocket | null>(null)

  // 获取未读数量
  const fetchUnreadCount = async () => {
    // 检查 localStorage 中的 token，确保与 request.ts 行为一致
    // userStore.token 可能是旧的（在 401 自动登出后 store 状态未清除）
    const token = localStorage.getItem('token-admin')
    if (!token) return

    try {
      const res = await request({
        url: '/system/notification/unread-count',
        method: 'get'
      })
      unreadCount.value = res // Interceptor returns res.data which is the count
    } catch (error) {
      console.error(error)
    }
  }

  // 获取列表
  const fetchList = async (current = 1, size = 10) => {
    const token = localStorage.getItem('token-admin')
    if (!token) return { records: [], total: 0 }

    try {
      const res = await request({
        url: '/system/notification/list',
        method: 'get',
        params: { current, size }
      })
      // res is PageResult<SysUserNotification>
      list.value = res.records
      return res
    } catch (error) {
      console.error(error)
      return { records: [], total: 0 }
    }
  }

  // 标记已读
  const markRead = async (id: number) => {
    try {
      await request({
        url: `/system/notification/${id}/read`,
        method: 'put'
      })
      unreadCount.value = Math.max(0, unreadCount.value - 1)
      // 更新列表状态
      const item = list.value.find(i => i.id === id)
      if (item) item.isRead = 1
    } catch (error) {
      console.error(error)
    }
  }

  // 全部已读
  const markAllRead = async () => {
    try {
      await request({
        url: '/system/notification/read-all',
        method: 'put'
      })
      unreadCount.value = 0
      list.value.forEach(item => item.isRead = 1)
    } catch (error) {
      console.error(error)
    }
  }

  // 连接 WebSocket
  const connectWebSocket = () => {
    if (ws.value) return
    
    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
    // 改用 localStorage 获取 token，避免 stale token 导致无限重连
    const token = localStorage.getItem('token-admin')
    if (!token) return

    // 通过 Vite 管理端代理连接后端 /ws/notification/{token}
    const wsUrl = `${protocol}://${window.location.host}/admin-api/ws/notification/${token}`
    
    ws.value = new WebSocket(wsUrl)

    ws.value.onopen = () => {}

    ws.value.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        // 收到新通知
        unreadCount.value++
        ElNotification({
          title: data.title,
          message: data.content,
          type: data.type || 'info',
          position: 'top-right',
          duration: 3000
        })
        // 刷新列表（如果列表打开）
        fetchList()
      } catch (e) {
        console.error('WS Message Parse Error', e)
      }
    }

    ws.value.onclose = () => {
      ws.value = null
      // 断线重连机制可以加在这里
      setTimeout(() => connectWebSocket(), 5000)
    }

    ws.value.onerror = (error) => {
      console.error('WebSocket Error', error)
    }
  }

  return {
    unreadCount,
    list,
    fetchUnreadCount,
    fetchList,
    markRead,
    markAllRead,
    connectWebSocket
  }
})
