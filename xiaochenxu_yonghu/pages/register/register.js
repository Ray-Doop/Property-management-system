const app = getApp()

Page({
  data: {
    password: '',
    confirmPassword: '',
    nickname: '',
    phone: '',
    buildingNo: '',
    unitNo: '',
    roomNo: '',
    area: '',
    vehicleInfo: '',
    openid: '' // 存储微信一键登录带来的 openid
  },

  onLoad(options) {
    if (options.openid) {
      this.setData({ 
        openid: options.openid
      })
      wx.showToast({ title: '请完善信息完成绑定', icon: 'none', duration: 2000 })
    }
  },

  handleRegister() {
    // 校验必填
    const { password, confirmPassword, nickname, phone, buildingNo, unitNo, roomNo, area, openid } = this.data
    if (!password || !confirmPassword || !nickname || !phone || !buildingNo || !unitNo || !roomNo || !area) {
      wx.showToast({ title: '请填写完整必填信息', icon: 'none' })
      return
    }

    // 校验密码一致性
    if (password !== confirmPassword) {
      wx.showToast({ title: '两次输入的密码不一致', icon: 'none' })
      return
    }

    // 校验手机号
    if (!/^1\d{10}$/.test(phone)) {
      wx.showToast({ title: '手机号格式不正确', icon: 'none' })
      return
    }

    wx.showLoading({ title: '提交中...' })

    const userData = {
      username: openid || phone, // 如果有openid则使用openid，否则使用手机号
      password,
      nickname,
      phone,
      buildingNo: parseInt(buildingNo),
      unitNo: parseInt(unitNo),
      roomNo: parseInt(roomNo),
      area: parseInt(area),
      vehicleInfo: this.data.vehicleInfo || '',
      status: 0, // 0-未激活/待审核
      remark: ''
    }

    app.request({
      url: '/LoginRegister/userRegister',
      method: 'POST',
      data: userData
    }).then(res => {
      wx.hideLoading()
      if (res.code === '200') {
        wx.showModal({
          title: '注册成功',
          content: '您的账号已提交审核，请等待管理员通过后登录。',
          showCancel: false,
          success: () => {
            wx.navigateBack()
          }
        })
      } else {
        wx.showToast({ title: res.msg || '注册失败', icon: 'none' })
      }
    }).catch(err => {
      wx.hideLoading()
      console.error('注册请求失败', err)
      wx.showToast({ title: '请求失败', icon: 'none' })
    })
  }
})
