<template>
  <div class="admin-page">
    <div class="page-title">内容管理</div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-input v-model="filters.keyword" placeholder="搜索标题/内容" clearable style="width:200px" @keyup.enter="search" />
      <el-select v-model="filters.status" placeholder="状态" clearable style="width:120px">
        <el-option label="草稿" :value="0" />
        <el-option label="已发布" :value="1" />
        <el-option label="审核中" :value="2" />
        <el-option label="违规下架" :value="3" />
      </el-select>
      <el-select v-model="filters.type" placeholder="类型" clearable style="width:100px">
        <el-option label="图文" :value="1" />
        <el-option label="视频" :value="2" />
      </el-select>
      <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始" end-placeholder="结束" format="YYYY-MM-DD" value-format="YYYY-MM-DD" style="width:240px" />
      <el-button type="primary" @click="search">搜索</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <!-- 批量操作 -->
    <div class="batch-bar" v-if="selectedIds.length">
      <span class="count">已选 {{ selectedIds.length }} 项</span>
      <el-button type="danger" @click="batchRemove">批量下架</el-button>
      <el-button @click="batchRestore">批量恢复</el-button>
    </div>

    <!-- 表格 -->
    <el-table :data="list" v-loading="loading" @selection-change="onSelect" stripe>
      <el-table-column type="selection" width="45" />
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="封面" width="80">
        <template #default="{row}">
          <el-image v-if="row.coverUrl" :src="row.coverUrl" style="width:56px;height:56px;border-radius:6px" fit="cover" />
          <span v-else class="text-muted">无</span>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="username" label="作者" width="100" />
      <el-table-column label="类型" width="70">
        <template #default="{row}">{{ row.type === 1 ? '图文' : '视频' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{row}">
          <el-tag v-if="row.status === 1" type="success" size="small">已发布</el-tag>
          <el-tag v-else-if="row.status === 0" type="info" size="small">草稿</el-tag>
          <el-tag v-else-if="row.status === 2" type="warning" size="small">审核中</el-tag>
          <el-tag v-else-if="row.status === 3" type="danger" size="small">违规下架</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="互动" width="180">
        <template #default="{row}">
          <span class="stat">👁 {{ row.viewCount || 0 }}</span>
          <span class="stat">❤ {{ row.likeCount || 0 }}</span>
          <span class="stat">💬 {{ row.commentCount || 0 }}</span>
          <span class="stat">⭐ {{ row.collectCount || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="发布时间" width="160" />
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{row}">
          <el-button text size="small" type="primary" @click="viewDetail(row)">详情</el-button>
          <el-button text size="small" type="success" @click="approvePost(row)" v-if="row.status === 2">审核通过</el-button>
          <el-button text size="small" type="danger" @click="removePost(row)" v-if="row.status !== 3 && row.status !== 2">下架</el-button>
          <el-button text size="small" @click="restorePost(row)" v-if="row.status === 3">恢复</el-button>
          <el-popconfirm title="确定删除？" @confirm="deletePost(row.id)">
            <template #reference><el-button text size="small">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :page-sizes="[10,20,50]" :total="total" layout="total,sizes,prev,pager,next" @change="fetchList" />
    </div>

    <!-- 下架原因弹窗 -->
    <el-dialog v-model="removeVisible" title="违规下架" width="420px">
      <el-form>
        <el-form-item label="帖子ID">{{ removeTarget?.id }}</el-form-item>
        <el-form-item label="下架原因">
          <el-input v-model="removeReason" type="textarea" :rows="3" placeholder="请输入下架原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="removeVisible = false">取消</el-button>
        <el-button type="danger" @click="doRemove">确认下架</el-button>
      </template>
    </el-dialog>

    <!-- 帖子详情弹窗（审核用） -->
    <el-dialog v-model="detailVisible" title="帖子详情审核" width="720px" top="5vh" @closed="onDetailClosed">
      <template v-if="detailPost">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="ID">{{ detailPost.id }}</el-descriptions-item>
          <el-descriptions-item label="作者">{{ detailPost.username }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ detailPost.type === 1 ? '图文' : '视频' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag v-if="detailPost.status === 2" type="warning" size="small">审核中</el-tag>
            <el-tag v-else-if="detailPost.status === 1" type="success" size="small">已发布</el-tag>
            <el-tag v-else-if="detailPost.status === 3" type="danger" size="small">违规下架</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="标题" :span="2"><strong>{{ detailPost.title }}</strong></el-descriptions-item>
          <el-descriptions-item label="正文" :span="2">
            <div class="detail-content">{{ detailPost.content }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="位置">{{ detailPost.location || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发布时间">{{ detailPost.createdAt || '-' }}</el-descriptions-item>
        </el-descriptions>
        <!-- 视频播放 -->
        <div v-if="detailPost.type === 2 && detailPost.videoUrl" style="margin-top:12px">
          <video ref="videoPlayer" :src="detailPost.videoUrl" controls style="width:100%;max-height:400px;border-radius:8px;background:#000" />
        </div>
        <!-- 图文帖：展示所有图片 -->
        <div v-if="detailPost.type === 1 && detailPost.images?.length" style="margin-top:12px">
          <div class="image-grid">
            <el-image
              v-for="(img, i) in detailPost.images"
              :key="i"
              :src="img.url"
              :preview-src-list="detailPost.images.map(x => x.url)"
              :initial-index="i"
              fit="cover"
              style="width:140px;height:140px;border-radius:6px;cursor:pointer"
            />
          </div>
        </div>
        <!-- 封面图（无多图时回退） -->
        <div v-else-if="detailPost.coverUrl && !detailPost.images?.length" style="margin-top:12px">
          <el-image :src="detailPost.coverUrl" style="max-width:100%;max-height:300px;border-radius:8px" fit="contain" />
        </div>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="detailPost?.status === 2" type="success" @click="approveFromDetail">审核通过</el-button>
        <el-button v-if="detailPost?.status !== 3 && detailPost?.status !== 2" type="danger" @click="removeFromDetail">下架</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminPostApi } from '../../api/admin'

const list = ref([]); const loading = ref(false)
const page = ref(1); const pageSize = ref(20); const total = ref(0)
const selectedIds = ref([]); const dateRange = ref(null)
const filters = reactive({ keyword: '', status: null, type: null })

function buildParams() {
  return {
    keyword: filters.keyword || undefined, status: filters.status, type: filters.type,
    startTime: dateRange.value?.[0] || undefined,
    endTime: dateRange.value?.[1] ? dateRange.value[1] + ' 23:59:59' : undefined,
    page: page.value, pageSize: pageSize.value
  }
}
async function fetchList() {
  loading.value = true
  try { const res = await adminPostApi.getList(buildParams()); list.value = res.records || []; total.value = res.total || 0 } catch (e) { ElMessage.error(e.message) } finally { loading.value = false }
}
function search() { page.value = 1; fetchList() }
function reset() { filters.keyword = ''; filters.status = null; filters.type = null; dateRange.value = null; page.value = 1; fetchList() }
function onSelect(rows) { selectedIds.value = rows.map(r => r.id) }

const removeVisible = ref(false); const removeTarget = ref(null); const removeReason = ref('')
function removePost(row) { removeTarget.value = row; removeReason.value = ''; removeVisible.value = true }
async function doRemove() {
  try { await adminPostApi.updateStatus(removeTarget.value.id, { status: 3, reason: removeReason.value }); ElMessage.success('已下架'); removeVisible.value = false; fetchList() } catch (e) { ElMessage.error(e.message) }
}
async function restorePost(row) {
  try { await adminPostApi.updateStatus(row.id, { status: 1, reason: '管理员恢复' }); ElMessage.success('已恢复'); fetchList() } catch (e) { ElMessage.error(e.message) }
}
async function deletePost(id) {
  try { await adminPostApi.deletePost(id); ElMessage.success('已删除'); fetchList() } catch (e) { ElMessage.error(e.message) }
}
const detailVisible = ref(false); const detailPost = ref(null); const videoPlayer = ref(null)
function onDetailClosed() {
  if (videoPlayer.value) { videoPlayer.value.pause(); videoPlayer.value.currentTime = 0 }
  detailPost.value = null
}
async function viewDetail(row) {
  try { detailPost.value = await adminPostApi.getDetail(row.id); detailVisible.value = true } catch (e) { ElMessage.error(e.message) }
}
async function approvePost(row) {
  try { await adminPostApi.updateStatus(row.id, { status: 1, reason: '审核通过' }); ElMessage.success('已审核通过'); fetchList() } catch (e) { ElMessage.error(e.message) }
}
async function approveFromDetail() {
  await approvePost(detailPost.value); detailVisible.value = false
}
async function removeFromDetail() {
  if (!detailPost.value) return
  removeTarget.value = detailPost.value; removeReason.value = ''; detailVisible.value = false; removeVisible.value = true
}

async function batchRemove() {
  try { await ElMessageBox.confirm('确认批量下架？') } catch { return }
  try { await adminPostApi.batchUpdateStatus(selectedIds.value, 3, '批量下架'); ElMessage.success('批量下架成功'); fetchList() } catch (e) { ElMessage.error(e.message) }
}
async function batchRestore() {
  try { await adminPostApi.batchUpdateStatus(selectedIds.value, 1, '批量恢复'); ElMessage.success('批量恢复成功'); fetchList() } catch (e) { ElMessage.error(e.message) }
}

onMounted(() => fetchList())
</script>

<style scoped>
.admin-page { max-width: 1500px; }
.page-title { font-size: 22px; font-weight: 600; margin-bottom: 20px; color: #303133; }
.filter-bar { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 16px; align-items: center; }
.batch-bar { display: flex; align-items: center; gap: 10px; padding: 10px 16px; background: #f0f5ff; border-radius: 8px; margin-bottom: 12px; }
.count { font-weight: 500; color: #1a73e8; margin-right: 8px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
.text-muted { color: #c0c4cc; font-size: 13px; }
.stat { margin-right: 8px; font-size: 12px; color: #909399; }
.detail-content { white-space: pre-wrap; max-height: 300px; overflow-y: auto; line-height: 1.7; font-size: 14px; color: #333; }
.image-grid { display: flex; flex-wrap: wrap; gap: 8px; }
</style>
