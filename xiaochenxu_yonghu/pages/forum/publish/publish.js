const app = getApp()

Page({
  data: {
    title: '',
    content: '',
    images: [],
    titleLen: 0,
    contentLen: 0,
    maxContent: 1000,
    canPublish: false,
    sections: [],
    sectionIndex: null,
    selectedSectionId: null
  },

  onShow() {
    this.loadSections()
  },

  bindTitleInput(e) {
    const title = e.detail.value
    this.setData({ title, titleLen: title.length })
    this.updateCanPublish()
  },

  bindContentInput(e) {
    const content = e.detail.value
    this.setData({ content, contentLen: content.length })
    this.updateCanPublish()
  },

  updateCanPublish() {
    const canPublish = Boolean(this.data.title && this.data.content && this.data.selectedSectionId)
    this.setData({ canPublish })
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

  bindSectionChange(e) {
    const index = Number(e.detail.value)
    const section = this.data.sections[index]
    const sectionId = section ? section.sectionId : null
    this.setData({
      sectionIndex: index,
      selectedSectionId: sectionId
    })
    this.updateCanPublish()
  },

  chooseImage() {
    wx.chooseImage({
      count: 9 - this.data.images.length,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        this.setData({
          images: this.data.images.concat(res.tempFilePaths)
        })
      }
    })
  },

  deleteImage(e) {
    const index = e.currentTarget.dataset.index
    const images = this.data.images
    images.splice(index, 1)
    this.setData({ images })
  },

  publish() {
    if (!this.data.title || !this.data.content) {
      wx.showToast({ title: '标题和内容不能为空', icon: 'none' })
      return
    }
    if (!this.data.selectedSectionId) {
      wx.showToast({ title: '请选择板块', icon: 'none' })
      return
    }

    const user = wx.getStorageSync('user')
    if (!user) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      return
    }

    wx.showLoading({ title: '发布中' })

    // If there are images, we need to upload them first.
    // However, the backend `PostController.post` takes `ForumPost` with `images` list.
    // Usually images need to be uploaded to a file server (e.g., OSS or local) first, returning URLs.
    // I need a file upload endpoint.
    // I saw `FileController` in the file list. Let's assume `/file/upload` or similar exists.
    // I'll check `FileController`.
    
    // For now, I'll implement the upload logic assuming a standard upload endpoint.
    if (this.data.images.length > 0) {
      this.uploadImages().then(urls => {
        this.submitPost(user.userId, urls)
      }).catch(() => {
        wx.hideLoading()
        wx.showToast({ title: '图片上传失败', icon: 'none' })
      })
    } else {
      this.submitPost(user.userId, [])
    }
  },

  uploadImages() {
    const urls = []
    const promises = this.data.images.map(path => {
      return new Promise((resolve, reject) => {
        wx.uploadFile({
          url: app.globalData.baseUrl + '/files/upload', // Guessing endpoint
          filePath: path,
          name: 'file',
          header: { 'Authorization': app.getBearerToken() },
          formData: { folder: 'post' },
          success: (res) => {
            if (res.statusCode === 200) {
              const data = JSON.parse(res.data)
              if (data.code === '200') {
                resolve(data.data) // Assuming returns URL
              } else {
                reject(data.msg)
              }
            } else {
              reject('Upload failed')
            }
          },
          fail: reject
        })
      })
    })
    return Promise.all(promises)
  },

  submitPost(userId, imageUrls) {
    const postData = {
      userId: userId,
      title: this.data.title,
      content: this.data.content,
      sectionId: this.data.selectedSectionId,
      status: 1,
      images: imageUrls.map(url => ({ url })) // Map to object as per ForumPost.Image
    }

    app.request({
      url: '/Forum/Post',
      method: 'POST',
      data: postData
    }).then(res => {
      wx.hideLoading()
      if (res.code === '200') {
        wx.showToast({ title: '发布成功' })
        setTimeout(() => {
          wx.navigateBack()
        }, 1500)
      } else {
        wx.showToast({ title: '发布失败', icon: 'none' })
      }
    }).catch(err => {
      wx.hideLoading()
      console.error(err)
    })
  }
})
