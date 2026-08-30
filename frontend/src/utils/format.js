/**
 * 格式化工具：日期 / 状态码 → 中文文案与标签颜色
 *
 * 后端 LocalDateTime 序列化成 "2026-08-30T21:03:36"（ISO 格式带 T），
 * 展示时把 T 换成空格更符合中文阅读习惯。
 */

/**
 * 日期时间格式化："2026-08-30T21:03:36" → "2026-08-30 21:03"
 * @param {string} v 后端返回的时间字符串
 * @param {boolean} withSeconds 是否保留秒
 */
export function formatDateTime(v, withSeconds = false) {
  if (!v) return '-'
  return withSeconds ? String(v).replace('T', ' ').slice(0, 19) : String(v).replace('T', ' ').slice(0, 16)
}

/** 纯日期："2026-08-30" */
export function formatDate(v) {
  if (!v) return '-'
  return String(v).slice(0, 10)
}

/**
 * 预约状态字典（对标 database.md D4 订单状态：0=待审核 1=通过 2=驳回 3=已取消）
 * tag 值对应 Element-Plus el-tag 的 type（颜色：warning 橙 / success 绿 / danger 红 / info 灰）
 */
export const RESERVATION_STATUS = {
  0: { text: '待审核', tag: 'warning' },
  1: { text: '已通过', tag: 'success' },
  2: { text: '已驳回', tag: 'danger' },
  3: { text: '已取消', tag: 'info' },
}

/** 状态 → el-tag 颜色类型 */
export function reservationTag(status) {
  return RESERVATION_STATUS[status]?.tag || 'info'
}

/** 状态 → 中文文案（后端 statusText 兜底用） */
export function reservationText(status) {
  return RESERVATION_STATUS[status]?.text || '未知'
}
