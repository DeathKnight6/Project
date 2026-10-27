<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { cancelOrder, fetchOrders, fetchPositions, fetchSummary } from '@/api/trade'
import type { OrderView, PortfolioSummary, PositionView } from '@/api/types'
import StatCard from '@/components/StatCard.vue'
import AssetAllocation from '@/components/AssetAllocation.vue'
import { dateTime, money, orderTypeText, price } from '@/utils/format'

const loading = ref(false)
const summary = ref<PortfolioSummary | null>(null)
const positions = ref<PositionView[]>([])
const orders = ref<OrderView[]>([])
const total = ref(0)
const page = ref(0)
const size = ref(20)
const openOnly = ref(false)
const search = ref('')
const failed = ref(false)
const canceling = ref<number | null>(null)
const filteredPositions = computed(() => positions.value.filter((position) => `${position.stockName}${position.stockCode}`.includes(search.value.trim())))
const rankedPositions = computed(() => [...positions.value].sort((a, b) => b.marketValue - a.marketValue))
let loadVersion = 0

/** 持仓成本按最新价展示市值，同时给出总市值合计 */
const positionValue = computed(() =>
  positions.value.reduce((sum, p) => sum + Number(p.marketValue || 0), 0)
)

async function load() {
  const version = ++loadVersion
  loading.value = true
  failed.value = false
  try {
    const [s, p, orderPage] = await Promise.all([
      fetchSummary(),
      fetchPositions(),
      fetchOrders(page.value, size.value, openOnly.value)
    ])
    if (version !== loadVersion) return
    summary.value = s
    positions.value = p
    orders.value = orderPage.content
    total.value = orderPage.total
    if (page.value > 0 && orderPage.content.length === 0) {
      page.value = Math.max(0, Math.ceil(orderPage.total / size.value) - 1)
      await load()
    }
  } catch {
    if (version === loadVersion) failed.value = true
    // 提示已由响应拦截器处理
  } finally {
    if (version === loadVersion) loading.value = false
  }
}

function handlePageChange(next: number) {
  page.value = next - 1
  load()
}

async function handleCancel(row: OrderView) {
  if (canceling.value !== null) return
  canceling.value = row.id
  try {
    await ElMessageBox.confirm(
      `确定撤销订单 #${row.id}（${row.stockName} ${orderTypeText(row.type)} ${row.undealed} 股未成交）吗？`,
      '撤单确认',
      { confirmButtonText: '撤单', cancelButtonText: '再想想', type: 'warning' }
    )
    await cancelOrder(row.id)
    ElMessage.success('撤单成功')
    await load()
  } catch {
    // 提示已由响应拦截器处理
  } finally {
    canceling.value = null
  }
}

function changeScope(pending: boolean) {
  openOnly.value = pending
  page.value = 0
  load()
}

onMounted(load)
</script>
<template>
  <div class="page">
    <div class="page-header"><h2>持仓与订单</h2><el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button></div>
    <el-alert v-if="failed" title="数据加载失败，请重试" type="warning" :closable="false" class="load-error" />
    <div v-loading="loading">
      <div class="stat-row">
        <StatCard label="总资产" :value="money(summary?.totalAsset)" />
        <StatCard label="可用资金" :value="money(summary?.cnyFree)" />
        <StatCard label="冻结资金" :value="money(summary?.cnyFrozen)" />
        <StatCard label="持仓市值" :value="summary ? money(positionValue) : '--'" />
      </div>
      <div class="portfolio-insights">
        <AssetAllocation :summary="summary" />
        <section class="card weight-panel">
          <div class="section-head"><h3 class="card-title">持仓权重</h3></div>
          <div v-if="rankedPositions.length" class="weight-list">
            <div v-for="(position, index) in rankedPositions" :key="position.stockId" class="weight-item">
              <div class="weight-label"><strong>{{ position.stockName }}</strong><span class="number">{{ positionValue > 0 ? (position.marketValue / positionValue * 100).toFixed(1) : '0.0' }}%</span></div>
              <div class="weight-track"><span :style="{ width: `${positionValue > 0 ? position.marketValue / positionValue * 100 : 0}%`, background: ['#b51622', '#d3ab4e', '#efcf79', '#99141e', '#dfb963'][index % 5] }"></span></div>
            </div>
          </div>
          <div v-else class="empty-state">暂无持仓</div>
        </section>
      </div>
      <section class="card">
        <div class="section-head"><h3 class="card-title">持仓明细</h3><el-input v-model="search" :prefix-icon="Search" placeholder="名称 / 代码" aria-label="搜索持仓" clearable class="position-search" /></div>
        <el-table :data="filteredPositions">
          <el-table-column label="股票" min-width="180"><template #default="{ row }"><div class="stock-cell"><span class="stock-monogram">{{ row.stockName.slice(0, 1) }}</span><span><strong>{{ row.stockName }}</strong><small>{{ row.stockCode }}</small></span></div></template></el-table-column>
          <el-table-column prop="total" label="总持仓" width="100" align="right" />
          <el-table-column prop="numFree" label="可用" width="100" align="right" />
          <el-table-column prop="numFrozen" label="冻结" width="100" align="right" />
          <el-table-column label="最新价 / 元" width="140" align="right"><template #default="{ row }">{{ price(row.currentPrice) }}</template></el-table-column>
          <el-table-column label="市值 / 元" min-width="160" align="right"><template #default="{ row }">{{ money(row.marketValue) }}</template></el-table-column>
          <el-table-column label="操作" width="100" align="right"><template #default="{ row }"><router-link :to="{ path: '/trading', query: { stockId: row.stockId } }">交易 →</router-link></template></el-table-column>
          <template #empty><div class="empty-state">{{ search ? '无匹配持仓' : '暂无持仓' }}</div></template>
        </el-table>
      </section>
      <section class="card">
        <div class="section-head"><h3 class="card-title">订单记录（{{ total }}）</h3><div class="segmented" aria-label="订单范围"><button :class="{ active: !openOnly }" :aria-pressed="!openOnly" @click="changeScope(false)">全部订单</button><button :class="{ active: openOnly }" :aria-pressed="openOnly" @click="changeScope(true)">待成交</button></div></div>
        <el-table :data="orders">
          <el-table-column prop="id" label="订单号" width="90" />
          <el-table-column label="股票" min-width="170"><template #default="{ row }">{{ row.stockName }}（{{ row.stockCode }}）</template></el-table-column>
          <el-table-column label="方向" width="80"><template #default="{ row }">{{ orderTypeText(row.type) }}</template></el-table-column>
          <el-table-column label="委托价" width="110" align="right"><template #default="{ row }">{{ price(row.price) }}</template></el-table-column>
          <el-table-column prop="amount" label="委托量" width="95" align="right" />
          <el-table-column prop="dealed" label="已成交" width="95" align="right" />
          <el-table-column prop="undealed" label="未成交" width="95" align="right" />
          <el-table-column label="成交进度" width="140"><template #default="{ row }"><el-progress :percentage="row.amount ? Math.round(row.dealed / row.amount * 100) : 0" :stroke-width="5" :color="row.canceled ? '#d3ab4e' : '#b51622'" /></template></el-table-column>
          <el-table-column label="状态" width="110"><template #default="{ row }">{{ row.status }}</template></el-table-column>
          <el-table-column label="下单时间" width="195"><template #default="{ row }">{{ dateTime(row.createdAt) }}</template></el-table-column>
          <el-table-column label="操作" width="90" align="center"><template #default="{ row }"><el-button v-if="!row.canceled && row.undealed > 0" link :loading="canceling === row.id" :disabled="canceling !== null" @click="handleCancel(row)">撤单</el-button><span v-else>—</span></template></el-table-column>
          <template #empty><div class="empty-state">{{ openOnly ? '暂无待成交订单' : '暂无订单' }}</div></template>
        </el-table>
        <el-pagination v-if="total > size" layout="prev, pager, next" :total="total" :page-size="size" :current-page="page + 1" @current-change="handlePageChange" />
      </section>
    </div>
  </div>
</template>
<style scoped>
.load-error { margin-bottom: 22px; }.portfolio-insights { display: grid; grid-template-columns: minmax(350px, 1fr) minmax(0, 1.35fr); gap: 36px; }.weight-panel { border-left: 1px solid var(--line); padding-left: 34px; }.weight-list { display: grid; grid-template-columns: 1fr 1fr; gap: 30px; padding-top: 16px; }.weight-label { display: flex; align-items: center; justify-content: space-between; gap: 14px; font-size: 15px; }.weight-label strong { font-weight: 600; }.weight-track { height: 6px; background: #eedfba; margin-top: 17px; }.weight-track > span { display: block; height: 100%; }.position-search { max-width: 220px; }
@media(max-width:900px) { .portfolio-insights { grid-template-columns: 1fr; gap: 0; }.weight-panel { border-left: 0; padding-left: 0; }.weight-list { padding-top: 0; padding-bottom: 12px; } }
@media(max-width:480px) { .weight-list { grid-template-columns: 1fr; }.position-search { max-width: 100%; } }
</style>
