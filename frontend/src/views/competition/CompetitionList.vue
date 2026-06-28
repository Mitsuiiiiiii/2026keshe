<template>
  <div class="page-container">
    <div class="header-row">
      <h2 class="page-title">竞赛信息</h2>
      <el-button v-if="isAdmin" type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon> 录入竞赛
      </el-button>
    </div>

    <div class="filter-bar">
      <el-select v-model="query.type" placeholder="全部类型" clearable style="width: 160px" @change="reload">
        <el-option v-for="t in COMPETITION_TYPES" :key="t.value" :label="t.label" :value="t.value" />
      </el-select>
      <el-input
        v-model="query.keyword"
        placeholder="搜索竞赛名称"
        clearable
        style="width: 240px"
        @keyup.enter="reload"
        @clear="reload"
      />
      <el-button type="primary" @click="reload">搜索</el-button>
    </div>

    <el-row :gutter="16" v-loading="loading">
      <el-col v-for="c in list" :key="c.id" :xs="24" :sm="12" :lg="8">
        <el-card class="comp-card" shadow="hover">
          <div class="comp-top">
            <span class="comp-name">{{ c.name }}</span>
            <el-tag :type="COMPETITION_TYPE_TAG[c.type]" size="small">
              {{ COMPETITION_TYPE_MAP[c.type] }}
            </el-tag>
          </div>
          <p class="comp-intro">{{ c.intro || '暂无简介' }}</p>
          <div class="comp-foot">
            <span class="text-muted">
              <el-icon><Clock /></el-icon>
              报名截止：{{ c.deadline ? c.deadline.slice(0, 10) : '不限' }}
            </span>
            <div class="card-ops">
              <el-button
                v-if="isLogin"
                link
                size="small"
                :type="favMap[c.id] ? 'warning' : 'info'"
                @click.stop="toggleFav(c)"
              >
                <el-icon><StarFilled v-if="favMap[c.id]" /><Star v-else /></el-icon>
                {{ favMap[c.id] ? '已收藏' : '收藏' }}
              </el-button>
              <template v-if="isAdmin">
                <el-button link type="primary" size="small" @click="openEdit(c)">编辑</el-button>
                <el-button link type="danger" size="small" @click="onDelete(c)">删除</el-button>
              </template>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-empty v-if="!loading && !list.length" description="暂无竞赛" />

    <el-pagination
      v-if="total > 0"
      class="pager"
      layout="prev, pager, next, total"
      :total="total"
      :current-page="query.current"
      :page-size="query.size"
      @current-change="onPage"
    />

    <!-- 录入 / 编辑 -->
    <el-dialog v-model="dialog" :title="editing ? '编辑竞赛' : '录入竞赛'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" placeholder="选择类型" style="width: 100%">
            <el-option v-for="t in COMPETITION_TYPES" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="报名截止">
          <el-date-picker
            v-model="form.deadline"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.intro" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  pageCompetitions,
  createCompetition,
  updateCompetition,
  deleteCompetition
} from '@/api/competition'
import { listFavorites, addFavorite, removeFavorite } from '@/api/user'
import { useUserStore } from '@/store/user'
import { COMPETITION_TYPES, COMPETITION_TYPE_MAP, COMPETITION_TYPE_TAG } from '@/constants'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.isAdmin)
const isLogin = computed(() => userStore.isLogin)

// 竞赛id -> 收藏记录id（用于取消收藏）
const favMap = reactive({})

async function loadFavorites() {
  if (!isLogin.value) return
  try {
    const list = await listFavorites('COMPETITION')
    Object.keys(favMap).forEach((k) => delete favMap[k])
    ;(list || []).forEach((f) => { favMap[f.refId] = f.id })
  } catch (e) { /* 忽略收藏加载失败 */ }
}

async function toggleFav(c) {
  try {
    if (favMap[c.id]) {
      await removeFavorite(favMap[c.id])
      delete favMap[c.id]
      ElMessage.success('已取消收藏')
    } else {
      const rec = await addFavorite({ refType: 'COMPETITION', refId: c.id })
      favMap[c.id] = rec.id
      ElMessage.success('已收藏')
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 9, type: '', keyword: '' })

const dialog = ref(false)
const editing = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive({ id: null, name: '', type: '', deadline: null, intro: '' })
const rules = {
  name: [{ required: true, message: '请输入竞赛名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

async function load() {
  loading.value = true
  try {
    const data = await pageCompetitions(query)
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function reload() {
  query.current = 1
  load()
}

function onPage(p) {
  query.current = p
  load()
}

function openCreate() {
  editing.value = false
  Object.assign(form, { id: null, name: '', type: '', deadline: null, intro: '' })
  dialog.value = true
}

function openEdit(c) {
  editing.value = true
  Object.assign(form, { id: c.id, name: c.name, type: c.type, deadline: c.deadline, intro: c.intro })
  dialog.value = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (editing.value) {
      await updateCompetition(form.id, form)
    } else {
      await createCompetition(form)
    }
    ElMessage.success('已保存')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function onDelete(c) {
  await ElMessageBox.confirm(`确定删除竞赛「${c.name}」？`, '提示', { type: 'warning' })
  await deleteCompetition(c.id)
  ElMessage.success('已删除')
  load()
}

onMounted(() => { load(); loadFavorites() })
</script>

<style scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.comp-card {
  margin-bottom: 16px;
}
.comp-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.comp-name {
  font-size: 16px;
  font-weight: 600;
}
.comp-intro {
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
  min-height: 42px;
  margin: 0 0 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.comp-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.comp-foot .text-muted {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.card-ops {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}
.card-ops .el-icon {
  margin-right: 2px;
}
.pager {
  margin-top: 16px;
  justify-content: center;
}
</style>
