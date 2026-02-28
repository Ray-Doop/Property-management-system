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
      defaultAvatar: withAuthUrl(`${app.globalData.baseUrl}/files/download/img.jpg`)
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
        post.avatarUrl = withAuthUrl(post.avatarUrl)
        post.createdTime = formatTimeRelative(post.createdTime)
        if (Array.isArray(post.images)) {
          post.images = post.images.map(img => {
            if (typeof img === 'string') {
              return { url: withAuthUrl(img) }
            }
            return {
              ...img,
              url: withAuthUrl(img.url)
            }
          })
        }
        if (Array.isArray(post.comments)) {
          post.comments = post.comments.map(comment => {
            const next = { ...comment }
            next.avatarUrl = withAuthUrl(next.avatarUrl)
            next.createdTime = formatTimeRelative(next.createdTime)
            if (Array.isArray(next.images)) {
              next.images = next.images.map(img => {
                if (typeof img === 'string') return withAuthUrl(img)
                return withAuthUrl(img.url || img)
              })
            }
            if (Array.isArray(next.attachments)) {
              next.attachments = next.attachments.map(att => ({
                ...att,
                url: withAuthUrl(att.url || att.filePath)
              }))
            }
            if (Array.isArray(next.replies)) {
              next.replies = next.replies.map(reply => {
                const r = { ...reply }
                r.avatarUrl = withAuthUrl(r.avatarUrl)
                r.createdTime = formatTimeRelative(r.createdTime)
                if (Array.isArray(r.images)) {
                  r.images = r.images.map(img => {
                    if (typeof img === 'string') return withAuthUrl(img)
                    return withAuthUrl(img.url || img)
                  })
                }
                if (Array.isArray(r.attachments)) {
                  r.attachments = r.attachments.map(att => ({
                    ...att,
                    url: withAuthUrl(att.url || att.filePath)
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
    }).catch(() => {
      wx.hideLoading()
    })
  },

  checkCollected(postId) {
    const employee = wx.getStorageSync('employee')
    if (!employee) return
    app.request({
      url: '/Forum/isCollected',
      method: 'GET',
      data: { postId, userId: employee.employeeId || employee.id || 0 }
    }).then(res => {
      if (res.code === '200') {
        this.setData({ isCollected: res.data })
      }
    })
  },

  toggleCollect() {
    const employee = wx.getStorageSync('employee')
    if (!employee) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      return
    }
    const url = this.data.isCollected ? '/Forum/delCancelCollect' : '/Forum/addToCollection'
    const data = {
      postId: this.data.post.postId,
      userId: employee.employeeId || employee.id || 0
    }
    app.request({
      url,
      method: this.data.isCollected ? 'DELETE' : 'POST',
      data
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
    const employee = wx.getStorageSync('employee')
    if (!employee) {
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
          userId: employee.employeeId || employee.id || 0,
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
        wx.hideLoading()
        wx.showToast({ title: '图片上传失败', icon: 'none' })
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
  },

  toggleLike(e) {
    const id = e.currentTarget.dataset.id
    const employee = wx.getStorageSync('employee')
    if (!employee) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      return
    }
    app.request({
      url: '/Forum/likeComment',
      method: 'POST',
      data: { commentId: id, userId: employee.employeeId || employee.id || 0 }
    }).then(res => {
      if (res.code === '200') {
        const count = res.data.likeCount
        const comments = (this.data.post.comments || []).map(c => {
          if (c.commentId === id) { c.likeCount = count }
          return c
        })
        this.setData({ 'post.comments': comments })
      }
    })
  }
})
