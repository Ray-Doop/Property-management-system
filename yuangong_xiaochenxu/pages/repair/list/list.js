const app = getApp()

Page({
  data: {
    list: [],
    page: 1,
    size: 10,
    status: '待处理',
    loading: false,
    hasMore: true,
    stats: {
      pending: 0,
      processing: 0,
      completed: 0
    }
  },

  onShow() {
    this.setData({ list: [], page: 1, hasMore: true })
    this.loadData()
    this.loadStats()
  },

  loadStats() {
    app.request({ url: '/repair/stats' }).then(res => {
      if (res.code === '200') {
        this.setData({ stats: res.data })
      }
    })
  },

  switchTab(e) {
    const status = e.currentTarget.dataset.status
    this.setData({ status, list: [], page: 1, hasMore: true })
    this.loadData()
  },

  loadData() {
    if (this.data.loading) return
    this.setData({ loading: true })

    const employee = wx.getStorageSync('employee') || getApp().globalData.employee || {}
    const status = this.data.status
    let url = '/repair/status'
    let params = { page: this.data.page, size: this.data.size, status }

    // if (status === '已完成') {
    //   // 已完成只看本人? 暂时先用 status 接口统一查，后续根据需求调整
    //   // url = '/repair/byWorker'
    //   // params.workerId = employee.employeeId || employee.id || 0
    // }

    app.request({ url, data: params }).then(res => {
      this.setData({ loading: false })
      if (res.code === '200') {
        const data = res.data
        const newList = data.list || []
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
    const status = e.currentTarget.dataset.status || ''
    wx.navigateTo({ url: `/pages/repair/detail/detail?id=${id}&status=${encodeURIComponent(status)}` })
  }
})
