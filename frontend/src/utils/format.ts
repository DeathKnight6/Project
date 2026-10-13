/** 金额千分位，保留两位小数 */
export function money(value?: number | null): string {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return '--'
  }
  return Number(value).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

/** 价格，保留两位小数 */
export function price(value?: number | null): string {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return '--'
  }
  return Number(value).toFixed(2)
}

/** 后端返回 ISO 时间（2026-09-22T19:10:00），统一转成可读格式 */
export function dateTime(value?: string | null): string {
  if (!value) {
    return '--'
  }
  return value.replace('T', ' ').slice(0, 19)
}

/** 订单方向的中文名 */
export function orderTypeText(type: string): string {
  return type === 'BUY' ? '买入' : '卖出'
}
