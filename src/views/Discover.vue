<template>
  <div class="discover-page">
    <!-- 主 Tab：推荐 | 关注 -->
    <div class="discover-tabs">
      <el-tabs v-model="feedTab" @tab-change="onFeedTabChange">
        <el-tab-pane label="发现" name="recommend" />
        <el-tab-pane name="follow">
          <template #label>
            <el-badge :value="followNewCount" :hidden="followNewCount <= 0" class="follow-badge">
              关注
            </el-badge>
          </template>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- ══════ 推荐流 ══════ -->
    <div v-if="feedTab === 'recommend'">
      <div class="discover-header">
        <h2>{{ collectionName || "发现" }}</h2>
        <div class="filters">
          <el-radio-group v-model="activeTab" size="small">
            <el-radio-button value="latest">最新</el-radio-button>
            <el-radio-button value="hot">热门</el-radio-button>
          </el-radio-group>
          <el-select v-model="tagFilter" placeholder="选择标签" clearable size="small" style="width: 120px">
            <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" />
          </el-select>
        </div>
      </div>

      <!-- 搜索历史区域 -->
      <div v-if="!route.query.q && searchHistory.length > 0" class="history-section">
        <div class="history-header">
          <span class="history-title">搜索历史</span>
          <span class="clear-btn" @click="clearSearchHistory">清空</span>
        </div>
        <div class="history-tags">
          <el-tag
            v-for="(item, index) in searchHistory"
            :key="index"
            size="small"
            class="history-tag"
            @click="handleHistoryClick(item.keyword || item)"
          >
            {{ item.keyword || item }}
          </el-tag>
        </div>
      </div>

      <div v-if="loading" class="state-box">
        <el-icon class="spin" :size="32" color="#ff2442"><Loading /></el-icon>
        <p>正在加载...</p>
      </div>

      <div v-else-if="posts.length === 0" class="state-box">
        <el-empty description="暂时没有内容" />
      </div>

      <Waterfall v-else :posts="posts" :columns="4" :gap="16" />

      <div v-if="loadingMore" class="load-more-hint">
        <el-icon class="spin"><Loading /></el-icon> 加载中...
      </div>
      <div v-if="!hasMore && posts.length > 0" class="load-more-hint">已经到底了</div>
    </div>

    <!-- ══════ 关注流 ══════ -->
    <div v-if="feedTab === 'follow'">
      <div v-if="!store.isLoggedIn" class="state-box">
        <el-empty description="登录后查看关注的创作者" :image-size="100">
          <template #extra>
            <el-button type="primary" size="small" @click="router.push('/login')">去登录</el-button>
          </template>
        </el-empty>
      </div>
      <div v-else-if="followLoading && followPosts.length === 0" class="state-box">
        <el-icon class="spin" :size="32" color="#ff2442"><Loading /></el-icon>
        <p>加载关注动态...</p>
      </div>
      <div v-else-if="followPosts.length === 0" class="state-box">
        <el-empty description="还没有关注创作者" :image-size="100">
          <template #extra>
            <el-button type="primary" size="small" @click="feedTab = 'recommend'">去发现更多创作者</el-button>
          </template>
        </el-empty>
      </div>
      <Waterfall v-else :posts="followPosts" :columns="4" :gap="16" />

      <div v-if="followLoadingMore" class="load-more-hint">
        <el-icon class="spin"><Loading /></el-icon> 加载中...
      </div>
      <div v-if="!followHasMore && followPosts.length > 0" class="load-more-hint">已经到底了</div>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import { postApi, tagApi, collectionApi, searchApi } from '../api'
import Waterfall from '../components/Waterfall.vue'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

// ==================== 通用工具：统一解析分页列表数据 ====================
// 兼容后端所有返回格式：res / res.list / res.data / res.records / res.data.records
const resolveList = (res) => {
  if (!res) return []
  if (Array.isArray(res)) return res
  if (Array.isArray(res.records)) return res.records
  if (Array.isArray(res.list)) return res.list
  if (res.data) {
    if (Array.isArray(res.data)) return res.data
    if (Array.isArray(res.data.records)) return res.data.records
    if (Array.isArray(res.data.list)) return res.data.list
  }
  return []
}

// 基础状态
const posts = ref([])
const tags = ref([])
const loading = ref(true)
const loadingMore = ref(false)
const hasMore = ref(true)
const page = ref(1)
const activeTab = ref('latest')
const tagFilter = ref(null)
const collectionName = ref('')

// 搜索历史状态
const searchHistory = ref([])

// ==================== 关注流状态 ====================
const feedTab = ref('recommend')
const followPosts = ref([])
const followLoading = ref(false)
const followLoadingMore = ref(false)
const followHasMore = ref(true)
const followPage = ref(1)
const followNewCount = ref(0)
const followLastViewTime = ref(null)
let followPollTimer = null

// Tab 切换事件（修正：事件触发后加载对应数据）
async function onFeedTabChange(tabName) {
  if (tabName === 'follow') {
    followNewCount.value = 0
    await fetchFollowFeed(true)
    followLastViewTime.value = new Date().toISOString().slice(0, 19).replace('T', ' ')
  } else if (tabName === 'recommend') {
    fetchPosts(true)
  }
}

/**
 * 获取关注流帖子
 */
async function fetchFollowFeed(reset = false) {
  if (!store.isLoggedIn) return
  if (reset) {
    followPage.value = 1
    followPosts.value = []
    followHasMore.value = true
  }
  if (!followHasMore.value && !reset) return

  reset ? (followLoading.value = true) : (followLoadingMore.value = true)

  try {
    const res = await postApi.getFollowFeed({
      page: followPage.value,
      pageSize: 20
    })
    const records = resolveList(res)
    followPosts.value = reset ? records : [...followPosts.value, ...records]
    followHasMore.value = records.length >= 20
    followPage.value++
  } catch (e) {
    console.error('获取关注流失败:', e)
  } finally {
    followLoading.value = false
    followLoadingMore.value = false
  }
}

/**
 * 拉取关注流新内容数量
 */
async function fetchFollowNewCount() {
  if (!store.isLoggedIn || feedTab.value === 'follow') return
  try {
    const res = await postApi.getFollowNewCount({
      lastTime: followLastViewTime.value || undefined
    })
    followNewCount.value = typeof res === 'number' ? res : (res.count || res.data || 0)
  } catch {
    followNewCount.value = 0
  }
}

/**
 * 定时轮询新内容提醒
 */
function startFollowPoll() {
  stopFollowPoll()
  followPollTimer = setInterval(fetchFollowNewCount, 5 * 60 * 1000)
  fetchFollowNewCount()
}

function stopFollowPoll() {
  if (followPollTimer) {
    clearInterval(followPollTimer)
    followPollTimer = null
  }
}

/**
 * 获取帖子列表（推荐流）
 */
async function fetchPosts(reset = false) {
  if (reset) {
    page.value = 1
    posts.value = []
    hasMore.value = true
  }
  if (!hasMore.value && !reset) return

  reset ? (loading.value = true) : (loadingMore.value = true)

  try {
    const params = {
      page: page.value,
      pageSize: 20,
      sort: activeTab.value === 'hot' ? 'hot' : 'latest'
    }
    if (tagFilter.value) params.tagId = tagFilter.value
    if (route.query.q) params.q = route.query.q

    const collectionId = route.query.collectionId
    let res

    if (collectionId) {
      res = await collectionApi.getCollectionPosts(collectionId, params)
    } else if (route.query.q) {
      res = await searchApi.search(params)
    } else {
      res = await postApi.getFeed(params)
    }

    const list = resolveList(res)
    posts.value = reset ? list : [...posts.value, ...list]
    hasMore.value = list.length >= 20
    page.value++
  } catch (e) {
    console.error('Failed to load posts:', e)
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

/**
 * 获取标签列表
 */
async function fetchTags() {
  try {
    const res = await tagApi.getTags()
    tags.value = resolveList(res)
  } catch (e) {
    console.error('Failed to load tags:', e)
  }
}

/**
 * 获取搜索历史记录
 */
async function fetchSearchHistory() {
  try {
    const res = await searchApi.getHistory()
    searchHistory.value = resolveList(res)
  } catch (e) {
    console.error('Failed to load search history:', e)
  }
}

/**
 * 清空搜索历史
 */
async function clearSearchHistory() {
  try {
    await searchApi.clearHistory()
    searchHistory.value = []
    ElMessage.success('已清空历史记录')
  } catch (e) {
    console.error('Failed to clear history:', e)
    ElMessage.error('清空失败')
  }
}

/**
 * 点击历史词触发搜索
 */
function handleHistoryClick(keyword) {
  if (!keyword) return
  router.push({
    query: { ...route.query, q: keyword }
  })
}

// 监听筛选条件变化
watch(activeTab, () => { if (feedTab.value === 'recommend') fetchPosts(true) })
watch(tagFilter, () => { if (feedTab.value === 'recommend') fetchPosts(true) })

// 监听搜索关键词变化
watch(() => route.query.q, () => {
  if (feedTab.value === 'recommend') {
    fetchPosts(true)
    if (route.query.q) {
      fetchSearchHistory()
    }
  }
})

// 监听收藏夹ID变化
watch(() => route.query.collectionId, () => {
  collectionName.value = route.query.collectionName || ""
  if (feedTab.value === 'recommend') fetchPosts(true)
})

// 滚动加载更多
function onScroll() {
  const el = document.documentElement
  if (el.scrollHeight - el.scrollTop - el.clientHeight < 300) {
    if (feedTab.value === 'follow') {
      fetchFollowFeed(false)
    } else {
      fetchPosts(false)
    }
  }
}

onMounted(() => {
  fetchTags()
  fetchPosts(true)
  fetchSearchHistory()
  window.addEventListener('scroll', onScroll)
  startFollowPoll()
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
  stopFollowPoll()
})
</script>

<style scoped>
.discover-page {
  max-width: 1100px;
  margin: 0 auto;
  padding-bottom: 40px;
}

.discover-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.discover-header h2 {
  font-size: 22px;
  font-weight: 600;
}

.filters {
  display: flex;
  gap: 12px;
  align-items: center;
}

/* 搜索历史样式 */
.history-section {
  margin-bottom: 20px;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.history-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.clear-btn {
  font-size: 12px;
  color: #909399;
  cursor: pointer;
  transition: color 0.2s;
}

.clear-btn:hover {
  color: #ff2442;
}

.history-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.history-tag {
  cursor: pointer;
  transition: all 0.2s;
}

.history-tag:hover {
  background: #ff2442;
  color: #fff;
  border-color: #ff2442;
}

.state-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  color: var(--text-secondary);
}

.state-box p { margin-top: 12px; }

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.load-more-hint {
  text-align: center;
  padding: 24px;
  color: var(--text-secondary);
  font-size: 14px;
}
</style>