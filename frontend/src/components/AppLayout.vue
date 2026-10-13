<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 菜单高亮跟随当前路由 */
const activeMenu = computed(() => route.path)

async function handleLogout() {
  await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '退出',
    cancelButtonText: '取消',
    type: 'warning'
  })
  userStore.logout()
  router.replace('/login')
}
</script>

<template>
  <el-container class="layout">
    <el-header class="header">
      <div class="brand">模拟股票交易系统</div>

      <el-menu class="nav" mode="horizontal" :default-active="activeMenu" :ellipsis="false" router>
        <el-menu-item index="/dashboard">资产概览</el-menu-item>
        <el-menu-item index="/trading">交易下单</el-menu-item>
        <el-menu-item index="/portfolio">我的持仓</el-menu-item>
      </el-menu>

      <div class="user">
        <span class="user-name">{{ userStore.displayName }}</span>
        <el-tag v-if="userStore.isAdmin" size="small" type="warning" effect="plain">管理员</el-tag>
        <el-button link type="primary" @click="handleLogout">退出</el-button>
      </div>
    </el-header>

    <el-main class="main">
      <router-view />
    </el-main>
  </el-container>
</template>

<style scoped>
.layout {
  min-height: 100vh;
}

.header {
  display: flex;
  align-items: center;
  gap: 24px;
  height: 60px;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #e8ecf3;
}

.brand {
  font-size: 16px;
  font-weight: 700;
  color: var(--brand);
  white-space: nowrap;
}

.nav {
  flex: 1;
  border-bottom: none;
}

.user {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  white-space: nowrap;
}

.user-name {
  color: #1f2733;
}

.main {
  padding: 0;
}
</style>
