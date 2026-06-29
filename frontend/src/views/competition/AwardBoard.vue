<template>
  <div class="page-container">
    <div class="cl-hero" style="display:flex;justify-content:space-between;align-items:flex-start">
      <div>
        <h1>获奖排行榜</h1>
        <p>按队伍历年累计获奖数与获奖人数聚合统计</p>
      </div>
      <el-button v-if="isAdmin" type="primary" @click="openMgmt">
        <el-icon style="margin-right:4px"><Setting /></el-icon>管理榜单
      </el-button>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :sm="8">
        <el-card class="stat-card">
          <div class="stat-num">{{ stats.totalTeams || 0 }}</div>
          <div class="stat-label">获奖队伍总数</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card class="stat-card">
          <div class="stat-num">{{ stats.totalMembers || 0 }}</div>
          <div class="stat-label">获奖人数总计</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card class="stat-card">
          <div class="stat-num">{{ ranking.length }}</div>
          <div class="stat-label">上榜队伍数</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 16px" v-loading="loading">
      <template #header>获奖排行榜 Top {{ ranking.length }}</template>
      <EChart v-if="ranking.length" :option="rankOption" height="360px" />
      <el-empty v-else description="暂无获奖数据" />
    </el-card>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :xs="24" :sm="12">
        <el-card v-loading="loading">
          <template #header>按奖项等级分布</template>
          <EChart v-if="levelData.length" :option="levelOption" height="320px" />
          <el-empty v-else description="暂无数据" />
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-card v-loading="loading">
          <template #header>按获奖年份分布</template>
          <EChart v-if="yearData.length" :option="yearOption" height="320px" />
          <el-empty v-else description="暂无数据" />
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 16px">
      <template #header>获奖排行明细</template>
      <el-table :data="ranking" stripe>
        <el-table-column label="名次" width="80">
          <template #default="{ $index }">
            <span class="medal" :class="$index < 3 ? ['gold','silver','bronze'][$index] : ''">{{ $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="队伍" prop="teamName" />
        <el-table-column label="累计获奖数" prop="awardCount" width="140" />
        <el-table-column label="累计获奖人数" prop="memberCount" width="140" />
      </el-table>
    </el-card>

    <!-- 管理窗口（仅管理员） -->
    <el-dialog v-model="mgmt" title="获奖榜单管理" width="900px" top="6vh">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
        <strong>历年获奖公示</strong>
        <el-button type="primary" @click="openAdd">新增获奖</el-button>
      </div>
      <el-table :data="awards" v-loading="mgmtLoading" stripe max-height="440">
        <el-table-column label="ID" prop="id" width="64" />
        <el-table-column label="竞赛" prop="competitionId" width="80" />
        <el-table-column label="获奖队伍" prop="teamName" min-width="130" />
        <el-table-column label="奖项" prop="awardLevel" width="130" />
        <el-table-column label="年份" prop="awardYear" width="80" />
        <el-table-column label="人数" prop="memberCount" width="70" />
        <el-table-column label="成员" prop="members" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button text type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 新增/编辑表单窗口 -->
    <el-dialog v-model="formDialog" :title="form.id ? '编辑获奖' : '新增获奖'" width="520px" append-to-body>
      <el-form :model="form" label-width="90px">
        <el-form-item label="竞赛" required>
          <el-select v-model="form.competitionId" filterable placeholder="选择竞赛" :disabled="!!form.id" style="width:100%">
            <el-option v-for="c in competitions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="获奖队伍" required>
          <el-input v-model="form.teamName" placeholder="队伍名称" />
        </el-form-item>
        <el-form-item label="队伍ID">
          <el-input v-model.number="form.teamId" placeholder="可空，关联系统内队伍" />
        </el-form-item>
        <el-form-item label="奖项等级" required>
          <el-select v-model="form.awardLevel" placeholder="选择奖项" style="width:100%">
            <el-option v-for="o in awardLevels" :key="o" :label="o" :value="o" />
          </el-select>
        </el-form-item>
        <el-form-item label="获奖年份" required>
          <el-input-number v-model="form.awardYear" :min="2000" :max="2100" />
        </el-form-item>
        <el-form-item label="获奖人数">
          <el-input-number v-model="form.memberCount" :min="0" />
        </el-form-item>
        <el-form-item label="成员名单">
          <el-input v-model="form.members" placeholder="逗号分隔，如 张三,李四" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import EChart from '@/components/EChart.vue'
import { useUserStore } from '@/store/user'
import {
  awardRanking, awardStats,
  listAllAwards, publishAward, updateAward, deleteAward
} from '@/api/competitionExtra'
import { pageCompetitions } from '@/api/competition'

const AWARD_MAP = { FIRST: '一等奖', SECOND: '二等奖', THIRD: '三等奖', EXCELLENT: '优秀奖' }

const userStore = useUserStore()
const isAdmin = computed(() => userStore.user?.role === 'ADMIN')

const loading = ref(false)
const ranking = ref([])
const stats = ref({})

const levelData = computed(() => {
  const by = stats.value.byLevel || {}
  return Object.keys(by).map((k) => ({ name: AWARD_MAP[k] || k, value: by[k] }))
})
const yearData = computed(() => {
  const by = stats.value.byYear || {}
  return Object.keys(by).map((k) => ({ name: String(k), value: by[k] }))
})

const rankOption = computed(() => {
  const names = ranking.value.map((r) => r.teamName)
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['获奖数', '获奖人数'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: names, axisLabel: { interval: 0, rotate: names.length > 6 ? 30 : 0 } },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      { name: '获奖数', type: 'bar', data: ranking.value.map((r) => r.awardCount), itemStyle: { color: '#409eff' } },
      { name: '获奖人数', type: 'bar', data: ranking.value.map((r) => r.memberCount), itemStyle: { color: '#67c23a' } }
    ]
  }
})

const levelOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0 },
  series: [{
    type: 'pie',
    radius: ['40%', '70%'],
    data: levelData.value,
    label: { formatter: '{b}: {c}' }
  }]
}))

const yearOption = computed(() => {
  const sorted = [...yearData.value].sort((a, b) => Number(a.name) - Number(b.name))
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: sorted.map((d) => d.name) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{ type: 'bar', data: sorted.map((d) => d.value), itemStyle: { color: '#e6a23c' }, barWidth: '50%' }]
  }
})

async function loadBoard() {
  loading.value = true
  try {
    ranking.value = (await awardRanking()) || []
    stats.value = (await awardStats()) || {}
  } finally {
    loading.value = false
  }
}

/* ===== 管理（仅管理员） ===== */
const mgmt = ref(false)
const mgmtLoading = ref(false)
const formDialog = ref(false)
const awards = ref([])
const competitions = ref([])
const awardLevels = ['国家级一等奖', '国家级二等奖', '国家级三等奖', '省级一等奖', '省级二等奖', '省级三等奖', '优秀奖']

const emptyForm = () => ({ id: null, competitionId: null, teamId: null, teamName: '', awardLevel: '', awardYear: new Date().getFullYear(), memberCount: 0, members: '' })
const form = reactive(emptyForm())

async function loadAwards() {
  mgmtLoading.value = true
  try {
    awards.value = await listAllAwards()
  } finally {
    mgmtLoading.value = false
  }
}

async function openMgmt() {
  mgmt.value = true
  await loadAwards()
  if (!competitions.value.length) {
    const res = await pageCompetitions({ current: 1, size: 200 })
    competitions.value = res?.records || res || []
  }
}

function reset(data) { Object.assign(form, emptyForm(), data || {}) }
function openAdd() { reset(); formDialog.value = true }
function openEdit(row) { reset(row); formDialog.value = true }

async function save() {
  if (!form.competitionId) { ElMessage.warning('请选择竞赛'); return }
  if (!form.teamName) { ElMessage.warning('请填写获奖队伍'); return }
  if (!form.awardLevel) { ElMessage.warning('请选择奖项等级'); return }
  const payload = {
    teamId: form.teamId || null,
    teamName: form.teamName,
    awardLevel: form.awardLevel,
    awardYear: form.awardYear,
    memberCount: form.memberCount,
    members: form.members
  }
  if (form.id) {
    await updateAward(form.id, payload)
    ElMessage.success('已更新')
  } else {
    await publishAward(form.competitionId, payload)
    ElMessage.success('已新增')
  }
  formDialog.value = false
  await loadAwards()
  await loadBoard()
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除「${row.teamName} - ${row.awardLevel}」？`, '提示', { type: 'warning' })
  await deleteAward(row.id)
  ElMessage.success('已删除')
  await loadAwards()
  await loadBoard()
}

onMounted(loadBoard)
</script>

<style scoped>
.stat-card { text-align: center; }
.stat-num { font-size: 32px; font-weight: 700; color: var(--cl-primary, #409eff); }
.stat-label { color: #909399; margin-top: 6px; }
.medal { display: inline-block; width: 28px; height: 28px; line-height: 28px; text-align: center; border-radius: 50%; background: var(--cl-primary-soft, #ecf5ff); color: var(--cl-primary, #409eff); font-weight: 700; }
.medal.gold { background: linear-gradient(135deg, #fde68a, #f59e0b); color: #fff; }
.medal.silver { background: linear-gradient(135deg, #e2e8f0, #94a3b8); color: #fff; }
.medal.bronze { background: linear-gradient(135deg, #fecaca, #b45309); color: #fff; }
</style>
