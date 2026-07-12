import { existsSync, readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const root = fileURLToPath(new URL('../', import.meta.url))

function read(path) {
  const target = fileURLToPath(new URL(path, new URL('../', import.meta.url)))
  if (!existsSync(target)) {
    throw new Error(`Missing UI contract file: ${path}`)
  }
  return readFileSync(target, 'utf8')
}

const theme = read('src/styles/system-theme.css')
const shell = read('src/layouts/AppShell.vue')
const dashboard = read('src/views/dashboard/DashboardView.vue')
const dashboardStyles = read('src/views/dashboard/dashboard-view.css')
const analyze = read('src/views/analyze/AnalyzeView.vue')
const analyzeStyles = read('src/views/analyze/analyze-view.css')
const history = read('src/views/history/HistoryView.vue')
const historyStyles = read('src/views/history/history-view.css')
const knowledge = read('src/views/knowledge/KnowledgeBaseView.vue')
const knowledgeStyles = read('src/views/knowledge/knowledge-base-view.css')
const favorites = read('src/views/favorites/FavoriteCasesView.vue')
const favoritesStyles = read('src/views/favorites/favorite-cases-view.css')
const graph = read('src/views/graph/KnowledgeGraphView.vue')
const graphStyles = read('src/views/graph/knowledge-graph-view.css')
const users = read('src/views/users/UserManagementView.vue')
const userStyles = read('src/views/users/user-management-view.css')
const roles = read('src/views/roles/RoleManagementView.vue')
const roleStyles = read('src/views/roles/role-management-view.css')
const profile = read('src/views/profile/ProfileView.vue')
const profileStyles = read('src/views/profile/profile-view.css')
const login = read('src/LoginView.vue')
const register = read('src/RegisterView.vue')

const required = [
  [theme, '--surface-canvas'],
  [theme, '.surface-card'],
  [theme, '.inset-well'],
  [shell, 'shell-surface'],
  [dashboard, 'trendChartRef'],
  [dashboard, 'distributionChartRef'],
  [dashboard, "shallowRef(null)"],
  [dashboardStyles, '.distribution-chart {'],
  [dashboardStyles, 'min-height: 300px'],
  [analyze, 'analysis-track'],
  [analyze, 'surface-card'],
  [analyze, 'shallowRef(null)'],
  [analyze, 'analyze-graph-canvas'],
  [analyzeStyles, 'grid-template-areas:'],
  [analyzeStyles, 'min-height: 420px'],
  [analyzeStyles, '  height: 420px;'],
  [analyzeStyles, 'align-self: start'],
  [analyzeStyles, 'max-height: 560px'],
  [history, 'history-workspace'],
  [history, 'history-primary-column'],
  [history, 'history-side'],
  [history, 'history-detail-card'],
  [history, 'history-spectrum-card'],
  [history, 'surface-card'],
  [historyStyles, 'gap: 28px'],
  [historyStyles, 'max-height: 460px'],
  [historyStyles, '.history-hero {\n  display: flex;\n  gap: 32px;\n  align-items: center;\n  justify-content: space-between;\n  min-height: 204px'],
  [knowledge, 'knowledge-hero-card'],
  [knowledge, 'knowledge-detail-card'],
  [knowledge, 'knowledge-case-row'],
  [knowledge, 'surface-card'],
  [knowledge, 'inset-well'],
  [knowledge, 'shallowRef(null)'],
  [knowledgeStyles, '.knowledge-view .knowledge-card'],
  [knowledgeStyles, 'gap: 24px'],
  [knowledgeStyles, '.knowledge-view .knowledge-case-row'],
  [knowledge, 'knowledge-hero-card'],
  [knowledge, 'knowledge-stat-card'],
  [knowledge, 'knowledge-toolbar'],
  [knowledgeStyles, '.knowledge-grid > .knowledge-stat-card'],
  [favorites, 'favorites-overview'],
  [favorites, 'favorites-toolbar'],
  [favorites, 'favorites-list-card'],
  [favorites, 'favorites-detail-card'],
  [favorites, 'surface-card'],
  [favoritesStyles, '.favorites-overview__metrics'],
  [favoritesStyles, '.favorites-view .favorite-row'],
  [graph, 'kg-hero-card'],
  [graph, 'kg-stat-card'],
  [graph, 'kg-toolbar'],
  [graph, 'kg-graph-panel'],
  [graph, 'kg-side-panel'],
  [graph, 'kg-graph-canvas'],
  [graph, 'surface-card'],
  [graph, 'shallowRef(null)'],
  [graphStyles, 'gap: 24px'],
  [graphStyles, 'min-height: 204px'],
  [graphStyles, '.kg-view .kg-graph-canvas'],
  [users, 'users-hero-card'],
  [users, 'users-summary-card'],
  [users, 'users-summary-metric'],
  [users, 'surface-card'],
  [userStyles, 'gap: 24px'],
  [userStyles, 'min-height: 204px'],
  [userStyles, '.users-grid {\n  display: grid;\n  grid-template-columns: repeat(12, minmax(0, 1fr));\n  gap: 24px;\n  align-items: stretch'],
  [roles, 'roles-hero-card'],
  [roles, 'roles-summary-card'],
  [roles, 'roles-summary-metric'],
  [roles, 'surface-card'],
  [roleStyles, 'gap: 24px'],
  [roleStyles, 'min-height: 204px'],
  [roleStyles, '.role-grid {\n  display: grid;\n  grid-template-columns: repeat(12, minmax(0, 1fr));\n  gap: 24px;\n  align-items: stretch'],
  [profile, 'profile-workspace'],
  [profile, 'profile-identity-rail'],
  [profile, 'profile-editor'],
  [profile, 'profile-basic-section'],
  [profile, 'profile-security-section'],
  [profile, "defineEmits(['navigate', 'profile-updated'])"],
  [profile, 'saveProfile'],
  [profile, 'changePassword'],
  [profileStyles, 'grid-template-columns: 300px minmax(0, 1fr)'],
  [profileStyles, 'box-shadow: none'],
  [profileStyles, '.profile-section + .profile-section'],
  [login, '欢迎回来'],
  [register, '创建账户']
]

for (const [content, token] of required) {
  if (!content.includes(token)) {
    throw new Error(`Missing UI contract: ${token}`)
  }
}

console.log(`UI contract check passed for ${root}`)
