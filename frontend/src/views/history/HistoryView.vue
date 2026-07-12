<template>
  <section class="history-view">
    <div class="history-grid">
      <section class="history-card history-hero surface-card surface-card--strong">
        <div>
          <p class="section-kicker">{{ isPersonal ? 'MY REPORTS' : 'ANALYSIS HISTORY' }}</p>
          <h1>{{ isPersonal ? '我的分析历史' : '分析历史' }}</h1>
          <p>
            复盘智能排障输入、命中案例、知识图谱证据和 AI 建议，
            让每一次报错都沉淀成可追踪的排障记录。
          </p>
        </div>
        <div class="hero-metrics">
          <span class="history-metric metric-plate blue">
            <strong>{{ histories.length }}</strong>
            <small>记录</small>
          </span>
          <span class="history-metric metric-plate violet">
            <strong>{{ solvedRate }}%</strong>
            <small>命中率</small>
          </span>
          <span class="history-metric metric-plate cyan">
            <strong>{{ totalGraphNodes }}</strong>
            <small>图谱节点</small>
          </span>
        </div>
      </section>

      <section class="history-card history-filter-card surface-card">
        <label>
          <span>关键词</span>
          <input v-model="filters.keyword" placeholder="搜索报错、技术栈、命中案例或 AI 建议" @keyup.enter="loadHistories" />
        </label>
        <label>
          <span>命中状态</span>
          <select v-model="filters.matched" @change="loadHistories">
            <option value="">全部记录</option>
            <option value="true">已命中知识库</option>
            <option value="false">未命中</option>
          </select>
        </label>
        <button class="soft-button action-button" type="button" :disabled="loading" @click="loadHistories">
          <RefreshCw :size="16" :stroke-width="1.6" />
          刷新
        </button>
      </section>

      <div class="history-workspace">
      <div class="history-primary-column">
      <section class="history-card history-timeline-card surface-card">
        <div class="panel-heading">
          <div>
            <p class="section-kicker">TIMELINE</p>
            <h2>历史记录</h2>
          </div>
          <span>{{ histories.length }} 条</span>
        </div>

        <div v-if="loading" class="loading-state inset-well">
          <span></span>
          <p>正在加载分析历史...</p>
        </div>
        <div v-else-if="!histories.length" class="empty-state inset-well">
          <History :size="42" :stroke-width="1.35" />
          <p>暂时还没有分析历史，先去智能排障跑一次就会出现在这里。</p>
        </div>
        <div v-else class="history-list">
          <button
            v-for="item in histories"
            :key="item.id"
            class="history-item"
            :class="{ active: selectedId === item.id }"
            type="button"
            @click="selectHistory(item)"
          >
            <i :class="toneClass(item.techStack)" aria-hidden="true"></i>
            <div>
              <strong>{{ item.matchedErrorName || '未命中知识库案例' }}</strong>
              <p>{{ compactText(item.inputText, 96) }}</p>
              <span>
                <small>{{ item.techStack || 'Unknown Stack' }}</small>
                <small>{{ formatTime(item.createTime) }}</small>
              </span>
            </div>
            <em :class="item.matched ? 'matched' : 'unmatched'">
              {{ item.matched ? '已命中' : '待补充' }}
            </em>
          </button>
        </div>
      </section>

      <section class="history-card history-spectrum-card surface-card">
        <div class="panel-heading compact">
          <div>
            <p class="section-kicker">STACK SPECTRUM</p>
            <h2>技术栈分布</h2>
          </div>
          <BarChart3 :size="18" :stroke-width="1.45" />
        </div>
        <div class="spectrum-list inset-well">
          <div v-for="item in stackStats" :key="item.label" class="spectrum-row">
            <span>{{ item.label }}</span>
            <i :style="{ '--value': `${item.percent}%`, '--tone': item.color }"></i>
            <small>{{ item.count }} 次</small>
          </div>
        </div>
      </section>
      </div>

      <aside class="history-side">
      <section class="history-card history-detail-card surface-card">
        <div class="panel-heading">
          <div>
            <p class="section-kicker">REPORT DETAIL</p>
            <h2>复盘详情</h2>
          </div>
          <div v-if="selectedDetail" class="detail-actions">
            <button class="export-action action-button" type="button" @click="exportHistoryWord">
              <FileDown :size="15" :stroke-width="1.55" />
              Word
            </button>
            <button class="export-action action-button" type="button" @click="exportHistoryPdf">
              <Printer :size="15" :stroke-width="1.55" />
              PDF
            </button>
            <button class="delete-button action-button action-button--danger" type="button" @click="deleteHistory">
              <Trash2 :size="15" :stroke-width="1.55" />
              删除
            </button>
          </div>
        </div>

        <div v-if="detailLoading" class="loading-state detail-loading inset-well">
          <span></span>
          <p>正在打开历史详情...</p>
        </div>
        <div v-else-if="!selectedDetail" class="empty-state detail-empty inset-well">
          <FileText :size="42" :stroke-width="1.35" />
          <p>选择左侧记录，查看完整输入、AI 建议和图谱快照。</p>
        </div>
        <template v-else>
          <div class="detail-title">
            <small>#{{ selectedDetail.id }} · {{ selectedDetail.techStack || 'Unknown Stack' }}</small>
            <strong>{{ selectedDetail.matchedErrorName || '未命中知识库案例' }}</strong>
            <span :class="selectedDetail.matched ? 'status-chip matched' : 'status-chip unmatched'">
              {{ selectedDetail.matched ? '已命中知识库' : '未命中，建议补充案例' }}
            </span>
          </div>

          <div class="detail-metrics">
            <div class="metric-plate">
              <strong>{{ selectedDetail.graph?.nodes?.length || 0 }}</strong>
              <span>图谱节点</span>
            </div>
            <div class="metric-plate">
              <strong>{{ selectedDetail.graph?.links?.length || 0 }}</strong>
              <span>关系连线</span>
            </div>
            <div class="metric-plate">
              <strong>{{ selectedDetail.nickname || selectedDetail.username || '系统用户' }}</strong>
              <span>提交用户</span>
            </div>
          </div>

          <article class="text-panel input-panel inset-well">
            <h3>原始报错输入</h3>
            <p>{{ selectedDetail.inputText }}</p>
          </article>

          <article class="text-panel advice-panel inset-well">
            <h3>AI 建议</h3>
            <p>{{ selectedDetail.aiAdvice || '暂无 AI 建议。' }}</p>
          </article>

          <article class="graph-snapshot inset-well">
            <div>
              <h3>图谱快照</h3>
              <span>{{ selectedDetail.graph?.nodes?.length || 0 }} nodes / {{ selectedDetail.graph?.links?.length || 0 }} links</span>
            </div>
            <div class="node-cloud">
              <em
                v-for="node in visibleGraphNodes"
                :key="node.id"
                :class="node.category"
              >
                {{ node.name }}
              </em>
              <small v-if="remainingNodeCount > 0">+{{ remainingNodeCount }}</small>
            </div>
          </article>
        </template>
      </section>
      </aside>
      </div>
    </div>
  </section>
</template>

<script setup>
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { BarChart3, FileDown, FileText, History, Printer, RefreshCw, Trash2 } from 'lucide-vue-next'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { buildHistoryReport, downloadWordReport, printReportAsPdf } from '../../utils/reportExport'

const props = defineProps({
  currentUser: {
    type: Object,
    default: () => ({})
  },
  scope: {
    type: String,
    default: 'history'
  }
})

const histories = ref([])
const selectedId = ref(null)
const selectedDetail = ref(null)
const loading = ref(false)
const detailLoading = ref(false)
const filters = reactive({
  keyword: '',
  matched: ''
})

const isPersonal = computed(() => props.scope === 'my-history')
const totalGraphNodes = computed(() => histories.value.reduce((sum, item) => sum + (item.graphNodeCount || 0), 0))
const solvedRate = computed(() => {
  if (!histories.value.length) return 0
  const matched = histories.value.filter(item => item.matched).length
  return Math.round((matched / histories.value.length) * 100)
})
const stackStats = computed(() => {
  const counts = new Map()
  histories.value.forEach(item => {
    const key = item.techStack || 'Unknown'
    counts.set(key, (counts.get(key) || 0) + 1)
  })
  const max = Math.max(...counts.values(), 1)
  return [...counts.entries()].slice(0, 5).map(([label, count]) => ({
    label,
    count,
    percent: Math.max(14, Math.round((count / max) * 100)),
    color: toneColor(label)
  }))
})
const visibleGraphNodes = computed(() => selectedDetail.value?.graph?.nodes?.slice(0, 10) || [])
const remainingNodeCount = computed(() => Math.max((selectedDetail.value?.graph?.nodes?.length || 0) - visibleGraphNodes.value.length, 0))

watch(() => props.scope, () => {
  selectedId.value = null
  selectedDetail.value = null
  loadHistories()
})

async function loadHistories() {
  loading.value = true
  try {
    const params = {}
    if (filters.keyword.trim()) params.keyword = filters.keyword.trim()
    if (filters.matched !== '') params.matched = filters.matched
    if (isPersonal.value && props.currentUser?.id) params.userId = props.currentUser.id

    const response = await axios.get('/api/analysis-history/list', { params })
    histories.value = response.data.items || []
    if (histories.value.length) {
      const next = histories.value.find(item => item.id === selectedId.value) || histories.value[0]
      await selectHistory(next)
    } else {
      selectedId.value = null
      selectedDetail.value = null
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '分析历史加载失败，请确认后端已启动')
  } finally {
    loading.value = false
  }
}

async function selectHistory(item) {
  selectedId.value = item.id
  detailLoading.value = true
  try {
    const response = await axios.get(`/api/analysis-history/${item.id}`)
    selectedDetail.value = response.data
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '历史详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

async function deleteHistory() {
  if (!selectedDetail.value) return
  try {
    await ElMessageBox.confirm('确认删除这条分析历史吗？', '删除确认', { type: 'warning' })
    await axios.delete(`/api/analysis-history/${selectedDetail.value.id}`)
    ElMessage.success('分析历史已删除')
    await loadHistories()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }
}

function exportHistoryWord() {
  if (!selectedDetail.value) return
  const html = buildHistoryReport(selectedDetail.value)
  downloadWordReport(html, `历史排障报告-${selectedDetail.value.id || 'history'}`)
  ElMessage.success('Word 报告已生成')
}

function exportHistoryPdf() {
  if (!selectedDetail.value) return
  try {
    const html = buildHistoryReport(selectedDetail.value)
    printReportAsPdf(html)
  } catch (error) {
    ElMessage.error(error.message || 'PDF 打印窗口打开失败')
  }
}

function compactText(value, maxLength) {
  if (!value) return '暂无输入内容'
  return value.length > maxLength ? `${value.slice(0, maxLength)}...` : value
}

function formatTime(value) {
  if (!value) return '--'
  return String(value).replace('T', ' ').slice(0, 16)
}

function toneClass(value) {
  const normalized = String(value || '').toLowerCase()
  if (normalized.includes('spring')) return 'spring'
  if (normalized.includes('mysql')) return 'mysql'
  if (normalized.includes('neo4j')) return 'neo4j'
  if (normalized.includes('maven')) return 'maven'
  if (normalized.includes('vue')) return 'vue'
  if (normalized.includes('llm')) return 'llm'
  return 'default'
}

function toneColor(value) {
  const colors = {
    spring: '#4f86e8',
    mysql: '#62b9ca',
    neo4j: '#9689e6',
    maven: '#6f79d8',
    vue: '#68b99a',
    llm: '#dc7378',
    default: '#9db0c5'
  }
  return colors[toneClass(value)] || colors.default
}

onMounted(loadHistories)
</script>

<style scoped src="./history-view.css"></style>
