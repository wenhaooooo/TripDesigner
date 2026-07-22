<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { aiApi } from '@/api/ai'
import { tripApi } from '@/api/trip'
import { conversationApi } from '@/api/conversation'
import { xiaohongshuApi } from '@/api/xiaohongshu'
import type { XiaohongshuNote } from '@/api/xiaohongshu'
import { ElMessage, ElDialog } from 'element-plus'
import type { TripVO, TripDayVO, TripDetailVO, TripGenerationTask } from '@/types/api'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { MagicStick, Check, Loading, Close, Minus, View, ArrowDown, ArrowUp } from '@element-plus/icons-vue'

const router = useRouter()
const prompt = ref('')
const loading = ref(false)
const resultTrip = ref<TripVO | null>(null)
const resultDays = ref<TripDayVO[]>([])
const generatedConversationId = ref<number | null>(null)
const workflowSessionId = ref<number | null>(null)
const workflowStatus = ref('')
const workflowSteps = ref<any[]>([])

const aiResponse = ref('')

const agentOutputs = ref<Record<string, string>>({})
const currentAgent = ref<string | null>(null)
const agentStatuses = ref<Record<string, 'pending' | 'running' | 'completed' | 'failed' | 'skipped'>>({})

const xhsNotes = ref<XiaohongshuNote[]>([])
const xhsLoading = ref(false)

const workflowStartTime = ref<number | null>(null)
const agentErrorMessages = ref<Record<string, string>>({})

const generationTask = ref<TripGenerationTask | null>(null)

const showAgentDialog = ref(false)
const selectedAgentKey = ref<string>('')
const expandedAgents = ref<Set<string>>(new Set())

const totalAgents = computed(() => Object.keys(agentNames).length)
const completedAgents = computed(() =>
  Object.values(agentStatuses.value).filter(s => s === 'completed' || s === 'skipped').length
)
const progressPercent = computed(() => {
  if (generationTask.value) {
    return generationTask.value.progress
  }
  if (!loading.value) return 0
  return Math.round((completedAgents.value / totalAgents.value) * 100)
})
const estimatedRemaining = computed(() => {
  if (!workflowStartTime.value || completedAgents.value === 0) return null
  const elapsed = (Date.now() - workflowStartTime.value) / 1000
  const avgPerAgent = elapsed / completedAgents.value
  const remaining = (totalAgents.value - completedAgents.value) * avgPerAgent
  if (remaining < 60) return `${Math.ceil(remaining)}秒`
  return `${Math.ceil(remaining / 60)}分钟`
})

const agentNames: Record<string, string> = {
  planner: '规划师',
  transport: '交通专家',
  dining: '美食专家',
  sightseeing: '景点专家',
  accommodation: '住宿专家',
  budget: '预算专家',
  activity: '活动专家',
  reflection: '复盘专家'
}

let eventSource: EventSource | null = null
let pollInterval: ReturnType<typeof setInterval> | null = null

async function handleGenerate() {
  if (!prompt.value.trim()) {
    ElMessage.warning('请输入旅行需求')
    return
  }

  loading.value = true
  aiResponse.value = ''
  resultTrip.value = null
  resultDays.value = []
  generatedConversationId.value = null
  workflowSessionId.value = null
  workflowStatus.value = ''
  workflowSteps.value = []
  agentOutputs.value = {}
  currentAgent.value = null
  agentStatuses.value = {}
  agentErrorMessages.value = {}
  workflowStartTime.value = Date.now()
  generationTask.value = null

  ;['planner', 'transport', 'dining', 'sightseeing', 'accommodation', 'budget', 'activity', 'reflection'].forEach(name => {
    agentOutputs.value[name] = ''
    agentStatuses.value[name] = 'pending'
  })
  expandedAgents.value = new Set(['planner', 'transport', 'dining', 'sightseeing', 'accommodation', 'budget', 'activity', 'reflection'])

  xhsNotes.value = []
  xhsLoading.value = true

  try {
    const convRes = await conversationApi.create({ title: prompt.value.slice(0, 80) })
    generatedConversationId.value = convRes.data.id

    await conversationApi.addMessage(generatedConversationId.value, 'USER', prompt.value)

    searchXiaohongshu(prompt.value)

    startSSEStream(prompt.value, generatedConversationId.value)
  } catch {
    ElMessage.error('生成失败，请重试')
    loading.value = false
  }
}

async function searchXiaohongshu(keyword: string) {
  const mockNotes: XiaohongshuNote[] = [
    {
      id: 'mock-1',
      title: `${keyword} 必去景点攻略 | 三天两夜完美行程`,
      content: `刚从${keyword}回来，整理了这份超详细攻略！包含景点、美食、住宿全攻略，第一次去的一定要收藏！`,
      coverImage: 'https://picsum.photos/seed/xhs1/400/300',
      images: [],
      authorName: '旅行达人小李',
      authorAvatar: '',
      likes: 1234,
      comments: 89,
      shares: 56,
      noteUrl: 'https://www.xiaohongshu.com',
      tags: `${keyword},旅行攻略,景点推荐`
    },
    {
      id: 'mock-2',
      title: `${keyword} 美食地图 | 本地人推荐的10家老店`,
      content: `作为一个在${keyword}生活了5年的吃货，今天来给大家盘点一下真正值得排队的老字号店铺...`,
      coverImage: 'https://picsum.photos/seed/xhs2/400/300',
      images: [],
      authorName: '吃货日记',
      authorAvatar: '',
      likes: 2567,
      comments: 234,
      shares: 189,
      noteUrl: 'https://www.xiaohongshu.com',
      tags: `${keyword}美食,探店,老字号`
    },
    {
      id: 'mock-3',
      title: `${keyword} 民宿推荐 | 这几家美哭了！`,
      content: `这次去${keyword}住了4家民宿，每一家都很有特色，特别是第二家，清晨推开窗就是山景...`,
      coverImage: 'https://picsum.photos/seed/xhs3/400/300',
      images: [],
      authorName: '民宿种草机',
      authorAvatar: '',
      likes: 3421,
      comments: 167,
      shares: 423,
      noteUrl: 'https://www.xiaohongshu.com',
      tags: `${keyword}民宿,住宿推荐,设计感`
    },
    {
      id: 'mock-4',
      title: `${keyword} 小众玩法 | 避开人潮的宝藏路线`,
      content: `不想去景点人挤人？这条小众路线带你发现不一样的${keyword}，随手一拍都是大片！`,
      coverImage: 'https://picsum.photos/seed/xhs4/400/300',
      images: [],
      authorName: '小众旅行家',
      authorAvatar: '',
      likes: 1890,
      comments: 145,
      shares: 267,
      noteUrl: 'https://www.xiaohongshu.com',
      tags: `${keyword},小众景点,拍照打卡`
    },
    {
      id: 'mock-5',
      title: `带爸妈去${keyword} | 慢节奏舒适游攻略`,
      content: '带长辈出行最重要的是轻松舒适，这篇攻略包含交通、住宿、餐饮全方面考虑，适合带父母出游参考。',
      coverImage: 'https://picsum.photos/seed/xhs5/400/300',
      images: [],
      authorName: '亲子旅行家',
      authorAvatar: '',
      likes: 876,
      comments: 67,
      shares: 134,
      noteUrl: 'https://www.xiaohongshu.com',
      tags: `${keyword},亲子游,慢旅行`
    }
  ]

  await new Promise(resolve => setTimeout(resolve, 800))

  xhsNotes.value = mockNotes
  xhsLoading.value = false
}

function startSSEStream(userPrompt: string, conversationId?: number) {
  const token = localStorage.getItem('trip_designer_token')
  const url = '/api/ai/workflow/generate/stream'

  fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'text/event-stream',
      ...(token ? { 'Authorization': `Bearer ${token}` } : {})
    },
    body: JSON.stringify({ prompt: userPrompt, conversationId })
  })
  .then(response => {
    if (!response.ok) {
      throw new Error('HTTP error ' + response.status)
    }
    const reader = response.body?.getReader()
    if (!reader) {
      throw new Error('No response body')
    }
    const decoder = new TextDecoder()
    let buffer = ''

    const readChunk = (): Promise<void> => {
      return reader.read().then(({ done, value }) => {
        if (done) {
          if (buffer) {
            processSSEBuffer(buffer)
          }
          return
        }

        buffer += decoder.decode(value, { stream: true })
        buffer = processSSEBuffer(buffer)
        return readChunk()
      })
    }

    return readChunk()
  })
  .catch(error => {
    try {
      const errorText = error.message
      if (errorText) {
        const errorMatch = errorText.match(/"error":"([^"]+)"/)
        if (errorMatch) {
          ElMessage.error('生成失败：' + errorMatch[1])
        } else {
          ElMessage.error('生成失败，请重试')
        }
      }
    } catch {
      ElMessage.error('生成失败，请重试')
    }
    loading.value = false
  })
}

async function startAsyncGeneration(userPrompt: string) {
  try {
    const res = await aiApi.generateTripAsync(userPrompt)
    generationTask.value = res.data
    workflowStatus.value = 'RUNNING'

    pollInterval = setInterval(async () => {
      if (!generationTask.value) return
      try {
        const statusRes = await aiApi.getTaskStatus(generationTask.value.id)
        generationTask.value = statusRes.data

        if (generationTask.value.status === 'COMPLETED') {
          if (pollInterval) clearInterval(pollInterval)
          loading.value = false
          workflowStatus.value = 'COMPLETED'
          fetchGeneratedTrip()
        } else if (generationTask.value.status === 'FAILED') {
          if (pollInterval) clearInterval(pollInterval)
          loading.value = false
          ElMessage.error('生成失败：' + (generationTask.value.errorMessage || '未知错误'))
        }
      } catch {
      }
    }, 2000)
  } catch {
    ElMessage.error('启动生成失败，请重试')
    loading.value = false
  }
}

function processSSEBuffer(buffer: string) {
  const lines = buffer.split('\n')
  let remaining = ''

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]
    if (line.startsWith('event:')) {
      continue
    } else if (line.startsWith('data:')) {
      const jsonStr = line.substring(5).trim()
      if (jsonStr) {
        try {
          const data = JSON.parse(jsonStr)
          handleSseMessage(data)
        } catch {
          remaining += line + '\n'
        }
      }
    } else if (line.trim() === '') {
      if (remaining) {
        try {
          const data = JSON.parse(remaining)
          handleSseMessage(data)
        } catch {
        }
        remaining = ''
      }
    } else {
      remaining += line + '\n'
    }
  }

  return remaining
}

function handleSseMessage(data: any) {
  const { type, sessionId, agentName, content, error } = data

  if (type === 'start') {
    workflowSessionId.value = sessionId
    workflowStatus.value = 'RUNNING'
  } else if (type === 'agent_start') {
    currentAgent.value = agentName
    agentStatuses.value[agentName] = 'running'
  } else if (type === 'agent_content') {
    if (!agentOutputs.value[agentName]) {
      agentOutputs.value[agentName] = ''
    }
    agentOutputs.value[agentName] += content
  } else if (type === 'agent_end') {
    agentStatuses.value[agentName] = 'completed'
    currentAgent.value = null
  } else if (type === 'agent_failed') {
    agentStatuses.value[agentName] = 'failed'
    agentErrorMessages.value[agentName] = error || '执行失败，将使用通用推荐'
    currentAgent.value = null
  } else if (type === 'agent_skipped') {
    agentStatuses.value[agentName] = 'skipped'
    agentErrorMessages.value[agentName] = error || '已跳过，不影响整体行程'
    currentAgent.value = null
  } else if (type === 'summary') {
    aiResponse.value = content
  } else if (type === 'complete') {
    loading.value = false
    workflowStatus.value = 'COMPLETED'
    fetchGeneratedTrip()
  } else if (type === 'error') {
    loading.value = false
    ElMessage.error('生成失败：' + error)
  }
}

function stopSSE() {
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
  if (pollInterval) {
    clearInterval(pollInterval)
  }
}

async function fetchGeneratedTrip() {
  try {
    const tripsRes = await tripApi.list()
    const recentTrips = tripsRes.data?.sort((a: any, b: any) => 
      new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    )
    
    if (recentTrips && recentTrips.length > 0) {
      const detailRes = await tripApi.detail(recentTrips[0].id)
      const detail = detailRes.data as TripDetailVO
      if (detail) {
        resultTrip.value = detail
        resultDays.value = detail.days || []
      }
    }
    
    ElMessage.success('行程生成完成！')
  } catch {
    ElMessage.error('获取行程详情失败')
  }
}

function buildSummary(trip: TripVO, days: TripDayVO[]) {
  let lines = [`## 已为你生成行程：${trip.title}`, ``]

  if (trip.destinationName) {
    lines.push(`**目的地**：${trip.destinationName}`)
    lines.push(`**日期**：${trip.startDate} ~ ${trip.endDate}`)
    if (trip.budget) lines.push(`**预算**：¥${trip.budget.toLocaleString()}`)
    lines.push('')
  }

  for (const day of days) {
    lines.push(`### Day ${day.dayNumber} - ${day.title || day.date}`)
    if (day.activities && day.activities.length > 0) {
      for (const a of day.activities) {
        const time = `${a.startTime || '--:--'} - ${a.endTime || '--:--'}`
        lines.push(`- ${time} | ${a.name}${a.place ? `（${a.place}）` : ''}`)
      }
    }
    lines.push('')
  }

  return lines.join('\n')
}

function goToChat() {
  if (generatedConversationId.value) {
    router.push(`/chat/${generatedConversationId.value}`)
  }
}

function goToTrip() {
  if (resultTrip.value) {
    router.push(`/trips/${resultTrip.value.id}`)
  }
}

function handleCancel() {
  if (workflowSessionId.value) {
    aiApi.cancelWorkflow(workflowSessionId.value)
    stopSSE()
    loading.value = false
    ElMessage.info('已发送取消请求')
  }
}

onUnmounted(() => {
  stopSSE()
  if (pollInterval) {
    clearInterval(pollInterval)
  }
})

function renderMarkdown(text: string): string {
  if (!text) return ''
  const raw = marked.parse(text, { async: false }) as string
  return DOMPurify.sanitize(raw)
}

function toggleAgentExpand(key: string) {
  const newSet = new Set(expandedAgents.value)
  if (newSet.has(key)) {
    newSet.delete(key)
  } else {
    newSet.add(key)
  }
  expandedAgents.value = newSet
}

function openAgentDetail(key: string) {
  selectedAgentKey.value = key
  showAgentDialog.value = true
}

function closeAgentDialog() {
  showAgentDialog.value = false
  selectedAgentKey.value = ''
}

const samplePrompts = [
  '帮我规划一个东京5日游，喜欢动漫和美食，预算15000元',
  '巴黎3日游，情侣出行，想去看艺术展和浪漫晚餐，预算10000元',
  '日本京都+大阪6日游，带老人，节奏慢一点，住舒适型酒店',
  '泰国清迈7日游，自由行，喜欢户外活动和当地美食，预算8000元',
]

function openNoteUrl(url: string) {
  if (url) window.open(url, '_blank')
}
</script>

<template>
  <div class="page-container">
    <div class="main-card">
      <div class="card-header">
        <div class="header-icon-wrapper">
          <el-icon :size="28" color="var(--primary-color)"><MagicStick /></el-icon>
          <div class="icon-glow"></div>
        </div>
        <div class="header-text">
          <h1 class="page-title">AI 生成行程</h1>
          <p class="page-subtitle">描述你的旅行需求，AI 将为你设计完美行程</p>
        </div>
      </div>

      <div class="input-section">
        <div class="section-title">
          <span>✈️</span>
          <span>旅行需求</span>
        </div>
        <div class="prompt-wrapper">
          <el-input
            v-model="prompt"
            type="textarea"
            :rows="4"
            placeholder="例如：帮我规划一个东京5日游，喜欢动漫和美食，预算15000元..."
            class="prompt-input"
          />
          <div class="prompt-border-glow"></div>
        </div>

        <div class="sample-prompts">
          <div class="sample-label">试试这些示例：</div>
          <div class="sample-chips">
            <div
              v-for="(s, i) in samplePrompts"
              :key="i"
              class="sample-chip"
              @click="prompt = s"
            >
              {{ s }}
            </div>
          </div>
        </div>

        <div class="action-row">
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="generate-btn"
            @click="handleGenerate"
          >
            {{ loading ? 'AI 正在规划中...' : '开始生成行程' }}
          </el-button>
          <el-button
            size="large"
            :disabled="!loading || !workflowSessionId"
            class="cancel-btn"
            @click="handleCancel"
          >
            取消
          </el-button>
        </div>
      </div>

      <div v-if="(xhsLoading || xhsNotes.length > 0)" class="xhs-section">
        <div class="section-title">
          <span class="xhs-icon">📕</span>
          <span>小红书旅行推荐</span>
          <span v-if="xhsLoading" class="xhs-loading">搜索中...</span>
        </div>
        <div class="xhs-cards">
          <div v-if="xhsLoading" class="xhs-skeleton">
            <el-skeleton :rows="1" animated style="width: 80%" />
            <el-skeleton :rows="2" animated style="width: 60%" />
          </div>
          <div
            v-for="note in xhsNotes"
            :key="note.id"
            class="xhs-card"
            @click="openNoteUrl(note.noteUrl)"
          >
            <div class="xhs-card-image">
              <img :src="note.coverImage" :alt="note.title" />
              <div class="xhs-image-overlay"></div>
            </div>
            <div class="xhs-card-content">
              <div class="xhs-card-title">{{ note.title }}</div>
              <div class="xhs-card-desc">{{ note.content }}</div>
              <div class="xhs-card-meta">
                <div class="xhs-author">
                  <div class="xhs-avatar">👤</div>
                  <span>{{ note.authorName }}</span>
                </div>
                <div class="xhs-stats">
                  <span class="xhs-stat">👍 {{ note.likes }}</span>
                  <span class="xhs-stat">💬 {{ note.comments }}</span>
                  <span class="xhs-stat">🔗 {{ note.shares }}</span>
                </div>
              </div>
              <div v-if="note.tags" class="xhs-card-tags">
                <span class="xhs-tag">#{{ note.tags.split(',')[0] }}</span>
                <span v-if="note.tags.split(',').length > 1" class="xhs-tag">#{{ note.tags.split(',')[1] }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="(loading && workflowStatus === 'RUNNING') || (workflowStatus === 'COMPLETED' && Object.keys(agentOutputs).some(k => agentOutputs[k]))" class="agents-section">
        <div class="progress-bar-wrapper" v-if="loading && workflowStatus === 'RUNNING'">
          <div class="progress-info">
            <span class="progress-text">{{ completedAgents }}/{{ totalAgents }} Agent 执行中</span>
            <span v-if="estimatedRemaining" class="progress-eta">预计剩余 {{ estimatedRemaining }}</span>
          </div>
          <div class="progress-bar-track">
            <div class="progress-bar-fill" :style="{ width: progressPercent + '%' }"></div>
            <div class="progress-bar-glow" :style="{ width: progressPercent + '%' }"></div>
          </div>
        </div>
        <div class="agents-container">
          <div
            v-for="(name, key) in agentNames"
            :key="key"
            class="agent-card"
            :class="{ 'agent-active': currentAgent === key, 'agent-completed': agentStatuses[key] === 'completed' }"
          >
            <div class="agent-header" @click="toggleAgentExpand(key)">
              <div class="agent-status">
                <el-icon v-if="agentStatuses[key] === 'completed'" color="#10b981"><Check /></el-icon>
                <el-icon v-else-if="agentStatuses[key] === 'running'" class="step-running" color="var(--primary-color)"><Loading /></el-icon>
                <el-icon v-else-if="agentStatuses[key] === 'failed'" color="var(--error-color)"><Close /></el-icon>
                <el-icon v-else-if="agentStatuses[key] === 'skipped'" color="#f59e0b"><Minus /></el-icon>
                <span v-else class="step-pending">{{ Object.keys(agentNames).indexOf(key) + 1 }}</span>
              </div>
              <div class="agent-name">{{ name }}</div>
              <div class="agent-badge" :class="agentStatuses[key]">
                {{ agentStatuses[key] === 'completed' ? '完成' : agentStatuses[key] === 'running' ? '处理中' : agentStatuses[key] === 'failed' ? '失败' : agentStatuses[key] === 'skipped' ? '跳过' : '等待' }}
              </div>
              <div class="agent-header-actions">
                <el-button 
                  text 
                  size="small" 
                  class="view-detail-btn"
                  @click.stop="openAgentDetail(key)"
                  :disabled="!agentOutputs[key]"
                >
                  <el-icon><View /></el-icon>
                  查看完整内容
                </el-button>
                <el-icon 
                  class="agent-toggle" 
                  color="var(--text-muted)" 
                  :class="{ rotated: expandedAgents.has(key) }"
                >
                  <ArrowDown />
                </el-icon>
              </div>
            </div>
            <div v-if="(expandedAgents.has(key) || currentAgent === key) || (!agentOutputs[key] && agentStatuses[key] !== 'pending')" class="agent-output-wrapper">
              <div v-if="agentStatuses[key] === 'running' && !agentOutputs[key]" class="typing-indicator">
                <span class="typing-dot"></span>
                <span class="typing-dot"></span>
                <span class="typing-dot"></span>
              </div>
              <div v-else-if="agentOutputs[key]" class="agent-output" v-html="renderMarkdown(agentOutputs[key])"></div>
              <div v-if="agentErrorMessages[key]" class="agent-error-msg">
                {{ agentErrorMessages[key] }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="loading && workflowStatus !== 'RUNNING'" class="loading-placeholder">
        <el-skeleton :rows="4" animated />
      </div>

      <div v-if="aiResponse && resultTrip" class="result-section">
        <div class="result-header">
          <div class="result-title">
            <span class="success-icon">✨</span>
            <span>行程已生成</span>
          </div>
          <div class="result-actions">
            <el-button size="small" class="result-btn" @click="goToChat">在对话中继续优化</el-button>
            <el-button type="primary" size="small" class="result-btn primary" @click="goToTrip">查看行程详情</el-button>
          </div>
        </div>

        <div class="result-summary">
          <div class="summary-item">
            <span class="summary-key">目的地</span>
            <span class="summary-val">{{ resultTrip.destinationName }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-key">日期</span>
            <span class="summary-val">{{ resultTrip.startDate }} ~ {{ resultTrip.endDate }}</span>
          </div>
          <div v-if="resultTrip.budget" class="summary-item">
            <span class="summary-key">预算</span>
            <span class="summary-val">¥{{ resultTrip.budget.toLocaleString() }}</span>
          </div>
        </div>

        <div class="result-days">
          <div
            v-for="day in resultDays"
            :key="day.id"
            class="result-day"
          >
            <div class="day-header">
              <span class="day-number">Day {{ day.dayNumber }}</span>
              <span class="day-title">{{ day.title || '每日行程' }}</span>
            </div>
            <div v-if="day.activities && day.activities.length > 0">
              <div
                v-for="a in day.activities"
                :key="a.id"
                class="activity-row"
              >
                <span class="activity-time">
                  {{ a.startTime || '--:--' }} - {{ a.endTime || '--:--' }}
                </span>
                <span class="activity-name">{{ a.name }}</span>
                <span v-if="a.place" class="activity-place">({{ a.place }})</span>
                <el-tag v-if="a.category" size="small" effect="plain" class="activity-tag">
                  {{ a.category }}
                </el-tag>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="!loading && !resultTrip && !aiResponse" class="empty-state">
        <div class="empty-icon">🌍</div>
        <div class="empty-title">开始规划你的旅行</div>
        <div class="empty-desc">在上方描述你的旅行需求，AI 将为你设计完整行程</div>
      </div>
    </div>

    <ElDialog
      v-model="showAgentDialog"
      :title="agentNames[selectedAgentKey] + ' - 完整输出'"
      width="700px"
      class="agent-detail-dialog"
      :close-on-click-modal="true"
    >
      <div class="agent-detail-content" v-html="renderMarkdown(agentOutputs[selectedAgentKey])"></div>
    </ElDialog>
  </div>
</template>

<style scoped>
.page-container {
  min-height: 100vh;
  background: var(--bg-secondary);
  padding: 24px;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

.main-card {
  width: 100%;
  max-width: 1400px;
  background: var(--input-bg);
  backdrop-filter: blur(12px);
  border-radius: 24px;
  border: 1px solid var(--hover-bg);
  padding: 32px;
  position: relative;
  overflow: hidden;
}

.main-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(var(--primary-rgb), 0.5), transparent);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.header-icon-wrapper {
  position: relative;
}

.icon-glow {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 48px;
  height: 48px;
  background: radial-gradient(circle, rgba(var(--primary-rgb), 0.3), transparent);
  border-radius: 50%;
  animation: pulse-glow 2s ease-in-out infinite;
}

@keyframes pulse-glow {
  0%, 100% { transform: translate(-50%, -50%) scale(1); opacity: 0.6; }
  50% { transform: translate(-50%, -50%) scale(1.2); opacity: 0.3; }
}

.header-text {
  display: flex;
  flex-direction: column;
}

.page-title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -1px;
}

.page-subtitle {
  margin: 4px 0 0;
  font-size: 14px;
  color: var(--text-muted);
}

.input-section {
  background: var(--input-bg);
  border-radius: 16px;
  padding: 20px 24px;
  margin-bottom: 24px;
  border: 1px solid var(--hover-bg);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
}

.prompt-wrapper {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
}

.prompt-border-glow {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border: 1px solid rgba(var(--primary-rgb), 0.2);
  border-radius: 12px;
  pointer-events: none;
  transition: all 0.3s;
}

.prompt-wrapper:focus-within .prompt-border-glow {
  border-color: rgba(var(--primary-rgb), 0.5);
  box-shadow: 0 0 20px var(--hover-bg);
}

:deep(.prompt-input .el-input__wrapper) {
  background: var(--overlay-bg-dark);
  border: none;
  box-shadow: none;
  border-radius: 12px;
}

:deep(.prompt-input .el-input__inner) {
  background: transparent;
  color: var(--text-primary);
  font-size: 15px;
  line-height: 1.6;
}

:deep(.prompt-input .el-input__placeholder) {
  color: var(--text-secondary);
}

.sample-prompts {
  margin-top: 12px;
}

.sample-label {
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 8px;
}

.sample-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.sample-chip {
  padding: 8px 14px;
  background: var(--hover-bg);
  border-radius: 20px;
  font-size: 12px;
  color: var(--text-secondary);
  cursor: pointer;
  border: 1px solid rgba(var(--primary-rgb), 0.2);
  transition: all 0.2s;
}

.sample-chip:hover {
  background: rgba(var(--primary-rgb), 0.2);
  color: #a5b4fc;
  border-color: rgba(var(--primary-rgb), 0.4);
  transform: translateY(-1px);
}

.action-row {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

.generate-btn {
  flex: 1;
  height: 48px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
  border: none;
  transition: all 0.3s;
}

.generate-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(var(--primary-rgb), 0.4);
}

.cancel-btn {
  height: 48px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 500;
  background: var(--border-color);
  border: 1px solid var(--border-color-light);
  color: var(--text-secondary);
  transition: all 0.2s;
}

.cancel-btn:hover:not(:disabled) {
  background: var(--border-color-light);
}

.agents-section {
  margin-bottom: 24px;
}

.progress-bar-wrapper {
  margin-bottom: 20px;
  padding: 16px 20px;
  background: rgba(var(--primary-rgb), 0.08);
  border-radius: 14px;
  border: 1px solid rgba(var(--primary-rgb), 0.15);
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.progress-text {
  font-size: 14px;
  font-weight: 600;
  color: var(--primary-light);
}

.progress-eta {
  font-size: 12px;
  color: var(--text-muted);
}

.progress-bar-track {
  height: 10px;
  background: var(--hover-bg);
  border-radius: 5px;
  overflow: hidden;
  position: relative;
}

.progress-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--primary-color), var(--primary-light));
  border-radius: 5px;
  transition: width 0.5s ease;
  position: relative;
  z-index: 1;
}

.progress-bar-glow {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  background: linear-gradient(90deg, rgba(var(--primary-rgb), 0.5), transparent);
  border-radius: 5px;
  transition: width 0.5s ease;
  opacity: 0.5;
}

.agents-container {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.agent-card {
  background: var(--input-bg);
  border-radius: 14px;
  padding: 16px;
  border: 1px solid var(--border-color);
  transition: all 0.2s;
  position: relative;
  overflow: hidden;
}

.agent-card:hover {
  border-color: rgba(var(--primary-rgb), 0.2);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.03);
}

.agent-active {
  border-color: rgba(var(--primary-rgb), 0.5);
  background: rgba(var(--primary-rgb), 0.05);
  box-shadow: 0 0 15px rgba(var(--primary-rgb), 0.08);
}

.agent-completed {
  border-color: rgba(var(--success-rgb), 0.3);
}

.agent-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  cursor: pointer;
}

.agent-status {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.step-pending {
  background: var(--border-color-light);
  color: var(--text-muted);
}

.step-running {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.agent-status:has(.Check) {
  background: rgba(var(--success-rgb), 0.2);
}

.agent-status:has(.Close) {
  background: rgba(var(--error-rgb), 0.2);
}

.agent-status:has(.Minus) {
  background: rgba(var(--warning-rgb), 0.2);
}

.agent-name {
  flex: 1;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.agent-badge {
  font-size: 9px;
  padding: 3px 8px;
  border-radius: 10px;
  font-weight: 500;
}

.agent-badge.pending {
  background: var(--border-color-light);
  color: var(--text-muted);
}

.agent-badge.running {
  background: rgba(var(--primary-rgb), 0.2);
  color: var(--primary-light);
}

.agent-badge.completed {
  background: rgba(var(--success-rgb), 0.2);
  color: #10b981;
}

.agent-badge.failed {
  background: rgba(var(--error-rgb), 0.2);
  color: var(--error-color);
}

.agent-badge.skipped {
  background: rgba(var(--warning-rgb), 0.2);
  color: #f59e0b;
}

.agent-header-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.view-detail-btn {
  font-size: 11px;
  color: var(--primary-color);
  padding: 4px 10px;
  border-radius: 6px;
  transition: all 0.2s;
}

.view-detail-btn:hover:not(:disabled) {
  background: rgba(var(--primary-rgb), 0.1);
}

.view-detail-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.agent-toggle {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s;
  opacity: 0.6;
}

.agent-toggle.rotated {
  transform: rotate(180deg);
}

.agent-card:hover .agent-toggle {
  opacity: 1;
}

.agent-output-wrapper {
  margin-top: 0;
  padding-top: 12px;
  border-top: 1px dashed var(--border-color);
  animation: slideDown 0.2s ease;
}

@keyframes slideDown {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}

.agent-output {
  font-size: 12px;
  line-height: 1.6;
  color: var(--text-secondary);
  max-height: 200px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-word;
}

.agent-output::-webkit-scrollbar {
  width: 4px;
}

.agent-output::-webkit-scrollbar-track {
  background: transparent;
}

.agent-output::-webkit-scrollbar-thumb {
  background: rgba(var(--primary-rgb), 0.3);
  border-radius: 2px;
}

.agent-output h1,
.agent-output h2,
.agent-output h3 {
  font-size: 13px;
  font-weight: 600;
  margin: 6px 0 4px;
  color: var(--text-primary);
}

.agent-output h1 { font-size: 1.3em; color: var(--primary-color); }
.agent-output h2 { font-size: 1.2em; color: var(--primary-color); }
.agent-output h3 { font-size: 1.1em; }

.agent-output p {
  margin: 4px 0;
}

.agent-output ul,
.agent-output ol {
  margin: 4px 0;
  padding-left: 18px;
}

.agent-output li {
  margin: 3px 0;
}

.agent-output li::marker {
  color: var(--primary-color);
}

.agent-output strong {
  font-weight: 600;
  color: var(--text-primary);
}

.agent-output code {
  background: rgba(var(--primary-rgb), 0.15);
  padding: 2px 5px;
  border-radius: 3px;
  font-size: 11px;
  font-family: 'SF Mono', Monaco, monospace;
  color: var(--primary-color);
}

.agent-output pre {
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 10px;
  margin: 6px 0;
  overflow-x: auto;
}

.agent-output pre code {
  background: transparent;
  padding: 0;
  color: var(--text-primary);
}

.typing-indicator {
  display: flex;
  gap: 6px;
  padding: 10px 0;
  justify-content: flex-start;
}

.typing-dot {
  width: 6px;
  height: 6px;
  background: var(--primary-color);
  border-radius: 50%;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-dot:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.5; }
  40% { transform: scale(1); opacity: 1; }
}

.agent-error-msg {
  margin-top: 10px;
  padding: 10px;
  background: rgba(var(--error-rgb), 0.1);
  border-radius: 8px;
  font-size: 11px;
  color: #f87171;
  line-height: 1.5;
}

:deep(.agent-detail-dialog .el-dialog__header) {
  background: var(--modal-bg);
  border-bottom: 1px solid var(--border-color);
}

:deep(.agent-detail-dialog .el-dialog__title) {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

:deep(.agent-detail-dialog .el-dialog__body) {
  background: var(--input-bg);
  padding: 24px;
  max-height: 60vh;
  overflow-y: auto;
}

.agent-detail-content {
  font-size: 14px;
  line-height: 1.8;
  color: var(--text-secondary);
}

.agent-detail-content h1,
.agent-detail-content h2,
.agent-detail-content h3,
.agent-detail-content h4 {
  font-weight: 600;
  margin: 16px 0 10px;
  color: var(--text-primary);
}

.agent-detail-content h1 { font-size: 1.5em; }
.agent-detail-content h2 { font-size: 1.3em; }
.agent-detail-content h3 { font-size: 1.15em; }
.agent-detail-content h4 { font-size: 1em; }

.agent-detail-content p {
  margin: 10px 0;
}

.agent-detail-content ul,
.agent-detail-content ol {
  margin: 10px 0;
  padding-left: 24px;
}

.agent-detail-content li {
  margin: 6px 0;
}

.agent-detail-content strong {
  font-weight: 600;
  color: var(--primary-color);
}

.agent-detail-content code {
  background: rgba(var(--primary-rgb), 0.15);
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 0.9em;
  font-family: 'SF Mono', Monaco, monospace;
  color: var(--primary-color);
}

.agent-detail-content pre {
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 10px;
  padding: 16px;
  margin: 12px 0;
  overflow-x: auto;
}

.agent-detail-content pre code {
  background: transparent;
  padding: 0;
  color: var(--text-primary);
}

.agent-detail-content blockquote {
  border-left: 3px solid var(--primary-color);
  padding: 10px 16px;
  margin: 12px 0;
  background: rgba(var(--primary-rgb), 0.08);
  border-radius: 0 8px 8px 0;
  color: var(--text-secondary);
  font-style: italic;
}

.agent-detail-content table {
  width: 100%;
  border-collapse: collapse;
  margin: 12px 0;
  font-size: 0.9em;
}

.agent-detail-content th,
.agent-detail-content td {
  border: 1px solid var(--border-color);
  padding: 10px 14px;
  text-align: left;
}

.agent-detail-content th {
  background: rgba(var(--primary-rgb), 0.1);
  font-weight: 600;
  color: var(--text-primary);
}

.agent-detail-content tr:nth-child(even) {
  background: rgba(var(--primary-rgb), 0.03);
}

.agent-detail-content a {
  color: var(--primary-color);
  text-decoration: none;
  border-bottom: 1px dashed rgba(var(--primary-rgb), 0.4);
}

.agent-detail-content a:hover {
  border-bottom-style: solid;
}

.agent-detail-content hr {
  border: none;
  border-top: 1px solid var(--border-color);
  margin: 20px 0;
}

.loading-placeholder {
  padding: 24px;
  background: var(--input-bg);
  border-radius: 16px;
  margin-bottom: 24px;
}

.result-section {
  margin-top: 24px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.result-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
}

.success-icon {
  font-size: 24px;
}

.result-actions {
  display: flex;
  gap: 10px;
}

.result-btn {
  padding: 10px 20px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 500;
  background: var(--border-color);
  border: 1px solid var(--border-color-light);
  color: var(--text-secondary);
  transition: all 0.2s;
}

.result-btn:hover {
  background: var(--border-color-light);
}

.result-btn.primary {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
  border: none;
  color: #fff;
}

.result-btn.primary:hover {
  box-shadow: 0 4px 15px rgba(var(--primary-rgb), 0.3);
}

.result-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  padding: 20px;
  background: rgba(var(--success-rgb), 0.08);
  border-radius: 14px;
  margin-bottom: 20px;
  border: 1px solid rgba(var(--success-rgb), 0.15);
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.summary-key {
  font-size: 13px;
  color: var(--text-muted);
  font-weight: 500;
}

.summary-val {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.result-days {
  background: var(--input-bg);
  border-radius: 16px;
  padding: 24px;
  border: 1px solid var(--border-color);
}

.result-day {
  padding: 16px 0;
  border-bottom: 1px solid var(--border-color);
}

.result-day:last-child {
  border-bottom: none;
}

.day-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.day-number {
  font-size: 12px;
  font-weight: 700;
  color: var(--primary-color);
  background: rgba(var(--primary-rgb), 0.15);
  padding: 4px 12px;
  border-radius: 8px;
}

.day-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.activity-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  font-size: 13px;
}

.activity-time {
  font-family: 'SF Mono', Monaco, monospace;
  color: var(--text-muted);
  min-width: 90px;
}

.activity-name {
  color: var(--text-primary);
  font-weight: 500;
}

.activity-place {
  color: var(--text-secondary);
  font-size: 12px;
}

.activity-tag {
  margin-left: auto;
  font-size: 11px;
  background: rgba(var(--primary-rgb), 0.15);
  color: var(--primary-light);
  border-color: rgba(var(--primary-rgb), 0.3);
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.empty-icon {
  font-size: 56px;
  margin-bottom: 16px;
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
}

.xhs-section {
  margin-bottom: 24px;
}

.xhs-section .section-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.xhs-icon {
  font-size: 18px;
}

.xhs-loading {
  font-size: 12px;
  color: var(--primary-color);
  font-weight: 400;
  margin-left: 8px;
}

.xhs-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.xhs-skeleton {
  padding: 16px;
  background: var(--input-bg);
  border-radius: 14px;
}

.xhs-card {
  background: var(--input-bg);
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid var(--border-color);
}

.xhs-card:hover {
  transform: translateY(-3px);
  border-color: rgba(var(--primary-rgb), 0.3);
  box-shadow: 0 8px 25px var(--shadow-sm);
}

.xhs-card-image {
  width: 100%;
  height: 160px;
  overflow: hidden;
  position: relative;
}

.xhs-card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.xhs-card:hover .xhs-card-image img {
  transform: scale(1.05);
}

.xhs-image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(to top, var(--overlay-bg-dark), transparent);
  pointer-events: none;
}

.xhs-card-content {
  padding: 16px;
}

.xhs-card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 6px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.xhs-card-desc {
  font-size: 12px;
  color: var(--text-muted);
  line-height: 1.5;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.xhs-card-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.xhs-author {
  display: flex;
  align-items: center;
  gap: 6px;
}

.xhs-avatar {
  font-size: 16px;
}

.xhs-author span {
  font-size: 12px;
  color: var(--text-muted);
}

.xhs-stats {
  display: flex;
  gap: 10px;
}

.xhs-stat {
  font-size: 11px;
  color: var(--text-muted);
}

.xhs-card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.xhs-tag {
  font-size: 11px;
  color: var(--primary-light);
  background: rgba(var(--primary-rgb), 0.15);
  padding: 3px 8px;
  border-radius: 4px;
}

@media (max-width: 768px) {
  .page-container {
    padding: 12px;
  }

  .main-card {
    border-radius: 16px;
    padding: 20px;
  }

  .page-title {
    font-size: 22px;
  }

  .input-section {
    padding: 14px;
  }

  .sample-chips {
    flex-direction: column;
    gap: 6px;
  }

  .sample-chip {
    font-size: 11px;
    padding: 7px 12px;
  }

  .action-row {
    flex-direction: column;
    gap: 10px;
  }

  .generate-btn {
    width: 100%;
    height: 46px;
  }

  .cancel-btn {
    width: 100%;
    height: 46px;
  }

  .agents-container {
    grid-template-columns: 1fr;
  }

  .xhs-cards {
    grid-template-columns: 1fr;
  }

  .progress-bar-wrapper {
    padding: 12px 14px;
  }

  .progress-text {
    font-size: 13px;
  }

  .result-summary {
    flex-direction: column;
    gap: 10px;
  }

  .activity-row {
    flex-wrap: wrap;
  }

  .activity-time {
    min-width: auto;
  }
}
</style>