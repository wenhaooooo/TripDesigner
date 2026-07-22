<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { statisticsApi } from '@/api/statistics'
import type { TripStatisticsVO } from '@/types/api'
import { Trophy, MapLocation, Wallet, Calendar, Star } from '@element-plus/icons-vue'

const stats = ref<TripStatisticsVO | null>(null)
const loading = ref(false)

onMounted(() => {
  loadStats()
})

async function loadStats() {
  loading.value = true
  try {
    const res = await statisticsApi.get()
    stats.value = res.data
  } finally {
    loading.value = false
  }
}

const cards = computed(() => {
  if (!stats.value) return []
  return [
    { label: '总行程数', value: stats.value.totalTrips, color: 'var(--primary-color)', icon: Calendar },
    { label: '已完成', value: stats.value.completedTrips, color: '#10b981', icon: Trophy },
    { label: '总预算', value: `¥${(stats.value.totalBudget || 0).toLocaleString()}`, color: '#f59e0b', icon: Wallet },
    { label: '平均预算', value: `¥${(stats.value.averageBudget || 0).toLocaleString()}`, color: 'var(--primary-light)', icon: Star },
  ]
})

const topDestinations = computed(() => {
  if (!stats.value?.topDestinations) return []
  return stats.value.topDestinations
})

const activityDist = computed(() => {
  if (!stats.value?.activityCategoryDistribution) return []
  return stats.value.activityCategoryDistribution
})

const tripDurationDist = computed(() => {
  if (!stats.value?.tripDurationDistribution) return []
  return stats.value.tripDurationDistribution
})

const monthlyStats = computed(() => {
  if (!stats.value?.monthlyStats) return []
  return stats.value.monthlyStats
})

const achievements = computed(() => stats.value?.achievements || [])

const palette = ['var(--primary-color)', '#10b981', '#f59e0b', 'var(--primary-light)', '#ec4899', '#06b6d4', 'var(--error-color)', '#84cc16']

function getPaletteColor(index: number) {
  return palette[index % palette.length]
}

function getMaxValue(items: Array<Record<string, any>>, valueKey: string) {
  if (!items || items.length === 0) return 1
  return Math.max(...items.map(item => Number(item[valueKey]) || 0), 1)
}

function getDestName(item: Record<string, any>) {
  return (item.destination || item.name || item.destinationName || '未知') as string
}

function getDestCount(item: Record<string, any>) {
  return Number(item.count || item.tripCount || item.total || 0)
}

function getCategoryName(item: Record<string, any>) {
  return (item.category || item.name || '其他') as string
}

function getCategoryCount(item: Record<string, any>) {
  return Number(item.count || item.total || 0)
}

function getDurationLabel(item: Record<string, any>) {
  return (item.duration || item.label || item.range || '未知') as string
}

function getDurationCount(item: Record<string, any>) {
  return Number(item.count || item.total || 0)
}

function getMonthLabel(item: Record<string, any>) {
  return (item.month || item.label || '') as string
}

function getMonthTrips(item: Record<string, any>) {
  return Number(item.tripCount || item.trips || item.count || 0)
}

function getMonthBudget(item: Record<string, any>) {
  return Number(item.totalBudget || item.budget || 0)
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-text">
        <h1 class="page-title">旅行统计</h1>
        <p class="page-subtitle">回顾你的旅行足迹与成就</p>
      </div>
    </div>

    <div v-loading="loading">
      <div v-if="stats">
        <el-row :gutter="16" class="card-row">
          <el-col v-for="(card, idx) in cards" :key="idx" :xs="12" :sm="6">
            <el-card :body-style="{ padding: '24px' }" class="stat-card" :style="{ animationDelay: `${idx * 100}ms` }">
              <div class="stat-icon" :style="{ background: `${card.color}15` }">
                <el-icon :color="card.color"><component :is="card.icon" /></el-icon>
              </div>
              <div class="stat-value" :style="{ color: card.color }">{{ card.value }}</div>
              <div class="stat-label">{{ card.label }}</div>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="16" class="card-row">
          <el-col :xs="24" :md="12">
            <el-card class="chart-card" shadow="never">
              <div class="chart-title">
                <el-icon><MapLocation /></el-icon>
                <span>热门目的地</span>
              </div>
              <div v-if="topDestinations.length === 0" class="empty-data">暂无数据</div>
              <div v-else class="bar-list">
                <div v-for="(item, idx) in topDestinations" :key="idx" class="bar-item">
                  <div class="bar-label">{{ getDestName(item) }}</div>
                  <div class="bar-track">
                    <div
                      class="bar-fill"
                      :style="{
                        width: `${(getDestCount(item) / getMaxValue(topDestinations, 'count')) * 100}%`,
                        background: getPaletteColor(idx),
                      }"
                    ></div>
                  </div>
                  <div class="bar-value">{{ getDestCount(item) }}</div>
                </div>
              </div>
            </el-card>
          </el-col>

          <el-col :xs="24" :md="12">
            <el-card class="chart-card" shadow="never">
              <div class="chart-title">
                <el-icon><Calendar /></el-icon>
                <span>活动类型分布</span>
              </div>
              <div v-if="activityDist.length === 0" class="empty-data">暂无数据</div>
              <div v-else class="bar-list">
                <div v-for="(item, idx) in activityDist" :key="idx" class="bar-item">
                  <div class="bar-label">{{ getCategoryName(item) }}</div>
                  <div class="bar-track">
                    <div
                      class="bar-fill"
                      :style="{
                        width: `${(getCategoryCount(item) / getMaxValue(activityDist, 'count')) * 100}%`,
                        background: getPaletteColor(idx),
                      }"
                    ></div>
                  </div>
                  <div class="bar-value">{{ getCategoryCount(item) }}</div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="16" class="card-row">
          <el-col :xs="24" :md="12">
            <el-card class="chart-card" shadow="never">
              <div class="chart-title">
                <el-icon><Calendar /></el-icon>
                <span>行程时长分布</span>
              </div>
              <div v-if="tripDurationDist.length === 0" class="empty-data">暂无数据</div>
              <div v-else class="bar-list">
                <div v-for="(item, idx) in tripDurationDist" :key="idx" class="bar-item">
                  <div class="bar-label">{{ getDurationLabel(item) }}</div>
                  <div class="bar-track">
                    <div
                      class="bar-fill"
                      :style="{
                        width: `${(getDurationCount(item) / getMaxValue(tripDurationDist, 'count')) * 100}%`,
                        background: getPaletteColor(idx),
                      }"
                    ></div>
                  </div>
                  <div class="bar-value">{{ getDurationCount(item) }}</div>
                </div>
              </div>
            </el-card>
          </el-col>

          <el-col :xs="24" :md="12">
            <el-card class="chart-card" shadow="never">
              <div class="chart-title">
                <el-icon><Wallet /></el-icon>
                <span>月度行程统计</span>
              </div>
              <div v-if="monthlyStats.length === 0" class="empty-data">暂无数据</div>
              <div v-else class="monthly-list">
                <div v-for="(item, idx) in monthlyStats" :key="idx" class="month-row">
                  <div class="month-label">{{ getMonthLabel(item) }}</div>
                  <div class="month-data">
                    <div class="month-trips">
                      <span class="data-num">{{ getMonthTrips(item) }}</span>
                      <span class="data-label">行程</span>
                    </div>
                    <div v-if="getMonthBudget(item) > 0" class="month-budget">
                      <span class="data-num">¥{{ getMonthBudget(item).toLocaleString() }}</span>
                      <span class="data-label">预算</span>
                    </div>
                  </div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-card class="achievement-card" shadow="never">
          <div class="chart-title">
            <el-icon><Trophy /></el-icon>
            <span>成就徽章</span>
          </div>
          <div v-if="achievements.length === 0" class="empty-data">还未解锁任何成就</div>
          <div v-else class="achievement-grid">
            <div v-for="(achievement, idx) in achievements" :key="idx" class="achievement-item">
              <el-icon :size="32" :color="getPaletteColor(idx)">
                <Trophy />
              </el-icon>
              <div class="achievement-name">{{ achievement }}</div>
            </div>
          </div>
        </el-card>
      </div>

      <div v-else-if="!loading" class="empty-state">
        <div class="empty-icon">
          <Star :size="64" color="var(--text-muted)" />
        </div>
        <h3 class="empty-title">暂无统计数据</h3>
        <p class="empty-desc">开始规划你的旅行，查看详细的统计信息</p>
      </div>
    </div>
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

.card-row {
  margin-bottom: 16px;
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

.chart-card,
.achievement-card {
  background: var(--input-bg);
  backdrop-filter: blur(12px);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  height: 100%;
}

.chart-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 20px;
}

.empty-data {
  text-align: center;
  color: var(--text-muted);
  font-size: 13px;
  padding: 32px 0;
}

.bar-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.bar-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.bar-label {
  width: 80px;
  font-size: 13px;
  color: var(--text-secondary);
  flex-shrink: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.bar-track {
  flex: 1;
  height: 24px;
  background: var(--border-color);
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid var(--border-color);
}

.bar-fill {
  height: 100%;
  border-radius: 12px;
  transition: width 0.8s ease;
  min-width: 4px;
  position: relative;
}

.bar-fill::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 50%;
  background: linear-gradient(to bottom, rgba(255, 255, 255, 0.05), transparent);
  border-radius: 12px 12px 0 0;
}

.bar-value {
  width: 48px;
  text-align: right;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.monthly-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.month-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--input-bg);
  border-radius: 10px;
  border: 1px solid var(--border-color);
}

.month-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.month-data {
  display: flex;
  gap: 20px;
}

.month-trips,
.month-budget {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.data-num {
  font-size: 16px;
  font-weight: 600;
  color: var(--primary-light);
}

.month-budget .data-num {
  color: #f59e0b;
}

.data-label {
  font-size: 11px;
  color: var(--text-muted);
}

.achievement-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 16px;
}

.achievement-item {
  background: var(--input-bg);
  border-radius: 16px;
  padding: 24px;
  text-align: center;
  border: 1px solid var(--border-color);
  transition: all 0.3s;
}

.achievement-item:hover {
  background: var(--hover-bg);
  border-color: rgba(var(--primary-rgb), 0.3);
  transform: translateY(-2px);
}

.achievement-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
  margin-top: 10px;
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
}

@media (max-width: 768px) {
  .stat-value {
    font-size: 28px;
  }

  .bar-label {
    width: 60px;
    font-size: 12px;
  }

  .month-data {
    flex-direction: column;
    gap: 4px;
    align-items: flex-end;
  }

  .achievement-grid {
    grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  }
}
</style>