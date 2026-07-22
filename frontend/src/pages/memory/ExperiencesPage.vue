<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { experienceApi } from '@/api/experience'
import { tripApi } from '@/api/trip'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ExperienceVO, CreateExperienceRequest, TripVO } from '@/types/api'
import { Plus, Delete, Edit, Star } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const experiences = ref<ExperienceVO[]>([])
const trips = ref<TripVO[]>([])
const loading = ref(false)

const showForm = ref(false)
const editingId = ref<number | null>(null)
const form = ref<CreateExperienceRequest>({
  tripId: 0,
  tripDayId: undefined,
  tripActivityId: undefined,
  title: '',
  content: '',
  rating: undefined,
  tags: [],
  mediaUrls: [],
})

onMounted(async () => {
  loading.value = true
  try {
    const [expRes, tripRes] = await Promise.all([
      experienceApi.list(),
      tripApi.list(),
    ])
    experiences.value = expRes.data
    trips.value = tripRes.data
  } finally {
    loading.value = false
  }
})

function openCreate() {
  editingId.value = null
  form.value = {
    tripId: trips.value[0]?.id || 0,
    tripDayId: undefined,
    tripActivityId: undefined,
    title: '',
    content: '',
    rating: undefined,
    tags: [],
    mediaUrls: [],
  }
  showForm.value = true
}

function openEdit(exp: ExperienceVO) {
  editingId.value = exp.id
  form.value = {
    tripId: exp.tripId,
    tripDayId: exp.tripDayId || undefined,
    tripActivityId: exp.tripActivityId || undefined,
    title: exp.title,
    content: exp.content,
    rating: exp.rating || undefined,
    tags: exp.tags,
    mediaUrls: exp.mediaUrls,
  }
  showForm.value = true
}

async function save() {
  if (!form.value.title) {
    ElMessage.warning('请输入标题')
    return
  }
  if (!form.value.tripId) {
    ElMessage.warning('请选择关联行程')
    return
  }

  try {
    if (editingId.value) {
      await experienceApi.update(editingId.value, form.value)
      ElMessage.success('体验已更新')
    } else {
      await experienceApi.create(form.value)
      ElMessage.success('体验已创建')
    }
    showForm.value = false
    await loadExperiences()
  } catch {
    ElMessage.error('保存失败')
  }
}

async function loadExperiences() {
  loading.value = true
  try {
    const res = await experienceApi.list()
    experiences.value = res.data
  } finally {
    loading.value = false
  }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定删除这条体验吗？', '确认删除', { type: 'warning' })
    await experienceApi.delete(id)
    ElMessage.success('已删除')
    await loadExperiences()
  } catch {
    // cancelled
  }
}

function getTripName(tripId: number) {
  return trips.value.find(t => t.id === tripId)?.title || `行程 #${tripId}`
}

function formatDate(date: string) {
  return dayjs(date).format('YYYY-MM-DD')
}

function getStatusType(status: string) {
  const map: Record<string, string> = {
    PUBLISHED: 'success',
    DRAFT: 'info',
    ARCHIVED: '',
    DELETED: 'danger',
  }
  return map[status] || ''
}

function getStatusLabel(status: string) {
  const map: Record<string, string> = {
    PUBLISHED: '已发布',
    DRAFT: '草稿',
    ARCHIVED: '已归档',
    DELETED: '已删除',
  }
  return map[status] || status
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-content">
        <div class="header-text">
          <h1 class="page-title">体验分享</h1>
          <p class="page-subtitle">记录旅行中的美好体验和回忆</p>
        </div>
        <el-button type="primary" :icon="Plus" class="create-btn" @click="openCreate">
          新建体验
        </el-button>
      </div>
    </div>

    <div v-loading="loading" class="experiences-section">
      <div v-if="experiences.length === 0" class="empty-state">
        <div class="empty-icon">
          <Star :size="64" color="var(--text-muted)" />
        </div>
        <h3 class="empty-title">还没有体验分享</h3>
        <p class="empty-desc">分享你的旅行故事，记录难忘的时刻</p>
        <el-button type="primary" :icon="Plus" class="empty-btn" @click="openCreate">创建第一个体验</el-button>
      </div>

      <div v-else class="experience-grid">
        <div
          v-for="(exp, index) in experiences"
          :key="exp.id"
          class="experience-card"
          :style="{ animationDelay: `${index * 80}ms` }"
        >
          <div class="exp-header">
            <div class="exp-title">{{ exp.title }}</div>
            <el-tag :type="getStatusType(exp.status)" effect="light" size="small" class="status-tag">
              {{ getStatusLabel(exp.status) }}
            </el-tag>
          </div>

          <div class="exp-meta">
            <span>{{ getTripName(exp.tripId) }}</span>
            <span class="exp-date">{{ formatDate(exp.createdAt) }}</span>
          </div>

          <div v-if="exp.rating" class="exp-rating">
            <el-rate :model-value="exp.rating" disabled :colors="['#f59e0b']" />
          </div>

          <div v-if="exp.content" class="exp-content">
            {{ exp.content }}
          </div>

          <div v-if="exp.tags && exp.tags.length > 0" class="exp-tags">
            <el-tag
              v-for="tag in exp.tags"
              :key="tag"
              size="small"
              effect="plain"
              class="tag-item"
            >
              #{{ tag }}
            </el-tag>
          </div>

          <div class="exp-actions">
            <el-button :icon="Edit" link size="small" class="action-btn" @click="openEdit(exp)">编辑</el-button>
            <el-button :icon="Delete" link type="danger" size="small" class="action-btn delete-btn" @click="handleDelete(exp.id)">删除</el-button>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="showForm" :title="editingId ? '编辑体验' : '新建体验'" width="540px" class="create-dialog">
      <el-form :model="form" label-width="80px" class="create-form">
        <el-form-item label="关联行程" required>
          <el-select v-model="form.tripId" style="width: 100%;" class="form-select">
            <el-option
              v-for="t in trips"
              :key="t.id"
              :label="t.title"
              :value="t.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="例如：浅草寺之旅" class="form-input" />
        </el-form-item>
        <el-form-item label="评分">
          <el-rate v-model="form.rating" :colors="['#f59e0b']" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="4" placeholder="分享你的旅行体验..." class="form-input" />
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="form.tags" multiple style="width: 100%;" class="form-select">
            <el-option label="美食" value="美食" />
            <el-option label="风景" value="风景" />
            <el-option label="文化" value="文化" />
            <el-option label="购物" value="购物" />
            <el-option label="摄影" value="摄影" />
            <el-option label="亲子" value="亲子" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="dialog-btn" @click="showForm = false">取消</el-button>
        <el-button type="primary" class="dialog-btn primary" @click="save">保存</el-button>
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

.experiences-section {
  animation: fadeInUp 0.5s ease-out forwards;
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

.experience-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 16px;
}

.experience-card {
  background: var(--input-bg);
  backdrop-filter: blur(12px);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 20px;
  transition: all 0.3s ease;
  animation: fadeInUp 0.5s ease-out forwards;
  opacity: 0;
  border-top: 3px solid rgba(var(--primary-rgb), 0.3);
}

.experience-card:hover {
  background: var(--card-bg-hover);
  border-color: rgba(var(--primary-rgb), 0.3);
  transform: translateY(-2px);
  box-shadow: 0 8px 25px var(--shadow-sm);
}

.exp-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 10px;
}

.exp-title {
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

.exp-meta {
  font-size: 13px;
  color: var(--text-muted);
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.exp-date {
  margin-left: auto;
}

.exp-rating {
  margin-bottom: 10px;
}

.exp-content {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.exp-tags {
  margin-bottom: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tag-item {
  color: var(--primary-light);
  border-color: rgba(var(--primary-rgb), 0.3);
  background: var(--hover-bg);
}

.exp-actions {
  display: flex;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid var(--border-color);
}

.action-btn {
  font-size: 12px;
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
  .experience-grid {
    grid-template-columns: 1fr;
  }
}
</style>