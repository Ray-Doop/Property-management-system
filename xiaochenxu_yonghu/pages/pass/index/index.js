const app = getApp()
const util = require('../../../utils/util.js') // Assuming util.js exists for date formatting

Page({
  data: {
    user: null,
    qrCode: '',
    recordId: null,
    expireTimeStr: '',
    timer: null,
    showModal: false,
    inputPlate: '',
    defaultAvatar: '/assets/default-avatar.png',
    avatarSrc: '/assets/default-avatar.png'
  },

  onShow() {
    const user = wx.getStorageSync('user')
    if (user) {
      const rawAvatar = (user.avatarUrl || '').trim()
      let fullAvatar = rawAvatar
      if (rawAvatar && !/^https?:\/\//i.test(rawAvatar)) {
        fullAvatar = rawAvatar.startsWith('/') ? `${app.globalData.baseUrl}${rawAvatar}` : `${app.globalData.baseUrl}/${rawAvatar}`
      }
      const avatarUrl = app.withAuthUrl(fullAvatar)
      const invalidAvatar = !avatarUrl || avatarUrl === 'null' || avatarUrl === 'undefined' || avatarUrl.startsWith('http://')
      user.avatarUrl = invalidAvatar ? '' : avatarUrl
      const fallback = this.data.defaultAvatar || '/assets/default-avatar.png'
      this.setData({ user, avatarSrc: user.avatarUrl || fallback, defaultAvatar: fallback })
      // If we already have a valid code, don't regenerate immediately?
      // But the requirement says "dynamic", so maybe we should just refresh if valid.
      // For now, keep generating on show as before.
      this.generateCode()
    } else {
      wx.showToast({ title: '请先登录', icon: 'none' })
    }
    const fallback = this.data.defaultAvatar || '/assets/default-avatar.png'
    this.setData({ defaultAvatar: fallback, avatarSrc: this.data.avatarSrc || fallback })
  },

  onHide() {
    this.clearTimer()
  },

  onUnload() {
    this.clearTimer()
  },

  clearTimer() {
    if (this.data.timer) {
      clearInterval(this.data.timer)
      this.setData({ timer: null })
    }
  },
  
  showApplyModal() {
    this.setData({ showModal: true, inputPlate: this.data.user.vehicleInfo || '' })
  },
  
  hideModal() {
    this.setData({ showModal: false })
  },
  
  preventBubble() {},
  
  submitApply() {
    this.hideModal()
    const user = this.data.user
    const plate = this.data.inputPlate
    this.doGenerate(user, !!plate, plate)
  },

  generateCode() {
    const user = this.data.user
    this.doGenerate(user, !!user.vehicleInfo, user.vehicleInfo || '')
  },
  
  doGenerate(user, hasVehicle, plateNumber) {
    wx.showLoading({ title: '生成中...' })
    app.request({
      url: '/travel-pass/issue',
      method: 'GET',
      data: {
        username: user.username,
        duration: 120,
        hasVehicle: hasVehicle,
        plateNumber: plateNumber,
        paid: true // Assuming resident fees are paid or not checked strictly here
      }
    }).then(res => {
      wx.hideLoading()
      if (res.code === '200' || (res.qrCode)) {
        const data = res.qrCode ? res : res.data
        
        this.setData({
          qrCode: data.qrCode,
          recordId: data.recordId,
          expireTimeStr: new Date(data.expireAt).toLocaleString()
        })
        
        // Update user vehicle info locally if changed?
        if (plateNumber && plateNumber !== user.vehicleInfo) {
           user.vehicleInfo = plateNumber
           this.setData({ user })
           wx.setStorageSync('user', user)
        }

        // Auto refresh every 30 seconds
        this.clearTimer()
        this.setData({
          timer: setInterval(() => {
            this.refreshCode()
          }, 30000)
        })
      }
    }).catch(err => {
      wx.hideLoading()
      console.error(err)
      wx.showToast({ title: '生成失败', icon: 'none' })
    })
  },

  onShareAppMessage() {
    return {
      title: '我的小区出行码',
      path: '/pages/pass/index/index',
      imageUrl: '/assets/qr-placeholder.png' // Or screenshot
    }
  },

  goToHistory() {
    wx.navigateTo({ url: '/pages/pass/record/list/list' })
  },

  refreshCode() {
    if (!this.data.recordId) return
    app.request({
      url: '/travel-pass/refresh',
      method: 'GET',
      data: { recordId: this.data.recordId }
    }).then(res => {
      // Similarly handle response
      const data = res.qrCode ? res : res.data
      if (data && data.qrCode) {
        this.setData({
          qrCode: data.qrCode,
          expireTimeStr: new Date(data.expireAt).toLocaleString()
        })
      }
    })
  },
  handleAvatarError() {
    if (this.data.avatarSrc !== this.data.defaultAvatar) {
      this.setData({ avatarSrc: this.data.defaultAvatar })
    }
  }
})
