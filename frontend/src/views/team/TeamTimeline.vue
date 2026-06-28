<template>
  <div class="page-container">
    <el-page-header @back="$router.back()" title="返回">
      <template #content>队伍时间线 #{{ teamId }}</template>
    </el-page-header>

    <el-card style="margin-top: 16px" v-loading="loading">
      <el-empty v-if="!loading && !events.length" description="暂无队伍事件" />
      <el-timeline v-else>
        <el-timeline-item
          v-for="e in events"
          :key="e.id"
          :timestamp="e.createTime"
          :type="EVENT_TYPE_TAG[e.type] || 'primary'"
          :hollow="true"
          placement="top"
        >
          <div class="event-line">
            <el-tag size="small" :type="EVENT_TYPE_TAG[e.type] || 'info'" effect="plain">
              {{ EVENT_TYPE_MAP[e.type] || e.type }}
            </el-tag>
            <span class="event-content">{{ e.content }}</span>
          </div>
          <div v-if="e.actorId" class="text-muted event-actor">操作人 #{{ e.actorId }}</div>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { listTeamEvents } from '@/api/teamExtra'

const route = useRoute()
const teamId = route.params.id
const events = ref([])
const loading = ref(false)

const EVENT_TYPE_MAP = {
  CREATE: '创建队伍',
  JOIN: '成员加入',
  LEAVE: '成员退出',
  POST: '发布动态',
  AWARD: '获奖',
  ARCHIVE: '归档',
  TRANSFER: '队长转让',
  DEPUTY: '任命副队长',
  NOTICE: '发布公告',
  RECRUIT: '招募更新'
}
const EVENT_TYPE_TAG = {
  CREATE: 'primary',
  JOIN: 'success',
  LEAVE: 'info',
  POST: 'primary',
  AWARD: 'warning',
  ARCHIVE: 'danger',
  TRANSFER: 'warning',
  DEPUTY: 'success',
  NOTICE: 'primary',
  RECRUIT: 'success'
}

async function load() {
  loading.value = true
  try {
    events.value = await listTeamEvents(teamId)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.event-line { display: flex; align-items: center; gap: 8px; }
.event-content { font-weight: 500; }
.event-actor { font-size: 12px; margin-top: 4px; }
</style>
