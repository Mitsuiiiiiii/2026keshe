<template>
  <div class="page-container">
    <div class="cl-hero">
      <h1>获奖排行榜</h1>
      <p>按队伍历年累计获奖数与获奖人数聚合统计</p>
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
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import EChart from '@/components/EChart.vue'
import { awardRanking, awardStats } from '@/api/competitionExtra'

const AWARD_MAP = { FIRST: '一等奖', SECOND: '二等奖', THIRD: '三等奖', EXCELLENT: '优秀奖' }

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

onMounted(async () => {
  loading.value = true
  try {
    ranking.value = (await awardRanking()) || []
    stats.value = (await awardStats()) || {}
  } finally {
    loading.value = false
  }
})
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
