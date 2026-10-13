import { http } from './request'
import type {
  OrderView,
  PageResult,
  PlaceOrderPayload,
  PortfolioSummary,
  PositionView,
  StockView,
  TradeResult
} from './types'

export function fetchStocks() {
  return http.get<StockView[]>('/stock/list')
}

export function placeOrder(payload: PlaceOrderPayload) {
  return http.post<TradeResult>('/trade/place', payload)
}

export function cancelOrder(orderId: number) {
  return http.post<void>(`/trade/cancel/${orderId}`)
}

export function fetchSummary() {
  return http.get<PortfolioSummary>('/portfolio/summary')
}

export function fetchPositions() {
  return http.get<PositionView[]>('/portfolio/positions')
}

export function fetchOrders(page = 0, size = 50) {
  return http.get<PageResult<OrderView>>('/portfolio/orders', { page, size })
}
