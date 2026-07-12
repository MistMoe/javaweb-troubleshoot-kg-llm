<template>
  <section class="profile-view">
    <div class="profile-workspace">
      <aside class="profile-identity-rail">
        <div class="profile-avatar">{{ avatarText }}</div>
        <p class="profile-eyebrow">PERSONAL ACCOUNT</p>
        <h1>{{ displayName }}</h1>
        <p class="profile-username">@{{ currentUser.username }}</p>

        <div class="profile-role-list">
          <span v-for="role in currentUser.roles || []" :key="role.roleCode">
            {{ role.roleName }} · {{ role.roleCode }}
          </span>
        </div>

        <div class="profile-account-state">
          <i :class="{ disabled: currentUser.status === 0 }"></i>
          <span>{{ currentUser.status === 0 ? '账号已停用' : '账号正常启用' }}</span>
        </div>

        <dl class="profile-metrics" aria-label="个人工作数据">
          <div>
            <dt>分析记录</dt>
            <dd>{{ histories.length }}</dd>
          </div>
          <div>
            <dt>收藏案例</dt>
            <dd>{{ favorites.length }}</dd>
          </div>
          <div>
            <dt>经验投稿</dt>
            <dd>{{ submissions.length }}</dd>
          </div>
        </dl>

        <nav class="profile-shortcuts" aria-label="个人快捷入口">
          <button type="button" @click="$emit('navigate', 'analyze')">开始智能排障 <ArrowRight :size="16" /></button>
          <button type="button" @click="$emit('navigate', 'favorites')">查看我的收藏 <ArrowRight :size="16" /></button>
          <button type="button" @click="$emit('navigate', 'knowledge')">浏览故障知识库 <ArrowRight :size="16" /></button>
        </nav>
      </aside>

      <main class="profile-editor">
        <header class="profile-editor-header">
          <div>
            <p class="profile-eyebrow">ACCOUNT WORKSPACE</p>
            <h2>个人资料</h2>
            <p>管理公开显示信息与登录安全。用户名、角色和账号状态由系统管理员维护。</p>
          </div>
          <span class="profile-id">ID {{ currentUser.id }}</span>
        </header>

        <section class="profile-section profile-basic-section">
          <div class="profile-section-heading">
            <div>
              <span>01</span>
              <h3>基本资料</h3>
            </div>
            <p>昵称与邮箱保存后会同步到系统顶部用户区。</p>
          </div>

          <form class="profile-form" @submit.prevent="saveProfile">
            <label class="profile-field profile-field--readonly">
              <span>用户名</span>
              <input :value="currentUser.username" type="text" disabled />
              <small>登录标识不可自行修改</small>
            </label>
            <label class="profile-field">
              <span>显示名称</span>
              <input v-model="profileForm.nickname" type="text" maxlength="50" placeholder="填写显示名称" />
              <small>用于顶部用户区和个人身份展示</small>
            </label>
            <label class="profile-field profile-field--wide">
              <span>邮箱</span>
              <input v-model="profileForm.email" type="email" maxlength="120" placeholder="name@example.com" />
              <small>可留空；填写时需使用有效邮箱格式</small>
            </label>

            <div class="profile-actions profile-field--wide">
              <button class="profile-primary-action" type="submit" :disabled="savingProfile || !profileChanged">
                <LoaderCircle v-if="savingProfile" :size="17" class="spin" />
                <Save v-else :size="17" />
                {{ savingProfile ? '保存中' : '保存资料' }}
              </button>
              <button class="profile-secondary-action" type="button" :disabled="savingProfile || !profileChanged" @click="resetProfileForm">
                重置修改
              </button>
            </div>
          </form>
        </section>

        <section class="profile-section profile-security-section">
          <div class="profile-section-heading">
            <div>
              <span>02</span>
              <h3>登录安全</h3>
            </div>
            <p>修改密码前需要验证当前密码，新密码不能少于 6 位。</p>
          </div>

          <form class="profile-form profile-password-form" @submit.prevent="changePassword">
            <label class="profile-field">
              <span>当前密码</span>
              <input v-model="passwordForm.currentPassword" type="password" autocomplete="current-password" placeholder="输入当前密码" />
            </label>
            <label class="profile-field">
              <span>新密码</span>
              <input v-model="passwordForm.newPassword" type="password" autocomplete="new-password" placeholder="至少 6 位" />
            </label>
            <label class="profile-field profile-field--wide">
              <span>确认新密码</span>
              <input v-model="passwordForm.confirmPassword" type="password" autocomplete="new-password" placeholder="再次输入新密码" />
            </label>

            <div class="profile-actions profile-field--wide">
              <button class="profile-outline-action" type="submit" :disabled="changingPassword">
                <LoaderCircle v-if="changingPassword" :size="17" class="spin" />
                <KeyRound v-else :size="17" />
                {{ changingPassword ? '更新中' : '修改密码' }}
              </button>
            </div>
          </form>
        </section>

        <section class="profile-section profile-activity-section">
          <div class="profile-section-heading">
            <div>
              <span>03</span>
              <h3>最近活动</h3>
            </div>
            <p>汇总当前账号最近的排障、收藏与知识补充记录。</p>
          </div>

          <div v-if="loading" class="profile-activity-state">
            <LoaderCircle :size="20" class="spin" /> 正在整理个人数据
          </div>
          <div v-else-if="!activityItems.length" class="profile-activity-state">
            暂无个人活动，完成一次排障、收藏或投稿后会显示在这里。
          </div>
          <div v-else class="profile-activity-list">
            <article v-for="item in activityItems" :key="item.key">
              <i :class="item.tone"></i>
              <div>
                <strong>{{ item.title }}</strong>
                <p>{{ item.description }}</p>
              </div>
              <time>{{ item.time }}</time>
            </article>
          </div>
        </section>
      </main>
    </div>
  </section>
</template>

<script setup>
import './profile-view.css'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { ArrowRight, KeyRound, LoaderCircle, Save } from 'lucide-vue-next'
import { computed, onMounted, reactive, ref, watch } from 'vue'

const props = defineProps({
  currentUser: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['navigate', 'profile-updated'])

const histories = ref([])
const favorites = ref([])
const submissions = ref([])
const loading = ref(false)
const savingProfile = ref(false)
const changingPassword = ref(false)

const profileForm = reactive({ nickname: '', email: '' })
const passwordForm = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })

const avatarText = computed(() => (props.currentUser?.nickname || props.currentUser?.username || 'U').slice(0, 1).toUpperCase())
const displayName = computed(() => props.currentUser?.nickname || props.currentUser?.username || '当前用户')
const profileChanged = computed(() => {
  return profileForm.nickname.trim() !== (props.currentUser?.nickname || '').trim()
    || profileForm.email.trim() !== (props.currentUser?.email || '').trim()
})
const activityItems = computed(() => {
  const items = []
  histories.value.slice(0, 2).forEach(item => {
    items.push({
      key: `history-${item.id}`,
      title: item.matchedErrorName || '智能排障记录',
      description: item.techStack || '未知技术栈',
      time: formatTime(item.createTime),
      tone: item.matched ? 'blue' : 'violet'
    })
  })
  favorites.value.slice(0, 2).forEach(item => {
    items.push({
      key: `favorite-${item.id}`,
      title: `收藏：${item.errorName}`,
      description: item.component || '故障知识库',
      time: formatTime(item.createTime),
      tone: 'cyan'
    })
  })
  submissions.value.slice(0, 2).forEach(item => {
    items.push({
      key: `submission-${item.id}`,
      title: `投稿：${item.draftErrorName}`,
      description: statusText(item.status),
      time: formatTime(item.createTime),
      tone: item.status === 'APPROVED' ? 'blue' : 'violet'
    })
  })
  return items.slice(0, 5)
})

watch(
  () => props.currentUser,
  () => resetProfileForm(),
  { immediate: true, deep: true }
)

function authConfig() {
  return {
    headers: {
      Authorization: `Bearer ${localStorage.getItem('demo_token') || ''}`
    }
  }
}

function resetProfileForm() {
  profileForm.nickname = props.currentUser?.nickname || ''
  profileForm.email = props.currentUser?.email || ''
}

async function saveProfile() {
  if (!profileChanged.value) return
  const email = profileForm.email.trim()
  if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    ElMessage.warning('请输入有效的邮箱地址')
    return
  }

  savingProfile.value = true
  try {
    const response = await axios.put('/api/auth/profile', {
      nickname: profileForm.nickname.trim(),
      email
    }, authConfig())
    emit('profile-updated', response.data)
    ElMessage.success('个人资料已保存')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '个人资料保存失败')
  } finally {
    savingProfile.value = false
  }
}

async function changePassword() {
  if (!passwordForm.currentPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    ElMessage.warning('请完整填写三个密码字段')
    return
  }
  if (passwordForm.newPassword.length < 6) {
    ElMessage.warning('新密码不能少于 6 位')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }

  changingPassword.value = true
  try {
    const response = await axios.put('/api/auth/password', {
      currentPassword: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword
    }, authConfig())
    passwordForm.currentPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    ElMessage.success(response.data.message || '密码修改成功')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '密码修改失败')
  } finally {
    changingPassword.value = false
  }
}

async function loadProfileStats() {
  loading.value = true
  const userId = props.currentUser?.id || 1
  const [historyResult, favoriteResult, submissionResult] = await Promise.allSettled([
    axios.get('/api/analysis-history/list', { params: { userId } }),
    axios.get('/api/favorites/cases', { params: { userId } }),
    axios.get('/api/submissions/list', { params: { submitUserId: userId } })
  ])

  if (historyResult.status === 'fulfilled') histories.value = historyResult.value.data.items || []
  if (favoriteResult.status === 'fulfilled') favorites.value = favoriteResult.value.data.items || []
  if (submissionResult.status === 'fulfilled') submissions.value = submissionResult.value.data.items || []
  if ([historyResult, favoriteResult, submissionResult].every(result => result.status === 'rejected')) {
    ElMessage.error('个人活动数据加载失败')
  }
  loading.value = false
}

function statusText(status) {
  return {
    PENDING: '待审核',
    APPROVED: '已通过',
    REJECTED: '已驳回'
  }[status] || status || '未知状态'
}

function formatTime(value) {
  if (!value) return '时间未知'
  return String(value).replace('T', ' ').slice(0, 16)
}

onMounted(loadProfileStats)
</script>
