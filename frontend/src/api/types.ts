/** 后端统一响应格式 */
export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface LoginResponse {
  token: string
  userId: number
  name: string
  loginName: string
  type: number
}

export interface UserProfile {
  userId: number
  name: string
  loginName: string
  type: number
  cnyFree: number
  cnyFrozen: number
}

export interface StockView {
  id: number
  code: string
  name: string
  currentPrice: number
}

/** 订单方向：后端用枚举名序列化 */
export type OrderType = 'BUY' | 'SELL'

export interface Fill {
  transactionId: number
  price: number
  amount: number
  counterpartyOrderSide: OrderType
}

export interface TradeResult {
  orderId: number
  amount: number
  /** 本次立即成交的数量 */
  dealed: number
  /** 剩余挂单数量 */
  pending: number
  fills: Fill[]
  message: string
}

export interface OrderView {
  id: number
  stockId: number
  stockCode: string
  stockName: string
  type: OrderType
  price: number
  amount: number
  dealed: number
  undealed: number
  canceled: boolean
  status: string
  createdAt: string
}

export interface PositionView {
  stockId: number
  stockCode: string
  stockName: string
  numFree: number
  numFrozen: number
  total: number
  currentPrice: number
  marketValue: number
}

export interface PortfolioSummary {
  userId: number
  name: string
  loginName: string
  cnyFree: number
  cnyFrozen: number
  cashTotal: number
  marketValue: number
  totalAsset: number
}

export interface PageResult<T> {
  content: T[]
  total: number
  page: number
  size: number
}

export interface PlaceOrderPayload {
  stockId: number
  type: OrderType
  price: number
  amount: number
}
