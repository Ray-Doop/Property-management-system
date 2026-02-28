const app = getApp()

Page({
  data: {
    order: null,
    rating: 5,
    evaluation: '',
    videoPreviewVisible: false,
    videoPreviewSrc: ''
  },

  onLoad(options) {
    if (options.id) {
      this.loadDetail(options.id)
    }
  },

  setRating(e) {
    this.setData({ rating: e.currentTarget.dataset.score })
  },

  bindEvalInput(e) {
    this.setData({ evaluation: e.detail.value })
  },

  previewImage(e) {
    const current = e.currentTarget.dataset.src
    const urls = this.data.order.files
      .filter(f => f.fileType !== 'video')
      .map(f => f.fileUrl)
    wx.previewImage({
      current,
      urls
    })
  },

  previewVideo(e) {
    const current = e.currentTarget.dataset.src
    if (!current) {
      return
    }
    this.setData({
      videoPreviewVisible: true,
      videoPreviewSrc: current
    })
  },

  closeVideoPreview() {
    this.setData({
      videoPreviewVisible: false,
      videoPreviewSrc: ''
    })
  },

  stopTap() {},

  submitEvaluation() {
    if (!this.data.evaluation) {
      wx.showToast({ title: '请输入评价内容', icon: 'none' })
      return
    }

    wx.showLoading({ title: '提交中' })
    app.request({
      url: '/repair/evaluate',
      method: 'POST',
      data: {
        orderId: this.data.order.orderId,
        evaluation: this.data.evaluation,
        rating: this.data.rating
      }
    }).then(res => {
      wx.hideLoading()
      if (res.code === '200') {
        wx.showToast({ title: '评价成功' })
        this.loadDetail(this.data.order.orderId)
      } else {
        wx.showToast({ title: '评价失败', icon: 'none' })
      }
    }).catch(err => {
      wx.hideLoading()
      console.error(err)
    })
  },

  loadDetail(id) {
    wx.showLoading({ title: '加载中' })
    app.request({
      url: `/repair/detail/${id}`,
      method: 'GET'
    }).then(res => {
      wx.hideLoading()
      if (res.code === '200') {
        const order = { ...res.data }
        if (Array.isArray(order.files)) {
          order.files = order.files.map(f => ({
            ...f,
            fileUrl: app.withAuthUrl(f.fileUrl)
          }))
        }
        this.setData({ order })
      } else {
        wx.showToast({ title: '加载失败', icon: 'none' })
      }
    }).catch(err => {
      wx.hideLoading()
      console.error(err)
    })
  },

  cancelOrder() {
    wx.showModal({
      title: '提示',
      content: '确定要取消该报修单吗？',
      success: (res) => {
        if (res.confirm) {
          app.request({
            url: `/repair/cancel/${this.data.order.orderId}`,
            method: 'POST'
          }).then(res => {
            if (res.code === '200') {
              wx.showToast({ title: '已取消' })
              this.loadDetail(this.data.order.orderId)
            } else {
              wx.showToast({ title: '取消失败', icon: 'none' })
            }
          })
        }
      }
    })
  }
})
