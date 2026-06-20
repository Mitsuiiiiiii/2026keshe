<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="brand">
        <el-icon :size="28"><Connection /></el-icon>
        <h1>CampusLink</h1>
      </div>
      <p class="subtitle">注册新账号</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" placeholder="3-50 位账号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="6-50 位密码" show-password />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="昵称" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="学院">
              <el-input v-model="form.college" placeholder="如 软件学院" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="专业">
              <el-input v-model="form.major" placeholder="如 软件工程" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="年级">
              <el-input v-model.number="form.grade" placeholder="如 2023" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="form.email" placeholder="可选" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-button type="primary" :loading="loading" class="submit-btn" @click="onSubmit">
          注册
        </el-button>
      </el-form>
      <div class="auth-footer">
        已有账号？<router-link to="/login">去登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/auth'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
  nickname: '',
  college: '',
  major: '',
  grade: null,
  email: ''
})
const rules = {
  username: [{ required: true, min: 3, max: 50, message: '账号长度 3-50', trigger: 'blur' }],
  password: [{ required: true, min: 6, max: 50, message: '密码长度 6-50', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }]
}

async function onSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await register(form)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
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
  padding: 24px 0;
}
.auth-card {
  width: 420px;
  background: #fff;
  border-radius: 12px;
  padding: 36px 32px 28px;
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
  margin: 8px 0 20px;
}
.submit-btn {
  width: 100%;
}
.auth-footer {
  text-align: center;
  margin-top: 16px;
  font-size: 14px;
  color: #606266;
}
.auth-footer a {
  color: var(--cl-primary);
}
</style>
