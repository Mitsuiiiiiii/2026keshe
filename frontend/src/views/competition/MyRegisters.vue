<template>
  <div class="page-container">
    <div class="cl-hero">
      <h1>我的报名</h1>
      <p>查看你作为队长发起的竞赛报名记录与审核状态</p>
    </div>

    <el-card v-loading="loading">
      <el-table :data="list" stripe>
        <el-table-column label="竞赛" prop="competitionName">
          <template #default="{ row }">
            <el-link type="primary" @click="goDetail(row.competitionId)">{{ row.competitionName || ('#' + row.competitionId) }}</el-link>
          </template>
        </el-table-column>
        <el-table-column label="队伍" prop="teamName">
          <template #default="{ row }">{{ row.teamName || ('#' + row.teamId) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="STATUS_TAG[row.status] || 'info'" size="small">{{ STATUS_MAP[row.status] || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核意见" prop="auditReason" show-overflow-tooltip />
        <el-table-column label="报名时间" prop="createTime" width="180">
          <template #default="{ row }">{{ fmt(row.createTime) }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !list.length" description="暂无报名记录" />
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { myRegisters } from '@/api/competitionExtra'

const router = useRouter()
const loading = ref(false)
const list = ref([])

const STATUS_MAP = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝' }
const STATUS_TAG = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }

function fmt(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : ''
}

function goDetail(id) {
  router.push(`/competitions/${id}`)
}

onMounted(async () => {
  loading.value = true
  try {
    list.value = (await myRegisters()) || []
  } finally {
    loading.value = false
  }
})
</script>
