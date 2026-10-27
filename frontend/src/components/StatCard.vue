<script setup lang="ts">
defineProps<{ label: string; value: string }>()
</script>
<template>
  <div class="stat"><div class="label">{{ label }}</div><div class="value number">{{ value }}</div></div>
</template>
<style scoped>
.stat { min-width: 0; padding: 28px 26px; border-right: 1px solid var(--line); }.stat:first-child { padding-left: 0; }.stat:last-child { border-right: 0; }.label { color: #000; font-size: 15px; }.value { color: #000; font-size: clamp(24px, 2.4vw, 34px); font-weight: 600; letter-spacing: -.8px; margin-top: 18px; overflow-wrap: anywhere; }
@media(max-width:700px) { .stat { padding: 22px 16px; border-bottom: 1px solid var(--line); }.stat:nth-child(odd) { padding-left: 0; }.stat:nth-child(even) { border-right: 0; }.value { font-size: 25px; margin-top: 13px; } }
</style>
