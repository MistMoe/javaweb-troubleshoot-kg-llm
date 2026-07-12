<template>
  <section class="user-management-view">
    <div class="users-grid">
      <section class="user-card users-hero-card surface-card surface-card--strong">
        <div>
          <p class="section-kicker">USER CONTROL</p>
          <h1>用户管理</h1>
          <p>维护登录账号、启停状态和角色归属，让不同身份进入系统后看到对应的功能边界。</p>
        </div>
        <div class="hero-actions">
          <button class="action-button" type="button" :disabled="loading" @click="loadAll">
            <RefreshCw :size="16" :stroke-width="1.6" />
            刷新
          </button>
          <button class="action-button action-button--primary" type="button" @click="openCreate">
            <Plus :size="17" :stroke-width="1.8" />
            新增用户
          </button>
        </div>
      </section>

      <section class="user-card users-filter-card surface-card">
        <label>
          <span>关键词</span>
          <input v-model="filters.keyword" placeholder="搜索用户名、昵称或邮箱" @keyup.enter="loadUsers" />
        </label>
        <label>
          <span>角色</span>
          <select v-model="filters.roleCode" @change="loadUsers">
            <option value="">全部角色</option>
            <option v-for="role in roles" :key="role.id" :value="role.roleCode">
              {{ role.roleCode }}
            </option>
          </select>
        </label>
        <label>
          <span>状态</span>
          <select v-model="filters.status" @change="loadUsers">
            <option value="">全部状态</option>
            <option value="1">启用</option>
            <option value="0">停用</option>
          </select>
        </label>
        <button class="action-button query-button" type="button" @click="loadUsers">查询</button>
      </section>

      <section class="user-card users-summary-card surface-card">
        <div class="metric-plate users-summary-metric is-blue">
          <strong>{{ users.length }}</strong>
          <span>账号总数</span>
        </div>
        <div class="metric-plate users-summary-metric is-violet">
          <strong>{{ enabledCount }}</strong>
          <span>启用账号</span>
        </div>
        <div class="metric-plate users-summary-metric is-cyan">
          <strong>{{ roles.length }}</strong>
          <span>系统角色</span>
        </div>
      </section>

      <section class="user-card users-list-card surface-card">
        <div class="panel-heading">
          <div>
            <p class="section-kicker">ACCOUNT LIST</p>
            <h2>账号名册</h2>
          </div>
          <span>{{ users.length }} 条</span>
        </div>

        <div v-if="loading" class="loading-state">
          <span></span>
          <p>正在加载用户...</p>
        </div>
        <div v-else-if="!users.length" class="empty-state">
          <UsersRound :size="42" :stroke-width="1.35" />
          <p>暂无用户数据，可以先新增一个演示账号。</p>
        </div>
        <div v-else class="user-list">
          <article v-for="user in users" :key="user.id" class="user-row">
            <div class="avatar-mark">
              {{ avatarText(user) }}
            </div>
            <div class="user-main">
              <strong>{{ user.nickname || user.username }}</strong>
              <p>{{ user.username }} · {{ user.email || '未填写邮箱' }}</p>
              <div class="role-tags">
                <span v-for="role in user.roles" :key="role.id">{{ role.roleCode }}</span>
              </div>
            </div>
            <span :class="user.status === 1 ? 'status online' : 'status offline'">
              {{ user.status === 1 ? '启用' : '停用' }}
            </span>
            <div class="row-actions">
              <button type="button" @click="openEdit(user)">编辑</button>
              <button type="button" @click="toggleStatus(user)">
                {{ user.status === 1 ? '停用' : '启用' }}
              </button>
            </div>
          </article>
        </div>
      </section>

      <aside class="user-card users-role-panel surface-card">
        <div class="panel-heading">
          <div>
            <p class="section-kicker">ROLE BOUNDARY</p>
            <h2>角色边界</h2>
          </div>
          <ShieldCheck :size="18" :stroke-width="1.45" />
        </div>

        <div class="role-list">
          <article v-for="role in roles" :key="role.id" class="role-item metric-plate">
            <div>
              <strong>{{ role.roleCode }}</strong>
              <span>{{ role.roleName }}</span>
            </div>
            <p>{{ role.description }}</p>
          </article>
        </div>

        <div class="role-tip inset-well">
          <strong>当前逻辑</strong>
          <p>账号登录后根据角色显示菜单：ADMIN 可维护用户，MAINTAINER 维护知识库，USER 进入排障与个人历史。</p>
        </div>
      </aside>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editingUserId ? '编辑用户' : '新增用户'"
      width="520px"
      class="user-editor-dialog"
      destroy-on-close
    >
      <div class="editor-form">
        <label>
          <span>用户名</span>
          <input v-model="form.username" placeholder="例如 qa_user" />
        </label>
        <label>
          <span>昵称</span>
          <input v-model="form.nickname" placeholder="用于页面显示" />
        </label>
        <label>
          <span>邮箱</span>
          <input v-model="form.email" placeholder="name@example.com" />
        </label>
        <label>
          <span>{{ editingUserId ? '新密码（不填则不修改）' : '登录密码' }}</span>
          <input v-model="form.password" type="password" placeholder="默认演示可填 123456" />
        </label>
        <label>
          <span>状态</span>
          <select v-model.number="form.status">
            <option :value="1">启用</option>
            <option :value="0">停用</option>
          </select>
        </label>
        <div class="role-picker">
          <span>角色</span>
          <button
            v-for="role in roles"
            :key="role.id"
            type="button"
            :class="{ selected: form.roleIds.includes(role.id) }"
            @click="toggleRole(role.id)"
          >
            {{ role.roleCode }}
          </button>
        </div>
      </div>
      <template #footer>
        <button class="action-button" type="button" @click="dialogVisible = false">取消</button>
        <button class="action-button action-button--primary" type="button" :disabled="saving" @click="saveUser">
          {{ saving ? '保存中...' : '保存用户' }}
        </button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, RefreshCw, ShieldCheck, UsersRound } from 'lucide-vue-next'
import { computed, onMounted, reactive, ref } from 'vue'

const users = ref([])
const roles = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingUserId = ref(null)

const filters = reactive({
  keyword: '',
  roleCode: '',
  status: ''
})

const form = reactive(emptyForm())
const enabledCount = computed(() => users.value.filter(user => user.status === 1).length)

function emptyForm() {
  return {
    username: '',
    password: '',
    nickname: '',
    email: '',
    status: 1,
    roleIds: []
  }
}

async function loadAll() {
  await Promise.all([loadRoles(), loadUsers()])
}

async function loadRoles() {
  try {
    const response = await axios.get('/api/users/roles')
    roles.value = response.data.items || []
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '角色加载失败')
  }
}

async function loadUsers() {
  loading.value = true
  try {
    const params = {}
    if (filters.keyword.trim()) params.keyword = filters.keyword.trim()
    if (filters.roleCode) params.roleCode = filters.roleCode
    if (filters.status !== '') params.status = filters.status
    const response = await axios.get('/api/users/list', { params })
    users.value = response.data.items || []
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '用户加载失败，请确认后端已启动')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, emptyForm())
  if (roles.value.length) {
    form.roleIds = [roles.value[roles.value.length - 1].id]
  }
  editingUserId.value = null
  dialogVisible.value = true
}

function openEdit(user) {
  Object.assign(form, {
    username: user.username,
    password: '',
    nickname: user.nickname || '',
    email: user.email || '',
    status: user.status,
    roleIds: user.roles.map(role => role.id)
  })
  editingUserId.value = user.id
  dialogVisible.value = true
}

function toggleRole(roleId) {
  if (form.roleIds.includes(roleId)) {
    form.roleIds = form.roleIds.filter(id => id !== roleId)
  } else {
    form.roleIds = [...form.roleIds, roleId]
  }
}

async function saveUser() {
  saving.value = true
  try {
    const payload = {
      username: form.username,
      password: form.password,
      nickname: form.nickname,
      email: form.email,
      status: form.status,
      roleIds: form.roleIds
    }
    if (editingUserId.value) {
      await axios.put(`/api/users/${editingUserId.value}`, payload)
      ElMessage.success('用户已更新')
    } else {
      await axios.post('/api/users/create', payload)
      ElMessage.success('用户已创建')
    }
    dialogVisible.value = false
    await loadUsers()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function toggleStatus(user) {
  const nextStatus = user.status === 1 ? 0 : 1
  try {
    await ElMessageBox.confirm(
      `确认${nextStatus === 1 ? '启用' : '停用'}账号 ${user.username} 吗？`,
      '状态确认',
      { type: 'warning' }
    )
    await axios.put(`/api/users/${user.id}/status`, { status: nextStatus })
    ElMessage.success('状态已更新')
    await loadUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '状态更新失败')
    }
  }
}

function avatarText(user) {
  return (user.nickname || user.username || 'U').slice(0, 1).toUpperCase()
}

onMounted(loadAll)
</script>

<style scoped src="./user-management-view.css"></style>
