<template>
  <div class="profile-page">
    <!-- 个人信息卡片 -->
    <div class="profile-card">
      <div class="profile-header">
        <div class="avatar-wrapper" :class="{ clickable: isOwner }" @click="isOwner && triggerAvatarUpload()">
          <el-avatar :size="72" :src="profile.avatarUrl || undefined" class="profile-avatar" :style="{ cursor: isOwner ? 'pointer' : 'default' }">
            {{ (profile.username || '?').charAt(0)?.toUpperCase() }}
          </el-avatar>
          <div v-if="isOwner" class="avatar-overlay">
            <el-icon :size="18"><Camera /></el-icon>
            <span>更换</span>
          </div>
        </div>
        <input v-if="isOwner" ref="avatarInput" type="file" accept="image/*" style="display:none" @change="onAvatarChange" />
        <div class="profile-info">
          <h3>
            {{ profile.username || '未知用户' }}
            <span v-if="profile.gender === 1" class="gender-icon male" title="男">♂</span>
            <span v-else-if="profile.gender === 2" class="gender-icon female" title="女">♀</span>
          </h3>
          <p class="bio">{{ profile.bio || '这个人很懒，什么都没留下~' }}</p>
          <!-- 自己的资料 → 编辑按钮；别人的资料 → 关注按钮 -->
          <el-button v-if="isOwner" text type="primary" size="small" @click="openEditDialog">
            编辑资料
          </el-button>
          <el-button v-else-if="store.isLoggedIn && profile.id !== store.userId"
            :type="profile.isFollowed ? 'default' : 'primary'"
            size="small" round :loading="followLoading"
            @click="toggleFollow"
          >
            {{ profile.isFollowed ? '已关注' : '关注' }}
          </el-button>
        </div>
      </div>
      <div class="profile-stats">
        <div class="stat-item">
          <span class="stat-num">{{ profile.postCount ?? 0 }}</span>
          <span class="stat-label">笔记</span>
        </div>
        <div class="stat-item" :class="{ clickable: isOwner }" @click="isOwner && goUserList('followers')">
          <span class="stat-num">{{ profile.followerCount ?? 0 }}</span>
          <span class="stat-label">粉丝</span>
        </div>
        <div class="stat-item" :class="{ clickable: isOwner }" @click="isOwner && goUserList('following')">
          <span class="stat-num">{{ profile.followingCount ?? 0 }}</span>
          <span class="stat-label">关注</span>
        </div>
      </div>
    </div>

    <!-- Tab 切换 -->
    <div class="profile-tabs">
      <el-tabs v-model="activeTab">
        <el-tab-pane :label="isOwner ? '我的笔记' : '他的笔记'" name="posts">
          <div v-if="loadingPosts" class="state-box">
            <el-icon class="spin" :size="24" color="#ff2442"><Loading /></el-icon>
          </div>
          <div v-else-if="myPosts.length === 0" class="state-box">
            <el-empty description="还没有发布笔记" :image-size="100" />
          </div>
          <div v-else class="post-grid">
            <div v-for="post in myPosts" :key="post.id" class="post-grid-item" @click="goPostDetail(post.id)">
              <el-image :src="post.coverUrl" fit="cover" class="grid-cover" loading="lazy">
                <template #error>
                  <div class="grid-placeholder"><el-icon><Picture /></el-icon></div>
                </template>
              </el-image>
              <div class="grid-footer">
                <span>{{ post.likeCount || 0 }} 赞</span>
                <el-button v-if="isOwner" text size="small" type="danger" @click.stop="deletePost(post.id)">删除</el-button>
              </div>
            </div>
          </div>
        </el-tab-pane>
        <!-- 草稿（仅自己可见） -->
        <el-tab-pane v-if="isOwner" label="草稿" name="drafts">
          <div v-if="loadingDrafts" class="state-box">
            <el-icon class="spin" :size="24" color="#ff2442"><Loading /></el-icon>
          </div>
          <div v-else-if="draftPosts.length === 0" class="state-box">
            <el-empty description="暂无草稿" :image-size="100">
              <template #extra>
                <el-button type="primary" size="small" @click="$router.push('/publish')">去写笔记</el-button>
              </template>
            </el-empty>
          </div>
          <div v-else class="post-grid">
            <div v-for="post in draftPosts" :key="post.id" class="post-grid-item draft-item" @click="editDraft(post.id)">
              <el-image v-if="post.coverUrl" :src="post.coverUrl" fit="cover" class="grid-cover" loading="lazy">
                <template #error>
                  <div class="grid-placeholder draft-placeholder"><el-icon><Edit /></el-icon></div>
                </template>
              </el-image>
              <div v-else class="grid-placeholder draft-placeholder">
                <el-icon :size="28"><Edit /></el-icon>
                <span class="draft-label">草稿</span>
              </div>
              <div class="draft-overlay">
                <el-button type="primary" size="small" round>继续编辑</el-button>
              </div>
              <div class="grid-footer draft-footer">
                <span class="draft-badge">草稿</span>
                <span class="draft-time">{{ formatDate(post.updatedAt || post.createdAt) }}</span>
                <el-button text size="small" type="danger" @click.stop="deleteDraft(post.id)">删除</el-button>
              </div>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane :label="isOwner ? '收藏' : '他的收藏'" name="collections">
          <div v-if="isOwner" class="col-header">
            <h3 class="col-section-title">我的收藏夹</h3>
            <el-button size="small" type="primary" @click="showCreateColDialog = true">
              <el-icon><Plus /></el-icon> 新建收藏夹
            </el-button>
          </div>
          <div v-else class="col-header">
            <h3 class="col-section-title">公开收藏夹</h3>
          </div>
          <div v-if="loadingCols" class="state-box">
            <el-icon class="spin" :size="24" color="#ff2442"><Loading /></el-icon>
          </div>
          <div v-else-if="collections.length === 0" class="col-empty">
            <el-empty :image-size="80" :description="isOwner ? '还没有收藏夹' : '暂无公开收藏夹'">
              <template v-if="isOwner" #extra>
                <el-button type="primary" size="small" @click="showCreateColDialog = true">创建第一个收藏夹</el-button>
              </template>
            </el-empty>
          </div>
          <div v-else class="collection-list">
            <div v-for="col in collections" :key="col.id" class="collection-card" @click="viewCollection(col)">
              <div class="col-cover-wrap" :style="coverBg(col)">
                <span class="col-cover-letter">{{ col.name?.charAt(0) }}</span>
                <div v-if="isOwner" class="col-delete-btn" @click.stop="confirmDeleteCollection(col)">
                  <el-icon><Delete /></el-icon>
                </div>
                <div class="col-card-overlay">
                  <span class="col-view-hint">查看全部</span>
                </div>
                <div class="col-post-badge">{{ col.postCount || 0 }}</div>
              </div>
              <div class="col-info">
                <div class="col-info-top">
                  <p class="col-name">{{ col.name }}</p>
                  <span v-if="col.isPublic === 0" class="col-badge-sm">私密</span>
                  <span v-else class="col-badge-sm public">公开</span>
                </div>
                <p v-if="col.description" class="col-desc">{{ col.description }}</p>
                <p class="col-date">{{ formatDate(col.createdAt) }}</p>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 编辑资料对话框（仅自己） -->
    <el-dialog v-if="isOwner" v-model="showEditDialog" title="编辑资料" width="420px">
      <el-form :model="editForm" label-position="top">
        <el-form-item label="用户名">
          <el-input v-model="editForm.username" disabled />
        </el-form-item>
        <el-form-item label="个人简介">
          <el-input v-model="editForm.bio" type="textarea" :rows="3" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="editForm.gender">
            <el-radio :value="0">保密</el-radio>
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="所在城市">
          <el-input v-model="editForm.location" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>

    <!-- 新建收藏夹对话框（仅自己） -->
    <el-dialog v-if="isOwner" v-model="showCreateColDialog" title="新建收藏夹" width="420px">
      <el-form :model="colForm" label-position="top">
        <el-form-item label="封面图片">
          <div class="col-cover-upload" @click="triggerCoverUpload">
            <img v-if="colForm.coverPreview" :src="colForm.coverPreview" class="col-cover-preview" />
            <div v-else class="col-cover-placeholder">
              <el-icon :size="28"><Picture /></el-icon>
              <span>点击上传封面</span>
            </div>
          </div>
          <input ref="colCoverInput" type="file" accept="image/*" style="display:none" @change="onCoverSelected" />
        </el-form-item>
        <el-form-item label="收藏夹名称">
          <el-input v-model="colForm.name" placeholder="给收藏夹取个名字" maxlength="50" />
        </el-form-item>
        <el-form-item label="描述（选填）">
          <el-input v-model="colForm.description" type="textarea" :rows="2" maxlength="200" />
        </el-form-item>
        <el-form-item label="公开状态">
          <el-radio-group v-model="colForm.isPublic">
            <el-radio :value="1">公开</el-radio>
            <el-radio :value="0">私密</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateColDialog = false">取消</el-button>
        <el-button type="primary" :loading="creatingCol" @click="createCollection">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading, Picture, Camera, Plus, Delete, Edit } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import { postApi, collectionApi, userApi, uploadApi } from '../api'

const store = useUserStore()
const route = useRoute()
const router = useRouter()

// ==================== 判断是自己的主页还是他人的 ====================
const targetUserId = computed(() => {
  const id = Number(route.query.userId)
  return id && id > 0 ? id : store.userId
})
const isOwner = computed(() => targetUserId.value === store.userId)

// ==================== 目标用户资料（独立于 store） ====================
const profile = reactive({
  id: null, username: '', avatarUrl: '', bio: '',
  gender: 0, location: '', postCount: 0,
  followerCount: 0, followingCount: 0, isFollowed: false
})

const activeTab = ref('posts')
const showEditDialog = ref(false)
const saving = ref(false)
const loadingPosts = ref(false)
const loadingCols = ref(false)
const loadingDrafts = ref(false)
const myPosts = ref([])
const draftPosts = ref([])
const collections = ref([])
const uploadingAvatar = ref(false)
const avatarInput = ref(null)
const colCoverInput = ref(null)
const followLoading = ref(false)

// ==================== 关注 / 取关 ====================
async function toggleFollow() {
  if (!store.isLoggedIn) { ElMessage.warning('请先登录'); return }
  if (followLoading.value) return
  followLoading.value = true
  try {
    if (profile.isFollowed) {
      await userApi.unfollow(profile.id)
      profile.isFollowed = false
      if (profile.followerCount > 0) profile.followerCount--
      store.removeFollowedUser(profile.id)
    } else {
      await userApi.follow(profile.id)
      profile.isFollowed = true
      profile.followerCount++
      store.addFollowedUser(profile.id)
    }
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    followLoading.value = false
  }
}

// ==================== 头像上传 ====================
function triggerAvatarUpload() { avatarInput.value?.click() }

async function onAvatarChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  uploadingAvatar.value = true
  try {
    const res = await uploadApi.uploadImage(file)
    const url = res.url || res.imageUrl || (typeof res === 'string' ? res : '')
    if (!url) throw new Error('上传失败')
    await userApi.updateProfile({ avatarUrl: url })
    store.$patch({ userInfo: { ...store.userInfo, avatarUrl: url } })
    localStorage.setItem('forum_user_info', JSON.stringify(store.userInfo))
    profile.avatarUrl = url
    ElMessage.success('头像已更新')
  } catch (e) {
    ElMessage.error(e.message || '头像上传失败')
  } finally {
    uploadingAvatar.value = false
    e.target.value = ''
  }
}

// ==================== 收藏夹表单 ====================
function triggerCoverUpload() { colCoverInput.value?.click() }
function onCoverSelected(e) {
  const file = e.target.files?.[0]
  if (!file) return
  colForm.coverFile = file
  colForm.coverPreview = URL.createObjectURL(file)
}

const showCreateColDialog = ref(false)
const creatingCol = ref(false)
const colForm = reactive({
  name: '', description: '', isPublic: 1,
  coverUrl: '', coverPreview: '', coverFile: null
})

// ==================== 编辑资料表单 ====================
const editForm = reactive({
  username: '', bio: '', gender: 0, location: ''
})

function openEditDialog() {
  editForm.username = store.username || ''
  editForm.bio = store.bio || ''
  editForm.gender = store.userInfo?.gender ?? 0
  editForm.location = store.userInfo?.location || ''
  showEditDialog.value = true
}

async function saveProfile() {
  saving.value = true
  try {
    await userApi.updateProfile({
      bio: editForm.bio, gender: editForm.gender, location: editForm.location
    })
    store.$patch({
      userInfo: { ...store.userInfo, bio: editForm.bio, gender: editForm.gender, location: editForm.location }
    })
    localStorage.setItem('forum_user_info', JSON.stringify(store.userInfo))
    profile.bio = editForm.bio
    profile.gender = editForm.gender
    profile.location = editForm.location
    ElMessage.success('保存成功')
    showEditDialog.value = false
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ==================== 加载目标用户资料 ====================
async function fetchProfile() {
  try {
    if (isOwner.value) {
      // 查看自己：用 store 数据
      await store.fetchProfile()
      Object.assign(profile, {
        id: store.userId,
        username: store.username,
        avatarUrl: store.avatar,
        bio: store.bio,
        gender: store.gender,
        location: store.location,
        postCount: store.postCount,
        followerCount: store.followerCount,
        followingCount: store.followingCount,
        isFollowed: false
      })
    } else {
      // 查看他人：调接口
      const res = await userApi.getProfile(targetUserId.value)
      if (res) {
        Object.assign(profile, {
          id: res.id,
          username: res.username,
          avatarUrl: res.avatarUrl || res.avatar_url,
          bio: res.bio || '',
          gender: res.gender ?? 0,
          location: res.location || '',
          postCount: res.postCount ?? res.post_count ?? 0,
          followerCount: res.followerCount ?? res.follower_count ?? 0,
          followingCount: res.followingCount ?? res.following_count ?? 0,
          isFollowed: res.isFollowed ?? false
        })
      }
    }
  } catch (e) {
    console.warn('[Profile] fetchProfile failed:', e)
  }
}

// ==================== 加载帖子 ====================
async function fetchPosts() {
  loadingPosts.value = true
  try {
    const res = await postApi.getMyPosts({ userId: targetUserId.value, page: 1, pageSize: 50 })
    myPosts.value = Array.isArray(res) ? res : (
      res && typeof res === 'object' && Array.isArray(res.records) ? res.records : (
        res && typeof res === 'object' && Array.isArray(res.list) ? res.list : (
          res && typeof res === 'object' && Array.isArray(res.data) ? res.data : []
        )
      )
    )
  } catch (_) {
    myPosts.value = []
  } finally {
    loadingPosts.value = false
  }
}

// ==================== 加载草稿 ====================
async function fetchDrafts() {
  if (!isOwner.value) return
  loadingDrafts.value = true
  try {
    const res = await postApi.getDrafts({ page: 1, pageSize: 50 })
    draftPosts.value = Array.isArray(res) ? res : (
      res && typeof res === 'object' && Array.isArray(res.records) ? res.records : (
        res && typeof res === 'object' && Array.isArray(res.list) ? res.list : (
          res && typeof res === 'object' && Array.isArray(res.data) ? res.data : []
        )
      )
    )
  } catch (_) {
    draftPosts.value = []
  } finally {
    loadingDrafts.value = false
  }
}

function editDraft(id) {
  router.push({ path: '/publish', query: { draftId: id } })
}

async function deleteDraft(id) {
  try {
    await ElMessageBox.confirm('确定删除该草稿？', '确认', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    })
    await postApi.deletePost(id)
    draftPosts.value = draftPosts.value.filter(p => p.id !== id)
    ElMessage.success('草稿已删除')
  } catch (_) {}
}

// ==================== 加载收藏夹 ====================
async function fetchCollections() {
  loadingCols.value = true
  try {
    const res = await collectionApi.getCollections(isOwner.value ? null : targetUserId.value)
    collections.value = Array.isArray(res) ? res : (
      res && typeof res === 'object' && Array.isArray(res.records) ? res.records : (
        res && typeof res === 'object' && Array.isArray(res.list) ? res.list : (
          res && typeof res === 'object' && Array.isArray(res.data) ? res.data : []
        )
      )
    )
  } catch (_) {
    collections.value = []
  } finally {
    loadingCols.value = false
  }
}

function viewCollection(col) {
  router.push({ path: '/discover', query: { collectionId: col.id, collectionName: col.name } })
}

function coverBg(col) {
  const url = col.coverUrl || col.cover_url
  if (url) return { backgroundImage: 'url(' + url + ')', backgroundSize: 'cover', backgroundPosition: 'center' }
  const colors = ['#ff6b6b', '#ffa94d', '#ffd43b', '#69db7c', '#38d9a9', '#4dabf7', '#748ffc', '#da77f2']
  const hash = col.name ? col.name.split('').reduce((a, c) => a + c.charCodeAt(0), 0) : 0
  const color = colors[hash % colors.length]
  return { background: 'linear-gradient(135deg, ' + color + ', ' + color + 'dd)' }
}

async function confirmDeleteCollection(col) {
  try {
    await ElMessageBox.confirm('确定要删除收藏夹「' + col.name + '」吗？', '删除收藏夹', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    })
    await collectionApi.deleteCollection(col.id)
    ElMessage.success('已删除')
    fetchCollections()
  } catch (_) {}
}

function formatDate(t) {
  if (!t) return ''
  const d = new Date(t)
  return d.getFullYear() + '年' + (d.getMonth() + 1) + '月' + d.getDate() + '日'
}

async function createCollection() {
  if (!colForm.name.trim()) { ElMessage.warning('请输入收藏夹名称'); return }
  creatingCol.value = true
  try {
    let coverUrl = colForm.coverUrl
    if (colForm.coverFile) {
      const res = await uploadApi.uploadImage(colForm.coverFile)
      coverUrl = res.url || res.imageUrl || (typeof res === 'string' ? res : '')
    }
    await collectionApi.createCollection({
      name: colForm.name.trim(), description: colForm.description.trim(),
      isPublic: colForm.isPublic, is_public: colForm.isPublic,
      coverUrl: coverUrl, cover_url: coverUrl
    })
    ElMessage.success('收藏夹已创建')
    showCreateColDialog.value = false
    colForm.name = ''; colForm.description = ''; colForm.isPublic = 1
    colForm.coverUrl = ''; colForm.coverPreview = ''; colForm.coverFile = null
    fetchCollections()
  } catch (e) {
    ElMessage.error(e.message || '创建失败')
  } finally {
    creatingCol.value = false
  }
}

async function deletePost(id) {
  try {
    await ElMessageBox.confirm('确定要删除这篇笔记吗？', '确认', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    })
    await postApi.deletePost(id)
    myPosts.value = myPosts.value.filter(p => p.id !== id)
    ElMessage.success('已删除')
  } catch (_) {}
}

function goUserList(type) {
  router.push({ path: '/users', query: { type, userId: targetUserId.value } })
}

function goPostDetail(id) {
  router.push('/post/' + id)
}

// 监听路由变化（从用户列表点击不同用户时）
watch(targetUserId, () => {
  fetchProfile()
  fetchPosts()
  fetchDrafts()
  fetchCollections()
})

onMounted(() => {
  fetchProfile()
  fetchPosts()
  fetchDrafts()
  fetchCollections()
})
</script>

<style scoped>
.profile-page { max-width: 860px; margin: 0 auto; }
.profile-card { background: #fff; border-radius: 12px; padding: 28px; box-shadow: 0 1px 4px rgba(0,0,0,0.04); margin-bottom: 20px; }
.profile-header { display: flex; align-items: center; gap: 20px; }
.avatar-wrapper { position: relative; flex-shrink: 0; border-radius: 50%; }
.avatar-wrapper.clickable { cursor: pointer; }
.avatar-wrapper.clickable:hover .avatar-overlay { opacity: 1; }
.avatar-overlay { position: absolute; inset: 0; border-radius: 50%; background: rgba(0,0,0,0.5); color: #fff; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 2px; font-size: 11px; opacity: 0; transition: opacity 0.2s; }
.profile-avatar { flex-shrink: 0; border: 3px solid #fff; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
.profile-info h3 { font-size: 20px; font-weight: 600; margin-bottom: 4px; display: flex; align-items: center; gap: 6px; }
.gender-icon { display: inline-flex; align-items: center; justify-content: center; width: 18px; height: 18px; border-radius: 50%; font-size: 12px; line-height: 1; flex-shrink: 0; }
.gender-icon.male { background: #e7f5ff; color: #339af0; border: 1px solid #a5d8ff; }
.gender-icon.female { background: #fff0f6; color: #f06595; border: 1px solid #faa2c1; }
.profile-info .bio { font-size: 14px; color: var(--text-secondary); margin-bottom: 4px; }
.profile-stats { display: flex; gap: 40px; margin-top: 20px; padding-top: 16px; border-top: 1px solid var(--border-color); }
.stat-item { display: flex; flex-direction: column; align-items: center; }
.stat-item.clickable { cursor: pointer; }
.stat-item.clickable:hover .stat-num { color: var(--primary-color); }
.stat-num { font-size: 20px; font-weight: 600; transition: color 0.2s; }
.stat-label { font-size: 13px; color: var(--text-secondary); }
.profile-tabs { background: #fff; border-radius: 12px; padding: 16px 20px; box-shadow: 0 1px 4px rgba(0,0,0,0.04); }
.state-box { display: flex; justify-content: center; padding: 40px 0; }
.spin { animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.post-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.post-grid-item { border-radius: 8px; overflow: hidden; position: relative; aspect-ratio: 1; cursor: pointer; }
.grid-cover { width: 100%; height: 100%; }
.grid-placeholder { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; background: #f5f5f5; color: #ccc; }
.grid-footer { position: absolute; bottom: 0; left: 0; right: 0; background: linear-gradient(transparent, rgba(0,0,0,0.5)); color: #fff; font-size: 12px; padding: 8px; display: flex; justify-content: space-between; align-items: center; opacity: 0; transition: opacity 0.2s; }
.post-grid-item:hover .grid-footer { opacity: 1; }
.col-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.col-section-title { font-size: 15px; font-weight: 600; color: #333; margin: 0; }
.col-empty { padding: 10px 0; }
.collection-list { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
.collection-card { border-radius: 12px; overflow: hidden; cursor: pointer; transition: all 0.25s; background: #fff; border: 1px solid #eee; box-shadow: 0 1px 3px rgba(0,0,0,0.02); }
.collection-card:hover { transform: translateY(-3px); box-shadow: 0 6px 20px rgba(255,36,66,0.08); border-color: #ffd4d4; }
.col-cover-wrap { position: relative; overflow: hidden; aspect-ratio: 16/10; display: flex; align-items: center; justify-content: center; background: #f5f5f5; }
.col-cover-letter { font-size: 28px; font-weight: 700; color: rgba(255,255,255,0.6); text-shadow: 0 1px 3px rgba(0,0,0,0.1); user-select: none; pointer-events: none; }
.col-card-overlay { position: absolute; inset: 0; background: rgba(0,0,0,0.25); display: flex; align-items: center; justify-content: center; opacity: 0; transition: opacity 0.2s; }
.collection-card:hover .col-card-overlay { opacity: 1; }
.col-view-hint { color: #fff; font-size: 12px; font-weight: 500; background: rgba(255,255,255,0.25); padding: 5px 16px; border-radius: 20px; backdrop-filter: blur(4px); letter-spacing: 1px; }
.col-delete-btn { position: absolute; top: 6px; left: 6px; width: 28px; height: 28px; border-radius: 50%; background: rgba(0,0,0,0.5); color: #fff; display: flex; align-items: center; justify-content: center; cursor: pointer; opacity: 0; transition: opacity 0.2s; z-index: 2; }
.collection-card:hover .col-delete-btn { opacity: 1; }
.col-delete-btn:hover { background: #e53935; }
.col-post-badge { position: absolute; top: 8px; right: 8px; background: rgba(0,0,0,0.55); color: #fff; font-size: 11px; font-weight: 600; padding: 2px 8px; border-radius: 6px; backdrop-filter: blur(2px); }
.col-info { padding: 10px 12px 12px; }
.col-info-top { display: flex; align-items: center; gap: 6px; margin-bottom: 3px; }
.col-badge-sm { font-size: 10px; padding: 0 6px; border-radius: 4px; background: #f0f0f0; color: #999; line-height: 18px; flex-shrink: 0; }
.col-badge-sm.public { background: #e8f5e9; color: #43a047; }
.col-name { font-size: 14px; font-weight: 600; color: #222; margin: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.col-desc { font-size: 12px; color: #999; margin: 2px 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.col-date { font-size: 11px; color: #ccc; margin: 4px 0 0; }
.col-cover-upload { width: 100%; height: 140px; border: 2px dashed #ddd; border-radius: 10px; overflow: hidden; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: border-color 0.2s; }
.col-cover-upload:hover { border-color: var(--el-color-primary); }
.col-cover-placeholder { display: flex; flex-direction: column; align-items: center; gap: 8px; color: #bbb; font-size: 13px; }
.col-cover-preview { width: 100%; height: 100%; object-fit: cover; display: block; }

/* 草稿 */
.draft-item { position: relative; }
.draft-item:hover .draft-overlay { opacity: 1; }
.draft-overlay {
  position: absolute; inset: 0; background: rgba(0,0,0,0.35);
  display: flex; align-items: center; justify-content: center;
  opacity: 0; transition: opacity 0.2s; border-radius: 8px;
}
.draft-placeholder {
  flex-direction: column; gap: 8px; color: #ccc;
}
.draft-label { font-size: 13px; color: #999; }
.draft-footer { opacity: 1 !important; }
.draft-badge {
  background: #e6a23c; color: #fff; font-size: 10px;
  padding: 1px 8px; border-radius: 4px; font-weight: 500;
}
.draft-time { font-size: 11px; color: rgba(255,255,255,0.8); }
</style>