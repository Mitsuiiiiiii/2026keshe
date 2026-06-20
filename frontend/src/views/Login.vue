<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="brand">
        <el-icon :size="28"><Connection /></el-icon>
        <h1>CampusLink</h1>
      </div>
      <p class="subtitle">校园竞赛组队与协作平台</p>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @submit.prevent>
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="账号" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="onSubmit"
          />
        </el-form-item>
        <el-button type="primary" :loading="loading" class="submit-btn" @click="onSubmit">
          登录
        </el-button>
      </el-form>
      <div class="auth-footer">
        没有账号？<router-link to="/register">去注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    const data = await login(form)
    userStore.setAuth(data.token, data.user)
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/teams')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #3b6ef5 0%, #6a93f8 100%);
}
.auth-card {
  width: 380px;
  background: #fff;
  border-radius: 12px;
  padding: 40px 32px 32px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
}
.brand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--cl-primary);
}
.brand h1 {
  font-size: 26px;
  margin: 0;
}
.subtitle {
  text-align: center;
  color: #909399;
  margin: 8px 0 28px;
}
.submit-btn {
  width: 100%;
}
.auth-footer {
  text-align: center;
  margin-top: 18px;
  font-size: 14px;
  color: #606266;
}
.auth-footer a {
  color: var(--cl-primary);
}
</style>
