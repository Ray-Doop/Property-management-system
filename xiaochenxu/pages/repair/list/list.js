const app = getApp()

Page({
  data: {
    list: [],
    page: 1,
    size: 10,
    loading: false,
    hasMore: true
  },

  onShow() {
    this.setData({
      list: [],
      page: 1,
      hasMore: true
    })
    this.loadData()
  },

  onPullDownRefresh() {
    this.setData({
      list: [],
      page: 1,
      hasMore: true
    })
    this.loadData(() => {
      wx.stopPullDownRefresh()
    })
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.setData({ page: this.data.page + 1 })
      this.loadData()
    }
  },

  loadData(cb) {
    if (this.data.loading) return
    const user = wx.getStorageSync('user')
    if (!user) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      return
    }

    this.setData({ loading: true })

    app.request({
      url: '/repair/myRepair',
      data: {
        userId: user.userId,
        page: this.data.page,
        size: this.data.size
      }
    }).then(res => {
      if (res.code === '200') {
        const newList = res.data.list || []
        this.setData({
          list: this.data.page === 1 ? newList : this.data.list.concat(newList),
          hasMore: newList.length === this.data.size,
          loading: false
        })
      } else {
        this.setData({ loading: false })
      }
      if (cb) cb()
    }).catch(err => {
      console.error(err)
      this.setData({ loading: false })
      if (cb) cb()
    })
  },

  goToSubmit() {
    wx.navigateTo({ url: '/pages/repair/submit/submit' })
  },

  goToDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/repair/detail/detail?id=${id}` })
  }
})
