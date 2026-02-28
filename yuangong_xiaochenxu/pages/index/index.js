const app = getApp()

Page({
  data: {
    employee: null,
    menuList: [
      { id: 'scan', name: '扫码核销', icon: '/assets/icons/scan.png', color: '#07c160' },
      { id: 'repair', name: '维修工单', icon: '/assets/icons/repair.png', color: '#1890ff' },
      { id: 'notice', name: '公告管理', icon: '/assets/icons/notice.png', color: '#faad14' },
      { id: 'forum', name: '业主论坛', icon: '/assets/icons/forum.png', color: '#722ed1' }
    ]
  },

  onShow() {
    const employee = wx.getStorageSync('employee')
    if (!employee) {
      wx.redirectTo({ url: '/pages/login/login' })
      return
    }
    this.setData({ employee })
  },

  handleMenuClick(e) {
    const id = e.currentTarget.dataset.id
    switch (id) {
      case 'scan':
        this.scanCode()
        break
      case 'repair':
        wx.navigateTo({ url: '/pages/repair/list/list' })
        break
      case 'notice':
        wx.navigateTo({ url: '/pages/notice/list/list' })
        break
      case 'forum':
        wx.navigateTo({ url: '/pages/forum/index/index' })
        break
    }
  },

  scanCode() {
    wx.scanCode({
      success: (res) => {
        const result = res.result
        console.log('扫描结果:', result)
        
        // 二维码内容可能是JSON字符串，包含recordId字段
        let recordId = null
        try {
           const obj = JSON.parse(result)
           if (obj.recordId) {
             recordId = obj.recordId
           } else if (obj.id) {
             recordId = obj.id
           }
        } catch(e) {
          // 如果不是JSON，尝试直接解析为数字（可能是纯ID）
          const num = parseInt(result)
          if (!isNaN(num)) {
            recordId = num
          }
        }
        
        if (!recordId) {
          wx.showModal({
            title: '解析失败',
            content: '无法解析二维码内容，请手动输入recordId',
            editable: true,
            placeholderText: '请输入recordId',
            success: (inputRes) => {
              if (inputRes.confirm && inputRes.content) {
                this.verifyPass(inputRes.content)
              }
            }
          })
          return
        }

        this.verifyPass(recordId)
      },
      fail: (err) => {
        console.error(err)
        // 游客模式或模拟器不支持扫码，提供手动输入入口方便调试
        if (err.errMsg.includes('fail') || err.errMsg.includes('cancel')) {
          wx.showModal({
            title: '扫码失败/调试模式',
            content: '无法调用摄像头(可能是模拟器/游客模式)，是否手动输入ID进行核销测试？',
            confirmText: '手动输入',
            cancelText: '取消',
            success: (res) => {
              if (res.confirm) {
                wx.showModal({
                  title: '输入核销码/ID',
                  editable: true,
                  placeholderText: '请输入通行证RecordID',
                  success: (inputRes) => {
                    if (inputRes.confirm && inputRes.content) {
                      this.verifyPass(inputRes.content)
                    }
                  }
                })
              }
            }
          })
          return
        }
        wx.showToast({ title: '扫码取消或失败', icon: 'none' })
      }
    })
  },

  verifyPass(recordId) {
    wx.showLoading({ title: '核验中...' })
    app.request({
      url: '/travel-pass/verify',
      method: 'POST',
      data: { recordId: recordId }
    }).then(res => {
      wx.hideLoading()
      console.log('核验响应:', res)
      if (res.valid === true) {
        wx.showModal({
          title: '核验成功',
          content: res.message || '允许通行',
          showCancel: false,
          confirmText: '确定'
        })
      } else {
        wx.showModal({
          title: '核验失败',
          content: res.message || res.error || '无效二维码或已过期',
          showCancel: false,
          confirmColor: '#ff4d4f'
        })
      }
    }).catch(() => {
      wx.hideLoading()
      wx.showToast({ title: '核验请求失败', icon: 'none' })
    })
  },

  // 跳转到个人信息页面
  goToProfile() {
    wx.navigateTo({ url: '/pages/profile/profile' })
  },

  handleLogout() {
    wx.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          wx.removeStorageSync('token')
          wx.removeStorageSync('employee')
          wx.reLaunch({ url: '/pages/login/login' })
        }
      }
    })
  }
})
