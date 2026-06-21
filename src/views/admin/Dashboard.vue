<template>
  <div class="admin-page">
    <div class="page-title">数据统计看板</div>

    <!-- 核心数据卡片 -->
    <div class="section-title">核心数据概览</div>
    <div class="stats-grid" v-if="overview">
      <!-- 用户 -->
      <el-card shadow="never"><div class="card-group-label">用户数据</div>
        <div class="card-row"><span>总用户数</span><strong>{{ overview.userStats?.total || 0 }}</strong></div>
        <div class="card-row"><span>今日新增</span><strong class="green">{{ overview.userStats?.todayNew || 0 }}</strong></div>
        <div class="card-row"><span>禁言中</span><strong class="orange">{{ overview.userStats?.muted || 0 }}</strong></div>
        <div class="card-row"><span>已禁用</span><strong class="red">{{ overview.userStats?.disabled || 0 }}</strong></div>
      </el-card>
      <!-- 内容 -->
      <el-card shadow="never"><div class="card-group-label">内容数据</div>
        <div class="card-row"><span>总帖子数</span><strong>{{ overview.postStats?.total || 0 }}</strong></div>
        <div class="card-row"><span>今日新增</span><strong class="green">{{ overview.postStats?.todayNew || 0 }}</strong></div>
        <div class="card-row"><span>图文 / 视频</span><strong>{{ overview.postStats?.image || 0 }} / {{ overview.postStats?.video || 0 }}</strong></div>
        <div class="card-row"><span>违规下架</span><strong class="red">{{ overview.postStats?.removed || 0 }}</strong></div>
      </el-card>
      <!-- 互动 -->
      <el-card shadow="never"><div class="card-group-label">今日互动</div>
        <div class="card-row"><span>点赞</span><strong>{{ overview.interactStats?.todayLikes || 0 }}</strong></div>
        <div class="card-row"><span>评论</span><strong>{{ overview.interactStats?.todayComments || 0 }}</strong></div>
        <div class="card-row"><span>收藏</span><strong>{{ overview.interactStats?.todayCollects || 0 }}</strong></div>
      </el-card>
      <!-- 举报 -->
      <el-card shadow="never"><div class="card-group-label">举报数据</div>
        <div class="card-row"><span>今日新增</span><strong>{{ overview.reportStats?.todayNew || 0 }}</strong></div>
        <div class="card-row"><span>待处理</span><strong class="orange">{{ overview.reportStats?.pending || 0 }}</strong></div>
        <div class="card-row"><span>处理率</span><strong>{{ overview.reportStats?.handleRate || '0%' }}</strong></div>
        <div class="card-row"><span>通过率</span><strong>{{ overview.reportStats?.passRate || '0%' }}</strong></div>
      </el-card>
    </div>
    <div v-else class="loading-hint"><el-icon class="spin"><Loading /></el-icon> 加载中...</div>

    <!-- 趋势 -->
    <div class="section-title">数据趋势</div>
    <div class="range-bar">
      <el-radio-group v-model="trendRange" @change="fetchTrends">
        <el-radio-button value="7d">近7天</el-radio-button>
        <el-radio-button value="30d">近30天</el-radio-button>
      </el-radio-group>
    </div>
    <div class="trend-grid" v-if="trends">
      <el-card shadow="never"><div class="card-label">用户新增趋势</div>
        <div class="mini-chart">{{ summaryText(trends.userTrend) }}</div>
      </el-card>
      <el-card shadow="never"><div class="card-label">帖子发布趋势</div>
        <div class="mini-chart">{{ summaryText(trends.postTrend) }}</div>
      </el-card>
      <el-card shadow="never"><div class="card-label">互动量趋势</div>
        <div class="mini-chart">{{ summaryText(trends.interactTrend) }}</div>
      </el-card>
      <el-card shadow="never"><div class="card-label">举报量趋势</div>
        <div class="mini-chart">{{ summaryText(trends.reportTrend) }}</div>
      </el-card>
    </div>

    <!-- 排行榜 -->
    <div class="section-title">排行榜</div>
    <el-tabs v-model="rankType" @tab-change="fetchRankings">
      <el-tab-pane label="热门帖子" name="hotPosts">
        <el-table :data="rankings" stripe size="small">
          <el-table-column type="index" label="#" width="50" />
          <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
          <el-table-column prop="username" label="作者" width="100" />
          <el-table-column prop="likeCount" label="点赞" width="70" />
          <el-table-column prop="commentCount" label="评论" width="70" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="活跃创作者" name="activeCreators">
        <el-table :data="rankings" stripe size="small">
          <el-table-column type="index" label="#" width="50" />
          <el-table-column prop="username" label="用户名" width="120" />
          <el-table-column prop="postCount" label="发帖数" width="80" />
          <el-table-column prop="totalLikes" label="总获赞" width="90" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="热门标签" name="hotTags">
        <el-table :data="rankings" stripe size="small">
          <el-table-column type="index" label="#" width="50" />
          <el-table-column prop="name" label="标签名" width="120" />
          <el-table-column prop="postCount" label="帖子数" width="80" />
          <el-table-column prop="followCount" label="关注数" width="80" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="高频违规" name="topViolators">
        <el-table :data="rankings" stripe size="small">
          <el-table-column type="index" label="#" width="50" />
          <el-table-column prop="username" label="用户名" width="120" />
          <el-table-column prop="reportCount" label="被举报次数" width="100" />
          <el-table-column prop="reporterCount" label="举报人数" width="80" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { adminDashboardApi } from '../../api/admin'

const overview = ref(null)
const trends = ref(null)
const trendRange = ref('7d')
const rankType = ref('hotPosts')
const rankings = ref([])

async function fetchOverview() {
  try { overview.value = await adminDashboardApi.getOverview() } catch (_) {}
}
async function fetchTrends() {
  try { trends.value = await adminDashboardApi.getTrends(trendRange.value) } catch (_) {}
}
async function fetchRankings() {
  try { rankings.value = await adminDashboardApi.getRankings(rankType.value) || [] } catch (_) { rankings.value = [] }
}

function summaryText(arr) {
  if (!arr || !arr.length) return '暂无数据'
  const total = arr.reduce((s, i) => s + (i.count || 0), 0)
  return `共 ${total} 条 · ${arr.length} 天数据`
}

onMounted(() => { fetchOverview(); fetchTrends(); fetchRankings() })
</script>

<style scoped>
.admin-page { max-width: 1400px; }
.page-title { font-size: 22px; font-weight: 600; margin-bottom: 20px; color: #303133; }
.section-title { font-size: 16px; font-weight: 600; margin: 24px 0 12px; color: #303133; }
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.card-group-label { font-size: 13px; font-weight: 600; color: #909399; margin-bottom: 10px; border-bottom: 1px solid #f0f0f0; padding-bottom: 8px; }
.card-row { display: flex; justify-content: space-between; padding: 3px 0; font-size: 13px; }
.card-row span { color: #909399; }
.card-row strong { color: #303133; }
.green { color: #67c23a !important; }
.orange { color: #e6a23c !important; }
.red { color: #f56c6c !important; }
.loading-hint { text-align: center; padding: 40px; color: #909399; }
.spin { animation: spin 1s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.range-bar { margin-bottom: 16px; }
.trend-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.card-label { font-size: 13px; color: #909399; margin-bottom: 8px; }
.mini-chart { font-size: 14px; color: #606266; }
</style>
