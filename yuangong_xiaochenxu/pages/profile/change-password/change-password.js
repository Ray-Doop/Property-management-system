const app = getApp()

Page({
  data: {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  },

  // 旧密码输入
  onOldPasswordInput(e) {
    this.setData({
      oldPassword: e.detail.value
    })
  },

  // 新密码输入
  onNewPasswordInput(e) {
    this.setData({
      newPassword: e.detail.value
    })
  },

  // 确认新密码输入
  onConfirmPasswordInput(e) {
    this.setData({
      confirmPassword: e.detail.value
    })
  },

  // 修改密码
  handleChangePassword() {
    const { oldPassword, newPassword, confirmPassword } = this.data
    const employee = wx.getStorageSync('employee')
    if (!employee) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      return
    }
    
    // 基本验证
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
      url: '/employee/checkOldPassword',
      method: 'POST',
      data: {
        employeeId: employee.employeeId || employee.id,
        oldPassword: oldPassword
      }
    }).then(res => {
      if (res.code === '200') {
        return this.updatePassword(employee.employeeId || employee.id, newPassword)
      } else {
        wx.hideLoading()
        wx.showToast({ title: res.msg || '旧密码验证失败', icon: 'none' })
      }
    }).catch(() => {
      wx.hideLoading()
      wx.showToast({ title: '网络请求失败', icon: 'none' })
    })
  },

  // 更新密码
  updatePassword(employeeId, newPassword) {
    return app.request({
      url: '/employee/updatePassword',
      method: 'POST',
      data: {
        employeeId,
        newPassword
      }
    }).then(res => {
      wx.hideLoading()
      if (res.code === '200') {
        // 密码修改成功，清除本地存储并跳转到登录页面
        wx.showToast({ title: '密码修改成功，请重新登录', icon: 'none', duration: 2000 })
        
        // 清除本地存储
        setTimeout(() => {
          wx.removeStorageSync('token')
          wx.removeStorageSync('employee')
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
