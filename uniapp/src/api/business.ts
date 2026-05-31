import {request} from '@/utils/request'

const businessApiEnabled = import.meta.env.VITE_BUSINESS_API_ENABLED === 'true'

const emptyResult = (data: any[] = []) => Promise.resolve({
    code: 200,
    message: 'success',
    data
})

const disabledResult = () => Promise.reject({
    code: 503,
    message: '业务接口未接入',
    data: null
})

export const getProductCategories = () => {
    if (!businessApiEnabled) return emptyResult()
    return request({url: '/app/business/product-categories', method: 'GET'})
}

export const getProducts = () => {
    if (!businessApiEnabled) return emptyResult()
    return request({url: '/app/business/products', method: 'GET'})
}

export const getCartItems = (userId?: number) => {
    if (!businessApiEnabled) return emptyResult()
    const query = typeof userId === 'number' ? `?userId=${userId}` : ''
    return request({url: '/app/business/cart' + query, method: 'GET'})
}

export const addCartItem = (data: any) => {
    if (!businessApiEnabled) return disabledResult()
    return request({url: '/app/business/cart', method: 'POST', data})
}

export const updateCartItem = (id: number, data: any) => {
    if (!businessApiEnabled) return disabledResult()
    return request({url: `/app/business/cart/item/${id}`, method: 'PUT', data})
}

export const removeCartItem = (id: number) => {
    if (!businessApiEnabled) return disabledResult()
    return request({url: `/app/business/cart/item/${id}`, method: 'DELETE'})
}

export const clearCart = (userId: number) => {
    if (!businessApiEnabled) return disabledResult()
    return request({url: `/app/business/cart/clear?userId=${userId}`, method: 'DELETE'})
}

export const getOrders = (userId?: number) => {
    if (!businessApiEnabled) return emptyResult()
    const query = typeof userId === 'number' ? `?userId=${userId}` : ''
    return request({url: '/app/business/orders' + query, method: 'GET'})
}

export const getStores = () => {
    if (!businessApiEnabled) return emptyResult()
    return request({url: '/app/business/stores', method: 'GET'})
}

export const getUsers = () => {
    if (!businessApiEnabled) return emptyResult()
    return request({url: '/app/business/users', method: 'GET'})
}

export const createOrder = (data: any) => {
    if (!businessApiEnabled) return disabledResult()
    return request({url: '/app/business/orders', method: 'POST', data})
}
