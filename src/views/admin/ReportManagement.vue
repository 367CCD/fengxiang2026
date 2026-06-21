<template>
  <div class="admin-page">
    <div class="page-title">举报处理</div>

    <!-- 统计卡片 -->
    <div class="stats-row" v-if="stats">
      <div class="stat-card"><div class="stat-num">{{ stats.totalCount }}</div><div class="stat-label">举报总数</div></div>
      <div class="stat-card pending"><div class="stat-num">{{ stats.pendingCount }}</div><div class="stat-label">待处理</div></div>
      <div class="stat-card done"><div class="stat-num">{{ stats.handledCount }}</div><div class="stat-label">已处理</div></div>
      <div class="stat-card reject"><div class="stat-num">{{ stats.rejectedCount }}</div><div class="stat-label">已驳回</div></div>
      <div class="stat-card"><div class="stat-num">{{ stats.avgHandleHours?.toFixed(1) || 0 }}h</div><div class="stat-label">平均处理时长</div></div>
      <div class="stat-card"><div class="stat-num">{{ (stats.passRate * 100).toFixed(0) }}%</div><div class="stat-label">通过率</div></div>
    </div>

    <!-- 筛选 -->
    <div class="filter-bar">
      <el-select v-model="filters.status" placeholder="状态" clearable style="width:120px">
        <el-option label="待处理" :value="0" />
        <el-option label="已处理" :value="1" />
        <el-option label="已驳回" :value="2" />
      </el-select>
      <el-select v-model="filters.targetType" placeholder="举报类型" clearable style="width:120px">
        <el-option label="帖子" :value="1" />
        <el-option label="评论" :value="2" />
        <el-option label="用户" :value="3" />
      </el-select>
      <el-button type="primary" @click="search">筛选</el-button>
      <el-button @click="refreshStats" :loading="statsLoading">刷新统计</el-button>
    </div>

    <!-- 表格 -->
    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="类型" width="80">
        <template #default="{row}">{{ {1:'帖子',2:'评论',3:'用户'}[row.targetType] }}</template>
      </el-table-column>
      <el-table-column prop="targetSummary" label="举报对象" min-width="150" show-overflow-tooltip />
      <el-table-column prop="reporterName" label="举报人" width="100" />
      <el-table-column prop="reason" label="举报原因" min-width="150" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{row}">
          <el-tag v-if="row.status === 0" type="warning" size="small">待处理</el-tag>
          <el-tag v-else-if="row.status === 1" type="success" size="small">已处理</el-tag>
          <el-tag v-else-if="row.status === 2" type="info" size="small">已驳回</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="handlerName" label="处理人" width="100" />
      <el-table-column prop="createdAt" label="提交时间" width="160" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{row}">
          <el-button v-if="row.status === 0" text size="small" type="primary" @click="openHandle(row)">处理</el-button>
          <el-button text size="small" @click="viewDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :page-sizes="[10,20,50]" :total="total" layout="total,sizes,prev,pager,next" @change="fetchList" />
    </div>

    <!-- 处理弹窗 -->
    <el-dialog v-model="handleVisible" title="处理举报" width="560px">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="举报ID">{{ handleTarget?.id }}</el-descriptions-item>
        <el-descriptions-item label="举报人">{{ handleTarget?.reporterName }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ {1:'帖子',2:'评论',3:'用户'}[handleTarget?.targetType] }}</el-descriptions-item>
        <el-descriptions-item label="对象摘要">{{ handleTarget?.targetSummary }}</el-descriptions-item>
        <el-descriptions-item label="原因" :span="2">{{ handleTarget?.reason }}</el-descriptions-item>
      </el-descriptions>
      <el-divider />
      <el-form label-width="80px">
        <el-form-item label="处理结果">
          <el-radio-group v-model="handleForm.status">
            <el-radio :value="1">通过</el-radio>
            <el-radio :value="2">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="处罚措施" v-if="handleForm.status === 1">
          <el-select v-model="handleForm.penaltyAction" placeholder="选择处罚">
            <el-option label="下架帖子" :value="1" v-if="handleTarget?.targetType === 1" />
            <el-option label="删除评论" :value="2" v-if="handleTarget?.targetType === 2" />
            <el-option label="禁言用户" :value="3" />
            <el-option label="禁用账号" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="禁言时长" v-if="handleForm.penaltyAction === 3">
          <el-radio-group v-model="handleForm.penaltyDuration">
            <el-radio :value="1">1天</el-radio><el-radio :value="3">3天</el-radio><el-radio :value="7">7天</el-radio><el-radio :value="30">30天</el-radio><el-radio :value="null">永久</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="处理备注">
          <el-input v-model="handleForm.remark" type="textarea" :rows="2" placeholder="处理备注" />
        </el-form-item>
        <el-form-item label="处罚原因" v-if="handleForm.penaltyAction">
          <el-input v-model="handleForm.penaltyReason" placeholder="处罚原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" :loading="handleLoading" @click="doHandle">确认处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminReportApi } from '../../api/admin'

const list = ref([]); const loading = ref(false)
const page = ref(1); const pageSize = ref(20); const total = ref(0)
const filters = reactive({ status: null, targetType: null })
const stats = ref(null); const statsLoading = ref(false)

async function fetchList() {
  loading.value = true
  try { const res = await adminReportApi.getList({ status: filters.status, targetType: filters.targetType, page: page.value, pageSize: pageSize.value }); list.value = res.records || []; total.value = res.total || 0 } catch (e) { ElMessage.error(e.message) } finally { loading.value = false }
}
async function refreshStats() {
  statsLoading.value = true
  try { stats.value = await adminReportApi.getStats() } catch (e) { ElMessage.error(e.message) } finally { statsLoading.value = false }
}
function search() { page.value = 1; fetchList() }

const handleVisible = ref(false); const handleTarget = ref(null); const handleLoading = ref(false)
const handleForm = reactive({ status: 1, remark: '', penaltyAction: null, penaltyDuration: 7, penaltyReason: '' })
function openHandle(row) {
  handleTarget.value = row
  handleForm.status = 1; handleForm.remark = ''; handleForm.penaltyAction = null; handleForm.penaltyDuration = 7; handleForm.penaltyReason = ''
  handleVisible.value = true
}
async function doHandle() {
  handleLoading.value = true
  try {
    await adminReportApi.handle(handleTarget.value.id, {
      status: handleForm.status,
      handleRemark: handleForm.remark,
      penaltyAction: handleForm.penaltyAction,
      penaltyDuration: handleForm.penaltyDuration,
      penaltyReason: handleForm.penaltyReason
    })
    ElMessage.success('处理完成')
    handleVisible.value = false; fetchList(); refreshStats()
  } catch (e) { ElMessage.error(e.message) } finally { handleLoading.value = false }
}
function viewDetail(row) {
  ElMessage.info(`举报详情 — 类型:${row.targetType}, 对象:${row.targetSummary}, 原因:${row.reason}`)
}

onMounted(() => { fetchList(); refreshStats() })
</script>

<style scoped>
.admin-page { max-width: 1500px; }
.page-title { font-size: 22px; font-weight: 600; margin-bottom: 20px; color: #303133; }
.stats-row { display: flex; gap: 16px; margin-bottom: 20px; flex-wrap: wrap; }
.stat-card { flex: 1; min-width: 130px; background: #fff; border-radius: 10px; padding: 20px 16px; text-align: center; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.stat-card.pending { border-left: 4px solid #e6a23c; }
.stat-card.done { border-left: 4px solid #67c23a; }
.stat-card.reject { border-left: 4px solid #909399; }
.stat-num { font-size: 28px; font-weight: 700; color: #303133; }
.stat-label { font-size: 13px; color: #909399; margin-top: 4px; }
.filter-bar { display: flex; gap: 10px; margin-bottom: 16px; align-items: center; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
