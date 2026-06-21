<template>
  <div class="detail-page">
    <!-- 加载中状态 -->
    <div v-if="loading" class="state-box">
      <el-icon class="spin" :size="32" color="#ff2442"><Loading /></el-icon>
      <p>加载中...</p>
    </div>

    <!-- 帖子不存在状态 -->
    <div v-else-if="!post" class="state-box">
      <el-empty description="帖子不存在或已被删除" />
    </div>

    <!-- 帖子详情主体 -->
    <div v-if="!loading && post" class="detail-container">
      <!-- 左侧：媒体区域（图片/视频） -->
      <div class="media-section">
        <!-- 视频类型 -->
        <div v-if="post.type === 2 && post.videoUrl" class="video-wrapper">
          <video
            :src="post.videoUrl"
            class="detail-video"
            controls
            autoplay
            playsinline
          >
            您的浏览器不支持视频播放
          </video>
        </div>

        <!-- 多图轮播 -->
        <el-carousel
          v-else-if="post.images && post.images.length > 0"
          height="100%"
          indicator-position="none"
        >
          <el-carousel-item v-for="img in post.images" :key="img.id">
            <img :src="img.url" class="detail-image" alt="帖子图片" />
          </el-carousel-item>
        </el-carousel>

        <!-- 单张封面图 -->
        <div v-else-if="post.coverUrl" class="single-image">
          <el-image :src="post.coverUrl" fit="contain" class="detail-image" />
        </div>
      </div>

      <!-- 右侧：信息区域 -->
      <div class="info-section">
        <!-- 用户信息栏 -->
        <div class="user-bar">
          <el-avatar :size="44" :src="post.avatarUrl" style="cursor:pointer" @click="goUserProfile">
            {{ post.username?.charAt(0)?.toUpperCase() }}
          </el-avatar>
          <div class="user-meta" style="cursor:pointer" @click="goUserProfile">
            <span class="user-name">{{ post.username }}</span>
            <span class="post-time">{{ formatTime(post.createdAt) }}</span>
          </div>
          <el-button class="follow-btn" round size="small" @click="toggleFollow">
            {{ isFollowed ? '已关注' : '关注' }}
          </el-button>
        </div>

        <!-- 帖子标题 -->
        <h2 class="post-title">{{ post.title }}</h2>

        <!-- 帖子正文 -->
        <p class="post-content">{{ post.content }}</p>

        <!-- 标签列表 -->
        <div v-if="post.tags && post.tags.length > 0" class="tag-list">
          <el-tag v-for="tag in post.tags" :key="tag.id" size="small" class="tag-item">
            #{{ tag.name }}
          </el-tag>
        </div>

        <!-- 发布地点 -->
        <div v-if="post.location" class="location">
          <el-icon><Location /></el-icon>
          <span>{{ post.location }}</span>
        </div>

        <!-- 互动统计栏 -->
        <div class="stats-bar">
          <span
            class="stat-action"
            :class="{liked: isLiked, disabled: liking}"
            @click="toggleLike"
          >
            {{ isLiked ? '❤️' : '🤍' }} {{ post.likeCount }}
          </span>
          <span class="stat-action">
            <el-icon><ChatDotRound /></el-icon> {{ post.commentCount }}
          </span>
          <span
            class="stat-action"
            :class="{collected: isCollected}"
            @click="toggleCollect"
          >
            {{ isCollected ? '⭐️' : '☆' }} {{ post.collectCount }}
          </span>
          <!-- 新增：帖子举报按钮 -->
          <span class="stat-action report-btn" @click="handlePostReport">
            <el-icon><Warning /></el-icon> 举报
          </span>
        </div>

        <!-- 评论区域 -->
        <div class="comment-section">
          <h3>评论 ({{ post.commentCount }})</h3>

          <!-- 评论输入框 -->
          <div class="comment-input-row">
            <el-input
              v-model="newComment"
              :placeholder="replyTo ? `回复 @${replyTo.username}` : '说点什么...'"
              size="large"
              @keyup.enter="submitComment"
            >
              <template v-if="replyTo" #prefix>
                <el-tag closable size="small" @close="cancelReply" style="margin-right: 4px">
                  回复 @{{ replyTo.username }}
                </el-tag>
              </template>
            </el-input>
            <el-button type="primary" :loading="commenting" @click="submitComment">
              发送
            </el-button>
          </div>

          <!-- 空评论状态 -->
          <div v-if="comments.length === 0" class="no-comment">
            暂无评论，快来抢沙发吧~
          </div>

          <!-- 一级评论列表 -->
          <div v-for="c in comments" :key="c.id" class="comment-item">
            <el-avatar :size="32" :src="c.avatarUrl">
              {{ c.username?.charAt(0)?.toUpperCase() }}
            </el-avatar>

            <div class="comment-body">
              <!-- 评论发布者 -->
              <div class="comment-user">{{ c.username }}</div>
              <!-- 评论内容 -->
              <div class="comment-text">{{ c.content }}</div>
              <!-- 评论底部操作栏 -->
              <div class="comment-footer">
                <span class="comment-time">{{ formatTime(c.createdAt) }}</span>
                <span class="reply-btn" @click="startReply(c)">回复</span>
                <!-- 新增：一级评论点赞 -->
                <span 
                  class="stat-action comment-like" 
                  :class="{liked: likedComments[c.id]}"
                  @click="toggleCommentLike(c)"
                >
                  {{ likedComments[c.id] ? '❤️' : '🤍' }} {{ c.likeCount }}
                </span>
                <!-- 新增：一级评论举报 -->
                <span class="reply-btn report-btn" @click="handleCommentReport(c)">举报</span>
                <!-- 回复数展开按钮 -->
                <span 
                  v-if="c.replyCount > 0" 
                  class="reply-count" 
                  @click="toggleReplies(c)"
                >
                  {{ getLoading(c.id) ? '加载中...' : (getExpanded(c.id) ? '收起回复' : `${c.replyCount} 条回复`) }}
                </span>
              </div>

              <!-- 二级回复列表 -->
              <div v-if="getExpanded(c.id)" class="reply-list">
                <div v-if="getRepliesList(c.id).length === 0" class="no-reply">
                  暂无回复
                </div>
                <div v-for="r in getRepliesList(c.id)" :key="r.id" class="reply-item">
                  <el-avatar :size="24" :src="r.avatarUrl">
                    {{ r.username?.charAt(0)?.toUpperCase() }}
                  </el-avatar>
                  <div class="reply-body">
                    <div class="reply-user">{{ r.username }}</div>
                    <div class="reply-text">
                      <span v-if="r.replyToUsername" class="reply-mention">
                        @{{ r.replyToUsername }}
                      </span>
                      {{ r.content }}
                    </div>
                    <div class="reply-footer">
                      <span class="reply-time">{{ formatTime(r.createdAt) }}</span>
                      <span 
                        class="reply-btn-sm" 
                        @click="startReply({ id: c.id, username: r.username, userId: r.userId })"
                      >
                        回复
                      </span>
                      <!-- 新增：二级回复点赞 -->
                      <span 
                        class="reply-btn-sm comment-like" 
                        :class="{liked: likedComments[r.id]}"
                        @click="toggleCommentLike(r)"
                      >
                        {{ likedComments[r.id] ? '❤️' : '🤍' }} {{ r.likeCount }}
                      </span>
                      <!-- 新增：二级回复举报 -->
                      <span class="reply-btn-sm report-btn" @click="handleCommentReport(r)">举报</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 收藏夹选择弹窗 -->
    <el-dialog v-model="showColPicker" title="收藏到..." width="380px" top="28vh">
      <div class="col-picker-list">
        <div
          v-for="col in userCollections"
          :key="col.id"
          class="col-picker-item"
          @click="doCollect(col.id)"
        >
          <span class="col-picker-icon">📁</span>
          <div class="col-picker-info">
            <span class="col-picker-name">{{ col.name }}</span>
            <span class="col-picker-meta">
              {{ col.postCount || 0 }} 篇
              <span v-if="col.isPublic === 0" class="col-badge private">私密</span>
              <span v-else class="col-badge public">公开</span>
            </span>
          </div>
          <span class="col-picker-arrow">›</span>
        </div>
      </div>
      <div class="col-picker-footer">
        <el-button text type="primary" size="small" @click="goCreateCollection">
          ＋ 新建收藏夹
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
// Element Plus 图标
import { Loading, Location, ChatDotRound, Warning } from '@element-plus/icons-vue'
// Pinia 用户状态
import { useUserStore } from '@/stores/user'
// API 接口
import { postApi, userApi, collectionApi, notificationApi } from '@/api'


// ========== 通知发送辅助函数 ==========
const sendNotification = async (type, recipientId, targetId, targetType) => {
  // 不给自己发通知
  if (!recipientId || recipientId === store.userId) return
  try {
    await notificationApi.createNotification({ type, recipientId, targetId, targetType })
  } catch (e) {
    console.warn('创建通知失败:', e.message)
  }
}

// ========== 基础实例 ==========
const route = useRoute()
const router = useRouter()
const store = useUserStore()

// ========== 页面状态 ==========
const loading = ref(true)       // 页面整体加载状态
const post = ref(null)           // 帖子详情数据
const isLiked = ref(false)       // 帖子点赞状态
const isCollected = ref(false)   // 帖子收藏状态
const liking = ref(false)        // 帖子点赞加载锁

// ========== 评论相关状态 ==========
const comments = ref([])         // 一级评论列表
const newComment = ref('')       // 输入框内容
const replyTo = ref(null)        // 当前回复目标对象
const commenting = ref(false)     // 评论提交锁

// ========== 新增：评论点赞状态 ==========
const likedComments = ref({})    // 评论点赞状态缓存 {评论id: true/false}
const commentLiking = ref({})    // 评论点赞加载锁 {评论id: true/false}

// ========== 二级评论状态缓存 ==========
const expandedReplies = ref({})  // 展开状态：{ 一级评论id: true/false }
const repliesMap = ref({})       // 回复数据：{ 一级评论id: [二级回复数组] }
const loadingReplies = ref({})   // 加载状态：{ 一级评论id: true/false }

// ========== 收藏夹相关 ==========
const showColPicker = ref(false)
const userCollections = ref([])
// ========== 新增：关注相关状态 ==========
const isFollowed = ref(false)    // 是否已关注该作者
const followLoading = ref(false)  // 关注操作加载锁，防止重复点击

// ================================================================
// 工具函数
// ================================================================

/**
 * 获取指定一级评论的二级回复加载状态
 */
const getLoading = (parentId) => loadingReplies.value[parentId] ?? false

/**
 * 获取指定一级评论的二级回复展开状态
 */
const getExpanded = (parentId) => expandedReplies.value[parentId] ?? false

/**
 * 获取指定一级评论的二级回复列表
 */
const getRepliesList = (parentId) => repliesMap.value[parentId] || []

/**
 * 清空所有二级评论缓存
 */
const clearReplyCache = () => {
  expandedReplies.value = {}
  repliesMap.value = {}
  loadingReplies.value = {}
}

/**
 * 格式化时间
 */
const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

// ================================================================
// 新增：评论点赞本地缓存工具
// ================================================================
const getCommentStorageKey = () => {
  const username = store.username || 'anonymous'
  return `liked_comments_${username}`
}

const loadLikedComments = () => {
  try {
    const raw = localStorage.getItem(getCommentStorageKey())
    return new Set(raw ? JSON.parse(raw) : [])
  } catch {
    return new Set()
  }
}

const saveLikedComments = (set) => {
  try {
    localStorage.setItem(getCommentStorageKey(), JSON.stringify([...set]))
  } catch {}
}

// ================================================================
// 帖子点赞/收藏本地缓存工具
// ================================================================
const getStorageKey = () => {
  const username = store.username || 'anonymous'
  return `liked_posts_${username}`
}

const getCollectStorageKey = () => {
  const username = store.username || 'anonymous'
  return `collected_posts_${username}`
}

const loadLikedPosts = () => {
  try {
    const raw = localStorage.getItem(getStorageKey())
    return new Set(raw ? JSON.parse(raw) : [])
  } catch {
    return new Set()
  }
}

const saveLikedPosts = (set) => {
  try {
    localStorage.setItem(getStorageKey(), JSON.stringify([...set]))
  } catch {}
}

const loadCollectedPosts = () => {
  try {
    const raw = localStorage.getItem(getCollectStorageKey())
    return new Set(raw ? JSON.parse(raw) : [])
  } catch {
    return new Set()
  }
}

const saveCollectedPosts = (set) => {
  try {
    localStorage.setItem(getCollectStorageKey(), JSON.stringify([...set]))
  } catch {}
}

// ================================================================
// 核心业务方法
// ================================================================

// ========== 新增：关注/取消关注核心方法 ==========
function goUserProfile() {
  const authorId = post.value?.userId
  if (!authorId) return
  router.push({ path: '/profile', query: { userId: authorId } })
}

const toggleFollow = async () => {
  // 未登录拦截
  if (!store.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  if (followLoading.value) return

  followLoading.value = true
  try {
    // 帖子作者的用户ID，从帖子详情中取
    const authorId = post.value.userId

    if (isFollowed.value) {
      // 取消关注
      await userApi.unfollow(authorId)
      isFollowed.value = false
      ElMessage.success('已取消关注')
    } else {
      // 关注
      await userApi.follow(authorId)
      isFollowed.value = true
      await sendNotification(5, authorId, authorId, 0)
      ElMessage.success('关注成功')
    }
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    followLoading.value = false
  }
}
/**
 * 获取帖子详情
 */
const fetchDetail = async () => {
  loading.value = true
  clearReplyCache()
  try {
    const res = await postApi.getPostDetail(route.params.id)
    post.value = res

    // 原有：点赞状态
    isLiked.value = res.isLiked === true || loadLikedPosts().has(String(res.id))
    // 原有：收藏状态
    isCollected.value = res.isCollected === true || res.collected === true || loadCollectedPosts().has(String(res.id))
    
    // ========== 新增：同步关注状态 ==========
    // 优先读取后端返回的 isFollowed 字段，没有则默认未关注
    isFollowed.value = res.isFollowed === true
  } catch (e) {
    post.value = null
    ElMessage.error(e.message || '加载帖子失败')
  } finally {
    loading.value = false
  }
}

/**
 * 获取一级评论列表
 */
const fetchComments = async () => {
  try {
    const res = await postApi.getComments(route.params.id, {
      page: 1,
      pageSize: 20
    })
    comments.value = res.records || res.list || res.data || res || []

    // 新增：初始化评论点赞状态
    const localLiked = loadLikedComments()
    comments.value.forEach(comment => {
      likedComments.value[comment.id] = comment.isLiked === true || localLiked.has(String(comment.id))
    })
  } catch (e) {
    console.error('加载评论失败:', e)
    comments.value = []
  }
}

/**
 * 帖子点赞/取消点赞
 */
const toggleLike = async () => {
   // 加这行调试
 console.log('【点赞】点击时 isLoggedIn：', store.isLoggedIn)
  console.log('【点赞】点击时 token：', store.token)
  console.log('【点赞】点击时 userId：', store.userId)
  console.log('【点赞】localStorage里的token：', localStorage.getItem('token'))

  if (!store.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  if (liking.value) return

  liking.value = true
  try {
    if (isLiked.value) {
      await postApi.unlikePost(post.value.id)
      post.value.likeCount = Math.max(0, post.value.likeCount - 1)
      isLiked.value = false
      const set = loadLikedPosts()
      set.delete(String(post.value.id))
      saveLikedPosts(set)
    } else {
      await postApi.likePost(post.value.id)
      post.value.likeCount++
      await sendNotification(1, post.value.userId, post.value.id, 1)
      isLiked.value = true
      const set = loadLikedPosts()
      set.add(String(post.value.id))
      saveLikedPosts(set)
    }
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    liking.value = false
  }
}

/**
 * 新增：评论点赞/取消点赞（支持一级+二级评论）
 */
const toggleCommentLike = async (comment) => {
  if (!store.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  const commentId = comment.id
  if (commentLiking.value[commentId]) return

  commentLiking.value[commentId] = true
  try {
    const isLiked = likedComments.value[commentId]
    if (isLiked) {
      // 取消点赞
      await postApi.unlikeComment(commentId)
      comment.likeCount = Math.max(0, (comment.likeCount || 0) - 1)
      likedComments.value[commentId] = false
      // 同步本地缓存
      const set = loadLikedComments()
      set.delete(String(commentId))
      saveLikedComments(set)
    } else {
      // 点赞
      await postApi.likeComment(commentId)
      comment.likeCount = (comment.likeCount || 0) + 1
      likedComments.value[commentId] = true
      await sendNotification(2, comment.userId, commentId, 2)
      // 同步本地缓存
      const set = loadLikedComments()
      set.add(String(commentId))
      saveLikedComments(set)
    }
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    commentLiking.value[commentId] = false
  }
}

/**
 * 收藏/取消收藏帖子
 */
const toggleCollect = async () => {
  if (!store.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }

  if (isCollected.value) {
    try {
      await postApi.uncollectPost(post.value.id)
      post.value.collectCount = Math.max(0, (post.value.collectCount || 0) - 1)
      isCollected.value = false
      const set = loadCollectedPosts()
      set.delete(String(post.value.id))
      saveCollectedPosts(set)
      ElMessage.success('已取消收藏')
    } catch (e) {
      ElMessage.error(e.message || '取消收藏失败')
    }
    return
  }

  try {
    const res = await collectionApi.getCollections()
    userCollections.value = res.records || res.list || res.data || res || []
    if (userCollections.value.length === 0) {
      ElMessage.warning('还没有收藏夹，请先创建')
      return
    }
    showColPicker.value = true
  } catch (e) {
    ElMessage.error('获取收藏夹失败')
  }
}

/**
 * 执行收藏操作
 */
const doCollect = async (collectionId) => {
  try {
    if (!post.value || !collectionId) {
      ElMessage.error('参数错误')
      return
    }
    await postApi.collectPost(post.value.id, collectionId)
    post.value.collectCount++
    isCollected.value = true
    showColPicker.value = false

    const set = loadCollectedPosts()
    set.add(String(post.value.id))
    saveCollectedPosts(set)

    ElMessage.success('收藏成功')
    await sendNotification(6, post.value.userId, post.value.id, 1)
  } catch (e) {
    console.error('收藏失败:', e)
    ElMessage.error(e.message || '收藏失败')
  }
}

/**
 * 跳转到新建收藏夹页面
 */
const goCreateCollection = () => {
  showColPicker.value = false
  router.push('/profile')
}

// ================================================================
// 新增：举报功能
// ================================================================
/**
 * 帖子举报（带描述输入）
 */
const handlePostReport = async () => {
  if (!store.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    const { value } = await ElMessageBox.prompt('请输入举报原因', '举报帖子', {
      confirmButtonText: '提交举报',
      cancelButtonText: '取消',
      inputPlaceholder: '请描述违规内容...',
      inputType: 'textarea',
      inputValidator: (value) => {
        if (!value || !value.trim()) return '请输入举报原因'
        if (value.trim().length > 500) return '举报原因不能超过500字'
        return true
      }
    })
    await postApi.report({
      targetType: 1, // 1=帖子
      targetId: post.value.id,
      reason: value.trim()
    })
    ElMessage.success('举报提交成功，我们会尽快处理')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '举报提交失败')
    }
  }
}

/**
 * 评论举报（无需描述）
 */
const handleCommentReport = async (comment) => {
  if (!store.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    await ElMessageBox.confirm('确定要举报该评论吗？', '举报评论', {
      confirmButtonText: '确定举报',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await postApi.report({
      targetType: 2, // 2=评论
      targetId: comment.id,
      reason: '违规评论'
    })
    ElMessage.success('举报提交成功，我们会尽快处理')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '举报提交失败')
    }
  }
}

// ================================================================
// 二级评论核心方法
// ================================================================

/**
 * 展开/收起指定一级评论的二级回复
 */
const toggleReplies = async (parentComment) => {
  const parentId = parentComment.id
  if (parentComment.replyCount <= 0) return
  if (getLoading(parentId)) return
  if (getExpanded(parentId)) {
    expandedReplies.value[parentId] = false
    return
  }

  if (!repliesMap.value[parentId]) {
    loadingReplies.value[parentId] = true
    try {
      const res = await postApi.getCommentReplies(route.params.id, parentId, {
        page: 1,
        pageSize: 50
      })
      repliesMap.value[parentId] = res.records || res.list || res.data || res || []

      // 新增：初始化二级回复点赞状态
      const localLiked = loadLikedComments()
      repliesMap.value[parentId].forEach(reply => {
        likedComments.value[reply.id] = reply.isLiked === true || localLiked.has(String(reply.id))
      })
    } catch (e) {
      ElMessage.error('加载回复失败')
      repliesMap.value[parentId] = []
    } finally {
      loadingReplies.value[parentId] = false
    }
  }

  expandedReplies.value[parentId] = true
}

/**
 * 局部刷新指定一级评论的二级回复
 */
const reloadTargetReplies = async (parentId) => {
  loadingReplies.value[parentId] = true
  try {
    const res = await postApi.getCommentReplies(route.params.id, parentId, {
      page: 1,
      pageSize: 50
    })
    repliesMap.value[parentId] = res.records || res.list || res.data || res || []
    expandedReplies.value[parentId] = true
  } catch (e) {
    ElMessage.error('刷新回复失败')
  } finally {
    loadingReplies.value[parentId] = false
  }
}

/**
 * 点击回复按钮，激活回复输入框
 */
const startReply = (comment, parentId = null) => {
  if (!store.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  replyTo.value = {
    id: parentId || comment.id,
    username: comment.username,
    userId: comment.userId
  }
  newComment.value = ''
}

/**
 * 取消回复
 */
const cancelReply = () => {
  replyTo.value = null
  newComment.value = ''
}

/**
 * 提交评论/回复
 */
const submitComment = async () => {
  const content = newComment.value.trim()
  if (!content) {
    ElMessage.warning('请输入评论内容')
    return
  }
  if (!store.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }

  commenting.value = true
  try {
    const params = {
      postId: post.value.id,
      content: content
    }

    if (replyTo.value) {
      params.parentId = replyTo.value.id
      params.replyToUserId = replyTo.value.userId
    }

    await postApi.createComment(params)
    ElMessage.success(replyTo.value ? '回复成功' : '评论成功')
    if (replyTo.value) {
      await sendNotification(4, replyTo.value.userId, post.value.id, 1)
    } else {
      await sendNotification(3, post.value.userId, post.value.id, 1)
    }

    post.value.commentCount++

    if (replyTo.value) {
      // 回复后同步更新一级评论回复数
      const parentComment = comments.value.find(item => item.id === replyTo.value.id)
      if (parentComment) {
        parentComment.replyCount = (parentComment.replyCount || 0) + 1
      }
      await reloadTargetReplies(replyTo.value.id)
    } else {
      fetchComments()
    }

    newComment.value = ''
    cancelReply()
  } catch (e) {
    ElMessage.error(e.message || '发布失败')
  } finally {
    commenting.value = false
  }
}

// ================================================================
// 生命周期 & 监听
// ================================================================

onMounted(() => {
  fetchDetail()
  fetchComments()
})

watch(() => route.params.id, (newId, oldId) => {
  if (newId && newId !== oldId) {
    fetchDetail()
    fetchComments()
  }
})

onBeforeUnmount(() => {
  clearReplyCache()
})
</script>

<style scoped>
.detail-page {
  height: calc(100vh - 40px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  box-sizing: border-box;
}

.state-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 0;
  color: var(--text-secondary, #909399);
}

.state-box p {
  margin-top: 12px;
  font-size: 14px;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 详情容器 */
.detail-container {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  width: 100%;
  height: 100%;
  max-width: 1200px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

/* 左侧媒体区 */
.media-section {
  flex: 1;
  min-width: 0;
  background: #000;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  height: 100%;
}

.video-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.detail-video {
  width: 100%;
  height: 100%;
  max-height: 100%;
  object-fit: contain;
  outline: none;
}

.detail-image {
  display: block;
  max-width: 100%;
  max-height: 100%;
  width: auto;
  height: auto;
  object-fit: contain;
}

.single-image {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.media-section :deep(.el-carousel) {
  width: 100%;
  height: 100%;
}

.media-section :deep(.el-carousel__container) {
  height: 100% !important;
}

.media-section :deep(.el-carousel__item) {
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 右侧信息区 */
.info-section {
  width: 420px;
  flex-shrink: 0;
  padding: 24px;
  overflow-y: auto;
  height: 100%;
  box-sizing: border-box;
}

/* 用户信息栏 */
.user-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.user-meta {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.user-name {
  font-weight: 600;
  font-size: 15px;
  color: #303133;
}

.post-time {
  font-size: 12px;
  color: #909399;
}

.follow-btn {
  flex-shrink: 0;
}

/* 标题正文 */
.post-title {
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 12px 0;
  color: #303133;
  line-height: 1.4;
}

.post-content {
  font-size: 15px;
  line-height: 1.8;
  color: #606266;
  white-space: pre-wrap;
  margin: 0 0 16px 0;
}

/* 标签 */
.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.tag-item {
  cursor: pointer;
}

/* 地点 */
.location {
  font-size: 13px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 16px;
}

/* 统计栏 */
.stats-bar {
  display: flex;
  gap: 24px;
  padding: 16px 0;
  border-top: 1px solid #ebeef5;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 20px;
  align-items: center;
}

.stat-action {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: #606266;
  cursor: pointer;
  transition: color 0.2s;
  user-select: none;
}

.stat-action:hover {
  color: #ff2442;
}

.stat-action.liked {
  color: #ff2442;
}

.stat-action.collected {
  color: #ffb800;
}

.stat-action.disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

/* 新增：举报按钮样式 */
.report-btn:hover {
  color: #f56c6c !important;
}

/* 评论区 */
.comment-section h3 {
  font-size: 16px;
  margin: 0 0 12px 0;
  color: #303133;
}

.comment-input-row {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
}

.no-comment {
  text-align: center;
  color: #909399;
  padding: 24px 0;
  font-size: 14px;
}

/* 一级评论项 */
.comment-item {
  display: flex;
  gap: 10px;
  padding: 12px 0;
}

.comment-item + .comment-item {
  border-top: 1px solid #f5f5f5;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-user {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 2px;
  color: #303133;
}

.comment-text {
  font-size: 14px;
  line-height: 1.5;
  color: #303133;
  word-break: break-word;
}

.comment-footer {
  display: flex;
  gap: 16px;
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
  align-items: center;
  flex-wrap: wrap;
}

.reply-btn {
  cursor: pointer;
  transition: color 0.2s;
}

.reply-btn:hover {
  color: #ff2442;
}

.reply-count {
  cursor: pointer;
  color: #409eff;
}

.reply-count:hover {
  text-decoration: underline;
}

/* 评论内点赞适配 */
.comment-like {
  font-size: inherit;
  gap: 2px;
}
.comment-like.liked {
  color: #ff2442;
}

/* 二级回复列表 */
.reply-list {
  margin-top: 10px;
  padding-left: 12px;
  border-left: 2px solid #f0f0f0;
}

.no-reply {
  font-size: 13px;
  color: #c0c4cc;
  padding: 8px 0;
}

.reply-item {
  display: flex;
  gap: 8px;
  padding: 8px 0;
}

.reply-item + .reply-item {
  border-top: 1px solid #fafafa;
}

.reply-body {
  flex: 1;
  min-width: 0;
}

.reply-user {
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 1px;
  color: #606266;
}

.reply-text {
  font-size: 13px;
  line-height: 1.5;
  color: #303133;
  word-break: break-word;
}

.reply-mention {
  color: #409eff;
  margin-right: 2px;
}

.reply-footer {
  display: flex;
  gap: 12px;
  margin-top: 2px;
  align-items: center;
}

.reply-time {
  font-size: 11px;
  color: #c0c4cc;
}

.reply-btn-sm {
  font-size: 11px;
  color: #909399;
  cursor: pointer;
  transition: color 0.2s;
}

.reply-btn-sm:hover {
  color: #ff2442;
}

/* 收藏夹选择弹窗 */
.col-picker-list {
  max-height: 320px;
  overflow-y: auto;
  padding: 0 4px;
}

.col-picker-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  margin-bottom: 8px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid #f0f0f0;
  background: #fafafa;
}

.col-picker-item:last-child {
  margin-bottom: 0;
}

.col-picker-item:hover {
  background: linear-gradient(135deg, #fff5f5, #fff0f0);
  border-color: #ffd4d4;
  transform: translateX(3px);
  box-shadow: 0 2px 8px rgba(255, 36, 66, 0.06);
}

.col-picker-icon {
  font-size: 24px;
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff5f5;
  border-radius: 10px;
}

.col-picker-info {
  flex: 1;
  min-width: 0;
}

.col-picker-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  display: block;
}

.col-picker-meta {
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 3px;
}

.col-badge {
  font-size: 10px;
  padding: 1px 7px;
  border-radius: 4px;
  font-weight: 500;
}

.col-badge.private {
  background: #f0f0f0;
  color: #606266;
}

.col-badge.public {
  background: #e8f5e9;
  color: #43a047;
}

.col-picker-arrow {
  color: #ddd;
  font-size: 20px;
  flex-shrink: 0;
  font-weight: bold;
}

.col-picker-item:hover .col-picker-arrow {
  color: #ff2442;
}

.col-picker-footer {
  border-top: 1px solid #f0f0f0;
  padding: 10px 0 4px;
  margin-top: 8px;
  text-align: center;
}
</style>
