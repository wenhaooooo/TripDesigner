<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { MagicStick, Message, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const email = ref('')
const password = ref('')
const loginError = ref('')
const isLoading = ref(false)

async function handleLogin() {
  loginError.value = ''
  if (!email.value || !password.value) {
    loginError.value = '请填写邮箱和密码'
    return
  }

  isLoading.value = true
  try {
    const ok = await auth.login(email.value, password.value)
    if (ok) {
      const redirect = (route.query.redirect as string) || '/'
      router.push(redirect)
    } else {
      loginError.value = '邮箱或密码错误'
    }
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-visual">
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
      <div class="orb orb-3"></div>
    </div>

    <div class="auth-card animate-fade-in-up">
      <div class="auth-header">
        <div class="logo-wrapper">
          <div class="logo-icon">
            <el-icon :size="32" color="#fff"><MagicStick /></el-icon>
          </div>
        </div>
        <h1>欢迎回来</h1>
        <p>登录 Trip Designer，开启智能旅行规划</p>
      </div>

      <el-form @submit.prevent="handleLogin" class="auth-form">
        <el-form-item>
          <div class="input-wrapper">
            <el-icon class="input-icon"><Message /></el-icon>
            <el-input
              v-model="email"
              placeholder="邮箱地址"
              size="large"
              @keyup.enter="handleLogin"
            />
          </div>
        </el-form-item>

        <el-form-item>
          <div class="input-wrapper">
            <el-icon class="input-icon"><Lock /></el-icon>
            <el-input
              v-model="password"
              type="password"
              placeholder="密码"
              size="large"
              show-password
              @keyup.enter="handleLogin"
            />
          </div>
        </el-form-item>

        <div v-if="loginError" class="login-error">
          <el-alert :title="loginError" type="error" show-icon :closable="false" />
        </div>

        <el-button
          type="primary"
          size="large"
          :loading="isLoading"
          class="login-btn"
          @click="handleLogin"
        >
          登录
        </el-button>
      </el-form>

      <div class="auth-footer">
        还没有账号？
        <el-button text class="register-btn" @click="router.push('/register')">
          立即注册
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f8fafc 0%, #eef2f7 100%);
  position: relative;
  overflow: hidden;
}

.auth-visual {
  position: absolute;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  opacity: 0.3;
}

.orb-1 {
  width: 500px;
  height: 500px;
  background: var(--primary-color);
  top: -150px;
  left: -150px;
  animation: float 8s ease-in-out infinite;
}

.orb-2 {
  width: 400px;
  height: 400px;
  background: var(--secondary-color);
  bottom: -100px;
  right: -100px;
  animation: float 10s ease-in-out infinite reverse;
}

.orb-3 {
  width: 350px;
  height: 350px;
  background: var(--accent-color);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation: float 12s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-20px); }
}

.auth-card {
  width: 420px;
  padding: 48px;
  background: var(--bg-primary);
  border-radius: var(--radius-xl);
  border: 1px solid var(--border-color);
  box-shadow: 0 20px 50px -10px rgba(15, 23, 42, 0.1), 0 10px 20px -5px rgba(15, 23, 42, 0.05);
  position: relative;
  z-index: 10;
}

.auth-header {
  text-align: center;
  margin-bottom: 32px;
}

.logo-wrapper {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
}

.logo-icon {
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
  box-shadow: 0 8px 20px rgba(var(--primary-rgb), 0.3);
}

.auth-header h1 {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.5px;
}

.auth-header p {
  font-size: 14px;
  color: var(--text-muted);
  margin-top: 8px;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.input-wrapper {
  position: relative;
  background: var(--input-bg);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
  transition: all var(--transition-fast);
}

.input-wrapper:focus-within {
  border-color: var(--primary-color);
  background: var(--bg-primary);
  box-shadow: 0 0 0 3px rgba(var(--primary-rgb), 0.1);
}

.input-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-muted);
  z-index: 1;
}

:deep(.el-input__wrapper) {
  background: transparent !important;
  box-shadow: none !important;
  border: none !important;
  padding-left: 40px;
}

:deep(.el-input__inner) {
  background: transparent !important;
  color: var(--text-primary) !important;
  font-size: 15px;
  height: 48px;
}

:deep(.el-input__placeholder) {
  color: var(--text-muted) !important;
}

.login-error {
  margin-bottom: 4px;
}

:deep(.el-alert--error) {
  background: rgba(var(--error-rgb), 0.08);
  border: 1px solid rgba(var(--error-rgb), 0.2);
  border-radius: var(--radius-md);
}

.login-btn {
  width: 100%;
  background: var(--primary-color);
  border: none;
  border-radius: var(--radius-md);
  height: 48px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.5px;
  transition: all var(--transition-base);
  box-shadow: 0 4px 12px rgba(var(--primary-rgb), 0.25);
}

.login-btn:hover:not(:disabled) {
  background: var(--primary-dark);
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(var(--primary-rgb), 0.35);
}

.auth-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: var(--text-muted);
}

.register-btn {
  color: var(--primary-color);
  font-weight: 600;
  margin-left: 4px;
}

.register-btn:hover {
  color: var(--primary-dark);
}
</style>
