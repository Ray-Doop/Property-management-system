const app = getApp()
const util = require('../../../../utils/util.js')

Page({
  data: {
    list: [],
    page: 1,
    size: 10,
    loading: false,
    hasMore: true,
    user: null
  },

  onLoad() {
    const user = wx.getStorageSync('user')
    this.setData({ user })
    if (user) {
      this.loadData()
    } else {
      wx.showToast({ title: '请先登录', icon: 'none' })
    }
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
    this.setData({ loading: true })

    app.request({
      url: '/travel-pass/myRecords',
      data: {
        pageNum: this.data.page,
        pageSize: this.data.size,
        username: this.data.user.username
      }
    }).then(res => {
      if (res.code === '200' || Array.isArray(res.list)) { // handle PageInfo or List
        const data = res.data || res
        const newList = (data.list || data).map(item => {
          item.issueTimeStr = util.formatTime(new Date(item.issueTime))
          item.expireTimeStr = util.formatTime(new Date(item.expireTime))
          return item
        })
        
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

  goToDetail(e) {
    const id = e.currentTarget.dataset.id
    const item = this.data.list.find(i => i.id === id)
    wx.navigateTo({
      url: `/pages/pass/record/detail/detail?id=${id}`,
      success: (res) => {
        res.eventChannel.emit('acceptDataFromOpenerPage', item)
      }
    })
  }
})
