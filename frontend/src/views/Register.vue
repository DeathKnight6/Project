<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import AuthShell from '@/components/AuthShell.vue'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({ name: '', loginName: '', passwd: '', confirm: '' })

const rules: FormRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  loginName: [
    { required: true, message: '请输入登录名', trigger: 'blur' },
    {
      pattern: /^[A-Za-z0-9_]{3,32}$/,
      message: '登录名需为 3-32 位字母、数字或下划线',
      trigger: 'blur'
    }
  ],
  passwd: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需为 6-32 位', trigger: 'blur' }
  ],
  confirm: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== form.passwd) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
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
    await userStore.register({
      name: form.name,
      loginName: form.loginName,
      passwd: form.passwd
    })
    ElMessage.success('注册成功，已自动登录')
    router.replace('/dashboard')
  } catch {
    // 错误提示已由 axios 响应拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthShell>
      <h1>注册</h1>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="handleSubmit">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" maxlength="64" autocomplete="name" clearable />
        </el-form-item>
        <el-form-item label="登录名" prop="loginName">
          <el-input v-model="form.loginName" autocomplete="username" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="passwd">
          <el-input v-model="form.passwd" type="password" autocomplete="new-password" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirm">
          <el-input v-model="form.confirm" type="password" autocomplete="new-password" show-password />
        </el-form-item>
        <el-button type="primary" :loading="loading" style="width: 100%" @click="handleSubmit">
          注册账号
        </el-button>
      </el-form>

      <div class="footer-link">
        <router-link to="/login">返回登录 →</router-link>
      </div>
  </AuthShell>
</template>
