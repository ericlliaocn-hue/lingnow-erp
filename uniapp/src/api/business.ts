import {request} from '@/utils/request'

export const getMobileCustomers = () => {
    return request({url: '/app/erp/customers', method: 'GET'})
}

export const getMobileProducts = () => {
    return request({url: '/app/erp/products', method: 'GET'})
}

export const getMobileProductDetail = (id: string | number) => {
    return request({url: `/app/erp/products/${id}`, method: 'GET'})
}

export const getMobileBills = () => {
    return request({url: '/app/erp/bills', method: 'GET'})
}

export const getMobileDashboard = () => {
    return request({url: '/app/erp/dashboard', method: 'GET'})
}
