<script setup lang="ts">
/** 资产概览用的数字卡片 */
defineProps<{
  label: string
  value: string
  hint?: string
  tone?: 'default' | 'brand' | 'up' | 'down'
}>()
</script>

<template>
  <div class="card stat">
    <div class="label">{{ label }}</div>
    <div class="value" :class="tone && tone !== 'default' ? tone : ''">{{ value }}</div>
    <div v-if="hint" class="hint">{{ hint }}</div>
  </div>
</template>

<style scoped>
.stat {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.label {
  font-size: 13px;
  color: #7a8699;
}

.value {
  font-size: 24px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.value.brand {
  color: var(--brand);
}

.value.up {
  color: var(--up);
}

.value.down {
  color: var(--down);
}

.hint {
  font-size: 12px;
  color: #9aa5b6;
}
</style>
