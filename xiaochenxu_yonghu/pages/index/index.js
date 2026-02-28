Page({
  data: {
    user: null,
    latestNotice: null,
    defaultAvatar: ''
  },

  onShow() {
    const app = getApp()
    const user = wx.getStorageSync('user')
    const token = app.globalData.token || wx.getStorageSync('token')

    if (!token) {
      wx.reLaunch({ url: '/pages/login/login' })
      return
    }

    if (user) {
      user.avatarUrl = app.withAuthUrl(user.avatarUrl)
      this.setData({ user })
    }
    this.setData({
      defaultAvatar: app.withAuthUrl(`${app.globalData.baseUrl}/files/download/img.jpg`)
    })

    this.getLatestNotice()
  },

  getLatestNotice() {
    const app = getApp()
    app.request({
      url: '/notice/List',
      data: { page: 1, size: 1 }
    }).then(res => {
      if (res.code === '200' && res.data.list && res.data.list.length > 0) {
        this.setData({ latestNotice: res.data.list[0] })
      }
    }).catch(err => {
      console.error('获取公告失败', err)
    })
  },

  navigateTo(e) {
    const url = e.currentTarget.dataset.url
    if (url) {
      wx.navigateTo({ url })
    }
  },

  switchTab(e) {
    const url = e.currentTarget.dataset.url
    if (url) {
      wx.switchTab({ url })
    }
  },

  goToLogin() {
    wx.reLaunch({ url: '/pages/login/login' })
  }
})
