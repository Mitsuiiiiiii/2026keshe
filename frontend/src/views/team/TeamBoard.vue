<template>
  <div class="page-container" v-loading="loading">
    <div class="board-head">
      <el-page-header content="任务看板" @back="$router.push(`/teams/${teamId}`)" />
      <el-button type="primary" @click="openCreate()">
        <el-icon><Plus /></el-icon> 新建任务
      </el-button>
    </div>

    <!-- 进度统计柱状图 -->
    <el-card class="stat-card">
      <div class="stat-title">整体进度（共 {{ stat.total }} 个任务）</div>
      <div class="bars">
        <div v-for="b in bars" :key="b.key" class="bar-row">
          <span class="bar-label">{{ b.label }}</span>
          <div class="bar-track">
            <div class="bar-fill" :style="{ width: pct(b.value) + '%', background: b.color }">
              <span v-if="b.value" class="bar-num">{{ b.value }}</span>
            </div>
          </div>
          <span class="bar-pct">{{ pct(b.value) }}%</span>
        </div>
      </div>
    </el-card>

    <!-- 三栏看板 -->
    <div class="board">
      <div v-for="col in columns" :key="col.status" class="column">
        <div class="column-head" :style="{ borderColor: col.color }">
          <span>{{ col.label }}</span>
          <el-tag size="small" round>{{ col.list.length }}</el-tag>
        </div>
        <draggable
          :list="col.list"
          group="tasks"
          item-key="id"
          class="card-list"
          :animation="180"
          @change="(e) => onChange(e, col.status)"
        >
          <template #item="{ element }">
            <div class="task-card" @click="openEdit(element)">
              <div class="task-title-row">
                <el-tag :type="priorityTag(element.priority)" size="small" effect="dark" class="prio-tag">
                  {{ priorityLabel(element.priority) }}
                </el-tag>
                <span class="task-title">{{ element.title }}</span>
              </div>
              <div v-if="element.tags" class="task-tags">
                <el-tag v-for="t in element.tags.split(',').filter(Boolean)" :key="t"
                        size="small" effect="plain" class="tg">{{ t.trim() }}</el-tag>
              </div>
              <div v-if="element.description" class="task-desc">{{ element.description }}</div>
              <div class="task-foot">
                <span v-if="element.assigneeName" class="assignee">
                  <el-icon><User /></el-icon> {{ element.assigneeName }}
                </span>
                <span v-else class="text-muted">未指派</span>
                <span v-if="element.deadline" class="deadline">
                  {{ element.deadline.slice(5, 10) }}
                </span>
              </div>
            </div>
          </template>
        </draggable>
      </div>
    </div>

    <!-- 任务新建 / 编辑 -->
    <el-dialog v-model="dialog" :title="editing ? '编辑任务' : '新建任务'" width="620px">
      <el-form :model="form" label-width="72px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="任务标题" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-radio-group v-model="form.priority">
            <el-radio-button value="LOW">低</el-radio-button>
            <el-radio-button value="MEDIUM">中</el-radio-button>
            <el-radio-button value="HIGH">高</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="form.tags" placeholder="多个标签用逗号分隔，如：前端,紧急" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="form.assigneeId" placeholder="选择成员" clearable style="width: 100%">
            <el-option v-for="m in members" :key="m.userId" :label="m.nickname" :value="m.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止">
          <el-date-picker v-model="form.deadline" type="datetime"
                          value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
      </el-form>

      <!-- 子任务 / 评论：仅编辑已存在任务时显示 -->
      <template v-if="editing">
        <el-divider content-position="left">子任务（{{ subtasks.length }}）</el-divider>
        <div class="sub-list">
          <div v-for="s in subtasks" :key="s.id" class="sub-row">
            <el-checkbox :model-value="s.status === 'DONE'"
                         @change="(v) => toggleSubtask(s, v)" />
            <span :class="{ 'sub-done': s.status === 'DONE' }">{{ s.title }}</span>
            <span v-if="s.assigneeName" class="text-muted sub-assignee">@{{ s.assigneeName }}</span>
          </div>
          <el-empty v-if="!subtasks.length" description="暂无子任务" :image-size="48" />
        </div>
        <div class="sub-add">
          <el-input v-model="newSubtask" placeholder="新增子任务标题" @keyup.enter="addSub" />
          <el-button type="primary" @click="addSub">添加</el-button>
        </div>

        <el-divider content-position="left">评论沟通（{{ comments.length }}）</el-divider>
        <div class="comment-list">
          <div v-for="c in comments" :key="c.id" class="comment-row">
            <div class="comment-meta">
              <span class="comment-author">{{ memberName(c.authorId) }}</span>
              <span class="text-muted">{{ c.createTime }}</span>
            </div>
            <div class="comment-content">{{ c.content }}</div>
          </div>
          <el-empty v-if="!comments.length" description="还没有评论" :image-size="48" />
        </div>
        <div class="sub-add">
          <el-input v-model="newComment" placeholder="写下你的评论…" @keyup.enter="addComment" />
          <el-button type="primary" @click="addComment">发送</el-button>
        </div>
      </template>

      <template #footer>
        <el-button v-if="editing" type="danger" plain style="float: left" @click="onDelete">删除</el-button>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import draggable from 'vuedraggable'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTeam } from '@/api/team'
import {
  listTeamTasks, taskStat, createTask, updateTask, updateTaskStatus, deleteTask
} from '@/api/task'
import {
  listSubtasks, createSubtask, updateSubtaskStatus,
  listTaskComments, commentTask
} from '@/api/taskExtra'

const route = useRoute()
const teamId = route.params.id

const PRIORITY_LABEL = { LOW: '低', MEDIUM: '中', HIGH: '高' }
const PRIORITY_TAG = { LOW: 'info', MEDIUM: 'warning', HIGH: 'danger' }
const priorityLabel = (p) => PRIORITY_LABEL[p] || '中'
const priorityTag = (p) => PRIORITY_TAG[p] || 'warning'
const memberName = (id) => members.value.find((m) => m.userId === id)?.nickname || ('用户 #' + id)

const loading = ref(false)
const members = ref([])
const todo = ref([])
const doing = ref([])
const done = ref([])
const stat = reactive({ todo: 0, doing: 0, done: 0, total: 0 })

const columns = computed(() => [
  { status: 'TODO', label: '待办', color: '#909399', list: todo.value },
  { status: 'DOING', label: '进行中', color: '#e6a23c', list: doing.value },
  { status: 'DONE', label: '已完成', color: '#67c23a', list: done.value }
])

const bars = computed(() => [
  { key: 'todo', label: '待办', value: stat.todo, color: '#909399' },
  { key: 'doing', label: '进行中', value: stat.doing, color: '#e6a23c' },
  { key: 'done', label: '已完成', value: stat.done, color: '#67c23a' }
])

function pct(v) {
  return stat.total ? Math.round((v / stat.total) * 100) : 0
}

const dialog = ref(false)
const editing = ref(false)
const saving = ref(false)
const form = reactive({ id: null, title: '', description: '', assigneeId: null, deadline: null, priority: 'MEDIUM', tags: '' })

// 子任务 / 评论
const subtasks = ref([])
const comments = ref([])
const newSubtask = ref('')
const newComment = ref('')

async function loadTasks() {
  const tasks = await listTeamTasks(teamId)
  todo.value = tasks.filter((t) => t.status === 'TODO')
  doing.value = tasks.filter((t) => t.status === 'DOING')
  done.value = tasks.filter((t) => t.status === 'DONE')
  Object.assign(stat, await taskStat(teamId))
}

async function onChange(evt, status) {
  // 仅当卡片被拖入本列时更新其状态
  if (evt.added) {
    const task = evt.added.element
    try {
      await updateTaskStatus(task.id, { status, sortOrder: evt.added.newIndex })
      Object.assign(stat, await taskStat(teamId))
    } catch {
      loadTasks()
    }
  }
}

function openCreate() {
  editing.value = false
  subtasks.value = []
  comments.value = []
  Object.assign(form, { id: null, title: '', description: '', assigneeId: null, deadline: null, priority: 'MEDIUM', tags: '' })
  dialog.value = true
}

async function openEdit(task) {
  editing.value = true
  Object.assign(form, {
    id: task.id,
    title: task.title,
    description: task.description,
    assigneeId: task.assigneeId,
    deadline: task.deadline,
    priority: task.priority || 'MEDIUM',
    tags: task.tags || ''
  })
  dialog.value = true
  await loadSubtasksAndComments(task.id)
}

async function loadSubtasksAndComments(taskId) {
  subtasks.value = []
  comments.value = []
  try {
    subtasks.value = await listSubtasks(taskId)
    comments.value = await listTaskComments(taskId)
  } catch { /* 忽略加载失败 */ }
}

async function addSub() {
  if (!newSubtask.value.trim()) return
  await createSubtask({ parentId: form.id, title: newSubtask.value.trim() })
  newSubtask.value = ''
  subtasks.value = await listSubtasks(form.id)
}

async function toggleSubtask(s, checked) {
  await updateSubtaskStatus(s.id, { status: checked ? 'DONE' : 'TODO' })
  subtasks.value = await listSubtasks(form.id)
}

async function addComment() {
  if (!newComment.value.trim()) return
  await commentTask(form.id, { content: newComment.value.trim() })
  newComment.value = ''
  comments.value = await listTaskComments(form.id)
}

async function save() {
  if (!form.title?.trim()) {
    ElMessage.warning('请输入任务标题')
    return
  }
  saving.value = true
  try {
    if (editing.value) {
      await updateTask(form.id, form)
    } else {
      await createTask({ teamId, ...form })
    }
    ElMessage.success('已保存')
    dialog.value = false
    loadTasks()
  } finally {
    saving.value = false
  }
}

async function onDelete() {
  await ElMessageBox.confirm('确定删除该任务？', '提示', { type: 'warning' })
  await deleteTask(form.id)
  ElMessage.success('已删除')
  dialog.value = false
  loadTasks()
}

onMounted(async () => {
  loading.value = true
  try {
    const detail = await getTeam(teamId)
    members.value = detail.members || []
    await loadTasks()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.board-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.stat-card {
  margin: 16px 0;
}
.stat-title {
  font-weight: 600;
  margin-bottom: 14px;
}
.bars {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.bar-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.bar-label {
  width: 56px;
  font-size: 13px;
  color: #606266;
}
.bar-track {
  flex: 1;
  height: 22px;
  background: #f0f2f5;
  border-radius: 4px;
  overflow: hidden;
}
.bar-fill {
  height: 100%;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  transition: width 0.4s ease;
  min-width: 2px;
}
.bar-num {
  color: #fff;
  font-size: 12px;
  padding-right: 6px;
}
.bar-pct {
  width: 40px;
  text-align: right;
  font-size: 13px;
  color: #909399;
}
.board {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.column {
  flex: 1;
  background: #f0f2f5;
  border-radius: 8px;
  padding: 12px;
  min-height: 200px;
}
.column-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  padding-bottom: 8px;
  margin-bottom: 10px;
  border-bottom: 2px solid;
}
.card-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 120px;
}
.task-card {
  background: #fff;
  border-radius: 6px;
  padding: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  cursor: grab;
}
.task-card:active {
  cursor: grabbing;
}
.task-title-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}
.task-title {
  font-weight: 500;
}
.prio-tag {
  flex-shrink: 0;
}
.task-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 6px;
}
.task-tags .tg {
  font-size: 11px;
}
.sub-list {
  max-height: 160px;
  overflow-y: auto;
  margin-bottom: 8px;
}
.sub-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
}
.sub-done {
  text-decoration: line-through;
  color: #909399;
}
.sub-assignee {
  font-size: 12px;
}
.sub-add {
  display: flex;
  gap: 8px;
  margin-bottom: 4px;
}
.comment-list {
  max-height: 180px;
  overflow-y: auto;
  margin-bottom: 8px;
}
.comment-row {
  padding: 6px 0;
  border-bottom: 1px dashed #ebeef5;
}
.comment-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  margin-bottom: 2px;
}
.comment-author {
  font-weight: 600;
}
.comment-content {
  white-space: pre-wrap;
  line-height: 1.5;
  color: #303133;
}
.task-desc {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.task-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
}
.assignee {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  color: #3b6ef5;
}
.deadline {
  color: #e6a23c;
}
</style>
