<template>
  <div class="page-container" v-loading="loading">
    <el-page-header content="申请管理" @back="$router.back()" />

    <el-tabs v-model="tab" class="tabs">
      <el-tab-pane :label="`待审核 (${pending.length})`" name="pending" />
      <el-tab-pane label="已处理" name="processed" />
    </el-tabs>

    <div v-for="a in shown" :key="a.id" class="apply-card">
      <el-card shadow="hover">
        <div class="apply-head">
          <el-avatar :size="44" :src="a.avatar" @click="$router.push(`/user/${a.userId}`)">
            {{ a.nickname?.charAt(0) }}
          </el-avatar>
          <div class="who">
            <span class="name">{{ a.nickname }}</span>
            <span class="text-muted">
              {{ a.college || '—' }} · {{ a.major || '—' }} ·
              {{ a.grade ? a.grade + '级' : '—' }} · 信誉 {{ Number(a.reputation).toFixed(1) }}
            </span>
          </div>
          <el-tag :type="statusTag(a.status)">{{ statusLabel(a.status) }}</el-tag>
        </div>
        <div class="apply-body">
          <p><b>自我介绍：</b>{{ a.selfIntro || '—' }}</p>
          <p><b>技能说明：</b>{{ a.skillDesc || '—' }}</p>
          <p v-if="a.profileLink">
            <b>个人主页：</b><a :href="a.profileLink" target="_blank">{{ a.profileLink }}</a>
          </p>
          <p v-if="a.status === 'REJECTED' && a.rejectReason" class="text-muted">
            拒绝理由：{{ a.rejectReason }}
          </p>
        </div>
        <div class="apply-ops">
          <template v-if="a.status === 'PENDING'">
            <el-button type="success" @click="onApprove(a)">通过</el-button>
            <el-button type="danger" plain @click="onReject(a)">拒绝</el-button>
          </template>
          <el-button type="warning" plain @click="onBlacklist(a)">
            <el-icon><CircleClose /></el-icon> 拉黑
          </el-button>
        </div>
      </el-card>
    </div>
    <el-empty v-if="!loading && !shown.length" description="暂无申请" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleClose } from '@element-plus/icons-vue'
import { listTeamApplies, auditApply } from '@/api/apply'
import { addBlacklist } from '@/api/teamExtra'

const route = useRoute()
const teamId = route.params.id

const loading = ref(false)
const tab = ref('pending')
const applies = ref([])

const pending = computed(() => applies.value.filter((a) => a.status === 'PENDING'))
const processed = computed(() => applies.value.filter((a) => a.status !== 'PENDING'))
const shown = computed(() => (tab.value === 'pending' ? pending.value : processed.value))

function statusLabel(s) {
  return { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝' }[s]
}
function statusTag(s) {
  return { PENDING: 'warning', APPROVED: 'success', REJECTED: 'info' }[s]
}

async function load() {
  loading.value = true
  try {
    applies.value = await listTeamApplies(teamId)
  } finally {
    loading.value = false
  }
}

async function onApprove(a) {
  await ElMessageBox.confirm(`通过 ${a.nickname} 的申请？`, '提示', { type: 'success' })
  await auditApply(a.id, { approved: true })
  ElMessage.success('已通过')
  load()
}

async function onReject(a) {
  const { value } = await ElMessageBox.prompt('请输入拒绝理由（可选）', '拒绝申请', {
    confirmButtonText: '确定拒绝',
    cancelButtonText: '取消',
    inputType: 'textarea'
  })
  await auditApply(a.id, { approved: false, reason: value })
  ElMessage.success('已拒绝')
  load()
}

async function onBlacklist(a) {
  const { value } = await ElMessageBox.prompt(
    `将「${a.nickname}」加入队伍黑名单后，对方将无法再次申请。请输入拉黑原因（可选）`,
    '加入黑名单',
    { confirmButtonText: '确认拉黑', cancelButtonText: '取消', inputType: 'textarea' }
  )
  await addBlacklist({ teamId, userId: a.userId, reason: value || '恶意申请' })
  ElMessage.success('已加入黑名单')
}

onMounted(load)
</script>

<style scoped>
.tabs {
  margin-top: 12px;
}
.apply-card {
  margin-bottom: 12px;
}
.apply-head {
  display: flex;
  align-items: center;
  gap: 12px;
}
.who {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
}
.who .name {
  font-weight: 600;
  font-size: 15px;
}
.apply-body {
  margin: 12px 0;
  line-height: 1.7;
  font-size: 14px;
  color: #606266;
}
.apply-body p {
  margin: 4px 0;
}
.apply-ops {
  display: flex;
  gap: 12px;
}
</style>
