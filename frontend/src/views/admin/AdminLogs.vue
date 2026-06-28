<template>
  <div>
    <h2 class="page-title">操作日志</h2>
    <div class="filter-bar">
      <el-input v-model="keyword" placeholder="操作人 / 路径关键字" clearable style="width: 220px" @keyup.enter="loadData(1)" />
      <el-select v-model="method" placeholder="方法" clearable style="width: 130px">
        <el-option label="POST" value="POST" />
        <el-option label="PUT" value="PUT" />
        <el-option label="DELETE" value="DELETE" />
      </el-select>
      <el-select v-model="status" placeholder="状态" clearable style="width: 130px">
        <el-option label="成功" value="SUCCESS" />
        <el-option label="失败" value="FAIL" />
      </el-select>
      <el-button type="primary" @click="loadData(1)">查询</el-button>
      <el-button @click="exportExcel">导出 Excel</el-button>
      <el-button type="danger" plain @click="clean">清理 30 天前</el-button>
    </div>
    <el-card>
      <el-table :data="logs" stripe>
        <el-table-column label="ID" prop="id" width="80" />
        <el-table-column label="操作人" prop="username" width="120">
          <template #default="{ row }">{{ row.username || '匿名' }}</template>
        </el-table-column>
        <el-table-column label="方法" prop="method" width="80" />
        <el-table-column label="路径" prop="path" show-overflow-tooltip min-width="180" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">{{ row.status === 'SUCCESS' ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="IP" prop="ip" width="130" />
        <el-table-column label="耗时" prop="costMs" width="90">
          <template #default="{ row }">{{ row.costMs }} ms</template>
        </el-table-column>
        <el-table-column label="错误信息" prop="errorMsg" show-overflow-tooltip min-width="160" />
        <el-table-column label="时间" prop="createTime" width="170" />
      </el-table>
      <div class="pager">
        <el-pagination :current-page="page.current" :page-size="page.size" :total="page.total" @current-change="(p) => loadData(p)" layout="prev, pager, next, total" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminLogs, adminCleanLogs } from '@/api/admin'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const keyword = ref('')
const method = ref('')
const status = ref('')
const logs = ref([])
const page = reactive({ current: 1, size: 10, total: 0 })

async function loadData(p = 1) {
  const data = await adminLogs({
    keyword: keyword.value || undefined,
    method: method.value || undefined,
    status: status.value || undefined,
    current: p,
    size: page.size
  })
  logs.value = data.records
  page.current = data.current
  page.total = data.total
}

async function exportExcel() {
  const qs = new URLSearchParams()
  if (keyword.value) qs.append('keyword', keyword.value)
  if (method.value) qs.append('method', method.value)
  if (status.value) qs.append('status', status.value)
  const res = await fetch(`/api/admin/logs/export?${qs.toString()}`, {
    headers: { Authorization: `Bearer ${userStore.token}` }
  })
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = 'operation-log.xlsx'; a.click()
  URL.revokeObjectURL(url)
}

async function clean() {
  await ElMessageBox.confirm('确认清理 30 天前的操作日志？该操作不可恢复', '清理日志', { type: 'warning' })
  const count = await adminCleanLogs(30)
  ElMessage.success(`已清理 ${count} 条`)
  await loadData(1)
}

onMounted(loadData)
</script>

<style scoped>
.filter-bar { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.pager { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
