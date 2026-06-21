<template>
  <div class="admin-page">
    <div class="page-title">通知管理</div>

    <!-- 发送通知卡片 -->
    <el-card class="send-card" shadow="never">
      <template #header><span style="font-weight:600">发送系统通知</span></template>
      <el-form label-width="80px">
        <el-form-item label="发送方式">
          <el-radio-group v-model="sendForm.broadcast">
            <el-radio :value="false">定向发送</el-radio>
            <el-radio :value="true">全站广播</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="目标用户" v-if="!sendForm.broadcast">
          <el-input v-model="recipientIdsStr" placeholder="输入用户ID，多个用逗号分隔" />
        </el-form-item>
        <el-form-item label="通知内容">
          <el-input v-model="sendForm.content" type="textarea" :rows="3" placeholder="请输入通知内容" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="sendLoading" @click="doSend">发送通知</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-divider />

    <!-- 历史记录 -->
    <div class="section-title">通知记录</div>
    <div class="filter-bar">
      <el-select v-model="recordType" placeholder="通知类型" clearable style="width:140px">
        <el-option label="系统通知" :value="7" />
        <el-option label="点赞帖子" :value="1" />
        <el-option label="评论" :value="3" />
        <el-option label="关注" :value="5" />
      </el-select>
      <el-input v-model="recordUserId" placeholder="接收用户ID" clearable style="width:140px" />
      <el-date-picker v-model="recordDateRange" type="daterange" range-separator="至" start-placeholder="开始" end-placeholder="结束" format="YYYY-MM-DD" value-format="YYYY-MM-DD" style="width:240px" />
      <el-button type="primary" @click="searchRecords">查询</el-button>
    </div>

    <el-table :data="recordList" v-loading="recordLoading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="类型" width="100">
        <template #default="{row}">
          {{ {1:'点赞帖子',2:'点赞评论',3:'评论',4:'回复',5:'关注',6:'收藏',7:'系统通知'}[row.type] || row.type }}
        </template>
      </el-table-column>
      <el-table-column prop="content" label="内容" min-width="250" show-overflow-tooltip />
      <el-table-column prop="senderName" label="发送者" width="100" />
      <el-table-column prop="recipientName" label="接收者" width="100" />
      <el-table-column label="已读" width="70">
        <template #default="{row}">{{ row.isRead ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column prop="createdAt" label="时间" width="160" />
    </el-table>

    <div class="pagination-wrap">
      <el-pagination v-model:current-page="recordPage" v-model:page-size="recordPageSize" :page-sizes="[10,20,50]" :total="recordTotal" layout="total,sizes,prev,pager,next" @change="fetchRecords" />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminNotificationApi } from '../../api/admin'

// ── 发送 ──
const sendLoading = ref(false)
const sendForm = reactive({ broadcast: false, content: '' })
const recipientIdsStr = ref('')

async function doSend() {
  if (!sendForm.content.trim()) { ElMessage.warning('请输入通知内容'); return }
  sendLoading.value = true
  try {
    const data = {
      content: sendForm.content,
      broadcast: sendForm.broadcast,
      type: 7
    }
    if (!sendForm.broadcast) {
      data.recipientIds = recipientIdsStr.value.split(',').map(s => Number(s.trim())).filter(Boolean)
      if (!data.recipientIds.length) { ElMessage.warning('请输入目标用户ID'); sendLoading.value = false; return }
    }
    await adminNotificationApi.send(data)
    ElMessage.success(sendForm.broadcast ? '全站广播已发送' : '定向通知已发送')
    sendForm.content = ''; recipientIdsStr.value = ''
    fetchRecords()
  } catch (e) { ElMessage.error(e.message) } finally { sendLoading.value = false }
}

// ── 记录 ──
const recordList = ref([]); const recordLoading = ref(false)
const recordPage = ref(1); const recordPageSize = ref(20); const recordTotal = ref(0)
const recordType = ref(null); const recordUserId = ref(''); const recordDateRange = ref(null)

async function fetchRecords() {
  recordLoading.value = true
  try {
    const params = {
      type: recordType.value, page: recordPage.value, pageSize: recordPageSize.value,
      userId: recordUserId.value ? Number(recordUserId.value) : undefined,
      startTime: recordDateRange.value?.[0] || undefined,
      endTime: recordDateRange.value?.[1] ? recordDateRange.value[1] + ' 23:59:59' : undefined
    }
    const res = await adminNotificationApi.getRecords(params)
    recordList.value = res.records || []; recordTotal.value = res.total || 0
  } catch (e) { ElMessage.error(e.message) } finally { recordLoading.value = false }
}
function searchRecords() { recordPage.value = 1; fetchRecords() }

onMounted(() => fetchRecords())
</script>

<style scoped>
.admin-page { max-width: 1400px; }
.page-title { font-size: 22px; font-weight: 600; margin-bottom: 20px; color: #303133; }
.send-card { margin-bottom: 8px; }
.section-title { font-size: 18px; font-weight: 600; margin-bottom: 12px; color: #303133; }
.filter-bar { display: flex; gap: 10px; margin-bottom: 16px; align-items: center; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
