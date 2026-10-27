<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import AuthShell from '@/components/AuthShell.vue'
import { User, Lock, ArrowRight } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ loginName: '', passwd: '' })

const rules: FormRules = {
  loginName: [{ required: true, message: '请输入登录名', trigger: 'blur' }],
  passwd: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleSubmit() {
  if (!formRef.value || loading.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  loading.value = true
  try {
    await userStore.login({ ...form })
    ElMessage.success('登录成功')
    const redirect = route.query.redirect
    router.replace(typeof redirect === 'string' ? redirect : '/dashboard')
  } catch {
    // 错误提示已由 axios 响应拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthShell>
      <h1>登录</h1>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="handleSubmit">
        <el-form-item label="登录名" prop="loginName">
          <el-input v-model="form.loginName" :prefix-icon="User" autocomplete="username" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="passwd">
          <el-input v-model="form.passwd" type="password" :prefix-icon="Lock" autocomplete="current-password" show-password />
        </el-form-item>
        <el-button type="primary" :loading="loading" style="width: 100%" @click="handleSubmit">
          登录账户 <el-icon style="margin-left: 10px"><ArrowRight /></el-icon>
        </el-button>
      </el-form>

      <div class="footer-link">
        <router-link to="/register">注册账号 →</router-link>
      </div>

      <div class="demo-accounts">
        <button type="button" @click="form.loginName = 'user001'; form.passwd = '123456'">普通演示账号</button>
        <button type="button" @click="form.loginName = 'admin'; form.passwd = 'admin123'">管理员演示账号</button>
      </div>
  </AuthShell>
</template>
<style scoped>
.demo-accounts { display: flex; gap: 20px; justify-content: space-between; margin-top: 34px; border-top: 1px solid var(--gold); padding-top: 24px; }
.demo-accounts button { border: 0; border-bottom: 1px solid var(--gold); padding: 8px 0; color: #000; background: none; font-size: 14px; }
.demo-accounts button:hover { border-bottom-color: var(--red); }
</style>
