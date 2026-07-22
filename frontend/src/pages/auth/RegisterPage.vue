<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { MagicStick, Message, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const auth = useAuthStore()

const email = ref('')
const password = ref('')
const passwordConfirm = ref('')
const registerError = ref('')
const isLoading = ref(false)

async function handleRegister() {
  registerError.value = ''
  if (!email.value || !password.value) {
    registerError.value = '请填写邮箱和密码'
    return
  }
  if (password.value !== passwordConfirm.value) {
    registerError.value = '两次密码不一致'
    return
  }
  if (password.value.length < 6) {
    registerError.value = '密码长度至少 6 位'
    return
  }

  isLoading.value = true
  try {
    const ok = await auth.register(email.value, password.value)
    if (ok) {
      router.push('/')
    } else {
      registerError.value = '注册失败，邮箱可能已存在'
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
        <h1>创建账号</h1>
        <p>加入 Trip Designer，开启 AI 旅行设计之旅</p>
      </div>

      <el-form @submit.prevent="handleRegister" class="auth-form">
        <el-form-item>
          <div class="input-wrapper">
            <el-icon class="input-icon"><Message /></el-icon>
            <el-input
              v-model="email"
              placeholder="邮箱地址"
              size="large"
              @keyup.enter="handleRegister"
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
              @keyup.enter="handleRegister"
            />
          </div>
        </el-form-item>

        <el-form-item>
          <div class="input-wrapper">
            <el-icon class="input-icon"><Lock /></el-icon>
            <el-input
              v-model="passwordConfirm"
              type="password"
              placeholder="确认密码"
              size="large"
              show-password
              @keyup.enter="handleRegister"
            />
          </div>
        </el-form-item>

        <div v-if="registerError" class="register-error">
          <el-alert :title="registerError" type="error" show-icon :closable="false" />
        </div>

        <el-button
          type="primary"
          size="large"
          :loading="isLoading"
          class="submit-btn"
          @click="handleRegister"
        >
          创建账号
        </el-button>
      </el-form>

      <div class="auth-footer">
        已有账号？
        <el-button text class="login-link" @click="router.push('/login')">
          立即登录
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
  background: var(--secondary-color);
  top: -150px;
  right: -150px;
  animation: float 8s ease-in-out infinite;
}

.orb-2 {
  width: 400px;
  height: 400px;
  background: var(--primary-color);
  bottom: -100px;
  left: -100px;
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
  background: linear-gradient(135deg, var(--secondary-color), #f43f5e);
  box-shadow: 0 8px 20px rgba(var(--secondary-rgb), 0.3);
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
  border-color: var(--secondary-color);
  background: var(--bg-primary);
  box-shadow: 0 0 0 3px rgba(var(--secondary-rgb), 0.1);
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

.register-error {
  margin-bottom: 4px;
}

:deep(.el-alert--error) {
  background: rgba(var(--error-rgb), 0.08);
  border: 1px solid rgba(var(--error-rgb), 0.2);
  border-radius: var(--radius-md);
}

.submit-btn {
  width: 100%;
  background: var(--secondary-color);
  border: none;
  border-radius: var(--radius-md);
  height: 48px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.5px;
  transition: all var(--transition-base);
  box-shadow: 0 4px 12px rgba(var(--secondary-rgb), 0.25);
}

.submit-btn:hover:not(:disabled) {
  background: #db2777;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(var(--secondary-rgb), 0.35);
}

.auth-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: var(--text-muted);
}

.login-link {
  color: var(--secondary-color);
  font-weight: 600;
  margin-left: 4px;
}

.login-link:hover {
  color: #db2777;
}
</style>
