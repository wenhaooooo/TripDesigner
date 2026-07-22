<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { priceMonitorApi, type TrainTicketInfo } from '@/api/priceMonitor'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { PriceMonitorVO, CreateMonitorRequest, PricePoint } from '@/types/api'
import { Plus, Delete, Close, TrendCharts, Position, Search, Clock } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const monitors = ref<PriceMonitorVO[]>([])
const loading = ref(false)

const showDialog = ref(false)
const submitting = ref(false)
const searchingTrains = ref(false)
const availableTrains = ref<TrainTicketInfo[]>([])
const showTrainSelector = ref(false)
const selectedTrainId = ref<number | null>(null)

const form = ref<CreateMonitorRequest>({
  destination: '',
  departure: '',
  ticketClass: '',
  departureTime: '',
  arrivalTime: '',
  monitorType: 'FLIGHT',
  targetPrice: undefined,
})

const monitorTypeOptions = [
  { label: '机票', value: 'FLIGHT' },
  { label: '酒店', value: 'HOTEL' },
  { label: '火车', value: 'TRAIN' },
]

const ticketClassOptions: Record<string, Array<{ label: string; value: string }>> = {
  FLIGHT: [
    { label: '经济舱', value: 'ECONOMY' },
    { label: '商务舱', value: 'BUSINESS' },
    { label: '头等舱', value: 'FIRST' },
  ],
  TRAIN: [
    { label: '无座', value: 'STANDING' },
    { label: '硬座', value: 'HARD_SEAT' },
    { label: '硬卧', value: 'HARD_BERTH' },
    { label: '软卧', value: 'SOFT_BERTH' },
    { label: '二等座', value: 'SECOND_CLASS' },
    { label: '一等座', value: 'FIRST_CLASS' },
    { label: '商务座', value: 'BUSINESS_CLASS' },
  ],
  HOTEL: [],
}

watch(() => form.value.monitorType, (newType) => {
  if (newType !== 'TRAIN') {
    showTrainSelector.value = false
    availableTrains.value = []
    selectedTrainId.value = null
  }
})

onMounted(() => {
  loadMonitors()
})

async function loadMonitors() {
  loading.value = true
  try {
    const res = await priceMonitorApi.list()
    monitors.value = res.data
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.value = {
    destination: '',
    departure: '',
    ticketClass: '',
    departureTime: '',
    arrivalTime: '',
    monitorType: 'FLIGHT',
    targetPrice: undefined,
  }
  availableTrains.value = []
  showTrainSelector.value = false
  selectedTrainId.value = null
  showDialog.value = true
}

async function searchTrains() {
  if (!form.value.departure?.trim() || !form.value.destination?.trim()) {
    ElMessage.warning('请先输入出发地和目的地')
    return
  }
  searchingTrains.value = true
  try {
    const res = await priceMonitorApi.listTrains({
      departure: form.value.departure.trim(),
      destination: form.value.destination.trim(),
      ticketClass: form.value.ticketClass?.trim() || undefined,
    })
    availableTrains.value = res.data
    showTrainSelector.value = true
  } finally {
    searchingTrains.value = false
  }
}

function selectTrain(train: TrainTicketInfo) {
  selectedTrainId.value = train.id
  form.value.ticketClass = train.ticketClass
  form.value.departureTime = train.departureTime
  form.value.arrivalTime = train.arrivalTime
  if (form.value.targetPrice === undefined) {
    form.value.targetPrice = train.price
  }
  showTrainSelector.value = false
}

function clearTrainSelection() {
  selectedTrainId.value = null
  form.value.ticketClass = ''
  form.value.departureTime = ''
  form.value.arrivalTime = ''
}

async function submit() {
  if (!form.value.destination?.trim()) {
    ElMessage.warning('请输入目的地')
    return
  }
  submitting.value = true
  try {
    await priceMonitorApi.create({
      destination: form.value.destination.trim(),
      departure: form.value.departure?.trim() || undefined,
      ticketClass: form.value.ticketClass?.trim() || undefined,
      departureTime: form.value.departureTime || undefined,
      arrivalTime: form.value.arrivalTime || undefined,
      monitorType: form.value.monitorType,
      targetPrice: form.value.targetPrice,
    })
    ElMessage.success('监测已创建')
    showDialog.value = false
    await loadMonitors()
  } catch {
    // 错误信息由全局拦截器提示
  } finally {
    submitting.value = false
  }
}

async function handleCancel(monitor: PriceMonitorVO) {
  const title = monitor.departure ? `${monitor.departure} - ${monitor.destination}` : monitor.destination
  try {
    await ElMessageBox.confirm(
      `确定取消监测「${title}」吗？取消后将不再追踪价格。`,
      '确认取消',
      { type: 'warning' },
    )
    await priceMonitorApi.cancel(monitor.id)
    ElMessage.success('已取消监测')
    await loadMonitors()
  } catch {
    // cancelled
  }
}

async function handleDelete(monitor: PriceMonitorVO) {
  const title = monitor.departure ? `${monitor.departure} - ${monitor.destination}` : monitor.destination
  try {
    await ElMessageBox.confirm(
      `确定删除监测「${title}」吗？该操作不可恢复。`,
      '确认删除',
      { type: 'warning' },
    )
    await priceMonitorApi.delete(monitor.id)
    ElMessage.success('已删除')
    await loadMonitors()
  } catch {
    // cancelled
  }
}

const stats = computed(() => {
  const total = monitors.value.length
  const active = monitors.value.filter(m => m.status === 'ACTIVE').length
  const triggered = monitors.value.filter(m => m.status === 'TRIGGERED').length
  const others = total - active - triggered
  return [
    { label: '总监测数', value: total, color: 'var(--primary-color)' },
    { label: '活跃中', value: active, color: '#10b981' },
    { label: '已触发', value: triggered, color: 'var(--error-color)' },
    { label: '其他', value: others, color: 'var(--text-muted)' },
  ]
})

function getMonitorTypeLabel(type: string) {
  const map: Record<string, string> = {
    FLIGHT: '机票',
    HOTEL: '酒店',
    TRAIN: '火车',
  }
  return map[type] || type
}

function getMonitorTypeTag(type: string) {
  const map: Record<string, string> = {
    FLIGHT: 'primary',
    HOTEL: 'warning',
    TRAIN: 'success',
  }
  return map[type] || ''
}

function getTicketClassLabel(ticketClass: string | null) {
  if (!ticketClass) return ''
  const map: Record<string, string> = {
    STANDING: '无座',
    HARD_SEAT: '硬座',
    HARD_BERTH: '硬卧',
    SOFT_BERTH: '软卧',
    SECOND_CLASS: '二等座',
    FIRST_CLASS: '一等座',
    BUSINESS_CLASS: '商务座',
    ECONOMY: '经济舱',
    BUSINESS: '商务舱',
    FIRST: '头等舱',
  }
  return map[ticketClass] || ticketClass
}

function formatTime(timeStr: string | null | undefined) {
  if (!timeStr) return ''
  return timeStr.substring(0, 5)
}

function getStatusLabel(status: string) {
  const map: Record<string, string> = {
    ACTIVE: '监测中',
    TRIGGERED: '已触发',
    EXPIRED: '已过期',
    CANCELLED: '已取消',
  }
  return map[status] || status
}

function getStatusType(status: string) {
  const map: Record<string, string> = {
    ACTIVE: 'success',
    TRIGGERED: 'danger',
    EXPIRED: 'info',
    CANCELLED: 'info',
  }
  return map[status] || 'info'
}

function getStatusColor(status: string) {
  const map: Record<string, string> = {
    ACTIVE: '#10b981',
    TRIGGERED: 'var(--error-color)',
    EXPIRED: 'var(--text-muted)',
    CANCELLED: 'var(--text-muted)',
  }
  return map[status] || 'var(--text-muted)'
}

function formatPrice(price: number | null | undefined) {
  if (price === null || price === undefined) return '--'
  return `¥${price.toLocaleString()}`
}

function formatDate(date: string) {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

function buildSparkline(history: PricePoint[]) {
  if (!history || history.length === 0) return null
  const sorted = [...history].sort(
    (a, b) => new Date(a.recordedAt).getTime() - new Date(b.recordedAt).getTime(),
  )
  const prices = sorted.map(p => p.price)
  const min = Math.min(...prices)
  const max = Math.max(...prices)
  const range = max - min || 1
  const width = 120
  const height = 32
  const stepX = prices.length > 1 ? width / (prices.length - 1) : 0
  const points = prices
    .map((p, i) => {
      const x = i * stepX
      const y = height - ((p - min) / range) * height
      return `${x.toFixed(2)},${y.toFixed(2)}`
    })
    .join(' ')
  const lastPrice = prices[prices.length - 1]
  const firstPrice = prices[0]
  const trendUp = lastPrice >= firstPrice
  return {
    points,
    width,
    height,
    color: trendUp ? 'var(--error-color)' : '#10b981',
    count: prices.length,
    firstPrice,
    lastPrice,
  }
}

function priceTrend(history: PricePoint[]) {
  const spark = buildSparkline(history)
  if (!spark) return null
  const diff = spark.lastPrice - spark.firstPrice
  const percent = spark.firstPrice === 0 ? 0 : (diff / spark.firstPrice) * 100
  return {
    up: diff >= 0,
    percent: Math.abs(percent).toFixed(1),
  }
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-content">
        <div class="header-text">
          <h1 class="page-title">价格监测</h1>
          <p class="page-subtitle">实时追踪机票、酒店、火车票价格变化</p>
        </div>
        <el-button type="primary" :icon="Plus" class="create-btn" @click="openCreate">
          创建监测
        </el-button>
      </div>
    </div>

    <el-row :gutter="16" class="stat-row">
      <el-col v-for="(stat, index) in stats" :key="stat.label" :xs="12" :sm="6">
        <el-card :body-style="{ padding: '24px' }" class="stat-card" :style="{ animationDelay: `${index * 100}ms` }">
          <div class="stat-value" :style="{ color: stat.color }">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <div v-loading="loading" class="monitor-section">
      <div v-if="monitors.length === 0" class="empty-state">
        <div class="empty-icon">
          <TrendCharts :size="64" color="var(--text-muted)" />
        </div>
        <h3 class="empty-title">还没有价格监测</h3>
        <p class="empty-desc">设置价格监测，第一时间获取降价提醒</p>
        <el-button type="primary" :icon="Plus" class="empty-btn" @click="openCreate">创建第一个监测</el-button>
      </div>

      <div v-else class="monitor-grid">
        <div
          v-for="(monitor, index) in monitors"
          :key="monitor.id"
          class="monitor-card"
          :style="{ animationDelay: `${index * 80}ms` }"
        >
          <div class="card-header">
            <div class="card-title-area">
              <el-icon class="card-icon"><Position /></el-icon>
              <span class="card-title">
                {{ monitor.departure ? `${monitor.departure} - ${monitor.destination}` : monitor.destination }}
              </span>
              <el-tag :type="getMonitorTypeTag(monitor.monitorType)" effect="light" size="small" class="type-tag">
                {{ getMonitorTypeLabel(monitor.monitorType) }}
              </el-tag>
              <el-tag v-if="monitor.ticketClass" type="info" effect="light" size="small" class="type-tag">
                {{ getTicketClassLabel(monitor.ticketClass) }}
              </el-tag>
              <span v-if="monitor.departureTime" class="time-display">
                {{ formatTime(monitor.departureTime) }} → {{ formatTime(monitor.arrivalTime) }}
              </span>
            </div>
            <el-tag
              :type="getStatusType(monitor.status)"
              effect="dark"
              size="small"
              class="status-tag"
            >
              {{ getStatusLabel(monitor.status) }}
            </el-tag>
          </div>

          <div class="price-row">
            <div class="price-cell">
              <div class="price-label">当前价格</div>
              <div class="price-value primary">{{ formatPrice(monitor.currentPrice) }}</div>
            </div>
            <div class="price-cell">
              <div class="price-label">最低价格</div>
              <div class="price-value success">{{ formatPrice(monitor.lowestPrice) }}</div>
            </div>
            <div class="price-cell">
              <div class="price-label">目标价格</div>
              <div class="price-value warning">{{ formatPrice(monitor.targetPrice) }}</div>
            </div>
          </div>

          <div v-if="monitor.targetPrice && monitor.currentPrice" class="progress-area">
            <el-progress
              :percentage="Math.min(
                100,
                Math.round(
                  (monitor.targetPrice / monitor.currentPrice) * 100,
                ),
              )"
              :color="getStatusColor(monitor.status)"
              :stroke-width="6"
              :show-text="false"
              class="price-progress"
            />
            <span class="progress-hint">
              距目标价格
              <template v-if="monitor.currentPrice <= monitor.targetPrice">
                <em class="reached">已达成</em>
              </template>
              <template v-else>
                还需降
                <em>¥{{ (monitor.currentPrice - monitor.targetPrice).toLocaleString() }}</em>
              </template>
            </span>
          </div>

          <div class="history-area">
            <div class="history-title">
              <el-icon><TrendCharts /></el-icon>
              <span>价格走势</span>
              <el-tag
                v-if="priceTrend(monitor.priceHistory)"
                :type="priceTrend(monitor.priceHistory)!.up ? 'danger' : 'success'"
                size="small"
                effect="plain"
                class="trend-tag"
              >
                {{ priceTrend(monitor.priceHistory)!.up ? '↑' : '↓' }}
                {{ priceTrend(monitor.priceHistory)!.percent }}%
              </el-tag>
            </div>

            <div v-if="monitor.priceHistory && monitor.priceHistory.length > 0" class="history-content">
              <svg
                :viewBox="`0 0 ${buildSparkline(monitor.priceHistory)!.width} ${buildSparkline(monitor.priceHistory)!.height}`"
                class="sparkline"
                preserveAspectRatio="none"
              >
                <polyline
                  :points="buildSparkline(monitor.priceHistory)!.points"
                  fill="none"
                  :stroke="buildSparkline(monitor.priceHistory)!.color"
                  stroke-width="1.5"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
              <div class="history-list">
                <div
                  v-for="(point, idx) in [...monitor.priceHistory]
                    .sort((a, b) => new Date(b.recordedAt).getTime() - new Date(a.recordedAt).getTime())
                    .slice(0, 3)"
                  :key="idx"
                  class="history-item"
                >
                  <span class="history-price">¥{{ point.price.toLocaleString() }}</span>
                  <span class="history-date">{{ formatDate(point.recordedAt) }}</span>
                </div>
              </div>
            </div>
            <div v-else class="history-empty">暂无价格记录</div>
          </div>

          <div class="card-footer">
            <span class="created-time">创建于 {{ formatDate(monitor.createdAt) }}</span>
            <div class="footer-actions">
              <el-button
                v-if="monitor.status === 'ACTIVE'"
                :icon="Close"
                link
                size="small"
                class="action-btn"
                @click="handleCancel(monitor)"
              >
                取消
              </el-button>
              <el-button
                :icon="Delete"
                link
                type="danger"
                size="small"
                class="action-btn delete-btn"
                @click="handleDelete(monitor)"
              >
                删除
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="showDialog" title="创建价格监测" width="500px" class="create-dialog">
      <el-form :model="form" label-width="90px" class="create-form">
        <el-form-item label="目的地" required>
          <el-input
            v-model="form.destination"
            placeholder="例如：东京"
            maxlength="50"
            show-word-limit
            class="form-input"
          />
        </el-form-item>
        <el-form-item label="出发地">
          <el-input
            v-model="form.departure"
            placeholder="例如：北京（机票/火车必填）"
            maxlength="50"
            show-word-limit
            class="form-input"
          />
        </el-form-item>
        <el-form-item label="座位等级">
          <div style="display: flex; gap: 8px;">
            <el-select
              v-model="form.ticketClass"
              style="flex: 1;"
              placeholder="选择座位等级（可选）"
              clearable
              class="form-select"
            >
              <el-option
                v-for="opt in ticketClassOptions[form.monitorType || 'FLIGHT']"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
            <el-button
              v-if="form.monitorType === 'TRAIN'"
              type="primary"
              :icon="Search"
              :loading="searchingTrains"
              class="search-train-btn"
              @click="searchTrains"
            >
              搜索车次
            </el-button>
          </div>
        </el-form-item>
        <el-form-item v-if="selectedTrainId" label="已选车次">
          <div class="selected-train">
            <span>{{ formatTime(form.departureTime) }} - {{ formatTime(form.arrivalTime) }}</span>
            <span class="train-price">¥{{ form.targetPrice?.toLocaleString() }}</span>
            <el-button link type="danger" size="small" @click="clearTrainSelection">清除</el-button>
          </div>
        </el-form-item>
        <el-form-item label="发车时间">
          <el-time-picker
            v-model="form.departureTime"
            style="width: 100%;"
            placeholder="选择发车/出发时间"
            format="HH:mm"
            value-format="HH:mm:ss"
            class="form-picker"
          />
        </el-form-item>
        <el-form-item label="到达时间">
          <el-time-picker
            v-model="form.arrivalTime"
            style="width: 100%;"
            placeholder="选择到达时间"
            format="HH:mm"
            value-format="HH:mm:ss"
            class="form-picker"
          />
        </el-form-item>
        <el-form-item label="监测类型">
          <el-select v-model="form.monitorType" style="width: 100%;" class="form-select">
            <el-option
              v-for="opt in monitorTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="目标价格">
          <el-input-number
            v-model="form.targetPrice"
            :min="0"
            :precision="2"
            :step="100"
            placeholder="期望达到的价格"
            style="width: 100%;"
            class="form-input"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="dialog-btn" @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" class="dialog-btn primary" @click="submit">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showTrainSelector" title="选择车次" width="600px" class="train-dialog">
      <div v-loading="searchingTrains">
        <div v-if="availableTrains.length === 0" class="empty-state">
          <el-empty description="未找到匹配的车次" />
        </div>
        <div v-else class="train-list">
          <div
            v-for="train in availableTrains"
            :key="train.id"
            class="train-item"
            :class="{ selected: selectedTrainId === train.id }"
            @click="selectTrain(train)"
          >
            <div class="train-header">
              <span class="train-number">{{ train.trainNumber }}</span>
              <span class="train-type">{{ train.trainType === 'CRH' ? '高铁' : '普速' }}</span>
            </div>
            <div class="train-time">
              <span class="time">{{ formatTime(train.departureTime) }}</span>
              <span class="arrow">→</span>
              <span class="time">{{ formatTime(train.arrivalTime) }}</span>
            </div>
            <div class="train-info">
              <span class="duration">{{ train.durationMinutes }}分钟</span>
              <span class="ticket-class">{{ getTicketClassLabel(train.ticketClass) }}</span>
              <span class="price">¥{{ train.price.toLocaleString() }}</span>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button class="dialog-btn" @click="showTrainSelector = false">关闭</el-button>
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

.monitor-section {
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

.monitor-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 16px;
}

.monitor-card {
  background: var(--input-bg);
  backdrop-filter: blur(12px);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  transition: all 0.3s ease;
  animation: fadeInUp 0.5s ease-out forwards;
  opacity: 0;
  border-top: 3px solid rgba(var(--primary-rgb), 0.3);
}

.monitor-card:hover {
  background: var(--card-bg-hover);
  border-color: rgba(var(--primary-rgb), 0.3);
  transform: translateY(-2px);
  box-shadow: 0 8px 25px var(--shadow-sm);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.card-title-area {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex: 1;
  flex-wrap: wrap;
}

.card-icon {
  color: var(--primary-color);
  font-size: 16px;
  flex-shrink: 0;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.type-tag {
  flex-shrink: 0;
}

.time-display {
  font-size: 13px;
  color: var(--text-muted);
  white-space: nowrap;
}

.status-tag {
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 20px;
}

.price-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-bottom: 12px;
}

.price-cell {
  background: var(--input-bg);
  border-radius: 10px;
  padding: 12px 8px;
  text-align: center;
  border: 1px solid var(--border-color);
}

.price-label {
  font-size: 11px;
  color: var(--text-muted);
  margin-bottom: 4px;
}

.price-value {
  font-size: 15px;
  font-weight: 600;
  line-height: 1.2;
}

.price-value.primary {
  color: var(--primary-light);
}

.price-value.success {
  color: #10b981;
}

.price-value.warning {
  color: #f59e0b;
}

.progress-area {
  margin-bottom: 14px;
  padding: 0 2px;
}

.price-progress {
  :deep(.el-progress-bar__outer) {
    background: var(--border-color);
    border-radius: 6px;
  }
  :deep(.el-progress-bar__inner) {
    border-radius: 6px;
  }
}

.progress-hint {
  display: block;
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 6px;
}

.progress-hint em {
  font-style: normal;
  font-weight: 600;
  color: var(--primary-light);
}

.progress-hint em.reached {
  color: #10b981;
}

.history-area {
  background: var(--bg-tertiary);
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 12px;
  flex: 1;
  border: 1px solid var(--border-color);
}

.history-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 8px;
}

.trend-tag {
  margin-left: auto;
}

.history-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sparkline {
  width: 100%;
  height: 32px;
  display: block;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
}

.history-price {
  color: var(--text-primary);
  font-weight: 500;
}

.history-date {
  color: var(--text-muted);
}

.history-empty {
  font-size: 12px;
  color: var(--text-muted);
  text-align: center;
  padding: 8px 0;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid var(--border-color);
}

.created-time {
  font-size: 11px;
  color: var(--text-muted);
}

.footer-actions {
  display: flex;
  align-items: center;
  gap: 4px;
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

.search-train-btn {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
  border: none;
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

.selected-train {
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--hover-bg);
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid rgba(var(--primary-rgb), 0.2);
  color: var(--text-primary);
}

.train-price {
  margin-left: auto;
  font-weight: 600;
  color: var(--primary-light);
}

.train-dialog {
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

.train-list {
  max-height: 400px;
  overflow-y: auto;
}

.train-item {
  padding: 16px;
  border: 2px solid transparent;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 8px;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-color);
}

.train-item:hover {
  background: var(--input-bg);
  border-color: rgba(var(--primary-rgb), 0.3);
}

.train-item.selected {
  background: rgba(var(--primary-rgb), 0.15);
  border-color: rgba(var(--primary-rgb), 0.5);
}

.train-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.train-number {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.train-type {
  font-size: 12px;
  color: #fff;
  background: rgba(var(--primary-rgb), 0.5);
  padding: 2px 8px;
  border-radius: 4px;
}

.train-time {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.train-time .time {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
}

.train-time .arrow {
  font-size: 16px;
  color: var(--text-muted);
  margin: 0 16px;
}

.train-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.train-info .duration {
  font-size: 13px;
  color: var(--text-muted);
}

.train-info .ticket-class {
  font-size: 13px;
  color: var(--text-secondary);
  background: var(--border-color);
  padding: 2px 8px;
  border-radius: 4px;
}

.train-info .price {
  margin-left: auto;
  font-size: 16px;
  font-weight: 600;
  color: var(--error-color);
}

@media (max-width: 768px) {
  .monitor-grid {
    grid-template-columns: 1fr;
  }

  .stat-value {
    font-size: 24px;
  }

  .price-value {
    font-size: 13px;
  }

  .train-time .time {
    font-size: 16px;
  }
}
</style>