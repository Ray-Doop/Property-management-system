App({
  globalData: {
    baseUrl: 'http://localhost:8080',
    token: ''
  },
  
  onLaunch() {
    const token = wx.getStorageSync('token')
    if (token) {
      this.globalData.token = token
    }
  },

  setToken(token) {
    this.globalData.token = token
    wx.setStorageSync('token', token)
  },

  getToken() {
    const user = wx.getStorageSync('user') || {}
    return this.globalData.token || wx.getStorageSync('token') || user.token || ''
  },

  getBearerToken() {
    const token = this.getToken()
    if (!token) return ''
    return token.startsWith('Bearer ') ? token : `Bearer ${token}`
  },

  withAuthUrl(url) {
    if (!url || typeof url !== 'string') return url
    if (!url.includes('/files/download/')) return url
    const token = this.getToken()
    if (!token) return url
    const pure = token.startsWith('Bearer ') ? token.slice(7) : token
    if (!pure) return url
    if (url.includes('token=')) return url
    const join = url.includes('?') ? '&' : '?'
    return `${url}${join}token=${encodeURIComponent(pure)}`
  },

  request({ url, method = 'GET', data = {}, header = {} }) {
    const fullUrl = `${this.globalData.baseUrl}${url}`;
    const token = this.getBearerToken();
    const reqHeader = Object.assign(
      {
        'Content-Type': 'application/json',
      },
      token ? { Authorization: token } : {},
      header
    );
    return new Promise((resolve, reject) => {
      wx.request({
        url: fullUrl,
        method,
        data,
        header: reqHeader,
        timeout: 30000,
        success: (res) => {
          if (typeof res.data === 'string') {
            try {
              res.data = JSON.parse(res.data);
            } catch (e) {}
          }
          resolve(res.data);
        },
        fail: reject,
      });
    });
  }
})
