<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { SwitchButton } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const navigation = [
  { path: '/dashboard', label: '资产概览' },
  { path: '/trading', label: '交易中心' },
  { path: '/portfolio', label: '持仓与订单' }
]

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定退出当前账号吗？', '退出登录', {
      confirmButtonText: '退出', cancelButtonText: '取消'
    })
    userStore.logout()
    router.replace('/login')
  } catch { /* 用户取消 */ }
}
</script>

<template>
  <div class="app-shell">
    <header class="masthead">
      <router-link to="/dashboard" class="brand-lockup" aria-label="模拟股票交易系统首页">
        <span class="brand-seal" aria-hidden="true">股</span>
        <strong>模拟股票交易系统</strong>
      </router-link>
      <nav aria-label="主导航" class="main-nav">
        <router-link v-for="item in navigation" :key="item.path" :to="item.path"
          :class="{ selected: route.path === item.path }"
          :aria-current="route.path === item.path ? 'page' : undefined">
          {{ item.label }}
        </router-link>
      </nav>
      <div class="account-menu">
        <span>{{ userStore.displayName }}</span>
        <button aria-label="退出登录" title="退出登录" @click="handleLogout"><el-icon><SwitchButton /></el-icon></button>
      </div>
    </header>
    <div class="masthead-rule" aria-hidden="true"><span></span></div>
    <main><router-view /></main>
  </div>
</template>

<style scoped>
.app-shell { min-height: 100vh; background: var(--paper); }
.masthead { display: flex; align-items: center; gap: 50px; min-height: 108px; padding: 0 48px; background: var(--red) url('../assets/gold-clouds.svg') center 38% / 1200px auto; color: #fff; }
.brand-lockup { display: flex; align-items: center; gap: 16px; color: #fff; flex-shrink: 0; text-decoration: none; }
.brand-lockup strong { font-family: 'STSong', 'SimSun', serif; font-size: 28px; letter-spacing: 2px; }
.brand-seal { display: grid; place-items: center; width: 44px; height: 44px; background: var(--gold); color: #000; font-family: 'SimSun', serif; font-size: 30px; border: 2px solid #edcf83; outline: 1px solid var(--gold); outline-offset: 3px; }
.main-nav { display: flex; align-self: stretch; gap: 38px; margin-left: auto; }
.main-nav a { display: flex; align-items: center; border-bottom: 4px solid transparent; padding: 7px 0 3px; color: #fff; font-size: 17px; white-space: nowrap; text-decoration: none; }
.main-nav a.selected { border-bottom-color: var(--gold); font-weight: 700; }
.main-nav a:hover { border-bottom-color: #edcf83; }
.account-menu { display: flex; align-items: center; gap: 18px; margin-left: 20px; color: #fff; font-size: 15px; white-space: nowrap; }
.account-menu button { display: flex; padding: 9px 0 9px 16px; border: 0; border-left: 1px solid var(--gold); background: none; color: #fff; font-size: 20px; }
.masthead-rule { height: 8px; background: var(--gold); border-top: 2px solid #f1d98f; border-bottom: 1px solid #8f671c; }
@media(min-width:1650px) { .masthead { padding-inline: max(48px, calc((100vw - 1484px) / 2)); } }
@media(max-width:1000px) { .masthead { padding-inline: 28px; gap: 24px; }.main-nav { gap: 25px; }.brand-lockup strong { font-size: 24px; letter-spacing: 1px; }.account-menu { gap: 10px; margin-left: 5px; } }
@media(max-width:700px) {
  .masthead { padding: 24px 18px 0; gap: 24px 12px; flex-wrap: wrap; }
  .brand-lockup { gap: 14px; }.brand-lockup strong { font-size: 20px; letter-spacing: .5px; }.brand-seal { width: 33px; height: 33px; font-size: 25px; }
  .account-menu { margin-left: auto; font-size: 14px; }.account-menu button { padding-left: 10px; }
  .main-nav { order: 3; flex-basis: 100%; margin: 0; justify-content: space-between; gap: 10px; }
  .main-nav a { padding: 0 0 18px; font-size: 16px; }
}
</style>
