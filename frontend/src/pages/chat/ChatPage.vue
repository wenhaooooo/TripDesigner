<script setup lang="ts">
import { ref, computed, nextTick, onMounted, watch } from 'vue'
import { conversationApi } from '@/api/conversation'
import type { ConversationMessageVO } from '@/types/api'
import { ChatLineRound, Menu } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

interface RouteProps {
  id?: string | number
}
const props = defineProps<RouteProps>()
const conversationId = computed(() => Number(props.id))

const messages = ref<ConversationMessageVO[]>([])
const inputValue = ref('')
const loading = ref(false)
const sending = ref(false)

const messagesContainer = ref<HTMLElement | null>(null)

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

onMounted(async () => {
  try {
    const res = await conversationApi.list()
    conversations.value = res.data
  } catch {
  }

  if (isNaN(conversationId.value) || conversationId.value === 0) {
    if (conversations.value.length > 0) {
      selectConversation(conversations.value[0].id)
    } else {
      await createNewConversation()
    }
    return
  }

  const exists = conversations.value.some(c => c.id === conversationId.value)
  if (!exists) {
    await createNewConversation()
    return
  }

  await fetchMessages()
  scrollToBottom()
})

watch(() => props.id, async () => {
  if (isNaN(conversationId.value) || conversationId.value === 0) return
  await fetchMessages()
  scrollToBottom()
})

async function fetchMessages() {
  try {
    const res = await conversationApi.listMessages(conversationId.value)
    messages.value = res.data
  } catch {
    messages.value = []
  }
}

async function createNewConversation() {
  try {
    const res = await conversationApi.create({ title: '新对话' })
    const newId = res.data.id
    selectedConvId.value = newId
    import('vue-router').then(({ useRouter }) => {
      const router = useRouter()
      router.push(`/chat/${newId}`)
    })
  } catch {
  }
}

async function sendMessage() {
  const text = inputValue.value.trim()
  if (!text) return

  sending.value = true
  inputValue.value = ''

  const userMsg: ConversationMessageVO = {
    id: Date.now(),
    conversationId: conversationId.value,
    role: 'USER',
    content: text,
    createdAt: new Date().toISOString(),
  }
  messages.value.push(userMsg)
  scrollToBottom()

  try {
    await conversationApi.addMessage(conversationId.value, 'USER', text)
    await fetchMessages()
    scrollToBottom()
  } catch {
    messages.value.pop()
  } finally {
    sending.value = false
  }
}

function formatTime(date: string) {
  return dayjs(date).format('HH:mm')
}

const conversations = ref<any[]>([])
const selectedConvId = ref(conversationId.value)
const showConvList = ref(true)

function selectConversation(id: number) {
  selectedConvId.value = id
  messages.value = []
  import('vue-router').then(({ useRouter }) => {
    const router = useRouter()
    router.push(`/chat/${id}`)
  })
}
</script>

<template>
  <div class="chat-page">
    <aside class="chat-sidebar" :class="{ hidden: !showConvList }">
      <div class="sidebar-header">
        <div class="sidebar-title">
          <el-icon :size="20" color="var(--primary-color)"><ChatLineRound /></el-icon>
          <span>对话列表</span>
        </div>
        <el-button size="small" class="close-btn" @click="showConvList = false">×</el-button>
      </div>
      <div class="conv-list">
        <div v-if="conversations.length === 0" class="empty-conversations">
          <div class="empty-icon-wrapper">
            <ChatLineRound :size="32" color="var(--text-muted)" />
          </div>
          <div class="empty-text">暂无对话</div>
        </div>
        <div
          v-for="conv in conversations"
          :key="conv.id"
          class="conv-item"
          :class="{ active: selectedConvId === conv.id }"
          @click="selectConversation(conv.id)"
        >
          <div class="conv-title">{{ conv.title || '新对话' }}</div>
          <div class="conv-time">{{ formatTime(conv.updatedAt || conv.createdAt) }}</div>
        </div>
      </div>
    </aside>

    <el-button
      v-if="!showConvList"
      circle
      size="small"
      class="toggle-sidebar"
      @click="showConvList = true"
    >
      <Menu :size="18" />
    </el-button>

    <main class="chat-main">
      <header class="chat-header">
        <el-button :icon="Menu" link class="menu-btn" @click="showConvList = true">
          对话列表
        </el-button>
        <div class="header-info">
          <span class="chat-title">对话 #{{ conversationId }}</span>
        </div>
      </header>

      <div ref="messagesContainer" class="messages-container">
        <div v-if="messages.length === 0 && !sending" class="empty-state">
          <div class="empty-avatar-wrapper">
            <el-avatar :size="80" class="empty-avatar">
              <el-icon :size="44" color="#fff"><ChatLineRound /></el-icon>
            </el-avatar>
            <div class="empty-avatar-glow"></div>
          </div>
          <div class="empty-title">开始对话</div>
          <div class="empty-desc">输入消息开始与 AI 对话</div>
        </div>

        <template v-for="msg in messages" :key="msg.id">
          <div
            class="message"
            :class="msg.role === 'USER' ? 'user-msg' : 'assistant-msg'"
          >
            <div class="msg-avatar">
              <el-avatar :size="36" :class="msg.role === 'USER' ? 'user-avatar' : 'assistant-avatar'">
                {{ msg.role === 'USER' ? '我' : 'AI' }}
              </el-avatar>
            </div>
            <div class="msg-content">
              <div class="msg-bubble" :class="msg.role === 'USER' ? 'user-bubble' : 'assistant-bubble'">
                {{ msg.content }}
              </div>
              <div class="msg-time">{{ formatTime(msg.createdAt) }}</div>
            </div>
          </div>
        </template>

        <div v-if="sending" class="message assistant-msg">
          <div class="msg-avatar">
            <el-avatar :size="36" class="assistant-avatar">AI</el-avatar>
          </div>
          <div class="msg-content">
            <div class="msg-bubble typing-bubble">
              <span class="typing-dot"></span>
              <span class="typing-dot"></span>
              <span class="typing-dot"></span>
            </div>
          </div>
        </div>
      </div>

      <footer class="chat-input">
        <div class="input-row">
          <el-input
            v-model="inputValue"
            type="textarea"
            :rows="2"
            :autosize="{ minRows: 1, maxRows: 4 }"
            resize="none"
            placeholder="输入消息... (Enter 发送)"
            @keydown.enter.exact.prevent="sendMessage"
            class="message-input"
          />
          <el-button
            type="primary"
            :icon="ChatLineRound"
            :loading="sending"
            :disabled="!inputValue.trim()"
            class="send-btn"
            @click="sendMessage"
          >
            发送
          </el-button>
        </div>
      </footer>
    </main>
  </div>
</template>

<style scoped>
.chat-page {
  display: flex;
  height: 100%;
  background: var(--bg-secondary);
}

.chat-sidebar {
  width: 260px;
  flex-shrink: 0;
  background: var(--sidebar-bg);
  backdrop-filter: blur(20px);
  border-right: 1px solid var(--hover-bg);
  display: flex;
  flex-direction: column;
  transition: width 0.2s;
}

.chat-sidebar.hidden {
  width: 0;
  overflow: hidden;
}

.sidebar-header {
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.close-btn {
  color: var(--text-secondary);
  border: none;
  padding: 4px 8px;
  font-size: 18px;
  transition: color 0.2s;
}

.close-btn:hover {
  color: var(--text-primary);
}

.conv-list {
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
}

.conv-item {
  padding: 12px 14px;
  border-radius: 12px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: all 0.2s;
  background: var(--input-bg);
}

.conv-item:hover {
  background: var(--hover-bg);
}

.conv-item.active {
  background: rgba(var(--primary-rgb), 0.15);
  border: 1px solid rgba(var(--primary-rgb), 0.3);
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

.toggle-sidebar {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 10;
  background: var(--modal-bg);
  border: 1px solid rgba(var(--primary-rgb), 0.2);
  color: var(--text-secondary);
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-header {
  height: 70px;
  flex-shrink: 0;
  background: var(--modal-bg);
  border-bottom: 1px solid var(--hover-bg);
  display: flex;
  align-items: center;
  padding: 0 24px;
  gap: 12px;
}

.menu-btn {
  color: var(--text-secondary);
  font-size: 13px;
}

.header-info {
  flex: 1;
}

.chat-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
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

.empty-state {
  margin: auto;
  text-align: center;
  padding: 60px 20px;
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

@keyframes avatar-pulse {
  0%, 100% { transform: translate(-50%, -50%) scale(1); opacity: 0.6; }
  50% { transform: translate(-50%, -50%) scale(1.1); opacity: 0.3; }
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

.message {
  display: flex;
  gap: 12px;
  max-width: 75%;
}

.user-msg {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.assistant-msg {
  align-self: flex-start;
}

.msg-avatar {
  flex-shrink: 0;
}

.user-avatar {
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
  color: #fff;
}

.assistant-avatar {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
  color: #fff;
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
  white-space: pre-wrap;
  word-break: break-word;
}

.user-bubble {
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

.msg-time {
  font-size: 11px;
  color: var(--text-muted);
  padding: 0 4px;
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

.chat-input {
  flex-shrink: 0;
  background: var(--modal-bg);
  border-top: 1px solid var(--hover-bg);
  padding: 16px 24px 20px;
}

.input-row {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.message-input {
  flex: 1;
}

:deep(.message-input .el-textarea__inner) {
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

:deep(.message-input .el-textarea__inner::placeholder) {
  color: var(--text-secondary);
}

:deep(.message-input .el-textarea__inner:focus) {
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
  .chat-sidebar {
    display: none;
  }

  .messages-container {
    padding: 16px 12px;
  }

  .message {
    max-width: 90%;
  }

  .chat-input {
    padding: 12px 12px 16px;
  }
}
</style>