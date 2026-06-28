<template>
  <div class="page-container" v-loading="loading">
    <div class="eval-head">
      <el-page-header :content="headerText" @back="$router.back()" />
    </div>

    <el-tabs v-model="activeTab">
      <!-- 队伍互评：收到的评价 -->
      <el-tab-pane name="received">
        <template #label>
          <span>队伍互评</span>
          <el-badge v-if="list.length" :value="list.length" class="tab-badge" type="info" />
        </template>

        <div v-if="!isSelf" class="text-muted target-tip">
          正在查看用户 #{{ targetId }} 收到的评价
        </div>

        <div v-if="list.length" class="card-list">
          <el-card v-for="e in list" :key="e.id" class="eval-card" shadow="hover">
            <div class="eval-card-head">
              <div class="from">
                <el-avatar :size="36">
                  {{ e.anonymous === 1 ? '匿' : (e.fromUserName || '?').charAt(0) }}
                </el-avatar>
                <div class="from-meta">
                  <span class="from-name">
                    {{ e.anonymous === 1 ? '匿名用户' : (e.fromUserName || '未知用户') }}
                    <el-tag v-if="e.anonymous === 1" size="small" type="info" effect="plain">匿名</el-tag>
                    <el-tag v-if="isMine(e)" size="small" type="success" effect="plain">我发出的</el-tag>
                  </span>
                  <span class="text-muted time">{{ e.createTime }}</span>
                </div>
              </div>
              <div class="avg-score">
                <span class="avg-num">{{ avgOf(e) }}</span>
                <span class="text-muted">综合</span>
              </div>
            </div>

            <div class="scores">
              <div class="score-row">
                <span class="score-label">责任心</span>
                <el-rate :model-value="e.responsibility" disabled />
              </div>
              <div class="score-row">
                <span class="score-label">技术力</span>
                <el-rate :model-value="e.tech" disabled />
              </div>
              <div class="score-row">
                <span class="score-label">沟通力</span>
                <el-rate :model-value="e.communication" disabled />
              </div>
            </div>

            <div class="eval-actions">
              <el-button link type="primary" size="small" @click="openReply(e)">
                <el-icon><ChatLineSquare /></el-icon> 回复
              </el-button>
              <el-button link type="warning" size="small" @click="openReport(e)">
                <el-icon><Warning /></el-icon> 举报
              </el-button>
              <el-button
                v-if="isMine(e)"
                link
                type="info"
                size="small"
                @click="onToggleAnonymous(e)"
              >
                <el-icon><Switch /></el-icon>
                {{ e.anonymous === 1 ? '转为实名' : '转为匿名' }}
              </el-button>
            </div>
          </el-card>
        </div>
        <el-empty v-else-if="!loading" description="暂无评价" />
      </el-tab-pane>

      <!-- 我的信誉变动 -->
      <el-tab-pane label="我的信誉变动" name="reputation">
        <el-timeline v-if="repLogs.length" class="rep-timeline">
          <el-timeline-item
            v-for="log in repLogs"
            :key="log.id"
            :timestamp="log.createTime"
            placement="top"
            :type="changeType(log)"
          >
            <el-card>
              <div class="rep-row">
                <span class="rep-change" :class="{ up: up(log), down: !up(log) }">
                  {{ up(log) ? '+' : '' }}{{ Number(log.changeValue).toFixed(2) }}
                </span>
                <span class="rep-value">
                  {{ Number(log.beforeValue).toFixed(2) }} →
                  <b>{{ Number(log.afterValue).toFixed(2) }}</b>
                </span>
                <el-tag size="small" type="info" effect="plain">{{ log.sourceType }}</el-tag>
              </div>
              <div class="text-muted rep-reason">{{ log.reason }}</div>
            </el-card>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else-if="!loading" description="暂无信誉分变动记录" />
      </el-tab-pane>
    </el-tabs>

    <!-- 回复弹框 -->
    <el-dialog v-model="replyDialog" title="评价回复" width="520px">
      <div v-loading="replyLoading" class="reply-list">
        <div v-for="r in replies" :key="r.id" class="reply-item">
          <div class="reply-meta">
            <span class="reply-author">用户 #{{ r.authorId }}</span>
            <span class="text-muted">{{ r.createTime }}</span>
          </div>
          <div class="reply-content">{{ r.content }}</div>
        </div>
        <el-empty v-if="!replyLoading && !replies.length" description="还没有回复" :image-size="60" />
      </div>
      <el-input
        v-model="replyContent"
        type="textarea"
        :rows="3"
        placeholder="写下你的回复…"
        maxlength="500"
        show-word-limit
      />
      <template #footer>
        <el-button @click="replyDialog = false">关闭</el-button>
        <el-button type="primary" :loading="replySaving" @click="submitReply">发送回复</el-button>
      </template>
    </el-dialog>

    <!-- 举报弹框 -->
    <el-dialog v-model="reportDialog" title="举报评价" width="480px">
      <el-form label-position="top">
        <el-form-item label="举报原因">
          <el-input
            v-model="reportReason"
            type="textarea"
            :rows="4"
            placeholder="请描述该评价存在的问题（如恶意差评、人身攻击等）"
            maxlength="300"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportDialog = false">取消</el-button>
        <el-button type="warning" :loading="reportSaving" @click="submitReport">提交举报</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listByUser,
  listReplies,
  reply as replyApi,
  reportEval,
  toggleAnonymous,
  reputationLog
} from '@/api/evaluation'
import { useUserStore } from '@/store/user'

const route = useRoute()
const userStore = useUserStore()
const currentUserId = computed(() => userStore.user?.id)

// 目标用户：优先取路由 query.userId，否则为当前登录用户
const targetId = computed(() => route.query.userId || currentUserId.value)
const isSelf = computed(() => String(targetId.value) === String(currentUserId.value))

const loading = ref(false)
const activeTab = ref('received')
const list = ref([])
const repLogs = ref([])

function isMine(e) {
  return e.fromUserId != null && String(e.fromUserId) === String(currentUserId.value)
}
function avgOf(e) {
  return ((e.responsibility + e.tech + e.communication) / 3).toFixed(1)
}
const headerText = computed(() => (isSelf.value ? '我的评价与信誉' : '用户评价'))

function up(log) {
  return Number(log.changeValue) >= 0
}
function changeType(log) {
  return up(log) ? 'success' : 'danger'
}

async function loadEvaluations() {
  if (!targetId.value) return
  loading.value = true
  try {
    list.value = await listByUser(targetId.value)
  } finally {
    loading.value = false
  }
}

async function loadRepLogs() {
  if (!currentUserId.value) return
  loading.value = true
  try {
    repLogs.value = await reputationLog(currentUserId.value)
  } finally {
    loading.value = false
  }
}

// 回复
const replyDialog = ref(false)
const replyLoading = ref(false)
const replySaving = ref(false)
const replies = ref([])
const replyContent = ref('')
const activeEval = ref(null)

async function openReply(e) {
  activeEval.value = e
  replyContent.value = ''
  replies.value = []
  replyDialog.value = true
  replyLoading.value = true
  try {
    replies.value = await listReplies(e.id)
  } finally {
    replyLoading.value = false
  }
}

async function submitReply() {
  if (!replyContent.value.trim()) {
    ElMessage.warning('回复内容不能为空')
    return
  }
  replySaving.value = true
  try {
    await replyApi(activeEval.value.id, { content: replyContent.value.trim() })
    ElMessage.success('回复已发送')
    replyContent.value = ''
    replies.value = await listReplies(activeEval.value.id)
  } finally {
    replySaving.value = false
  }
}

// 举报
const reportDialog = ref(false)
const reportSaving = ref(false)
const reportReason = ref('')

function openReport(e) {
  activeEval.value = e
  reportReason.value = ''
  reportDialog.value = true
}

async function submitReport() {
  if (!reportReason.value.trim()) {
    ElMessage.warning('请填写举报原因')
    return
  }
  reportSaving.value = true
  try {
    await reportEval(activeEval.value.id, { reason: reportReason.value.trim() })
    ElMessage.success('举报已提交，等待管理员处理')
    reportDialog.value = false
  } finally {
    reportSaving.value = false
  }
}

// 切换匿名
async function onToggleAnonymous(e) {
  const next = e.anonymous === 1 ? 0 : 1
  await ElMessageBox.confirm(
    next === 1 ? '将这条评价转为匿名？被评价人将看不到你的身份。' : '将这条评价转为实名？',
    '提示',
    { type: 'warning' }
  )
  await toggleAnonymous(e.id, next)
  ElMessage.success('已更新匿名状态')
  await loadEvaluations()
}

watch(activeTab, (tab) => {
  if (tab === 'reputation' && !repLogs.value.length) loadRepLogs()
})
watch(targetId, () => loadEvaluations())

onMounted(() => {
  loadEvaluations()
})
</script>

<style scoped>
.eval-head {
  margin-bottom: 12px;
}
.tab-badge {
  margin-left: 6px;
}
.target-tip {
  margin-bottom: 12px;
}
.card-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.eval-card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.from {
  display: flex;
  align-items: center;
  gap: 10px;
}
.from-meta {
  display: flex;
  flex-direction: column;
}
.from-name {
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
}
.time {
  font-size: 12px;
}
.avg-score {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.avg-num {
  font-size: 22px;
  font-weight: 700;
  color: #f7ba2a;
  line-height: 1;
}
.scores {
  margin: 14px 0 6px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.score-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.score-label {
  width: 56px;
  color: #606266;
  font-size: 13px;
}
.eval-actions {
  border-top: 1px solid #f0f0f0;
  padding-top: 8px;
  margin-top: 6px;
  display: flex;
  gap: 8px;
}
.rep-timeline {
  margin-top: 8px;
  padding-left: 4px;
}
.rep-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.rep-change {
  font-size: 18px;
  font-weight: 700;
}
.rep-change.up {
  color: #67c23a;
}
.rep-change.down {
  color: #f56c6c;
}
.rep-value {
  color: #606266;
}
.rep-reason {
  margin-top: 6px;
  font-size: 13px;
}
.reply-list {
  max-height: 280px;
  overflow-y: auto;
  margin-bottom: 12px;
}
.reply-item {
  padding: 8px 0;
  border-bottom: 1px dashed #ebeef5;
}
.reply-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  margin-bottom: 4px;
}
.reply-author {
  font-weight: 600;
}
.reply-content {
  white-space: pre-wrap;
  line-height: 1.6;
  color: #303133;
}
</style>
