<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Refresh, Search, ArrowRight, CircleCheck } from '@element-plus/icons-vue'
import { cancelOrder, fetchOrders, fetchPositions, fetchStocks, fetchSummary, placeOrder } from '@/api/trade'
import type { OrderType, OrderView, PositionView, StockView, TradeResult } from '@/api/types'
import { dateTime, money, orderTypeText, price } from '@/utils/format'

const route = useRoute()

const stocks = ref<StockView[]>([])
const orders = ref<OrderView[]>([])
const availableCash = ref(0)
const positions = ref<PositionView[]>([])
const orderPage = ref(0)
const orderTotal = ref(0)
const orderSize = 20
const canceling = ref<number | null>(null)
const loading = ref(false)
const submitting = ref(false)
const search = ref('')
const failed = ref(false)
const lastResult = ref<TradeResult | null>(null)
const filteredStocks = computed(() => stocks.value.filter((stock) => `${stock.name}${stock.code}`.includes(search.value.trim())))
function useFraction(fraction: number) {
  if (maximumAmount.value > 0) form.amount = Math.max(1, Math.floor(maximumAmount.value * fraction))
}

const formRef = ref<FormInstance>()
const form = reactive<{ stockId: number | null; type: OrderType; price: number | null; amount: number | null }>({
  stockId: null,
  type: 'BUY',
  price: null,
  amount: null
})

const rules: FormRules = {
  stockId: [{ required: true, message: '请选择股票', trigger: 'change' }],
  type: [{ required: true, message: '请选择买卖方向', trigger: 'change' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  amount: [{ required: true, message: '请输入数量', trigger: 'blur' }]
}

const selectedStock = computed(() => stocks.value.find((s) => s.id === form.stockId) || null)
const availableShares = computed(() => positions.value.find((p) => p.stockId === form.stockId)?.numFree ?? 0)
const maximumAmount = computed(() => Math.max(0, Math.min(1000000,
  form.type === 'SELL' ? availableShares.value : Math.floor(availableCash.value / (form.price || Infinity)),
  Math.floor(100000000 / (form.price || Infinity)))))
const submitError = computed(() => {
  if (!form.amount || !form.price) return ''
  if (!Number.isInteger(form.amount)) return '数量必须为整数'
  if (estimate.value > 100000000) return '单笔委托金额不能超过 1 亿元'
  if (form.type === 'BUY' && estimate.value > availableCash.value) return '可用资金不足'
  if (form.type === 'SELL' && form.amount > availableShares.value) return '可用股票不足'
  return ''
})

/** 预估金额，帮助用户看清这单要花多少钱 */
const estimate = computed(() => {
  if (!form.price || !form.amount) {
    return 0
  }
  return Number(form.price) * Number(form.amount)
})

/** 只展示还能撤的订单 */
const cancelableOrders = computed(() =>
  orders.value.filter((o) => !o.canceled && o.undealed > 0)
)

async function load() {
  loading.value = true
  failed.value = false
  try {
    const [list, pendingPage, summary, currentPositions] = await Promise.all([
      fetchStocks(),
      fetchOrders(orderPage.value, orderSize, true),
      fetchSummary(),
      fetchPositions()
    ])
    stocks.value = list
    orders.value = pendingPage.content
    orderTotal.value = pendingPage.total
    positions.value = currentPositions
    availableCash.value = summary.cnyFree

    if (orderPage.value > 0 && pendingPage.content.length === 0) {
      orderPage.value = Math.max(0, Math.ceil(pendingPage.total / orderSize) - 1)
      await load()
      return
    }

    // 从「资产概览」点「去交易」跳过来时，自动带上股票并填好现价
    const fromQuery = Number(route.query.stockId)
    if (!form.stockId && fromQuery && list.some((s) => s.id === fromQuery)) {
      pickStock(fromQuery)
    } else if (!form.stockId && list.length > 0) {
      pickStock(list[0].id)
    }
  } catch {
    failed.value = true
    // 提示已由响应拦截器处理
  } finally {
    loading.value = false
  }
}

function pickStock(stockId: number) {
  form.stockId = stockId
  const stock = stocks.value.find((s) => s.id === stockId)
  if (stock) {
    form.price = stock.currentPrice
  }
}

async function handleSubmit() {
  if (!formRef.value || submitting.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  if (submitError.value) {
    ElMessage.warning(submitError.value)
    return
  }

  submitting.value = true
  try {
    const result = await placeOrder({
      stockId: form.stockId as number,
      type: form.type,
      price: Number(form.price),
      amount: Number(form.amount)
    })
    lastResult.value = result

    const detail = result.fills.length
      ? result.fills.map((f) => `成交 ${f.amount} 股 @ ${price(f.price)}`).join('，')
      : '暂无成交'
    ElMessage.success(`${result.message}（${detail}）`)

    form.amount = null
    await load()
  } catch {
    // 提示已由响应拦截器处理
  } finally {
    submitting.value = false
  }
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
    ElMessage.success('撤单成功，冻结的资金或股票已退回')
    await load()
  } catch {
    // 提示已由响应拦截器处理
  } finally {
    canceling.value = null
  }
}

function changeOrderPage(next: number) {
  orderPage.value = next - 1
  load()
}

onMounted(load)
</script>
<template>
  <div class="page">
    <div class="page-header"><h2>交易中心</h2><el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button></div>
    <el-alert v-if="failed" title="数据加载失败，请重试" type="warning" :closable="false" class="load-error" />
    <div v-loading="loading" class="trade-grid">
      <section class="card market-panel">
        <div class="section-head"><h3 class="card-title">行情报价</h3><el-input v-model="search" :prefix-icon="Search" aria-label="搜索交易股票" placeholder="名称 / 代码" clearable class="stock-search" /></div>
        <div v-if="selectedStock" class="selected-quote"><div class="stock-cell"><span class="stock-monogram">{{ selectedStock.name.slice(0, 1) }}</span><span><strong>{{ selectedStock.name }}</strong><small>{{ selectedStock.code }}</small></span></div><strong class="selected-price number">{{ price(selectedStock.currentPrice) }} <span>元</span></strong></div>
        <el-table :data="filteredStocks" highlight-current-row :current-row-key="form.stockId ?? undefined" row-key="id" @row-click="(row: StockView) => !submitting && pickStock(row.id)">
          <el-table-column prop="code" label="代码" width="110" />
          <el-table-column prop="name" label="名称" min-width="130" />
          <el-table-column label="最新价 / 元" min-width="140" align="right"><template #default="{ row }">{{ price(row.currentPrice) }}</template></el-table-column>
          <el-table-column label="选择" width="70" align="right"><template #default="{ row }"><el-icon><CircleCheck v-if="row.id === form.stockId" /><ArrowRight v-else /></el-icon></template></el-table-column>
          <template #empty><div class="empty-state">{{ search ? '无匹配股票' : '暂无股票' }}</div></template>
        </el-table>
      </section>
      <section class="card order-panel">
        <div class="section-head"><h3 class="card-title">提交委托</h3></div>
        <div class="available-balance"><span>可用资金</span><strong class="number">¥ {{ money(availableCash) }}</strong></div>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" :disabled="submitting">
          <el-form-item label="股票" prop="stockId"><el-select v-model="form.stockId" style="width: 100%" @change="pickStock"><el-option v-for="stock in stocks" :key="stock.id" :label="`${stock.name}（${stock.code}）`" :value="stock.id" /></el-select></el-form-item>
          <el-form-item label="方向" prop="type"><el-radio-group v-model="form.type" class="trade-direction"><el-radio-button value="BUY">买入</el-radio-button><el-radio-button value="SELL">卖出</el-radio-button></el-radio-group></el-form-item>
          <div class="order-fields">
            <el-form-item label="价格（元）" prop="price"><el-input-number v-model="form.price" :min="0.01" :step="0.01" :precision="2" controls-position="right" style="width: 100%" /></el-form-item>
            <el-form-item label="数量（股）" prop="amount"><el-input-number v-model="form.amount" :min="1" :max="1000000" :precision="0" :step="100" controls-position="right" style="width: 100%" /></el-form-item>
          </div>
          <div class="quantity-shortcuts"><button v-for="fraction in [0.25, 0.5, 0.75, 1]" :key="fraction" type="button" :disabled="submitting || maximumAmount < 1" @click="useFraction(fraction)">{{ fraction * 100 }}%</button></div>
          <div class="quantity-tools"><span>可卖 {{ availableShares }} 股</span><el-button link :disabled="maximumAmount < 1" @click="form.amount = maximumAmount">最大 {{ maximumAmount }} 股</el-button></div>
          <div class="estimate"><span>预估{{ form.type === 'BUY' ? '支出' : '收入' }}</span><strong class="number">{{ money(estimate) }} 元</strong></div>
          <el-button type="primary" :loading="submitting" :disabled="Boolean(submitError) || loading || failed" class="submit-order" @click="handleSubmit">提交{{ form.type === 'BUY' ? '买入' : '卖出' }}委托</el-button>
          <el-alert v-if="submitError" :title="submitError" type="warning" :closable="false" class="submit-error" />
        </el-form>
      </section>
    </div>
    <div v-if="lastResult" class="order-receipt" role="status"><el-icon><CircleCheck /></el-icon><span>委托 #{{ lastResult.orderId }} 已提交 · 已成交 {{ lastResult.dealed }} 股 · 待成交 {{ lastResult.pending }} 股</span><button aria-label="关闭委托回执" @click="lastResult = null">×</button></div>
    <section class="card">
      <div class="section-head"><h3 class="card-title">当前挂单（{{ orderTotal }}）</h3><router-link to="/portfolio">全部订单 →</router-link></div>
      <el-table :data="cancelableOrders">
        <el-table-column prop="id" label="订单号" width="90" />
        <el-table-column label="股票" min-width="180"><template #default="{ row }">{{ row.stockName }}（{{ row.stockCode }}）</template></el-table-column>
        <el-table-column label="方向" width="80"><template #default="{ row }">{{ orderTypeText(row.type) }}</template></el-table-column>
        <el-table-column label="委托价" width="110" align="right"><template #default="{ row }">{{ price(row.price) }}</template></el-table-column>
        <el-table-column prop="amount" label="委托量" width="95" align="right" />
        <el-table-column prop="dealed" label="已成交" width="95" align="right" />
        <el-table-column prop="undealed" label="未成交" width="95" align="right" />
        <el-table-column label="下单时间" width="195"><template #default="{ row }">{{ dateTime(row.createdAt) }}</template></el-table-column>
        <el-table-column label="操作" width="90" align="center"><template #default="{ row }"><el-button link :loading="canceling === row.id" :disabled="canceling !== null" @click="handleCancel(row)">撤单</el-button></template></el-table-column>
        <template #empty><div class="empty-state">暂无挂单</div></template>
      </el-table>
      <el-pagination v-if="orderTotal > orderSize" layout="prev, pager, next" :total="orderTotal" :page-size="orderSize" :current-page="orderPage + 1" @current-change="changeOrderPage" />
    </section>
  </div>
</template>
<style scoped>
.load-error { margin-bottom: 22px; }.trade-grid { display: grid; grid-template-columns: minmax(0, 1.25fr) minmax(360px, 1fr); gap: 38px; margin-bottom: 8px; }.market-panel { padding-right: 32px; border-right: 1px solid var(--line); }.market-panel :deep(.el-table__row) { cursor: pointer; }.stock-search { max-width: 220px; }.selected-quote { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 5px 0 27px; }.selected-price { color: #000; font-size: 34px; font-weight: 600; }.selected-price span { font-size: 15px; }.available-balance { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 0 0 24px; margin-bottom: 23px; border-bottom: 1px solid var(--gold); font-size: 15px; }.available-balance strong { font-size: 23px; font-weight: 600; }
.order-panel :deep(.el-form-item) { margin-bottom: 26px; }.order-fields { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }.trade-direction { display: flex; width: 100%; }.trade-direction :deep(.el-radio-button) { flex: 1; }.trade-direction :deep(.el-radio-button__inner) { width: 100%; padding: 13px; }
.quantity-shortcuts { display: flex; gap: 0; margin: -6px 0 18px; border: 1px solid var(--gold); }.quantity-shortcuts button { flex: 1; border: 0; border-right: 1px solid var(--gold); background: none; color: #000; padding: 10px 5px; font-size: 14px; }.quantity-shortcuts button:last-child { border-right: 0; }.quantity-shortcuts button:hover:not(:disabled) { background: #f0dda8; }.quantity-shortcuts button:disabled { background: #f4e8cb; }
.quantity-tools { display: flex; justify-content: space-between; gap: 12px; flex-wrap: wrap; align-items: center; margin-bottom: 22px; font-size: 14px; }.quantity-tools :deep(.el-button) { font-size: 14px; }.estimate { display: flex; align-items: center; justify-content: space-between; padding: 22px 0; border-top: 1px solid var(--gold); gap: 12px; font-size: 15px; }.estimate strong { font-size: 24px; }.submit-order { width: 100%; min-height: 49px !important; margin-top: 6px; }.submit-error { margin-top: 18px; }
.order-receipt { display: flex; align-items: center; gap: 13px; padding: 20px 0; color: #000; border-block: 1px solid var(--gold); margin-bottom: 24px; font-size: 15px; line-height: 1.8; }.order-receipt > .el-icon { font-size: 22px; }.order-receipt button { margin-left: auto; color: #000; border: 0; background: none; font-size: 25px; }
@media(max-width:1000px) { .trade-grid { grid-template-columns: 1fr; gap: 0; }.market-panel { padding-right: 0; border-right: 0; } }
@media(max-width:480px) { .stock-search { max-width: 100%; }.order-fields { grid-template-columns: 1fr; gap: 0; }.selected-price { font-size: 28px; }.estimate strong { font-size: 22px; } }
</style>
