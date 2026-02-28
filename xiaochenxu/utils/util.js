const formatTime = date => {
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  const hour = date.getHours()
  const minute = date.getMinutes()
  const second = date.getSeconds()

  return `${[year, month, day].map(formatNumber).join('/')} ${[hour, minute, second].map(formatNumber).join(':')}`
}

const formatTimeRelative = dateStr => {
  if (!dateStr) return ''
  // 兼容 ISO 8601 格式 (如 2026-01-11T14:20:26.000+00:00)
  // 小程序 iOS 环境下 Date.parse 可能不支持 ISO 格式，需转换
  let date = new Date(dateStr)
  if (isNaN(date.getTime())) {
    // 尝试替换 T 为空格，去掉毫秒等
    dateStr = dateStr.replace('T', ' ').replace(/\..+/, '').replace(/-/g, '/')
    date = new Date(dateStr)
  }
  if (isNaN(date.getTime())) return dateStr

  const now = new Date()
  const diff = now - date
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  const month = 30 * day
  const year = 12 * month

  if (diff < minute) {
    return '刚刚'
  } else if (diff < hour) {
    return Math.floor(diff / minute) + '分钟前'
  } else if (diff < day) {
    return Math.floor(diff / hour) + '小时前'
  } else if (diff < month) {
    return Math.floor(diff / day) + '天前'
  } else if (diff < year) {
    return Math.floor(diff / month) + '个月前'
  } else {
    return Math.floor(diff / year) + '年前'
  }
}

const formatNumber = n => {
  n = n.toString()
  return n[1] ? n : `0${n}`
}

module.exports = {
  formatTime,
  formatTimeRelative
}
