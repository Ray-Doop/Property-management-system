const app = getApp()

Page({
  data: {
    user: null,
    nickname: '',
    phone: ''
  },

  onLoad() {
    const user = wx.getStorageSync('user')
    if (user) {
      this.setData({
        user: user,
        nickname: user.nickname,
        phone: user.phone
      })
    }
  },

  bindNicknameInput(e) {
    this.setData({ nickname: e.detail.value })
  },

  bindPhoneInput(e) {
    this.setData({ phone: e.detail.value })
  },

  changeAvatar() {
    wx.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      success: (res) => {
        const tempFilePath = res.tempFilePaths[0]
        wx.uploadFile({
          url: app.globalData.baseUrl + '/files/upload', // Assuming endpoint
          filePath: tempFilePath,
          name: 'file',
          header: { 'Authorization': app.getBearerToken() },
          formData: { folder: 'avatar' },
          success: (uploadRes) => {
            const data = JSON.parse(uploadRes.data)
            if (data.code === '200') {
              const newUrl = data.data
              this.updateAvatarUrl(newUrl)
            }
          }
        })
      }
    })
  },

  updateAvatarUrl(url) {
    app.request({
      url: '/LoginRegister/UpdateAvatar',
      method: 'POST',
      data: {
        userId: this.data.user.userId,
        avatarUrl: url
      }
    }).then(res => {
      if (res.code === '200') {
        wx.showToast({ title: '头像更新成功' })
        const user = this.data.user
        user.avatarUrl = url
        this.setData({ user })
        wx.setStorageSync('user', user)
      }
    })
  },

  save() {
    app.request({
      url: '/LoginRegister/UpdateUserData',
      method: 'POST',
      data: {
        userId: this.data.user.userId,
        nickname: this.data.nickname,
        phone: this.data.phone
      }
    }).then(res => {
      if (res.code === '200') {
        wx.showToast({ title: '保存成功' })
        const user = this.data.user
        user.nickname = this.data.nickname
        user.phone = this.data.phone
        wx.setStorageSync('user', user)
        setTimeout(() => {
          wx.navigateBack()
        }, 1500)
      } else {
        wx.showToast({ title: '保存失败', icon: 'none' })
      }
    })
  }
})
