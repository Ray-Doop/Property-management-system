const app = getApp()

Page({
  data: {
    order: null,
    loading: true,
    images: [],
    uploading: false,
    evaluation: null,
    replyText: ''
  },

  onLoad(options) {
    if (options.id) {
      // 预设状态用于避免后端缓存造成的显示延迟
      if (options.status) {
        this.setData({ order: { ...(this.data.order || {}), orderId: Number(options.id), status: decodeURIComponent(options.status) } })
      }
      this.loadDetail(options.id)
    }
  },

  loadDetail(id) {
    app.request({
      url: `/repair/detail/${id}`
    }).then(res => {
      if (res.code === '200') {
      const expectedStatus = (this.data.order && this.data.order.status) ? this.data.order.status : ''
      const merged = expectedStatus ? { ...res.data, status: expectedStatus } : res.data
      this.setData({ order: merged, loading: false })
      if (merged.status === '已完成') {
        this.loadEvaluation(merged.orderId)
      }
      } else {
        wx.showToast({ title: '加载失败', icon: 'none' })
      }
    }).catch(err => {
      console.error(err)
      this.setData({ loading: false })
    })
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
        this.setData({ images: this.data.images.concat(res.tempFilePaths) })
      }
    })
  },

  removeImage(e) {
    const idx = e.currentTarget.dataset.index
    const arr = this.data.images.slice()
    arr.splice(idx, 1)
    this.setData({ images: arr })
  },

  handleAction(e) {
    const action = e.currentTarget.dataset.action
    // action: 'accept' (接单/开始维修), 'complete' (完成)
    let newStatus = ''
    let confirmContent = ''
    
    if (action === 'accept') {
      newStatus = '维修中'
      confirmContent = '确认接单并开始维修吗？'
    }

    wx.showModal({
      title: '确认操作',
      content: confirmContent,
      success: (res) => {
        if (res.confirm) {
          this.updateStatus(newStatus)
        }
      }
    })
  },

  updateStatus(status) {
    wx.showLoading({ title: '提交中...' })
    const employee = wx.getStorageSync('employee') || app.globalData.employee || {}
    const workerId = employee.employeeId || employee.id || 0
    const req = status === '维修中' 
      ? { url: '/repair/accept', method: 'POST', data: { orderId: this.data.order.orderId, workerId } }
      : { url: '/repair/updateStatus', method: 'POST', data: { orderId: this.data.order.orderId, status } }
    app.request(req).then(res => {
      wx.hideLoading()
      if (res.code === '200') {
        wx.showToast({ title: '操作成功' })
        // 本地更新状态，避免等待接口刷新
        this.setData({ 'order.status': status })
        // 重新拉取详情确保数据和附件最新
        this.loadDetail(this.data.order.orderId)
      } else {
        wx.showToast({ title: res.msg || '操作失败', icon: 'none' })
      }
    }).catch(err => {
      wx.hideLoading()
      wx.showToast({ title: '网络错误', icon: 'none' })
    })
  },

  // 上传所有选择的图片并完成维修
  async completeWithImages() {
    if (!this.data.order) return
    if (this.data.images.length === 0) {
      wx.showToast({ title: '请先选择图片', icon: 'none' })
      return
    }
    const baseUrl = app.globalData.baseUrl
    const token = app.globalData.token || wx.getStorageSync('token') || ''
    this.setData({ uploading: true })
    try {
      const uploadOne = (filePath) => new Promise((resolve, reject) => {
        wx.uploadFile({
          url: `${baseUrl}/files/upload`,
          name: 'file',
          filePath,
          header: token ? { Authorization: 'Bearer ' + token } : {},
          formData: { folder: 'repair' },
          success: (res) => {
            try {
              const data = JSON.parse(res.data)
              if (data.code === '200') {
                resolve(data.data) // 返回文件URL
              } else {
                reject(data.msg || '上传失败')
              }
            } catch (e) {
              reject('响应解析失败')
            }
          },
          fail: reject
        })
      })
      const urls = []
      for (const p of this.data.images) {
        const url = await uploadOne(p)
        urls.push(url)
      }
      // 调用完成接口，保存附件并置为已完成
      const employee = wx.getStorageSync('employee') || app.globalData.employee || {}
      await app.request({
        url: '/repair/complete',
        method: 'POST',
        data: {
          orderId: this.data.order.orderId,
          fileUrls: urls,
          uploaderId: employee.employeeId || employee.id || 0
        }
      })
      wx.showToast({ title: '已完成维修' })
      // 状态更新及清理
      this.setData({ images: [], uploading: false, 'order.status': '已完成' })
      // 重新加载详情
      setTimeout(() => {
        this.loadDetail(this.data.order.orderId)
      }, 1000)
    } catch (err) {
      console.error(err)
      this.setData({ uploading: false })
      wx.showToast({ title: '上传失败', icon: 'none' })
    }
  }
  ,
  loadEvaluation(orderId) {
    app.request({
      url: `/repair/evaluation/${orderId}`,
      method: 'GET'
    }).then(res => {
      if (res.code === '200') {
        this.setData({ evaluation: res.data || null })
      }
    })
  },
  submitReply() {
    if (!this.data.replyText) {
      wx.showToast({ title: '请输入回复内容', icon: 'none' })
      return
    }
    wx.showLoading({ title: '提交中...' })
    const evalId = this.data.evaluation && this.data.evaluation.evalId
    app.request({
      url: `/repair/evaluation/reply?evalId=${evalId}&replyContent=${encodeURIComponent(this.data.replyText)}`,
      method: 'POST'
    }).then(res => {
      wx.hideLoading()
      if (res.code === '200') {
        wx.showToast({ title: '已回复' })
        this.setData({ replyText: '' })
        this.loadEvaluation(this.data.order.orderId)
      } else {
        wx.showToast({ title: res.msg || '提交失败', icon: 'none' })
      }
    }).catch(() => {
      wx.hideLoading()
      wx.showToast({ title: '网络错误', icon: 'none' })
    })
  }
})
