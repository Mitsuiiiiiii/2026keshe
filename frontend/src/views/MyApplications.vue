<template>
  <div class="page-container" v-loading="loading">
    <h2 class="page-title">我的申请</h2>
    <el-table :data="list" stripe>
      <el-table-column prop="teamName" label="队伍" min-width="160">
        <template #default="{ row }">
          <el-link type="primary" @click="$router.push(`/teams/${row.teamId}`)">{{ row.teamName }}</el-link>
        </template>
      </el-table-column>
      <el-table-column prop="selfIntro" label="自我介绍" min-width="200" show-overflow-tooltip />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="拒绝理由" min-width="160">
        <template #default="{ row }">
          <span class="text-muted">{{ row.status === 'REJECTED' ? (row.rejectReason || '—') : '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="申请时间" width="180">
        <template #default="{ row }">{{ row.createTime }}</template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !list.length" description="你还没有提交过申请" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { myApplies } from '@/api/apply'

const loading = ref(false)
const list = ref([])

function statusLabel(s) {
  return { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝' }[s]
}
function statusTag(s) {
  return { PENDING: 'warning', APPROVED: 'success', REJECTED: 'info' }[s]
}

onMounted(async () => {
  loading.value = true
  try {
    list.value = await myApplies()
  } finally {
    loading.value = false
  }
})
</script>
