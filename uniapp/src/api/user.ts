import {request} from '../utils/request';

export const login = (data: any) => {
  return request({
    url: '/app/auth/login',
    method: 'POST',
    data
  });
};

export const register = (data: any) => {
  return request({
    url: '/app/auth/register',
    method: 'POST',
    data
  });
};

export const forgetPassword = (data: any) => {
  return request({
    url: '/app/auth/forget-password',
    method: 'POST',
    data
  });
};

export const getProfile = (options?: any) => {
  return request({
    url: '/app/user/profile',
      method: 'GET',
      ...options
  });
};

export const updateProfile = (data: any) => {
  return request({
    url: '/app/user/profile',
    method: 'PUT',
    data
  });
};

export const sendCode = (phone: string) => {
    return request({
        url: `/app/auth/send-code?phone=${phone}`,
        method: 'POST'
    });
};

export const validateCode = (data: any) => {
    return request({
        url: `/app/auth/validate-code?phone=${data.phone}&code=${data.code}`,
        method: 'POST'
    });
};

export const changePassword = (data: any) => {
    return request({
        url: '/app/user/password',
        method: 'POST',
        data
    });
};

export const changePhone = (data: any) => {
    return request({
        url: '/app/user/phone',
        method: 'POST',
        data
    });
};

export const getSubscribedList = (params?: any) => {
    return request({
        url: '/app/user/subscribed',
        method: 'GET',
        data: params
    });
};

export const getFavoritesList = (params?: any) => {
    return request({
        url: '/app/user/favorites',
        method: 'GET',
        data: params
    });
};

export const getPurchasedList = (params?: any) => {
    return request({
        url: '/app/user/purchased',
        method: 'GET',
        data: params
    });
};

export const getHistoryList = (params?: any) => {
    return request({
        url: '/app/user/history',
        method: 'GET',
        data: params
    });
};

export const getUserCounts = () => {
    return request({
        url: '/app/user/counts',
        method: 'GET'
    });
};
