const tabPages = [
  '/pages/business/home/index',
  '/pages/business/category/index',
  '/pages/business/cart/index',
  '/pages/business/order/index',
  '/pages/business/mine/index'
]

export const isLoggedIn = () => {
  return !!uni.getStorageSync('token')
}

export const getCurrentFullPath = () => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  if (!currentPage?.route) {
    return '/pages/business/home/index'
  }

  const route = currentPage.route.startsWith('/') ? currentPage.route : `/${currentPage.route}`
  const options = (currentPage as any).options || {}
  const query = Object.keys(options)
    .map((key) => `${key}=${encodeURIComponent(options[key])}`)
    .join('&')

  return query ? `${route}?${query}` : route
}

export const goLogin = (redirect?: string) => {
  const target = redirect || getCurrentFullPath()
  const url = `/pages/login/index?redirect=${encodeURIComponent(target)}`

  uni.navigateTo({
    url,
    fail: () => {
      uni.redirectTo({
        url,
        fail: () => {
          uni.reLaunch({ url: '/pages/login/index' })
        }
      })
    }
  })
}

export const requireLogin = (redirect?: string) => {
  if (isLoggedIn()) {
    return true
  }

  goLogin(redirect)
  return false
}

export const goProtectedPage = (url: string) => {
  if (!requireLogin(url)) {
    return
  }

  const path = url.split('?')[0]
  if (tabPages.includes(path)) {
    uni.switchTab({ url: path })
    return
  }

  uni.navigateTo({ url })
}
