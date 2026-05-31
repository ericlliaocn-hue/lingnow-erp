import {request} from '@/utils/request'

export interface MessageSetting {
    id?: number
    userId?: number
    allowStrangerMsg: boolean
    foldStrangerMsg: boolean
}

export const getMessageSetting = () => {
    return request<MessageSetting>({
        url: '/app/message/setting/get',
        method: 'GET'
    })
}

export const updateMessageSetting = (data: Partial<MessageSetting>) => {
    return request({
        url: '/app/message/setting/update',
        method: 'POST',
        data
    })
}
