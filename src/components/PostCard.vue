<template>
  <div class="post-card" @click="goDetail">
    <div class="card-cover" :style="{ paddingBottom: coverRatio + '%' }">
     <el-image
        :src="post.coverUrl"
        fit="cover"
        class="cover-img"
        loading="lazy"
      >
        <template #error>
          <div class="img-placeholder">
            <el-icon :size="24" color="#ccc"><Picture /></el-icon>
          </div>
        </template>
      </el-image>
      <div v-if="post.type === 2" class="video-tag">
        <el-icon :size="20"><VideoPlay /></el-icon>
      </div>
      <div v-if="post.type === 2" class="video-play-overlay">
        <div class="play-circle">
          <el-icon :size="28"><VideoPlay /></el-icon>
        </div>
      </div>
    </div>
    <div class="card-body">
      <p class="card-title">{{ post.title }}</p>
      <div class="card-footer">
        <div class="user-row" @click.stop="goUserProfile">
          <el-avatar :size="20" :src="post.avatarUrl || undefined">
            {{ post.username?.charAt(0)?.toUpperCase() }}
          </el-avatar>
          <span class="username">{{ post.username }}</span>
        </div>
        <div class="stats">
          <span>🤍 {{ formatNum(post.likeCount) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Picture, VideoPlay } from '@element-plus/icons-vue'

const props = defineProps({
  post: { type: Object, required: true }
})

const router = useRouter()

const coverRatio = computed(() => {
  if (props.post.width && props.post.height) {
    return (props.post.height / props.post.width) * 100
  }
  return 75 + Math.random() * 25
})

function formatNum(n) {
  if (!n) return 0
  return n >= 10000 ? (n / 10000).toFixed(1) + 'w' : n >= 1000 ? (n / 1000).toFixed(1) + 'k' : n
}

function goDetail() {
  const id = props.post?.id
  if (!id) {
    console.warn("PostCard: post.id is missing", props.post)
    return
  }
  router.push({ path: `/post/${id}` }).catch(e => {
    console.error("Navigation failed:", e)
  })
}

function goUserProfile() {
  const uid = props.post?.userId
  if (!uid) return
  router.push({ path: '/profile', query: { userId: uid } })
}
</script>

<style scoped>
.post-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.post-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
}

.card-cover {
  position: relative;
  width: 100%;
  overflow: hidden;
  background: #f0f0f0;
}

.cover-img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.img-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
}

.video-tag {
  position: absolute;
  top: 8px;
  left: 8px;
  background: rgba(0,0,0,0.6);
  color: #fff;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
}

.video-play-overlay {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.play-circle {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: rgba(0,0,0,0.5);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-body {
  padding: 10px 12px 12px;
}

.card-title {
  font-size: 14px;
  font-weight: 500;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-box-align: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 8px;
  color: #222;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.user-row {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.username {
  font-size: 12px;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.stats {
  font-size: 12px;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 2px;
}
</style>
