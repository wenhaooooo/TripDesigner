<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { communityApi } from '@/api/community'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { CommunityPostVO, CommunityCommentVO, CreateCommentRequest } from '@/types/api'
import { ArrowLeft, ChatDotRound, Star, Delete, Plus } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()

const postId = Number(route.params.id)
const post = ref<CommunityPostVO | null>(null)
const comments = ref<CommunityCommentVO[]>([])
const loading = ref(false)
const submitting = ref(false)

const newComment = ref<CreateCommentRequest>({ content: '' })
const replyTo = ref<CommunityCommentVO | null>(null)
const replyContent = ref('')

onMounted(() => {
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const [postRes, commentsRes] = await Promise.all([
      communityApi.getPost(postId),
      communityApi.listComments(postId),
    ])
    post.value = postRes.data
    comments.value = commentsRes.data
  } finally {
    loading.value = false
  }
}

async function toggleLike() {
  if (!post.value) return
  try {
    const res = await communityApi.toggleLike(post.value.id)
    post.value.likedByMe = res.data.liked
    post.value.likeCount += res.data.liked ? 1 : -1
  } catch {
    // 错误由全局拦截器提示
  }
}

async function toggleFavorite() {
  if (!post.value) return
  try {
    const res = await communityApi.toggleFavorite(post.value.id)
    post.value.favoritedByMe = res.data.favorited
    post.value.favoriteCount += res.data.favorited ? 1 : -1
  } catch {
    // 错误由全局拦截器提示
  }
}

async function submitComment() {
  if (!newComment.value.content?.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  submitting.value = true
  try {
    await communityApi.createComment(postId, {
      content: newComment.value.content.trim(),
    })
    ElMessage.success('评论成功')
    newComment.value = { content: '' }
    await loadData()
  } catch {
    // 错误由全局拦截器提示
  } finally {
    submitting.value = false
  }
}

function startReply(comment: CommunityCommentVO) {
  replyTo.value = comment
  replyContent.value = ''
}

function cancelReply() {
  replyTo.value = null
  replyContent.value = ''
}

async function submitReply(parent: CommunityCommentVO) {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  submitting.value = true
  try {
    await communityApi.createComment(postId, {
      content: replyContent.value.trim(),
      parentId: parent.id,
    })
    ElMessage.success('回复成功')
    cancelReply()
    await loadData()
  } catch {
    // 错误由全局拦截器提示
  } finally {
    submitting.value = false
  }
}

async function toggleCommentLike(comment: CommunityCommentVO) {
  try {
    const res = await communityApi.toggleCommentLike(comment.id)
    comment.likedByMe = res.data.liked
    comment.likeCount += res.data.liked ? 1 : -1
  } catch {
    // 错误由全局拦截器提示
  }
}

async function handleDeleteComment(comment: CommunityCommentVO) {
  try {
    await ElMessageBox.confirm('确定删除该评论吗？', '确认删除', { type: 'warning' })
    await communityApi.deleteComment(comment.id)
    ElMessage.success('已删除')
    await loadData()
  } catch {
    // cancelled
  }
}

function goBack() {
  router.push('/community')
}

function formatDate(date: string) {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}
</script>

<template>
  <div class="page-container">
    <el-button :icon="ArrowLeft" link @click="goBack" style="margin-bottom: 12px;">
      返回社区
    </el-button>

    <div v-loading="loading">
      <div v-if="post" class="post-detail">
        <el-card class="post-card" shadow="never">
          <div class="post-header">
            <h1 class="post-title">{{ post.title }}</h1>
            <div class="post-tags" v-if="post.tags && post.tags.length > 0">
              <el-tag v-for="tag in post.tags" :key="tag" size="small" effect="plain">
                {{ tag }}
              </el-tag>
            </div>
          </div>

          <div class="post-meta">
            <el-avatar :size="32" :style="{ background: '#1890ff' }">
              {{ post.authorEmail[0]?.toUpperCase() }}
            </el-avatar>
            <div class="meta-info">
              <div class="author">{{ post.authorEmail }}</div>
              <div class="time">{{ formatDate(post.createdAt) }}</div>
            </div>
            <el-tag v-if="post.destination" effect="plain" size="small">
              {{ post.destination }}
            </el-tag>
          </div>

          <div class="post-content">{{ post.content }}</div>

          <div class="post-actions">
            <el-button
              :type="post.likedByMe ? 'primary' : ''"
              :icon="ChatDotRound"
              @click="toggleLike"
            >
              {{ post.likeCount }} 点赞
            </el-button>
            <el-button
              :type="post.favoritedByMe ? 'warning' : ''"
              :icon="Star"
              @click="toggleFavorite"
            >
              {{ post.favoriteCount }} 收藏
            </el-button>
          </div>
        </el-card>

        <el-card class="comment-section" shadow="never">
          <div class="section-title">
            <span>评论 ({{ comments.length }})</span>
          </div>

          <div class="comment-input">
            <el-input
              v-model="newComment.content"
              type="textarea"
              :rows="3"
              placeholder="分享你的看法..."
              maxlength="500"
              show-word-limit
            />
            <div class="input-actions">
              <el-button type="primary" :loading="submitting" @click="submitComment">
                发布评论
              </el-button>
            </div>
          </div>

          <div class="comment-list">
            <div v-if="comments.length === 0" class="empty-comments">
              还没有评论，快来抢沙发吧
            </div>

            <div v-for="comment in comments" :key="comment.id" class="comment-item">
              <el-avatar :size="32" :style="{ background: '#52c41a' }">
                {{ comment.authorEmail[0]?.toUpperCase() }}
              </el-avatar>
              <div class="comment-body">
                <div class="comment-header">
                  <span class="comment-author">{{ comment.authorEmail }}</span>
                  <span class="comment-time">{{ formatDate(comment.createdAt) }}</span>
                </div>
                <div class="comment-content">{{ comment.content }}</div>
                <div class="comment-actions">
                  <el-button
                    :type="comment.likedByMe ? 'primary' : ''"
                    :icon="ChatDotRound"
                    link
                    size="small"
                    @click="toggleCommentLike(comment)"
                  >
                    {{ comment.likeCount }}
                  </el-button>
                  <el-button :icon="Plus" link size="small" @click="startReply(comment)">
                    回复
                  </el-button>
                  <el-button
                    :icon="Delete"
                    link
                    type="danger"
                    size="small"
                    @click="handleDeleteComment(comment)"
                  >
                    删除
                  </el-button>
                </div>

                <div v-if="replyTo?.id === comment.id" class="reply-box">
                  <el-input
                    v-model="replyContent"
                    type="textarea"
                    :rows="2"
                    :placeholder="`回复 ${comment.authorEmail}...`"
                    maxlength="500"
                  />
                  <div class="reply-actions">
                    <el-button size="small" @click="cancelReply">取消</el-button>
                    <el-button
                      type="primary"
                      size="small"
                      :loading="submitting"
                      @click="submitReply(comment)"
                    >
                      回复
                    </el-button>
                  </div>
                </div>

                <div v-if="comment.replies && comment.replies.length > 0" class="replies">
                  <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
                    <el-avatar :size="24" :style="{ background: '#722ed1' }">
                      {{ reply.authorEmail[0]?.toUpperCase() }}
                    </el-avatar>
                    <div class="reply-body">
                      <div class="comment-header">
                        <span class="comment-author">{{ reply.authorEmail }}</span>
                        <span class="comment-time">{{ formatDate(reply.createdAt) }}</span>
                      </div>
                      <div class="comment-content">{{ reply.content }}</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<style scoped>
.post-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.post-card,
.comment-section {
  background: var(--input-bg);
  backdrop-filter: blur(12px);
  border: 1px solid var(--border-color);
  border-radius: 16px;
}

.post-header {
  margin-bottom: 16px;
}

.post-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 12px;
  letter-spacing: -0.5px;
}

.post-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.post-tags .el-tag {
  color: var(--primary-light);
  border-color: rgba(var(--primary-rgb), 0.3);
  background: var(--hover-bg);
}

.post-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 20px;
}

.meta-info {
  flex: 1;
}

.author {
  font-size: 14px;
  color: var(--primary-light);
  font-weight: 600;
}

.time {
  font-size: 12px;
  color: var(--text-muted);
}

.post-content {
  font-size: 15px;
  color: var(--text-primary);
  line-height: 1.8;
  white-space: pre-wrap;
  margin-bottom: 20px;
}

.post-actions {
  display: flex;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

.post-actions .el-button {
  padding: 8px 16px;
  border-radius: 10px;
}

.post-actions .el-button--primary {
  background: rgba(var(--primary-rgb), 0.15);
  border-color: rgba(var(--primary-rgb), 0.3);
  color: var(--primary-light);
}

.post-actions .el-button--warning {
  background: rgba(var(--warning-rgb), 0.15);
  border-color: rgba(var(--warning-rgb), 0.3);
  color: #f59e0b;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 20px;
}

.comment-input {
  background: var(--input-bg);
  padding: 16px;
  border-radius: 12px;
  margin-bottom: 20px;
  border: 1px solid var(--border-color);
}

.comment-input :deep(.el-input__wrapper) {
  background: var(--overlay-bg-dark);
  border-color: var(--border-color-light);
}

.comment-input :deep(.el-input__inner) {
  color: var(--text-primary);
}

.comment-input :deep(.el-input__placeholder) {
  color: var(--text-secondary);
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.input-actions .el-button {
  padding: 10px 24px;
  border-radius: 10px;
}

.input-actions .el-button--primary {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
  border: none;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.empty-comments {
  text-align: center;
  color: var(--text-muted);
  font-size: 14px;
  padding: 40px 0;
}

.comment-item {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: var(--bg-tertiary);
  border-radius: 12px;
  border: 1px solid var(--border-color);
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.comment-author {
  font-size: 14px;
  color: var(--primary-light);
  font-weight: 600;
}

.comment-time {
  font-size: 12px;
  color: var(--text-muted);
}

.comment-content {
  font-size: 14px;
  color: var(--text-primary);
  line-height: 1.6;
  margin-bottom: 10px;
}

.comment-actions {
  display: flex;
  gap: 8px;
}

.comment-actions .el-button {
  font-size: 12px;
}

.reply-box {
  margin: 12px 0;
  background: var(--overlay-bg-dark);
  border: 1px solid rgba(var(--primary-rgb), 0.2);
  border-radius: 10px;
  padding: 12px;
}

.reply-box :deep(.el-input__wrapper) {
  background: var(--overlay-bg-dark);
  border-color: var(--border-color-light);
}

.reply-box :deep(.el-input__inner) {
  color: var(--text-primary);
}

.reply-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

.reply-actions .el-button {
  padding: 6px 16px;
  border-radius: 8px;
}

.reply-actions .el-button--primary {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
  border: none;
}

.replies {
  margin-top: 16px;
  padding-left: 16px;
  border-left: 3px solid rgba(var(--primary-rgb), 0.3);
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.reply-item {
  display: flex;
  gap: 10px;
}

.reply-body {
  flex: 1;
}

.reply-body .comment-content {
  font-size: 13px;
}

@media (max-width: 768px) {
  .post-title {
    font-size: 20px;
  }

  .post-content {
    font-size: 14px;
  }

  .comment-item {
    padding: 12px;
  }
}
</style>
