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

    const employee = wx.getStorageSync('employee')
    if (!employee) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      return
    }

    wx.showLoading({ title: '发布中' })

    if (this.data.images.length > 0) {
      this.uploadImages().then(urls => {
        this.submitPost(employee, urls)
      }).catch(() => {
        wx.hideLoading()
        wx.showToast({ title: '图片上传失败', icon: 'none' })
      })
    } else {
      this.submitPost(employee, [])
    }
  },

  uploadImages() {
    const promises = this.data.images.map(path => {
      return new Promise((resolve, reject) => {
        const baseUrl = app.globalData.baseUrl
        const token = app.globalData.token || wx.getStorageSync('token') || ''
        wx.uploadFile({
          url: `${baseUrl}/files/upload`,
          filePath: path,
          name: 'file',
          header: token ? { Authorization: 'Bearer ' + token } : {},
          formData: { folder: 'post' },
          success: (res) => {
            if (res.statusCode === 200) {
              try {
                const data = JSON.parse(res.data)
                if (data.code === '200') {
                  resolve(data.data)
                } else {
                  reject(data.msg || '上传失败')
                }
              } catch (e) {
                reject('解析失败')
              }
            } else {
              reject('上传失败')
            }
          },
          fail: reject
        })
      })
    })
    return Promise.all(promises)
  },

  submitPost(employee, imageUrls) {
    const postData = {
      userId: employee.employeeId || employee.id || 0,
      nickname: employee.name || employee.username || '员工',
      title: this.data.title,
      content: this.data.content,
      sectionId: this.data.selectedSectionId,
      status: 1,
      images: imageUrls.map(url => ({ url }))
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
    }).catch(() => {
      wx.hideLoading()
    })
  }
})
