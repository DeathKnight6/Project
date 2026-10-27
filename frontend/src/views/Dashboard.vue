<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh, ArrowRight, Search, Star, StarFilled, View, Hide } from '@element-plus/icons-vue'
import { fetchOrders, fetchPositions, fetchStocks, fetchSummary } from '@/api/trade'
import type { OrderView, PortfolioSummary, PositionView, StockView } from '@/api/types'
import StatCard from '@/components/StatCard.vue'
import AssetAllocation from '@/components/AssetAllocation.vue'
import { useUserStore } from '@/stores/user'
import { dateTime, money, price } from '@/utils/format'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const failed = ref(false)
const summary = ref<PortfolioSummary | null>(null)
const stocks = ref<StockView[]>([])
const positions = ref<PositionView[]>([])
const recentOrders = ref<OrderView[]>([])
const openOrders = ref(0)
const hidden = ref(false)
const search = ref('')
const onlyFavorites = ref(false)
const favoriteKey = `stock_watchlist_${userStore.user?.userId || 'guest'}`

function readFavorites(): number[] {
  try {
    const stored: unknown = JSON.parse(localStorage.getItem(favoriteKey) || '[]')
    return Array.isArray(stored) ? stored.filter((id): id is number => typeof id === 'number') : []
  } catch { return [] }
}
const favorites = ref<number[]>(readFavorites())
const filteredStocks = computed(() => stocks.value.filter((stock) =>
  (!onlyFavorites.value || favorites.value.includes(stock.id)) &&
  `${stock.code}${stock.name}`.toLowerCase().includes(search.value.trim().toLowerCase())))
const topPositions = computed(() => [...positions.value].sort((a, b) => b.marketValue - a.marketValue).slice(0, 4))

function toggleFavorite(id: number) {
  favorites.value = favorites.value.includes(id) ? favorites.value.filter((value) => value !== id) : [...favorites.value, id]
  try { localStorage.setItem(favoriteKey, JSON.stringify(favorites.value)) } catch { /* 保留当前页面的自选 */ }
}
function amount(value?: number) { return hidden.value ? '••••••' : money(value) }
function goTrading(stockId?: number) {
  router.push({ path: '/trading', query: stockId ? { stockId: String(stockId) } : {} })
}
async function load() {
  if (loading.value) return
  loading.value = true
  failed.value = false
  try {
    const [s, list, p, recent, pending] = await Promise.all([
      fetchSummary(), fetchStocks(), fetchPositions(), fetchOrders(0, 5), fetchOrders(0, 1, true)
    ])
    summary.value = s
    stocks.value = list
    positions.value = p
    recentOrders.value = recent.content
    openOrders.value = pending.total
  } catch { failed.value = true } finally { loading.value = false }
}
onMounted(load)
</script>

<template>
  <div class="page dashboard">
    <div class="page-header"><h2>账户总览</h2><el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button></div>
    <el-alert v-if="failed" title="数据加载失败，请重试" type="warning" :closable="false" class="load-error" />
    <div v-loading="loading">
      <div class="overview-grid">
        <section class="balance-banner">
          <div class="balance-heading"><span>总资产（元）</span><button :aria-label="hidden ? '显示金额' : '隐藏金额'" :aria-pressed="hidden" @click="hidden = !hidden"><el-icon><Hide v-if="hidden" /><View v-else /></el-icon></button></div>
          <div class="balance-number number">{{ amount(summary?.totalAsset) }}</div>
          <div class="balance-footer"><div><span>账户资金</span><strong class="number">{{ amount(summary?.cashTotal) }}</strong></div><button @click="goTrading()">开始交易 <el-icon><ArrowRight /></el-icon></button></div>
        </section>
        <AssetAllocation :summary="summary" :hidden="hidden" />
      </div>
      <div class="stat-row">
        <StatCard label="可用资金" :value="amount(summary?.cnyFree)" />
        <StatCard label="持仓市值" :value="amount(summary?.marketValue)" />
        <StatCard label="冻结资金" :value="amount(summary?.cnyFrozen)" />
        <StatCard label="待成交委托" :value="summary ? String(openOrders) : '--'" />
      </div>
      <div class="detail-grid">
        <section class="card market-panel">
          <div class="section-head"><h3 class="card-title">股票行情</h3><el-input v-model="search" aria-label="搜索股票" placeholder="名称 / 代码" :prefix-icon="Search" clearable class="market-search" /></div>
          <div class="market-toolbar"><div class="segmented" aria-label="股票范围"><button :class="{ active: !onlyFavorites }" :aria-pressed="!onlyFavorites" @click="onlyFavorites = false">全部股票</button><button :class="{ active: onlyFavorites }" :aria-pressed="onlyFavorites" @click="onlyFavorites = true">我的自选</button></div></div>
          <el-table :data="filteredStocks">
            <el-table-column label="自选" width="60" align="center"><template #default="{ row }"><button class="favorite-btn" :class="{ saved: favorites.includes(row.id) }" :aria-label="`${favorites.includes(row.id) ? '取消自选' : '添加自选'} ${row.name}`" :aria-pressed="favorites.includes(row.id)" @click="toggleFavorite(row.id)"><el-icon><StarFilled v-if="favorites.includes(row.id)" /><Star v-else /></el-icon></button></template></el-table-column>
            <el-table-column label="股票" min-width="170"><template #default="{ row }"><div class="stock-cell"><span class="stock-monogram">{{ row.name.slice(0, 1) }}</span><span><strong>{{ row.name }}</strong><small>{{ row.code }}</small></span></div></template></el-table-column>
            <el-table-column label="最新价 / 元" prop="currentPrice" sortable min-width="135" align="right"><template #default="{ row }"><strong class="number">{{ price(row.currentPrice) }}</strong></template></el-table-column>
            <el-table-column label="操作" width="90" align="right"><template #default="{ row }"><el-button link @click="goTrading(row.id)">交易 →</el-button></template></el-table-column>
            <template #empty><div class="empty-state">{{ search ? '无匹配股票' : onlyFavorites ? '暂无自选股票' : '暂无股票' }}</div></template>
          </el-table>
        </section>
        <section class="card holdings-panel">
          <div class="section-head"><h3 class="card-title">持仓一览</h3><router-link to="/portfolio">全部 →</router-link></div>
          <div v-if="topPositions.length" class="holding-list">
            <button v-for="position in topPositions" :key="position.stockId" @click="goTrading(position.stockId)">
              <div class="holding-row"><strong>{{ position.stockName }}</strong><strong class="number">{{ amount(position.marketValue) }}</strong></div>
              <div class="holding-quantity">{{ position.total.toLocaleString() }} 股</div>
              <div class="holding-bar"><span :style="{ width: `${summary?.marketValue ? position.marketValue / summary.marketValue * 100 : 0}%` }"></span></div>
            </button>
          </div>
          <div v-else class="empty-state">暂无持仓</div>
        </section>
      </div>
      <section class="card recent-panel">
        <div class="section-head"><h3 class="card-title">最近委托</h3><router-link to="/portfolio">全部订单 →</router-link></div>
        <el-table :data="recentOrders">
          <el-table-column label="股票" min-width="180"><template #default="{ row }"><strong>{{ row.stockName }}</strong><span class="order-code">{{ row.stockCode }}</span></template></el-table-column>
          <el-table-column label="方向" width="90"><template #default="{ row }">{{ row.type === 'BUY' ? '买入' : '卖出' }}</template></el-table-column>
          <el-table-column label="委托价" width="120" align="right"><template #default="{ row }">{{ hidden ? '••••' : price(row.price) }}</template></el-table-column>
          <el-table-column label="成交 / 委托" min-width="130" align="right"><template #default="{ row }">{{ row.dealed }} / {{ row.amount }}</template></el-table-column>
          <el-table-column label="状态" width="120" align="center"><template #default="{ row }">{{ row.status }}</template></el-table-column>
          <el-table-column label="下单时间" width="195" align="right"><template #default="{ row }">{{ dateTime(row.createdAt) }}</template></el-table-column>
          <template #empty><div class="empty-state">暂无委托</div></template>
        </el-table>
      </section>
    </div>
  </div>
</template>

<style scoped>
.load-error { margin-bottom: 22px; }
.overview-grid { display: grid; grid-template-columns: minmax(0, 1.5fr) minmax(360px, 1fr); gap: 38px; align-items: center; }
.balance-banner { padding: 30px 34px; background: var(--red) url('../assets/gold-clouds.svg') right center / 780px auto; color: #fff; border-left: 5px solid var(--gold); }
.balance-heading { display: flex; align-items: center; gap: 16px; font-size: 16px; }.balance-heading button { display: flex; border: 0; color: #fff; background: none; padding: 6px; font-size: 20px; }
.balance-number { margin: 24px 0 30px; font-size: clamp(34px, 4vw, 58px); font-weight: 500; letter-spacing: -1.5px; }
.balance-footer { display: flex; align-items: end; justify-content: space-between; gap: 18px; padding-top: 22px; border-top: 1px solid var(--gold); }.balance-footer span { display: block; font-size: 15px; }.balance-footer strong { display: block; font-size: 23px; margin-top: 12px; font-weight: 500; }.balance-footer button { display: flex; align-items: center; gap: 22px; border: 1px solid var(--gold); background: var(--gold); color: #000; font-size: 16px; padding: 13px 21px; }
.overview-grid :deep(.allocation) { border: 0; padding: 18px 0; }
.detail-grid { display: grid; grid-template-columns: minmax(0, 1.7fr) minmax(270px, 1fr); gap: 38px; }.market-panel { padding-right: 32px; border-right: 1px solid var(--line); }.market-search { max-width: 220px; }.market-toolbar { margin-bottom: 18px; }.market-toolbar .segmented { width: fit-content; }
.favorite-btn { display: inline-flex; align-items: center; justify-content: center; color: #000; border: 0; background: none; padding: 5px; font-size: 20px; }.favorite-btn.saved { background: #f1dda5; }
.holding-list { display: grid; }.holding-list > button { border: 0; border-bottom: 1px solid var(--line); background: none; padding: 22px 0; text-align: left; color: #000; }.holding-list > button:first-child { padding-top: 10px; }.holding-row { display: flex; justify-content: space-between; gap: 14px; font-size: 15px; }.holding-row strong { font-weight: 600; }.holding-quantity { margin-top: 13px; font-size: 14px; }.holding-bar { height: 5px; background: #efdfb7; margin-top: 16px; }.holding-bar span { display: block; height: 100%; background: var(--red); }.order-code { margin-left: 12px; font-size: 14px; }
@media(max-width:1100px) { .overview-grid { gap: 28px; grid-template-columns: minmax(0, 1.2fr) minmax(330px, 1fr); }.balance-banner { padding: 28px; }.detail-grid { gap: 26px; grid-template-columns: minmax(0, 1.6fr) minmax(250px, 1fr); }.market-panel { padding-right: 24px; } }
@media(max-width:900px) { .overview-grid, .detail-grid { grid-template-columns: 1fr; gap: 0; }.overview-grid :deep(.allocation) { padding: 28px 0; }.market-panel { border-right: 0; padding-right: 0; }.holding-list { grid-template-columns: 1fr 1fr; gap: 24px; }.balance-number { font-size: 45px; } }
@media(max-width:480px) { .balance-banner { padding: 24px 20px; }.balance-number { font-size: 35px; }.balance-footer strong { font-size: 21px; }.balance-footer button { padding: 12px 14px; gap: 9px; }.holding-list { grid-template-columns: 1fr; gap: 0; }.market-search { max-width: 100%; }.order-code { display: block; margin: 5px 0 0; } }
</style>
