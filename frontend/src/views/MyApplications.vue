<template>
  <div class="page-container" v-loading="loading">
    <div class="header-row">
      <h2 class="page-title">我的申请</h2>
      <el-button type="primary" @click="openBatch">
        <el-icon><Plus /></el-icon> 批量申请
      </el-button>
    </div>

    <!-- 申请概览 -->
    <div v-if="stat" class="stat-bar">
      <el-card shadow="never" class="stat-card">
        <div class="stat-num">{{ stat.total ?? 0 }}</div>
        <div class="stat-label">总申请</div>
      </el-card>
      <el-card shadow="never" class="stat-card">
        <div class="stat-num pending">{{ stat.pending ?? 0 }}</div>
        <div class="stat-label">待审核</div>
      </el-card>
      <el-card shadow="never" class="stat-card">
        <div class="stat-num approved">{{ stat.approved ?? 0 }}</div>
        <div class="stat-label">已通过</div>
      </el-card>
      <el-card shadow="never" class="stat-card">
        <div class="stat-num rejected">{{ stat.rejected ?? 0 }}</div>
        <div class="stat-label">已拒绝</div>
      </el-card>
      <el-card shadow="never" class="stat-card">
        <div class="stat-num">{{ approveRatePct }}</div>
        <div class="stat-label">通过率</div>
      </el-card>
    </div>

    <el-table :data="list" stripe>
      <el-table-column prop="teamName" label="队伍" min-width="160">
        <template #default="{ row }">
          <el-link type="primary" @click="$router.push(`/teams/${row.teamId}`)">{{ row.teamName }}</el-link>
        </template>
      </el-table-column>
      <el-table-column prop="selfIntro" label="自我介绍" min-width="180" show-overflow-tooltip />
      <el-table-column prop="skillDesc" label="技能描述" min-width="140" show-overflow-tooltip />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="拒绝理由" min-width="140">
        <template #default="{ row }">
          <span class="text-muted">{{ row.status === 'REJECTED' ? (row.rejectReason || '—') : '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="申请时间" width="180">
        <template #default="{ row }">{{ row.createTime }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PENDING'" type="danger" link size="small"
                     @click="cancel(row)">撤回</el-button>
          <span v-else class="text-muted">—</span>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !list.length" description="你还没有提交过申请" />

    <!-- 批量申请弹框 -->
    <el-dialog v-model="batchVisible" title="批量申请加入队伍" width="640px" @open="loadTeams">
      <el-form label-width="84px">
        <el-form-item label="选择队伍">
          <el-select v-model="form.teamIds" multiple filterable :loading="teamLoading"
                     placeholder="从招募中的队伍中选择（可多选）" style="width: 100%">
            <el-option v-for="t in recruitTeams" :key="t.id"
                       :label="teamOptionLabel(t)" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="自我介绍">
          <el-input v-model="form.selfIntro" type="textarea" :rows="3"
                    maxlength="500" show-word-limit placeholder="将统一应用到所选队伍的申请" />
        </el-form-item>
        <el-form-item label="技能描述">
          <el-input v-model="form.skillDesc" type="textarea" :rows="2"
                    maxlength="300" show-word-limit placeholder="你的技能/擅长方向（选填）" />
        </el-form-item>
        <el-form-item label="作品链接">
          <el-input v-model="form.profileLink" placeholder="个人主页 / 作品集链接（选填）" />
        </el-form-item>
      </el-form>
      <div class="tip text-muted">同一份介绍会提交到所选的每个队伍；已申请过或非招募中的队伍会自动跳过。</div>
      <template #footer>
        <el-button @click="batchVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting"
                   :disabled="!form.teamIds.length" @click="submitBatch">
          提交申请（{{ form.teamIds.length }}）
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { myApplies } from '@/api/apply'
import { batchApply, cancelApply, applyStat } from '@/api/taskExtra'
import { pageTeams } from '@/api/team'

const loading = ref(false)
const list = ref([])
const stat = ref(null)

const batchVisible = ref(false)
const submitting = ref(false)
const teamLoading = ref(false)
const recruitTeams = ref([])
const form = reactive({
  teamIds: [],
  selfIntro: '',
  skillDesc: '',
  profileLink: ''
})

const approveRatePct = computed(() => {
  const r = Number(stat.value?.approveRate || 0)
  return `${(r * 100).toFixed(0)}%`
})

function statusLabel(s) {
  return { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝' }[s] || s
}
function statusTag(s) {
  return { PENDING: 'warning', APPROVED: 'success', REJECTED: 'info' }[s]
}
function teamOptionLabel(t) {
  const size = (t.currentSize != null && t.totalSize != null) ? ` (${t.currentSize}/${t.totalSize})` : ''
  return `${t.name}${t.competitionName ? ' · ' + t.competitionName : ''}${size}`
}

async function load() {
  loading.value = true
  try {
    const [applies, s] = await Promise.all([myApplies(), applyStat().catch(() => null)])
    list.value = applies
    stat.value = s
  } finally {
    loading.value = false
  }
}

async function cancel(row) {
  try {
    await ElMessageBox.confirm(`确认撤回对「${row.teamName}」的申请？撤回后需重新申请。`, '撤回申请', {
      type: 'warning',
      confirmButtonText: '确认撤回',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await cancelApply(row.id)
  ElMessage.success('已撤回')
  await load()
}

function openBatch() {
  form.teamIds = []
  form.selfIntro = ''
  form.skillDesc = ''
  form.profileLink = ''
  batchVisible.value = true
}

async function loadTeams() {
  teamLoading.value = true
  try {
    const data = await pageTeams({ current: 1, size: 100, status: 'RECRUITING' })
    recruitTeams.value = data.records || []
  } finally {
    teamLoading.value = false
  }
}

async function submitBatch() {
  if (!form.teamIds.length) return
  submitting.value = true
  try {
    const payload = form.teamIds.map((teamId) => ({
      teamId,
      selfIntro: form.selfIntro,
      skillDesc: form.skillDesc,
      profileLink: form.profileLink
    }))
    const res = await batchApply(payload)
    const success = res?.success ?? 0
    const failed = res?.failed ?? 0
    if (failed > 0) {
      ElMessage.warning(`提交完成：成功 ${success} 个，失败 ${failed} 个`)
    } else {
      ElMessage.success(`已提交 ${success} 个申请`)
    }
    batchVisible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.stat-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.stat-card {
  flex: 1;
  min-width: 110px;
  text-align: center;
}
.stat-num {
  font-size: 24px;
  font-weight: 600;
  line-height: 1.3;
}
.stat-num.pending { color: #e6a23c; }
.stat-num.approved { color: #67c23a; }
.stat-num.rejected { color: #909399; }
.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}
.tip {
  font-size: 12px;
  margin-top: 4px;
}
</style>
