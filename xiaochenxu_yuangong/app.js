App({
  globalData: {
    baseUrl: 'http://localhost:8080',
    token: '',
    employee: null
  },

  onLaunch() {
    const employee = wx.getStorageSync('employee')
    const token = wx.getStorageSync('token') || (employee && employee.token) || ''
    if (employee) {
      this.globalData.employee = employee
    }
    if (token) {
      this.globalData.token = token
      wx.setStorageSync('token', token)
    }
  },

  setToken(token) {
    this.globalData.token = token
    wx.setStorageSync('token', token)
  },

  withAuthUrl(url) {
    if (!url || typeof url !== 'string') return url
    const employee = wx.getStorageSync('employee') || {}
    const token = this.globalData.token || wx.getStorageSync('token') || employee.token || ''
    let next = url
    if (!/^https?:\/\//.test(next)) {
      next = `${this.globalData.baseUrl}${next.startsWith('/') ? '' : '/'}${next}`
    }
    if (!next.includes('/files/download/') || !token || next.includes('token=')) return next
    const pure = token.startsWith('Bearer ') ? token.slice(7) : token
    if (!pure) return next
    const join = next.includes('?') ? '&' : '?'
    return `${next}${join}token=${encodeURIComponent(pure)}`
  },

  request({ url, method = 'GET', data = {}, header = {} }) {
    const fullUrl = `${this.globalData.baseUrl}${url}`;
    const employee = wx.getStorageSync('employee')
    const token = this.globalData.token || wx.getStorageSync('token') || (employee && employee.token) || '';
    const isLogin = url === '/LoginRegister/employeeLogin';
    if (!isLogin && !token) {
      wx.redirectTo({ url: '/pages/login/login' })
      return Promise.resolve({ code: '401', msg: '未登录' })
    }
    const reqData = Object.assign({}, data)
    const reqHeader = Object.assign(
      {
        'Content-Type': 'application/json',
      },
      header
    );
    if (!isLogin && token) {
      reqHeader['Authorization'] = 'Bearer ' + token
    }

    return new Promise((resolve, reject) => {
      wx.request({
        url: fullUrl,
        method,
        data: reqData,
        header: reqHeader,
        success: (res) => {
          if (res.statusCode === 401) {
            wx.redirectTo({ url: '/pages/login/login' })
          } else if (res.statusCode === 403) {
            wx.showToast({ title: '无权限或会话失效', icon: 'none' })
          }
          resolve(res.data);
        },
        fail: reject,
      });
    });
  }
})
