<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useTripStore } from '@/stores/trip'
import { tripApi } from '@/api/trip'
import { weatherApi } from '@/api/weather'
import { shareApi } from '@/api/share'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { CreateTripDayRequest, CreateTripActivityRequest, WeatherInfo, TripShareVO, CreateShareRequest } from '@/types/api'
import { Plus, Delete, Edit, ArrowLeft, MagicStick, Check, Close, ArrowDown, ArrowRight, Share, Sunny, MapLocation, Calendar, Wallet } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

interface RouteProps {
  id?: string | number
  editMode?: boolean
}

const props = defineProps<RouteProps>()
const tripId = computed(() => {
  const id = Number(props.id)
  return Number.isNaN(id) ? 0 : id
})
const trip = useTripStore()

const editMode = ref(Boolean(props.editMode))
const showDayForm = ref(false)
const showActivityForm = ref(false)
const activeDayId = ref<number | null>(null)

const newDay = ref<CreateTripDayRequest>({
  dayNumber: 0,
  date: '',
  title: '',
  description: '',
})

const newActivity = ref<{
  name: string
  description: string
  startTime: Date | null
  endTime: Date | null
  category: string
  place: string
  notes: string
}>({
  name: '',
  description: '',
  startTime: null,
  endTime: null,
  category: '',
  place: '',
  notes: '',
})

const editingActivity = ref<{ dayId: number; activity: any } | null>(null)
const generating = ref(false)
const expandedDays = ref<Set<number>>(new Set())

const weatherInfo = ref<WeatherInfo | null>(null)
const weatherLoading = ref(false)

const showShareDialog = ref(false)
const shareList = ref<TripShareVO[]>([])
const shareLoading = ref(false)
const newShare = ref<CreateShareRequest>({ shareType: 'VIEW', maxViews: 100, expireDays: 30 })

async function loadWeather() {
  if (!trip.currentTrip?.destinationName || !trip.currentTrip?.startDate || !trip.currentTrip?.endDate) return
  weatherLoading.value = true
  try {
    const res = await weatherApi.getWeather(
      trip.currentTrip.destinationName,
      trip.currentTrip.startDate,
      trip.currentTrip.endDate
    )
    weatherInfo.value = res.data
  } catch (e) {
  } finally {
    weatherLoading.value = false
  }
}

async function openShareDialog() {
  showShareDialog.value = true
  shareLoading.value = true
  try {
    const res = await shareApi.listShares(tripId.value)
    shareList.value = res.data
  } catch (e) {
  } finally {
    shareLoading.value = false
  }
}

async function createShare() {
  try {
    const res = await shareApi.createShare(tripId.value, newShare.value)
    shareList.value.unshift(res.data)
    ElMessage.success('分享链接已创建')
  } catch (e) {
    ElMessage.error('创建分享失败')
  }
}

async function revokeShare(shareId: number) {
  try {
    await shareApi.revokeShare(tripId.value, shareId)
    shareList.value = shareList.value.filter(s => s.id !== shareId)
    ElMessage.success('已撤销分享')
  } catch (e) {
    ElMessage.error('撤销失败')
  }
}

function copyShareLink(token: string) {
  const url = `${window.location.origin}/shared/${token}`
  navigator.clipboard.writeText(url)
  ElMessage.success('链接已复制到剪贴板')
}

function toggleDay(dayId: number) {
  if (expandedDays.value.has(dayId)) {
    expandedDays.value.delete(dayId)
  } else {
    expandedDays.value.add(dayId)
  }
}

function isDayExpanded(dayId: number) {
  return expandedDays.value.has(dayId)
}

function parseTime(timeStr?: string): Date | null {
  if (!timeStr || timeStr === '--:--') return null
  const d = new Date()
  const [h, m] = timeStr.split(':').map(Number)
  d.setHours(h, m, 0, 0)
  return d
}
function formatTime(date: Date | null): string {
  if (!date) return ''
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}
function formatTimeDisplay(timeStr?: string): string {
  if (!timeStr || timeStr === '--:--') return '--:--'
  return timeStr.slice(0, 5)
}

const days = computed(() => trip.currentTrip?.days || [])

async function addDay() {
  if (!tripId.value || tripId.value <= 0) {
    ElMessage.error('无效的行程 ID')
    return
  }
  try {
    const dayNumber = days.value.length + 1
    newDay.value.dayNumber = dayNumber
    await tripApi.createDay(tripId.value, newDay.value)
    ElMessage.success('行程日已添加')
    showDayForm.value = false
    resetDayForm()
    await trip.fetchTrip(tripId.value)
  } catch {
    ElMessage.error('添加失败')
  }
}

function resetDayForm() {
  newDay.value = { dayNumber: 0, date: '', title: '', description: '' }
}

async function deleteDay(dayId: number) {
  try {
    await ElMessageBox.confirm('确定删除这一天吗？', '确认删除', { type: 'warning' })
    await tripApi.deleteDay(tripId.value, dayId)
    ElMessage.success('已删除')
    await trip.fetchTrip(tripId.value)
  } catch {
  }
}

function openAddActivity(dayId: number) {
  activeDayId.value = dayId
  editingActivity.value = null
  newActivity.value = { name: '', description: '', startTime: null, endTime: null, category: '', place: '', notes: '' }
  showActivityForm.value = true
}

function openEditActivity(dayId: number, activity: any) {
  activeDayId.value = dayId
  editingActivity.value = { dayId, activity }
  newActivity.value = {
    name: activity.name,
    description: activity.description,
    startTime: parseTime(activity.startTime),
    endTime: parseTime(activity.endTime),
    category: activity.category,
    place: activity.place,
    notes: activity.notes,
  }
  showActivityForm.value = true
}

async function saveActivity() {
  if (!newActivity.value.name) {
    ElMessage.warning('请输入活动名称')
    return
  }
  const dayId = activeDayId.value!
  const payload = {
    name: newActivity.value.name,
    description: newActivity.value.description,
    startTime: formatTime(newActivity.value.startTime) || undefined,
    endTime: formatTime(newActivity.value.endTime) || undefined,
    category: newActivity.value.category,
    place: newActivity.value.place,
    notes: newActivity.value.notes,
  }
  try {
    if (editingActivity.value) {
      await tripApi.updateActivity(tripId.value, dayId, editingActivity.value.activity.id, payload)
    } else {
      await tripApi.createActivity(tripId.value, dayId, payload)
    }
    ElMessage.success('活动已保存')
    showActivityForm.value = false
    editingActivity.value = null
    await trip.fetchTrip(tripId.value)
  } catch {
    ElMessage.error('保存失败')
  }
}

async function deleteActivity(dayId: number, activityId: number) {
  try {
    await ElMessageBox.confirm('确定删除此活动吗？', '确认删除', { type: 'warning' })
    await tripApi.deleteActivity(tripId.value, dayId, activityId)
    ElMessage.success('已删除')
    await trip.fetchTrip(tripId.value)
  } catch {
  }
}

function getCategoryColor(cat: string) {
  const map: Record<string, string> = {
    sightseeing: '#3b82f6', dining: '#f97316', transport: '#10b981',
    accommodation: 'var(--primary-light)', shopping: '#ec4899',
  }
  return map[cat?.toLowerCase()] || 'var(--text-muted)'
}

function getCategoryLabel(cat: string) {
  const map: Record<string, string> = {
    sightseeing: '观光', dining: '餐饮', transport: '交通',
    accommodation: '住宿', shopping: '购物',
  }
  return map[cat?.toLowerCase()] || cat
}

watch(() => tripId.value, async (newId) => {
  if (newId) {
    await trip.fetchTrip(newId)
    loadWeather()
  }
}, { immediate: true })
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <el-button :icon="ArrowLeft" link class="back-btn" @click="$router.push('/')">返回行程列表</el-button>
        <div class="trip-title-section">
          <h1 class="page-title">{{ trip.currentTrip?.title || '行程详情' }}</h1>
          <div class="trip-meta" v-if="trip.currentTrip">
            <span class="meta-item">
              <el-icon :size="14" color="var(--primary-color)"><MapLocation /></el-icon>
              {{ trip.currentTrip.destinationName }}
            </span>
            <span class="meta-item">
              <el-icon :size="14" color="var(--primary-color)"><Calendar /></el-icon>
              {{ trip.currentTrip.startDate }} ~ {{ trip.currentTrip.endDate }}
            </span>
            <span class="meta-item">
              <el-icon :size="14" color="var(--primary-color)"><Wallet /></el-icon>
              ¥{{ trip.currentTrip.budget?.toLocaleString() || '--' }}
            </span>
          </div>
        </div>
      </div>
      <div class="header-actions">
        <el-button :icon="Share" class="action-btn" @click="openShareDialog">分享</el-button>
        <el-button :icon="MagicStick" class="action-btn primary" @click="generating = true">AI 优化</el-button>
        <el-button v-if="!editMode" :icon="Edit" class="action-btn" @click="editMode = true">编辑</el-button>
        <el-button v-else :icon="Check" class="action-btn primary" @click="editMode = false">保存</el-button>
        <el-button v-if="editMode" :icon="Close" class="action-btn danger" @click="editMode = false">取消</el-button>
      </div>
    </div>

    <div class="content-wrapper">
      <div class="main-content">
        <div v-if="weatherInfo && weatherInfo.dailyForecasts?.length" class="weather-card">
          <div class="card-header">
            <el-icon :size="18" color="#3b82f6"><Sunny /></el-icon>
            <span>{{ weatherInfo.destination }} 天气预报</span>
          </div>
          <div class="weather-forecast-list">
            <div v-for="f in weatherInfo.dailyForecasts" :key="f.date" class="weather-day">
              <span class="weather-date">{{ f.date }}</span>
              <span class="weather-desc">{{ f.weatherDescription }}</span>
              <span class="weather-temp">{{ f.minTemp }}°C ~ {{ f.maxTemp }}°C</span>
              <span class="weather-rain" v-if="f.precipitation > 0">降雨 {{ f.precipitation }}mm</span>
            </div>
          </div>
        </div>
        <div v-else-if="weatherLoading" class="weather-card">
          <el-skeleton :rows="2" animated />
        </div>

        <div class="days-container">
          <div v-for="day in days" :key="day.id" class="day-card">
            <div class="day-header" @click="toggleDay(day.id)">
              <div class="day-info">
                <el-icon class="collapse-icon" :class="{ 'collapsed': !isDayExpanded(day.id) }">
                  <ArrowDown v-if="isDayExpanded(day.id)" />
                  <ArrowRight v-else />
                </el-icon>
                <span class="day-number">Day {{ day.dayNumber }}</span>
                <span class="day-date">{{ day.date }}</span>
                <span class="day-title">{{ day.title }}</span>
                <span v-if="day.activities" class="day-activity-count">{{ day.activities.length }} 个活动</span>
              </div>
              <div class="day-actions" v-if="editMode" @click.stop>
                <el-button :icon="Plus" link size="small" class="add-activity-btn" @click="openAddActivity(day.id)">添加活动</el-button>
                <el-button :icon="Delete" link type="danger" size="small" @click="deleteDay(day.id)">删除</el-button>
              </div>
            </div>

            <div v-show="isDayExpanded(day.id)" class="activities-list">
              <div v-for="a in day.activities || []" :key="a.id" class="activity-row">
                <span class="activity-time">{{ formatTimeDisplay(a.startTime) }} - {{ formatTimeDisplay(a.endTime) }}</span>
                <span class="activity-name">{{ a.name }}</span>
                <span v-if="a.place" class="activity-place">({{ a.place }})</span>
                <el-tag v-if="a.category" size="small" :style="{ background: getCategoryColor(a.category) + '20', borderColor: getCategoryColor(a.category) + '40', color: getCategoryColor(a.category) }">
                  {{ getCategoryLabel(a.category) }}
                </el-tag>
                <div class="activity-actions" v-if="editMode">
                  <el-button :icon="Edit" link size="small" @click="openEditActivity(day.id, a)">编辑</el-button>
                  <el-button :icon="Delete" link type="danger" size="small" @click="deleteActivity(day.id, a.id)">删除</el-button>
                </div>
              </div>
            </div>
          </div>

          <div v-if="editMode" class="add-day-section">
            <el-button :icon="Plus" class="add-day-btn" @click="showDayForm = true">添加新一天</el-button>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="showDayForm" title="添加行程日" width="400px" @close="resetDayForm" class="dark-dialog">
      <el-form :model="newDay" label-width="80px">
        <el-form-item label="天数">
          <el-input-number v-model="newDay.dayNumber" :min="1" />
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="newDay.date" type="date" />
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="newDay.title" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="newDay.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDayForm = false">取消</el-button>
        <el-button type="primary" @click="addDay">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showActivityForm" :title="editingActivity ? '编辑活动' : '添加活动'" width="500px" class="dark-dialog">
      <el-form :model="newActivity" label-width="100px">
        <el-form-item label="活动名称">
          <el-input v-model="newActivity.name" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="newActivity.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-time-picker v-model="newActivity.startTime" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-time-picker v-model="newActivity.endTime" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="newActivity.category">
            <el-option label="观光" value="sightseeing" />
            <el-option label="餐饮" value="dining" />
            <el-option label="交通" value="transport" />
            <el-option label="住宿" value="accommodation" />
            <el-option label="购物" value="shopping" />
          </el-select>
        </el-form-item>
        <el-form-item label="地点">
          <el-input v-model="newActivity.place" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="newActivity.notes" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showActivityForm = false">取消</el-button>
        <el-button type="primary" @click="saveActivity">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="generating" title="AI 优化行程" width="500px" :close-on-click-modal="false" class="dark-dialog">
      <div class="generating-content">
        <div class="generating-icon-wrapper">
          <el-icon :size="48" color="var(--primary-color)"><MagicStick /></el-icon>
          <div class="generating-glow"></div>
        </div>
        <p>AI 正在优化您的行程，请稍候...</p>
      </div>
    </el-dialog>

    <el-dialog v-model="showShareDialog" title="行程分享" width="600px" class="dark-dialog">
      <div v-loading="shareLoading">
        <div class="share-create-section">
          <h4>创建新分享链接</h4>
          <el-form :model="newShare" label-width="100px" size="small">
            <el-form-item label="分享类型">
              <el-select v-model="newShare.shareType" style="width: 100%;">
                <el-option label="仅查看" value="VIEW" />
                <el-option label="可评论" value="COMMENT" />
                <el-option label="可编辑" value="EDIT" />
              </el-select>
            </el-form-item>
            <el-form-item label="最大访问次数">
              <el-input-number v-model="newShare.maxViews" :min="1" :max="10000" />
            </el-form-item>
            <el-form-item label="有效天数">
              <el-input-number v-model="newShare.expireDays" :min="1" :max="365" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Share" @click="createShare">生成分享链接</el-button>
            </el-form-item>
          </el-form>
        </div>

        <el-divider />

        <div class="share-list-section">
          <h4>已有分享 ({{ shareList.length }})</h4>
          <el-empty v-if="!shareList.length" description="暂无分享" :image-size="60" />
          <div v-else class="share-list">
            <div v-for="s in shareList" :key="s.id" class="share-item">
              <div class="share-item-info">
                <div class="share-token">{{ s.shareToken }}</div>
                <div class="share-meta">
                  <el-tag size="small" :type="s.status === 'ACTIVE' ? 'success' : 'info'">
                    {{ s.status === 'ACTIVE' ? '有效' : '已失效' }}
                  </el-tag>
                  <span class="share-views">访问 {{ s.currentViews }}/{{ s.maxViews }}</span>
                  <span v-if="s.expiresAt" class="share-expires">到期 {{ s.expiresAt }}</span>
                </div>
              </div>
              <div class="share-item-actions">
                <el-button link type="primary" size="small" @click="copyShareLink(s.shareToken)">复制链接</el-button>
                <el-button link type="danger" size="small" @click="revokeShare(s.id)">撤销</el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container {
  min-height: 100%;
  background: var(--bg-secondary);
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
}

.header-left {
  flex: 1;
}

.back-btn {
  color: var(--text-secondary);
  font-size: 13px;
  padding-left: 0;
}

.back-btn:hover {
  color: var(--primary-color);
}

.trip-title-section {
  margin-top: 8px;
}

.page-title {
  font-size: 26px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 10px 0;
}

.trip-meta {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--text-secondary);
}

.header-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.action-btn {
  background: var(--border-color);
  border: 1px solid var(--border-color-light);
  color: var(--text-secondary);
  border-radius: 10px;
  font-size: 13px;
  padding: 8px 16px;
  transition: all 0.2s;
}

.action-btn:hover {
  background: var(--border-color-light);
}

.action-btn.primary {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
  border: none;
  color: #fff;
}

.action-btn.primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 15px rgba(var(--primary-rgb), 0.3);
}

.action-btn.danger {
  border-color: rgba(var(--error-rgb), 0.3);
  color: var(--error-color);
}

.action-btn.danger:hover {
  background: rgba(var(--error-rgb), 0.1);
}

.content-wrapper {
  max-width: 900px;
  margin: 0 auto;
}

.main-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.weather-card {
  background: var(--input-bg);
  border-radius: 16px;
  padding: 20px;
  border: 1px solid var(--hover-bg);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 16px;
}

.weather-forecast-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.weather-day {
  display: grid;
  grid-template-columns: 100px 1fr 140px 100px;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--input-bg);
  border-radius: 10px;
  font-size: 13px;
}

.weather-date {
  font-weight: 600;
  color: var(--primary-color);
}

.weather-desc {
  color: var(--text-secondary);
}

.weather-temp {
  color: var(--text-primary);
  font-weight: 500;
}

.weather-rain {
  color: #3b82f6;
  font-size: 12px;
}

.days-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.day-card {
  background: var(--input-bg);
  border-radius: 16px;
  border: 1px solid var(--hover-bg);
  overflow: hidden;
}

.day-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  cursor: pointer;
  transition: background 0.2s;
}

.day-header:hover {
  background: rgba(var(--primary-rgb), 0.05);
}

.day-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.collapse-icon {
  font-size: 14px;
  color: var(--primary-color);
  transition: transform 0.2s ease;
}

.collapse-icon.collapsed {
  transform: rotate(-90deg);
}

.day-number {
  font-weight: 700;
  color: var(--primary-color);
  font-size: 16px;
}

.day-date {
  color: var(--text-secondary);
  font-size: 14px;
}

.day-title {
  font-weight: 600;
  color: var(--text-primary);
  font-size: 15px;
}

.day-activity-count {
  font-size: 12px;
  color: var(--text-muted);
  background: var(--hover-bg);
  padding: 3px 10px;
  border-radius: 10px;
}

.day-actions {
  display: flex;
  gap: 8px;
}

.add-activity-btn {
  color: var(--primary-color);
}

.add-activity-btn:hover {
  color: var(--primary-light);
}

.activities-list {
  display: flex;
  flex-direction: column;
  gap: 0;
  border-top: 1px solid var(--hover-bg);
}

.activity-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.08);
  transition: background 0.2s;
}

.activity-row:hover {
  background: rgba(var(--primary-rgb), 0.05);
}

.activity-time {
  font-size: 13px;
  color: var(--text-muted);
  min-width: 120px;
  flex-shrink: 0;
}

.activity-name {
  flex: 1;
  font-size: 14px;
  color: var(--text-primary);
}

.activity-place {
  font-size: 13px;
  color: var(--text-muted);
}

.activity-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.add-day-section {
  padding: 16px;
  text-align: center;
}

.add-day-btn {
  background: var(--hover-bg);
  border: 1px dashed rgba(var(--primary-rgb), 0.3);
  color: var(--primary-color);
  border-radius: 12px;
  font-size: 14px;
  padding: 12px 24px;
  transition: all 0.2s;
}

.add-day-btn:hover {
  background: rgba(var(--primary-rgb), 0.2);
  border-style: solid;
}

.share-create-section {
  padding: 4px 0;
}

.share-create-section h4 {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 12px 0;
}

.share-list-section {
  padding: 4px 0;
}

.share-list-section h4 {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 12px 0;
}

.share-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.share-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px;
  background: var(--input-bg);
  border-radius: 12px;
  border: 1px solid var(--border-color);
}

.share-item-info {
  flex: 1;
  min-width: 0;
}

.share-token {
  font-family: 'Courier New', monospace;
  font-size: 13px;
  color: var(--primary-color);
  word-break: break-all;
  margin-bottom: 8px;
}

.share-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: var(--text-muted);
}

.share-item-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.generating-content {
  text-align: center;
  padding: 30px 20px;
}

.generating-icon-wrapper {
  position: relative;
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.generating-glow {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 100px;
  height: 100px;
  background: radial-gradient(circle, rgba(var(--primary-rgb), 0.3), transparent);
  border-radius: 50%;
  animation: avatar-pulse 2s ease-in-out infinite;
}

@keyframes avatar-pulse {
  0%, 100% { transform: translate(-50%, -50%) scale(1); opacity: 0.6; }
  50% { transform: translate(-50%, -50%) scale(1.1); opacity: 0.3; }
}

.generating-content p {
  font-size: 15px;
  color: var(--text-secondary);
  margin: 0;
}

:deep(.dark-dialog .el-dialog) {
  background: var(--input-bg);
  border: 1px solid rgba(var(--primary-rgb), 0.2);
  border-radius: 16px;
}

:deep(.dark-dialog .el-dialog__header) {
  border-bottom: 1px solid var(--hover-bg);
}

:deep(.dark-dialog .el-dialog__title) {
  color: var(--text-primary);
}

:deep(.dark-dialog .el-dialog__body) {
  color: var(--text-primary);
}

:deep(.dark-dialog .el-input__inner),
:deep(.dark-dialog .el-textarea__inner),
:deep(.dark-dialog .el-select .el-input__inner) {
  background: var(--overlay-bg-dark);
  border: 1px solid var(--border-color-light);
  color: var(--text-primary);
}

:deep(.dark-dialog .el-input__inner::placeholder),
:deep(.dark-dialog .el-textarea__inner::placeholder) {
  color: var(--text-secondary);
}

:deep(.dark-dialog .el-form-item__label) {
  color: var(--text-secondary);
}

@media (max-width: 768px) {
  .page-container {
    padding: 16px 12px;
  }

  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .header-actions {
    justify-content: flex-end;
  }

  .trip-meta {
    gap: 12px;
  }

  .weather-day {
    grid-template-columns: 1fr;
    gap: 6px;
  }

  .day-info {
    flex-wrap: wrap;
    gap: 8px;
  }

  .day-actions {
    flex-wrap: wrap;
  }

  .activity-row {
    flex-wrap: wrap;
    gap: 8px;
  }

  .activity-time {
    min-width: auto;
  }

  .share-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}
</style>