const app = getApp()

Page({
  data: {
    username: '',
    password: '',
    isUserFocus: false,
    isPwdFocus: false
  },

  onUserFocus() {
    this.setData({ isUserFocus: true })
  },

  onUserBlur() {
    this.setData({ isUserFocus: false })
  },

  onPwdFocus() {
    this.setData({ isPwdFocus: true })
  },

  onPwdBlur() {
    this.setData({ isPwdFocus: false })
  },

  handleLogin() {
    if (!this.data.username || !this.data.password) {
      wx.showToast({ title: '请输入用户名和密码', icon: 'none' })
      return
    }

    wx.showLoading({ title: '登录中...' })

    app.request({
      url: '/LoginRegister/employeeLogin',
      method: 'POST',
      data: {
        username: this.data.username,
        password: this.data.password
      }
    }).then(res => {
      wx.hideLoading()
      if (res.code === '200' || res.code === 200) {
        const employee = res.data
        if (employee && employee.token) {
          app.setToken(employee.token)
        }
        if (employee) {
          app.globalData.employee = employee
          wx.setStorageSync('employee', employee)
        }
        
        wx.showToast({ title: '登录成功' })
        setTimeout(() => {
          wx.reLaunch({ url: '/pages/index/index' })
        }, 1500)
      } else {
        wx.showToast({ title: res.msg || '登录失败', icon: 'none' })
      }
    }).catch(err => {
      wx.hideLoading()
      console.error(err)
      wx.showToast({ title: '网络错误', icon: 'none' })
    })
  },

  goToOwner() {
    // Optional: link to owner app if needed, but usually separate
  }
})
