<script setup lang="ts">
import { computed } from 'vue'
import type { PortfolioSummary } from '@/api/types'
import { money } from '@/utils/format'
const props = defineProps<{ summary: PortfolioSummary | null; hidden?: boolean }>()
const slices = computed(() => [
  { label: '持仓市值', value: Number(props.summary?.marketValue || 0), color: '#b51622' },
  { label: '可用资金', value: Number(props.summary?.cnyFree || 0), color: '#d3ab4e' },
  { label: '冻结资金', value: Number(props.summary?.cnyFrozen || 0), color: '#f0dda8' }
])
const total = computed(() => slices.value.reduce((sum, item) => sum + item.value, 0))
const stockPercent = computed(() => total.value > 0 ? slices.value[0].value / total.value * 100 : 0)
const gradient = computed(() => {
  if (props.hidden || !total.value) return '#f0dda8'
  let cursor = 0
  return `conic-gradient(${slices.value.map((item) => {
    const from = cursor
    cursor += item.value / total.value * 100
    return `${item.color} ${from}% ${cursor}%`
  }).join(',')})`
})
</script>
<template>
  <section class="card allocation">
    <div class="section-head"><h3 class="card-title">资产分布</h3></div>
    <div class="allocation-content">
      <div class="donut" :style="{ background: gradient }" role="img"
        :aria-label="hidden ? '资产分布已隐藏' : summary ? `股票仓位 ${stockPercent.toFixed(1)}%` : '资产待加载'">
        <div><strong class="number">{{ hidden ? '•••' : summary ? `${stockPercent.toFixed(1)}%` : '--' }}</strong><span>股票仓位</span></div>
      </div>
      <div class="legend">
        <div v-for="item in slices" :key="item.label"><span class="legend-dot" :style="{ background: item.color }"></span><span>{{ item.label }}<strong class="number">{{ hidden ? '••••' : summary ? money(item.value) : '--' }}</strong></span></div>
      </div>
    </div>
  </section>
</template>
<style scoped>
.allocation-content { display: flex; align-items: center; justify-content: center; gap: 30px; padding: 6px 0; }
.donut { width: 158px; height: 158px; border-radius: 50%; padding: 18px; flex-shrink: 0; }
.donut > div { height: 100%; border-radius: 50%; background: var(--paper); display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; color: #000; }
.donut strong { font-size: 27px; font-weight: 600; letter-spacing: -1px; }.donut span { font-size: 14px; }
.legend { display: grid; gap: 20px; }.legend > div { display: flex; gap: 10px; align-items: flex-start; font-size: 14px; color: #000; }.legend-dot { width: 9px; height: 9px; margin-top: 5px; flex-shrink: 0; }.legend strong { display: block; font-size: 17px; font-weight: 600; color: #000; margin-top: 6px; }
@media(max-width:1100px) { .allocation-content { gap: 22px; }.donut { width: 140px; height: 140px; padding: 16px; }.donut strong { font-size: 24px; } }
@media(max-width:380px) { .allocation-content { gap: 14px; }.donut { width: 128px; height: 128px; }.legend strong { font-size: 15px; } }
</style>
