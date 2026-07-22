<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { teamApi } from '@/api/team'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { TravelTeamVO, TeamApplicationVO } from '@/types/api'
import { ArrowLeft, Check, Close, Delete, User } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()

const teamId = Number(route.params.id)
const team = ref<TravelTeamVO | null>(null)
const applications = ref<TeamApplicationVO[]>([])
const loading = ref(false)
const processingId = ref<number | null>(null)

onMounted(() => {
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const [teamRes, appsRes] = await Promise.all([
      teamApi.get(teamId),
      teamApi.listApplications(teamId),
    ])
    team.value = teamRes.data
    applications.value = appsRes.data
  } finally {
    loading.value = false
  }
}

async function approve(app: TeamApplicationVO) {
  processingId.value = app.id
  try {
    await teamApi.approve(app.id)
    ElMessage.success('已通过申请')
    await loadData()
  } catch {
    // 错误由全局拦截器提示
  } finally {
    processingId.value = null
  }
}

async function reject(app: TeamApplicationVO) {
  processingId.value = app.id
  try {
    await teamApi.reject(app.id)
    ElMessage.success('已拒绝申请')
    await loadData()
  } catch {
    // 错误由全局拦截器提示
  } finally {
    processingId.value = null
  }
}

async function handleApply() {
  try {
    const { value: message } = await ElMessageBox.prompt(
      '请输入申请理由（可选）',
      '申请加入队伍',
      {
        confirmButtonText: '提交申请',
        cancelButtonText: '取消',
        inputType: 'textarea',
      },
    )
    await teamApi.apply(teamId, message ? { message } : {})
    ElMessage.success('申请已提交')
    await loadData()
  } catch {
    // cancelled
  }
}

async function handleClose() {
  try {
    await ElMessageBox.confirm('确定关闭该队伍吗？', '确认', { type: 'warning' })
    await teamApi.close(teamId)
    ElMessage.success('已关闭')
    await loadData()
  } catch {
    // cancelled
  }
}

async function handleDelete() {
  try {
    await ElMessageBox.confirm('确定删除该队伍吗？此操作不可恢复。', '确认删除', { type: 'warning' })
    await teamApi.delete(teamId)
    ElMessage.success('已删除')
    router.push('/teams')
  } catch {
    // cancelled
  }
}

async function handleLeave() {
  try {
    await ElMessageBox.confirm('确定退出该队伍吗？', '确认退出', { type: 'warning' })
    await teamApi.leave(teamId)
    ElMessage.success('已退出')
    router.push('/teams')
  } catch {
    // cancelled
  }
}

const pendingApplications = computed(() =>
  applications.value.filter(a => a.status === 'PENDING'),
)

const processedApplications = computed(() =>
  applications.value.filter(a => a.status !== 'PENDING'),
)

function formatDate(date: string) {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

function getApplicationStatusLabel(status: string) {
  const map: Record<string, string> = {
    PENDING: '待审核',
    APPROVED: '已通过',
    REJECTED: '已拒绝',
    CANCELLED: '已取消',
  }
  return map[status] || status
}

function getApplicationStatusType(status: string) {
  const map: Record<string, string> = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
    CANCELLED: 'info',
  }
  return map[status] || 'info'
}

function goBack() {
  router.push('/teams')
}
</script>

<template>
  <div class="page-container">
    <el-button :icon="ArrowLeft" link @click="goBack" style="margin-bottom: 12px;">
      返回队伍列表
    </el-button>

    <div v-loading="loading">
      <div v-if="team" class="team-detail">
        <el-card class="info-card" shadow="never">
          <div class="detail-header">
            <h1 class="detail-title">{{ team.title }}</h1>
            <el-tag effect="dark" size="default">
              {{ team.status === 'RECRUITING' ? '招募中' : team.status === 'CLOSED' ? '已关闭' : team.status }}
            </el-tag>
          </div>

          <div class="detail-meta">
            <div class="meta-row">
              <span class="meta-label">目的地</span>
              <span class="meta-value">{{ team.destination }}</span>
            </div>
            <div class="meta-row">
              <span class="meta-label">日期</span>
              <span class="meta-value">{{ formatDate(team.startDate) }} - {{ formatDate(team.endDate) }}</span>
            </div>
            <div class="meta-row">
              <span class="meta-label">人数</span>
              <span class="meta-value">{{ team.currentMembers }} / {{ team.maxMembers }}</span>
            </div>
            <div class="meta-row">
              <span class="meta-label">创建者</span>
              <span class="meta-value">{{ team.creatorEmail }}</span>
            </div>
            <div v-if="team.interests && team.interests.length > 0" class="meta-row">
              <span class="meta-label">兴趣</span>
              <div class="meta-tags">
                <el-tag v-for="interest in team.interests" :key="interest" size="small" effect="plain">
                  {{ interest }}
                </el-tag>
              </div>
            </div>
            <div v-if="team.description" class="meta-row">
              <span class="meta-label">简介</span>
              <span class="meta-desc">{{ team.description }}</span>
            </div>
          </div>

          <div class="detail-actions">
            <el-button
              v-if="!team.isCreator && !team.isMember && !team.hasApplied && team.status === 'RECRUITING'"
              type="primary"
              @click="handleApply"
            >
              申请加入
            </el-button>
            <el-tag v-if="team.hasApplied && !team.isMember" type="warning">
              已申请，等待审核
            </el-tag>
            <el-button v-if="team.isMember && !team.isCreator" type="danger" @click="handleLeave">
              退出队伍
            </el-button>
            <el-button
              v-if="team.isCreator && team.status === 'RECRUITING'"
              type="warning"
              @click="handleClose"
            >
              关闭招募
            </el-button>
            <el-button v-if="team.isCreator" type="danger" @click="handleDelete">
              删除队伍
            </el-button>
          </div>
        </el-card>

        <el-card v-if="team.isCreator" class="apps-card" shadow="never">
          <div class="section-header">
            <h2 class="section-title">
              待审核申请
              <el-tag v-if="pendingApplications.length > 0" type="warning" size="small">
                {{ pendingApplications.length }}
              </el-tag>
            </h2>
          </div>

          <div v-if="pendingApplications.length === 0" class="empty-state">
            <el-empty description="没有待处理的申请" />
          </div>

          <div v-else class="app-list">
            <div v-for="app in pendingApplications" :key="app.id" class="app-item">
              <div class="app-info">
                <div class="app-header">
                  <el-icon><User /></el-icon>
                  <span class="app-name">{{ app.applicantEmail }}</span>
                  <span class="app-time">{{ formatDate(app.createdAt) }}</span>
                </div>
                <div v-if="app.message" class="app-message">{{ app.message }}</div>
              </div>
              <div class="app-actions">
                <el-button
                  type="success"
                  :icon="Check"
                  :loading="processingId === app.id"
                  @click="approve(app)"
                >
                  通过
                </el-button>
                <el-button
                  type="danger"
                  :icon="Close"
                  :loading="processingId === app.id"
                  @click="reject(app)"
                >
                  拒绝
                </el-button>
              </div>
            </div>
          </div>
        </el-card>

        <el-card v-if="team.isCreator && processedApplications.length > 0" class="history-card" shadow="never">
          <div class="section-header">
            <h2 class="section-title">历史申请</h2>
          </div>
          <div class="app-list">
            <div v-for="app in processedApplications" :key="app.id" class="app-item history">
              <div class="app-info">
                <div class="app-header">
                  <span class="app-name">{{ app.applicantEmail }}</span>
                  <span class="app-time">{{ formatDate(app.createdAt) }}</span>
                </div>
                <div v-if="app.message" class="app-message">{{ app.message }}</div>
              </div>
              <el-tag :type="getApplicationStatusType(app.status)" effect="light" size="small">
                {{ getApplicationStatusLabel(app.status) }}
              </el-tag>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<style scoped>
.team-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-card,
.apps-card,
.history-card {
  background: var(--input-bg);
  backdrop-filter: blur(12px);
  border: 1px solid var(--border-color);
  border-radius: 16px;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.detail-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
  flex: 1;
  letter-spacing: -0.5px;
}

.detail-meta {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 16px 0;
  border-top: 1px solid var(--border-color);
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 20px;
}

.meta-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.meta-label {
  width: 80px;
  color: var(--text-muted);
  font-size: 13px;
  flex-shrink: 0;
}

.meta-value {
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 600;
  flex: 1;
}

.meta-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.meta-tags .el-tag {
  color: var(--primary-light);
  border-color: rgba(var(--primary-rgb), 0.3);
  background: var(--hover-bg);
}

.meta-desc {
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.6;
  flex: 1;
}

.detail-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.detail-actions .el-button {
  padding: 10px 20px;
  border-radius: 10px;
}

.detail-actions .el-button--primary {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
  border: none;
}

.detail-actions .el-button--warning {
  background: rgba(var(--warning-rgb), 0.15);
  border-color: rgba(var(--warning-rgb), 0.3);
  color: #f59e0b;
}

.detail-actions .el-button--danger {
  background: rgba(var(--error-rgb), 0.15);
  border-color: rgba(var(--error-rgb), 0.3);
  color: var(--error-color);
}

.section-header {
  margin-bottom: 20px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.app-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.app-item {
  background: var(--input-bg);
  border-radius: 12px;
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  border: 1px solid var(--border-color);
}

.app-item.history {
  opacity: 0.6;
}

.app-info {
  flex: 1;
  min-width: 0;
}

.app-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.app-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.app-time {
  font-size: 12px;
  color: var(--text-muted);
  margin-left: auto;
}

.app-message {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 6px;
  line-height: 1.5;
}

.app-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.app-actions .el-button {
  padding: 8px 16px;
  border-radius: 8px;
}

.app-actions .el-button--success {
  background: linear-gradient(135deg, #10b981, #059669);
  border: none;
}

.app-actions .el-button--danger {
  background: rgba(var(--error-rgb), 0.15);
  border-color: rgba(var(--error-rgb), 0.3);
  color: var(--error-color);
}

.empty-state {
  text-align: center;
  padding: 40px 0;
}

.empty-state .el-empty__text {
  color: var(--text-muted);
}

@media (max-width: 768px) {
  .detail-title {
    font-size: 20px;
  }

  .app-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .app-actions {
    width: 100%;
    justify-content: flex-end;
    margin-top: 12px;
  }
}
</style>
