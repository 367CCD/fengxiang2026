<template>
  <div class="admin-page">
    <div class="page-title-bar">
      <span class="page-title">标签管理</span>
      <el-button type="primary" @click="openCreate">新增标签</el-button>
    </div>

    <div class="filter-bar">
      <el-input v-model="keyword" placeholder="搜索标签名" clearable style="width:200px" @keyup.enter="search" />
      <el-select v-model="statusFilter" placeholder="状态" clearable style="width:120px">
        <el-option label="正常" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-select v-model="sortBy" placeholder="排序" style="width:140px" @change="search">
        <el-option label="按帖子数" value="postCount" />
        <el-option label="按关注数" value="followCount" />
        <el-option label="按创建时间" value="createdAt" />
      </el-select>
      <el-button type="primary" @click="search">搜索</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="标签名" width="120" />
      <el-table-column prop="description" label="描述" min-width="250" show-overflow-tooltip />
      <el-table-column prop="postCount" label="关联帖子" width="90" sortable />
      <el-table-column prop="followCount" label="关注数" width="80" />
      <el-table-column label="状态" width="80">
        <template #default="{row}">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="160" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{row}">
          <el-button text size="small" type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button text size="small" :type="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :page-sizes="[10,20,50]" :total="total" layout="total,sizes,prev,pager,next" @change="fetchList" />
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="formVisible" :title="editingTag?.id ? '编辑标签' : '新增标签'" width="460px">
      <el-form label-width="80px">
        <el-form-item label="标签名"><el-input v-model="tagForm.name" placeholder="2~20字" maxlength="20" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="tagForm.description" type="textarea" :rows="2" placeholder="可选" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="formLoading" @click="doSave">{{ editingTag?.id ? '保存' : '创建' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminTagApi } from '../../api/admin'

const list = ref([]); const loading = ref(false)
const page = ref(1); const pageSize = ref(20); const total = ref(0)
const keyword = ref(''); const statusFilter = ref(null); const sortBy = ref('postCount')

async function fetchList() {
  loading.value = true
  try {
    const res = await adminTagApi.getList({ keyword: keyword.value || undefined, status: statusFilter.value, sortBy: sortBy.value, page: page.value, pageSize: pageSize.value })
    list.value = res.records || []; total.value = res.total || 0
  } catch (e) { ElMessage.error(e.message) } finally { loading.value = false }
}
function search() { page.value = 1; fetchList() }

const formVisible = ref(false); const formLoading = ref(false); const editingTag = ref({})
const tagForm = reactive({ name: '', description: '' })
function openCreate() { editingTag.value = {}; tagForm.name = ''; tagForm.description = ''; formVisible.value = true }
function openEdit(row) { editingTag.value = row; tagForm.name = row.name; tagForm.description = row.description || ''; formVisible.value = true }
async function doSave() {
  if (!tagForm.name || tagForm.name.length < 2) { ElMessage.warning('标签名至少2字'); return }
  formLoading.value = true
  try {
    if (editingTag.value.id) { await adminTagApi.update(editingTag.value.id, tagForm) } else { await adminTagApi.create(tagForm) }
    ElMessage.success(editingTag.value.id ? '已更新' : '已创建'); formVisible.value = false; fetchList()
  } catch (e) { ElMessage.error(e.message) } finally { formLoading.value = false }
}
async function toggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  try { await adminTagApi.toggleStatus(row.id, newStatus); ElMessage.success(newStatus ? '已启用' : '已禁用'); fetchList() } catch (e) { ElMessage.error(e.message) }
}

onMounted(() => fetchList())
</script>

<style scoped>
.admin-page { max-width: 1200px; }
.page-title-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-title { font-size: 22px; font-weight: 600; color: #303133; }
.filter-bar { display: flex; gap: 10px; margin-bottom: 16px; align-items: center; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
.text-muted { color: #c0c4cc; font-size: 13px; }
</style>
