<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  House,
  ChatDotRound,
  MagicStick,
  Notebook,
  User,
  ArrowRight,
  TrendCharts,
  Headset,
  Picture,
  ChatLineSquare,
  UserFilled,
  Location,
  DataLine,
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const menuItems = [
  { name: 'Dashboard', path: '/', icon: House },
  { name: 'AI 生成行程', path: '/ai/generate', icon: MagicStick },
  { name: 'AI 旅行顾问', path: '/advisor', icon: Headset },
  { name: '多模态生成', path: '/multimodal', icon: Picture },
  { name: '旅行社区', path: '/community', icon: ChatLineSquare },
  { name: '旅行组队', path: '/teams', icon: UserFilled },
  { name: '智能打卡', path: '/checkins', icon: Location },
  { name: '价格监测', path: '/price-monitors', icon: TrendCharts },
  { name: '旅行统计', path: '/statistics', icon: DataLine },
  { name: '体验分享', path: '/experiences', icon: Notebook },
  { name: '偏好记忆', path: '/memory/preferences', icon: User },
]

const activeMenu = computed(() => route.path)

function navigate(path: string) {
  router.push(path)
}

function handleLogout() {
  auth.logout()
  router.push({ name: 'Login' })
}
</script>

<template>
  <aside class="sidebar">
    <div class="sidebar-header">
      <div class="logo-container">
        <div class="logo-icon">
          <el-icon :size="22" color="#fff"><MagicStick /></el-icon>
        </div>
        <div class="brand-text">
          <span class="brand-title">Trip Designer</span>
          <span class="brand-subtitle">AI 旅行设计师</span>
        </div>
      </div>
    </div>

    <nav class="sidebar-nav">
      <div
        v-for="item in menuItems"
        :key="item.path"
        class="nav-item"
        :class="{ active: activeMenu === item.path }"
        @click="navigate(item.path)"
      >
        <div class="nav-icon-wrapper">
          <el-icon :size="18"><component :is="item.icon" /></el-icon>
        </div>
        <span class="nav-text">{{ item.name }}</span>
      </div>
    </nav>

    <div class="sidebar-footer">
      <div class="user-info" v-if="auth.user">
        <el-avatar :size="38" class="user-avatar">
          {{ auth.user.email[0].toUpperCase() }}
        </el-avatar>
        <div class="user-detail">
          <div class="user-name">{{ auth.user.email.split('@')[0] }}</div>
          <div class="user-email">{{ auth.user.email }}</div>
        </div>
        <el-tooltip content="退出登录" placement="top">
          <el-button :icon="ArrowRight" class="logout-btn" circle @click="handleLogout"></el-button>
        </el-tooltip>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  width: 240px;
  background: var(--sidebar-bg);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  height: 100%;
  flex-shrink: 0;
  position: relative;
}

.sidebar-header {
  padding: 20px 20px;
  border-bottom: 1px solid var(--border-color-light);
}

.logo-container {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
  box-shadow: 0 4px 12px rgba(var(--primary-rgb), 0.3);
}

.brand-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.3px;
  line-height: 1.2;
}

.brand-subtitle {
  font-size: 11px;
  color: var(--text-muted);
  font-weight: 500;
}

.sidebar-nav {
  flex: 1;
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow-y: auto;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  transition: all var(--transition-fast);
  position: relative;
}

.nav-item:hover {
  background: var(--primary-bg);
  color: var(--primary-color);
}

.nav-item.active {
  background: var(--primary-color);
  color: white;
  box-shadow: 0 4px 12px rgba(var(--primary-rgb), 0.25);
}

.nav-icon-wrapper {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}

.nav-item.active .nav-icon-wrapper {
  background: rgba(255, 255, 255, 0.2);
}

.nav-text {
  flex: 1;
  white-space: nowrap;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid var(--border-color-light);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.user-info:hover {
  background: var(--bg-tertiary);
}

.user-avatar {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
  font-size: 14px;
  font-weight: 600;
  color: white;
  flex-shrink: 0;
}

.user-detail {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.3;
}

.user-email {
  font-size: 11px;
  color: var(--text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-top: 2px;
}

.logout-btn {
  color: var(--text-muted);
  border: none;
  background: transparent;
  flex-shrink: 0;
}

.logout-btn:hover {
  color: var(--error-color);
  background: rgba(var(--error-rgb), 0.1);
}
</style>
