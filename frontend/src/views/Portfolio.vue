<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelOrder, fetchOrders, fetchPositions, fetchSummary } from '@/api/trade'
import type { OrderView, PortfolioSummary, PositionView } from '@/api/types'
import StatCard from '@/components/StatCard.vue'
import { dateTime, money, orderTypeText, price } from '@/utils/format'

const loading = ref(false)
const summary = ref<PortfolioSummary | null>(null)
const positions = ref<PositionView[]>([])
const orders = ref<OrderView[]>([])
const total = ref(0)
const page = ref(0)
const size = ref(20)

/** 持仓成本按最新价展示市值，同时给出总市值合计 */
const positionValue = computed(() =>
  positions.value.reduce((sum, p) => sum + Number(p.marketValue || 0), 0)
)

async function load() {
  loading.value = true
  try {
    const [s, p, orderPage] = await Promise.all([
      fetchSummary(),
      fetchPositions(),
      fetchOrders(page.value, size.value)
    ])
    summary.value = s
    positions.value = p
    orders.value = orderPage.content
    total.value = orderPage.total
  } catch {
    // 提示已由响应拦截器处理
  } finally {
    loading.value = false
  }
}

function handlePageChange(next: number) {
  page.value = next - 1
  load()
}

async function handleCancel(row: OrderView) {
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
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <h2>我的持仓</h2>
      <div>
        <span class="tip">共 {{ total }} 条订单记录</span>
        <el-button link type="primary" :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <div v-loading="loading">
      <div class="stat-row">
        <StatCard label="总资产" :value="money(summary?.totalAsset)" tone="brand" />
        <StatCard label="可用资金" :value="money(summary?.cnyFree)" />
        <StatCard label="冻结资金" :value="money(summary?.cnyFrozen)" />
        <StatCard label="持仓市值" :value="money(positionValue)" :hint="`${positions.length} 只股票`" />
      </div>

      <div class="card">
        <h3 class="card-title">持仓明细</h3>
        <el-table :data="positions" stripe>
          <el-table-column label="股票" min-width="170">
            <template #default="{ row }">{{ row.stockName }}（{{ row.stockCode }}）</template>
          </el-table-column>
          <el-table-column prop="total" label="总持仓" width="100" align="right" />
          <el-table-column prop="numFree" label="可用" width="100" align="right" />
          <el-table-column prop="numFrozen" label="冻结" width="100" align="right" />
          <el-table-column label="最新价" width="120" align="right">
            <template #default="{ row }">{{ price(row.currentPrice) }}</template>
          </el-table-column>
          <el-table-column label="市值" width="150" align="right">
            <template #default="{ row }">{{ money(row.marketValue) }}</template>
          </el-table-column>
          <template #empty>
            <span class="muted">暂无持仓</span>
          </template>
        </el-table>
      </div>

      <div class="card">
        <h3 class="card-title">订单记录</h3>
        <el-table :data="orders" stripe>
          <el-table-column prop="id" label="订单号" width="90" />
          <el-table-column label="股票" min-width="150">
            <template #default="{ row }">{{ row.stockName }}（{{ row.stockCode }}）</template>
          </el-table-column>
          <el-table-column label="方向" width="80">
            <template #default="{ row }">
              <span :class="row.type === 'BUY' ? 'up' : 'down'">{{ orderTypeText(row.type) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="委托价" width="100" align="right">
            <template #default="{ row }">{{ price(row.price) }}</template>
          </el-table-column>
          <el-table-column prop="amount" label="委托量" width="90" align="right" />
          <el-table-column prop="dealed" label="已成交" width="90" align="right" />
          <el-table-column prop="undealed" label="未成交" width="90" align="right" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="row.canceled ? 'info' : row.undealed > 0 ? 'warning' : 'success'">
                {{ row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="下单时间" width="170">
            <template #default="{ row }">{{ dateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="90" align="center">
            <template #default="{ row }">
              <el-button
                v-if="!row.canceled && row.undealed > 0"
                link
                type="danger"
                @click="handleCancel(row)"
              >
                撤单
              </el-button>
              <span v-else class="muted">—</span>
            </template>
          </el-table-column>
          <template #empty>
            <span class="muted">暂无订单</span>
          </template>
        </el-table>

        <el-pagination
          v-if="total > size"
          class="pager"
          layout="prev, pager, next"
          :total="total"
          :page-size="size"
          :current-page="page + 1"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.pager {
  margin-top: 14px;
  justify-content: flex-end;
}
</style>
