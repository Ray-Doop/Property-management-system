const app = getApp()

Page({
  data: {
    url: ''
  },

  onLoad(options) {
    if (options.billId && options.residenceId) {
      // Construct backend URL
      // Use the baseUrl from app.js but ensure it doesn't end with / if we append /api
      let baseUrl = app.globalData.baseUrl
      if (baseUrl.endsWith('/')) baseUrl = baseUrl.slice(0, -1)
      
      // Ensure parameters are encoded
      const encodedResidenceId = encodeURIComponent(options.residenceId)
      
      const url = `${baseUrl}/api/pay/page/alipay?billId=${options.billId}&residenceId=${encodedResidenceId}`
      console.log('Pay URL:', url)
      this.setData({ url })
    } else {
      wx.showToast({ title: '参数错误', icon: 'none' })
    }
  }
})
