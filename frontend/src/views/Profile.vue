<template>
  <div class="page-container" v-loading="loading">
    <el-card v-if="user" class="profile-card">
      <div class="profile-head">
        <el-avatar :size="80" :src="user.avatar">{{ user.nickname?.charAt(0) }}</el-avatar>
        <div class="profile-meta">
          <div class="name-line">
            <span class="nickname">{{ user.nickname }}</span>
            <el-tag type="info" size="small" effect="plain" class="uid-tag" @click="copyId">
              ID: {{ user.id }} <el-icon class="copy-ic"><CopyDocument /></el-icon>
            </el-tag>
            <el-tag v-if="user.role === 'ADMIN'" type="danger" size="small">管理员</el-tag>
            <span class="reputation">
              <el-icon color="#f7ba2a"><StarFilled /></el-icon>
              信誉分 {{ Number(user.reputation).toFixed(1) }}
            </span>
          </div>
          <div class="text-muted">
            {{ user.college || '—' }} · {{ user.major || '—' }} ·
            {{ user.grade ? user.grade + ' 级' : '—' }}
          </div>
          <div class="text-muted">{{ user.email || '未填写邮箱' }}</div>
        </div>
        <div class="head-ops">
          <el-button v-if="isSelf" type="primary" plain @click="openEdit">编辑资料</el-button>
          <template v-else>
            <el-button type="primary" @click="$router.push(`/private-chat?withUserId=${user.id}`)">
              <el-icon><ChatDotRound /></el-icon> 发私信
            </el-button>
            <el-button plain @click="$router.push(`/evaluations?userId=${user.id}`)">
              <el-icon><Star /></el-icon> TA的评价
            </el-button>
          </template>
        </div>
      </div>

      <el-divider content-position="left">技能标签</el-divider>
      <div class="skills">
        <el-tag
          v-for="s in skills"
          :key="s.id"
          :closable="isSelf"
          type="info"
          effect="plain"
          @close="onRemoveSkill(s.id)"
        >
          {{ s.name }}
        </el-tag>
        <span v-if="!skills.length" class="text-muted">暂无技能标签</span>
        <el-button v-if="isSelf" size="small" link type="primary" @click="skillDialog = true">
          <el-icon><Plus /></el-icon> 添加
        </el-button>
      </div>

      <el-divider content-position="left">个人简介 / 项目经历</el-divider>
      <div class="intro">{{ user.intro || '这个人很懒，什么都没写~' }}</div>
    </el-card>

    <!-- 编辑资料 -->
    <el-dialog v-model="editDialog" title="编辑资料" width="480px">
      <el-form :model="editForm" label-width="64px">
        <el-form-item label="昵称"><el-input v-model="editForm.nickname" /></el-form-item>
        <el-form-item label="头像"><el-input v-model="editForm.avatar" placeholder="头像 URL" /></el-form-item>
        <el-form-item label="学院"><el-input v-model="editForm.college" /></el-form-item>
        <el-form-item label="专业"><el-input v-model="editForm.major" /></el-form-item>
        <el-form-item label="年级"><el-input v-model.number="editForm.grade" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="editForm.email" /></el-form-item>
        <el-form-item label="简介">
          <el-input v-model="editForm.intro" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>

    <!-- 添加技能 -->
    <el-dialog v-model="skillDialog" title="添加技能标签" width="420px">
      <el-select v-model="pickedSkill" placeholder="选择技能" filterable style="width: 100%">
        <el-option
          v-for="s in availableSkills"
          :key="s.id"
          :label="`${s.name}（${s.category}）`"
          :value="s.id"
        />
      </el-select>
      <template #footer>
        <el-button @click="skillDialog = false">取消</el-button>
        <el-button type="primary" :disabled="!pickedSkill" @click="onAddSkill">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CopyDocument, ChatDotRound, Star } from '@element-plus/icons-vue'
import { getMe, getUser, updateProfile, getMySkills, getUserSkills, addSkill, removeSkill } from '@/api/user'
import { listSkills } from '@/api/skill'
import { useUserStore } from '@/store/user'

const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const user = ref(null)
const skills = ref([])
const allSkills = ref([])

const targetId = computed(() => route.params.id ? Number(route.params.id) : null)
const isSelf = computed(() => !targetId.value || targetId.value === userStore.user?.id)

const availableSkills = computed(() => {
  const owned = new Set(skills.value.map((s) => s.id))
  return allSkills.value.filter((s) => !owned.has(s.id))
})

const editDialog = ref(false)
const saving = ref(false)
const editForm = reactive({ nickname: '', avatar: '', college: '', major: '', grade: null, email: '', intro: '' })

const skillDialog = ref(false)
const pickedSkill = ref(null)

async function load() {
  loading.value = true
  try {
    if (isSelf.value) {
      user.value = await getMe()
      skills.value = await getMySkills()
      userStore.setUser(user.value)
    } else {
      user.value = await getUser(targetId.value)
      skills.value = await getUserSkills(targetId.value)
    }
  } finally {
    loading.value = false
  }
}

function openEdit() {
  Object.assign(editForm, {
    nickname: user.value.nickname,
    avatar: user.value.avatar,
    college: user.value.college,
    major: user.value.major,
    grade: user.value.grade,
    email: user.value.email,
    intro: user.value.intro
  })
  editDialog.value = true
}

async function saveProfile() {
  saving.value = true
  try {
    user.value = await updateProfile(editForm)
    userStore.setUser(user.value)
    ElMessage.success('已保存')
    editDialog.value = false
  } finally {
    saving.value = false
  }
}

async function onAddSkill() {
  await addSkill(pickedSkill.value)
  skills.value = await getMySkills()
  pickedSkill.value = null
  skillDialog.value = false
  ElMessage.success('已添加')
}

async function onRemoveSkill(id) {
  await removeSkill(id)
  skills.value = await getMySkills()
}

async function copyId() {
  try {
    await navigator.clipboard.writeText(String(user.value.id))
    ElMessage.success(`已复制 ID：${user.value.id}`)
  } catch {
    ElMessage.info(`用户 ID：${user.value.id}`)
  }
}

onMounted(async () => {
  allSkills.value = await listSkills()
  await load()
})

watch(() => route.params.id, load)
</script>

<style scoped>
.profile-card {
  max-width: 800px;
  margin: 0 auto;
}
.profile-head {
  display: flex;
  align-items: center;
  gap: 20px;
}
.profile-meta {
  flex: 1;
}
.name-line {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}
.nickname {
  font-size: 22px;
  font-weight: 600;
}
.uid-tag {
  cursor: pointer;
  font-family: monospace;
}
.copy-ic {
  font-size: 12px;
  vertical-align: -1px;
}
.head-ops {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-self: flex-start;
}
.reputation {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: #e6a23c;
}
.skills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
.intro {
  white-space: pre-wrap;
  line-height: 1.7;
  color: #606266;
}
</style>
