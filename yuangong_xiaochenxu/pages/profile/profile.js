const app = getApp()

Page({
  data: {
    employee: null,
    nicknameInput: '',
    saving: false,
    avatarSrc: ''
  },

  onLoad() {
    // 从本地存储获取员工信息
    const employee = wx.getStorageSync('employee')
    if (!employee) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      wx.navigateBack()
      return
    }
    this.setData({
      employee,
      nicknameInput: employee.nickname || employee.name || '',
      avatarSrc: this.getAvatarUrl(employee)
    })
  },

  onShow() {
    const employee = wx.getStorageSync('employee')
    if (employee) {
      this.setData({
        employee,
        nicknameInput: this.data.nicknameInput || employee.nickname || employee.name || '',
        avatarSrc: this.getAvatarUrl(employee)
      })
    }
  },

  // 选择头像
  chooseAvatar() {
    const that = this
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      sizeType: ['compressed'],
      success(res) {
        const tempFilePath = res.tempFiles[0].tempFilePath
        // 上传头像到服务器
        that.uploadAvatar(tempFilePath)
      },
      fail(err) {
        console.error('选择头像失败', err)
      }
    })
  },

  // 上传头像
  uploadAvatar(filePath) {
    const that = this
    const employee = this.data.employee
    const token = app.globalData.token || wx.getStorageSync('token') || (employee && employee.token) || ''
    
    wx.showLoading({ title: '上传中...' })
    
    wx.uploadFile({
      url: app.globalData.baseUrl + '/employee/updateAvatar',
      filePath: filePath,
      name: 'file',
      formData: {
        employeeId: employee.employeeId
      },
      header: {
        'Authorization': token ? 'Bearer ' + token : ''
      },
      success(res) {
        wx.hideLoading()
        if (res.statusCode !== 200 || !res.data) {
          wx.showToast({ title: '头像上传失败', icon: 'none' })
          return
        }
        try {
          const data = JSON.parse(res.data)
          if (data.code === '200') {
            employee.avatarUrl = data.data.avatarUrl
            wx.setStorageSync('employee', employee)
            that.setData({
              employee,
              avatarSrc: that.getAvatarUrl(employee)
            })
            wx.showToast({ title: '头像更新成功', icon: 'success' })
          } else {
            wx.showToast({ title: data.msg || '头像上传失败', icon: 'none' })
          }
        } catch (e) {
          wx.showToast({ title: '上传失败', icon: 'none' })
        }
      },
      fail(err) {
        wx.hideLoading()
        console.error('上传头像失败', err)
        wx.showToast({ title: '网络请求失败', icon: 'none' })
      }
    })
  },

  getAvatarUrl(employee) {
    const avatar = employee && employee.avatarUrl ? employee.avatarUrl : `${app.globalData.baseUrl}/files/download/img.jpg`
    return app.withAuthUrl(avatar)
  },

  bindNicknameInput(e) {
    this.setData({ nicknameInput: e.detail.value })
  },

  saveProfile() {
    if (this.data.saving) return
    const employee = this.data.employee
    if (!employee) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      return
    }
    const nickname = (this.data.nicknameInput || '').trim()
    if (!nickname) {
      wx.showToast({ title: '昵称不能为空', icon: 'none' })
      return
    }
    const sameNickname = nickname === (employee.nickname || employee.name || '')
    if (sameNickname) {
      wx.showToast({ title: '昵称未修改', icon: 'none' })
      return
    }
    this.setData({ saving: true })
    app.request({
      url: '/employee/updata',
      method: 'PUT',
      data: {
        employeeId: employee.employeeId,
        nickname
      }
    }).then(res => {
      if (res.code === '200') {
        const next = { ...employee, nickname }
        wx.setStorageSync('employee', next)
        this.setData({ employee: next, saving: false })
        wx.showToast({ title: '保存成功' })
      } else {
        this.setData({ saving: false })
        wx.showToast({ title: res.msg || '保存失败', icon: 'none' })
      }
    }).catch(() => {
      this.setData({ saving: false })
      wx.showToast({ title: '网络请求失败', icon: 'none' })
    })
  },

  // 跳转到修改密码页面
  goToChangePassword() {
    wx.navigateTo({ url: '/pages/profile/change-password/change-password' })
  }
})
