<template>
  <div class="page-container">
    <el-page-header @back="$router.back()" title="返回">
      <template #content>竞赛详情 #{{ id }}</template>
    </el-page-header>

    <el-card style="margin-top: 16px">
      <div class="detail-head">
        <h2 style="margin: 0">{{ comp.name }}</h2>
        <el-button
          v-if="isLogin"
          :type="fav.id ? 'warning' : 'default'"
          plain
          @click="toggleFav"
        >
          <el-icon><StarFilled v-if="fav.id" /><Star v-else /></el-icon>
          {{ fav.id ? '已收藏' : '收藏' }}
        </el-button>
      </div>
      <p class="text-muted">类型：{{ comp.type }} · 报名截止：{{ comp.deadline || '不限' }}</p>
      <p>{{ comp.intro }}</p>
      <el-button class="cl-btn-hero" @click="openRegister">报名参赛（队伍）</el-button>
    </el-card>

    <el-tabs v-model="tab" type="border-card" style="margin-top: 16px">
      <el-tab-pane label="赛事资讯" name="news">
        <el-button v-if="isAdmin" type="primary" @click="newsDialog = true" style="margin-bottom: 10px">发布资讯</el-button>
        <el-card v-for="n in news" :key="n.id" style="margin-bottom: 10px">
          <strong>{{ n.title }}</strong>
          <p class="text-muted">{{ n.createTime }}</p>
          <p>{{ n.content }}</p>
        </el-card>
        <el-empty v-if="!news.length" description="暂无资讯" />
      </el-tab-pane>

      <el-tab-pane label="赛事日程" name="schedule">
        <el-button v-if="canManage" type="primary" @click="scheduleDialog = true" style="margin-bottom: 10px">发布日程</el-button>
        <el-timeline v-if="schedules.length">
          <el-timeline-item
            v-for="s in schedules"
            :key="s.id"
            :timestamp="fmtRange(s.startTime, s.endTime)"
            placement="top"
          >
            <el-card>
              <div class="sch-head">
                <span>
                  <el-tag v-if="s.stage" size="small" type="info">{{ STAGE_MAP[s.stage] || s.stage }}</el-tag>
                  <strong style="margin-left: 6px">{{ s.title }}</strong>
                </span>
                <el-button v-if="canManage" link type="danger" size="small" @click="onDeleteSchedule(s)">删除</el-button>
              </div>
              <p v-if="s.location" class="text-muted" style="margin: 6px 0 0">
                <el-icon><Location /></el-icon> {{ s.location }}
              </p>
              <p v-if="s.remark" style="margin: 6px 0 0">{{ s.remark }}</p>
            </el-card>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无日程" />
      </el-tab-pane>

      <el-tab-pane label="获奖公示" name="awards">
        <el-button v-if="isAdmin" type="primary" @click="awardDialog = true" style="margin-bottom: 10px">发布获奖</el-button>
        <el-table :data="awards" stripe>
          <el-table-column label="奖项" width="110">
            <template #default="{ row }">
              <el-tag :type="AWARD_TAG[row.awardLevel] || ''" size="small">{{ AWARD_MAP[row.awardLevel] || row.awardLevel }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="队伍" prop="teamName" />
          <el-table-column label="年份" prop="awardYear" width="90" />
          <el-table-column label="人数" prop="memberCount" width="80" />
          <el-table-column label="成员" prop="members" show-overflow-tooltip />
        </el-table>
        <el-empty v-if="!awards.length" description="暂无获奖公示" />
      </el-tab-pane>

      <el-tab-pane label="赛事附件" name="attach">
        <el-form v-if="canManage" :inline="true" class="upload-bar">
          <el-form-item label="类别">
            <el-select v-model="attachForm.category" style="width: 130px">
              <el-option label="通知" value="NOTICE" />
              <el-option label="报名表" value="FORM" />
              <el-option label="规程" value="RULE" />
            </el-select>
          </el-form-item>
          <el-form-item label="名称">
            <el-input v-model="attachForm.name" placeholder="可选，留空用文件名" style="width: 180px" />
          </el-form-item>
          <el-form-item>
            <el-upload
              :auto-upload="false"
              :show-file-list="true"
              :limit="1"
              :on-change="onPickFile"
              :on-exceed="onExceed"
              ref="uploadRef"
            >
              <el-button>选择文件</el-button>
            </el-upload>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="uploading" @click="submitAttachment">上传附件</el-button>
          </el-form-item>
        </el-form>

        <el-table :data="attachments">
          <el-table-column label="名称" prop="name" />
          <el-table-column label="分类" prop="category" width="120" />
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <el-button text type="primary" @click="downloadAttachment(row.fileId, row.name)">下载</el-button>
              <el-button v-if="canManage" text type="danger" @click="onDeleteAttachment(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!attachments.length" description="暂无附件" />
      </el-tab-pane>

      <el-tab-pane label="报名列表" name="register">
        <el-table :data="registers">
          <el-table-column label="ID" prop="id" width="80" />
          <el-table-column label="队伍 id" prop="teamId" />
          <el-table-column label="状态" prop="status" />
          <el-table-column label="备注" prop="remark" />
          <el-table-column label="操作">
            <template #default="{ row }">
              <template v-if="isAdmin && row.status === 'PENDING'">
                <el-button text type="success" @click="audit(row, true)">通过</el-button>
                <el-button text type="danger" @click="audit(row, false)">拒绝</el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="registerDialog" title="报名" width="420px">
      <el-form :model="regForm" label-width="80px">
        <el-form-item label="队伍 id"><el-input v-model="regForm.teamId" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="regForm.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="registerDialog = false">取消</el-button>
        <el-button type="primary" @click="submitRegister">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="newsDialog" title="发布资讯" width="540px">
      <el-form :model="newsForm" label-width="80px">
        <el-form-item label="标题"><el-input v-model="newsForm.title" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="newsForm.content" type="textarea" :rows="6" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="newsDialog = false">取消</el-button>
        <el-button type="primary" @click="submitNews">发布</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="scheduleDialog" title="发布赛事日程" width="540px">
      <el-form :model="scheduleForm" label-width="90px">
        <el-form-item label="阶段">
          <el-select v-model="scheduleForm.stage" clearable placeholder="选择阶段" style="width: 100%">
            <el-option v-for="(label, val) in STAGE_MAP" :key="val" :label="label" :value="val" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题"><el-input v-model="scheduleForm.title" /></el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="scheduleForm.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="scheduleForm.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="地点"><el-input v-model="scheduleForm.location" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="scheduleForm.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="scheduleForm.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scheduleDialog = false">取消</el-button>
        <el-button type="primary" @click="submitSchedule">发布</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="awardDialog" title="发布获奖公示" width="540px">
      <el-form :model="awardForm" label-width="90px">
        <el-form-item label="队伍名称"><el-input v-model="awardForm.teamName" /></el-form-item>
        <el-form-item label="队伍 id"><el-input v-model="awardForm.teamId" /></el-form-item>
        <el-form-item label="奖项等级">
          <el-select v-model="awardForm.awardLevel" placeholder="选择奖项" style="width: 100%">
            <el-option v-for="(label, val) in AWARD_MAP" :key="val" :label="label" :value="val" />
          </el-select>
        </el-form-item>
        <el-form-item label="获奖年份"><el-input-number v-model="awardForm.awardYear" :min="2000" :max="2100" /></el-form-item>
        <el-form-item label="获奖人数"><el-input-number v-model="awardForm.memberCount" :min="0" /></el-form-item>
        <el-form-item label="成员"><el-input v-model="awardForm.members" type="textarea" :rows="2" placeholder="姓名，逗号分隔" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="awardDialog = false">取消</el-button>
        <el-button type="primary" @click="submitAward">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCompetition } from '@/api/competition'
import {
  registerCompetition, auditRegister, listRegisters,
  listAttachments, addAttachment, deleteAttachment,
  listNews, publishNews,
  listSchedule, addSchedule, deleteSchedule,
  listAwards, publishAward
} from '@/api/competitionExtra'
import { uploadFile } from '@/api/file'
import { listFavorites, addFavorite, removeFavorite } from '@/api/user'
import { useUserStore } from '@/store/user'

const route = useRoute()
const userStore = useUserStore()
const id = route.params.id
const comp = ref({})
const tab = ref('news')
const news = ref([])
const attachments = ref([])
const registers = ref([])
const schedules = ref([])
const awards = ref([])

const isAdmin = computed(() => userStore.user?.role === 'ADMIN')
const isLogin = computed(() => userStore.isLogin)
const canManage = computed(() => isAdmin.value || (comp.value.creatorId && comp.value.creatorId === userStore.user?.id))

const STAGE_MAP = { SIGN_UP: '报名', PRELIM: '初赛', SEMI: '复赛', FINAL: '决赛', AWARD: '颁奖' }
const AWARD_MAP = { FIRST: '一等奖', SECOND: '二等奖', THIRD: '三等奖', EXCELLENT: '优秀奖' }
const AWARD_TAG = { FIRST: 'danger', SECOND: 'warning', THIRD: 'success', EXCELLENT: 'info' }

const fav = reactive({ id: null })

const registerDialog = ref(false)
const regForm = reactive({ teamId: '', remark: '' })

const newsDialog = ref(false)
const newsForm = reactive({ title: '', content: '' })

const scheduleDialog = ref(false)
const scheduleForm = reactive({ stage: '', title: '', startTime: null, endTime: null, location: '', remark: '', sortOrder: 0 })

const awardDialog = ref(false)
const awardForm = reactive({ teamId: '', teamName: '', awardLevel: '', awardYear: new Date().getFullYear(), memberCount: 0, members: '' })

const uploadRef = ref()
const uploading = ref(false)
const attachForm = reactive({ category: 'NOTICE', name: '', file: null })

function fmtRange(s, e) {
  if (!s && !e) return ''
  const f = (t) => (t ? String(t).replace('T', ' ').slice(0, 16) : '')
  return e ? `${f(s)} ~ ${f(e)}` : f(s)
}

function openRegister() { registerDialog.value = true }

async function submitRegister() {
  await registerCompetition(id, regForm)
  ElMessage.success('报名已提交')
  registerDialog.value = false
}

async function submitNews() {
  await publishNews(id, newsForm)
  ElMessage.success('已发布')
  newsDialog.value = false
  news.value = await listNews(id)
}

async function submitSchedule() {
  if (!scheduleForm.title) { ElMessage.warning('请填写日程标题'); return }
  await addSchedule(id, { ...scheduleForm })
  ElMessage.success('日程已发布')
  scheduleDialog.value = false
  Object.assign(scheduleForm, { stage: '', title: '', startTime: null, endTime: null, location: '', remark: '', sortOrder: 0 })
  schedules.value = await listSchedule(id)
}

async function onDeleteSchedule(s) {
  await ElMessageBox.confirm(`确定删除日程「${s.title}」？`, '提示', { type: 'warning' })
  await deleteSchedule(s.id)
  ElMessage.success('已删除')
  schedules.value = await listSchedule(id)
}

async function submitAward() {
  if (!awardForm.teamName || !awardForm.awardLevel) { ElMessage.warning('请填写队伍名称与奖项'); return }
  await publishAward(id, { ...awardForm, teamId: awardForm.teamId ? Number(awardForm.teamId) : null })
  ElMessage.success('获奖公示已发布')
  awardDialog.value = false
  Object.assign(awardForm, { teamId: '', teamName: '', awardLevel: '', awardYear: new Date().getFullYear(), memberCount: 0, members: '' })
  awards.value = await listAwards(id)
}

function onPickFile(uploadFileObj) {
  attachForm.file = uploadFileObj.raw
}

function onExceed(files) {
  uploadRef.value?.clearFiles()
  uploadRef.value?.handleStart(files[0])
  attachForm.file = files[0]
}

async function submitAttachment() {
  if (!attachForm.file) { ElMessage.warning('请先选择文件'); return }
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', attachForm.file)
    const up = await uploadFile(fd, { scope: 'COMPETITION', businessId: id })
    await addAttachment(id, {
      fileId: up.fileId,
      name: attachForm.name || up.originalName,
      category: attachForm.category
    })
    ElMessage.success('附件上传成功')
    attachForm.name = ''
    attachForm.file = null
    uploadRef.value?.clearFiles()
    attachments.value = await listAttachments(id)
  } finally {
    uploading.value = false
  }
}

async function onDeleteAttachment(row) {
  await ElMessageBox.confirm(`确定删除附件「${row.name}」？`, '提示', { type: 'warning' })
  await deleteAttachment(row.id)
  ElMessage.success('已删除')
  attachments.value = await listAttachments(id)
}

async function audit(row, approved) {
  await auditRegister(row.id, { approved, reason: approved ? '' : '不符合条件' })
  ElMessage.success('审核完成')
  registers.value = (await listRegisters(id, { current: 1, size: 100 })).records || []
}

async function downloadAttachment(fileId, name) {
  const res = await fetch(`/api/file/${fileId}/download`, {
    headers: { Authorization: `Bearer ${userStore.token}` }
  })
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a'); a.href = url; a.download = name; a.click()
  URL.revokeObjectURL(url)
}

async function loadFavorite() {
  if (!isLogin.value) return
  try {
    const list = await listFavorites('COMPETITION')
    const hit = (list || []).find((f) => String(f.refId) === String(id))
    fav.id = hit ? hit.id : null
  } catch (e) { /* 忽略 */ }
}

async function toggleFav() {
  try {
    if (fav.id) {
      await removeFavorite(fav.id)
      fav.id = null
      ElMessage.success('已取消收藏')
    } else {
      const rec = await addFavorite({ refType: 'COMPETITION', refId: Number(id) })
      fav.id = rec.id
      ElMessage.success('已收藏')
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

onMounted(async () => {
  comp.value = await getCompetition(id)
  news.value = await listNews(id)
  attachments.value = await listAttachments(id)
  schedules.value = await listSchedule(id)
  awards.value = await listAwards(id)
  registers.value = (await listRegisters(id, { current: 1, size: 100 })).records || []
  loadFavorite()
})
</script>

<style scoped>
.detail-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.sch-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.upload-bar {
  margin-bottom: 12px;
  padding: 12px;
  background: var(--cl-primary-soft, #f5f7fa);
  border-radius: 8px;
}
.text-muted {
  color: #909399;
}
</style>
