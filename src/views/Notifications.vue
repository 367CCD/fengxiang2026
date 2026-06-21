<template>
  <div class="notifications-page">
    <div class="page-header">
      <h2>通知</h2>
      <el-button text type="primary" :disabled="!hasUnread" @click="markAllRead">
        全部标为已读
      </el-button>
    </div>

    <div v-if="loading" class="state-box">
      <el-icon class="spin" :size="28" color="#ff2442"><Loading /></el-icon>
      <p>加载中...</p>
    </div>

    <div v-else-if="list.length === 0" class="state-box">
      <el-empty description="暂无通知" :image-size="120" />
    </div>

    <div v-else class="notification-list">
      <div
        v-for="item in list"
        :key="item.id"
        class="notification-item"
        :class="{ unread: !item.isRead }"
        @click="handleClick(item)"
      >
        <div class="notif-dot" v-if="!item.isRead" />
        <!-- 系统通知 → 显示 app 图标；其他通知 → 显示发送者头像 -->
        <div class="notif-avatar-box" v-if="item.type === 7">
          <div class="system-avatar">📖</div>
        </div>
        <el-avatar v-else :size="44" :src="item.senderAvatar || undefined">
          {{ item.senderName?.charAt(0)?.toUpperCase() || '?' }}
        </el-avatar>
        <div class="notif-body">
          <div class="notif-text">
            <span v-if="item.type === 1"><strong>{{ item.senderName }}</strong> 赞了你的笔记</span>
            <span v-else-if="item.type === 2"><strong>{{ item.senderName }}</strong> 赞了你的评论</span>
            <span v-else-if="item.type === 3"><strong>{{ item.senderName }}</strong> 评论了你的笔记</span>
            <span v-else-if="item.type === 4"><strong>{{ item.senderName }}</strong> 回复了你的评论</span>
            <span v-else-if="item.type === 5"><strong>{{ item.senderName }}</strong> 关注了你</span>
            <span v-else-if="item.type === 6"><strong>{{ item.senderName }}</strong> 收藏了你的笔记</span>
            <span v-else-if="item.type === 7"><strong>系统通知</strong> · {{ item.content || '' }}</span>
            <span v-else>{{ item.content }}</span>
          </div>
          <div class="notif-time">{{ formatTime(item.createdAt) }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { notificationApi } from '../api'
import { useUserStore } from '../stores/user'

const store = useUserStore()
const router = useRouter()

const list = ref([])
const loading = ref(true)

const hasUnread = computed(() => list.value.some(n => !n.isRead))

async function fetchNotifications() {
  loading.value = true
  try {
    const res = await notificationApi.getNotifications({ page: 1, pageSize: 50 })
    list.value = res.records || res.list || res.data || res || []
  } catch (_) {
    list.value = []
  } finally {
    loading.value = false
  }
}

async function markAllRead() {
  try {
    await notificationApi.markAllAsRead()
    list.value.forEach(n => (n.isRead = 1))
    store.unreadCount = 0
    ElMessage.success('已全部标为已读')
  } catch (_) {}
}

function handleClick(item) {
  if (!item.isRead) {
    notificationApi.markAsRead(item.id).catch(() => {})
    item.isRead = 1
    if (store.unreadCount > 0) store.unreadCount--
  }
  // 根据类型跳转
  if (item.type === 5) {
    // 关注通知 → 跳转到关注者主页
    router.push({ path: '/profile', query: { userId: item.senderId } })
  } else if ([1, 2, 3, 4, 6].includes(item.type)) {
    // 点赞/评论/收藏 → 跳转到帖子详情
    if (item.targetId) {
      router.push('/post/' + item.targetId)
    }
  } else if (item.type === 7) {
    // 系统通知 → 根据 targetType 跳转
    if (item.targetType === 1 && item.targetId) {
      router.push('/post/' + item.targetId)       // 帖子相关 → 帖子详情
    } else if (item.targetType === 2 && item.targetId) {
      router.push('/post/' + item.targetId)       // 评论相关 → 帖子详情（查看评论上下文）
    }
  }
}

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 259200000) return Math.floor(diff / 86400000) + '天前'
  return `${d.getMonth() + 1}/${d.getDate()}`
}

onMounted(() => {
  fetchNotifications()
  store.fetchUnreadCount()
})
</script>

<style scoped>
.notifications-page {
  max-width: 680px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  font-size: 22px;
  font-weight: 600;
}

.state-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 0;
  color: var(--text-secondary);
}

.state-box p { margin-top: 12px; }

.spin { animation: spin 1s linear infinite; }

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.notification-list {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px 20px;
  cursor: pointer;
  transition: background 0.15s;
  position: relative;
}

.notification-item:hover {
  background: #fafafa;
}

.notification-item + .notification-item {
  border-top: 1px solid var(--border-color);
}

.notif-dot {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--primary-color);
}

.notification-item.unread {
  background: #fff8f8;
}

.notif-body {
  flex: 1;
  min-width: 0;
}

.notif-text {
  font-size: 14px;
  line-height: 1.5;
  color: #333;
}

.notif-text strong {
  font-weight: 600;
}

.notif-time {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.notif-avatar-box {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
}

.system-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff2442 0%, #ff6b81 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}
</style>
