const app = getApp()
const util = require('../../../../utils/util.js')

Page({
  data: {
    record: null,
    qrCode: '',
    timer: null,
    expireTimeStr: ''
  },

  onLoad(options) {
    const eventChannel = this.getOpenerEventChannel()
    if (eventChannel && eventChannel.on) {
      eventChannel.on('acceptDataFromOpenerPage', (data) => {
        this.setData({ 
          record: data,
          expireTimeStr: util.formatTime(new Date(data.expireTime))
        })
        this.checkAndShowQR()
      })
    } else if (options.id) {
        // Fallback if not opened from list (e.g. share), but we don't have getDetail API yet.
        // For now assume opened from list.
    }
  },

  onUnload() {
    if (this.data.timer) clearInterval(this.data.timer)
  },

  checkAndShowQR() {
    const record = this.data.record
    if (!record) return

    const now = new Date().getTime()
    const expire = new Date(record.expireTime).getTime()
    const isExpired = now > expire
    const canShow = (record.status === 'ISSUED' || record.status === 'ENTERED') && !isExpired

    if (canShow) {
      this.refreshCode()
      // Auto refresh
      this.setData({
        timer: setInterval(() => {
          this.refreshCode()
        }, 60000)
      })
    }
  },

  refreshCode() {
    app.request({
      url: '/travel-pass/refresh',
      method: 'GET',
      data: { recordId: this.data.record.id }
    }).then(res => {
      if (res.code === '200' || res.qrCode) {
        const data = res.qrCode ? res : res.data
        this.setData({
          qrCode: data.qrCode
        })
      } else {
        // failed (expired etc), stop timer
        if (this.data.timer) clearInterval(this.data.timer)
        this.setData({ qrCode: '' })
      }
    }).catch(err => {
      console.error(err)
    })
  }
})
