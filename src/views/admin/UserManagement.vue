<template>
  <div class="admin-page">
    <div class="page-title">用户管理</div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-input v-model="filters.keyword" placeholder="用户名/手机/邮箱" clearable style="width:200px" @keyup.enter="search" />
      <el-select v-model="filters.status" placeholder="账号状态" clearable style="width:130px">
        <el-option label="正常" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-select v-model="filters.isMuted" placeholder="禁言状态" clearable style="width:130px">
        <el-option label="正常" :value="0" />
        <el-option label="禁言中" :value="1" />
      </el-select>
      <el-select v-model="filters.role" placeholder="角色" clearable style="width:130px">
        <el-option label="普通用户" :value="0" />
        <el-option label="管理员" :value="1" />
      </el-select>
      <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="注册开始" end-placeholder="注册结束" format="YYYY-MM-DD" value-format="YYYY-MM-DD" style="width:260px" />
      <el-button type="primary" @click="search">搜索</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <!-- 批量操作栏 -->
    <div class="batch-bar" v-if="selectedIds.length">
      <span class="selected-count">已选 {{ selectedIds.length }} 项</span>
      <el-button @click="batchEnable">批量启用</el-button>
      <el-button type="danger" @click="batchDisable">批量禁用</el-button>
      <el-button type="warning" @click="openMuteDialog(selectedIds)">批量禁言</el-button>
      <el-button @click="batchUnmute">批量解禁</el-button>
    </div>

    <!-- 表格 -->
    <el-table :data="list" v-loading="loading" @selection-change="onSelectionChange" stripe>
      <el-table-column type="selection" width="45" />
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column label="头像" width="70">
        <template #default="{row}">
          <el-tooltip content="点击清除违规头像" placement="top" :show-after="400">
            <el-avatar :size="32" :src="row.avatarUrl" :class="{ 'clickable-avatar': row.avatarUrl }" @click="resetAvatar(row)">{{ row.username?.charAt(0) }}</el-avatar>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="账号状态" width="90">
        <template #default="{row}">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="禁言状态" width="130">
        <template #default="{row}">
          <template v-if="row.isMuted === 1">
            <el-tag v-if="!row.muteEndTime" type="danger" size="small">永久禁言</el-tag>
            <el-tag v-else-if="new Date(row.muteEndTime) > new Date()" type="warning" size="small">禁言中</el-tag>
            <el-tag v-else type="info" size="small">已到期</el-tag>
          </template>
          <span v-else class="text-muted">正常</span>
        </template>
      </el-table-column>
      <el-table-column prop="role" label="角色" width="90">
        <template #default="{row}">{{ row.role === 1 ? '管理员' : '普通用户' }}</template>
      </el-table-column>
      <el-table-column prop="createdAt" label="注册时间" width="170" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{row}">
          <el-button text size="small" type="primary" @click="openMuteDialog([row.id])" v-if="row.isMuted !== 1">禁言</el-button>
          <el-button text size="small" type="success" @click="unmuteSingle(row.id)" v-else>解禁</el-button>
          <el-button text size="small" @click="showMuteHistory(row)">禁言记录</el-button>
          <el-button text size="small" type="warning" @click="resetAvatar(row)">重置头像</el-button>
          <el-button text size="small" :type="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrap">
      <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :page-sizes="[10,20,50]" :total="total" layout="total,sizes,prev,pager,next" @change="fetchList" />
    </div>

    <!-- 禁言弹窗 -->
    <el-dialog v-model="muteVisible" title="设置禁言" width="460px">
      <el-form label-width="80px">
        <el-form-item label="禁言用户"><span v-for="id in muteTargetIds" :key="id" class="tag-chip">{{ id }}</span></el-form-item>
        <el-form-item label="禁言时长">
          <el-radio-group v-model="muteForm.duration">
            <el-radio :value="1">1天</el-radio>
            <el-radio :value="3">3天</el-radio>
            <el-radio :value="7">7天</el-radio>
            <el-radio :value="30">30天</el-radio>
            <el-radio :value="null">永久</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="禁言原因">
          <el-input v-model="muteForm.reason" type="textarea" :rows="2" placeholder="请输入禁言原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="muteVisible = false">取消</el-button>
        <el-button type="primary" :loading="muteLoading" @click="doMute">确认禁言</el-button>
      </template>
    </el-dialog>


    <!-- 禁言历史弹窗 -->
    <el-dialog v-model="historyVisible" title="禁言历史记录" width="700px">
      <el-table :data="muteHistoryList" v-loading="historyLoading" size="small">
        <el-table-column prop="operatorName" label="操作人" width="100" />
        <el-table-column label="操作" width="70">
          <template #default="{row}">{{ row.action === 1 ? '禁言' : '解封' }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="150" show-overflow-tooltip />
        <el-table-column prop="muteDuration" label="时长" width="80" />
        <el-table-column prop="muteEndTime" label="到期时间" width="160" />
        <el-table-column label="解封方式" width="100">
          <template #default="{row}">{{ row.unmuteType === 1 ? '手动' : row.unmuteType === 2 ? '系统懒解封' : '-' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="160" />
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="historyPage" :page-size="10" :total="historyTotal" layout="total,prev,pager,next" @change="fetchMuteHistory" small />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminUserApi } from '../../api/admin'

const list = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const selectedIds = ref([])
const dateRange = ref(null)

const filters = reactive({ keyword: '', status: null, isMuted: null, role: null })

function buildParams() {
  return {
    keyword: filters.keyword || undefined,
    status: filters.status,
    isMuted: filters.isMuted,
    role: filters.role,
    startTime: dateRange.value?.[0] || undefined,
    endTime: dateRange.value?.[1] ? dateRange.value[1] + ' 23:59:59' : undefined,
    page: page.value,
    pageSize: pageSize.value
  }
}

async function fetchList() {
  loading.value = true
  try {
    const res = await adminUserApi.getList(buildParams())
    list.value = res.records || []
    total.value = res.total || 0
  } catch (e) {
    ElMessage.error(e.message)
  } finally { loading.value = false }
}

function search() { page.value = 1; fetchList() }
function reset() {
  filters.keyword = ''; filters.status = null; filters.isMuted = null; filters.role = null
  dateRange.value = null; page.value = 1; fetchList()
}

function onSelectionChange(rows) { selectedIds.value = rows.map(r => r.id) }

async function toggleStatus(row) {
  try {
    await adminUserApi.batchUpdateStatus([row.id], row.status === 1 ? 0 : 1)
    ElMessage.success('操作成功')
    fetchList()
  } catch (e) { ElMessage.error(e.message) }
}

async function batchEnable() {
  try { await adminUserApi.batchUpdateStatus(selectedIds.value, 1); ElMessage.success('批量启用成功'); fetchList() } catch (e) { ElMessage.error(e.message) }
}
async function batchDisable() {
  try { await ElMessageBox.confirm('确认禁用所选用户？') } catch { return }
  try { await adminUserApi.batchUpdateStatus(selectedIds.value, 0); ElMessage.success('批量禁用成功'); fetchList() } catch (e) { ElMessage.error(e.message) }
}
async function batchUnmute() {
  try { await adminUserApi.unmuteUsers(selectedIds.value); ElMessage.success('批量解禁成功'); fetchList() } catch (e) { ElMessage.error(e.message) }
}
async function unmuteSingle(id) {
  try { await adminUserApi.unmuteUsers([id]); ElMessage.success('解禁成功'); fetchList() } catch (e) { ElMessage.error(e.message) }
}

// ── 禁言弹窗 ──
const muteVisible = ref(false)
const muteTargetIds = ref([])
const muteLoading = ref(false)
const muteForm = reactive({ duration: 7, reason: '' })
function openMuteDialog(ids) { muteTargetIds.value = ids; muteForm.duration = 7; muteForm.reason = ''; muteVisible.value = true }
async function doMute() {
  muteLoading.value = true
  try {
    await adminUserApi.muteUsers({ userIds: muteTargetIds.value, reason: muteForm.reason, duration: muteForm.duration })
    ElMessage.success('禁言成功')
    muteVisible.value = false
    fetchList()
  } catch (e) { ElMessage.error(e.message) } finally { muteLoading.value = false }
}

// ── 重置头像 ──
async function resetAvatar(row) {
  if (!row.avatarUrl) { ElMessage.info('该用户未设置头像'); return }
  try {
    await ElMessageBox.confirm(`确认清除用户「${row.username}」的头像？`, '重置头像', { confirmButtonText: '确认清除', type: 'warning' })
  } catch { return }
  try {
    await adminUserApi.resetAvatar(row.id)
    ElMessage.success('头像已清除')
    fetchList()
  } catch (e) { ElMessage.error(e.message) }
}

// ── 禁言历史 ──
const historyVisible = ref(false)
const historyLoading = ref(false)
const muteHistoryList = ref([])
const historyPage = ref(1)
const historyTotal = ref(0)
const historyUserId = ref(null)
function showMuteHistory(row) { historyUserId.value = row.id; historyPage.value = 1; fetchMuteHistory(); historyVisible.value = true }
async function fetchMuteHistory() {
  historyLoading.value = true
  try {
    const res = await adminUserApi.getMuteHistory(historyUserId.value, { page: historyPage.value, pageSize: 10 })
    muteHistoryList.value = res.records || []
    historyTotal.value = res.total || 0
  } catch (e) { ElMessage.error(e.message) } finally { historyLoading.value = false }
}

onMounted(() => fetchList())
</script>

<style scoped>
.admin-page { max-width: 1400px; }
.page-title { font-size: 22px; font-weight: 600; margin-bottom: 20px; color: #303133; }
.filter-bar { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 16px; align-items: center; }
.batch-bar { display: flex; align-items: center; gap: 10px; padding: 10px 16px; background: #fef0f0; border-radius: 8px; margin-bottom: 12px; }
.selected-count { font-weight: 500; color: #e6424a; margin-right: 8px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
.text-muted { color: #c0c4cc; font-size: 13px; }
.tag-chip { display: inline-block; background: #f0f2f5; padding: 2px 10px; border-radius: 4px; margin-right: 6px; font-size: 13px; }
.clickable-avatar { cursor: pointer; transition: opacity 0.2s; }
.clickable-avatar:hover { opacity: 0.7; }
</style>
