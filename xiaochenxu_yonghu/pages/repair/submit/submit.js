const app = getApp()

Page({
  data: {
    categories: [],
    categoryIndex: null,
    description: '',
    phone: '',
    buildingNo: '',
    unitNo: '',
    roomNo: '',
    files: [],
    videoPreviewVisible: false,
    videoPreviewSrc: '',
    priority: 'normal', // 优先级，默认为普通
    date: '',
    time: '',
    dateText: '',
    startDate: '', // 可选日期开始
    endDate: '' // 可选日期结束
  },

  onLoad() {
    const user = wx.getStorageSync('user')
    const buildingNo = user?.buildingNo || '1'
    const unitNo = user?.unitNo || '1'
    const roomNo = user?.roomNo || '101'
    this.setData({
      phone: user?.phone || '',
      buildingNo,
      unitNo,
      roomNo
    })
    
    const now = new Date()
    const endDate = new Date()
    endDate.setDate(endDate.getDate() + 30)
    
    const formatDate = (date) => {
      const year = date.getFullYear()
      const month = (date.getMonth() + 1).toString().padStart(2, '0')
      const day = date.getDate().toString().padStart(2, '0')
      return `${year}-${month}-${day}`
    }

    const formatTime = (date) => {
      const hours = date.getHours().toString().padStart(2, '0')
      const minutes = date.getMinutes().toString().padStart(2, '0')
      return `${hours}:${minutes}`
    }
    
    const initialDate = formatDate(now)
    const initialTime = formatTime(now)

    this.setData({
      date: initialDate,
      time: initialTime,
      dateText: `${initialDate} ${initialTime}`,
      startDate: formatDate(now),
      endDate: formatDate(endDate)
    })
  },

  onShow() {
    this.loadCategories()
  },

  chooseMedia() {
    if (this.data.files.length >= 3) {
      wx.showToast({ title: '最多上传3个', icon: 'none' })
      return
    }
    wx.showActionSheet({
      itemList: ['选图片', '选视频'],
      success: (res) => {
        if (res.tapIndex === 0) {
          this.chooseImages()
        } else if (res.tapIndex === 1) {
          this.chooseVideo()
        }
      }
    })
  },

  chooseImages() {
    wx.chooseImage({
      count: 3 - this.data.files.length,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const paths = res.tempFilePaths || []
        if (!paths.length) {
          return
        }
        const newFiles = paths.map(path => ({ path, type: 'image' }))
        this.setData({ files: this.data.files.concat(newFiles) })
      }
    })
  },

  chooseVideo() {
    if (this.data.files.length >= 3) {
      wx.showToast({ title: '最多上传3个', icon: 'none' })
      return
    }
    wx.chooseVideo({
      sourceType: ['album', 'camera'],
      compressed: true,
      maxDuration: 30,
      success: (res) => {
        if (!res.tempFilePath) {
          return
        }
        wx.showLoading({ title: '转码中' })
        wx.compressVideo({
          src: res.tempFilePath,
          quality: 'medium',
          success: (comp) => {
            const path = comp.tempFilePath || res.tempFilePath
            const newFile = { path, type: 'video' }
            this.setData({ files: this.data.files.concat([newFile]) })
          },
          fail: () => {
            const newFile = { path: res.tempFilePath, type: 'video' }
            this.setData({ files: this.data.files.concat([newFile]) })
          },
          complete: () => {
            wx.hideLoading()
          }
        })
      }
    })
  },

  previewFile(e) {
    const { src, type } = e.currentTarget.dataset
    if (type === 'video') {
      this.setData({
        videoPreviewVisible: true,
        videoPreviewSrc: src
      })
      return
    }
    const imageUrls = this.data.files
      .filter(f => f.type !== 'video')
      .map(f => f.path)
    wx.previewImage({
      current: src,
      urls: imageUrls
    })
  },

  closeVideoPreview() {
    this.setData({
      videoPreviewVisible: false,
      videoPreviewSrc: ''
    })
  },

  stopTap() {},

  deleteFile(e) {
    const index = e.currentTarget.dataset.index
    const files = this.data.files
    files.splice(index, 1)
    this.setData({ files })
  },

  uploadSingleFile(file) {
    return new Promise((resolve, reject) => {
      const rawBaseUrl = wx.getStorageSync('baseUrl') || app.globalData.baseUrl || ''
      const baseUrl = rawBaseUrl.replace(/\/$/, '')
      const storedUser = wx.getStorageSync('user') || {}
      const token = app.globalData.token || wx.getStorageSync('token') || storedUser.token || ''
      const authHeader = token ? (token.startsWith('Bearer ') ? token : `Bearer ${token}`) : ''
      if (!authHeader) {
        reject('请先登录')
        return
      }
      const headers = { Authorization: authHeader }
      wx.uploadFile({
        url: `${baseUrl}/files/upload`,
        filePath: file.path,
        name: 'file',
        header: headers,
        formData: { folder: 'repair' },
        timeout: 30000,
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
              reject('响应解析失败')
            }
          } else {
            reject(`上传失败(${res.statusCode})`)
          }
        },
        fail: (err) => {
          reject(err && err.errMsg ? err.errMsg : '上传失败')
        }
      })
    })
  },

  loadCategories() {
    app.request({
      url: '/repair/categories',
      method: 'GET'
    }).then(res => {
      if (res.code === '200') {
        this.setData({ categories: res.data })
      }
    })
  },

  bindCategoryChange(e) {
    this.setData({ categoryIndex: e.detail.value })
  },

  bindDescriptionInput(e) {
    this.setData({ description: e.detail.value })
  },

  bindPhoneInput(e) {
    this.setData({ phone: e.detail.value })
  },

  bindBuildingInput(e) {
    this.setData({ buildingNo: e.detail.value })
  },

  bindUnitInput(e) {
    this.setData({ unitNo: e.detail.value })
  },

  bindRoomInput(e) {
    this.setData({ roomNo: e.detail.value })
  },

  // 处理优先级选择
  bindPriorityChange(e) {
    this.setData({ priority: e.detail.value })
  },

  bindDateChange(e) {
    const date = e.detail.value
    const time = this.data.time || '00:00'
    this.setData({
      date,
      dateText: `${date} ${time}`
    })
  },

  bindTimeChange(e) {
    const time = e.detail.value
    const date = this.data.date || ''
    this.setData({
      time,
      dateText: date ? `${date} ${time}` : time
    })
  },

  submit() {
    // 检查表单数据
    if (this.data.categoryIndex === null) {
      wx.showToast({ title: '请选择类别', icon: 'none' })
      return
    }
    if (!this.data.description) {
      wx.showToast({ title: '请填写描述', icon: 'none' })
      return
    }
    if (!this.data.date || !this.data.time) {
      wx.showToast({ title: '请选择预约上门时间', icon: 'none' })
      return
    }
    if (!this.data.phone) {
      wx.showToast({ title: '请填写联系电话', icon: 'none' })
      return
    }
    wx.showLoading({ title: '提交中' })

    const uploadPromises = this.data.files.map(file => {
      return this.uploadSingleFile(file)
    })

    Promise.all(uploadPromises).then(urls => {
      this.submitOrder(urls)
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({ title: err || '附件上传失败', icon: 'none' })
      console.error(err)
    })
  },

  submitOrder(fileUrls) {
    const user = wx.getStorageSync('user')
    
    // 检查用户信息是否存在
    if (!user || !user.userId) {
      wx.hideLoading()
      wx.showToast({ title: '请先登录', icon: 'none' })
      setTimeout(() => {
        wx.navigateTo({ url: '/pages/login/login' })
      }, 1000)
      return
    }
    
    const data = {
      userId: user.userId,
      categoryId: this.data.categories[this.data.categoryIndex].categoryId,
      description: this.data.description,
      phone: this.data.phone,
      buildingNo: parseInt(this.data.buildingNo) || 0,
      unitNo: parseInt(this.data.unitNo) || 0,
      roomNo: parseInt(this.data.roomNo) || 0,
      priority: this.data.priority,
      appointmentTime: `${this.data.date} ${this.data.time}:00`,
      status: '待处理',
      fileUrls: fileUrls // Pass URLs to backend
    }

    app.request({
      url: '/repair/submit',
      method: 'POST',
      data: data
    }).then(res => {
      wx.hideLoading()
      if (res.code === '200') {
        wx.showToast({ title: '提交成功' })
        setTimeout(() => {
          wx.navigateBack()
        }, 1500)
      } else {
        wx.showToast({ title: res.msg || '提交失败', icon: 'none' })
      }
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({ title: '网络异常', icon: 'none' })
      console.error('提交失败:', err)
    })
  }
})
