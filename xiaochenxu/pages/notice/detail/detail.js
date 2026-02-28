const app = getApp()

Page({
  data: {
    notice: null
  },

  onLoad(options) {
    if (options.id) {
      this.loadDetail(options.id)
    }
  },

  loadDetail(id) {
    wx.showLoading({ title: '加载中' })
    app.request({
      url: `/notice/${id}`,
      method: 'GET'
    }).then(res => {
      wx.hideLoading()
      if (res.code === '200') {
        const notice = { ...res.data }
        if (Array.isArray(notice.attachments)) {
          notice.attachments = notice.attachments.map(file => ({
            ...file,
            fileUrl: app.withAuthUrl(file.fileUrl)
          }))
        }
        this.setData({ notice })
        this.markAsRead(id)
      } else {
        wx.showToast({ title: '加载失败', icon: 'none' })
      }
    }).catch(err => {
      wx.hideLoading()
      console.error(err)
    })
  },

  markAsRead(noticeId) {
    const user = wx.getStorageSync('user')
    if (user && user.userId) {
      app.request({
        url: '/notice/read',
        method: 'POST',
        data: {
          noticeId: noticeId,
          userId: user.userId
        }
      }).then(res => {
        if (res.code === '200') {
          console.log('标记已读成功')
          // Optional: Update global event bus or previous page data if needed
        }
      })
    }
  },

  previewFile(e) {
    const url = app.withAuthUrl(e.currentTarget.dataset.url)
    const type = (e.currentTarget.dataset.type || '').toLowerCase()
    if (!url) {
      wx.showToast({ title: '附件地址无效', icon: 'none' })
      return
    }
    const isImage = type === 'image' || /\.(jpg|jpeg|png|gif|bmp|webp)$/i.test(url)
    const isVideo = type === 'video' || /\.(mp4|mov|avi|mkv|webm)$/i.test(url)
    if (isImage) {
      wx.previewImage({ urls: [url] })
      return
    }
    if (isVideo) {
      wx.previewMedia({
        sources: [{ url, type: 'video' }]
      })
      return
    }
    wx.showLoading({ title: '打开附件中' })
    wx.downloadFile({
      url,
      success: (res) => {
        if (res.statusCode !== 200) {
          wx.hideLoading()
          wx.showToast({ title: '下载失败', icon: 'none' })
          return
        }
        wx.openDocument({
          filePath: res.tempFilePath,
          success: () => {
            wx.hideLoading()
          },
          fail: () => {
            wx.hideLoading()
            wx.showToast({ title: '无法打开附件', icon: 'none' })
          }
        })
      },
      fail: () => {
        wx.hideLoading()
        wx.showToast({ title: '下载失败', icon: 'none' })
      }
    })
  }
})
