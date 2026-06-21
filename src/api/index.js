import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器 - 附加 token
api.interceptors.request.use(config => {
  const token = localStorage.getItem('forum_token')
  if (token && token !== 'undefined') {
    config.headers.token = token
  }
  return config
}, error => Promise.reject(error))

// 响应拦截器 - 统一处理后端返回格式 { code, msg, data }
api.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 1) {
      return res.data
    }
    // code === 0，把 msg 作为错误信息
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  error => {
    // 处理 HTTP 层面的错误（网络断开、500等）
    if (error.response?.status === 401) {
      localStorage.removeItem('forum_token')
      localStorage.removeItem('forum_user_info')
      window.location.href = '/login'
    }
    // 如果后端有返回 msg，优先使用
    const msg = error.response?.data?.msg || error.message || '网络异常'
    return Promise.reject(new Error(msg))
  }
)

// ==================== 用户模块 ====================
export const userApi = {
  login(data) { return api.post('/login', data) },
  register(data) { return api.post('/register', data) },
  getProfile(userId) { return api.get('/users/profile', { params: userId ? { userId } : {} }) },
  getFollowIds() { return api.get('/users/follow/ids') },
  updateProfile(data) { return api.put('/users/profile', data) },
  follow(userId) { return api.post(`/users/${userId}/follow`) },
  unfollow(userId) { return api.delete(`/users/${userId}/follow`) },
  getFollowers(userId, params) { return api.get(`/users/${userId}/followers`, { params }) },
  getFollowing(userId, params) { return api.get(`/users/${userId}/following`, { params }) }
}

// ==================== 帖子模块 ====================
export const postApi = {
  getMyPosts(params) { return api.get('/posts/mine', { params }) },
  getFeed(params) { return api.get('/posts', { params }) },
  getFollowFeed(params) { return api.get('/posts/follow', { params }) },
  getFollowNewCount(params) { return api.get('/posts/follow/new-count', { params }) },
  getPostDetail(id) { return api.get(`/posts/${id}`) },
  createPost(data) { return api.post('/posts', data) },
  updatePost(id, data) { return api.put(`/posts/${id}`, data) },
  getDrafts(params) { return api.get('/posts/drafts', { params }) },
  deletePost(id) { return api.delete(`/posts/${id}`) },
  likePost(id) { return api.post(`/posts/${id}/like`) },
  unlikePost(id) { return api.delete(`/posts/${id}/like`) },
  collectPost(id, collectionId) { return api.post(`/posts/${id}/collect`, { collectionId: collectionId }) },
  uncollectPost(id) { return api.delete(`/posts/${id}/collect`) },
  getComments(postId, params) { return api.get(`/posts/${postId}/comments`, { params }) },
  getCommentReplies(postId, parentId, params) {
    return api.get(`/posts/${postId}/comments/${parentId}/replies`, { params })
  },
  createComment(data) { return api.post('/comments', data) },
  deleteComment(id) { return api.delete(`/comments/${id}`) },
  likeComment(id) { return api.post(`/comments/${id}/like`) },  unlikeComment(id) { return api.delete(`/comments/${id}/like`) },
  // ===== 新增：通用举报接口 =====
  report(data) { return api.post('/reports', data) },
}

// ==================== 标签模块 ====================
export const tagApi = {
  getTags() { return api.get('/tags') },
  getPostsByTag(tagId, params) { return api.get(`/tags/${tagId}/posts`, { params }) }
}

// ==================== 收藏模块 ====================
export const collectionApi = {
  getCollections(userId) { return api.get('/collections', { params: userId ? { userId } : {} }) },
  getCollectionPosts(id, params) { return api.get(`/collections/${id}/posts`, { params }) },
  createCollection(data) { return api.post('/collections', data) },
  deleteCollection(id) { return api.delete('/collections/' + id) }
}

// ==================== 通知模块 ====================
export const notificationApi = {
  createNotification(data) { return api.post('/notifications', data) },
  getNotifications(params) { return api.get('/notifications', { params }) },
  getUnreadCount() { return api.get('/notifications/unread-count') },
  markAsRead(id) { return api.put(`/notifications/${id}/read`) },
  markAllAsRead() { return api.put('/notifications/read-all') }
}

// ==================== 搜索模块 ====================
export const searchApi = {
  search(params) { return api.get('/search', { params }) },
  getHistory() { return api.get('/search/history') },
  clearHistory() { return api.delete('/search/history') }
}

// ==================== 文件上传 ====================
export const uploadApi = {
  uploadImage(file) {
    const form = new FormData()
    form.append('file', file)
    return api.post('/upload/image', form)
  },
  uploadVideo(videoFile, coverFile) {
    const form = new FormData()
    form.append('file', videoFile)
    if (coverFile) {
      form.append('cover', coverFile)
    }
    return api.post('/upload/video', form)
  }
}

export default api




