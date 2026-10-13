<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { cancelOrder, fetchOrders, fetchStocks, fetchSummary, placeOrder } from '@/api/trade'
import type { OrderType, OrderView, StockView } from '@/api/types'
import { dateTime, money, orderTypeText, price } from '@/utils/format'

const route = useRoute()

const stocks = ref<StockView[]>([])
const orders = ref<OrderView[]>([])
const availableCash = ref(0)
const loading = ref(false)
const submitting = ref(false)

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
  try {
    const [list, orderPage, summary] = await Promise.all([
      fetchStocks(),
      fetchOrders(0, 50),
      fetchSummary()
    ])
    stocks.value = list
    orders.value = orderPage.content
    availableCash.value = summary.cnyFree

    // 从「资产概览」点「去交易」跳过来时，自动带上股票并填好现价
    const fromQuery = Number(route.query.stockId)
    if (fromQuery && list.some((s) => s.id === fromQuery)) {
      pickStock(fromQuery)
    } else if (!form.stockId && list.length > 0) {
      pickStock(list[0].id)
    }
  } catch {
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
  if (!formRef.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
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
  await ElMessageBox.confirm(
    `确定撤销订单 #${row.id}（${row.stockName} ${orderTypeText(row.type)} ${row.undealed} 股未成交）吗？`,
    '撤单确认',
    { confirmButtonText: '撤单', cancelButtonText: '再想想', type: 'warning' }
  )
  try {
    await cancelOrder(row.id)
    ElMessage.success('撤单成功，冻结的资金或股票已退回')
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
      <h2>交易下单</h2>
      <span class="tip">可用资金：{{ money(availableCash) }} 元</span>
    </div>

    <div v-loading="loading" class="trade-grid">
      <div class="card">
        <h3 class="card-title">选择股票</h3>
        <el-table
          :data="stocks"
          highlight-current-row
          :current-row-key="form.stockId ?? undefined"
          row-key="id"
          @current-change="(row: StockView | null) => row && pickStock(row.id)"
        >
          <el-table-column prop="code" label="代码" width="100" />
          <el-table-column prop="name" label="名称" min-width="120" />
          <el-table-column label="最新价" width="110" align="right">
            <template #default="{ row }">{{ price(row.currentPrice) }}</template>
          </el-table-column>
        </el-table>
      </div>

      <div class="card">
        <h3 class="card-title">下单</h3>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <el-form-item label="股票" prop="stockId">
            <el-select v-model="form.stockId" placeholder="请选择股票" style="width: 100%" @change="pickStock">
              <el-option
                v-for="s in stocks"
                :key="s.id"
                :label="`${s.name}（${s.code}）`"
                :value="s.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="方向" prop="type">
            <el-radio-group v-model="form.type">
              <el-radio-button value="BUY">买入</el-radio-button>
              <el-radio-button value="SELL">卖出</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="价格" prop="price">
            <el-input-number
              v-model="form.price"
              :min="0.01"
              :step="0.01"
              :precision="2"
              controls-position="right"
              style="width: 100%"
            />
          </el-form-item>

          <el-form-item label="数量（股）" prop="amount">
            <el-input-number
              v-model="form.amount"
              :min="1"
              :step="100"
              controls-position="right"
              style="width: 100%"
            />
          </el-form-item>

          <div class="estimate">
            <span class="muted">预估{{ form.type === 'BUY' ? '支出' : '收入' }}</span>
            <strong>{{ money(estimate) }} 元</strong>
          </div>

          <el-alert
            v-if="selectedStock"
            class="stock-tip"
            type="info"
            :closable="false"
            :title="`${selectedStock.name} 最新价 ${price(selectedStock.currentPrice)} 元`"
          />

          <el-button
            type="primary"
            :loading="submitting"
            style="width: 100%; margin-top: 14px"
            @click="handleSubmit"
          >
            提交{{ form.type === 'BUY' ? '买入' : '卖出' }}委托
          </el-button>
          <p class="rule-tip">
            买卖价格交叉时立即成交；未成交部分挂在委托簿等待撮合，可随时撤单。
          </p>
        </el-form>
      </div>
    </div>

    <div class="card">
      <h3 class="card-title">可撤销的挂单（{{ cancelableOrders.length }}）</h3>
      <el-table :data="cancelableOrders" stripe>
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
        <el-table-column label="下单时间" width="170">
          <template #default="{ row }">{{ dateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center">
          <template #default="{ row }">
            <el-button link type="danger" @click="handleCancel(row)">撤单</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <span class="muted">当前没有未成交的挂单</span>
        </template>
      </el-table>
    </div>
  </div>
</template>

<style scoped>
.trade-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(320px, 0.85fr);
  gap: 18px;
  align-items: start;
  margin-bottom: 18px;
}

@media (max-width: 1000px) {
  .trade-grid {
    grid-template-columns: 1fr;
  }
}

.estimate {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 8px;
  background: #f4f6fa;
  font-size: 14px;
}

.stock-tip {
  margin-top: 12px;
}

.rule-tip {
  margin: 10px 0 0;
  font-size: 12px;
  line-height: 1.7;
  color: #9aa5b6;
}
</style>
