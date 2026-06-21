<template>
  <div class="user-list-page">
    <div class="page-header">
      <el-button text @click="router.back()">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <h2>{{ isFollowers ? "粉丝" : "关注" }}</h2>
    </div>

    <div v-if="loading && list.length === 0" class="state-box">
      <el-icon class="spin" :size="28" color="#ff2442"><Loading /></el-icon>
      <p>加载中...</p>
    </div>

    <div v-else-if="list.length === 0" class="state-box">
      <el-empty :description="isFollowers ? '还没有粉丝' : '还没有关注任何人'" :image-size="100" />
    </div>

    <div v-else class="user-list">
      <div v-for="u in list" :key="u.id" class="user-item" @click="goUserProfile(u.id)">
        <el-avatar :size="44" :src="u.avatarUrl || undefined">
          {{ u.username?.charAt(0)?.toUpperCase() }}
        </el-avatar>
        <div class="user-meta">
          <div class="user-name-row">
            <span class="user-name">{{ u.username }}</span>
            <span v-if="u.isMutual" class="mutual-badge">互关</span>
          </div>
          <p class="user-bio">{{ u.bio || "这个人很懒，什么都没留下~" }}</p>
          <span class="user-stats">
            {{ u.followCount || 0 }} 关注 &nbsp; {{ u.fansCount || 0 }} 粉丝
          </span>
        </div>
        <el-button
          v-if="store.isLoggedIn && u.id !== store.userId"
          :type="u.isFollowed ? 'default' : 'primary'"
          size="small"
          round
          :loading="followLoadingMap[u.id]"
          @click.stop="toggleFollowUser(u)"
        >
          {{ u.isMutual ? "互相关注" : u.isFollowed ? "已关注" : "关注" }}
        </el-button>
      </div>
    </div>

    <div v-if="loadingMore" class="load-more-hint">
      <el-icon class="spin"><Loading /></el-icon> 加载中...
    </div>
    <div v-if="!hasMore && list.length > 0" class="load-more-hint">已经到底了</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from "vue"
import { useRoute, useRouter } from "vue-router"
import { ElMessage } from "element-plus"
import { ArrowLeft, Loading } from "@element-plus/icons-vue"
import { userApi } from "../api"
import { useUserStore } from "../stores/user"

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const isFollowers = computed(() => route.query.type === "followers")
const userId = computed(() => Number(route.query.userId) || store.userId)

const list = ref([])
const loading = ref(true)
const loadingMore = ref(false)
const hasMore = ref(true)
const page = ref(1)
const followLoadingMap = ref({})

async function fetchList(reset = false) {
  const id = userId.value
  if (!id) return
  if (reset) {
    page.value = 1
    list.value = []
    hasMore.value = true
  }
  if (!hasMore.value && !reset) return

  reset ? (loading.value = true) : (loadingMore.value = true)

  try {
    const apiFn = isFollowers.value ? userApi.getFollowers : userApi.getFollowing
    const res = await apiFn(id, { page: page.value, pageSize: 20 })
    const records = res.records || res.list || res.data || res || []
    list.value = reset ? records : [...list.value, ...records]
    hasMore.value = records.length >= 20
    page.value++
  } catch (e) {
    ElMessage.error(e.message || "加载失败")
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

async function toggleFollowUser(u) {
  if (followLoadingMap.value[u.id]) return
  followLoadingMap.value[u.id] = true
  try {
    if (u.isFollowed) {
      await userApi.unfollow(u.id)
      u.isFollowed = false
      u.isMutual = false
      store.removeFollowedUser(u.id)
      if (u.fansCount > 0) u.fansCount--
    } else {
      await userApi.follow(u.id)
      u.isFollowed = true
      store.addFollowedUser(u.id)
      u.fansCount = (u.fansCount || 0) + 1
    }
  } catch (e) {
    ElMessage.error(e.message || "操作失败")
  } finally {
    followLoadingMap.value[u.id] = false
  }
}

function goUserProfile(id) {
  router.push({ path: "/profile", query: { userId: id } })
}

// Infinite scroll
function onScroll() {
  const el = document.documentElement
  if (el.scrollHeight - el.scrollTop - el.clientHeight < 200) {
    fetchList()
  }
}

onMounted(() => {
  fetchList(true)
  window.addEventListener("scroll", onScroll)
})

onBeforeUnmount(() => {
  window.removeEventListener("scroll", onScroll)
})
</script>

<style scoped>
.user-list-page {
  max-width: 680px;
  margin: 0 auto;
  padding-bottom: 40px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.page-header h2 {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
}

.state-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 0;
  color: var(--text-secondary);
}
.state-box p { margin-top: 12px; }
.spin { animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

.user-list {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 20px;
  cursor: pointer;
  transition: background 0.15s;
}
.user-item:hover { background: #fafafa; }
.user-item + .user-item { border-top: 1px solid var(--border-color); }

.user-meta {
  flex: 1;
  min-width: 0;
}

.user-name-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.user-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.mutual-badge {
  font-size: 10px;
  background: #fef0f0;
  color: #ff2442;
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 500;
}

.user-bio {
  font-size: 13px;
  color: #909399;
  margin: 2px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-stats {
  font-size: 12px;
  color: #c0c4cc;
}

.load-more-hint {
  text-align: center;
  padding: 24px;
  color: var(--text-secondary);
  font-size: 14px;
}
</style>
