import {BASE_URL} from '@/utils/request'

export const uploadFile = (filePath: string, businessId?: number): Promise<string> => {
    return new Promise((resolve, reject) => {
        const token = uni.getStorageSync('token')
        let url = BASE_URL + '/app/file/upload'
        const params: string[] = []
        if (businessId) {
            params.push(`businessId=${businessId}`)
        }
        if (params.length > 0) {
            url += '?' + params.join('&')
        }
        uni.uploadFile({
            url: url,
            filePath: filePath,
            name: 'file',
            header: {
                'token-app': token
            },
            success: (uploadFileRes) => {
                try {
                    const data = JSON.parse(uploadFileRes.data)
                    if (data.code === 200) {
                        resolve(data.data)
                    } else {
                        reject(new Error(data.message || '上传失败'))
                    }
                } catch (e) {
                    reject(new Error('解析响应失败'))
                }
            },
            fail: (err) => {
                reject(err)
            }
        })
    })
}
