<template>
  <div class="page-container">
    <el-page-header @back="$router.back()" title="返回">
      <template #content>队伍文件库 #{{ teamId }}</template>
    </el-page-header>

    <div class="action-bar">
      <el-upload :http-request="onUpload" :show-file-list="false">
        <el-button class="cl-btn-hero"><el-icon><UploadFilled /></el-icon>上传文件</el-button>
      </el-upload>
      <el-input v-model="folder" placeholder="文件夹（默认 /）" style="width: 200px"
                @keyup.enter="loadData" @clear="loadData" clearable />
      <el-button @click="loadData">筛选</el-button>
    </div>

    <el-card>
      <el-table :data="files" v-loading="loading">
        <el-table-column label="文件夹" prop="folder" width="120" />
        <el-table-column label="文件名" prop="name" />
        <el-table-column label="上传人" prop="uploaderId" width="100" />
        <el-table-column label="上传时间" prop="createTime" width="180" />
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button v-if="canPreview(row)" text type="success" @click="preview(row)">预览</el-button>
            <el-button text type="primary" @click="download(row)">下载</el-button>
            <el-button v-if="isLeader" text type="danger" @click="del(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 在线预览 -->
    <el-dialog v-model="previewDialog" :title="previewName" width="70%" top="6vh"
               @closed="onPreviewClosed">
      <div class="preview-box">
        <img v-if="previewType === 'image'" :src="previewUrl" class="preview-img" />
        <iframe v-else-if="previewType === 'pdf'" :src="previewUrl" class="preview-frame" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { uploadFile } from '@/api/file'
import { listTeamFiles, addTeamFile, deleteTeamFile } from '@/api/teamExtra'
import { getTeam } from '@/api/team'
import { useUserStore } from '@/store/user'

const route = useRoute()
const userStore = useUserStore()
const teamId = route.params.id
const files = ref([])
const folder = ref('')
const loading = ref(false)

const leaderId = ref(null)
const isLeader = computed(() => leaderId.value && leaderId.value === userStore.user?.id)

const previewDialog = ref(false)
const previewUrl = ref('')
const previewName = ref('')
const previewType = ref('')

async function loadData() {
  loading.value = true
  try {
    // 把 folder 输入值作为参数传给后端，实现文件夹过滤
    files.value = await listTeamFiles(teamId, folder.value || undefined)
  } finally {
    loading.value = false
  }
}

async function loadTeam() {
  try {
    const detail = await getTeam(teamId)
    leaderId.value = detail?.team?.leaderId
  } catch (e) { /* 忽略，仅影响删除按钮显示 */ }
}

async function onUpload({ file }) {
  const form = new FormData()
  form.append('file', file)
  const uploaded = await uploadFile(form, { scope: 'TEAM', businessId: teamId })
  await addTeamFile(teamId, { fileId: uploaded.fileId, folder: folder.value || '/', name: file.name })
  ElMessage.success('已上传到文件库')
  await loadData()
}

function fileExt(name) {
  const i = (name || '').lastIndexOf('.')
  return i >= 0 ? name.slice(i + 1).toLowerCase() : ''
}

function canPreview(row) {
  const ext = fileExt(row.name)
  return ['png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp', 'pdf'].includes(ext)
}

async function buildBlobUrl(fileId) {
  const res = await fetch(`/api/file/${fileId}/download`, {
    headers: { Authorization: `Bearer ${userStore.token}` }
  })
  const blob = await res.blob()
  return URL.createObjectURL(blob)
}

async function preview(row) {
  const ext = fileExt(row.name)
  previewType.value = ext === 'pdf' ? 'pdf' : 'image'
  previewName.value = row.name
  previewUrl.value = await buildBlobUrl(row.fileId)
  previewDialog.value = true
}

function onPreviewClosed() {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = ''
  }
}

async function download(row) {
  const url = await buildBlobUrl(row.fileId)
  const a = document.createElement('a'); a.href = url; a.download = row.name; a.click()
  URL.revokeObjectURL(url)
}

async function del(row) {
  await ElMessageBox.confirm(`确定删除文件「${row.name}」？`, '提示', { type: 'warning' })
  await deleteTeamFile(row.id)
  ElMessage.success('已删除')
  await loadData()
}

onMounted(() => {
  loadTeam()
  loadData()
})
</script>

<style scoped>
.action-bar { display: flex; gap: 12px; align-items: center; margin: 16px 0; }
.preview-box { display: flex; justify-content: center; align-items: center; min-height: 60vh; }
.preview-img { max-width: 100%; max-height: 70vh; object-fit: contain; }
.preview-frame { width: 100%; height: 70vh; border: none; }
</style>
