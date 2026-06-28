<template>
  <div class="page-container">
    <el-page-header @back="$router.push(`/teams/${teamId}`)" title="返回">
      <template #content>动态墙 · 时间线 #{{ teamId }}</template>
    </el-page-header>

    <el-tabs v-model="tab" class="wall-tabs">
      <!-- 动态墙 -->
      <el-tab-pane label="动态墙" name="posts">
        <el-card style="margin-top: 8px">
          <template #header><strong>发布动态</strong></template>
          <el-input v-model="form.content" type="textarea" :rows="3" placeholder="分享组队进展、获奖喜讯、任务心得…" />
          <el-input v-model="form.imageUrls" placeholder="图片 URL，多张用逗号分隔（可选）" style="margin-top: 10px" />
          <div style="margin-top: 10px; text-align: right">
            <el-button class="cl-btn-hero" @click="publish">发布动态</el-button>
          </div>
        </el-card>

        <div class="posts">
          <el-card v-for="p in posts" :key="p.id" class="post-card">
            <div class="post-head">
              <el-avatar :size="38" class="cl-avatar">{{ authorName(p.authorId).charAt(0) }}</el-avatar>
              <div>
                <div><strong>{{ authorName(p.authorId) }}</strong></div>
                <div class="text-muted">{{ p.createTime }}</div>
              </div>
            </div>
            <p class="post-content">{{ p.content }}</p>
            <div v-if="p.imageUrls" class="post-images">
              <img v-for="url in p.imageUrls.split(',').filter(Boolean)" :key="url" :src="url" />
            </div>
            <div class="post-actions">
              <el-button text :type="p._liked ? 'danger' : ''" @click="like(p)">
                <el-icon><Star /></el-icon> {{ p.likeCount }}
              </el-button>
              <el-button text @click="openComment(p)">
                <el-icon><ChatDotRound /></el-icon> {{ p.commentCount }}
              </el-button>
            </div>
            <div v-if="commentingId === p.id" class="comment-box">
              <el-input v-model="commentContent" placeholder="评论…" @keyup.enter="submitComment(p)" />
              <el-button type="primary" @click="submitComment(p)">提交</el-button>
            </div>
          </el-card>
          <el-empty v-if="!posts.length" description="还没有动态，快来发布第一条吧" />
        </div>
      </el-tab-pane>

      <!-- 队伍时间线 -->
      <el-tab-pane label="队伍时间线" name="timeline">
        <el-card style="margin-top: 8px" v-loading="eventsLoading">
          <el-empty v-if="!eventsLoading && !events.length" description="暂无队伍事件" />
          <el-timeline v-else class="tl">
            <el-timeline-item
              v-for="e in events"
              :key="e.id"
              :timestamp="e.createTime"
              :type="EVENT_TYPE_TAG[e.type] || 'primary'"
              :hollow="true"
              placement="top"
            >
              <div class="event-line">
                <el-tag size="small" :type="EVENT_TYPE_TAG[e.type] || 'info'" effect="plain">
                  {{ EVENT_TYPE_MAP[e.type] || e.type }}
                </el-tag>
                <span class="event-content">{{ e.content }}</span>
              </div>
              <div v-if="e.actorId" class="text-muted event-actor">操作人：{{ authorName(e.actorId) }}</div>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { pageTeamPosts, publishTeamPost, commentPost, likePost, listTeamEvents } from '@/api/teamExtra'
import { getTeam } from '@/api/team'

const route = useRoute()
const teamId = route.params.id
const tab = ref(route.query.tab === 'timeline' ? 'timeline' : 'posts')

const form = reactive({ content: '', imageUrls: '' })
const posts = ref([])
const events = ref([])
const eventsLoading = ref(false)
const members = ref([])
const commentingId = ref(null)
const commentContent = ref('')

const EVENT_TYPE_MAP = {
  CREATE: '创建队伍', JOIN: '成员加入', LEAVE: '成员退出', POST: '发布动态',
  AWARD: '获奖', ARCHIVE: '归档', TRANSFER: '队长转让', DEPUTY: '任命副队长',
  NOTICE: '发布公告', RECRUIT: '招募更新', TASK: '任务事件'
}
const EVENT_TYPE_TAG = {
  CREATE: 'primary', JOIN: 'success', LEAVE: 'info', POST: 'primary',
  AWARD: 'warning', ARCHIVE: 'danger', TRANSFER: 'warning', DEPUTY: 'success',
  NOTICE: 'primary', RECRUIT: 'success', TASK: 'info'
}

function authorName(id) {
  return members.value.find((m) => m.userId === id)?.nickname || ('队员 #' + id)
}

async function loadMembers() {
  try {
    const detail = await getTeam(teamId)
    members.value = detail.members || []
  } catch { /* 忽略 */ }
}

async function loadPosts() {
  const data = await pageTeamPosts(teamId, { current: 1, size: 30 })
  posts.value = data.records
}

async function loadEvents() {
  eventsLoading.value = true
  try {
    events.value = await listTeamEvents(teamId)
  } finally {
    eventsLoading.value = false
  }
}

async function publish() {
  if (!form.content) { ElMessage.warning('内容不能为空'); return }
  await publishTeamPost(teamId, form)
  ElMessage.success('已发布')
  form.content = ''; form.imageUrls = ''
  await Promise.all([loadPosts(), loadEvents()])
}

async function like(p) {
  const data = await likePost(p.id)
  p._liked = data.liked
  p.likeCount += data.liked ? 1 : -1
}

function openComment(p) {
  commentingId.value = commentingId.value === p.id ? null : p.id
  commentContent.value = ''
}

async function submitComment(p) {
  if (!commentContent.value) return
  await commentPost(p.id, { content: commentContent.value })
  ElMessage.success('已评论')
  commentContent.value = ''
  commentingId.value = null
  await loadPosts()
}

onMounted(async () => {
  await loadMembers()
  await Promise.all([loadPosts(), loadEvents()])
})
</script>

<style scoped>
.wall-tabs { margin-top: 12px; }
.posts { margin-top: 12px; display: flex; flex-direction: column; gap: 12px; }
.post-head { display: flex; gap: 12px; align-items: center; margin-bottom: 10px; }
.post-content { white-space: pre-wrap; line-height: 1.6; }
.post-images { display: flex; gap: 8px; flex-wrap: wrap; }
.post-images img { width: 120px; height: 90px; object-fit: cover; border-radius: 8px; }
.post-actions { margin-top: 10px; display: flex; gap: 10px; }
.comment-box { display: flex; gap: 8px; margin-top: 10px; }
.cl-avatar { background: var(--cl-gradient-hero); color: #fff; font-weight: 700; }
.tl { padding-left: 4px; margin-top: 8px; }
.event-line { display: flex; align-items: center; gap: 8px; }
.event-content { font-weight: 500; }
.event-actor { font-size: 12px; margin-top: 4px; }
</style>
