<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useTripStore } from '@/stores/trip'
import { tripApi } from '@/api/trip'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { CreateTripRequest } from '@/types/api'
import { Plus, Delete, Edit, ArrowRight, Calendar, MapLocation, Wallet, Star } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const router = useRouter()
const trip = useTripStore()

const showCreateDialog = ref(false)
const newTrip = ref<CreateTripRequest>({
  title: '',
  destinationName: '',
  startDate: '',
  endDate: '',
  description: '',
  budget: undefined,
})

onMounted(async () => {
  await trip.fetchTrips()
})

async function handleCreateTrip() {
  if (!newTrip.value.title || !newTrip.value.destinationName) {
    ElMessage.warning('请填写行程名称和目的地')
    return
  }
  if (!newTrip.value.startDate || !newTrip.value.endDate) {
    ElMessage.warning('请选择开始和结束日期')
    return
  }

  try {
    await trip.createTrip(newTrip.value)
    ElMessage.success('行程创建成功')
    showCreateDialog.value = false
    resetNewTrip()
  } catch {
    ElMessage.error('创建失败')
  }
}

function resetNewTrip() {
  newTrip.value = {
    title: '',
    destinationName: '',
    startDate: '',
    endDate: '',
    description: '',
    budget: undefined,
  }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定删除该行程吗？', '确认删除', { 
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'delete-confirm-btn'
    })
    await trip.deleteTrip(id)
    ElMessage.success('已删除')
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '删除失败')
    }
  }
}

function formatDate(date: string) {
  return dayjs(date).format('YYYY-MM-DD')
}

function getStatusType(status: string) {
  const map: Record<string, string> = {
    DRAFT: 'info',
    PLANNING: 'warning',
    CONFIRMED: 'success',
    COMPLETED: '',
    CANCELLED: 'danger',
  }
  return map[status] || 'info'
}

function getStatusLabel(status: string) {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    PLANNING: '规划中',
    CONFIRMED: '已确认',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
  }
  return map[status] || status
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-content">
        <div class="header-text">
          <h1 class="page-title">我的行程</h1>
          <p class="page-subtitle">管理您的所有旅行计划</p>
        </div>
        <el-button type="primary" :icon="Plus" class="create-btn" @click="showCreateDialog = true">
          新建行程
        </el-button>
      </div>
    </div>

    <el-row :gutter="16" style="margin-bottom: 24px;">
      <el-col :span="6" v-for="(stat, index) in [
        { label: '总行程数', value: trip.trips.length, color: 'var(--primary-color)', icon: Star },
        { label: '规划中', value: trip.trips.filter((t: any) => t.status === 'PLANNING').length, color: '#f59e0b', icon: Calendar },
        { label: '已完成', value: trip.trips.filter((t: any) => t.status === 'COMPLETED').length, color: '#10b981', icon: ArrowRight },
        { label: '草稿', value: trip.trips.filter((t: any) => t.status === 'DRAFT').length, color: 'var(--text-muted)', icon: Edit },
      ]" :key="stat.label">
        <el-card :body-style="{ padding: '24px' }" class="stat-card" :style="{ animationDelay: `${index * 100}ms` }">
          <div class="stat-icon" :style="{ background: `${stat.color}15` }">
            <el-icon :color="stat.color"><component :is="stat.icon" /></el-icon>
          </div>
          <div class="stat-value" :style="{ color: stat.color }">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <div v-loading="trip.loading" class="content-card" style="padding: 0;">
      <div v-if="trip.trips.length === 0" class="empty-state">
        <div class="empty-icon">
          <Calendar :size="64" color="var(--text-muted)" />
        </div>
        <h3 class="empty-title">还没有行程</h3>
        <p class="empty-desc">开始规划您的第一次旅行吧</p>
        <el-button type="primary" :icon="Plus" class="empty-btn" @click="showCreateDialog = true">创建第一个行程</el-button>
      </div>

      <div v-else class="trip-list">
        <div
          v-for="(tripItem, index) in trip.trips"
          :key="tripItem.id"
          class="trip-card"
          :style="{ animationDelay: `${index * 80}ms` }"
          @click="router.push(`/trips/${tripItem.id}`)"
        >
          <div class="trip-left">
            <div class="trip-icon">
              <MapLocation :size="20" color="var(--primary-color)" />
            </div>
            <div class="trip-info">
              <div class="trip-header">
                <h3 class="trip-title">{{ tripItem.title }}</h3>
                <el-tag
                  :type="getStatusType(tripItem.status)"
                  effect="light"
                  class="status-tag"
                >
                  {{ getStatusLabel(tripItem.status) }}
                </el-tag>
              </div>
              <div class="trip-meta">
                <span class="meta-item">
                  <MapLocation :size="14" color="var(--text-muted)" />
                  {{ tripItem.destinationName }}
                </span>
                <span class="meta-item">
                  <Calendar :size="14" color="var(--text-muted)" />
                  {{ formatDate(tripItem.startDate) }} - {{ formatDate(tripItem.endDate) }}
                </span>
                <span v-if="tripItem.budget" class="meta-item budget-item">
                  <Wallet :size="14" color="#f59e0b" />
                  ¥{{ tripItem.budget.toLocaleString() }}
                </span>
              </div>
            </div>
          </div>
          <div class="trip-actions" @click.stop>
            <el-button :icon="Edit" class="action-btn" @click="router.push(`/trips/${tripItem.id}/edit`)">编辑</el-button>
            <el-button :icon="Delete" class="action-btn delete-btn" @click="handleDelete(tripItem.id)">删除</el-button>
            <el-button :icon="ArrowRight" class="action-btn primary-btn" @click="router.push(`/trips/${tripItem.id}`)">查看</el-button>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="showCreateDialog" title="新建行程" width="520px" class="create-dialog">
      <el-form :model="newTrip" label-width="80px" class="create-form">
        <el-form-item label="行程名称" required>
          <el-input v-model="newTrip.title" placeholder="例如：东京5日游" class="form-input" />
        </el-form-item>
        <el-form-item label="目的地" required>
          <el-input v-model="newTrip.destinationName" placeholder="例如：东京" class="form-input" />
        </el-form-item>
        <el-form-item label="开始日期" required>
          <el-date-picker v-model="newTrip.startDate" type="date" style="width: 100%;" class="form-picker" />
        </el-form-item>
        <el-form-item label="结束日期" required>
          <el-date-picker v-model="newTrip.endDate" type="date" style="width: 100%;" class="form-picker" />
        </el-form-item>
        <el-form-item label="预算">
          <el-input-number v-model="newTrip.budget" :min="0" style="width: 100%;" class="form-input" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="newTrip.description" type="textarea" :rows="3" class="form-input" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="dialog-btn" @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" class="dialog-btn primary" @click="handleCreateTrip">创建</el-button>
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

.stat-card {
  background: var(--input-bg);
  backdrop-filter: blur(12px);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  cursor: default;
  animation: fadeInUp 0.5s ease-out forwards;
  opacity: 0;
}

.stat-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  margin-bottom: 16px;
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

.content-card {
  background: var(--bg-tertiary);
  backdrop-filter: blur(12px);
  border: 1px solid var(--border-color);
  border-radius: 16px;
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

.trip-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
}

.trip-card {
  background: var(--input-bg);
  border-radius: 14px;
  padding: 20px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  border: 1px solid var(--border-color);
  transition: all 0.3s ease;
  animation: fadeInUp 0.5s ease-out forwards;
  opacity: 0;
}

.trip-card:hover {
  background: var(--input-bg);
  border-color: rgba(var(--primary-rgb), 0.3);
  transform: translateY(-2px);
  box-shadow: 0 8px 25px var(--shadow-sm);
}

.trip-left {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  min-width: 0;
}

.trip-icon {
  width: 44px;
  height: 44px;
  background: rgba(var(--primary-rgb), 0.15);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.trip-info {
  flex: 1;
  min-width: 0;
}

.trip-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.trip-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.status-tag {
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 20px;
}

.trip-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-muted);
}

.budget-item {
  color: #f59e0b;
}

.trip-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.action-btn {
  padding: 8px 16px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s;
}

.action-btn:hover {
  transform: translateY(-1px);
}

.primary-btn {
  background: rgba(var(--primary-rgb), 0.15);
  color: var(--primary-light);
  border: 1px solid rgba(var(--primary-rgb), 0.3);
}

.primary-btn:hover {
  background: rgba(var(--primary-rgb), 0.25);
}

.delete-btn {
  color: var(--error-color);
  border-color: rgba(var(--error-rgb), 0.2);
}

.delete-btn:hover {
  background: rgba(var(--error-rgb), 0.1);
}

:deep(.el-card__body) {
  padding: 24px;
}

:deep(.create-dialog .el-dialog) {
  background: var(--input-bg);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(var(--primary-rgb), 0.2);
  border-radius: 20px;
}

:deep(.create-dialog .el-dialog__header) {
  border-bottom: 1px solid var(--border-color);
}

:deep(.create-dialog .el-dialog__title) {
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 600;
}

:deep(.create-dialog .el-dialog__body) {
  padding: 24px;
}

:deep(.create-dialog .el-dialog__footer) {
  border-top: 1px solid var(--border-color);
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

.form-picker {
  :deep(.el-input__wrapper) {
    background: var(--input-bg);
    border-color: var(--border-color-light);
    box-shadow: none;
  }
  :deep(.el-input__inner) {
    color: var(--text-primary);
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

:deep(.delete-confirm-btn) {
  background: var(--error-color);
}
</style>
