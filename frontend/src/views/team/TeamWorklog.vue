<template>
  <div class="page-container">
    <el-page-header @back="$router.push(`/teams/${teamId}`)" title="返回">
      <template #content>工时填报 #{{ teamId }}</template>
    </el-page-header>

    <el-card style="margin-top: 16px">
      <template #header><strong>填报每日工时</strong></template>
      <el-form :model="form" label-width="72px" class="wl-form">
        <el-form-item label="关联任务">
          <el-select v-model="form.taskId" placeholder="选择任务" filterable style="width: 240px">
            <el-option v-for="t in tasks" :key="t.id" :label="t.title" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="form.workDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="工时">
          <el-input-number v-model="form.hours" :step="0.5" :min="0.5" :max="24" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" placeholder="今天做了什么" style="width: 240px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submit">保存工时</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 16px">
      <template #header>
        <strong>队伍工时统计报表</strong>
        <el-button text type="primary" style="float: right" @click="exportXlsx">导出 Excel</el-button>
      </template>
      <el-table :data="stat" stripe style="margin-bottom: 16px">
        <el-table-column label="成员" :formatter="(r) => memberName(r.userId)" />
        <el-table-column label="累计工时(小时)" prop="hours" />
      </el-table>
      <EChart v-if="option" :option="option" height="320px" />
      <el-empty v-else description="暂无工时数据" />
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import EChart from '@/components/EChart.vue'
import { logWork, teamWorklogStat } from '@/api/taskExtra'
import { listTeamTasks } from '@/api/task'
import { getTeam } from '@/api/team'
import { useUserStore } from '@/store/user'

const route = useRoute()
const userStore = useUserStore()
const teamId = route.params.id
const form = reactive({ taskId: null, workDate: new Date().toISOString().slice(0, 10), hours: 1, content: '' })
const stat = ref([])
const tasks = ref([])
const members = ref([])

function memberName(id) {
  return members.value.find((m) => m.userId === id)?.nickname || ('成员 #' + id)
}

const option = computed(() => stat.value.length && ({
  tooltip: {},
  xAxis: { type: 'category', data: stat.value.map(s => memberName(s.userId)) },
  yAxis: { type: 'value', name: '小时' },
  series: [{
    type: 'bar', data: stat.value.map(s => Number(s.hours)),
    itemStyle: { color: '#4f46e5', borderRadius: [6, 6, 0, 0] }
  }]
}))

async function submit() {
  if (!form.taskId) { ElMessage.warning('请选择关联任务'); return }
  await logWork({ ...form })
  ElMessage.success('已填报')
  await loadStat()
}

async function loadStat() { stat.value = await teamWorklogStat(teamId) }

async function exportXlsx() {
  const res = await fetch(`/api/worklog/team/${teamId}/export`, {
    headers: { Authorization: `Bearer ${userStore.token}` }
  })
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a'); a.href = url; a.download = `worklog-${teamId}.xlsx`; a.click()
  URL.revokeObjectURL(url)
}

onMounted(async () => {
  try {
    const detail = await getTeam(teamId)
    members.value = detail.members || []
    tasks.value = await listTeamTasks(teamId)
  } catch { /* 忽略 */ }
  await loadStat()
})
</script>

<style scoped>
.wl-form { display: flex; flex-wrap: wrap; gap: 4px 16px; }
</style>
