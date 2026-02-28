const app = getApp()

const formatTimeRelative = (dateStr) => {
  if (!dateStr) return ''
  let date = new Date(dateStr)
  if (isNaN(date.getTime())) {
    dateStr = dateStr.replace('T', ' ').replace(/\..+/, '').replace(/-/g, '/')
    date = new Date(dateStr)
  }
  if (isNaN(date.getTime())) return dateStr
  const now = new Date()
  const diff = now - date
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  const month = 30 * day
  const year = 12 * month
  if (diff < minute) return '刚刚'
  if (diff < hour) return Math.floor(diff / minute) + '分钟前'
  if (diff < day) return Math.floor(diff / hour) + '小时前'
  if (diff < month) return Math.floor(diff / day) + '天前'
  if (diff < year) return Math.floor(diff / month) + '个月前'
  return Math.floor(diff / year) + '年前'
}

const withAuthUrl = (url) => {
  if (!url || typeof url !== 'string') return url
  const employee = wx.getStorageSync('employee') || {}
  const token = app.globalData.token || wx.getStorageSync('token') || employee.token || ''
  let next = url
  if (!/^https?:\/\//.test(next)) {
    next = `${app.globalData.baseUrl}${next.startsWith('/') ? '' : '/'}${next}`
  }
  if (!next.includes('/files/download/') || !token || next.includes('token=')) return next
  const pure = token.startsWith('Bearer ') ? token.slice(7) : token
  if (!pure) return next
  const join = next.includes('?') ? '&' : '?'
  return `${next}${join}token=${encodeURIComponent(pure)}`
}

Page({
  data: {
    list: [],
    page: 1,
    size: 10,
    loading: false,
    hasMore: true,
    defaultAvatar: '',
    currentTab: 'all',
    keyword: '',
    sections: [],
    selectedSectionId: 0,
    myPosts: [],
    myPostPage: 1,
    myPostLoading: false,
    myPostHasMore: true,
    myComments: [],
    myCommentPage: 1,
    myCommentLoading: false,
    myCommentHasMore: true
  },

  onShow() {
    this.setData({
      list: [],
      page: 1,
      hasMore: true,
      sections: [],
      selectedSectionId: 0,
      myPosts: [],
      myPostPage: 1,
      myPostHasMore: true,
      myComments: [],
      myCommentPage: 1,
      myCommentHasMore: true
    })
    this.loadSections()
    this.loadCurrentTab()
    this.setData({
      defaultAvatar: withAuthUrl(`${app.globalData.baseUrl}/files/download/img.jpg`)
    })
  },

  onPullDownRefresh() {
    if (this.data.currentTab === 'all') {
      this.resetAllPosts()
      this.loadAllPosts(() => wx.stopPullDownRefresh())
      return
    }
    if (this.data.currentTab === 'myPosts') {
      this.resetMyPosts()
      this.loadMyPosts(() => wx.stopPullDownRefresh())
      return
    }
    this.resetMyComments()
    this.loadMyComments(() => wx.stopPullDownRefresh())
  },

  onReachBottom() {
    if (this.data.currentTab === 'all') {
      if (this.data.hasMore && !this.data.loading) {
        this.setData({ page: this.data.page + 1 })
        this.loadAllPosts()
      }
      return
    }
    if (this.data.currentTab === 'myPosts') {
      if (this.data.myPostHasMore && !this.data.myPostLoading) {
        this.setData({ myPostPage: this.data.myPostPage + 1 })
        this.loadMyPosts()
      }
      return
    }
    if (this.data.myCommentHasMore && !this.data.myCommentLoading) {
      this.setData({ myCommentPage: this.data.myCommentPage + 1 })
      this.loadMyComments()
    }
  },

  loadCurrentTab() {
    if (this.data.currentTab === 'all') {
      this.loadAllPosts()
      return
    }
    if (this.data.currentTab === 'myPosts') {
      this.loadMyPosts()
      return
    }
    this.loadMyComments()
  },

  resetAllPosts() {
    this.setData({
      list: [],
      page: 1,
      hasMore: true
    })
  },

  resetMyPosts() {
    this.setData({
      myPosts: [],
      myPostPage: 1,
      myPostHasMore: true
    })
  },

  resetMyComments() {
    this.setData({
      myComments: [],
      myCommentPage: 1,
      myCommentHasMore: true
    })
  },

  changeTab(e) {
    const tab = e.currentTarget.dataset.tab
    if (tab === this.data.currentTab) return
    this.setData({ currentTab: tab })
    if (tab === 'all') {
      this.resetAllPosts()
      this.loadAllPosts()
      return
    }
    if (tab === 'myPosts') {
      this.resetMyPosts()
      this.loadMyPosts()
      return
    }
    this.resetMyComments()
    this.loadMyComments()
  },

  onSearchInput(e) {
    this.setData({ keyword: e.detail.value })
  },

  handleSearch() {
    if (this.data.currentTab !== 'all') {
      this.setData({ currentTab: 'all' })
    }
    this.resetAllPosts()
    this.loadAllPosts()
  },

  loadSections() {
    app.request({ url: '/Forum/Sections' }).then(res => {
      if (res.code === '200') {
        this.setData({ sections: res.data || [] })
      }
    })
  },

  selectSection(e) {
    const id = Number(e.currentTarget.dataset.id || 0)
    if (id === this.data.selectedSectionId) return
    this.setData({ selectedSectionId: id })
    if (this.data.currentTab !== 'all') {
      this.setData({ currentTab: 'all' })
    }
    this.resetAllPosts()
    this.loadAllPosts()
  },

  formatPostList(list) {
    return (list || []).map(item => {
      const next = { ...item }
      next.avatarUrl = withAuthUrl(next.avatarUrl)
      next.createdTime = formatTimeRelative(next.createdTime)
      if (Array.isArray(next.images)) {
        next.images = next.images.map(img => {
          if (typeof img === 'string') {
            return { url: withAuthUrl(img) }
          }
          return { ...img, url: withAuthUrl(img.url) }
        })
      }
      return next
    })
  },

  loadAllPosts(cb) {
    if (this.data.loading) return
    this.setData({ loading: true })
    const isSearch = this.data.keyword && this.data.keyword.trim()
    const useSection = !isSearch && this.data.selectedSectionId
    const url = isSearch ? '/Forum/SelectByTitle' : (useSection ? '/Forum/SwitchSection' : '/Forum/SelectPage')
    const data = { pageNum: this.data.page, pageSize: this.data.size }
    if (isSearch) data.title = this.data.keyword.trim()
    if (useSection) data.sectionId = this.data.selectedSectionId
    app.request({ url, data }).then(res => {
      if (res.code === '200') {
        const newList = this.formatPostList(res.data.list || [])
        this.setData({
          list: this.data.page === 1 ? newList : this.data.list.concat(newList),
          hasMore: newList.length === this.data.size,
          loading: false
        })
      } else {
        this.setData({ loading: false })
      }
      cb && cb()
    }).catch(() => {
      this.setData({ loading: false })
      cb && cb()
    })
  },

  loadMyPosts(cb) {
    if (this.data.myPostLoading) return
    const employee = wx.getStorageSync('employee')
    if (!employee) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      cb && cb()
      return
    }
    this.setData({ myPostLoading: true })
    app.request({
      url: '/Forum/showMyPost',
      data: {
        userId: employee.employeeId || employee.id || 0,
        pageNum: this.data.myPostPage,
        pageSize: this.data.size
      }
    }).then(res => {
      if (res.code === '200') {
        const newList = this.formatPostList(res.data.list || [])
        this.setData({
          myPosts: this.data.myPostPage === 1 ? newList : this.data.myPosts.concat(newList),
          myPostHasMore: newList.length === this.data.size,
          myPostLoading: false
        })
      } else {
        this.setData({ myPostLoading: false })
      }
      cb && cb()
    }).catch(() => {
      this.setData({ myPostLoading: false })
      cb && cb()
    })
  },

  loadMyComments(cb) {
    if (this.data.myCommentLoading) return
    const employee = wx.getStorageSync('employee')
    if (!employee) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      cb && cb()
      return
    }
    this.setData({ myCommentLoading: true })
    app.request({
      url: '/Forum/SelectMyPostComment',
      data: {
        userId: employee.employeeId || employee.id || 0,
        pageNum: this.data.myCommentPage,
        pageSize: this.data.size
      }
    }).then(res => {
      if (res.code === '200') {
        const newList = (res.data.list || []).map(item => ({
          ...item,
          createdTime: formatTimeRelative(item.createdTime),
          postCreatedTime: formatTimeRelative(item.postCreatedTime),
          avatarUrl: withAuthUrl(item.avatarUrl)
        }))
        this.setData({
          myComments: this.data.myCommentPage === 1 ? newList : this.data.myComments.concat(newList),
          myCommentHasMore: newList.length === this.data.size,
          myCommentLoading: false
        })
      } else {
        this.setData({ myCommentLoading: false })
      }
      cb && cb()
    }).catch(() => {
      this.setData({ myCommentLoading: false })
      cb && cb()
    })
  },

  deletePost(e) {
    const postId = e.currentTarget.dataset.id
    wx.showModal({
      title: '删除帖子',
      content: '确定删除这条帖子吗？',
      success: (res) => {
        if (!res.confirm) return
        app.request({
          url: `/Forum/delMyPost?postId=${postId}`,
          method: 'DELETE'
        }).then(resp => {
          if (resp.code === '200') {
            wx.showToast({ title: '删除成功' })
            this.resetMyPosts()
            this.loadMyPosts()
          } else {
            wx.showToast({ title: '删除失败', icon: 'none' })
          }
        })
      }
    })
  },

  deleteComment(e) {
    const commentId = e.currentTarget.dataset.id
    wx.showModal({
      title: '删除评论',
      content: '确定删除这条评论吗？',
      success: (res) => {
        if (!res.confirm) return
        app.request({
          url: `/Forum/delComment/${commentId}`,
          method: 'DELETE'
        }).then(resp => {
          if (resp.code === '200') {
            wx.showToast({ title: '删除成功' })
            this.resetMyComments()
            this.loadMyComments()
          } else {
            wx.showToast({ title: '删除失败', icon: 'none' })
          }
        })
      }
    })
  },

  previewImage(e) {
    const current = e.currentTarget.dataset.current
    const urlsData = e.currentTarget.dataset.urls
    let urls = []
    if (Array.isArray(urlsData) && urlsData.length > 0) {
      urls = urlsData.map(item => item.url || item.filePath || item)
    } else if (current) {
      urls = [current]
    }
    wx.previewImage({ current, urls })
  },

  goToDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/forum/detail/detail?id=${id}` })
  },

  goToPublish() {
    wx.navigateTo({ url: '/pages/forum/publish/publish' })
  }
})
