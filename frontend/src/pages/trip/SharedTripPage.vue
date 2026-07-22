<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { shareApi } from '@/api/share'
import type { TripDetailVO } from '@/types/api'

const route = useRoute()
const token = computed(() => String(route.params.token || ''))

const loading = ref(true)
const error = ref<string>('')
const trip = ref<TripDetailVO | null>(null)

// 活动分类配置：颜色 + 中文标签
const categoryConfig: Record<string, { color: string; label: string }> = {
  sightseeing: { color: '#1890ff', label: '观光' },
  dining: { color: '#fa8c16', label: '餐饮' },
  transport: { color: '#52c41a', label: '交通' },
  accommodation: { color: '#722ed1', label: '住宿' },
  shopping: { color: '#eb2f96', label: '购物' },
  other: { color: '#999999', label: '其他' },
}

function getCategoryColor(category?: string): string {
  if (!category) return '#999999'
  return categoryConfig[category.toLowerCase()]?.color || '#999999'
}

function getCategoryLabel(category?: string): string {
  if (!category) return '其他'
  return categoryConfig[category.toLowerCase()]?.label || category
}

function formatTime(timeStr?: string): string {
  if (!timeStr || timeStr === '--:--') return '--:--'
  return timeStr.slice(0, 5)
}

function formatBudget(budget?: number): string {
  if (budget == null) return '--'
  return `¥${budget.toLocaleString()}`
}

function statusLabel(status?: string): string {
  const map: Record<string, string> = {
    draft: '草稿',
    planned: '已规划',
    ongoing: '进行中',
    completed: '已完成',
    cancelled: '已取消',
  }
  if (!status) return ''
  return map[status.toLowerCase()] || status
}

function statusType(status?: string): 'success' | 'warning' | 'info' | 'primary' | 'danger' {
  const map: Record<string, 'success' | 'warning' | 'info' | 'primary' | 'danger'> = {
    draft: 'info',
    planned: 'primary',
    ongoing: 'warning',
    completed: 'success',
    cancelled: 'danger',
  }
  if (!status) return 'info'
  return map[status.toLowerCase()] || 'info'
}

async function fetchTrip() {
  if (!token.value) {
    error.value = '分享链接无效'
    loading.value = false
    return
  }
  loading.value = true
  error.value = ''
  try {
    const res = await shareApi.getSharedTrip(token.value)
    trip.value = res.data
  } catch (e: any) {
    const status = e?.response?.status
    if (status === 404) {
      error.value = '分享链接不存在或已被撤销'
    } else if (status === 410) {
      error.value = '分享链接已过期'
    } else if (status === 403) {
      error.value = '分享链接已被禁用'
    } else {
      error.value = e?.response?.data?.message || '加载失败，请稍后重试'
    }
  } finally {
    loading.value = false
  }
}

onMounted(fetchTrip)
</script>

<template>
  <div class="shared-trip-page">
    <div class="shared-container">
      <!-- 加载中：骨架屏 -->
      <div v-if="loading" class="content-wrapper">
        <el-skeleton :rows="0" animated>
          <template #template>
            <el-skeleton-item variant="h1" style="width: 60%; height: 32px;" />
            <el-skeleton-item variant="text" style="width: 40%; margin-top: 12px;" />
            <el-skeleton-item variant="text" style="width: 80%; margin-top: 8px;" />
            <div style="margin-top: 24px;">
              <el-skeleton-item variant="rect" style="height: 120px; border-radius: 12px;" />
            </div>
            <div style="margin-top: 16px;">
              <el-skeleton-item variant="rect" style="height: 160px; border-radius: 12px;" />
            </div>
            <div style="margin-top: 16px;">
              <el-skeleton-item variant="rect" style="height: 120px; border-radius: 12px;" />
            </div>
          </template>
        </el-skeleton>
      </div>

      <!-- 错误状态 -->
      <div v-else-if="error" class="error-state">
        <el-empty :description="error">
          <template #image>
            <div class="error-icon">
              <el-icon :size="64" color="#ffcccc">
                <svg viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 15h2v-2h-2v2zm0-4h2V7h-2v6z" />
                </svg>
              </el-icon>
            </div>
          </template>
          <p class="error-hint">该行程可能已过期、被撤销或链接无效</p>
          <el-button type="primary" round @click="fetchTrip">重新加载</el-button>
        </el-empty>
      </div>

      <!-- 行程内容 -->
      <template v-else-if="trip">
        <div class="content-wrapper">
          <!-- 顶部行程信息卡片 -->
          <el-card class="trip-header-card" shadow="never" :body-style="{ padding: '28px 32px' }">
            <div class="trip-header">
              <div class="trip-title-row">
                <h1 class="trip-title">{{ trip.title }}</h1>
                <el-tag
                  v-if="trip.status"
                  :type="statusType(trip.status)"
                  effect="light"
                  round
                  size="default"
                >
                  {{ statusLabel(trip.status) }}
                </el-tag>
              </div>
              <div class="trip-meta">
                <div class="meta-item">
                  <el-icon><svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5a2.5 2.5 0 010-5 2.5 2.5 0 010 5z"/></svg></el-icon>
                  <span>{{ trip.destinationName || '未指定' }}</span>
                </div>
                <div class="meta-item">
                  <el-icon><svg viewBox="0 0 24 24" fill="currentColor"><path d="M19 4h-1V2h-2v2H8V2H6v2H5c-1.11 0-2 .9-2 2v14c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 16H5V10h14v10zM7 12h5v5H7z"/></svg></el-icon>
                  <span>{{ trip.startDate }} ~ {{ trip.endDate }}</span>
                </div>
                <div class="meta-item">
                  <el-icon><svg viewBox="0 0 24 24" fill="currentColor"><path d="M11.8 10.9c-2.27-.59-3-1.2-3-2.15 0-1.09 1.01-1.85 2.7-1.85 1.78 0 2.44.85 2.5 2.1h2.21c-.07-1.72-1.12-3.3-3.21-3.81V3h-3v2.16c-1.94.42-3.5 1.68-3.5 3.61 0 2.31 1.91 3.46 4.7 4.13 2.5.6 3 1.48 3 2.41 0 .69-.49 1.79-2.7 1.79-2.06 0-2.87-.92-2.98-2.1h-2.2c.12 2.19 1.76 3.42 3.68 3.83V21h3v-2.15c1.95-.37 3.5-1.5 3.5-3.55 0-2.84-2.43-3.81-4.7-4.4z"/></svg></el-icon>
                  <span>预算 {{ formatBudget(trip.budget) }}</span>
                </div>
              </div>
              <p v-if="trip.description" class="trip-description">{{ trip.description }}</p>
            </div>
          </el-card>

          <!-- 每日行程 -->
          <div class="days-list">
            <el-card
              v-for="day in trip.days || []"
              :key="day.id"
              class="day-card"
              shadow="never"
              :body-style="{ padding: '0' }"
            >
              <div class="day-card-header">
                <div class="day-badge">Day {{ day.dayNumber }}</div>
                <div class="day-card-title">
                  <span v-if="day.date" class="day-date">{{ day.date }}</span>
                  <span v-if="day.title" class="day-title-text">{{ day.title }}</span>
                </div>
                <span v-if="day.activities?.length" class="activity-count">
                  {{ day.activities.length }} 个活动
                </span>
              </div>

              <div v-if="day.description" class="day-description">
                {{ day.description }}
              </div>

              <div class="activities-list">
                <div
                  v-for="activity in day.activities || []"
                  :key="activity.id"
                  class="activity-item"
                >
                  <div class="activity-time-block">
                    <span class="time-start">{{ formatTime(activity.startTime) }}</span>
                    <span class="time-divider">—</span>
                    <span class="time-end">{{ formatTime(activity.endTime) }}</span>
                  </div>
                  <div class="activity-content">
                    <div class="activity-header">
                      <span class="activity-name">{{ activity.name }}</span>
                      <el-tag
                        v-if="activity.category"
                        size="small"
                        effect="light"
                        :color="getCategoryColor(activity.category)"
                        style="color: #fff; border: none;"
                      >
                        {{ getCategoryLabel(activity.category) }}
                      </el-tag>
                    </div>
                    <div v-if="activity.place" class="activity-place">
                      <el-icon><svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5a2.5 2.5 0 010-5 2.5 2.5 0 010 5z"/></svg></el-icon>
                      <span>{{ activity.place }}</span>
                    </div>
                    <p v-if="activity.description" class="activity-description">
                      {{ activity.description }}
                    </p>
                    <p v-if="activity.notes" class="activity-notes">
                      <span class="notes-label">备注：</span>{{ activity.notes }}
                    </p>
                  </div>
                </div>
                <el-empty
                  v-if="!day.activities || day.activities.length === 0"
                  description="暂无活动安排"
                  :image-size="60"
                />
              </div>
            </el-card>

            <el-empty
              v-if="!trip.days || trip.days.length === 0"
              description="该行程暂无日程安排"
            />
          </div>

          <!-- 底部水印 -->
          <div class="watermark">
            <span class="watermark-text">Powered by</span>
            <span class="watermark-brand">Trip Designer</span>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.shared-trip-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f0f5ff 0%, #f5f7fa 100%);
  padding: 32px 16px;
  box-sizing: border-box;
}

.shared-container {
  max-width: 800px;
  margin: 0 auto;
}

.content-wrapper {
  width: 100%;
}

/* 顶部行程卡片 */
.trip-header-card {
  border-radius: 16px;
  border: none;
  box-shadow: 0 6px 20px rgba(var(--primary-rgb), 0.08);
  background: linear-gradient(135deg, #ffffff 0%, #f7fbff 100%);
  margin-bottom: 24px;
}

.trip-header {
  width: 100%;
}

.trip-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.trip-title {
  font-size: 26px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
  line-height: 1.3;
  flex: 1;
  min-width: 0;
}

.trip-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-top: 16px;
  color: #5a6b7c;
  font-size: 14px;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.meta-item .el-icon {
  font-size: 16px;
  color: #1890ff;
}

.trip-description {
  margin-top: 16px;
  color: #6b7785;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
}

/* 每日卡片 */
.days-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.day-card {
  border-radius: 14px;
  border: none;
  box-shadow: 0 4px 14px var(--shadow-sm);
  overflow: hidden;
}

.day-card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 18px 24px;
  background: linear-gradient(90deg, #e6f7ff 0%, #ffffff 100%);
  border-bottom: 1px solid #f0f4f8;
}

.day-badge {
  background: #1890ff;
  color: #fff;
  padding: 4px 12px;
  border-radius: 14px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.3px;
  flex-shrink: 0;
}

.day-card-title {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
  flex-wrap: wrap;
}

.day-date {
  color: #1890ff;
  font-weight: 600;
  font-size: 14px;
}

.day-title-text {
  color: #1a1a1a;
  font-size: 15px;
  font-weight: 500;
}

.activity-count {
  font-size: 12px;
  color: #999;
  background: #f5f5f5;
  padding: 3px 10px;
  border-radius: 10px;
  flex-shrink: 0;
}

.day-description {
  padding: 12px 24px 0;
  color: #6b7785;
  font-size: 13px;
  line-height: 1.6;
}

/* 活动列表 */
.activities-list {
  padding: 16px 24px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.activity-item {
  display: flex;
  gap: 16px;
  padding: 14px;
  background: #fafbfc;
  border-radius: 10px;
  border-left: 3px solid transparent;
  transition: all 0.2s ease;
}

.activity-item:hover {
  background: #f0f7ff;
  border-left-color: #1890ff;
  transform: translateX(2px);
}

.activity-time-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 60px;
  padding: 6px 0;
  border-right: 1px dashed #e0e6ed;
  flex-shrink: 0;
}

.time-start,
.time-end {
  font-size: 13px;
  font-weight: 600;
  color: #1890ff;
  line-height: 1.4;
}

.time-divider {
  color: #c0c4cc;
  font-size: 10px;
  margin: 2px 0;
}

.time-end {
  color: #999;
}

.activity-content {
  flex: 1;
  min-width: 0;
}

.activity-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}

.activity-name {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}

.activity-place {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 6px;
  font-size: 12px;
  color: #999;
}

.activity-place .el-icon {
  font-size: 13px;
}

.activity-description {
  margin-top: 8px;
  font-size: 13px;
  color: #5a6b7c;
  line-height: 1.6;
}

.activity-notes {
  margin-top: 6px;
  font-size: 12px;
  color: #999;
  line-height: 1.5;
  background: #fff8e1;
  padding: 6px 10px;
  border-radius: 6px;
}

.notes-label {
  color: #fa8c16;
  font-weight: 600;
}

/* 错误状态 */
.error-state {
  background: #fff;
  border-radius: 16px;
  padding: 60px 24px;
  box-shadow: 0 4px 14px var(--shadow-sm);
}

.error-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 96px;
  height: 96px;
  margin: 0 auto 16px;
  background: #fff5f5;
  border-radius: 50%;
}

.error-hint {
  color: #999;
  font-size: 13px;
  margin-top: 8px;
  margin-bottom: 16px;
}

/* 水印 */
.watermark {
  text-align: center;
  margin-top: 32px;
  padding: 16px 0;
  font-size: 13px;
}

.watermark-text {
  color: #b0b8c4;
}

.watermark-brand {
  margin-left: 4px;
  font-weight: 600;
  background: linear-gradient(90deg, #1890ff, #722ed1);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

/* 响应式 */
@media (max-width: 600px) {
  .shared-trip-page {
    padding: 16px 12px;
  }

  .trip-header-card :deep(.el-card__body),
  .day-card :deep(.el-card__body) {
    padding-left: 20px;
    padding-right: 20px;
  }

  .trip-title {
    font-size: 22px;
  }

  .day-card-header {
    padding: 14px 16px;
  }

  .activities-list {
    padding: 12px 16px 16px;
  }

  .activity-item {
    flex-direction: column;
    gap: 8px;
  }

  .activity-time-block {
    flex-direction: row;
    border-right: none;
    border-bottom: 1px dashed #e0e6ed;
    padding: 0 0 6px;
    min-width: 0;
    gap: 8px;
  }

  .time-divider {
    margin: 0;
  }
}
</style>
