const app = getApp()
const util = require('../../../utils/util.js')

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
      defaultAvatar: app.withAuthUrl(`${app.globalData.baseUrl}/files/download/img.jpg`)
    })
  },

  onPullDownRefresh() {
    if (this.data.currentTab === 'all') {
      this.resetAllPosts()
      this.loadAllPosts(() => {
        wx.stopPullDownRefresh()
      })
      return
    }
    if (this.data.currentTab === 'myPosts') {
      this.resetMyPosts()
      this.loadMyPosts(() => {
        wx.stopPullDownRefresh()
      })
      return
    }
    this.resetMyComments()
    this.loadMyComments(() => {
      wx.stopPullDownRefresh()
    })
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
    app.request({
      url: '/Forum/Sections'
    }).then(res => {
      if (res.code === '200') {
        this.setData({ sections: res.data || [] })
      }
    }).catch(err => {
      console.error(err)
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
      next.avatarUrl = app.withAuthUrl(next.avatarUrl)
      next.createdTime = util.formatTimeRelative(next.createdTime)
      if (Array.isArray(next.images)) {
        next.images = next.images.map(img => {
          if (typeof img === 'string') {
            return { url: app.withAuthUrl(img) }
          }
          return {
            ...img,
            url: app.withAuthUrl(img.url)
          }
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
    const data = {
      pageNum: this.data.page,
      pageSize: this.data.size
    }
    if (isSearch) {
      data.title = this.data.keyword.trim()
    }
    if (useSection) {
      data.sectionId = this.data.selectedSectionId
    }

    app.request({
      url,
      data
    }).then(res => {
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
      if (cb) cb()
    }).catch(err => {
      console.error(err)
      this.setData({ loading: false })
      if (cb) cb()
    })
  },

  loadMyPosts(cb) {
    if (this.data.myPostLoading) return
    const user = wx.getStorageSync('user')
    if (!user) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      if (cb) cb()
      return
    }
    this.setData({ myPostLoading: true })
    app.request({
      url: '/Forum/showMyPost',
      data: {
        userId: user.userId,
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
      if (cb) cb()
    }).catch(err => {
      console.error(err)
      this.setData({ myPostLoading: false })
      if (cb) cb()
    })
  },

  loadMyComments(cb) {
    if (this.data.myCommentLoading) return
    const user = wx.getStorageSync('user')
    if (!user) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      if (cb) cb()
      return
    }
    this.setData({ myCommentLoading: true })
    app.request({
      url: '/Forum/SelectMyPostComment',
      data: {
        userId: user.userId,
        pageNum: this.data.myCommentPage,
        pageSize: this.data.size
      }
    }).then(res => {
      if (res.code === '200') {
        const newList = (res.data.list || []).map(item => ({
          ...item,
          createdTime: util.formatTimeRelative(item.createdTime),
          postCreatedTime: util.formatTimeRelative(item.postCreatedTime),
          avatarUrl: app.withAuthUrl(item.avatarUrl)
        }))
        this.setData({
          myComments: this.data.myCommentPage === 1 ? newList : this.data.myComments.concat(newList),
          myCommentHasMore: newList.length === this.data.size,
          myCommentLoading: false
        })
      } else {
        this.setData({ myCommentLoading: false })
      }
      if (cb) cb()
    }).catch(err => {
      console.error(err)
      this.setData({ myCommentLoading: false })
      if (cb) cb()
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

  goToDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/forum/detail/detail?id=${id}` })
  },

  goToPublish() {
    wx.navigateTo({ url: '/pages/forum/publish/publish' })
  }
})
