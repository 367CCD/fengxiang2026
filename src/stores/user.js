import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { userApi, notificationApi } from '../api'

// 本地存储工具函数：统一读写，避免重复代码
const TOKEN_KEY = 'forum_token'
const USER_KEY = 'forum_user_info'

function getStoredToken() {
  return localStorage.getItem(TOKEN_KEY) || ''
}

function getStoredUser() {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw && raw !== 'undefined' ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

function saveUserToStorage(user) {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

function clearUserStorage() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export const useUserStore = defineStore('user', () => {
  // 核心状态
  const token = ref(getStoredToken())
  const userInfo = ref(getStoredUser())
  const unreadCount = ref(0)

  // ==================== 关注状态全局同步 ====================
  const followedUserIds = ref([])
  const isLoggedIn = computed(() => !!token.value)
  const userId = computed(() => userInfo.value?.id || null)
  const username = computed(() => userInfo.value?.username || '')
  const avatar = computed(() => userInfo.value?.avatarUrl || userInfo.value?.avatar_url || '')
  const bio = computed(() => userInfo.value?.bio || '')
  const gender = computed(() => userInfo.value?.gender ?? 0)
  const location = computed(() => userInfo.value?.location || '')
  const postCount = computed(() => userInfo.value?.postCount ?? userInfo.value?.post_count ?? 0)
  const followerCount = computed(() => userInfo.value?.followerCount ?? userInfo.value?.follower_count ?? 0)
  const followingCount = computed(() => userInfo.value?.followingCount ?? userInfo.value?.following_count ?? 0)

  // ==================== 核心方法 ====================
  // 登录
async function login(credentials) {
  const res = await userApi.login(credentials)

  if (res && Array.isArray(res) && res.length >= 2) {
    const loginUserInfo = res[0]   // 后端返回: { id, username, role, avatarUrl }
    const userToken = res[1]
    token.value = userToken
    localStorage.setItem(TOKEN_KEY, userToken)

    // 先保存登录返回的基本信息（含 role），确保路由判断可用
    if (loginUserInfo && typeof loginUserInfo === 'object') {
      userInfo.value = loginUserInfo
      saveUserToStorage(loginUserInfo)
    }

    // 登录成功后拉取完整用户信息（覆盖补充完整资料）
    await fetchProfile()
  }
  return res
}

  // 注册
  async function register(data) {
    const res = await userApi.register(data)
    if (res && Array.isArray(res) && res.length >= 2) {
      const regUserInfo = res[0]   // { id, username, role, avatarUrl }
      const userToken = res[1]
      token.value = userToken
      localStorage.setItem(TOKEN_KEY, userToken)
      // 注册用户一律是普通用户（role=0），直接保存
      if (regUserInfo && typeof regUserInfo === 'object') {
        userInfo.value = regUserInfo
        saveUserToStorage(regUserInfo)
      }
      await fetchProfile()
      await fetchFollowedUserIds()
    }
    return res
  }

  // 退出登录
  function logout() {
    token.value = ''
    userInfo.value = null
    unreadCount.value = 0
    clearUserStorage()
  }

  // 拉取最新用户资料
async function fetchProfile() {
  // ========== 新增：token 校验与本地同步逻辑 ==========
  let validToken = token.value
  // 1. Store 里没有 token，尝试从本地存储读取并同步
  if (!validToken) {
    const localToken = localStorage.getItem(TOKEN_KEY)
    // 过滤空值、异常字符串
    if (localToken && localToken !== 'undefined' && localToken !== 'null') {
      validToken = localToken
      // 同步回 Store，保证 isLoggedIn 状态同步
      token.value = validToken
      // console.log('【fetchProfile】从本地同步token到Store')
    }
  }

  // 2. 最终没有有效 token，直接提示并终止
  if (!validToken) {
    ElMessage.warning('请先登录')
    throw new Error('未登录，无有效token')
  }
  // =====================================================

  // 原有逻辑：请求用户详情
  try {
    const res = await userApi.getProfile()
    if (res) {
      userInfo.value = res
      saveUserToStorage(res)
    }
    // 登录后拉取关注列表
    await fetchFollowedUserIds()
    return res
  } catch (e) {
    console.error('获取用户资料失败:', e)
    // 接口 401 等鉴权失败时，也可以在这里补充登出逻辑
    throw e
  }
}

  // 统一更新用户资料
  async function updateProfile(params) {
    try {
      await userApi.updateProfile(params)
      // 本地状态同步更新
      userInfo.value = { ...userInfo.value, ...params }
      saveUserToStorage(userInfo.value)
    } catch (e) {
      console.error('更新资料失败:', e)
      throw e
    }
  }

  // 更新用户统计数字（删除笔记/收藏后调用）
  function updateStat(key, value) {
    if (!userInfo.value) return
    userInfo.value[key] = value
    saveUserToStorage(userInfo.value)
  }

  // ==================== 关注状态同步方法 ====================
  async function fetchFollowedUserIds() {
    if (!isLoggedIn.value) { followedUserIds.value = []; return }
    try {
      const res = await userApi.getFollowIds()
      followedUserIds.value = Array.isArray(res) ? res : (res.data || res.list || [])
    } catch (e) {
      console.warn('获取关注ID列表失败:', e.message)
      followedUserIds.value = []
    }
  }

  function addFollowedUser(userId) {
    if (!followedUserIds.value.includes(userId)) {
      followedUserIds.value.push(userId)
    }
  }

  function removeFollowedUser(userId) {
    const idx = followedUserIds.value.indexOf(userId)
    if (idx > -1) {
      followedUserIds.value.splice(idx, 1)
    }
  }

  function isUserFollowed(userId) {
    return followedUserIds.value.includes(userId)
  }

  // 获取未读消息数
  async function fetchUnreadCount() {
    try {
      const res = await notificationApi.getUnreadCount()
      unreadCount.value = res.count || 0
    } catch (e) {
      console.error('获取未读消息失败:', e)
    }
  }

  return {
    // 状态
    token,
    userInfo,
    unreadCount,
    // 计算属性
    isLoggedIn,
    userId,
    username,
    avatar,
    bio,
    gender,
    location,
    postCount,
    followerCount,
    followingCount,
    // 关注状态
    followedUserIds,
    // 方法
    isUserFollowed,
    addFollowedUser,
    removeFollowedUser,
    fetchFollowedUserIds,
    login,
    register,
    logout,
    fetchProfile,
    updateProfile,
    updateStat,
    fetchUnreadCount
  }
})

