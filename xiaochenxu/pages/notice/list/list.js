const app = getApp()

Page({
  data: {
    list: [],
    page: 1,
    size: 10,
    loading: false,
    hasMore: true,
    currentTab: null, // null:全部, 0:未读, 1:已读
    userId: null,
    keyword: ''
  },

  onShow() {
    // 获取当前用户ID
    const user = wx.getStorageSync('user')
    if (user) {
      this.setData({ userId: user.userId })
    }
    
    this.setData({
      list: [],
      page: 1,
      hasMore: true
    })
    this.loadData()
  },

  onKeywordInput(e) {
    this.setData({ keyword: e.detail.value })
  },

  onSearch() {
    this.setData({
      list: [],
      page: 1,
      hasMore: true
    })
    this.loadData()
  },

  clearKeyword() {
    this.setData({
      keyword: '',
      list: [],
      page: 1,
      hasMore: true
    })
    this.loadData()
  },
  
  switchTab(e) {
    const tab = e.currentTarget.dataset.tab
    // tab: '', '0', '1'
    let queryReadStatus = null
    if (tab === '0') queryReadStatus = 0
    if (tab === '1') queryReadStatus = 1
    
    if (this.data.currentTab === queryReadStatus) return

    this.setData({
      currentTab: queryReadStatus,
      list: [],
      page: 1,
      hasMore: true
    })
    this.loadData()
  },

  loadData(cb) {
    if (this.data.loading) return
    this.setData({ loading: true })

    const params = {
      page: this.data.page,
      size: this.data.size,
      status: 1 // Only show published notices
    }
    
    if (this.data.userId) {
      params.currentUserId = this.data.userId
    }
    
    if (this.data.currentTab !== null) {
      params.queryReadStatus = this.data.currentTab
    }

    if (this.data.keyword) {
      params.title = this.data.keyword
    }

    app.request({
      url: '/notice/List',
      data: params
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

  goToDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/notice/detail/detail?id=${id}` })
  }
})
