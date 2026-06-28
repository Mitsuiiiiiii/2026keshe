<template>
  <div class="page-container" v-loading="loading">
    <el-page-header content="队伍详情" @back="$router.back()" />

    <template v-if="detail">
      <el-card class="detail-card">
        <div class="detail-head">
          <div>
            <div class="title-line">
              <span class="team-name">{{ team.name }}</span>
              <el-tag :type="statusTag(team.status)">{{ statusLabel(team.status) }}</el-tag>
              <el-tag v-if="archived" type="info" effect="dark">已归档</el-tag>
            </div>
            <div class="text-muted meta-line">
              <span v-if="detail.competitionName">
                <el-icon><Trophy /></el-icon> {{ detail.competitionName }}
              </span>
              <span><el-icon><User /></el-icon> 队长：{{ detail.leaderName }}</span>
              <span v-if="team.college"><el-icon><School /></el-icon> {{ team.college }}</span>
              <span><el-icon><UserFilled /></el-icon> {{ team.currentSize }}/{{ team.totalSize }} 人</span>
            </div>
          </div>
          <div class="head-ops">
            <el-button v-if="userStore.isLogin"
                       :type="favoriteId ? 'warning' : 'default'"
                       :icon="favoriteId ? StarFilled : Star"
                       @click="toggleFavorite">
              {{ favoriteId ? '已收藏' : '收藏' }}
            </el-button>
            <el-button v-if="userStore.isLogin && !isLeader && team.leaderId" type="primary" plain
                       @click="onChatLeader">
              <el-icon><ChatDotRound /></el-icon> 私聊队长
            </el-button>
            <template v-if="isLeader && !archived">
              <el-button type="primary" @click="$router.push(`/teams/${team.id}/applies`)">
                申请管理
              </el-button>
              <el-button type="primary" plain @click="openEdit">编辑</el-button>
              <el-button type="success" plain @click="onFinish">确认比赛结束</el-button>
              <el-button type="danger" plain @click="onDissolve">解散</el-button>
            </template>
            <el-button v-else-if="!isMember && team.status === 'RECRUITING'" type="primary" @click="onApply">
              申请加入
            </el-button>
            <el-tag v-else-if="isMember" type="success" size="large">已加入</el-tag>
          </div>
        </div>

        <!-- 队伍功能导航 -->
        <div v-if="isMember || isLeader" class="feature-bar">
          <el-button class="feat" @click="$router.push(`/teams/${team.id}/posts`)">
            <el-icon><ChatLineSquare /></el-icon> 动态·时间线
          </el-button>
          <el-button class="feat" @click="$router.push(`/teams/${team.id}/board`)">
            <el-icon><Grid /></el-icon> 任务看板
          </el-button>
          <el-button class="feat" @click="$router.push(`/teams/${team.id}/gantt`)">
            <el-icon><Calendar /></el-icon> 甘特图
          </el-button>
          <el-button class="feat" @click="$router.push(`/teams/${team.id}/worklog`)">
            <el-icon><Timer /></el-icon> 工时填报
          </el-button>
          <el-button class="feat" @click="$router.push(`/teams/${team.id}/files`)">
            <el-icon><Folder /></el-icon> 文件库
          </el-button>
          <el-button class="feat" @click="$router.push(`/teams/${team.id}/notices`)">
            <el-icon><Bell /></el-icon> 公告
          </el-button>
          <el-button class="feat" @click="$router.push(`/teams/${team.id}/stat`)">
            <el-icon><DataLine /></el-icon> 队伍统计
          </el-button>
          <el-button class="feat" type="success" plain @click="openEvaluate">
            <el-icon><Star /></el-icon> 队内互评
          </el-button>
          <el-button v-if="isLeader" class="feat" type="warning" plain
                     @click="$router.push(`/teams/${team.id}/blacklist`)">
            <el-icon><CircleClose /></el-icon> 黑名单
          </el-button>
        </div>

        <el-alert v-if="archived" type="success" :closable="false" show-icon class="archive-tip"
                  title="比赛已结束并归档：可对队友进行最终互评，沉淀本次组队的协作评价。" />

        <el-divider content-position="left">队伍简介</el-divider>
        <p class="intro">{{ team.intro || '暂无简介' }}</p>

        <el-divider content-position="left">招募岗位</el-divider>
        <div v-if="detail.recruits?.length" class="recruits">
          <el-card v-for="r in detail.recruits" :key="r.id" class="recruit-item" shadow="never"
                   :class="{ 'recruit-top': r.isTop === 1 }">
            <div class="recruit-head">
              <el-tag v-if="r.isTop === 1" type="danger" effect="dark" size="small">置顶</el-tag>
              <span class="recruit-pos">{{ r.position }}</span>
              <el-tag :type="r.filled >= r.count ? 'info' : 'warning'" effect="plain" size="small">
                {{ r.filled }}/{{ r.count }}
              </el-tag>
            </div>
            <div v-if="r.tags" class="recruit-tags">
              <el-tag v-for="tag in r.tags.split(',').filter(Boolean)" :key="tag"
                      type="success" effect="light" size="small">{{ tag.trim() }}</el-tag>
            </div>
            <div v-if="isLeader && !archived" class="recruit-ops">
              <el-button text size="small" type="primary" @click="onTopRecruit(r)">
                {{ r.isTop === 1 ? '已置顶' : '置顶' }}
              </el-button>
              <el-button text size="small" @click="openTags(r)">设置标签</el-button>
            </div>
          </el-card>
        </div>
        <span v-else class="text-muted">未设置招募岗位</span>

        <el-divider content-position="left">
          队伍成员
          <el-button v-if="isLeader && !archived" link type="primary" style="margin-left: 12px"
                     @click="openTransfer">转让队长</el-button>
        </el-divider>
        <div class="members">
          <div v-for="m in detail.members" :key="m.userId" class="member">
            <el-avatar :size="44" :src="m.avatar" @click="$router.push(`/user/${m.userId}`)">
              {{ m.nickname?.charAt(0) }}
            </el-avatar>
            <div class="member-info">
              <span class="member-name" @click="$router.push(`/user/${m.userId}`)">
                {{ m.nickname }}
                <el-tag v-if="m.role === 'LEADER'" type="danger" size="small">队长</el-tag>
                <el-tag v-else-if="m.role === 'LEADER_DEPUTY'" type="warning" size="small">副队长</el-tag>
              </span>
              <span class="text-muted">{{ m.major || '—' }} · {{ m.grade ? m.grade + '级' : '—' }}</span>
              <div v-if="isLeader && !archived && m.role !== 'LEADER'" class="member-ops">
                <el-button v-if="m.role !== 'LEADER_DEPUTY'" text size="small" type="primary"
                           @click="onPromoteDeputy(m)">任命副队长</el-button>
                <el-button v-else text size="small" type="info"
                           @click="onDemoteDeputy(m)">取消副队长</el-button>
                <el-button text size="small" type="warning"
                           @click="onTransferTo(m)">转让队长</el-button>
              </div>
            </div>
          </div>
        </div>
      </el-card>
    </template>

    <!-- 编辑队伍 -->
    <el-dialog v-model="editDialog" title="编辑队伍" width="520px">
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="队伍名称"><el-input v-model="editForm.name" /></el-form-item>
        <el-form-item label="关联竞赛">
          <el-select v-model="editForm.competitionId" clearable filterable style="width: 100%">
            <el-option v-for="c in competitions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属学院"><el-input v-model="editForm.college" /></el-form-item>
        <el-form-item label="总人数"><el-input-number v-model="editForm.totalSize" :min="1" :max="20" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="editForm.status" style="width: 100%">
            <el-option v-for="s in TEAM_STATUS" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="简介"><el-input v-model="editForm.intro" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 申请加入 -->
    <el-dialog v-model="applyDialog" title="申请加入队伍" width="480px">
      <el-form :model="applyForm" label-position="top">
        <el-form-item label="自我介绍">
          <el-input v-model="applyForm.selfIntro" type="textarea" :rows="3"
                    placeholder="简单介绍一下你自己" />
        </el-form-item>
        <el-form-item label="技能说明">
          <el-input v-model="applyForm.skillDesc" type="textarea" :rows="3"
                    placeholder="你能为队伍带来什么" />
        </el-form-item>
        <el-form-item label="个人主页链接">
          <el-input v-model="applyForm.profileLink" placeholder="可选，如 GitHub / 作品集" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyDialog = false">取消</el-button>
        <el-button type="primary" :loading="applying" @click="doApply">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 转让队长 -->
    <el-dialog v-model="transferDialog" title="转让队长" width="420px">
      <el-form label-width="90px">
        <el-form-item label="新队长">
          <el-select v-model="transferTarget" placeholder="选择成员" style="width: 100%">
            <el-option v-for="m in transferCandidates" :key="m.userId"
                       :label="m.nickname + (m.role === 'LEADER_DEPUTY' ? '（副队长）' : '')"
                       :value="m.userId" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferDialog = false">取消</el-button>
        <el-button type="primary" :loading="transferring" @click="doTransfer">确认转让</el-button>
      </template>
    </el-dialog>

    <!-- 设置招募标签 -->
    <el-dialog v-model="tagsDialog" title="设置招募标签" width="420px">
      <el-form label-position="top">
        <el-form-item label="标签（多个用逗号分隔）">
          <el-input v-model="tagsForm.tags" placeholder="如：前端,React,设计" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tagsDialog = false">取消</el-button>
        <el-button type="primary" :loading="tagsSaving" @click="doSaveTags">保存</el-button>
      </template>
    </el-dialog>

    <!-- 队内互评 / 评分 -->
    <el-dialog v-model="evalDialog" title="队内互评 · 评分" width="520px">
      <el-form label-width="84px">
        <el-form-item label="评价对象">
          <el-select v-model="evalForm.toUserId" placeholder="选择队友" style="width: 100%">
            <el-option v-for="m in evalCandidates" :key="m.userId"
                       :label="m.nickname" :value="m.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="责任心">
          <el-rate v-model="evalForm.responsibility" :max="5" show-score />
        </el-form-item>
        <el-form-item label="技术力">
          <el-rate v-model="evalForm.tech" :max="5" show-score />
        </el-form-item>
        <el-form-item label="沟通力">
          <el-rate v-model="evalForm.communication" :max="5" show-score />
        </el-form-item>
        <el-form-item label="匿名评价">
          <el-switch v-model="evalForm.anonymous" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="evalDialog = false">取消</el-button>
        <el-button type="primary" :loading="evalSaving" @click="doEvaluate">提交评分</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Star, StarFilled, ChatLineSquare, Grid, Calendar, Timer, Folder, Bell,
  DataLine, CircleClose
} from '@element-plus/icons-vue'
import { getTeam, updateTeam, deleteTeam, transferLeader, finishTeam } from '@/api/team'
import { promoteDeputy, demoteDeputy, topRecruit, updateRecruitTags } from '@/api/teamExtra'
import { submit as submitEvaluation } from '@/api/evaluation'
import { pageCompetitions } from '@/api/competition'
import { submitApply } from '@/api/apply'
import { listFavorites, addFavorite, removeFavorite } from '@/api/user'
import { useUserStore } from '@/store/user'
import { TEAM_STATUS, TEAM_STATUS_MAP, TEAM_STATUS_TAG } from '@/constants'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const detail = ref(null)
const team = computed(() => detail.value?.team || {})
const isLeader = computed(() => team.value.leaderId === userStore.user?.id)
const isMember = computed(() =>
  (detail.value?.members || []).some((m) => m.userId === userStore.user?.id)
)
const archived = computed(() => team.value.status === 'ARCHIVED')

// 状态标签兜底（ARCHIVED 不在 constants 中）
const STATUS_LABEL_EXTRA = { ARCHIVED: '已归档' }
const STATUS_TAG_EXTRA = { ARCHIVED: 'info' }
const statusLabel = (s) => TEAM_STATUS_MAP[s] || STATUS_LABEL_EXTRA[s] || s
const statusTag = (s) => TEAM_STATUS_TAG[s] || STATUS_TAG_EXTRA[s] || 'info'

// 收藏
const favoriteId = ref(null)
async function loadFavorite() {
  if (!userStore.isLogin) return
  try {
    const favs = await listFavorites('TEAM')
    const hit = (favs || []).find((f) => String(f.refId) === String(route.params.id))
    favoriteId.value = hit?.id || null
  } catch (e) { /* 忽略 */ }
}
async function toggleFavorite() {
  if (favoriteId.value) {
    await removeFavorite(favoriteId.value)
    favoriteId.value = null
    ElMessage.success('已取消收藏')
  } else {
    const fav = await addFavorite({ refType: 'TEAM', refId: Number(route.params.id) })
    favoriteId.value = fav?.id || true
    ElMessage.success('已收藏')
  }
}

// 转让队长
const transferDialog = ref(false)
const transferring = ref(false)
const transferTarget = ref(null)
const transferCandidates = computed(() =>
  (detail.value?.members || []).filter((m) => m.role !== 'LEADER')
)
function openTransfer() {
  transferTarget.value = null
  transferDialog.value = true
}
function onTransferTo(m) {
  transferTarget.value = m.userId
  transferDialog.value = true
}
async function doTransfer() {
  if (!transferTarget.value) { ElMessage.warning('请选择新队长'); return }
  await ElMessageBox.confirm('转让后你将不再是队长，确定继续？', '提示', { type: 'warning' })
  transferring.value = true
  try {
    await transferLeader(team.value.id, transferTarget.value)
    ElMessage.success('已转让队长')
    transferDialog.value = false
    load()
  } finally {
    transferring.value = false
  }
}

// 任命副队长
async function onPromoteDeputy(m) {
  await ElMessageBox.confirm(`任命「${m.nickname}」为副队长？`, '提示', { type: 'warning' })
  // 注意：后端按 team_member 行 id 解析，VO 暂只提供 userId，优先使用 memberId
  await promoteDeputy(m.memberId ?? m.userId)
  ElMessage.success('已任命副队长')
  load()
}

// 取消副队长
async function onDemoteDeputy(m) {
  await ElMessageBox.confirm(`取消「${m.nickname}」的副队长身份？`, '提示', { type: 'warning' })
  await demoteDeputy(m.memberId ?? m.userId)
  ElMessage.success('已取消副队长')
  load()
}

// 一键私聊队长
function onChatLeader() {
  router.push(`/private-chat?withUserId=${team.value.leaderId}`)
}

// 确认比赛结束（归档）
async function onFinish() {
  await ElMessageBox.confirm('确认比赛已结束并归档队伍？归档后将无法再编辑。', '提示', { type: 'warning' })
  await finishTeam(team.value.id)
  ElMessage.success('队伍已归档')
  load()
}

// 招募置顶 / 标签
async function onTopRecruit(r) {
  await topRecruit(r.id)
  ElMessage.success('已置顶')
  load()
}
const tagsDialog = ref(false)
const tagsSaving = ref(false)
const tagsForm = reactive({ id: null, tags: '' })
function openTags(r) {
  tagsForm.id = r.id
  tagsForm.tags = r.tags || ''
  tagsDialog.value = true
}
async function doSaveTags() {
  tagsSaving.value = true
  try {
    await updateRecruitTags(tagsForm.id, tagsForm.tags)
    ElMessage.success('标签已更新')
    tagsDialog.value = false
    load()
  } finally {
    tagsSaving.value = false
  }
}

// 队内互评 / 评分
const evalDialog = ref(false)
const evalSaving = ref(false)
const evalForm = reactive({ toUserId: null, responsibility: 5, tech: 5, communication: 5, anonymous: false })
const evalCandidates = computed(() =>
  (detail.value?.members || []).filter((m) => m.userId !== userStore.user?.id)
)
function openEvaluate() {
  if (!evalCandidates.value.length) { ElMessage.warning('暂无可评价的队友'); return }
  Object.assign(evalForm, { toUserId: null, responsibility: 5, tech: 5, communication: 5, anonymous: false })
  evalDialog.value = true
}
async function doEvaluate() {
  if (!evalForm.toUserId) { ElMessage.warning('请选择评价对象'); return }
  evalSaving.value = true
  try {
    await submitEvaluation({
      teamId: team.value.id,
      toUserId: evalForm.toUserId,
      responsibility: evalForm.responsibility,
      tech: evalForm.tech,
      communication: evalForm.communication,
      anonymous: evalForm.anonymous ? 1 : 0
    })
    ElMessage.success('评分已提交')
    evalDialog.value = false
  } finally {
    evalSaving.value = false
  }
}

const applyDialog = ref(false)
const applying = ref(false)
const applyForm = reactive({ selfIntro: '', skillDesc: '', profileLink: '' })

const competitions = ref([])
const editDialog = ref(false)
const saving = ref(false)
const editForm = reactive({ name: '', competitionId: null, college: '', totalSize: 1, status: '', intro: '' })

async function load() {
  loading.value = true
  try {
    detail.value = await getTeam(route.params.id)
  } finally {
    loading.value = false
  }
}

async function openEdit() {
  if (!competitions.value.length) {
    const data = await pageCompetitions({ current: 1, size: 100 })
    competitions.value = data.records
  }
  Object.assign(editForm, {
    name: team.value.name,
    competitionId: team.value.competitionId,
    college: team.value.college,
    totalSize: team.value.totalSize,
    status: team.value.status,
    intro: team.value.intro
  })
  editDialog.value = true
}

async function saveEdit() {
  saving.value = true
  try {
    await updateTeam(team.value.id, editForm)
    ElMessage.success('已保存')
    editDialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function onDissolve() {
  await ElMessageBox.confirm('确定解散该队伍？此操作不可恢复', '警告', { type: 'warning' })
  await deleteTeam(team.value.id)
  ElMessage.success('队伍已解散')
  router.push('/teams')
}

function onApply() {
  Object.assign(applyForm, { selfIntro: '', skillDesc: '', profileLink: '' })
  applyDialog.value = true
}

async function doApply() {
  applying.value = true
  try {
    await submitApply({ teamId: team.value.id, ...applyForm })
    ElMessage.success('申请已提交，请等待队长审核')
    applyDialog.value = false
  } finally {
    applying.value = false
  }
}

onMounted(() => {
  load()
  loadFavorite()
})
</script>

<style scoped>
.detail-card {
  margin-top: 16px;
}
.detail-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}
.feature-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 18px 0 4px;
  padding: 14px 16px;
  background: var(--cl-primary-soft);
  border-radius: var(--cl-radius);
}
.feature-bar .feat {
  border-radius: 10px;
}
.archive-tip {
  margin: 12px 0;
}
.title-line {
  display: flex;
  align-items: center;
  gap: 12px;
}
.team-name {
  font-size: 22px;
  font-weight: 600;
}
.meta-line {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  margin-top: 10px;
}
.meta-line span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.intro {
  white-space: pre-wrap;
  line-height: 1.7;
  color: #606266;
}
.recruits {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.recruit-item {
  width: 220px;
}
.recruit-top {
  border-color: #f56c6c;
}
.recruit-head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.recruit-pos {
  font-weight: 600;
}
.recruit-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}
.recruit-ops {
  margin-top: 8px;
}
.members {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
}
.member {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}
.member > .el-avatar {
  cursor: pointer;
}
.member-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.member-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
  cursor: pointer;
}
.member-ops {
  margin-top: 4px;
}
</style>
