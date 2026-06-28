<template>
  <div class="page-container">
    <div class="cl-hero">
      <h1>私信</h1>
      <p>与同学一对一沟通组队意向</p>
    </div>

    <div class="chat-toolbar">
      <span class="label">对方用户 id</span>
      <el-input
        v-model.number="withUserId"
        placeholder="输入对方用户 id"
        style="width: 200px"
        @keyup.enter="loadHistory"
      />
      <el-button type="primary" :disabled="!withUserId" @click="loadHistory">加载会话</el-button>
    </div>

    <el-card class="chat-card">
      <div ref="boxRef" class="chat-box">
        <div v-if="!withUserId" class="empty">请输入对方用户 id 并加载会话</div>
        <div v-else-if="messages.length === 0" class="empty">还没有消息，发条私信打个招呼吧~</div>
        <div
          v-for="m in messages"
          :key="m.id"
          class="msg-row"
          :class="isMine(m) ? 'mine' : 'theirs'"
        >
          <div class="bubble">
            <div class="bubble-content">{{ m.content }}</div>
            <div class="bubble-time">{{ m.createTime }}</div>
          </div>
        </div>
      </div>

      <div class="chat-input">
        <el-input
          v-model="draft"
          type="textarea"
          :rows="2"
          resize="none"
          placeholder="输入私信内容，Enter 发送 / Shift+Enter 换行"
          :disabled="!withUserId"
          @keyup.enter.exact.prevent="send"
        />
        <el-button type="primary" :loading="sending" :disabled="!withUserId || !draft.trim()" @click="send">
          发送
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { sendPrivate, listPrivate } from '@/api/messageExtra'
import { useUserStore } from '@/store/user'

const route = useRoute()
const userStore = useUserStore()

const withUserId = ref(route.query.withUserId ? Number(route.query.withUserId) : null)
const messages = ref([])
const draft = ref('')
const sending = ref(false)
const boxRef = ref()

// PRIVATE 消息：receiverId=接收者，refId=发送者。我发的 => refId === 我的 id
function isMine(m) {
  return Number(m.refId) === Number(userStore.user.id)
}

async function scrollToBottom() {
  await nextTick()
  if (boxRef.value) boxRef.value.scrollTop = boxRef.value.scrollHeight
}

async function loadHistory() {
  if (!withUserId.value) return
  messages.value = await listPrivate(withUserId.value)
  scrollToBottom()
}

async function send() {
  const content = draft.value.trim()
  if (!content || !withUserId.value) return
  sending.value = true
  try {
    await sendPrivate(withUserId.value, content)
    draft.value = ''
    await loadHistory()
    ElMessage.success('已发送')
  } finally {
    sending.value = false
  }
}

onMounted(() => {
  if (withUserId.value) loadHistory()
})
</script>

<style scoped>
.chat-toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.chat-toolbar .label { color: var(--cl-text-muted); }
.chat-card { display: flex; flex-direction: column; }
.chat-box {
  height: 420px;
  overflow-y: auto;
  padding: 8px 4px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.msg-row { display: flex; }
.msg-row.mine { justify-content: flex-end; }
.msg-row.theirs { justify-content: flex-start; }
.bubble {
  max-width: 70%;
  padding: 10px 14px;
  border-radius: 14px;
  background: #f1f5f9;
}
.msg-row.mine .bubble { background: #e0ecff; }
.bubble-content { word-break: break-word; white-space: pre-wrap; }
.bubble-time { font-size: 12px; color: var(--cl-text-muted); margin-top: 4px; }
.chat-input { display: flex; gap: 12px; align-items: flex-end; margin-top: 16px; }
.chat-input .el-textarea { flex: 1; }
.empty { margin: auto; color: var(--cl-text-muted); }
</style>
