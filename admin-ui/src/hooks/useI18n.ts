import { useSettingsStore } from '@/store/modules/settings'

const messages: Record<string, Record<string, string>> = {
  'zh-CN': {
    'sys.menu.dashboard': '数据看板',
    'sys.menu.system': '系统管理',
    'sys.menu.user': '用户管理',
    'sys.menu.role': '角色管理',
    'sys.menu.menu': '菜单管理',
    'sys.menu.logs': '操作日志',
    'layout.header.home': '首页',
    'layout.header.profile': '个人中心',
    'layout.header.settings': '设置',
    'layout.header.logout': '退出登录',
    'notification.title': '通知',
    'notification.tab.all': '全部',
    'notification.tab.unread': '未读',
    'notification.tab.archived': '已读',
    'notification.empty': '暂无通知',
    'notification.markAllRead': '全部标记为已读',
    'notification.category.system': '系统',
    'notification.type.info': '信息',
    'notification.type.success': '成功',
    'notification.type.warning': '警告',
    'notification.type.error': '错误',
  },
  'en': {
    'sys.menu.dashboard': 'Data Board',
    'sys.menu.system': 'System Management',
    'sys.menu.user': 'User Management',
    'sys.menu.role': 'Role Management',
    'sys.menu.menu': 'Menu Management',
    'sys.menu.logs': 'Operation Logs',
    'layout.header.home': 'Home',
    'layout.header.profile': 'Profile',
    'layout.header.settings': 'Settings',
    'layout.header.logout': 'Logout',
    'notification.title': 'Notifications',
    'notification.tab.all': 'All',
    'notification.tab.unread': 'Unread',
    'notification.tab.archived': 'Archived',
    'notification.empty': 'No notifications',
    'notification.markAllRead': 'Mark all as read',
    'notification.category.system': 'System',
    'notification.type.info': 'Info',
    'notification.type.success': 'Success',
    'notification.type.warning': 'Warning',
    'notification.type.error': 'Error',
  }
}

// Fallback map if keys aren't used (since backend might return "系统管理" directly)
const nameMap: Record<string, Record<string, string>> = {
  'zh-CN': {
    'System Management': '系统管理',
    'User Management': '用户管理',
    'Role Management': '角色管理',
    'Menu Management': '菜单管理',
    'Operation Logs': '操作日志',
    'Dashboard': '数据看板',
    'Data Board': '数据看板',
  },
  'en': {
    '系统管理': 'System Management',
    '用户管理': 'User Management',
    '角色管理': 'Role Management',
    '菜单管理': 'Menu Management',
    '操作日志': 'Operation Logs',
    '仪表盘': 'Data Board',
    '数据看板': 'Data Board',
    '日志管理': 'Log Management',
    '匹配管理': 'Matching Management'
  }
}

export function useI18n() {
  const settingsStore = useSettingsStore()

  const t = (key: string) => {
    if (!key) return ''
    const lang = settingsStore.settings.language || 'zh-CN'

    // 1. Try key lookup (standard i18n)
    if (messages[lang]?.[key]) return messages[lang][key]

    // 2. Try name lookup (fallback for dynamic backend data without keys)
    if (nameMap[lang]?.[key]) return nameMap[lang][key]

    return key
  }

  return { t }
}
