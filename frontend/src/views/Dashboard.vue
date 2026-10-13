<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchPositions, fetchStocks, fetchSummary } from '@/api/trade'
import type { PortfolioSummary, PositionView, StockView } from '@/api/types'
import StatCard from '@/components/StatCard.vue'
import { money, price } from '@/utils/format'

const router = useRouter()

const loading = ref(false)
const summary = ref<PortfolioSummary | null>(null)
const stocks = ref<StockView[]>([])
const positions = ref<PositionView[]>([])

async function load() {
  loading.value = true
  try {
    const [s, list, p] = await Promise.all([fetchSummary(), fetchStocks(), fetchPositions()])
    summary.value = s
    stocks.value = list
    positions.value = p
  } catch {
    // 提示已由响应拦截器处理
  } finally {
    loading.value = false
  }
}

function goTrading(stockId: number) {
  router.push({ path: '/trading', query: { stockId: String(stockId) } })
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h2>资产概览</h2>
      <div>
        <span class="tip">数据来自后端实时查询</span>
        <el-button link type="primary" :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <div v-loading="loading">
      <div class="stat-row">
        <StatCard label="总资产" :value="money(summary?.totalAsset)" tone="brand" hint="资金 + 持仓市值" />
        <StatCard label="可用资金" :value="money(summary?.cnyFree)" hint="可直接用于买入" />
        <StatCard label="冻结资金" :value="money(summary?.cnyFrozen)" hint="挂单中的买单占用" />
        <StatCard label="持仓市值" :value="money(summary?.marketValue)" :hint="`${positions.length} 只股票`" />
      </div>

      <div class="card">
        <h3 class="card-title">股票行情</h3>
        <el-table :data="stocks" stripe>
          <el-table-column prop="code" label="代码" width="120" />
          <el-table-column prop="name" label="名称" min-width="140" />
          <el-table-column label="最新价" width="140" align="right">
            <template #default="{ row }">{{ price(row.currentPrice) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click="goTrading(row.id)">去交易</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="card">
        <h3 class="card-title">我的持仓</h3>
        <el-table :data="positions" stripe>
          <el-table-column label="股票" min-width="160">
            <template #default="{ row }">{{ row.stockName }}（{{ row.stockCode }}）</template>
          </el-table-column>
          <el-table-column prop="total" label="持仓" width="100" align="right" />
          <el-table-column prop="numFree" label="可用" width="100" align="right" />
          <el-table-column prop="numFrozen" label="冻结" width="100" align="right" />
          <el-table-column label="最新价" width="120" align="right">
            <template #default="{ row }">{{ price(row.currentPrice) }}</template>
          </el-table-column>
          <el-table-column label="市值" width="150" align="right">
            <template #default="{ row }">{{ money(row.marketValue) }}</template>
          </el-table-column>
          <template #empty>
            <span class="muted">暂无持仓，去「交易下单」买入吧</span>
          </template>
        </el-table>
      </div>
    </div>
  </div>
</template>
