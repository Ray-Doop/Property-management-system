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
    this.setData({ list: [], page: 1, hasMore: true })
    this.loadData()
  },

  loadData() {
    if (this.data.loading) return
    this.setData({ loading: true })

    app.request({
      url: '/notice/List',
      data: {
        page: this.data.page,
        size: this.data.size
      }
    }).then(res => {
      this.setData({ loading: false })
      if (res.code === '200') {
        const data = res.data
        const newList = data.list
        this.setData({
          list: this.data.page === 1 ? newList : this.data.list.concat(newList),
          hasMore: newList.length === this.data.size
        })
      }
    }).catch(err => {
      this.setData({ loading: false })
      console.error(err)
    })
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.setData({ page: this.data.page + 1 })
      this.loadData()
    }
  },

  goToDetail(e) {
    const id = e.currentTarget.dataset.id
    // Can reuse detail page if exists, or show simple modal
    // For now simple modal
    const item = this.data.list.find(i => i.noticeId === id)
    wx.showModal({
      title: item.title,
      content: item.content,
      showCancel: false
    })
  },

  goToAdd() {
    wx.navigateTo({ url: '/pages/notice/add/add' })
  }
})
