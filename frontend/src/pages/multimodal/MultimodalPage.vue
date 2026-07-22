<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { multimodalApi } from '@/api/multimodal'
import { behaviorApi } from '@/api/behavior'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { MultimodalUploadVO } from '@/types/api'
import { Upload, Picture, MagicStick, Delete, View } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const router = useRouter()

const uploads = ref<MultimodalUploadVO[]>([])
const loading = ref(false)
const uploading = ref(false)
const generatingId = ref<number | null>(null)

const fileInput = ref<HTMLInputElement | null>(null)
const previewUrl = ref<string>('')
const selectedFile = ref<File | null>(null)

onMounted(() => {
  loadUploads()
})

async function loadUploads() {
  loading.value = true
  try {
    const res = await multimodalApi.list()
    uploads.value = res.data
  } finally {
    loading.value = false
  }
}

function triggerUpload() {
  fileInput.value?.click()
}

function handleFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  if (!target.files || target.files.length === 0) return
  const file = target.files[0]
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('仅支持图片文件')
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('文件大小不能超过 10MB')
    return
  }
  selectedFile.value = file
  previewUrl.value = URL.createObjectURL(file)
}

async function handleUpload() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择图片')
    return
  }
  uploading.value = true
  try {
    const res = await multimodalApi.upload(selectedFile.value)
    ElMessage.success('上传成功，已开始识别')
    // 行为埋点
    behaviorApi.track({
      behaviorType: 'UPLOAD',
      targetType: 'MULTIMODAL',
      targetId: res.data.id,
    }).catch(() => {})
    selectedFile.value = null
    previewUrl.value = ''
    if (fileInput.value) fileInput.value.value = ''
    await loadUploads()
  } catch {
    // 错误由全局拦截器提示
  } finally {
    uploading.value = false
  }
}

function cancelPreview() {
  selectedFile.value = null
  previewUrl.value = ''
  if (fileInput.value) fileInput.value.value = ''
}

async function handleGenerate(upload: MultimodalUploadVO) {
  generatingId.value = upload.id
  try {
    const res = await multimodalApi.generate(upload.id)
    ElMessage.success('已生成行程')
    if (res.data.generatedTripId) {
      try {
        await ElMessageBox.confirm(
          '行程已生成，是否立即查看？',
          '生成成功',
          { type: 'success', confirmButtonText: '查看行程', cancelButtonText: '稍后' },
        )
        router.push(`/trips/${res.data.generatedTripId}`)
      } catch {
        // 用户选择稍后查看
      }
    }
    await loadUploads()
  } catch {
    // 错误由全局拦截器提示
  } finally {
    generatingId.value = null
  }
}

async function handleDelete(upload: MultimodalUploadVO) {
  try {
    await ElMessageBox.confirm(
      `确定删除「${upload.originalFilename}」吗？`,
      '确认删除',
      { type: 'warning' },
    )
    await multimodalApi.delete(upload.id)
    ElMessage.success('已删除')
    await loadUploads()
  } catch {
    // cancelled
  }
}

function viewTrip(tripId: number) {
  router.push(`/trips/${tripId}`)
}

function getStatusLabel(status: string) {
  const map: Record<string, string> = {
    PENDING: '识别中',
    RECOGNIZED: '已识别',
    TRIP_GENERATED: '已生成行程',
    FAILED: '识别失败',
  }
  return map[status] || status
}

function getStatusType(status: string) {
  const map: Record<string, string> = {
    PENDING: 'warning',
    RECOGNIZED: 'primary',
    TRIP_GENERATED: 'success',
    FAILED: 'danger',
  }
  return map[status] || 'info'
}

function formatDate(date: string) {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

function extractDestination(upload: MultimodalUploadVO): string {
  const result = upload.recognitionResult as Record<string, any>
  return result?.destination || result?.location || '未识别'
}

function extractTags(upload: MultimodalUploadVO): string[] {
  const result = upload.recognitionResult as Record<string, any>
  if (!result) return []
  if (Array.isArray(result.tags)) return result.tags
  if (Array.isArray(result.keywords)) return result.keywords
  return []
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h1 class="page-title">多模态行程生成</h1>
        <p class="page-subtitle">上传一张目的地照片，AI 自动识别并生成行程</p>
      </div>
    </div>

    <el-card class="upload-card" shadow="never">
      <div class="upload-area">
        <input
          ref="fileInput"
          type="file"
          accept="image/*"
          style="display: none;"
          @change="handleFileChange"
        />
        <div v-if="!previewUrl" class="upload-placeholder" @click="triggerUpload">
          <el-icon :size="48"><Upload /></el-icon>
          <div class="upload-text">点击或拖拽上传图片</div>
          <div class="upload-hint">支持 JPG/PNG/WebP，单文件最大 10MB</div>
        </div>
        <div v-else class="preview-area">
          <img :src="previewUrl" class="preview-image" alt="预览图" />
          <div class="preview-actions">
            <el-button @click="triggerUpload" :icon="Picture">重新选择</el-button>
            <el-button type="primary" @click="handleUpload" :loading="uploading" :icon="Upload">
              {{ uploading ? '上传中...' : '上传并识别' }}
            </el-button>
            <el-button @click="cancelPreview">取消</el-button>
          </div>
        </div>
      </div>
    </el-card>

    <div class="section-title">
      <el-icon><Picture /></el-icon>
      <span>我的上传记录</span>
    </div>

    <div v-loading="loading">
      <div v-if="uploads.length === 0" class="empty-state">
        <el-empty description="还没有上传记录" />
      </div>

      <div v-else class="upload-grid">
        <div v-for="upload in uploads" :key="upload.id" class="upload-item">
          <div class="item-header">
            <el-tag :type="getStatusType(upload.status)" effect="dark" size="small">
              {{ getStatusLabel(upload.status) }}
            </el-tag>
            <span class="item-filename" :title="upload.originalFilename">
              {{ upload.originalFilename }}
            </span>
          </div>

          <div class="recognition-info">
            <div class="info-row">
              <span class="info-label">识别目的地：</span>
              <span class="info-value">{{ extractDestination(upload) }}</span>
            </div>
            <div v-if="extractTags(upload).length > 0" class="info-row">
              <span class="info-label">关键词：</span>
              <div class="tag-list">
                <el-tag
                  v-for="tag in extractTags(upload)"
                  :key="tag"
                  size="small"
                  effect="plain"
                >
                  {{ tag }}
                </el-tag>
              </div>
            </div>
          </div>

          <div class="item-meta">
            <span>{{ formatDate(upload.createdAt) }}</span>
            <span v-if="upload.fileSize">· {{ (upload.fileSize / 1024).toFixed(1) }} KB</span>
          </div>

          <div class="item-actions">
            <el-button
              v-if="upload.status === 'RECOGNIZED' && !upload.generatedTripId"
              type="primary"
              size="small"
              :icon="MagicStick"
              :loading="generatingId === upload.id"
              @click="handleGenerate(upload)"
            >
              生成行程
            </el-button>
            <el-button
              v-if="upload.generatedTripId"
              type="success"
              size="small"
              :icon="View"
              @click="viewTrip(upload.generatedTripId!)"
            >
              查看行程
            </el-button>
            <el-button
              type="danger"
              size="small"
              :icon="Delete"
              link
              @click="handleDelete(upload)"
            >
              删除
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-header {
  margin-bottom: 32px;
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

.upload-card {
  background: var(--input-bg);
  backdrop-filter: blur(12px);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  margin-bottom: 32px;
}

.upload-area {
  min-height: 240px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.upload-placeholder {
  width: 100%;
  height: 240px;
  border: 2px dashed var(--border-color-light);
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  color: var(--text-muted);
  background: var(--input-bg);
}

.upload-placeholder:hover {
  border-color: rgba(var(--primary-rgb), 0.6);
  color: var(--primary-light);
  background: var(--hover-bg);
}

.upload-text {
  font-size: 16px;
  margin-top: 16px;
  color: var(--text-primary);
  font-weight: 500;
}

.upload-hint {
  font-size: 13px;
  margin-top: 8px;
  color: var(--text-muted);
}

.preview-area {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.preview-image {
  max-width: 100%;
  max-height: 360px;
  border-radius: 16px;
  box-shadow: 0 8px 25px var(--shadow-sm);
  border: 2px solid var(--border-color);
}

.preview-actions {
  display: flex;
  gap: 12px;
}

.preview-actions .el-button {
  padding: 10px 20px;
  border-radius: 10px;
}

.preview-actions .el-button--primary {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
  border: none;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 20px;
}

.upload-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 16px;
}

.upload-item {
  background: var(--input-bg);
  backdrop-filter: blur(12px);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  transition: all 0.3s ease;
  animation: fadeInUp 0.5s ease-out forwards;
  opacity: 0;
  border-top: 3px solid rgba(var(--primary-rgb), 0.3);
}

.upload-item:hover {
  background: var(--card-bg-hover);
  border-color: rgba(var(--primary-rgb), 0.3);
  transform: translateY(-2px);
  box-shadow: 0 8px 25px var(--shadow-sm);
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

.item-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.item-filename {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.recognition-info {
  background: var(--input-bg);
  border-radius: 12px;
  padding: 14px;
  border: 1px solid var(--border-color);
}

.info-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
}

.info-row + .info-row {
  margin-top: 8px;
}

.info-label {
  color: var(--text-muted);
  flex-shrink: 0;
}

.info-value {
  color: var(--text-primary);
  font-weight: 500;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  flex: 1;
}

.tag-list .el-tag {
  color: var(--primary-light);
  border-color: rgba(var(--primary-rgb), 0.3);
  background: var(--hover-bg);
}

.item-meta {
  font-size: 12px;
  color: var(--text-muted);
  display: flex;
  gap: 6px;
}

.item-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 14px;
  border-top: 1px solid var(--border-color);
}

.item-actions .el-button {
  font-size: 12px;
}

.item-actions .el-button--primary {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
  border: none;
  border-radius: 10px;
}

.item-actions .el-button--success {
  background: linear-gradient(135deg, #10b981, #059669);
  border: none;
  border-radius: 10px;
}

.empty-state {
  text-align: center;
  padding: 80px 40px;
}

.empty-state .el-empty__text {
  color: var(--text-muted);
}

@media (max-width: 768px) {
  .upload-grid {
    grid-template-columns: 1fr;
  }

  .preview-image {
    max-height: 280px;
  }

  .page-title {
    font-size: 24px;
  }
}
</style>
