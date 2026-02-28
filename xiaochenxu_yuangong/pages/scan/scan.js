const app = getApp()

Page({
  data: {
    code: '',
    result: null,
    loading: false
  },

  handleInput(e) {
    this.setData({ code: e.detail.value })
  },

  handleScan() {
    wx.scanCode({
      scanType: ['qrCode'],
      success: (res) => {
        const raw = res.result || ''
        let recordId = raw
        try {
          const parsed = JSON.parse(raw)
          if (parsed && parsed.recordId) {
            recordId = String(parsed.recordId)
          }
        } catch {}
        this.setData({ code: recordId })
        if (recordId) {
          this.handleVerify()
        }
      },
      fail: () => {
        wx.showToast({ title: '扫码失败', icon: 'none' })
      }
    })
  },

  handleVerify() {
    if (!this.data.code) {
      wx.showToast({ title: '请输入核销码', icon: 'none' })
      return
    }

    this.setData({ loading: true, result: null })
    app.request({
      url: '/travel-pass/verify',
      method: 'POST',
      data: { recordId: this.data.code }
    }).then(res => {
      this.setData({ loading: false, result: res })
      const title = res.valid ? '核验成功' : '核验失败'
      const content = res.message || res.error || (res.valid ? '允许通行' : '无效二维码或已过期')
      wx.showModal({ title, content, showCancel: false })
    }).catch(() => {
      this.setData({ loading: false })
      wx.showToast({ title: '网络错误', icon: 'none' })
    })
  }
})
