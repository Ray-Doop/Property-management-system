const app = getApp()

Page({
  data: {
    title: '',
    content: '',
    images: [],
    submitting: false
  },
  chooseImages() {
    const remain = 9 - this.data.images.length
    if (remain <= 0) {
      wx.showToast({ title: '最多选择9张', icon: 'none' })
      return
    }
    wx.chooseImage({
      count: remain,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const paths = res.tempFilePaths || []
        if (!paths.length) return
        this.setData({ images: this.data.images.concat(paths) })
      }
    })
  },
  deleteImage(e) {
    const index = e.currentTarget.dataset.index
    const images = this.data.images.slice()
    images.splice(index, 1)
    this.setData({ images })
  },
  previewImage(e) {
    const current = e.currentTarget.dataset.current
    wx.previewImage({
      current,
      urls: this.data.images
    })
  },
  uploadImages() {
    const baseUrl = app.globalData.baseUrl
    const token = app.globalData.token || wx.getStorageSync('token') || ''
    const uploadOne = (filePath) => new Promise((resolve, reject) => {
      wx.uploadFile({
        url: `${baseUrl}/files/upload`,
        filePath,
        name: 'file',
        header: token ? { Authorization: 'Bearer ' + token } : {},
        formData: { folder: 'notice' },
        success: (res) => {
          try {
            if (res.statusCode !== 200) {
              reject('上传失败')
              return
            }
            const data = JSON.parse(res.data)
            if (data.code === '200') {
              resolve(data.data)
            } else {
              reject(data.msg || '上传失败')
            }
          } catch (e) {
            reject('解析失败')
          }
        },
        fail: reject
      })
    })
    return Promise.all(this.data.images.map(uploadOne))
  },

  handleSubmit() {
    if (!this.data.title || !this.data.content) {
      wx.showToast({ title: '请填写完整', icon: 'none' })
      return
    }
    if (this.data.submitting) return
    this.setData({ submitting: true })

    wx.showLoading({ title: '发布中...' })
    const doPublish = (urls) => {
      const attachments = (urls || []).map(url => ({ fileUrl: url }))
      app.request({
        url: '/notice/publish',
        method: 'POST',
        data: {
          title: this.data.title,
          content: this.data.content,
          publisherType: 'EMPLOYEE',
          publisherId: app.globalData.employee ? app.globalData.employee.id : 0,
          publisherName: app.globalData.employee ? app.globalData.employee.name : '员工',
          attachments
        }
      }).then(res => {
        wx.hideLoading()
        this.setData({ submitting: false })
        if (res.code === '200') {
          wx.showToast({ title: '已提交审核', icon: 'success' })
          setTimeout(() => {
            wx.navigateBack()
          }, 1500)
        } else {
          wx.showToast({ title: res.msg || '发布失败', icon: 'none' })
        }
      }).catch(err => {
        wx.hideLoading()
        this.setData({ submitting: false })
        console.error(err)
        wx.showToast({ title: '网络错误', icon: 'none' })
      })
    }
    if (this.data.images.length > 0) {
      this.uploadImages().then(doPublish).catch(() => {
        wx.hideLoading()
        this.setData({ submitting: false })
        wx.showToast({ title: '图片上传失败', icon: 'none' })
      })
    } else {
      doPublish([])
    }
  }
})
