<template>
  <div class="admin-page">
    <div class="page-title">评论管理</div>

    <div class="filter-bar">
      <el-input v-model="filters.keyword" placeholder="搜索评论内容" clearable style="width:200px" @keyup.enter="search" />
      <el-input v-model="filters.userId" placeholder="发布者ID" clearable style="width:140px" />
      <el-input v-model="filters.postId" placeholder="所属帖子ID" clearable style="width:140px" />
      <el-select v-model="filters.status" placeholder="状态" clearable style="width:100px">
        <el-option label="正常" :value="1" />
        <el-option label="已删除" :value="0" />
      </el-select>
      <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始" end-placeholder="结束" format="YYYY-MM-DD" value-format="YYYY-MM-DD" style="width:240px" />
      <el-button type="primary" @click="search">搜索</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <div class="batch-bar" v-if="selectedIds.length">
      <span class="count">已选 {{ selectedIds.length }} 项</span>
      <el-button type="danger" @click="batchDelete">批量删除</el-button>
      <el-button type="success" @click="batchRestore">批量恢复</el-button>
    </div>

    <el-table :data="list" v-loading="loading" @selection-change="onSelect" stripe>
      <el-table-column type="selection" width="45" />
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="content" label="内容" min-width="250" show-overflow-tooltip />
      <el-table-column prop="username" label="发布者" width="100" />
      <el-table-column label="所属帖子" width="150" show-overflow-tooltip>
        <template #default="{row}">{{ row.postTitle || `帖子#${row.postId}` }}</template>
      </el-table-column>
      <el-table-column label="层级" width="70">
        <template #default="{row}">{{ row.parentId ? '二级回复' : '一级评论' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{row}">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '正常' : '已删除' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="likeCount" label="点赞" width="70" />
      <el-table-column prop="createdAt" label="发布时间" width="160" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{row}">
          <el-button text size="small" type="primary" @click="viewDetail(row)">查看</el-button>
          <el-button text size="small" type="danger" @click="delOne(row.id)" v-if="row.status === 1">删除</el-button>
          <el-button text size="small" type="success" @click="restoreOne(row.id)" v-else>恢复</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :page-sizes="[10,20,50]" :total="total" layout="total,sizes,prev,pager,next" @change="fetchList" />
    </div>

    <!-- 评论详情弹窗 -->
    <el-dialog v-model="detailVisible" title="评论详情" width="600px">
      <template v-if="detailComment">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="ID">{{ detailComment.id }}</el-descriptions-item>
          <el-descriptions-item label="发布者">{{ detailComment.username }}</el-descriptions-item>
          <el-descriptions-item label="所属帖子">{{ detailComment.postTitle || `#${detailComment.postId}` }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="detailComment.status === 1 ? 'success' : 'danger'" size="small">{{ detailComment.status === 1 ? '正常' : '已删除' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="层级">{{ detailComment.parentId ? '二级回复' : '一级评论' }}</el-descriptions-item>
          <el-descriptions-item label="点赞">{{ detailComment.likeCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="内容" :span="2">
            <div class="detail-content">{{ detailComment.content }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="发布时间" :span="2">{{ detailComment.createdAt }}</el-descriptions-item>
        </el-descriptions>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="detailComment?.status === 1" type="danger" @click="delFromDetail">删除</el-button>
        <el-button v-else type="success" @click="restoreFromDetail">恢复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminCommentApi } from '../../api/admin'

const list = ref([]); const loading = ref(false)
const page = ref(1); const pageSize = ref(20); const total = ref(0)
const selectedIds = ref([]); const dateRange = ref(null)
const filters = reactive({ keyword: '', userId: '', postId: '', status: null })

function buildParams() {
  return {
    keyword: filters.keyword || undefined,
    userId: filters.userId ? Number(filters.userId) : undefined,
    postId: filters.postId ? Number(filters.postId) : undefined,
    status: filters.status,
    startTime: dateRange.value?.[0] || undefined,
    endTime: dateRange.value?.[1] ? dateRange.value[1] + ' 23:59:59' : undefined,
    page: page.value, pageSize: pageSize.value
  }
}
async function fetchList() {
  loading.value = true
  try { const res = await adminCommentApi.getList(buildParams()); list.value = res.records || []; total.value = res.total || 0 } catch (e) { ElMessage.error(e.message) } finally { loading.value = false }
}
function search() { page.value = 1; fetchList() }
function reset() { filters.keyword = ''; filters.userId = ''; filters.postId = ''; filters.status = null; dateRange.value = null; page.value = 1; fetchList() }
function onSelect(rows) { selectedIds.value = rows.map(r => r.id) }

const detailVisible = ref(false); const detailComment = ref(null)
async function viewDetail(row) {
  try { detailComment.value = await adminCommentApi.getDetail(row.id); detailVisible.value = true } catch (e) { ElMessage.error(e.message) }
}
async function delOne(id) { try { await adminCommentApi.deleteComment(id); ElMessage.success('已删除'); fetchList() } catch (e) { ElMessage.error(e.message) } }
async function restoreOne(id) { try { await adminCommentApi.restoreComment(id); ElMessage.success('已恢复'); fetchList() } catch (e) { ElMessage.error(e.message) } }
async function delFromDetail() {
  if (!detailComment.value) return
  await delOne(detailComment.value.id); detailVisible.value = false
}
async function restoreFromDetail() {
  if (!detailComment.value) return
  await restoreOne(detailComment.value.id); detailVisible.value = false
}

async function batchDelete() {
  try { await ElMessageBox.confirm('确认批量删除？') } catch { return }
  try { await adminCommentApi.batchDelete(selectedIds.value); ElMessage.success('批量删除成功'); fetchList() } catch (e) { ElMessage.error(e.message) }
}
async function batchRestore() {
  try { await adminCommentApi.batchRestore(selectedIds.value); ElMessage.success('批量恢复成功'); fetchList() } catch (e) { ElMessage.error(e.message) }
}

onMounted(() => fetchList())
</script>

<style scoped>
.admin-page { max-width: 1500px; }
.page-title { font-size: 22px; font-weight: 600; margin-bottom: 20px; color: #303133; }
.filter-bar { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 16px; align-items: center; }
.batch-bar { display: flex; align-items: center; gap: 10px; padding: 10px 16px; background: #fef0f0; border-radius: 8px; margin-bottom: 12px; }
.count { font-weight: 500; color: #e6424a; margin-right: 8px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
.detail-content { white-space: pre-wrap; max-height: 200px; overflow-y: auto; line-height: 1.7; font-size: 14px; color: #333; }
</style>
