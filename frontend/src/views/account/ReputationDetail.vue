<template>
  <div class="page-container">
    <div class="cl-hero">
      <h1>信誉分明细</h1>
      <p>每一次评价的加权来源都在这里</p>
    </div>

    <div class="stat-grid">
      <div class="stat-card hero">
        <div class="stat-card__label">评价次数</div>
        <div class="stat-card__value">{{ detail.total || 0 }}</div>
      </div>
      <div class="stat-card mint">
        <div class="stat-card__label">责任心均值</div>
        <div class="stat-card__value">{{ detail.avgResponsibility }}</div>
      </div>
      <div class="stat-card sky">
        <div class="stat-card__label">技术能力均值</div>
        <div class="stat-card__value">{{ detail.avgTech }}</div>
      </div>
      <div class="stat-card amber">
        <div class="stat-card__label">沟通能力均值</div>
        <div class="stat-card__value">{{ detail.avgCommunication }}</div>
      </div>
    </div>

    <el-card>
      <template #header><strong>评价明细</strong></template>
      <el-table :data="detail.items || []" stripe>
        <el-table-column label="队伍 id" prop="teamId" width="100" />
        <el-table-column label="评价人 id" prop="fromUserId" width="100" />
        <el-table-column label="责任心" prop="responsibility" width="100" />
        <el-table-column label="技术" prop="tech" width="100" />
        <el-table-column label="沟通" prop="communication" width="100" />
        <el-table-column label="时间" prop="createTime" />
      </el-table>
    </el-card>

    <el-card class="log-card">
      <template #header>
        <div class="log-header">
          <strong>信誉分变动记录</strong>
          <el-radio-group v-model="logView" size="small">
            <el-radio-button label="timeline">时间线</el-radio-button>
            <el-radio-button label="table">表格</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <div v-if="logs.length === 0" class="empty">暂无信誉分变动记录</div>

      <el-timeline v-else-if="logView === 'timeline'">
        <el-timeline-item
          v-for="log in logs"
          :key="log.id"
          :timestamp="log.createTime"
          placement="top"
          :type="changeType(log.changeValue)"
          :hollow="true"
        >
          <div class="log-line">
            <span class="cl-tag" :class="changeType(log.changeValue) === 'success' ? 'success' : 'danger'">
              {{ formatChange(log.changeValue) }}
            </span>
            <span class="log-reason">{{ log.reason || log.sourceType || '信誉分变动' }}</span>
          </div>
          <div class="text-muted">
            {{ log.beforeValue }} → {{ log.afterValue }}
            <template v-if="log.sourceType">· 来源 {{ log.sourceType }}</template>
            <template v-if="log.refId">· 关联 #{{ log.refId }}</template>
          </div>
        </el-timeline-item>
      </el-timeline>

      <el-table v-else :data="logs" stripe>
        <el-table-column label="时间" prop="createTime" width="180" />
        <el-table-column label="变动" width="100">
          <template #default="{ row }">
            <span class="cl-tag" :class="changeType(row.changeValue) === 'success' ? 'success' : 'danger'">
              {{ formatChange(row.changeValue) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="变动前" prop="beforeValue" width="100" />
        <el-table-column label="变动后" prop="afterValue" width="100" />
        <el-table-column label="来源" prop="sourceType" width="100" />
        <el-table-column label="原因" prop="reason" show-overflow-tooltip />
        <el-table-column label="关联 id" prop="refId" width="100" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getReputationDetail, getReputationLog } from '@/api/user'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const detail = ref({})
const logs = ref([])
const logView = ref('timeline')

function formatChange(v) {
  const n = Number(v ?? 0)
  return n >= 0 ? `+${n}` : `${n}`
}

function changeType(v) {
  return Number(v ?? 0) >= 0 ? 'success' : 'danger'
}

onMounted(async () => {
  const id = userStore.user.id
  detail.value = await getReputationDetail(id)
  logs.value = await getReputationLog(id)
})
</script>

<style scoped>
.log-card { margin-top: 16px; }
.log-header { display: flex; justify-content: space-between; align-items: center; }
.log-line { display: flex; align-items: center; gap: 8px; }
.log-reason { font-weight: 600; }
.empty { padding: 40px; text-align: center; color: var(--cl-text-muted); }
</style>
