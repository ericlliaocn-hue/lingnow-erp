export const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/app-api';

interface ApiResponse<T = any> {
  code: number;
  message: string;
  data: T;
}

let isReLoginModalShown = false;

const handleUnauthorized = (silent = false) => {
  if (isReLoginModalShown) return;

  uni.removeStorageSync('token');
  uni.removeStorageSync('userInfo');

    if (silent) return;

    // Trigger Global Login Popup
    uni.$emit('showGlobalLoginPopup');
};

export const request = <T = any>(options: UniApp.RequestOptions & {
    handleBigInt?: boolean;
    silent?: boolean;
}): Promise<ApiResponse<T>> => {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token');

      // Default to true for handleBigInt to support Snowflake IDs globally
      const shouldHandleBigInt = options.handleBigInt !== false;

      const header: any = {
          ...options.header
      };
      if (token) {
          header['token-app'] = token;
      }

    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data,
        header: header,
        // Request text if we need to handle BigInt manually, otherwise let uniapp parse JSON
        dataType: shouldHandleBigInt ? 'text' : 'json',
      success: (res) => {
          let data = res.data;

          // Handle BigInt precision loss
          if (shouldHandleBigInt && typeof data === 'string') {
              try {
                  // Wrap numbers with 16+ digits in quotes to prevent precision loss
                  // Matches ": 1234..." or ":1234..." or ":[1234...]"
                  const str = data.replace(/(":\s*)(\d{16,})/g, '$1"$2"');
                  data = JSON.parse(str);
              } catch (e) {
                  console.error('BigInt JSON parse failed', e);
                  try {
                      data = JSON.parse(res.data as string);
                  } catch (e2) {
                      // ignore
                  }
              }
          }

          const apiRes = data as ApiResponse<T>;
        if (res.statusCode === 200) {
            if (apiRes.code === 200) {
                resolve(apiRes);
            } else if (apiRes.code === 401) {
                handleUnauthorized(options.silent);
                reject(apiRes);
          } else {
                if (!options.silent) {
                    uni.showToast({title: apiRes.message || '请求失败', icon: 'none'});
                }
                reject(apiRes);
          }
        } else if (res.statusCode === 401) {
            handleUnauthorized(options.silent);
          reject(res);
        } else {
            if (!options.silent) {
                uni.showToast({title: '网络错误', icon: 'none'});
            }
          reject(res);
        }
      },
      fail: (err) => {
          if (!options.silent) {
              uni.showToast({title: '网络错误', icon: 'none'});
          }
        reject(err);
      }
    });
  });
};
