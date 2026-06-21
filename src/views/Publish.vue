<template>
  <div class="publish-page">
    <div class="publish-container">
      <h2 class="page-title">{{ editingDraftId ? '编辑草稿' : '发布笔记' }}</h2>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <!-- 上传区域 -->
        <el-form-item label="上传图片 / 视频" prop="media">
          <div class="upload-area">
            <div class="upload-tabs">
              <el-radio-group v-model="mediaType" size="small">
                <el-radio-button value="image">上传图片</el-radio-button>
                <el-radio-button value="video">上传视频</el-radio-button>
              </el-radio-group>
            </div>

            <!-- 图片上传 -->
            <template v-if="mediaType === 'image'">
              <el-upload
                v-model:file-list="imageList"
                action="#"
                list-type="picture-card"
                :auto-upload="false"
                :limit="9"
                multiple
                accept="image/*"
                class="image-uploader"
              >
                <el-icon><Plus /></el-icon>
                <template #file="{ file }">
                  <div>
                    <img class="el-upload-list__item-thumbnail" :src="file.url" alt="" />
                    <span class="el-upload-list__item-actions">
                      <span class="el-upload-list__item-preview" @click="previewImage(file)">
                        <el-icon><ZoomIn /></el-icon>
                      </span>
                      <span class="el-upload-list__item-delete" @click="removeImage(file)">
                        <el-icon><Delete /></el-icon>
                      </span>
                    </span>
                  </div>
                </template>
              </el-upload>
              <p class="upload-tip">最多上传 9 张图片</p>
            </template>

            <!-- 视频上传 -->
            <template v-else>
              <el-upload
                action="#"
                :auto-upload="false"
                :limit="1"
                accept="video/*"
                class="video-uploader"
                :on-change="onVideoChange"
              >
                <div v-if="!videoFile" class="video-upload-placeholder">
                  <el-icon :size="40"><VideoCamera /></el-icon>
                  <p>点击上传视频</p>
                </div>
                <template #file="{ file }">
                  <div class="video-preview">
                    <video :src="file.url" width="100%" controls />
                    <el-button type="danger" size="small" circle @click="clearVideo">
                      <el-icon><Close /></el-icon>
                    </el-button>
                  </div>
                </template>
              </el-upload>
              <p class="upload-tip">支持 MP4、MOV 格式，最大 500MB</p>
              <!-- 视频封面 -->
              <div class="cover-for-video">
                <p style="font-size:13px;color:#999;margin-bottom:8px">视频封面</p>
                <el-upload
                  action="#"
                  list-type="picture-card"
                  :auto-upload="false"
                  :limit="1"
                  accept="image/*"
                  :on-change="onCoverChange"
                  :on-remove="onCoverRemove"
                  :file-list="coverFileList"
                >
                  <el-icon><Plus /></el-icon>
                  <template #file="{ file }">
                    <div>
                      <img class="el-upload-list__item-thumbnail" :src="file.url" alt="" />
                      <span class="el-upload-list__item-actions">
                        <span class="el-upload-list__item-delete" @click="removeCover">
                          <el-icon><Delete /></el-icon>
                        </span>
                      </span>
                    </div>
                  </template>
                </el-upload>
              </div>
            </template>
          </div>
        </el-form-item>

        <!-- 标题 -->
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="给自己的笔记取个标题吧.." maxlength="200" show-word-limit />
        </el-form-item>

        <!-- 正文 -->
        <el-form-item label="正文" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="分享你的生活点滴..." maxlength="10000" show-word-limit />
        </el-form-item>

        <!-- 标签 -->
        <el-form-item label="标签">
          <el-select v-model="form.tagIds" multiple placeholder="选择标签（可多选）" style="width: 100%">
            <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" />
          </el-select>
        </el-form-item>

        <!-- 发布地点 -->
        <el-form-item label="发布地点（选填）">
          <el-input v-model="form.location" placeholder="你在哪里？" :prefix-icon="Location" />
        </el-form-item>

        <!-- 提交按钮 -->
        <el-form-item>
          <div class="form-actions">
            <el-button @click="saveDraft">保存为草稿</el-button>
            <el-button type="primary" size="large" :loading="submitting" @click="submitPost">
              发布笔记
            </el-button>
          </div>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, ZoomIn, Delete, VideoCamera, Close, Location } from '@element-plus/icons-vue'
import { postApi, tagApi, uploadApi } from '../api'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const mediaType = ref('image')
const imageList = ref([])
const videoFile = ref(null)
const videoFileObj = ref(null)
const tags = ref([])
const editingDraftId = ref(null)

// 封面
const coverFileList = ref([])
const coverFileObj = ref(null)

function onCoverChange(uploadFile) {
  coverFileList.value = [uploadFile]
  coverFileObj.value = uploadFile.raw
  return false
}

function onCoverRemove() {
  coverFileObj.value = null
}

function removeCover() {
  coverFileList.value = []
  coverFileObj.value = null
}

const form = reactive({
  title: '',
  content: '',
  tagIds: [],
  location: ''
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入正文', trigger: 'blur' }]
}

function onVideoChange(uploadFile) {
  videoFile.value = uploadFile.url
  videoFileObj.value = uploadFile.raw
  return false
}

function clearVideo() {
  videoFile.value = null
  videoFileObj.value = null
}

function removeImage(file) {
  imageList.value = imageList.value.filter(f => f.uid !== file.uid)
}

function previewImage(file) {
  // 可扩展图片预览
}

async function saveDraft() {
  submitting.value = true
  try {
    // 草稿不上传文件，只保存表单数据
    const postData = {
      title: form.title || '未命名草稿',
      content: form.content || '',
      type: mediaType.value === 'video' ? 2 : 1,
      status: 0,
      tagIds: form.tagIds,
      location: form.location || undefined
    }
    // 如果有已上传的图片/视频 URL（先上传再保存草稿的场景），也带上
    if (mediaType.value === 'image' && imageList.value.length > 0) {
      const images = imageList.value
        .map(f => (typeof f.url === 'string' && f.url.startsWith('http')) ? f.url : null)
        .filter(Boolean)
      postData.coverUrl = images[0] || ''
      postData.imageUrls = images
    }
    if (mediaType.value === 'video' && videoFileObj.value) {
      postData.videoUrl = typeof videoFile.value === 'string' && videoFile.value.startsWith('http') ? videoFile.value : ''
      postData.coverUrl = ''
    }

    await postApi.createPost(postData)
    ElMessage.success('草稿已保存')
    // 跳转到个人主页查看草稿
  } catch (e) {
    ElMessage.error(e.message || '保存草稿失败')
  } finally {
    submitting.value = false
  }
}

async function submitPost() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    // ===== 第一步：上传文件，拿到 URL =====
    let coverUrl = ''
    let images = []
    let videoUrl = ''

    if (mediaType.value === 'image' && imageList.value.length > 0) {
      // 挨个上传图片
      for (const f of imageList.value) {
        if (f.raw) {
          const res = await uploadApi.uploadImage(f.raw)
          const url = res.url || res.imageUrl || (typeof res === 'string' ? res : '')
          if (url) images.push(url)
        } else if (f.url && f.url.startsWith('http')) {
          // 已在 OSS 上的，直接使用
          images.push(f.url)
        }
      }
      coverUrl = images[0] || ''
    }

    if (mediaType.value === 'video' && videoFileObj.value) {
      // 视频和封面一起上传
      const res = await uploadApi.uploadVideo(videoFileObj.value, coverFileObj.value)
      videoUrl = res.videoUrl || res.url || (typeof res === 'string' ? res : '')
      coverUrl = res.coverUrl || videoUrl
    }

    // ===== 第二步：用 JSON 提交帖子数据 =====
    const postData = {
      title: form.title,
      content: form.content,
      type: mediaType.value === 'video' ? 2 : 1,
      status: 1,
      coverUrl: coverUrl,
      imageUrls: images,
      videoUrl: videoUrl,
      tagIds: form.tagIds,
      location: form.location || undefined
    }

    if (editingDraftId.value) {
      await postApi.updatePost(editingDraftId.value, postData)
      ElMessage.success('发布成功！')
    } else {
      await postApi.createPost(postData)
      ElMessage.success('发布成功！')
    }

    // 重置表单
    form.title = ''
    form.content = ''
    form.tagIds = []
    form.location = ''
    imageList.value = []
    clearVideo()
    coverFileList.value = []
    coverFileObj.value = null
  } catch (e) {
    ElMessage.error(e.message || '发布失败，请重试')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  // 加载标签
  try {
    const res = await tagApi.getTags()
    tags.value = res.records || res.list || res.data || res || []
  } catch (_) {}

  // 如果是编辑草稿，加载草稿数据
  const draftId = route.query.draftId
  if (draftId) {
    editingDraftId.value = Number(draftId)
    try {
      const res = await postApi.getPostDetail(draftId)
      if (res) {
        form.title = res.title || ''
        form.content = res.content || ''
        form.tagIds = res.tags?.map(t => t.id) || []
        form.location = res.location || ''
        mediaType.value = res.type === 2 ? 'video' : 'image'
        if (res.coverUrl) {
          // 将已有图片/封面加载到 imageList
          if (res.type === 1 && res.images?.length) {
            imageList.value = res.images.map((img, i) => ({
              uid: Date.now() + i,
              name: `image-${i}`,
              url: img.url,
              status: 'success'
            }))
          }
        }
      }
    } catch (e) {
      ElMessage.warning('草稿加载失败')
    }
  }
})
</script>

<style scoped>
.publish-page {
  max-width: 720px;
  margin: 0 auto;
}
.publish-container {
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}
.page-title {
  font-size: 22px;
  font-weight: 600;
  margin-bottom: 24px;
}
.upload-area { width: 100%; }
.upload-tabs { margin-bottom: 16px; }
.image-uploader { --el-upload-picture-card-size: 120px; }
.upload-tip { font-size: 12px; color: var(--text-secondary); margin-top: 8px; }
.video-upload-placeholder {
  width: 100%;
  min-height: 200px;
  border: 2px dashed #ddd;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: border-color 0.2s;
}
.video-upload-placeholder:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
}
.video-preview { position: relative; width: 100%; }
.video-preview video { border-radius: 8px; max-height: 400px; }
.video-preview .el-button { position: absolute; top: 8px; right: 8px; }
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  width: 100%;
  padding-top: 12px;
}
</style>
