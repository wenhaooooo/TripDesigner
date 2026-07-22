<script setup lang="ts">
import { ref, nextTick, computed, onMounted } from 'vue'
import { advisorApi } from '@/api/advisor'
import { conversationApi } from '@/api/conversation'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Promotion, Headset, ChatLineRound, Plus, Delete } from '@element-plus/icons-vue'
import type { AdvisorResponse, ConversationVO, ConversationMessageVO } from '@/types/api'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

interface ChatMessage {
  id: number
  role: 'user' | 'assistant'
  content: string
  createdAt: string
  streaming?: boolean
  waiting?: boolean
}

const messages = ref<ChatMessage[]>([])
const inputValue = ref('')
const sending = ref(false)
const conversationId = ref<number | undefined>(undefined)
const messagesContainer = ref<HTMLElement | null>(null)
const inputRef = ref<any>(null)
const conversations = ref<ConversationVO[]>([])
const loadingConversations = ref(false)
const loadingMessages = ref(false)
const selectedIds = ref<number[]>([])
const selectAll = ref(false)

const CONVERSATION_ID_KEY = 'advisor_conversation_id'

const recommendedQuestions = [
  '去日本需要签证吗？',
  '巴厘岛最佳旅行时间是什么时候？',
  '欧洲穷游有什么推荐路线？',
  '泰国落地签怎么办理？',
  '带小孩去新加坡玩什么？',
  '去西藏旅行需要注意什么？',
]

const isEmpty = computed(() => messages.value.length === 0)
const canSend = computed(() => inputValue.value.trim().length > 0 && !sending.value)
const isAllSelected = computed(() => 
  conversations.value.length > 0 && 
  conversations.value.every(c => selectedIds.value.includes(c.id))
)

function genId() {
  return Date.now() * 1000 + Math.floor(Math.random() * 1000)
}

function formatTime(date: string) {
  const d = new Date(date)
  const h = String(d.getHours()).padStart(2, '0')
  const m = String(d.getMinutes()).padStart(2, '0')
  return `${h}:${m}`
}

function formatDate(date: string) {
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function renderMarkdown(text: string): string {
  try {
    const html = marked.parse(text) as string
    return DOMPurify.sanitize(html)
  } catch {
    return text
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

function focusInput() {
  nextTick(() => {
    inputRef.value?.focus?.()
  })
}

function pickQuestion(q: string) {
  inputValue.value = q
  focusInput()
}

async function loadConversations() {
  loadingConversations.value = true
  try {
    const res = await conversationApi.list()
    conversations.value = res.data || []
  } catch {
  } finally {
    loadingConversations.value = false
  }
}

async function loadConversationMessages(convId: number) {
  loadingMessages.value = true
  try {
    const res = await conversationApi.listMessages(convId)
    const msgList = res.data || []
    messages.value = msgList.map((m: ConversationMessageVO) => ({
      id: m.id,
      role: m.role.toLowerCase() === 'user' ? 'user' : 'assistant',
      content: m.content,
      createdAt: m.createdAt,
    }))
    conversationId.value = convId
    localStorage.setItem(CONVERSATION_ID_KEY, String(convId))
    scrollToBottom()
  } catch (err: any) {
    ElMessage.error('加载对话失败：' + (err?.message || ''))
  } finally {
    loadingMessages.value = false
  }
}

async function createNewConversation() {
  messages.value = []
  conversationId.value = undefined
  localStorage.removeItem(CONVERSATION_ID_KEY)
  focusInput()
}

async function deleteConversation(convId: number) {
  try {
    await ElMessageBox.confirm(
      '确定删除这条对话吗？删除后无法恢复。',
      '确认删除',
      { type: 'warning' }
    )
    if (conversationId.value === convId) {
      messages.value = []
      conversationId.value = undefined
      localStorage.removeItem(CONVERSATION_ID_KEY)
    }
    await conversationApi.delete(convId)
    await loadConversations()
    selectedIds.value = selectedIds.value.filter(id => id !== convId)
    ElMessage.success('对话已删除')
  } catch {
  }
}

async function deleteSelectedConversations() {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请选择要删除的对话')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectedIds.value.length} 条对话吗？删除后无法恢复。`,
      '批量删除确认',
      { type: 'warning' }
    )
    const targetIds = [...selectedIds.value]
    if (targetIds.includes(conversationId.value!)) {
      messages.value = []
      conversationId.value = undefined
      localStorage.removeItem(CONVERSATION_ID_KEY)
    }
    for (const id of targetIds) {
      await conversationApi.delete(id)
    }
    await loadConversations()
    selectedIds.value = []
    ElMessage.success('已删除选中的对话')
  } catch {
  }
}

function toggleSelect(id: number) {
  const index = selectedIds.value.indexOf(id)
  if (index > -1) {
    selectedIds.value.splice(index, 1)
  } else {
    selectedIds.value.push(id)
  }
}

function toggleSelectAll() {
  if (isAllSelected.value) {
    selectedIds.value = []
  } else {
    selectedIds.value = conversations.value.map(c => c.id)
  }
}

async function sendMessage(text?: string) {
  const question = (text ?? inputValue.value).trim()
  if (!question || sending.value) return

  inputValue.value = ''
  sending.value = true

  const userMsg: ChatMessage = {
    id: genId(),
    role: 'user',
    content: question,
    createdAt: new Date().toISOString(),
  }
  messages.value.push(userMsg)

  const aiMsg: ChatMessage = {
    id: genId() + 1,
    role: 'assistant',
    content: '',
    createdAt: new Date().toISOString(),
    streaming: true,
    waiting: true,
  }
  messages.value.push(aiMsg)
  scrollToBottom()

  try {
    const response = await advisorApi.askStream({
      question,
      conversationId: conversationId.value,
    })

    if (!response.ok || !response.body) {
      throw new Error('HTTP error ' + response.status)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let currentEvent = 'message'
    let firstChunkReceived = false

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })

      const events = buffer.split('\n\n')
      buffer = events.pop() || ''

      for (const evt of events) {
        const lines = evt.split('\n')
        let dataStr = ''
        currentEvent = 'message'

        for (const line of lines) {
          if (line.startsWith('event:')) {
            currentEvent = line.slice(6).trim()
          } else if (line.startsWith('data:')) {
            dataStr += line.slice(5)
          }
        }
        dataStr = dataStr.trim()

        if (currentEvent === 'complete') {
          try {
            const resp: AdvisorResponse = JSON.parse(dataStr)
            if (resp?.conversationId) {
              conversationId.value = resp.conversationId
              localStorage.setItem(CONVERSATION_ID_KEY, String(resp.conversationId))
              await loadConversations()
            }
          } catch {
          }
        } else if (currentEvent === 'error') {
          throw new Error(dataStr || 'AI 服务异常')
        } else if (dataStr) {
          if (!firstChunkReceived) {
            firstChunkReceived = true
            aiMsg.waiting = false
          }
          aiMsg.content += dataStr
          messages.value[messages.value.length - 1] = { ...aiMsg }
          scrollToBottom()
        }
      }
    }

    aiMsg.streaming = false
    aiMsg.waiting = false
    messages.value[messages.value.length - 1] = { ...aiMsg }
  } catch (err: any) {
    const last = messages.value[messages.value.length - 1]
    if (last && last.role === 'assistant') {
      if (!last.content) {
        messages.value.pop()
      } else {
        last.streaming = false
        last.waiting = false
        last.content += '\n\n[回答已中断]'
        messages.value[messages.value.length - 1] = { ...last }
      }
    }
    const msg = err?.message || 'AI 回答失败，请稍后重试'
    ElMessage.error(msg)
  } finally {
    sending.value = false
    scrollToBottom()
    focusInput()
  }
}

function handleEnter(e: KeyboardEvent) {
  if (e.shiftKey) return
  e.preventDefault()
  sendMessage()
}

function clearConversation() {
  messages.value = []
  conversationId.value = undefined
  localStorage.removeItem(CONVERSATION_ID_KEY)
  focusInput()
}

onMounted(async () => {
  await loadConversations()
  const savedId = localStorage.getItem(CONVERSATION_ID_KEY)
  if (savedId) {
    await loadConversationMessages(parseInt(savedId))
  }
})
</script>

<template>
  <div class="advisor-page">
    <aside class="conversation-sidebar">
      <div class="sidebar-header">
        <div class="sidebar-title">
          <el-icon :size="20" color="var(--primary-color)"><ChatLineRound /></el-icon>
          <span>历史对话</span>
        </div>
        <div class="header-actions">
          <el-button size="small" type="primary" :icon="Plus" class="new-btn" @click="createNewConversation">
            新建
          </el-button>
        </div>
      </div>

      <div v-loading="loadingConversations" class="conversation-list">
        <div v-if="conversations.length === 0" class="empty-conversations">
          <div class="empty-icon-wrapper">
            <ChatLineRound :size="32" color="var(--text-muted)" />
          </div>
          <div class="empty-text">暂无对话</div>
          <div class="empty-hint">点击右上角新建开始对话</div>
        </div>

        <div v-if="conversations.length > 0" class="select-all-row">
          <el-checkbox 
            :model-value="isAllSelected" 
            @change="toggleSelectAll"
            class="select-all-checkbox"
          >
            全选
          </el-checkbox>
          <el-button 
            size="small" 
            text 
            class="batch-delete-btn"
            :disabled="selectedIds.length === 0"
            @click="deleteSelectedConversations"
          >
            <el-icon><Delete /></el-icon>
            批量删除
          </el-button>
        </div>

        <div
          v-for="conv in conversations"
          :key="conv.id"
          class="conversation-item"
          :class="{ active: conversationId === conv.id, selected: selectedIds.includes(conv.id) }"
          @click="loadConversationMessages(conv.id)"
        >
          <el-checkbox 
            :model-value="selectedIds.includes(conv.id)" 
            @click.stop="toggleSelect(conv.id)"
            class="conv-checkbox"
          />
          <div class="conv-info">
            <div class="conv-title">{{ conv.title || '未命名对话' }}</div>
            <div class="conv-time">{{ formatDate(conv.createdAt) }}</div>
          </div>
          <el-button
            size="small"
            text
            class="delete-btn"
            @click.stop="deleteConversation(conv.id)"
          >
            <el-icon color="var(--error-color)"><Delete /></el-icon>
          </el-button>
        </div>
      </div>
    </aside>

    <main class="conversation-main">
      <header class="advisor-header">
        <div class="header-left">
          <div class="header-avatar-wrapper">
            <el-avatar :size="44" class="header-avatar">
              <el-icon :size="24" color="#fff"><Headset /></el-icon>
            </el-avatar>
            <div class="avatar-glow"></div>
          </div>
          <div class="header-title">
            <div class="title-text">AI 旅行顾问</div>
            <div class="title-status">
              <span class="status-dot" :class="{ online: !sending, busy: sending }"></span>
              <span>{{ sending ? '正在回答...' : '在线' }}</span>
            </div>
          </div>
        </div>
        <div class="header-right">
          <el-button size="small" class="clear-btn" :disabled="isEmpty" @click="clearConversation">
            清空对话
          </el-button>
        </div>
      </header>

      <div ref="messagesContainer" class="messages-container" v-loading="loadingMessages">
        <div v-if="isEmpty" class="empty-state">
          <div class="empty-avatar-wrapper">
            <el-avatar :size="80" class="empty-avatar">
              <el-icon :size="44" color="#fff"><Headset /></el-icon>
            </el-avatar>
            <div class="empty-avatar-glow"></div>
          </div>
          <div class="empty-title">你好，我是你的 AI 旅行顾问 🌍</div>
          <div class="empty-desc">
            无论是签证、行程、预算还是目的地推荐，都可以问我。<br />
            试试下面的问题，或者直接输入你想了解的内容。
          </div>
          <div class="empty-recommend">
            <el-tag
              v-for="q in recommendedQuestions"
              :key="q"
              class="recommend-tag"
              effect="plain"
              @click="sendMessage(q)"
            >
              {{ q }}
            </el-tag>
          </div>
        </div>

        <template v-for="msg in messages" :key="msg.id">
          <div v-if="msg.role === 'user'" class="message user-msg">
            <div class="msg-bubble">{{ msg.content }}</div>
            <div class="msg-time">{{ formatTime(msg.createdAt) }}</div>
          </div>

          <div v-else class="message assistant-msg">
            <div class="msg-avatar">
              <el-avatar :size="36" class="msg-avatar-inner">
                <el-icon :size="18" color="#fff"><Headset /></el-icon>
              </el-avatar>
            </div>
            <div class="msg-content">
              <div v-if="msg.waiting" class="msg-bubble typing-bubble">
                <span class="typing-dot"></span>
                <span class="typing-dot"></span>
                <span class="typing-dot"></span>
              </div>
              <div v-else class="msg-bubble assistant-bubble">
                <div class="msg-markdown" v-html="renderMarkdown(msg.content)"></div>
                <span v-if="msg.streaming" class="streaming-cursor">▌</span>
              </div>
              <div v-if="!msg.waiting" class="msg-time">{{ formatTime(msg.createdAt) }}</div>
            </div>
          </div>
        </template>
      </div>

      <footer class="input-area">
        <div class="quick-questions" v-if="!isEmpty">
          <el-tag
            v-for="q in recommendedQuestions.slice(0, 3)"
            :key="q"
            class="quick-tag"
            effect="plain"
            @click="pickQuestion(q)"
          >
            {{ q }}
          </el-tag>
        </div>
        <div class="input-row">
          <el-input
            ref="inputRef"
            v-model="inputValue"
            type="textarea"
            :rows="2"
            :autosize="{ minRows: 1, maxRows: 4 }"
            resize="none"
            placeholder="输入你的问题，Enter 发送，Shift+Enter 换行"
            :disabled="sending"
            @keydown.enter="handleEnter"
            class="chat-input"
          />
          <el-button
            type="primary"
            :icon="Promotion"
            :loading="sending"
            :disabled="!canSend"
            class="send-btn"
            @click="sendMessage()"
          >
            发送
          </el-button>
        </div>
      </footer>
    </main>
  </div>
</template>

<style scoped>
.advisor-page {
  display: flex;
  height: 100%;
  background: var(--bg-secondary);
}

.conversation-sidebar {
  width: 280px;
  flex-shrink: 0;
  background: var(--sidebar-bg);
  backdrop-filter: blur(20px);
  border-right: 1px solid var(--hover-bg);
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--hover-bg);
}

.sidebar-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.new-btn {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
  border: none;
  border-radius: 10px;
  padding: 8px 14px;
  font-weight: 500;
  font-size: 13px;
  transition: all 0.2s;
}

.new-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 15px rgba(var(--primary-rgb), 0.3);
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.empty-conversations {
  padding: 40px 20px;
  text-align: center;
}

.empty-icon-wrapper {
  width: 64px;
  height: 64px;
  margin: 0 auto 12px;
  background: var(--border-color);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-text {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
  margin-bottom: 4px;
}

.empty-hint {
  font-size: 12px;
  color: var(--text-muted);
}

.select-all-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  margin-bottom: 4px;
  font-size: 13px;
}

.select-all-checkbox {
  color: var(--text-secondary);
}

.batch-delete-btn {
  color: var(--error-color);
  font-size: 12px;
}

.batch-delete-btn:hover:not(:disabled) {
  color: var(--error-color);
}

.conversation-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 4px;
  background: var(--input-bg);
}

.conversation-item:hover {
  background: var(--hover-bg);
}

.conversation-item.active {
  background: rgba(var(--primary-rgb), 0.15);
  border: 1px solid rgba(var(--primary-rgb), 0.3);
}

.conversation-item.selected {
  background: rgba(var(--primary-rgb), 0.12);
  border: 1px solid rgba(var(--primary-rgb), 0.4);
}

.conv-checkbox {
  flex-shrink: 0;
}

.conv-info {
  flex: 1;
  min-width: 0;
}

.conv-title {
  font-size: 14px;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conv-time {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
}

.delete-btn {
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.2s;
}

.conversation-item:hover .delete-btn {
  opacity: 1;
}

.conversation-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.advisor-header {
  height: 70px;
  flex-shrink: 0;
  background: var(--modal-bg);
  border-bottom: 1px solid var(--hover-bg);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.header-avatar-wrapper {
  position: relative;
}

.header-avatar {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
  font-size: 16px;
  font-weight: 600;
}

.avatar-glow {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 56px;
  height: 56px;
  background: radial-gradient(circle, rgba(var(--primary-rgb), 0.4), transparent);
  border-radius: 50%;
  animation: avatar-pulse 2s ease-in-out infinite;
}

@keyframes avatar-pulse {
  0%, 100% { transform: translate(-50%, -50%) scale(1); opacity: 0.6; }
  50% { transform: translate(-50%, -50%) scale(1.1); opacity: 0.3; }
}

.header-title {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.title-text {
  font-size: 17px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.2;
}

.title-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-muted);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--text-muted);
}

.status-dot.online {
  background: var(--success-color);
  box-shadow: 0 0 0 3px rgba(var(--success-rgb), 0.15);
}

.status-dot.busy {
  background: var(--warning-color);
  box-shadow: 0 0 0 3px rgba(var(--warning-rgb), 0.15);
  animation: pulse 1.2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.clear-btn {
  background: var(--border-color);
  border: 1px solid var(--border-color-light);
  color: var(--text-secondary);
  border-radius: 10px;
  font-size: 13px;
  padding: 8px 16px;
  transition: all 0.2s;
}

.clear-btn:hover:not(:disabled) {
  background: var(--border-color-light);
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.messages-container::-webkit-scrollbar {
  width: 6px;
}

.messages-container::-webkit-scrollbar-track {
  background: transparent;
}

.messages-container::-webkit-scrollbar-thumb {
  background: rgba(var(--primary-rgb), 0.3);
  border-radius: 3px;
}

.messages-container::-webkit-scrollbar-thumb:hover {
  background: rgba(var(--primary-rgb), 0.5);
}

.empty-state {
  margin: auto;
  text-align: center;
  padding: 60px 20px;
  max-width: 560px;
}

.empty-avatar-wrapper {
  position: relative;
  margin-bottom: 20px;
}

.empty-avatar {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
}

.empty-avatar-glow {
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

.empty-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 10px;
}

.empty-desc {
  font-size: 14px;
  color: var(--text-muted);
  line-height: 1.7;
  margin-bottom: 28px;
}

.empty-recommend {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
}

.recommend-tag {
  cursor: pointer;
  padding: 10px 16px;
  height: auto;
  border-radius: 20px;
  font-size: 13px;
  color: var(--primary-light);
  border-color: rgba(var(--primary-rgb), 0.3);
  background: var(--hover-bg);
  transition: all 0.2s;
}

.recommend-tag:hover {
  background: rgba(var(--primary-rgb), 0.1);
  color: var(--primary-color);
  border-color: rgba(var(--primary-rgb), 0.25);
  transform: translateY(-1px);
}

.message {
  display: flex;
  max-width: 75%;
  gap: 12px;
}

.user-msg {
  align-self: flex-end;
  flex-direction: column;
  align-items: flex-end;
}

.assistant-msg {
  align-self: flex-start;
}

.msg-avatar {
  flex-shrink: 0;
}

.msg-avatar-inner {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
}

.msg-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.msg-bubble {
  padding: 12px 16px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;
}

.user-msg .msg-bubble {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
  color: #fff;
  border-top-right-radius: 6px;
  box-shadow: 0 4px 15px rgba(var(--primary-rgb), 0.3);
}

.assistant-bubble {
  background: var(--input-bg);
  color: var(--text-primary);
  border-top-left-radius: 6px;
  border: 1px solid var(--border-color);
}

.msg-markdown {
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
  white-space: pre-wrap;
}

.msg-markdown :deep(h1),
.msg-markdown :deep(h2),
.msg-markdown :deep(h3),
.msg-markdown :deep(h4),
.msg-markdown :deep(h5),
.msg-markdown :deep(h6) {
  font-weight: 600;
  margin: 16px 0 8px;
  line-height: 1.3;
}

.msg-markdown :deep(h1) { font-size: 1.5em; color: var(--primary-color); }
.msg-markdown :deep(h2) { font-size: 1.3em; color: var(--primary-color); }
.msg-markdown :deep(h3) { font-size: 1.15em; }
.msg-markdown :deep(h4),
.msg-markdown :deep(h5),
.msg-markdown :deep(h6) { font-size: 1em; }

.msg-markdown :deep(p) { margin: 8px 0; }

.msg-markdown :deep(strong) {
  font-weight: 600;
  color: var(--primary-color);
}

.msg-markdown :deep(em) {
  font-style: italic;
  color: var(--text-secondary);
}

.msg-markdown :deep(u) {
  text-decoration: underline;
  text-decoration-color: var(--primary-color);
}

.msg-markdown :deep(s) {
  text-decoration: line-through;
  color: var(--text-muted);
}

.msg-markdown :deep(ul),
.msg-markdown :deep(ol) {
  padding-left: 20px;
  margin: 8px 0;
}

.msg-markdown :deep(li) {
  margin: 4px 0;
}

.msg-markdown :deep(li::marker) {
  color: var(--primary-color);
}

.msg-markdown :deep(blockquote) {
  border-left: 3px solid var(--primary-color);
  padding: 8px 12px;
  margin: 12px 0;
  background: rgba(var(--primary-rgb), 0.08);
  border-radius: 0 6px 6px 0;
  color: var(--text-secondary);
  font-style: italic;
}

.msg-markdown :deep(code) {
  background: rgba(var(--primary-rgb), 0.12);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  font-size: 0.9em;
  color: var(--primary-color);
}

.msg-markdown :deep(pre) {
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 12px 16px;
  margin: 12px 0;
  overflow-x: auto;
}

.msg-markdown :deep(pre code) {
  background: transparent;
  padding: 0;
  color: var(--text-primary);
  font-size: 0.85em;
  line-height: 1.6;
}

.msg-markdown :deep(a) {
  color: var(--primary-color);
  text-decoration: none;
  border-bottom: 1px dashed rgba(var(--primary-rgb), 0.4);
  transition: all 0.2s;
}

.msg-markdown :deep(a:hover) {
  border-bottom-style: solid;
  color: var(--primary-light);
}

.msg-markdown :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 12px 0;
  font-size: 0.9em;
}

.msg-markdown :deep(th),
.msg-markdown :deep(td) {
  border: 1px solid var(--border-color);
  padding: 8px 12px;
  text-align: left;
}

.msg-markdown :deep(th) {
  background: rgba(var(--primary-rgb), 0.1);
  font-weight: 600;
  color: var(--text-primary);
}

.msg-markdown :deep(tr:nth-child(even)) {
  background: rgba(var(--primary-rgb), 0.03);
}

.msg-markdown :deep(tr:hover) {
  background: rgba(var(--primary-rgb), 0.06);
}

.msg-markdown :deep(img) {
  max-width: 100%;
  border-radius: 6px;
  margin: 8px 0;
}

.msg-markdown :deep(hr) {
  border: none;
  border-top: 1px solid var(--border-color);
  margin: 16px 0;
}

.streaming-cursor {
  display: inline-block;
  color: var(--primary-light);
  margin-left: 2px;
  animation: blink 1s step-end infinite;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

.msg-time {
  font-size: 11px;
  color: var(--text-muted);
  padding: 0 4px;
}

.user-msg .msg-time {
  text-align: right;
}

.typing-bubble {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 16px 20px;
}

.typing-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--text-muted);
  animation: typing 1.4s infinite ease-in-out;
}

.typing-dot:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
    background: var(--primary-color);
  }
}

.input-area {
  flex-shrink: 0;
  background: var(--modal-bg);
  border-top: 1px solid var(--hover-bg);
  padding: 16px 24px 20px;
}

.quick-questions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.quick-tag {
  cursor: pointer;
  height: auto;
  padding: 6px 12px;
  border-radius: 14px;
  font-size: 12px;
  color: var(--text-secondary);
  border-color: var(--border-color-light);
  background: var(--border-color);
  transition: all 0.2s;
}

.quick-tag:hover {
  color: var(--primary-light);
  border-color: rgba(var(--primary-rgb), 0.4);
  background: var(--hover-bg);
}

.input-row {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.chat-input {
  flex: 1;
}

:deep(.chat-input .el-textarea__inner) {
  background: var(--input-bg);
  border: 1px solid var(--border-color-light);
  border-radius: 14px;
  padding: 12px 16px;
  font-size: 14px;
  line-height: 1.5;
  color: var(--text-primary);
  resize: none;
  transition: all 0.3s;
}

:deep(.chat-input .el-textarea__inner::placeholder) {
  color: var(--text-muted);
}

:deep(.chat-input .el-textarea__inner:focus) {
  border-color: rgba(var(--primary-rgb), 0.5);
  box-shadow: 0 0 0 3px var(--hover-bg);
}

.send-btn {
  height: 44px;
  border-radius: 14px;
  padding: 0 24px;
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
  border: none;
  font-weight: 600;
  font-size: 14px;
  transition: all 0.3s;
}

.send-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(var(--primary-rgb), 0.4);
}

@media (max-width: 768px) {
  .conversation-sidebar {
    display: none;
  }

  .messages-container {
    padding: 16px 12px;
  }

  .message {
    max-width: 90%;
  }

  .input-area {
    padding: 12px 12px 16px;
  }

  .empty-title {
    font-size: 18px;
  }

  .empty-desc {
    font-size: 13px;
  }
}
</style>