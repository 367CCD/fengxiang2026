<template>
  <div class="admin-page">
    <div class="page-title">搜索运营管理</div>

    <el-tabs v-model="activeTab">
      <!-- 热词统计 -->
      <el-tab-pane label="搜索热词" name="hot">
        <div class="range-bar">
          <el-radio-group v-model="hotRange" @change="fetchHotKeywords">
            <el-radio-button value="today">今日</el-radio-button>
            <el-radio-button value="7d">近7天</el-radio-button>
            <el-radio-button value="30d">近30天</el-radio-button>
          </el-radio-group>
        </div>
        <el-table :data="hotKeywords" v-loading="hotLoading" stripe>
          <el-table-column type="index" label="#" width="60" />
          <el-table-column prop="keyword" label="关键词" min-width="200" />
          <el-table-column prop="count" label="搜索次数" width="120" sortable />
        </el-table>
      </el-tab-pane>

      <!-- 历史管理 -->
      <el-tab-pane label="搜索历史" name="history">
        <div class="filter-bar">
          <el-input v-model="historyUserId" placeholder="用户ID" clearable style="width:140px" />
          <el-input v-model="historyKeyword" placeholder="关键词" clearable style="width:200px" @keyup.enter="searchHistory" />
          <el-button type="primary" @click="searchHistory">查询</el-button>
          <el-popconfirm title="确定清理该关键词？" @confirm="doClean">
            <template #reference>
              <el-button type="danger" :disabled="!historyKeyword">清理关键词</el-button>
            </template>
          </el-popconfirm>
        </div>
        <el-table :data="historyList" v-loading="historyLoading" stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="userId" label="用户ID" width="80" />
          <el-table-column prop="username" label="用户名" width="100" />
          <el-table-column prop="keyword" label="关键词" min-width="200" />
          <el-table-column prop="updatedAt" label="最后搜索" width="160" />
        </el-table>
        <div class="pagination-wrap">
          <el-pagination v-model:current-page="historyPage" v-model:page-size="historyPageSize" :page-sizes="[10,20,50]" :total="historyTotal" layout="total,sizes,prev,pager,next" @change="fetchHistory" />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminSearchApi } from '../../api/admin'

const activeTab = ref('hot')

// ── 热词 ──
const hotRange = ref('7d'); const hotKeywords = ref([]); const hotLoading = ref(false)
async function fetchHotKeywords() {
  hotLoading.value = true
  try { hotKeywords.value = await adminSearchApi.getHotKeywords(hotRange.value) || [] } catch (e) { ElMessage.error(e.message) } finally { hotLoading.value = false }
}

// ── 历史 ──
const historyUserId = ref(''); const historyKeyword = ref('')
const historyList = ref([]); const historyLoading = ref(false)
const historyPage = ref(1); const historyPageSize = ref(20); const historyTotal = ref(0)
async function fetchHistory() {
  historyLoading.value = true
  try {
    const params = { page: historyPage.value, pageSize: historyPageSize.value }
    if (historyUserId.value) params.userId = Number(historyUserId.value)
    if (historyKeyword.value) params.keyword = historyKeyword.value
    const res = await adminSearchApi.getHistory(params)
    historyList.value = res.records || []; historyTotal.value = res.total || 0
  } catch (e) { ElMessage.error(e.message) } finally { historyLoading.value = false }
}
function searchHistory() { historyPage.value = 1; fetchHistory() }
async function doClean() {
  try { await adminSearchApi.cleanKeywords(historyKeyword.value); ElMessage.success('已清理'); fetchHistory() } catch (e) { ElMessage.error(e.message) }
}

onMounted(() => { fetchHotKeywords(); fetchHistory() })
</script>

<style scoped>
.admin-page { max-width: 1200px; }
.page-title { font-size: 22px; font-weight: 600; margin-bottom: 20px; color: #303133; }
.range-bar { margin-bottom: 16px; }
.filter-bar { display: flex; gap: 10px; margin-bottom: 16px; align-items: center; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
