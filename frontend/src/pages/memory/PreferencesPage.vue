<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { memoryApi } from '@/api/memory'
import { tripApi } from '@/api/trip'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { PreferenceVO, TripMemoryVO, TripVO } from '@/types/api'
import { Plus, Delete, Edit } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const preferences = ref<PreferenceVO[]>([])
const memories = ref<TripMemoryVO[]>([])
const trips = ref<TripVO[]>([])
const loading = ref(false)

const activeTab = ref('preferences')

const showPrefForm = ref(false)
const editingPrefId = ref<number | null>(null)
const prefForm = ref({
  category: '',
  preference: {} as Record<string, unknown>,
  source: 'MANUAL',
})

const showMemForm = ref(false)
const memForm = ref({
  tripId: 0 as number,
  memoryType: '',
  content: '',
  tags: [] as string[],
})

onMounted(async () => {
  loading.value = true
  try {
    const [prefRes, memRes, tripRes] = await Promise.all([
      memoryApi.listPreferences(),
      memoryApi.listMemories(),
      tripApi.list(),
    ])
    preferences.value = prefRes.data
    memories.value = memRes.data
    trips.value = tripRes.data
  } finally {
    loading.value = false
  }
})

function openPrefCreate() {
  editingPrefId.value = null
  prefForm.value = {
    category: '',
    preference: {},
    source: 'MANUAL',
  }
  showPrefForm.value = true
}

function openPrefEdit(pref: PreferenceVO) {
  editingPrefId.value = pref.id
  prefForm.value = {
    category: pref.category,
    preference: pref.data,
    source: pref.source,
  }
  showPrefForm.value = true
}

async function savePreference() {
  if (!prefForm.value.category) {
    ElMessage.warning('请选择偏好类别')
    return
  }

  try {
    if (editingPrefId.value) {
      await memoryApi.savePreference(prefForm.value)
      ElMessage.success('偏好已更新')
    } else {
      await memoryApi.savePreference(prefForm.value)
      ElMessage.success('偏好已保存')
    }
    showPrefForm.value = false
    await loadPreferences()
  } catch {
    ElMessage.error('保存失败')
  }
}

async function loadPreferences() {
  try {
    const res = await memoryApi.listPreferences()
    preferences.value = res.data
  } catch {
    // error
  }
}

async function deletePreference(id: number) {
  try {
    await ElMessageBox.confirm('确定删除此偏好吗？', '确认删除', { type: 'warning' })
    await memoryApi.deletePreference(id)
    ElMessage.success('已删除')
    await loadPreferences()
  } catch {
    // cancelled
  }
}

function openMemCreate() {
  memForm.value = {
    tripId: trips.value[0]?.id || 0,
    memoryType: 'PREFERENCE_DISCOVERED',
    content: '',
    tags: [],
  }
  showMemForm.value = true
}

async function saveMemory() {
  if (!memForm.value.tripId) {
    ElMessage.warning('请选择关联行程')
    return
  }
  if (!memForm.value.content) {
    ElMessage.warning('请输入记忆内容')
    return
  }

  try {
    await memoryApi.saveMemory(memForm.value)
    ElMessage.success('记忆已保存')
    showMemForm.value = false
    await loadMemories()
  } catch {
    ElMessage.error('保存失败')
  }
}

async function loadMemories() {
  try {
    const res = await memoryApi.listMemories()
    memories.value = res.data
  } catch {
    // error
  }
}

function getMemoryTypeLabel(type: string) {
  const map: Record<string, string> = {
    PREFERENCE_DISCOVERED: '发现的偏好',
    LESSON_LEARNED: '经验教训',
    HIGHLIGHT: '亮点',
    LOWLIGHT: '不足',
    ADVICE: '建议',
  }
  return map[type] || type
}

function getMemoryTypeColor(type: string) {
  const map: Record<string, string> = {
    PREFERENCE_DISCOVERED: 'var(--primary-color)',
    LESSON_LEARNED: '#f59e0b',
    HIGHLIGHT: '#10b981',
    LOWLIGHT: 'var(--error-color)',
    ADVICE: 'var(--primary-light)',
  }
  return map[type] || 'var(--text-muted)'
}

function getTripName(tripId: number) {
  return trips.value.find(t => t.id === tripId)?.title || `行程 #${tripId}`
}

function formatDate(date: string) {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

const prefCategories = [
  { label: '美食', value: 'FOOD', placeholder: '{ 辣度: 微辣, 偏好: ["日料", "川菜"] }' },
  { label: '活动', value: 'ACTIVITY', placeholder: '{ 类型: ["户外", "文化", "休闲"] }' },
  { label: '住宿', value: 'ACCOMMODATION', placeholder: '{ 偏好: ["酒店", "民宿"], 价格档位: "中档" }' },
  { label: '交通', value: 'TRANSPORT', placeholder: '{ 偏好: ["高铁", "飞机"], 座位: "经济舱" }' },
  { label: '景点', value: 'SIGHTSEEING', placeholder: '{ 类型: ["自然风光", "历史人文"] }' },
  { label: '预算', value: 'BUDGET', placeholder: '{ 总预算: 15000, 人均: 7500 }' },
  { label: '其他', value: 'OTHER', placeholder: '{ ...自定义键值 }' },
]
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-text">
        <h1 class="page-title">偏好记忆</h1>
        <p class="page-subtitle">管理你的旅行偏好和AI学习到的记忆</p>
      </div>
    </div>

    <el-tabs v-model="activeTab" type="border-card" class="pref-tabs">
      <el-tab-pane label="用户偏好" name="preferences">
        <div class="section-header">
          <p class="section-desc">设置你的旅行偏好，AI 会在规划时为你个性化推荐</p>
          <el-button type="primary" :icon="Plus" size="small" class="add-btn" @click="openPrefCreate">
            添加偏好
          </el-button>
        </div>

        <div v-if="preferences.length === 0" class="empty-state">
          <div class="empty-icon">
            <Edit :size="64" color="var(--text-muted)" />
          </div>
          <h3 class="empty-title">还没有设置偏好</h3>
          <p class="empty-desc">告诉我们你的旅行偏好，让AI更好地为你规划行程</p>
          <el-button type="primary" :icon="Plus" class="empty-btn" @click="openPrefCreate">添加第一个偏好</el-button>
        </div>

        <div v-else class="pref-grid">
          <div v-for="(pref, index) in preferences" :key="pref.id" class="pref-card" :style="{ animationDelay: `${index * 80}ms` }">
            <div class="pref-header">
              <div class="pref-category">
                <el-tag :type="pref.category === 'FOOD' ? 'warning' : pref.category === 'BUDGET' ? 'primary' : 'info'" effect="light" class="category-tag">
                  {{ prefCategories.find(c => c.value === pref.category)?.label || pref.category }}
                </el-tag>
                <span class="pref-source">{{ pref.source }}</span>
              </div>
              <div class="pref-actions">
                <el-button :icon="Edit" link size="small" class="action-btn" @click="openPrefEdit(pref)">编辑</el-button>
                <el-button :icon="Delete" link type="danger" size="small" class="action-btn delete-btn" @click="deletePreference(pref.id)">删除</el-button>
              </div>
            </div>
            <div class="pref-data">
              <pre>{{ JSON.stringify(pref.data, null, 2) }}</pre>
            </div>
            <div class="pref-time">{{ formatDate(pref.updatedAt || pref.createdAt) }}</div>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="旅行记忆" name="memories">
        <div class="section-header">
          <p class="section-desc">从行程中提炼的经验洞察，帮助 AI 更好地为你规划</p>
          <el-button type="primary" :icon="Plus" size="small" class="add-btn" @click="openMemCreate">
            添加记忆
          </el-button>
        </div>

        <div v-if="memories.length === 0" class="empty-state">
          <div class="empty-icon">
            <Edit :size="64" color="var(--text-muted)" />
          </div>
          <h3 class="empty-title">还没有旅行记忆</h3>
          <p class="empty-desc">AI会从你的行程中自动学习，也可以手动添加</p>
        </div>

        <div v-else class="mem-list">
          <div v-for="(mem, index) in memories" :key="mem.id" class="mem-card" :style="{ animationDelay: `${index * 80}ms` }">
            <div class="mem-header">
              <el-tag :color="getMemoryTypeColor(mem.memoryType)" effect="light" size="small" class="mem-type-tag">
                {{ getMemoryTypeLabel(mem.memoryType) }}
              </el-tag>
              <span class="mem-trip">{{ getTripName(mem.tripId) }}</span>
            </div>
            <div class="mem-content">{{ mem.content }}</div>
            <div v-if="mem.tags && mem.tags.length > 0" class="mem-tags">
              <el-tag v-for="tag in mem.tags" :key="tag" size="small" effect="plain" class="mem-tag">
                #{{ tag }}
              </el-tag>
            </div>
            <div class="mem-time">{{ formatDate(mem.createdAt) }}</div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="showPrefForm" :title="editingPrefId ? '编辑偏好' : '添加偏好'" width="520px" class="pref-dialog">
      <el-form :model="prefForm" label-width="80px" class="create-form">
        <el-form-item label="类别" required>
          <el-select v-model="prefForm.category" style="width: 100%;" class="form-select">
            <el-option v-for="c in prefCategories" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="偏好数据">
          <el-input
            v-model="prefForm.preference"
            type="textarea"
            :rows="6"
            placeholder="JSON 格式，例如：{ '辣度': '微辣', '偏好': ['日料', '川菜'] }"
            class="form-input"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="dialog-btn" @click="showPrefForm = false">取消</el-button>
        <el-button type="primary" class="dialog-btn primary" @click="savePreference">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showMemForm" title="添加旅行记忆" width="480px" class="mem-dialog">
      <el-form :model="memForm" label-width="80px" class="create-form">
        <el-form-item label="关联行程" required>
          <el-select v-model="memForm.tripId" style="width: 100%;" class="form-select">
            <el-option v-for="t in trips" :key="t.id" :label="t.title" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="memForm.memoryType" style="width: 100%;" class="form-select">
            <el-option label="发现的偏好" value="PREFERENCE_DISCOVERED" />
            <el-option label="经验教训" value="LESSON_LEARNED" />
            <el-option label="亮点" value="HIGHLIGHT" />
            <el-option label="不足" value="LOWLIGHT" />
            <el-option label="建议" value="ADVICE" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="memForm.content" type="textarea" :rows="4" placeholder="例如：用户很喜欢温泉酒店，下次可以推荐..." class="form-input" />
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="memForm.tags" multiple style="width: 100%;" class="form-select">
            <el-option label="住宿" value="住宿" />
            <el-option label="美食" value="美食" />
            <el-option label="交通" value="交通" />
            <el-option label="景点" value="景点" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="dialog-btn" @click="showMemForm = false">取消</el-button>
        <el-button type="primary" class="dialog-btn primary" @click="saveMemory">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-header {
  margin-bottom: 32px;
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

.pref-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 24px;
  }
  :deep(.el-tabs__nav-wrap::after) {
    display: none;
  }
  :deep(.el-tabs__item) {
    color: var(--text-secondary);
    font-size: 14px;
    font-weight: 500;
    padding: 12px 24px;
    margin-right: 4px;
    border-radius: 12px 12px 0 0;
    background: var(--input-bg);
    border: 1px solid var(--border-color);
    border-bottom: none;
  }
  :deep(.el-tabs__item.is-active) {
    color: var(--text-primary);
    background: rgba(var(--primary-rgb), 0.15);
    border-color: rgba(var(--primary-rgb), 0.3);
    border-bottom: none;
  }
  :deep(.el-tabs__content) {
    background: var(--input-bg);
    border-radius: 0 16px 16px 16px;
    border: 1px solid var(--border-color);
    padding: 24px;
  }
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-desc {
  font-size: 13px;
  color: var(--text-muted);
}

.add-btn {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
  border: none;
  border-radius: 10px;
}

.empty-state {
  text-align: center;
  padding: 60px 40px;
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

.pref-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.pref-card {
  background: var(--input-bg);
  border-radius: 14px;
  padding: 16px;
  border: 1px solid var(--border-color);
  animation: fadeInUp 0.5s ease-out forwards;
  opacity: 0;
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

.pref-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.pref-category {
  display: flex;
  align-items: center;
  gap: 8px;
}

.category-tag {
  font-size: 12px;
}

.pref-source {
  font-size: 11px;
  color: var(--text-muted);
}

.pref-actions {
  display: flex;
  gap: 4px;
}

.action-btn {
  font-size: 12px;
}

.delete-btn {
  color: var(--error-color);
}

.pref-data {
  font-size: 12px;
  color: var(--text-secondary);
  background: var(--overlay-bg-dark);
  padding: 12px;
  border-radius: 10px;
  max-height: 140px;
  overflow: auto;
  white-space: pre-wrap;
  margin-bottom: 10px;
  border: 1px solid var(--border-color);
}

.pref-time {
  font-size: 11px;
  color: var(--text-muted);
}

.mem-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.mem-card {
  background: var(--input-bg);
  border-radius: 14px;
  padding: 18px;
  border-left: 4px solid;
  animation: fadeInUp 0.5s ease-out forwards;
  opacity: 0;
  border-right: 1px solid var(--border-color);
  border-top: 1px solid var(--border-color);
  border-bottom: 1px solid var(--border-color);
}

.mem-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.mem-type-tag {
  font-size: 12px;
}

.mem-trip {
  font-size: 13px;
  color: var(--text-muted);
}

.mem-content {
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-primary);
  margin-bottom: 10px;
}

.mem-tags {
  margin-bottom: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.mem-tag {
  color: var(--primary-light);
  border-color: rgba(var(--primary-rgb), 0.3);
  background: var(--hover-bg);
}

.mem-time {
  font-size: 11px;
  color: var(--text-muted);
}

.pref-dialog {
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

.mem-dialog {
  :deep(.el-dialog) {
    background: var(--input-bg);
    backdrop-filter: blur(20px);
    border: 1px solid rgba(var(--primary-rgb), 0.2);
    border-radius: 20px;
  }
  :deep(.el-dialog__title) {
    color: var(--text-primary);
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
  .pref-grid {
    grid-template-columns: 1fr;
  }

  :deep(.pref-tabs .el-tabs__item) {
    padding: 10px 16px;
    font-size: 13px;
  }
}
</style>