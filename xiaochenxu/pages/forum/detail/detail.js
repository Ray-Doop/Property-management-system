const app = getApp()
const util = require('../../../utils/util.js')

Page({
  data: {
    post: null,
    commentContent: '',
    isCollected: false,
    commentImages: [],
    replyToCommentId: null,
    replyToNickname: '',
    defaultAvatar: '',
    showEmojiPanel: false,
    inputFocus: false,
    emojiList: ['😀','😄','😁','😂','🤣','😊','😍','😘','😜','🤔','😴','😅','😇','😉','😌','😎','🥳','😤','😭','😡','👍','👎','🙏','🎉']
  },

  onLoad(options) {
    if (options.id) {
      this.loadDetail(options.id)
    }
    this.setData({
      defaultAvatar: app.withAuthUrl(`${app.globalData.baseUrl}/files/download/img.jpg`)
    })
  },

  loadDetail(id) {
    wx.showLoading({ title: '加载中' })
    app.request({
      url: `/Forum/PostDetail/${id}`,
      method: 'GET'
    }).then(res => {
      wx.hideLoading()
      if (res.code === '200') {
        const post = { ...res.data }
        post.avatarUrl = app.withAuthUrl(post.avatarUrl)
        post.createdTime = util.formatTimeRelative(post.createdTime) // Format time
        if (Array.isArray(post.images)) {
          post.images = post.images.map(img => {
            if (typeof img === 'string') {
              return { url: app.withAuthUrl(img) }
            }
            return {
              ...img,
              url: app.withAuthUrl(img.url)
            }
          })
        }
        if (Array.isArray(post.comments)) {
          post.comments = post.comments.map(comment => {
            const next = { ...comment }
            next.avatarUrl = app.withAuthUrl(next.avatarUrl)
            next.createdTime = util.formatTimeRelative(next.createdTime) // Format comment time
            if (Array.isArray(next.images)) {
              next.images = next.images.map(img => {
                if (typeof img === 'string') return app.withAuthUrl(img)
                return app.withAuthUrl(img.url || img)
              })
            }
            if (Array.isArray(next.attachments)) {
              next.attachments = next.attachments.map(att => ({
                ...att,
                url: app.withAuthUrl(att.url || att.filePath)
              }))
            }
            if (Array.isArray(next.replies)) {
              next.replies = next.replies.map(reply => {
                const r = { ...reply }
                r.avatarUrl = app.withAuthUrl(r.avatarUrl)
                r.createdTime = util.formatTimeRelative(r.createdTime)
                if (Array.isArray(r.images)) {
                  r.images = r.images.map(img => {
                    if (typeof img === 'string') return app.withAuthUrl(img)
                    return app.withAuthUrl(img.url || img)
                  })
                }
                if (Array.isArray(r.attachments)) {
                  r.attachments = r.attachments.map(att => ({
                    ...att,
                    url: app.withAuthUrl(att.url || att.filePath)
                  }))
                }
                return r
              })
            }
            return next
          })
        }
        this.setData({ post })
        this.checkCollected(id)
      } else {
        wx.showToast({ title: '加载失败', icon: 'none' })
      }
    }).catch(err => {
      wx.hideLoading()
      console.error(err)
    })
  },

  checkCollected(postId) {
    const user = wx.getStorageSync('user')
    if (!user) return
    app.request({
      url: '/Forum/isCollected',
      method: 'GET',
      data: { postId, userId: user.userId }
    }).then(res => {
      if (res.code === '200') {
        this.setData({ isCollected: res.data })
      }
    })
  },

  toggleCollect() {
    const user = wx.getStorageSync('user')
    if (!user) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      return
    }
    const url = this.data.isCollected ? '/Forum/delCancelCollect' : '/Forum/addToCollection'
    // For delete, backend expects body? Let's check Controller.
    // delCancelCollect: @DeleteMapping @RequestBody ForumPost
    // addToCollection: @PostMapping @RequestBody ForumPost
    
    const data = {
      postId: this.data.post.postId,
      userId: user.userId
    }

    app.request({
      url: url,
      method: this.data.isCollected ? 'DELETE' : 'POST',
      data: data
    }).then(res => {
      if (res.code === '200') {
        this.setData({ isCollected: !this.data.isCollected })
        wx.showToast({ title: this.data.isCollected ? '已收藏' : '已取消' })
      }
    })
  },

  bindInput(e) {
    this.setData({ commentContent: e.detail.value })
  },
  handleInputFocus() {
    if (this.data.showEmojiPanel) {
      this.setData({ showEmojiPanel: false })
    }
  },
  showEmojiPanel() {
    const next = !this.data.showEmojiPanel
    this.setData({ showEmojiPanel: next, inputFocus: false })
    if (next) {
      wx.pageScrollTo({ scrollTop: 999999, duration: 200 })
    }
  },
  insertEmoji(e) {
    const emoji = e.currentTarget.dataset.emoji || ''
    this.setData({
      commentContent: this.data.commentContent + emoji,
      showEmojiPanel: false,
      inputFocus: true
    })
  },
  chooseImage() {
    wx.chooseImage({
      count: 9 - this.data.commentImages.length,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        this.setData({ commentImages: this.data.commentImages.concat(res.tempFilePaths) })
      }
    })
  },
  deleteImage(e) {
    const idx = e.currentTarget.dataset.index
    const arr = this.data.commentImages.slice()
    arr.splice(idx, 1)
    this.setData({ commentImages: arr })
  },
  startReply(e) {
    const id = e.currentTarget.dataset.id
    const nickname = e.currentTarget.dataset.nickname
    this.setData({ replyToCommentId: id, replyToNickname: nickname, inputFocus: true, showEmojiPanel: false })
    wx.pageScrollTo({ scrollTop: 999999, duration: 200 })
  },
  cancelReply() {
    this.setData({ replyToCommentId: null, replyToNickname: '', inputFocus: false })
  },

  sendComment() {
    if (!this.data.commentContent.trim()) {
      wx.showToast({ title: '请输入内容', icon: 'none' })
      return
    }
    const user = wx.getStorageSync('user')
    if (!user) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      return
    }

    const doSend = (urls) => {
      wx.showLoading({ title: '发送中' })
      app.request({
        url: '/Forum/addPostComment',
        method: 'POST',
        data: {
          postId: this.data.post.postId,
          userId: user.userId,
          content: this.data.commentContent,
          status: 1,
          images: (urls || []).map(u => ({ url: u })),
          parentId: this.data.replyToCommentId || null
        }
      }).then(res => {
        wx.hideLoading()
        if (res.code === '200') {
          wx.showToast({ title: '评论成功' })
          this.setData({ commentContent: '', commentImages: [], replyToCommentId: null, replyToNickname: '' })
          this.loadDetail(this.data.post.postId)
        } else {
          wx.showToast({ title: '评论失败', icon: 'none' })
        }
      }).catch(() => {
        wx.hideLoading()
        wx.showToast({ title: '网络错误', icon: 'none' })
      })
    }
    if (this.data.commentImages.length > 0) {
      const baseUrl = app.globalData.baseUrl
      const token = app.globalData.token || wx.getStorageSync('token') || ''
      const uploadOne = (filePath) => new Promise((resolve, reject) => {
        wx.uploadFile({
          url: `${baseUrl}/files/upload`, filePath, name: 'file',
          header: token ? { Authorization: 'Bearer ' + token } : {},
          formData: { folder: 'comment' },
          success: (res) => {
            try {
              const data = JSON.parse(res.data)
              if (data.code === '200') resolve(data.data); else reject(data.msg || '上传失败')
            } catch(e) { reject('解析失败') }
          }, fail: reject
        })
      })
      Promise.all(this.data.commentImages.map(uploadOne)).then(doSend).catch(() => {
        wx.hideLoading(); wx.showToast({ title: '图片上传失败', icon: 'none' })
      })
    } else {
      doSend([])
    }
  },

  previewImage(e) {
    const current = e.currentTarget.dataset.current
    const urlsData = e.currentTarget.dataset.urls
    let urls = []
    if (Array.isArray(urlsData) && urlsData.length > 0) {
      urls = urlsData.map(item => item.url || item.filePath || item)
    } else if (this.data.post && Array.isArray(this.data.post.images)) {
      urls = this.data.post.images.map(img => img.url)
    } else if (current) {
      urls = [current]
    }
    wx.previewImage({
      current,
      urls
    })
  }
})
