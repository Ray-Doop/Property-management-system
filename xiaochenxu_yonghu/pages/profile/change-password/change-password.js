const app = getApp()

Page({
  data: {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  },

  onLoad() {
    // 检查用户是否已登录
    const user = wx.getStorageSync('user')
    if (!user) {
      wx.reLaunch({ url: '/pages/login/login' })
    }
  },

  handleChangePassword() {
    // 获取当前登录用户
    const user = wx.getStorageSync('user')
    if (!user) {
      wx.showToast({ title: '用户未登录', icon: 'none' })
      return
    }

    const { oldPassword, newPassword, confirmPassword } = this.data
    
    // 客户端验证
    if (!oldPassword || !newPassword || !confirmPassword) {
      wx.showToast({ title: '请填写完整信息', icon: 'none' })
      return
    }

    if (newPassword !== confirmPassword) {
      wx.showToast({ title: '新密码和确认密码不一致', icon: 'none' })
      return
    }

    // 验证旧密码
    wx.showLoading({ title: '验证中...' })

    app.request({
      url: '/LoginRegister/CheckOldPassword',
      method: 'POST',
      data: {
        username: user.username,
        password: oldPassword
      }
    }).then(res => {
      if (res.code === '200') {
        // 旧密码验证成功，修改密码
        return this.updatePassword(user.username, newPassword)
      } else {
        wx.hideLoading()
        wx.showToast({ title: res.msg || '旧密码验证失败', icon: 'none' })
      }
    }).catch(err => {
      wx.hideLoading()
      console.error('旧密码验证失败', err)
      wx.showToast({ title: '网络请求失败', icon: 'none' })
    })
  },

  updatePassword(username, newPassword) {
    return app.request({
      url: '/LoginRegister/UpdatePassword',
      method: 'POST',
      data: {
        username,
        password: newPassword
      }
    }).then(res => {
      wx.hideLoading()
      if (res.code === '200') {
        // 密码修改成功，清除本地存储并跳转到登录页面
        wx.showToast({ title: '密码修改成功，请重新登录', icon: 'none', duration: 2000 })
        
        // 清除本地存储
        setTimeout(() => {
          wx.removeStorageSync('token')
          wx.removeStorageSync('user')
          app.globalData.token = null
          wx.reLaunch({ url: '/pages/login/login' })
        }, 2000)
      } else {
        wx.showToast({ title: res.msg || '密码修改失败', icon: 'none' })
      }
    }).catch(err => {
      wx.hideLoading()
      console.error('密码修改失败', err)
      wx.showToast({ title: '网络请求失败', icon: 'none' })
    })
  }
})