<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { checkinApi } from '@/api/checkin'
import { tripApi } from '@/api/trip'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { TripCheckinVO, CheckinStatsVO, TripVO, CreateCheckinRequest } from '@/types/api'
import { Plus, Location, Delete, Check, Close } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const checkins = ref<TripCheckinVO[]>([])
const stats = ref<CheckinStatsVO | null>(null)
const trips = ref<TripVO[]>([])
const loading = ref(false)

const showDialog = ref(false)
const submitting = ref(false)
const form = ref<CreateCheckinRequest>({
  tripId: 0,
  placeName: '',
  notes: '',
})

onMounted(() => {
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const [checkinsRes, statsRes, tripsRes] = await Promise.all([
      checkinApi.listMine(),
      checkinApi.stats(),
      tripApi.list(),
    ])
    checkins.value = checkinsRes.data
    stats.value = statsRes.data
    trips.value = tripsRes.data
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.value = {
    tripId: trips.value[0]?.id || 0,
    placeName: '',
    notes: '',
  }
  showDialog.value = true
}

async function submit() {
  if (!form.value.tripId) {
    ElMessage.warning('请选择行程')
    return
  }
  if (!form.value.placeName?.trim()) {
    ElMessage.warning('请输入地点名称')
    return
  }
  submitting.value = true
  try {
    await checkinApi.create({
      tripId: form.value.tripId,
      placeName: form.value.placeName.trim(),
      notes: form.value.notes?.trim() || undefined,
    })
    ElMessage.success('打卡成功')
    showDialog.value = false
    await loadData()
  } catch {
    // 错误由全局拦截器提示
  } finally {
    submitting.value = false
  }
}

async function updateStatus(checkin: TripCheckinVO, status: 'COMPLETED' | 'SKIPPED') {
  try {
    await checkinApi.updateStatus(checkin.id, status)
    ElMessage.success(status === 'COMPLETED' ? '已标记完成' : '已标记跳过')
    await loadData()
  } catch {
    // 错误由全局拦截器提示
  }
}

async function handleDelete(checkin: TripCheckinVO) {
  try {
    await ElMessageBox.confirm(
      `确定删除「${checkin.placeName}」的打卡记录吗？`,
      '确认删除',
      { type: 'warning' },
    )
    await checkinApi.delete(checkin.id)
    ElMessage.success('已删除')
    await loadData()
  } catch {
    // cancelled
  }
}

const statsCards = computed(() => {
  if (!stats.value) return []
  return [
    { label: '总打卡数', value: stats.value.totalCheckins, color: 'var(--primary-color)' },
    { label: '已完成', value: stats.value.completedCount, color: '#10b981' },
    { label: '已跳过', value: stats.value.skippedCount, color: '#f59e0b' },
    { label: '去过的地方', value: stats.value.visitedPlaces?.length || 0, color: 'var(--primary-light)' },
  ]
})

function getStatusLabel(status: string) {
  const map: Record<string, string> = {
    PENDING: '待完成',
    COMPLETED: '已完成',
    SKIPPED: '已跳过',
  }
  return map[status] || status
}

function getStatusType(status: string) {
  const map: Record<string, string> = {
    PENDING: 'warning',
    COMPLETED: 'success',
    SKIPPED: 'info',
  }
  return map[status] || 'info'
}

function formatDate(date: string) {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

function getTripTitle(tripId: number) {
  return trips.value.find(t => t.id === tripId)?.title || `行程 #${tripId}`
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-content">
        <div class="header-text">
          <h1 class="page-title">智能打卡</h1>
          <p class="page-subtitle">记录旅行足迹，留住每个精彩瞬间</p>
        </div>
        <el-button type="primary" :icon="Plus" class="create-btn" @click="openCreate">
          新增打卡
        </el-button>
      </div>
    </div>

    <el-row :gutter="16" class="stat-row">
      <el-col v-for="(stat, index) in statsCards" :key="stat.label" :xs="12" :sm="6">
        <el-card :body-style="{ padding: '24px' }" class="stat-card" :style="{ animationDelay: `${index * 100}ms` }">
          <div class="stat-value" :style="{ color: stat.color }">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <div v-loading="loading" class="checkin-section">
      <div v-if="checkins.length === 0" class="empty-state">
        <div class="empty-icon">
          <Location :size="64" color="var(--text-muted)" />
        </div>
        <h3 class="empty-title">还没有打卡记录</h3>
        <p class="empty-desc">记录你的旅行足迹，留下美好回忆</p>
        <el-button type="primary" :icon="Plus" class="empty-btn" @click="openCreate">添加第一个打卡</el-button>
      </div>

      <div v-else class="checkin-list">
        <div
          v-for="(checkin, index) in checkins"
          :key="checkin.id"
          class="checkin-card"
          :style="{ animationDelay: `${index * 80}ms` }"
        >
          <div class="card-left">
            <div class="card-icon" :class="checkin.status">
              <el-icon><Location /></el-icon>
            </div>
          </div>
          <div class="card-main">
            <div class="card-header">
              <span class="card-title">{{ checkin.placeName }}</span>
              <el-tag :type="getStatusType(checkin.status)" effect="light" size="small" class="status-tag">
                {{ getStatusLabel(checkin.status) }}
              </el-tag>
            </div>
            <div class="card-meta">
              <span>{{ getTripTitle(checkin.tripId) }}</span>
              <span>·</span>
              <span>{{ formatDate(checkin.checkedInAt) }}</span>
            </div>
            <div v-if="checkin.notes" class="card-notes">{{ checkin.notes }}</div>
          </div>
          <div class="card-actions">
            <el-button
              v-if="checkin.status === 'PENDING'"
              type="success"
              :icon="Check"
              link
              size="small"
              class="action-btn"
              @click="updateStatus(checkin, 'COMPLETED')"
            >
              完成
            </el-button>
            <el-button
              v-if="checkin.status === 'PENDING'"
              type="warning"
              :icon="Close"
              link
              size="small"
              class="action-btn"
              @click="updateStatus(checkin, 'SKIPPED')"
            >
              跳过
            </el-button>
            <el-button
              :icon="Delete"
              link
              type="danger"
              size="small"
              class="action-btn delete-btn"
              @click="handleDelete(checkin)"
            >
              删除
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="showDialog" title="新增打卡" width="500px" class="create-dialog">
      <el-form :model="form" label-width="80px" class="create-form">
        <el-form-item label="行程" required>
          <el-select v-model="form.tripId" placeholder="选择行程" style="width: 100%;" class="form-select">
            <el-option
              v-for="trip in trips"
              :key="trip.id"
              :label="`${trip.title} - ${trip.destinationName}`"
              :value="trip.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="地点名称" required>
          <el-input
            v-model="form.placeName"
            placeholder="例如：东京塔"
            maxlength="100"
            show-word-limit
            class="form-input"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="form.notes"
            type="textarea"
            :rows="3"
            placeholder="记录这次打卡的感受或回忆"
            maxlength="500"
            show-word-limit
            class="form-input"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="dialog-btn" @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" class="dialog-btn primary" @click="submit">打卡</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-header {
  margin-bottom: 32px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-text {
  display: flex;
  flex-direction: column;
}

.page-title {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -1px;
  margin-bottom: 4px;
}

.page-subtitle {
  font-size: 14px;
  color: var(--text-muted);
}

.create-btn {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
  border: none;
  border-radius: 12px;
  padding: 12px 24px;
  font-weight: 600;
  transition: all 0.3s;
}

.create-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(var(--primary-rgb), 0.4);
}

.stat-row {
  margin-bottom: 24px;
}

.stat-card {
  background: var(--input-bg);
  backdrop-filter: blur(12px);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  cursor: default;
  animation: fadeInUp 0.5s ease-out forwards;
  opacity: 0;
}

.stat-value {
  font-size: 40px;
  font-weight: 700;
  line-height: 1;
}

.stat-label {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 8px;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.checkin-section {
  animation: fadeInUp 0.5s ease-out forwards;
}

.empty-state {
  text-align: center;
  padding: 80px 40px;
}

.empty-icon {
  width: 100px;
  height: 100px;
  margin: 0 auto 24px;
  background: var(--border-color);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.empty-desc {
  font-size: 14px;
  color: var(--text-muted);
  margin-bottom: 24px;
}

.empty-btn {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
  border: none;
  border-radius: 12px;
  padding: 12px 32px;
  font-weight: 600;
}

.checkin-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.checkin-card {
  background: var(--input-bg);
  backdrop-filter: blur(12px);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 20px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.3s ease;
  animation: fadeInUp 0.5s ease-out forwards;
  opacity: 0;
}

.checkin-card:hover {
  background: var(--card-bg-hover);
  border-color: rgba(var(--primary-rgb), 0.3);
  transform: translateY(-2px);
  box-shadow: 0 8px 25px var(--shadow-sm);
}

.card-left {
  flex-shrink: 0;
}

.card-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(var(--primary-rgb), 0.15);
  border: 1px solid rgba(var(--primary-rgb), 0.2);
}

.card-icon .el-icon {
  color: var(--primary-color);
  font-size: 22px;
}

.card-icon.COMPLETED {
  background: rgba(var(--success-rgb), 0.15);
  border-color: rgba(var(--success-rgb), 0.2);
}

.card-icon.COMPLETED .el-icon {
  color: #10b981;
}

.card-icon.SKIPPED {
  background: rgba(var(--warning-rgb), 0.15);
  border-color: rgba(var(--warning-rgb), 0.2);
}

.card-icon.SKIPPED .el-icon {
  color: #f59e0b;
}

.card-main {
  flex: 1;
  min-width: 0;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.status-tag {
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 20px;
}

.card-meta {
  font-size: 13px;
  color: var(--text-muted);
  display: flex;
  gap: 6px;
}

.card-notes {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 8px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.action-btn {
  font-size: 12px;
  padding: 6px 12px;
}

.delete-btn {
  color: var(--error-color);
}

.create-dialog {
  :deep(.el-dialog) {
    background: var(--input-bg);
    backdrop-filter: blur(20px);
    border: 1px solid rgba(var(--primary-rgb), 0.2);
    border-radius: 20px;
  }
  :deep(.el-dialog__header) {
    border-bottom: 1px solid var(--border-color);
  }
  :deep(.el-dialog__title) {
    color: var(--text-primary);
    font-size: 18px;
    font-weight: 600;
  }
  :deep(.el-dialog__body) {
    padding: 24px;
  }
  :deep(.el-dialog__footer) {
    border-top: 1px solid var(--border-color);
  }
}

.form-input {
  :deep(.el-input__wrapper) {
    background: var(--input-bg);
    border-color: var(--border-color-light);
    box-shadow: none;
  }
  :deep(.el-input__inner) {
    color: var(--text-primary);
  }
  :deep(.el-input__placeholder) {
    color: var(--text-secondary);
  }
}

.form-select {
  :deep(.el-select .el-input__wrapper) {
    background: var(--input-bg);
    border-color: var(--border-color-light);
  }
  :deep(.el-select .el-input__inner) {
    color: var(--text-primary);
  }
  :deep(.el-select .el-input__placeholder) {
    color: var(--text-secondary);
  }
}

.dialog-btn {
  padding: 10px 24px;
  border-radius: 10px;
  font-weight: 500;
}

.dialog-btn.primary {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
  border: none;
}

@media (max-width: 768px) {
  .checkin-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .card-actions {
    width: 100%;
    justify-content: flex-end;
    margin-top: 12px;
  }

  .stat-value {
    font-size: 28px;
  }
}
</style>