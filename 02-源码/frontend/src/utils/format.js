/**
 * 全站统一的时间与状态格式化。
 * 所有页面必须调用这里，禁止各自拼字符串（需求硬性要求 9）。
 */

export function formatTime(v) {
  if (!v) return '-'
  const d = typeof v === 'string' ? new Date(v.replace(' ', 'T')) : new Date(v)
  if (Number.isNaN(d.getTime())) return String(v)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

export function formatDate(v) {
  if (!v) return '-'
  const d = typeof v === 'string' ? new Date(v.replace(' ', 'T')) : new Date(v)
  if (Number.isNaN(d.getTime())) return String(v)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

export function money(v) {
  if (v === null || v === undefined || v === '') return '-'
  return `¥${Number(v).toFixed(2)}`
}

/** 预约状态 → 文案 + 标签类型（全站唯一映射） */
export const APPOINTMENT_STATUS = {
  PENDING_CONFIRM: { text: '待确认', type: 'info' },
  WAIT_PAY: { text: '待缴费', type: 'warning' },
  WAIT_CHECKIN: { text: '待到检', type: 'primary' },
  CHECKING: { text: '体检中', type: 'primary' },
  WAIT_REPORT: { text: '待报告', type: 'warning' },
  WAIT_PICKUP: { text: '待领取', type: 'warning' },
  COMPLETED: { text: '已完成', type: 'success' },
  CANCELLED: { text: '已取消', type: 'info' },
  RESCHEDULED: { text: '已改期', type: 'info' }
}

export function statusText(code) {
  return APPOINTMENT_STATUS[code]?.text || code || '-'
}

export function statusType(code) {
  return APPOINTMENT_STATUS[code]?.type || 'info'
}

export const PACKAGE_STATUS = {
  ON_SHELF: { text: '已上架', type: 'success' },
  DRAFT: { text: '草稿', type: 'info' },
  OFF_SHELF: { text: '已下架', type: 'danger' }
}

export const PROGRESS_STATUS = {
  WAITING: { text: '待检', type: 'info' },
  CHECKING: { text: '检查中', type: 'primary' },
  DONE: { text: '已检', type: 'success' },
  GIVEN_UP: { text: '放弃', type: 'info' },
  SUPPLEMENT: { text: '需补检', type: 'warning' }
}

export const LEVEL = {
  RECOMMEND: { text: '推荐', type: 'success' },
  NORMAL: { text: '一般', type: 'warning' },
  NOT_RECOMMEND: { text: '不推荐', type: 'danger' }
}

export const PAY_METHOD = {
  CASH: '现金', WECHAT: '微信', ALIPAY: '支付宝', CARD: '银行卡', GROUP: '单位统一结算'
}

export const PICKUP_TYPE = {
  ONLINE: '本人下载', SELF: '现场领取', ENTRUST: '委托代领'
}

export const BATCH_STATUS = {
  PENDING: { text: '待到检', type: 'primary' },
  CHECKING: { text: '体检中', type: 'warning' },
  DONE: { text: '已完成', type: 'success' }
}
