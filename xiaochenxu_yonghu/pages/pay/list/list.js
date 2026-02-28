const app = getApp()

Page({
  data: {
    user: null,
    list: [],
    page: 1,
    size: 10,
    currentTab: 1, // 1: 待支付, 2: 已支付
    loading: false,
    hasMore: true
  },

  onShow() {
    const user = wx.getStorageSync('user')
    if (user) {
      this.setData({ user })
      this.loadBills(true)
    } else {
      wx.showToast({ title: '请先登录', icon: 'none' })
    }
  },

  switchTab(e) {
    const tab = parseInt(e.currentTarget.dataset.tab)
    if (tab !== this.data.currentTab) {
      this.setData({ currentTab: tab, list: [], page: 1, hasMore: true })
      this.loadBills(true)
    }
  },

  loadBills(reset = false) {
    if (this.data.loading) return
    if (reset) {
      this.setData({ list: [], page: 1, hasMore: true })
    }

    this.setData({ loading: true })

    // API does not support filtering by status directly in `billsById`, it returns all.
    // So we might need to filter client side or use `allBills` if it supported status.
    // The provided `FeeController` has `billsById(residenceId)` and `allBills`.
    // We'll use `billsById` and filter on client side for now, or assume the backend sorts them.
    // Wait, the API `billsById` takes `residenceId`. 
    // `residenceId` is constructed as `building-unit-room`.
    
    // User entity has `residenceId` field but it says "(生成列)". 
    // Let's construct it if missing.
    let residenceId = this.data.user.residenceId
    if (!residenceId && this.data.user.buildingNo) {
      residenceId = `${this.data.user.buildingNo}-${this.data.user.unitNo}-${this.data.user.roomNo}`
    }

    if (!residenceId) {
      wx.showToast({ title: '房屋信息不全', icon: 'none' })
      this.setData({ loading: false })
      return
    }

    app.request({
      url: '/api/fee/billsById',
      data: {
        residenceId: residenceId,
        pageNum: this.data.page,
        pageSize: 20 // Get more to filter
      }
    }).then(res => {
      if (res.code === '200') {
        let bills = res.data.list || []
        
        // Client-side filter for status
        // status: 1-待支付, 2-已支付
        if (this.data.currentTab === 1) {
          bills = bills.filter(item => item.status === 1)
        } else {
          bills = bills.filter(item => item.status === 2)
        }

        this.setData({
          list: this.data.page === 1 ? bills : this.data.list.concat(bills),
          hasMore: res.data.list.length === 20, // Rough estimation
          loading: false
        })
      } else {
        this.setData({ loading: false })
      }
    }).catch(err => {
      console.error(err)
      this.setData({ loading: false })
    })
  },

  payBill(e) {
    const id = e.currentTarget.dataset.id
    const bill = this.data.list.find(i => i.id === id)
    if (!bill) return

    // Navigate to sandbox page for payment
    let residenceId = this.data.user.residenceId
    if (!residenceId && this.data.user.buildingNo) {
      residenceId = `${this.data.user.buildingNo}-${this.data.user.unitNo}-${this.data.user.roomNo}`
    }

    wx.navigateTo({
      url: `/pages/pay/sandbox/sandbox?billId=${id}&residenceId=${residenceId}`
    })
  }
})
