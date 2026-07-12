<template>
  <section class="role-management-view">
    <div class="role-grid">
      <section class="role-card roles-hero-card surface-card surface-card--strong">
        <div>
          <p class="role-kicker">ROLE GOVERNANCE</p>
          <h1>角色管理</h1>
          <p>说明系统内三类账号的权限边界：谁维护知识、谁管理账号、谁提交排障。这里用于展示角色逻辑，账号分配在用户管理页完成。</p>
        </div>
        <div class="role-hero-actions">
          <button class="action-button" type="button" :disabled="loading" @click="loadRoles">
            <RefreshCw :size="16" :stroke-width="1.6" />
            刷新角色
          </button>
        </div>
      </section>

      <section class="role-card roles-summary-card surface-card">
        <div class="metric-plate roles-summary-metric is-blue">
          <strong>{{ roles.length || roleBlueprints.length }}</strong>
          <span>系统角色</span>
        </div>
        <div class="metric-plate roles-summary-metric is-violet">
          <strong>{{ menuCount }}</strong>
          <span>菜单节点</span>
        </div>
        <div class="metric-plate roles-summary-metric is-cyan">
          <strong>3</strong>
          <span>权限层级</span>
        </div>
      </section>

      <section class="role-card role-list-card surface-card">
        <div class="role-panel-heading">
          <div>
            <p class="role-kicker">ROLE CARDS</p>
            <h2>角色说明</h2>
          </div>
          <ShieldCheck :size="18" :stroke-width="1.45" />
        </div>

        <div class="role-card-list">
          <article
            v-for="role in enrichedRoles"
            :key="role.roleCode"
            class="role-item-card metric-plate"
            :class="role.roleCode.toLowerCase()"
          >
            <div class="role-item-mark">
              <component :is="role.icon" :size="22" :stroke-width="1.55" />
            </div>
            <div>
              <strong>{{ role.roleCode }}</strong>
              <span>{{ role.roleName }}</span>
              <p>{{ role.description }}</p>
            </div>
          </article>
        </div>
      </section>

      <section class="role-card permission-card surface-card">
        <div class="role-panel-heading">
          <div>
            <p class="role-kicker">ACCESS MATRIX</p>
            <h2>权限矩阵</h2>
          </div>
          <Grid2X2 :size="18" :stroke-width="1.45" />
        </div>

        <div class="permission-table">
          <div class="permission-head">
            <span>功能页面</span>
            <span>ADMIN</span>
            <span>MAINTAINER</span>
            <span>USER</span>
          </div>
          <div v-for="item in permissionRows" :key="item.key" class="permission-row inset-well">
            <strong>{{ item.label }}</strong>
            <em v-for="role in roleCodes" :key="role" :class="{ enabled: item.roles.includes(role) }">
              {{ item.roles.includes(role) ? '可见' : '隐藏' }}
            </em>
          </div>
        </div>
      </section>

      <section class="role-card flow-card surface-card">
        <div class="role-panel-heading">
          <div>
            <p class="role-kicker">LOGIN FLOW</p>
            <h2>登录后的展示逻辑</h2>
          </div>
          <Route :size="18" :stroke-width="1.45" />
        </div>

        <div class="flow-steps">
          <article class="metric-plate">
            <i>1</i>
            <div>
              <strong>账号登录</strong>
              <p>后端校验 `sys_user` 状态，停用账号不能进入系统。</p>
            </div>
          </article>
          <article class="metric-plate">
            <i>2</i>
            <div>
              <strong>读取角色</strong>
              <p>通过 `sys_user_role` 关联 `sys_role`，获得 ADMIN / MAINTAINER / USER。</p>
            </div>
          </article>
          <article class="metric-plate">
            <i>3</i>
            <div>
              <strong>过滤菜单</strong>
              <p>前端根据角色显示对应 SideRail 菜单，普通用户只看到自己的工作区。</p>
            </div>
          </article>
        </div>
      </section>

      <section class="role-card boundary-card surface-card">
        <div class="role-panel-heading">
          <div>
            <p class="role-kicker">BOUNDARY</p>
            <h2>设计边界</h2>
          </div>
          <LockKeyhole :size="18" :stroke-width="1.45" />
        </div>
        <p>当前课程版本使用固定三角色，重点展示“不同账号看到不同功能”的 RBAC 思路；暂不开放删除角色或自由编辑菜单权限，避免演示数据被破坏。</p>
        <div class="boundary-tags">
          <span>固定角色</span>
          <span>用户分配</span>
          <span>菜单隔离</span>
          <span>后续可扩展</span>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import axios from 'axios'
import { BrainCircuit, Grid2X2, LockKeyhole, RefreshCw, Route, ShieldCheck, UserCog, UsersRound } from 'lucide-vue-next'
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'

const roles = ref([])
const loading = ref(false)

const roleCodes = ['ADMIN', 'MAINTAINER', 'USER']
const permissionRows = [
  { key: 'home', label: '工作台 / 首页', roles: ['ADMIN', 'MAINTAINER', 'USER'] },
  { key: 'analyze', label: '智能排障', roles: ['ADMIN', 'MAINTAINER', 'USER'] },
  { key: 'history', label: '分析历史', roles: ['ADMIN', 'MAINTAINER'] },
  { key: 'my-history', label: '我的分析历史', roles: ['USER'] },
  { key: 'knowledge', label: '故障知识库', roles: ['ADMIN', 'MAINTAINER', 'USER'] },
  { key: 'favorites', label: '我的收藏', roles: ['ADMIN', 'MAINTAINER', 'USER'] },
  { key: 'profile', label: '个人资料', roles: ['ADMIN', 'MAINTAINER', 'USER'] },
  { key: 'graph', label: '知识图谱', roles: ['ADMIN', 'MAINTAINER', 'USER'] },
  { key: 'users', label: '用户管理', roles: ['ADMIN'] },
  { key: 'roles', label: '角色管理', roles: ['ADMIN'] }
]

const roleBlueprints = [
  {
    roleCode: 'ADMIN',
    roleName: '系统管理员',
    description: '维护用户、查看全部历史、管理知识库和角色说明。',
    icon: UserCog
  },
  {
    roleCode: 'MAINTAINER',
    roleName: '知识库维护员',
    description: '维护故障案例、同步 Neo4j、查看全局分析记录。',
    icon: BrainCircuit
  },
  {
    roleCode: 'USER',
    roleName: '普通用户',
    description: '提交智能排障、查看知识图谱、浏览知识库并收藏故障案例。',
    icon: UsersRound
  }
]

const menuCount = computed(() => permissionRows.length)
const enrichedRoles = computed(() => roleBlueprints.map(blueprint => {
  const realRole = roles.value.find(role => role.roleCode === blueprint.roleCode)
  return {
    ...blueprint,
    ...realRole,
    roleName: realRole?.roleName || blueprint.roleName,
    description: realRole?.description || blueprint.description
  }
}))

async function loadRoles() {
  loading.value = true
  try {
    const response = await axios.get('/api/users/roles')
    roles.value = response.data.items || []
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '角色加载失败，请确认后端已启动')
  } finally {
    loading.value = false
  }
}

onMounted(loadRoles)
</script>

<style scoped src="./role-management-view.css"></style>
