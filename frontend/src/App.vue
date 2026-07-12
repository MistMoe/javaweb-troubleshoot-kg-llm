<template>
  <el-config-provider>
    <LoginView
      v-if="!currentUser && authMode === 'login'"
      :login-form="loginForm"
      :login-loading="loginLoading"
      :login-error="loginError"
      @login="login"
      @fill-demo="fillDemo"
      @create-account="authMode = 'register'"
    />
    <RegisterView
      v-if="!currentUser && authMode === 'register'"
      @back-to-sign-in="authMode = 'login'"
    />

    <AppShell
      v-if="currentUser"
      :current-user="currentUser"
      :primary-role="primaryRole"
      :visible-menus="visibleMenus"
      :active-menu="activeMenu"
      :active-menu-label="activeMenuLabel"
      @select-menu="selectMenu"
      @open-profile="selectMenu('profile')"
      @logout="logout"
    >
      <DashboardView
        v-if="activeMenu === 'home'"
        :current-user="currentUser"
        @navigate="selectMenu"
      />
      <AnalyzeView
        v-else-if="activeMenu === 'analyze'"
        :current-user="currentUser"
      />
      <KnowledgeBaseView
        v-else-if="activeMenu === 'knowledge'"
        :current-user="currentUser"
      />
      <FavoriteCasesView
        v-else-if="activeMenu === 'favorites'"
        :current-user="currentUser"
        @navigate="selectMenu"
      />
      <ProfileView
        v-else-if="activeMenu === 'profile'"
        :current-user="currentUser"
        @navigate="selectMenu"
        @profile-updated="currentUser = $event"
      />
      <KnowledgeGraphView
        v-else-if="activeMenu === 'graph'"
      />
      <HistoryView
        v-else-if="activeMenu === 'history' || activeMenu === 'my-history'"
        :current-user="currentUser"
        :scope="activeMenu"
      />
      <UserManagementView
        v-else-if="activeMenu === 'users'"
      />
      <RoleManagementView
        v-else-if="activeMenu === 'roles'"
      />
      <section v-else class="panel">
        <h2>{{ activeMenuLabel }}</h2>
        <p>{{ activeMenuDescription }}</p>
        <el-empty description="该页面会在后续阶段迁移到新的浅蓝灰工作台布局" />
      </section>
    </AppShell>

    <main v-if="false" class="login-page">
      <section class="login-panel">
        <p class="eyebrow">JavaWeb Troubleshoot KG LLM</p>
        <h1>智能排障系统登录</h1>
        <p class="intro">选择演示账号进入系统，不同角色会看到不同菜单。</p>

        <el-form label-position="top" class="login-form" @submit.prevent="login">
          <el-form-item label="用户名">
            <el-input v-model="loginForm.username" placeholder="admin / maintainer / user01" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="loginForm.password" type="password" show-password placeholder="123456" />
          </el-form-item>
          <el-alert v-if="loginError" :title="loginError" type="error" show-icon :closable="false" />
          <el-button type="primary" size="large" :loading="loginLoading" @click="login">登录</el-button>
        </el-form>

        <div class="demo-users">
          <el-button plain @click="fillDemo('admin')">ADMIN</el-button>
          <el-button plain @click="fillDemo('maintainer')">MAINTAINER</el-button>
          <el-button plain @click="fillDemo('user01')">USER</el-button>
        </div>
      </section>
    </main>

    <main v-if="false" class="app-shell">
      <aside class="sidebar">
        <div class="brand">
          <span class="brand-mark">KG</span>
          <div>
            <strong>智能排障系统</strong>
            <small>{{ primaryRole }}</small>
          </div>
        </div>

        <nav class="menu">
          <button
            v-for="item in visibleMenus"
            :key="item.key"
            :class="{ active: activeMenu === item.key }"
            @click="selectMenu(item.key)"
          >
            {{ item.label }}
          </button>
        </nav>
      </aside>

      <section class="content">
        <header class="topbar">
          <div>
            <p class="eyebrow">当前用户</p>
            <h1>{{ currentUser.nickname }} / {{ currentUser.username }}</h1>
          </div>
          <div class="actions">
            <el-tag v-for="role in currentUser.roles" :key="role.roleCode" type="success">
              {{ role.roleCode }}
            </el-tag>
            <el-button @click="logout">退出</el-button>
          </div>
        </header>

        <section v-if="activeMenu === 'home'" class="panel">
          <h2>首页概览</h2>
          <p>当前已完成多角色基础、故障知识库、Neo4j 知识图谱和基础智能排障分析流。Phase 4 会继续接入大模型生成最终建议。</p>
          <div class="health-row">
            <el-button type="primary" :loading="healthLoading" @click="checkHealth">检查后端健康状态</el-button>
            <el-tag :type="healthTagType" size="large">{{ healthLabel }}</el-tag>
          </div>
        </section>

        <section v-else-if="activeMenu === 'analyze'" class="panel">
          <div class="section-heading">
            <div>
              <h2>智能排障</h2>
              <p>输入 Java Web 项目的报错日志，系统会先匹配 MySQL 知识库，再读取 Neo4j 图谱并生成结构化排障建议。</p>
            </div>
          </div>

          <div class="analyze-layout">
            <div class="analyze-form">
              <el-form label-position="top">
                <el-form-item label="错误日志或报错信息">
                  <el-input
                    v-model="analyzeForm.inputText"
                    type="textarea"
                    :rows="9"
                    placeholder="例如：Web server failed to start. Port 8080 already in use."
                  />
                </el-form-item>
                <el-form-item label="技术栈">
                  <el-select v-model="analyzeForm.techStack" clearable placeholder="请选择技术栈">
                    <el-option v-for="item in techStackOptions" :key="item" :label="item" :value="item" />
                  </el-select>
                </el-form-item>
                <el-button type="primary" :loading="analyzing" @click="analyzeError">开始分析</el-button>
              </el-form>
            </div>

            <div class="analyze-result">
              <el-empty v-if="!analysisResult" description="提交错误日志后会显示分析结果" />
              <template v-else>
                <div class="result-title">
                  <h3>{{ analysisResult.matched ? analysisResult.errorName : '未匹配到明确案例' }}</h3>
                  <el-tag :type="analysisResult.matched ? 'success' : 'warning'">
                    {{ analysisResult.matched ? '已命中知识库' : '需要补充信息' }}
                  </el-tag>
                </div>
                <p class="result-message">{{ analysisResult.message }}</p>
                <div v-if="analysisResult.matched" class="result-grid">
                  <div class="result-card">
                    <strong>发生环节</strong>
                    <span>{{ analysisResult.stage }}</span>
                  </div>
                  <div class="result-card">
                    <strong>技术组件</strong>
                    <span>{{ analysisResult.component }}</span>
                  </div>
                  <div class="result-card">
                    <strong>可能原因</strong>
                    <el-tag v-for="item in analysisResult.causes" :key="item" type="danger">{{ item }}</el-tag>
                  </div>
                  <div class="result-card">
                    <strong>解决方案</strong>
                    <el-tag v-for="item in analysisResult.solutions" :key="item" type="success">{{ item }}</el-tag>
                  </div>
                  <div class="result-card">
                    <strong>相关文件</strong>
                    <el-tag v-for="item in analysisResult.files" :key="item">{{ item }}</el-tag>
                  </div>
                  <div class="result-card">
                    <strong>相关命令</strong>
                    <code v-for="item in analysisResult.commands" :key="item">{{ item }}</code>
                  </div>
                </div>
                <div class="advice-box">
                  <strong>当前建议</strong>
                  <p>{{ analysisResult.aiAdvice }}</p>
                </div>
                <div v-if="analysisResult.graph?.nodes?.length" class="analyze-graph" ref="analyzeGraphChartRef"></div>
              </template>
            </div>
          </div>
        </section>

        <section v-else-if="activeMenu === 'knowledge'" class="panel">
          <div class="section-heading">
            <div>
              <h2>故障知识库</h2>
              <p>维护 MySQL 中的常见 Java Web 故障案例，后续用于生成 Neo4j 知识图谱。</p>
            </div>
            <el-button type="primary" @click="openCreateDialog">新增故障案例</el-button>
          </div>

          <div class="filters">
            <el-input v-model="filters.keyword" clearable placeholder="关键字 / 现象 / 原因 / 方案" @keyup.enter="loadErrorCases" />
            <el-select v-model="filters.component" clearable placeholder="技术组件">
              <el-option v-for="item in componentOptions" :key="item" :label="item" :value="item" />
            </el-select>
            <el-select v-model="filters.stage" clearable placeholder="发生环节">
              <el-option v-for="item in stageOptions" :key="item" :label="item" :value="item" />
            </el-select>
            <el-button :loading="errorCaseLoading" @click="loadErrorCases">查询</el-button>
          </div>

          <el-table :data="errorCases" v-loading="errorCaseLoading" border stripe>
            <el-table-column prop="errorName" label="故障名称" min-width="150" />
            <el-table-column prop="stage" label="环节" width="120" />
            <el-table-column prop="component" label="组件" width="150" />
            <el-table-column prop="keyword" label="关键字" min-width="180" show-overflow-tooltip />
            <el-table-column prop="cause" label="可能原因" min-width="220" show-overflow-tooltip />
            <el-table-column prop="solution" label="解决方案" min-width="240" show-overflow-tooltip />
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
                <el-button link type="danger" @click="deleteErrorCase(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section v-else-if="activeMenu === 'graph'" class="panel">
          <div class="section-heading">
            <div>
              <h2>知识图谱</h2>
              <p>从 MySQL 故障案例导入 Neo4j，并展示故障、环节、组件、原因、方案、文件和命令之间的关系。</p>
            </div>
            <el-button type="primary" :loading="graphImporting" @click="importGraph">从 MySQL 导入图谱</el-button>
          </div>

          <div class="filters graph-filters">
            <el-select v-model="graphErrorId" filterable placeholder="选择故障案例" @change="loadGraph">
              <el-option v-for="item in errorCases" :key="item.id" :label="item.errorName" :value="item.id" />
            </el-select>
            <el-input v-model="graphKeyword" clearable placeholder="搜索图谱节点，如 MySQL / 端口 / application.yml" @keyup.enter="searchGraph" />
            <el-button :loading="graphLoading" @click="loadGraph">查看关系图</el-button>
            <el-button @click="searchGraph">搜索节点</el-button>
          </div>

          <div class="graph-summary">
            <el-tag>节点 {{ graphData.nodes.length }}</el-tag>
            <el-tag type="success">关系 {{ graphData.links.length }}</el-tag>
          </div>

          <div ref="graphChartRef" class="graph-chart"></div>
        </section>

        <section v-else class="panel">
          <h2>{{ activeMenuLabel }}</h2>
          <p>{{ activeMenuDescription }}</p>
          <el-empty description="该模块将在后续阶段实现" />
        </section>
      </section>
    </main>

    <el-dialog v-model="errorCaseDialogVisible" :title="editingErrorCaseId ? '编辑故障案例' : '新增故障案例'" width="760px">
      <el-form label-position="top" class="case-form">
        <el-form-item label="故障名称"><el-input v-model="caseForm.errorName" /></el-form-item>
        <el-form-item label="发生环节"><el-input v-model="caseForm.stage" /></el-form-item>
        <el-form-item label="技术组件"><el-input v-model="caseForm.component" /></el-form-item>
        <el-form-item label="匹配关键字"><el-input v-model="caseForm.keyword" placeholder="多个关键字可用逗号分隔" /></el-form-item>
        <el-form-item label="报错现象"><el-input v-model="caseForm.symptom" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="可能原因"><el-input v-model="caseForm.cause" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="解决方案"><el-input v-model="caseForm.solution" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="相关文件"><el-input v-model="caseForm.relatedFile" /></el-form-item>
        <el-form-item label="相关命令"><el-input v-model="caseForm.relatedCommand" /></el-form-item>
        <el-form-item label="风险提示"><el-input v-model="caseForm.riskTip" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="errorCaseDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingErrorCase" @click="saveErrorCase">保存</el-button>
      </template>
    </el-dialog>
  </el-config-provider>
</template>

<script setup>
import axios from 'axios'
import * as echarts from 'echarts'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import AppShell from './layouts/AppShell.vue'
import LoginView from './LoginView.vue'
import RegisterView from './RegisterView.vue'
import AnalyzeView from './views/analyze/AnalyzeView.vue'
import DashboardView from './views/dashboard/DashboardView.vue'
import FavoriteCasesView from './views/favorites/FavoriteCasesView.vue'
import KnowledgeGraphView from './views/graph/KnowledgeGraphView.vue'
import HistoryView from './views/history/HistoryView.vue'
import KnowledgeBaseView from './views/knowledge/KnowledgeBaseView.vue'
import ProfileView from './views/profile/ProfileView.vue'
import RoleManagementView from './views/roles/RoleManagementView.vue'
import UserManagementView from './views/users/UserManagementView.vue'

const loginLoading = ref(false)
const loginError = ref('')
const authMode = ref('login')
const currentUser = ref(null)
const token = ref(localStorage.getItem('demo_token') || '')
const activeMenu = ref('home')
const healthLoading = ref(false)
const health = ref(null)
const healthError = ref('')
const errorCases = ref([])
const errorCaseLoading = ref(false)
const errorCaseDialogVisible = ref(false)
const savingErrorCase = ref(false)
const editingErrorCaseId = ref(null)
const graphChartRef = ref(null)
const graphChart = ref(null)
const graphLoading = ref(false)
const graphImporting = ref(false)
const graphErrorId = ref(null)
const graphKeyword = ref('')
const graphData = ref({ nodes: [], links: [] })
const analyzeGraphChartRef = ref(null)
const analyzeGraphChart = ref(null)
const analyzing = ref(false)
const analysisResult = ref(null)

const loginForm = reactive({ username: 'admin', password: '123456' })
const filters = reactive({ keyword: '', component: '', stage: '' })
const caseForm = reactive(emptyCaseForm())
const analyzeForm = reactive({
  inputText: 'Web server failed to start. Port 8080 already in use.',
  techStack: 'Spring Boot'
})

const techStackOptions = ['Spring Boot', 'Maven', 'MySQL', 'Vue', 'Axios', 'Neo4j', 'LLM API']
const menus = [
  { key: 'home', label: '首页', roles: ['ADMIN', 'MAINTAINER', 'USER'], description: '系统概览和后端健康检查。' },
  { key: 'analyze', label: '智能排障', roles: ['ADMIN', 'MAINTAINER', 'USER'], description: '提交报错文本并生成排障建议。' },
  { key: 'my-history', label: '我的分析历史', roles: ['USER'], description: '普通用户查看自己的排障历史。' },
  { key: 'history', label: '分析历史', roles: ['ADMIN', 'MAINTAINER'], description: '管理员或维护员查看分析记录。' },
  { key: 'knowledge', label: '故障知识库', roles: ['ADMIN', 'MAINTAINER', 'USER'], description: '浏览、收藏或维护 MySQL 中的故障案例。' },
  { key: 'favorites', label: '我的收藏', roles: ['ADMIN', 'MAINTAINER', 'USER'], description: '查看当前账号收藏的故障案例。' },
  { key: 'profile', label: '个人资料', roles: ['ADMIN', 'MAINTAINER', 'USER'], description: '查看当前登录账号与个人排障统计。', hidden: true },
  { key: 'graph', label: '知识图谱', roles: ['ADMIN', 'MAINTAINER', 'USER'], description: '查看 Neo4j 故障关系图。' },
  { key: 'users', label: '用户管理', roles: ['ADMIN'], description: '管理员维护系统用户。' },
  { key: 'roles', label: '角色管理', roles: ['ADMIN'], description: '管理员查看角色配置。' }
]

const roleCodes = computed(() => currentUser.value?.roles?.map(role => role.roleCode) || [])
const primaryRole = computed(() => roleCodes.value[0] || 'UNKNOWN')
const visibleMenus = computed(() => menus.filter(item => !item.hidden && item.roles.some(role => roleCodes.value.includes(role))))
const activeMenuLabel = computed(() => menus.find(item => item.key === activeMenu.value)?.label || '模块')
const activeMenuDescription = computed(() => menus.find(item => item.key === activeMenu.value)?.description || '')
const componentOptions = computed(() => [...new Set(errorCases.value.map(item => item.component).filter(Boolean))])
const stageOptions = computed(() => [...new Set(errorCases.value.map(item => item.stage).filter(Boolean))])

const healthLabel = computed(() => {
  if (healthLoading.value) return '检查中'
  if (health.value?.status === 'UP') return '后端在线'
  if (healthError.value) return '后端未连接'
  return '等待检查'
})
const healthTagType = computed(() => {
  if (health.value?.status === 'UP') return 'success'
  if (healthError.value) return 'danger'
  return 'info'
})

function emptyCaseForm() {
  return {
    errorName: '',
    stage: '',
    component: '',
    keyword: '',
    symptom: '',
    cause: '',
    solution: '',
    relatedFile: '',
    relatedCommand: '',
    riskTip: '',
    userId: 1
  }
}

function resetCaseForm(values = emptyCaseForm()) {
  Object.assign(caseForm, values)
  caseForm.userId = currentUser.value?.id || 1
}

function fillDemo(username) {
  loginForm.username = username
  loginForm.password = '123456'
}

async function selectMenu(key) {
  activeMenu.value = key
  if (key === 'knowledge' || key === 'graph') {
    await loadErrorCases()
  }
  if (key === 'graph') {
    if (!graphErrorId.value && errorCases.value.length > 0) graphErrorId.value = errorCases.value[0].id
    await nextTick()
    if (graphErrorId.value) await loadGraph()
  }
}

async function login() {
  loginLoading.value = true
  loginError.value = ''
  try {
    const response = await axios.post('/api/auth/login', loginForm)
    token.value = response.data.token
    currentUser.value = response.data.user
    localStorage.setItem('demo_token', token.value)
    activeMenu.value = 'home'
    await checkHealth()
  } catch (error) {
    currentUser.value = null
    token.value = ''
    localStorage.removeItem('demo_token')
    loginError.value = error.response?.data?.message || '登录失败，请检查账号密码'
  } finally {
    loginLoading.value = false
  }
}

async function loadProfile() {
  if (!token.value) return
  try {
    const response = await axios.get('/api/auth/profile', { headers: { Authorization: `Bearer ${token.value}` } })
    currentUser.value = response.data
    await checkHealth()
  } catch {
    logout()
  }
}

function logout() {
  currentUser.value = null
  token.value = ''
  localStorage.removeItem('demo_token')
}

async function checkHealth() {
  healthLoading.value = true
  healthError.value = ''
  try {
    const response = await axios.get('/api/health')
    health.value = response.data
  } catch (error) {
    health.value = null
    healthError.value = error instanceof Error ? error.message : 'Health check failed'
  } finally {
    healthLoading.value = false
  }
}

async function loadErrorCases() {
  errorCaseLoading.value = true
  try {
    const response = await axios.get('/api/error/list', { params: filters })
    errorCases.value = response.data.items || []
  } catch {
    ElMessage.error('故障知识库加载失败，请确认后端已启动')
  } finally {
    errorCaseLoading.value = false
  }
}

function openCreateDialog() {
  editingErrorCaseId.value = null
  resetCaseForm()
  errorCaseDialogVisible.value = true
}

function openEditDialog(row) {
  editingErrorCaseId.value = row.id
  resetCaseForm({ ...row, userId: currentUser.value?.id || 1 })
  errorCaseDialogVisible.value = true
}

async function saveErrorCase() {
  savingErrorCase.value = true
  try {
    if (editingErrorCaseId.value) {
      await axios.put(`/api/error/${editingErrorCaseId.value}`, caseForm)
      ElMessage.success('故障案例已更新')
    } else {
      await axios.post('/api/error/create', caseForm)
      ElMessage.success('故障案例已新增')
    }
    errorCaseDialogVisible.value = false
    await loadErrorCases()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '保存失败')
  } finally {
    savingErrorCase.value = false
  }
}

async function deleteErrorCase(row) {
  try {
    await ElMessageBox.confirm(`确认删除“${row.errorName}”？`, '删除确认', { type: 'warning' })
    await axios.delete(`/api/error/${row.id}`)
    ElMessage.success('故障案例已删除')
    await loadErrorCases()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

async function analyzeError() {
  if (!analyzeForm.inputText.trim()) {
    ElMessage.warning('请先输入错误日志或报错信息')
    return
  }
  analyzing.value = true
  try {
    const response = await axios.post('/api/error/analyze', {
      ...analyzeForm,
      userId: currentUser.value?.id || 1
    })
    analysisResult.value = response.data
    await renderAnalyzeGraph()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '分析失败，请确认后端和 Neo4j 已启动')
  } finally {
    analyzing.value = false
  }
}

async function importGraph() {
  graphImporting.value = true
  try {
    const response = await axios.post('/api/kg/import')
    ElMessage.success(`已导入 ${response.data.importedCases} 条故障案例到 Neo4j`)
    if (graphErrorId.value) await loadGraph()
  } catch {
    ElMessage.error('导入失败，请确认 Neo4j 已启动且账号密码正确')
  } finally {
    graphImporting.value = false
  }
}

async function loadGraph() {
  if (!graphErrorId.value) return
  graphLoading.value = true
  try {
    const response = await axios.get(`/api/kg/graph/${graphErrorId.value}`)
    graphData.value = response.data
    await renderGraph()
  } catch {
    ElMessage.error('图谱加载失败，请先导入 Neo4j 数据')
  } finally {
    graphLoading.value = false
  }
}

async function searchGraph() {
  if (!graphKeyword.value.trim()) return
  graphLoading.value = true
  try {
    const response = await axios.get('/api/kg/search', { params: { keyword: graphKeyword.value } })
    graphData.value = response.data
    await renderGraph()
  } catch {
    ElMessage.error('图谱搜索失败')
  } finally {
    graphLoading.value = false
  }
}

async function renderGraph() {
  await nextTick()
  if (!graphChartRef.value) return
  if (!graphChart.value) graphChart.value = echarts.init(graphChartRef.value)
  renderChart(graphChart.value, graphData.value)
}

async function renderAnalyzeGraph() {
  await nextTick()
  if (!analyzeGraphChartRef.value || !analysisResult.value?.graph?.nodes?.length) return
  if (!analyzeGraphChart.value) analyzeGraphChart.value = echarts.init(analyzeGraphChartRef.value)
  renderChart(analyzeGraphChart.value, analysisResult.value.graph)
}

function renderChart(chart, data) {
  const categories = [...new Set(data.nodes.map(node => node.category))]
  chart.setOption({
    tooltip: {},
    legend: [{ data: categories, top: 0 }],
    series: [{
      type: 'graph',
      layout: 'force',
      roam: true,
      draggable: true,
      categories: categories.map(name => ({ name })),
      data: data.nodes.map(node => ({
        id: node.id,
        name: node.name,
        category: node.category,
        symbolSize: node.category === 'Error' ? 58 : 42
      })),
      links: data.links.map(link => ({
        source: link.source,
        target: link.target,
        label: { show: true, formatter: link.label }
      })),
      force: { repulsion: 220, edgeLength: 120 },
      label: { show: true, overflow: 'break', width: 120 },
      edgeSymbol: ['none', 'arrow'],
      edgeLabel: { show: true, fontSize: 11 }
    }]
  })
}

onMounted(loadProfile)
</script>
