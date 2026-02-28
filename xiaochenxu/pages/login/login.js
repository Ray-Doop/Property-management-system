const app = getApp()

Page({
  data: {
    username: '',
    password: '',
    captchaCode: '',
    captchaId: '',
    captchaImage: ''
  },

  onLoad() {
    this.refreshCaptcha()
  },

  refreshCaptcha() {
    app.request({
      url: '/LoginRegister/captcha',
      method: 'GET'
    }).then(res => {
      if (res.code === '200') {
        this.setData({
          captchaId: res.data.captchaId,
          captchaImage: res.data.imageBase64
        })
      }
    }).catch(err => {
      console.error('获取验证码失败', err)
    })
  },

  handleLogin() {
    if (!this.data.username || !this.data.password || !this.data.captchaCode) {
      wx.showToast({ title: '请填写完整信息', icon: 'none' })
      return
    }

    wx.showLoading({ title: '登录中...' })

    app.request({
      url: '/LoginRegister/userLogin',
      method: 'POST',
      data: {
        username: this.data.username,
        password: this.data.password,
        captchaId: this.data.captchaId,
        captchaCode: this.data.captchaCode
      }
    }).then(res => {
      wx.hideLoading()
      if (res.code === '200') {
        app.setToken(res.data.token)
        wx.setStorageSync('user', res.data)
        wx.showToast({ title: '登录成功' })
        setTimeout(() => {
          wx.reLaunch({ url: '/pages/index/index' })
        }, 1500)
      } else {
        wx.showToast({ title: res.msg || '登录失败', icon: 'none' })
        this.refreshCaptcha()
      }
    }).catch(err => {
      wx.hideLoading()
      console.error('登录请求失败', err)
      wx.showToast({ title: '请求失败', icon: 'none' })
    })
  },

  handleWeChatLogin() {
    wx.login({
      success: (res) => {
        if (res.code) {
          const cachedOpenid = wx.getStorageSync('wechatOpenid')
          wx.showLoading({ title: '一键登录中' })
          app.request({
            url: '/LoginRegister/wechatLogin',
            method: 'POST',
            data: {
              code: res.code,
              openid: cachedOpenid || ''
            }
          }).then(loginRes => {
            wx.hideLoading()
            if (loginRes.code === '200') {
              const data = loginRes.data
              if (data.needRegister) {
                if (data.openid) {
                  wx.setStorageSync('wechatOpenid', data.openid)
                }
                // 需要注册，跳转注册页并带上 openid
                wx.showModal({
                  title: '提示',
                  content: '您是首次登录，请完善注册信息',
                  showCancel: false,
                  success: () => {
                    wx.navigateTo({
                      url: `/pages/register/register?openid=${data.openid}`
                    })
                  }
                })
              } else {
                // 登录成功
                app.setToken(data.token)
                wx.setStorageSync('user', data)
                if (cachedOpenid) {
                  wx.setStorageSync('wechatOpenid', cachedOpenid)
                }
                wx.showToast({ title: '登录成功' })
                setTimeout(() => {
                  wx.reLaunch({ url: '/pages/index/index' })
                }, 1500)
              }
            } else if (loginRes.code === '403') {
              // 账号审核中或被禁用
              wx.showModal({
                title: '提示',
                content: loginRes.msg || '账号状态异常',
                showCancel: false
              })
            } else {
              wx.showToast({ title: loginRes.msg || '登录失败', icon: 'none' })
            }
          }).catch(err => {
            wx.hideLoading()
            wx.showToast({ title: '服务异常', icon: 'none' })
          })
        } else {
          wx.showToast({ title: '获取登录凭证失败', icon: 'none' })
        }
      }
    })
  },

  goToRegister() {
    wx.navigateTo({ url: '/pages/register/register' })
  }
})
