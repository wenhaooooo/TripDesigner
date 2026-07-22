<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { communityApi } from '@/api/community'
import { behaviorApi } from '@/api/behavior'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { CommunityPostVO, CreatePostRequest } from '@/types/api'
import { Plus, ChatDotRound, Star, View, Delete, Edit, Search } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const router = useRouter()

const posts = ref<CommunityPostVO[]>([])
const loading = ref(false)
const page = ref(0)
const size = 10
const total = ref(0)
const searchDestination = ref('')

const showDialog = ref(false)
const submitting = ref(false)
const form = ref<CreatePostRequest>({
  title: '',
  content: '',
  destination: '',
  tags: [],
})

const activeTab = ref<'latest' | 'hot' | 'mine' | 'favorites'>('latest')

onMounted(() => {
  loadPosts()
})

async function loadPosts() {
  loading.value = true
  try {
    if (activeTab.value === 'latest') {
      const res = await communityApi.listPosts(page.value, size)
      posts.value = res.data.content
      total.value = res.data.total
    } else if (activeTab.value === 'hot') {
      const res = await communityApi.listHot(20)
      posts.value = res.data
      total.value = res.data.length
    } else if (activeTab.value === 'mine') {
      const res = await communityApi.listMine()
      posts.value = res.data
      total.value = res.data.length
    } else if (activeTab.value === 'favorites') {
      const res = await communityApi.listFavorites()
      posts.value = res.data
      total.value = res.data.length
    }
  } finally {
    loading.value = false
  }
}

async function searchByDestination() {
  if (!searchDestination.value.trim()) {
    await loadPosts()
    return
  }
  loading.value = true
  try {
    const res = await communityApi.listByDestination(searchDestination.value.trim(), page.value, size)
    posts.value = res.data
    total.value = res.data.length
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.value = {
    title: '',
    content: '',
    destination: '',
    tags: [],
  }
  showDialog.value = true
}

async function submit() {
  if (!form.value.title?.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  if (!form.value.content?.trim()) {
    ElMessage.warning('请输入内容')
    return
  }
  submitting.value = true
  try {
    await communityApi.createPost({
      title: form.value.title.trim(),
      content: form.value.content.trim(),
      destination: form.value.destination?.trim() || undefined,
      tags: form.value.tags,
    })
    ElMessage.success('发布成功')
    showDialog.value = false
    if (activeTab.value !== 'mine') {
      activeTab.value = 'latest'
    }
    await loadPosts()
  } catch {
  } finally {
    submitting.value = false
  }
}

async function toggleLike(post: CommunityPostVO) {
  try {
    const res = await communityApi.toggleLike(post.id)
    post.likedByMe = res.data.liked
    post.likeCount += res.data.liked ? 1 : -1
    behaviorApi.track({
      behaviorType: 'LIKE',
      targetType: 'COMMUNITY_POST',
      targetId: post.id,
    }).catch(() => {})
  } catch {
  }
}

async function toggleFavorite(post: CommunityPostVO) {
  try {
    const res = await communityApi.toggleFavorite(post.id)
    post.favoritedByMe = res.data.favorited
    post.favoriteCount += res.data.favorited ? 1 : -1
    behaviorApi.track({
      behaviorType: 'FAVORITE',
      targetType: 'COMMUNITY_POST',
      targetId: post.id,
    }).catch(() => {})
  } catch {
  }
}

async function handleDelete(post: CommunityPostVO) {
  try {
    await ElMessageBox.confirm(
      `确定删除帖子「${post.title}」吗？`,
      '确认删除',
      { type: 'warning' },
    )
    await communityApi.deletePost(post.id)
    ElMessage.success('已删除')
    await loadPosts()
  } catch {
  }
}

function viewPost(post: CommunityPostVO) {
  behaviorApi.track({
    behaviorType: 'VIEW',
    targetType: 'COMMUNITY_POST',
    targetId: post.id,
  }).catch(() => {})
  router.push(`/community/${post.id}`)
}

const tabs = [
  { value: 'latest', label: '最新' },
  { value: 'hot', label: '热门' },
  { value: 'mine', label: '我的' },
  { value: 'favorites', label: '我的收藏' },
] as const

function formatDate(date: string) {
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h1 class="page-title">旅行社区</h1>
        <p class="page-subtitle">分享你的旅行故事，发现世界的精彩</p>
      </div>
      <el-button type="primary" :icon="Plus" class="create-btn" @click="openCreate">
        发布游记
      </el-button>
    </div>

    <div class="toolbar">
      <div class="tabs">
        <div
          v-for="tab in tabs"
          :key="tab.value"
          class="tab-item"
          :class="{ active: activeTab === tab.value }"
          @click="activeTab = tab.value; page = 0; loadPosts()"
        >
          {{ tab.label }}
        </div>
      </div>
      <div class="search-box">
        <el-input
          v-model="searchDestination"
          placeholder="按目的地搜索"
          clearable
          @keyup.enter="searchByDestination"
          class="search-input"
        >
          <template #append>
            <el-button :icon="Search" class="search-btn" @click="searchByDestination" />
          </template>
        </el-input>
      </div>
    </div>

    <div v-loading="loading">
      <div v-if="posts.length === 0" class="empty-state">
        <div class="empty-icon-wrapper">
          <Edit :size="48" color="var(--text-muted)" />
        </div>
        <div class="empty-title">还没有游记</div>
        <div class="empty-desc">分享你的旅行故事，成为社区的第一位创作者</div>
        <el-button type="primary" :icon="Plus" class="empty-btn" @click="openCreate">发布第一篇游记</el-button>
      </div>

      <div v-else class="post-list">
        <div v-for="post in posts" :key="post.id" class="post-card">
          <div class="post-main" @click="viewPost(post)">
            <div class="post-header">
              <span class="post-title">{{ post.title }}</span>
              <el-tag v-if="post.destination" effect="plain" size="small" class="dest-tag">
                {{ post.destination }}
              </el-tag>
            </div>
            <div class="post-content">{{ post.content }}</div>
            <div v-if="post.tags && post.tags.length > 0" class="post-tags">
              <el-tag
                v-for="tag in post.tags"
                :key="tag"
                size="small"
                effect="plain"
                class="tag-item"
              >
                {{ tag }}
              </el-tag>
            </div>
            <div class="post-meta">
              <span class="author">{{ post.authorEmail }}</span>
              <span>·</span>
              <span>{{ formatDate(post.createdAt) }}</span>
            </div>
          </div>

          <div class="post-actions" @click.stop>
            <el-button
              :type="post.likedByMe ? 'primary' : ''"
              :icon="ChatDotRound"
              link
              size="small"
              class="action-btn"
              @click="toggleLike(post)"
            >
              {{ post.likeCount }}
            </el-button>
            <el-button
              :type="post.favoritedByMe ? 'warning' : ''"
              :icon="Star"
              link
              size="small"
              class="action-btn"
              @click="toggleFavorite(post)"
            >
              {{ post.favoriteCount }}
            </el-button>
            <el-button :icon="View" link size="small" class="action-btn">
              {{ post.viewCount }}
            </el-button>
            <el-button
              v-if="post.status === 'PUBLISHED'"
              :icon="Edit"
              link
              size="small"
              class="action-btn"
              @click="viewPost(post)"
            >
              评论
            </el-button>
            <el-button
              v-if="post.status === 'PUBLISHED'"
              :icon="Delete"
              link
              type="danger"
              size="small"
              class="action-btn"
              @click="handleDelete(post)"
            >
              删除
            </el-button>
          </div>
        </div>
      </div>

      <div v-if="activeTab === 'latest' && total > size" class="pagination">
        <el-pagination
          :current-page="page + 1"
          :page-size="size"
          :total="total"
          layout="prev, pager, next"
          class="el-pagination"
          @current-change="(p: number) => { page = p - 1; loadPosts() }"
        />
      </div>
    </div>

    <el-dialog v-model="showDialog" title="发布游记" width="600px" class="dark-dialog">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题" required>
          <el-input
            v-model="form.title"
            placeholder="给你的游记起个标题"
            maxlength="100"
            show-word-limit
            class="dialog-input"
          />
        </el-form-item>
        <el-form-item label="目的地">
          <el-input v-model="form.destination" placeholder="例如：东京" maxlength="50" class="dialog-input" />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="6"
            placeholder="分享你的旅行故事、攻略或心得..."
            maxlength="5000"
            show-word-limit
            class="dialog-input"
          />
        </el-form-item>
        <el-form-item label="标签">
          <el-select
            v-model="form.tags"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="添加标签，回车确认"
            style="width: 100%;"
            class="dialog-select"
          >
            <el-option label="美食" value="美食" />
            <el-option label="景点" value="景点" />
            <el-option label="住宿" value="住宿" />
            <el-option label="交通" value="交通" />
            <el-option label="购物" value="购物" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">发布</el-button>
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

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.tabs {
  display: flex;
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

.search-box {
  width: 280px;
}

:deep(.search-input .el-input__inner) {
  background: var(--input-bg);
  border: 1px solid var(--border-color-light);
  border-radius: 12px;
  color: var(--text-primary);
}

:deep(.search-input .el-input__inner::placeholder) {
  color: var(--text-secondary);
}

.search-btn {
  background: rgba(var(--primary-rgb), 0.2);
  border: none;
  color: var(--primary-color);
}

.search-btn:hover {
  background: rgba(var(--primary-rgb), 0.3);
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.post-card {
  background: var(--input-bg);
  border-radius: 16px;
  padding: 20px;
  border: 1px solid var(--hover-bg);
  transition: all 0.3s;
}

.post-card:hover {
  transform: translateY(-2px);
  border-color: rgba(var(--primary-rgb), 0.3);
}

.post-main {
  cursor: pointer;
}

.post-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.post-title {
  font-size: 17px;
  font-weight: 600;
  color: var(--text-primary);
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dest-tag {
  background: rgba(var(--primary-rgb), 0.15);
  border-color: rgba(var(--primary-rgb), 0.3);
  color: var(--primary-light);
}

.post-content {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.7;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.tag-item {
  background: var(--border-color);
  border-color: var(--border-color-light);
  color: var(--text-secondary);
}

.post-meta {
  font-size: 12px;
  color: var(--text-muted);
  display: flex;
  gap: 8px;
  align-items: center;
}

.author {
  color: var(--primary-color);
}

.post-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  padding-top: 12px;
  margin-top: 12px;
  border-top: 1px solid var(--hover-bg);
}

.action-btn {
  color: var(--text-muted);
  font-size: 13px;
}

.action-btn:hover {
  color: var(--text-secondary);
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
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-box {
    width: 100%;
  }

  .post-card {
    padding: 16px;
  }
}
</style>