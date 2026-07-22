<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { teamApi } from '@/api/team'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { TravelTeamVO, TeamApplicationVO, CreateTeamRequest } from '@/types/api'
import { Plus, User, Delete, View, Search, Check, Close, Calendar, MapLocation } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const router = useRouter()

const teams = ref<TravelTeamVO[]>([])
const myApplications = ref<TeamApplicationVO[]>([])
const loading = ref(false)
const page = ref(0)
const size = 10
const total = ref(0)
const activeTab = ref<'open' | 'created' | 'joined' | 'applications'>('open')

const showDialog = ref(false)
const submitting = ref(false)
const form = ref<CreateTeamRequest>({
  title: '',
  description: '',
  destination: '',
  startDate: '',
  endDate: '',
  teamType: 'OPEN',
  interests: [],
  maxMembers: 5,
})

const searchDestination = ref('')
const searchStartDate = ref('')
const searchEndDate = ref('')
const matchedTeams = ref<TravelTeamVO[]>([])

onMounted(() => {
  loadTeams()
})

async function loadTeams() {
  loading.value = true
  try {
    if (activeTab.value === 'open') {
      const res = await teamApi.listOpen(page.value, size)
      teams.value = res.data.content
      total.value = res.data.total
    } else if (activeTab.value === 'created') {
      const res = await teamApi.myCreated()
      teams.value = res.data
      total.value = res.data.length
    } else if (activeTab.value === 'joined') {
      const res = await teamApi.myJoined()
      teams.value = res.data
      total.value = res.data.length
    } else if (activeTab.value === 'applications') {
      const res = await teamApi.myApplications()
      myApplications.value = res.data
      total.value = res.data.length
    }
  } finally {
    loading.value = false
  }
}

async function findMatches() {
  if (!searchDestination.value.trim() || !searchStartDate.value || !searchEndDate.value) {
    ElMessage.warning('请输入目的地和日期')
    return
  }
  loading.value = true
  try {
    const res = await teamApi.findMatches(
      searchDestination.value.trim(),
      searchStartDate.value,
      searchEndDate.value,
    )
    matchedTeams.value = res.data
    if (res.data.length === 0) {
      ElMessage.info('未找到匹配的队伍')
    }
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.value = {
    title: '',
    description: '',
    destination: '',
    startDate: '',
    endDate: '',
    teamType: 'OPEN',
    interests: [],
    maxMembers: 5,
  }
  showDialog.value = true
}

async function submit() {
  if (!form.value.title?.trim()) {
    ElMessage.warning('请输入队伍标题')
    return
  }
  if (!form.value.destination?.trim()) {
    ElMessage.warning('请输入目的地')
    return
  }
  if (!form.value.startDate || !form.value.endDate) {
    ElMessage.warning('请选择日期')
    return
  }
  submitting.value = true
  try {
    const res = await teamApi.create({
      ...form.value,
      title: form.value.title.trim(),
      destination: form.value.destination.trim(),
      description: form.value.description?.trim() || undefined,
    })
    ElMessage.success('队伍创建成功')
    showDialog.value = false
    activeTab.value = 'created'
    await loadTeams()
    router.push(`/teams/${res.data.id}`)
  } catch {
  } finally {
    submitting.value = false
  }
}

async function applyTeam(team: TravelTeamVO) {
  try {
    const { value: message } = await ElMessageBox.prompt(
      '请输入申请理由（可选）',
      `申请加入「${team.title}」`,
      {
        confirmButtonText: '提交申请',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: '简单介绍一下你自己...',
      },
    )
    await teamApi.apply(team.id, message ? { message } : {})
    ElMessage.success('申请已提交')
    await loadTeams()
  } catch {
  }
}

async function cancelApplication(application: TeamApplicationVO) {
  try {
    await ElMessageBox.confirm('确定取消该申请吗？', '确认', { type: 'warning' })
    await teamApi.cancel(application.id)
    ElMessage.success('已取消申请')
    await loadTeams()
  } catch {
  }
}

async function handleClose(team: TravelTeamVO) {
  try {
    await ElMessageBox.confirm(
      `确定关闭队伍「${team.title}」吗？关闭后将不再接受申请。`,
      '确认关闭',
      { type: 'warning' },
    )
    await teamApi.close(team.id)
    ElMessage.success('已关闭')
    await loadTeams()
  } catch {
  }
}

async function handleDelete(team: TravelTeamVO) {
  try {
    await ElMessageBox.confirm(
      `确定删除队伍「${team.title}」吗？此操作不可恢复。`,
      '确认删除',
      { type: 'warning' },
    )
    await teamApi.delete(team.id)
    ElMessage.success('已删除')
    await loadTeams()
  } catch {
  }
}

async function handleLeave(team: TravelTeamVO) {
  try {
    await ElMessageBox.confirm(`确定退出队伍「${team.title}」吗？`, '确认退出', { type: 'warning' })
    await teamApi.leave(team.id)
    ElMessage.success('已退出')
    await loadTeams()
  } catch {
  }
}

function viewTeam(team: TravelTeamVO) {
  router.push(`/teams/${team.id}`)
}

const tabs = [
  { value: 'open', label: '公开队伍' },
  { value: 'created', label: '我创建的' },
  { value: 'joined', label: '我加入的' },
  { value: 'applications', label: '我的申请' },
] as const

function formatDate(date: string) {
  return dayjs(date).format('YYYY-MM-DD')
}

function getTeamTypeLabel(type: string) {
  const map: Record<string, string> = {
    OPEN: '公开',
    PRIVATE: '私密',
    FRIENDS: '仅好友',
  }
  return map[type] || type
}

function getTeamTypeTag(type: string) {
  const map: Record<string, string> = {
    OPEN: '#10b981',
    PRIVATE: '#3b82f6',
    FRIENDS: '#f59e0b',
  }
  return map[type] || 'var(--text-muted)'
}

function getStatusLabel(status: string) {
  const map: Record<string, string> = {
    RECRUITING: '招募中',
    FULL: '已满员',
    CLOSED: '已关闭',
    DISBANDED: '已解散',
  }
  return map[status] || status
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
    PENDING: '#f59e0b',
    APPROVED: '#10b981',
    REJECTED: 'var(--error-color)',
    CANCELLED: 'var(--text-muted)',
  }
  return map[status] || 'var(--text-muted)'
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h1 class="page-title">旅行组队</h1>
        <p class="page-subtitle">找到志同道合的旅伴，一起探索世界</p>
      </div>
      <el-button type="primary" :icon="Plus" class="create-btn" @click="openCreate">
        创建队伍
      </el-button>
    </div>

    <div class="match-card">
      <div class="match-header">
        <el-icon :size="18" color="var(--primary-color)"><Search /></el-icon>
        <span>找队伍</span>
      </div>
      <div class="match-form">
        <el-input v-model="searchDestination" placeholder="目的地" style="flex: 1;" class="match-input" />
        <el-date-picker v-model="searchStartDate" type="date" placeholder="开始日期" style="width: 160px;" class="match-input" />
        <el-date-picker v-model="searchEndDate" type="date" placeholder="结束日期" style="width: 160px;" class="match-input" />
        <el-button type="primary" :icon="Search" class="match-btn" @click="findMatches" :loading="loading">
          匹配
        </el-button>
      </div>
      <div v-if="matchedTeams.length > 0" class="match-results">
        <div class="match-title">找到 {{ matchedTeams.length }} 支匹配队伍：</div>
        <div v-for="team in matchedTeams" :key="team.id" class="team-mini" @click="viewTeam(team)">
          <span class="mini-title">{{ team.title }}</span>
          <span class="mini-dest">{{ team.destination }}</span>
          <span class="mini-date">{{ formatDate(team.startDate) }} - {{ formatDate(team.endDate) }}</span>
        </div>
      </div>
    </div>

    <div class="toolbar">
      <div class="tabs">
        <div
          v-for="tab in tabs"
          :key="tab.value"
          class="tab-item"
          :class="{ active: activeTab === tab.value }"
          @click="activeTab = tab.value; page = 0; loadTeams()"
        >
          {{ tab.label }}
        </div>
      </div>
    </div>

    <div v-loading="loading">
      <div v-if="activeTab === 'applications'">
        <div v-if="myApplications.length === 0" class="empty-state">
          <div class="empty-icon-wrapper">
            <Check :size="48" color="var(--text-muted)" />
          </div>
          <div class="empty-title">还没有申请记录</div>
        </div>
        <div v-else class="app-list">
          <div v-for="app in myApplications" :key="app.id" class="app-card">
            <div class="app-info">
              <div class="app-team">{{ app.teamId }}</div>
              <div class="app-message" v-if="app.message">申请理由：{{ app.message }}</div>
              <div class="app-time">提交于 {{ formatDate(app.createdAt) }}</div>
            </div>
            <div class="app-status">
              <el-tag size="small" class="status-tag" :style="{ background: getApplicationStatusType(app.status) + '20', color: getApplicationStatusType(app.status) }">
                {{ getApplicationStatusLabel(app.status) }}
              </el-tag>
              <el-button
                v-if="app.status === 'PENDING'"
                type="danger"
                size="small"
                link
                @click="cancelApplication(app)"
              >
                取消
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <div v-else>
        <div v-if="teams.length === 0" class="empty-state">
          <div class="empty-icon-wrapper">
            <User :size="48" color="var(--text-muted)" />
          </div>
          <div class="empty-title">还没有队伍</div>
          <div class="empty-desc">创建一支队伍，寻找志同道合的旅伴</div>
          <el-button type="primary" :icon="Plus" class="empty-btn" @click="openCreate">创建第一支队伍</el-button>
        </div>

        <div v-else class="team-grid">
          <div v-for="team in teams" :key="team.id" class="team-card">
            <div class="card-header">
              <div class="card-title-area">
                <el-icon :size="18" color="var(--primary-color)"><User /></el-icon>
                <span class="card-title">{{ team.title }}</span>
              </div>
              <el-tag size="small" class="type-tag" :style="{ background: getTeamTypeTag(team.teamType) + '20', color: getTeamTypeTag(team.teamType) }">
                {{ getTeamTypeLabel(team.teamType) }}
              </el-tag>
            </div>

            <div class="card-info">
              <div class="info-row">
                <el-icon :size="14" color="var(--text-muted)"><MapLocation /></el-icon>
                <span class="info-value">{{ team.destination }}</span>
              </div>
              <div class="info-row">
                <el-icon :size="14" color="var(--text-muted)"><Calendar /></el-icon>
                <span class="info-value">{{ formatDate(team.startDate) }} - {{ formatDate(team.endDate) }}</span>
              </div>
              <div class="info-row">
                <el-icon :size="14" color="var(--text-muted)"><User /></el-icon>
                <span class="info-value">{{ team.currentMembers }} / {{ team.maxMembers }}</span>
              </div>
              <div v-if="team.interests && team.interests.length > 0" class="info-row">
                <span class="info-label">兴趣：</span>
                <div class="tag-list">
                  <el-tag v-for="interest in team.interests" :key="interest" size="small" class="interest-tag">
                    {{ interest }}
                  </el-tag>
                </div>
              </div>
              <div v-if="team.description" class="info-row">
                <span class="info-label">简介：</span>
                <span class="info-desc">{{ team.description }}</span>
              </div>
            </div>

            <div class="card-footer">
              <span class="creator">创建者：{{ team.creatorEmail }}</span>
              <div class="actions">
                <el-button :icon="View" link size="small" class="action-btn" @click="viewTeam(team)">查看</el-button>
                <el-button
                  v-if="!team.isCreator && !team.isMember && !team.hasApplied && team.status === 'RECRUITING'"
                  type="primary"
                  size="small"
                  class="apply-btn"
                  @click="applyTeam(team)"
                >
                  申请加入
                </el-button>
                <el-tag v-if="team.hasApplied && !team.isMember" size="small" class="applied-tag">已申请</el-tag>
                <el-tag v-if="team.isMember && !team.isCreator" size="small" class="joined-tag">已加入</el-tag>
                <el-button
                  v-if="team.isMember && !team.isCreator"
                  type="danger"
                  link
                  size="small"
                  class="action-btn"
                  @click="handleLeave(team)"
                >
                  退出
                </el-button>
                <el-button
                  v-if="team.isCreator && team.status === 'RECRUITING'"
                  type="warning"
                  link
                  size="small"
                  class="action-btn"
                  @click="handleClose(team)"
                >
                  关闭
                </el-button>
                <el-button
                  v-if="team.isCreator"
                  type="danger"
                  link
                  size="small"
                  class="action-btn"
                  @click="handleDelete(team)"
                >
                  删除
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="activeTab === 'open' && total > size" class="pagination">
        <el-pagination
          :current-page="page + 1"
          :page-size="size"
          :total="total"
          layout="prev, pager, next"
          class="el-pagination"
          @current-change="(p: number) => { page = p - 1; loadTeams() }"
        />
      </div>
    </div>

    <el-dialog v-model="showDialog" title="创建队伍" width="600px" class="dark-dialog">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="例如：东京樱花季约伴" maxlength="100" show-word-limit class="dialog-input" />
        </el-form-item>
        <el-form-item label="目的地" required>
          <el-input v-model="form.destination" placeholder="例如：东京" maxlength="50" class="dialog-input" />
        </el-form-item>
        <el-form-item label="开始日期" required>
          <el-date-picker v-model="form.startDate" type="date" style="width: 100%;" class="dialog-input" />
        </el-form-item>
        <el-form-item label="结束日期" required>
          <el-date-picker v-model="form.endDate" type="date" style="width: 100%;" class="dialog-input" />
        </el-form-item>
        <el-form-item label="队伍类型">
          <el-select v-model="form.teamType" style="width: 100%;" class="dialog-select">
            <el-option label="公开（任何人可申请）" value="OPEN" />
            <el-option label="私密（需审核）" value="PRIVATE" />
            <el-option label="仅好友" value="FRIENDS" />
          </el-select>
        </el-form-item>
        <el-form-item label="最大人数">
          <el-input-number v-model="form.maxMembers" :min="2" :max="20" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="兴趣标签">
          <el-select
            v-model="form.interests"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="选择或输入兴趣标签"
            style="width: 100%;"
            class="dialog-select"
          >
            <el-option label="摄影" value="摄影" />
            <el-option label="美食" value="美食" />
            <el-option label="徒步" value="徒步" />
            <el-option label="购物" value="购物" />
            <el-option label="文化" value="文化" />
            <el-option label="夜生活" value="夜生活" />
          </el-select>
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit class="dialog-input" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">创建</el-button>
      </template>
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

.page-title {
  font-size: 26px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
}

.page-subtitle {
  font-size: 14px;
  color: var(--text-muted);
  margin: 6px 0 0;
}

.create-btn {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
  border: none;
  border-radius: 12px;
  font-weight: 600;
  padding: 10px 20px;
  transition: all 0.3s;
}

.create-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 15px rgba(var(--primary-rgb), 0.3);
}

.match-card {
  background: var(--input-bg);
  border-radius: 16px;
  padding: 20px;
  border: 1px solid var(--hover-bg);
  margin-bottom: 20px;
}

.match-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 16px;
}

.match-form {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

:deep(.match-input .el-input__inner) {
  background: var(--overlay-bg-dark);
  border: 1px solid var(--border-color-light);
  border-radius: 10px;
  color: var(--text-primary);
}

:deep(.match-input .el-input__inner::placeholder) {
  color: var(--text-secondary);
}

:deep(.match-input .el-date-editor) {
  background: var(--overlay-bg-dark);
  border: 1px solid var(--border-color-light);
  border-radius: 10px;
}

:deep(.match-input .el-date-editor .el-input__inner) {
  background: transparent;
  border: none;
  color: var(--text-primary);
}

.match-btn {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
  border: none;
  border-radius: 10px;
  font-weight: 600;
}

.match-results {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed var(--border-color-light);
}

.match-title {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 10px;
}

.team-mini {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--input-bg);
  border-radius: 10px;
  margin-bottom: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.team-mini:hover {
  background: rgba(var(--primary-rgb), 0.15);
}

.mini-title {
  font-weight: 600;
  color: var(--text-primary);
  flex: 1;
}

.mini-dest {
  color: var(--primary-color);
}

.mini-date {
  color: var(--text-muted);
  font-size: 13px;
}

.toolbar {
  margin-bottom: 20px;
}

.tabs {
  display: inline-flex;
  gap: 4px;
  background: var(--input-bg);
  padding: 4px;
  border-radius: 12px;
  border: 1px solid var(--hover-bg);
}

.tab-item {
  padding: 8px 20px;
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
  color: var(--text-secondary);
  transition: all 0.2s;
}

.tab-item.active {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
  color: #fff;
  font-weight: 600;
}

.team-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 16px;
}

.team-card {
  background: var(--input-bg);
  border-radius: 16px;
  padding: 20px;
  border: 1px solid var(--hover-bg);
  display: flex;
  flex-direction: column;
  gap: 14px;
  transition: all 0.3s;
}

.team-card:hover {
  transform: translateY(-2px);
  border-color: rgba(var(--primary-rgb), 0.3);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.card-title-area {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  flex: 1;
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
  border: none;
}

.card-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-row {
  display: flex;
  font-size: 14px;
  align-items: flex-start;
  gap: 6px;
}

.info-label {
  color: var(--text-muted);
  flex-shrink: 0;
}

.info-value {
  color: var(--text-primary);
  flex: 1;
}

.info-desc {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  flex: 1;
}

.interest-tag {
  background: rgba(var(--primary-rgb), 0.15);
  border-color: rgba(var(--primary-rgb), 0.3);
  color: var(--primary-light);
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid var(--hover-bg);
}

.creator {
  font-size: 12px;
  color: var(--text-muted);
}

.actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.action-btn {
  color: var(--text-muted);
  font-size: 13px;
}

.action-btn:hover {
  color: var(--text-secondary);
}

.apply-btn {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
  border: none;
  border-radius: 8px;
}

.applied-tag {
  background: rgba(var(--warning-rgb), 0.15);
  color: var(--warning-color);
  border: none;
}

.joined-tag {
  background: rgba(var(--success-rgb), 0.15);
  color: var(--success-color);
  border: none;
}

.app-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.app-card {
  background: var(--input-bg);
  border-radius: 16px;
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 1px solid var(--hover-bg);
}

.app-info {
  flex: 1;
}

.app-team {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 6px;
}

.app-message {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.app-time {
  font-size: 12px;
  color: var(--text-muted);
}

.app-status {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.status-tag {
  border: none;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.empty-icon-wrapper {
  width: 80px;
  height: 80px;
  margin: 0 auto 16px;
  background: var(--border-color);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-title {
  font-size: 18px;
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
}

.pagination {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

:deep(.el-pagination .btn-prev),
:deep(.el-pagination .btn-next),
:deep(.el-pagination .el-pager li) {
  background: var(--input-bg);
  border-color: var(--border-color-light);
  color: var(--text-secondary);
}

:deep(.el-pagination .el-pager li.is-active) {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
  border-color: transparent;
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

:deep(.dark-dialog .el-form-item__label) {
  color: var(--text-secondary);
}

:deep(.dialog-input .el-input__inner),
:deep(.dialog-input .el-textarea__inner) {
  background: var(--overlay-bg-dark);
  border: 1px solid var(--border-color-light);
  color: var(--text-primary);
}

:deep(.dialog-input .el-input__inner::placeholder),
:deep(.dialog-input .el-textarea__inner::placeholder) {
  color: var(--text-secondary);
}

:deep(.dialog-select .el-input__inner) {
  background: var(--overlay-bg-dark);
  border: 1px solid var(--border-color-light);
  color: var(--text-primary);
}

@media (max-width: 768px) {
  .team-grid {
    grid-template-columns: 1fr;
  }

  .match-form {
    flex-wrap: wrap;
  }
}
</style>