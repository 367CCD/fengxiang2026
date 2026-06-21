import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截器
api.interceptors.request.use(config => {
  const token = localStorage.getItem('forum_token')
  if (token && token !== 'undefined') {
    config.headers.token = token
  }
  return config
}, error => Promise.reject(error))

// 响应拦截器
api.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 1) return res.data
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('forum_token')
      localStorage.removeItem('forum_user_info')
      window.location.href = '/login'
    }
    const msg = error.response?.data?.msg || error.message || '网络异常'
    return Promise.reject(new Error(msg))
  }
)

// ═══════════════════════════════════════
// 用户管理
// ═══════════════════════════════════════
export const adminUserApi = {
  /** 用户列表（支持多条件筛选） */
  getList(params) { return api.get('/admin/users', { params }) },
  /** 用户详情 */
  getDetail(id) { return api.get(`/admin/users/${id}`) },
  /** 批量禁用/启用 */
  batchUpdateStatus(ids, status) { return api.put('/admin/users/batch/status', { ids, status }) },
  /** 禁言用户 */
  muteUsers(data) { return api.post('/admin/users/mute', data) },
  /** 手动解除禁言 */
  unmuteUsers(ids) { return api.post('/admin/users/unmute', { ids }) },
  /** 禁言已到期用户列表 */
  getMutedExpired() { return api.get('/admin/users/muted-expired') },
  /** 用户禁言历史 */
  getMuteHistory(userId, params) { return api.get(`/admin/users/${userId}/mute-history`, { params }) },
  /** 重置密码 */
  resetPassword(userId, password) { return api.put(`/admin/users/${userId}/reset-password`, { password }) },
  /** 重置头像（清除违规头像） */
  resetAvatar(userId) { return api.put(`/admin/users/${userId}/reset-avatar`) }
}

// ═══════════════════════════════════════
// 内容管理
// ═══════════════════════════════════════
export const adminPostApi = {
  /** 帖子列表 */
  getList(params) { return api.get('/admin/posts', { params }) },
  /** 帖子详情（管理端） */
  getDetail(id) { return api.get(`/admin/posts/${id}`) },
  /** 更新帖子状态 */
  updateStatus(id, data) { return api.put(`/admin/posts/${id}/status`, data) },
  /** 批量下架/恢复 */
  batchUpdateStatus(ids, status, reason) { return api.put('/admin/posts/batch/status', { ids, status, reason }) },
  /** 批量置顶/取消 */
  batchSetTop(ids, isTop) { return api.put('/admin/posts/batch/top', { ids, isTop }) },
  /** 批量加精/取消 */
  batchSetEssence(ids, isEssence) { return api.put('/admin/posts/batch/essence', { ids, isEssence }) },
  /** 物理删除 */
  deletePost(id) { return api.delete(`/admin/posts/${id}`) }
}

// ═══════════════════════════════════════
// 评论管理
// ═══════════════════════════════════════
export const adminCommentApi = {
  /** 评论列表 */
  getList(params) { return api.get('/admin/comments', { params }) },
  /** 评论详情 */
  getDetail(id) { return api.get(`/admin/comments/${id}`) },
  /** 删除评论 */
  deleteComment(id) { return api.delete(`/admin/comments/${id}`) },
  /** 恢复评论 */
  restoreComment(id) { return api.put(`/admin/comments/${id}/restore`) },
  /** 批量删除 */
  batchDelete(ids) { return api.put('/admin/comments/batch/delete', { ids }) },
  /** 批量恢复 */
  batchRestore(ids) { return api.put('/admin/comments/batch/restore', { ids }) }
}

// ═══════════════════════════════════════
// 举报处理
// ═══════════════════════════════════════
export const adminReportApi = {
  /** 举报列表 */
  getList(params) { return api.get('/admin/reports', { params }) },
  /** 举报详情 */
  getDetail(id) { return api.get(`/admin/reports/${id}`) },
  /** 处理举报 */
  handle(id, data) { return api.put(`/admin/reports/${id}/handle`, data) },
  /** 举报统计 */
  getStats() { return api.get('/admin/reports/stats') }
}

// ═══════════════════════════════════════
// 标签管理
// ═══════════════════════════════════════
export const adminTagApi = {
  /** 标签列表 */
  getList(params) { return api.get('/admin/tags', { params }) },
  /** 创建标签 */
  create(data) { return api.post('/admin/tags', data) },
  /** 编辑标签 */
  update(id, data) { return api.put(`/admin/tags/${id}`, data) },
  /** 启用/禁用 */
  toggleStatus(id, status) { return api.put(`/admin/tags/${id}/status`, { status }) }
}

// ═══════════════════════════════════════
// 通知管理
// ═══════════════════════════════════════
export const adminNotificationApi = {
  /** 发送通知 */
  send(data) { return api.post('/admin/notifications', data) },
  /** 通知记录 */
  getRecords(params) { return api.get('/admin/notifications', { params }) }
}

// ═══════════════════════════════════════
// 搜索运营
// ═══════════════════════════════════════
export const adminSearchApi = {
  getHotKeywords(range) { return api.get('/admin/search/hot-keywords', { params: { range } }) },
  getTrend(range) { return api.get('/admin/search/trend', { params: { range } }) },
  getHistory(params) { return api.get('/admin/search/history', { params }) },
  cleanKeywords(keyword) { return api.delete('/admin/search/keywords', { data: { keyword } }) }
}

// ═══════════════════════════════════════
// 数据看板
// ═══════════════════════════════════════
export const adminDashboardApi = {
  getOverview() { return api.get('/admin/dashboard/overview') },
  getTrends(range) { return api.get('/admin/dashboard/trends', { params: { range } }) },
  getRankings(type) { return api.get('/admin/dashboard/rankings', { params: { type } }) }
}
