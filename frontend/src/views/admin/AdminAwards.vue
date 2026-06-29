<template>
  <div>
    <h2 class="page-title">获奖榜单管理</h2>
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <strong>历年获奖公示</strong>
          <el-button type="primary" @click="openAdd">新增获奖</el-button>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="竞赛" prop="competitionId" width="90" />
        <el-table-column label="获奖队伍" prop="teamName" min-width="140" />
        <el-table-column label="奖项" prop="awardLevel" width="130" />
        <el-table-column label="年份" prop="awardYear" width="90" />
        <el-table-column label="获奖人数" prop="memberCount" width="90" />
        <el-table-column label="成员" prop="members" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button text type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialog" :title="form.id ? '编辑获奖' : '新增获奖'" width="520px">
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
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listAllAwards, publishAward, updateAward, deleteAward } from '@/api/competitionExtra'
import { pageCompetitions } from '@/api/competition'

const list = ref([])
const competitions = ref([])
const loading = ref(false)
const dialog = ref(false)
const awardLevels = ['国家级一等奖', '国家级二等奖', '国家级三等奖', '省级一等奖', '省级二等奖', '省级三等奖', '优秀奖']

const emptyForm = () => ({ id: null, competitionId: null, teamId: null, teamName: '', awardLevel: '', awardYear: new Date().getFullYear(), memberCount: 0, members: '' })
const form = reactive(emptyForm())

async function loadData() {
  loading.value = true
  try {
    list.value = await listAllAwards()
  } finally {
    loading.value = false
  }
}

async function loadCompetitions() {
  const res = await pageCompetitions({ current: 1, size: 200 })
  competitions.value = res?.records || res || []
}

function reset(data) { Object.assign(form, emptyForm(), data || {}) }

function openAdd() { reset(); dialog.value = true }
function openEdit(row) { reset(row); dialog.value = true }

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
  dialog.value = false
  await loadData()
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除「${row.teamName} - ${row.awardLevel}」？`, '提示', { type: 'warning' })
  await deleteAward(row.id)
  ElMessage.success('已删除')
  await loadData()
}

onMounted(() => { loadData(); loadCompetitions() })
</script>
