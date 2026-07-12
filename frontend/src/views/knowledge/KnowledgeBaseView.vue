<template>
  <section class="knowledge-view">
    <div class="knowledge-grid">
      <section class="knowledge-card knowledge-hero-card surface-card surface-card--strong">
        <div class="knowledge-hero-card__copy">
          <p class="section-kicker">ERROR KNOWLEDGE BASE</p>
          <h1>故障知识库</h1>
          <p>
            维护 JavaWeb 常见故障案例，作为智能排障匹配和 Neo4j 知识图谱导入的数据源。
          </p>
        </div>
      </section>

      <section class="knowledge-card knowledge-stat-card surface-card is-blue">
        <span>案例总数</span>
        <strong>{{ cases.length }}</strong>
        <small>当前知识库记录</small>
      </section>
      <section class="knowledge-card knowledge-stat-card surface-card is-violet">
        <span>技术组件</span>
        <strong>{{ componentCount }}</strong>
        <small>覆盖组件类型</small>
      </section>
      <section class="knowledge-card knowledge-stat-card surface-card is-cyan">
        <span>发生环节</span>
        <strong>{{ stageCount }}</strong>
        <small>启动 / 连接 / 调用</small>
      </section>

      <section class="knowledge-card knowledge-toolbar surface-card">
        <div class="knowledge-toolbar__context">
          <span>KNOWLEDGE OPERATIONS</span>
          <small>投稿、维护与图谱同步</small>
        </div>
        <div class="knowledge-actions">
          <button class="action-button" type="button" @click="openSubmissionDialog">
            <UploadCloud :size="16" :stroke-width="1.6" />
            投稿经验
          </button>
          <button v-if="canReviewSubmissions" class="action-button review-entry" type="button" @click="toggleReviewPanel">
            <Inbox :size="16" :stroke-width="1.6" />
            待审核 {{ pendingCount }}
          </button>
          <button class="action-button knowledge-refresh-action" type="button" :disabled="loading" title="刷新案例" aria-label="刷新案例" @click="loadCases">
            <RefreshCw :size="16" :stroke-width="1.6" />
          </button>
          <button v-if="canMaintainKnowledge" class="action-button" type="button" :disabled="importing" @click="importToGraph">
            <Network :size="16" :stroke-width="1.6" />
            {{ importing ? '同步中...' : '同步到 Neo4j' }}
          </button>
          <button v-if="canMaintainKnowledge" class="action-button action-button--primary" type="button" @click="openCreateDialog">
            <Plus :size="17" :stroke-width="1.7" />
            新增案例
          </button>
        </div>
      </section>

      <section class="knowledge-card knowledge-filter-card surface-card">
        <div class="filter-fields">
          <label>
            <span>关键词</span>
            <input v-model="filters.keyword" placeholder="搜索故障名、关键词、原因、方案" @keyup.enter="loadCases" />
          </label>
          <label>
            <span>技术组件</span>
            <select v-model="filters.component">
              <option value="">全部组件</option>
              <option v-for="item in componentOptions" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
          <label>
            <span>发生环节</span>
            <select v-model="filters.stage">
              <option value="">全部环节</option>
              <option v-for="item in stageOptions" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
          <button class="action-button filter-button" type="button" :disabled="loading" @click="loadCases">查询</button>
        </div>
      </section>

      <section v-if="canReviewSubmissions && reviewPanelVisible" class="knowledge-card knowledge-review-card surface-card">
        <div class="card-heading">
          <div>
            <p class="section-kicker">SUBMISSION REVIEW</p>
            <h2>经验投稿审核</h2>
          </div>
          <select v-model="submissionFilter" class="review-filter" @change="loadSubmissions">
            <option value="PENDING">待审核</option>
            <option value="APPROVED">已通过</option>
            <option value="REJECTED">已驳回</option>
            <option value="">全部投稿</option>
          </select>
        </div>

        <div class="review-layout">
          <div class="submission-list inset-well">
            <button
              v-for="item in submissions"
              :key="item.id"
              type="button"
              class="submission-row"
              :class="{ active: selectedSubmission?.id === item.id }"
              @click="selectSubmission(item)"
            >
              <span>
                <strong>{{ item.draftErrorName }}</strong>
                <small>{{ item.submitNickname || item.submitUsername || '未知用户' }} · {{ item.techStack || '未填写技术栈' }}</small>
              </span>
              <em :class="item.status.toLowerCase()">{{ statusText(item.status) }}</em>
            </button>
            <el-empty v-if="!submissionLoading && !submissions.length" description="暂无投稿记录" />
          </div>

          <div class="submission-detail inset-well">
            <el-empty v-if="!selectedSubmission" description="选择左侧投稿查看详情" />
            <template v-else>
              <div class="review-source">
                <h3>原始经验</h3>
                <p>{{ selectedSubmission.rawLog }}</p>
                <small>解决过程：{{ selectedSubmission.userSolution }}</small>
              </div>

              <div class="draft-editor compact">
                <label>
                  <span>故障名称</span>
                  <input v-model="reviewDraft.errorName" />
                </label>
                <label>
                  <span>关键词</span>
                  <input v-model="reviewDraft.keyword" />
                </label>
                <label>
                  <span>报错现象</span>
                  <textarea v-model="reviewDraft.symptom" rows="2"></textarea>
                </label>
                <label>
                  <span>可能原因</span>
                  <textarea v-model="reviewDraft.cause" rows="2"></textarea>
                </label>
                <label>
                  <span>解决方案</span>
                  <textarea v-model="reviewDraft.solution" rows="2"></textarea>
                </label>
                <label>
                  <span>相关文件</span>
                  <input v-model="reviewDraft.relatedFile" />
                </label>
                <label>
                  <span>相关命令</span>
                  <input v-model="reviewDraft.relatedCommand" />
                </label>
                <label>
                  <span>风险提示</span>
                  <textarea v-model="reviewDraft.riskTip" rows="2"></textarea>
                </label>
              </div>

              <div class="review-actions" v-if="selectedSubmission.status === 'PENDING'">
                <button class="dialog-secondary" type="button" :disabled="rejecting" @click="rejectSubmission">
                  驳回
                </button>
                <button class="dialog-primary" type="button" :disabled="approving" @click="approveSubmission">
                  {{ approving ? '入库中...' : '通过入库' }}
                </button>
              </div>
              <p v-else class="review-note">
                审核结果：{{ statusText(selectedSubmission.status) }} · {{ selectedSubmission.reviewComment || '无备注' }}
              </p>
            </template>
          </div>
        </div>
      </section>

      <section class="knowledge-card knowledge-list-card surface-card">
        <div class="card-heading">
          <div>
            <p class="section-kicker">CASE LIBRARY</p>
            <h2>故障案例</h2>
          </div>
          <span>{{ filteredHint }}</span>
        </div>

        <div v-if="loading" class="loading-block">
          <span></span>
          <p>正在加载知识库...</p>
        </div>
        <el-empty v-else-if="!cases.length" description="暂无故障案例" />

        <div v-else class="case-list">
          <button
            v-for="item in cases"
            :key="item.id"
            type="button"
            class="knowledge-case-row"
            :class="{ active: selectedCase?.id === item.id }"
            @click="selectedCase = item"
          >
            <span class="case-main">
              <strong>{{ item.errorName }}</strong>
              <small>{{ item.stage || '未填写环节' }} · {{ item.component || '未填写组件' }}</small>
            </span>
            <span class="case-keyword">{{ item.keyword || '无关键词' }}</span>
            <span class="case-actions">
              <i class="favorite" :class="{ active: isFavorite(item.id) }" @click.stop="toggleFavorite(item)">
                <Heart :size="14" :fill="isFavorite(item.id) ? 'currentColor' : 'none'" :stroke-width="1.8" />
                {{ isFavorite(item.id) ? '已收藏' : '收藏' }}
              </i>
              <template v-if="canMaintainKnowledge">
                <i @click.stop="openEditDialog(item)">编辑</i>
                <i class="danger" @click.stop="deleteCase(item)">删除</i>
              </template>
            </span>
          </button>
        </div>
      </section>

      <section class="knowledge-card knowledge-detail-card surface-card">
        <div class="card-heading">
          <div>
            <p class="section-kicker">CASE DETAIL</p>
            <h2>案例详情</h2>
          </div>
          <BookOpen :size="18" :stroke-width="1.45" />
        </div>

        <div class="knowledge-inline-chart-card inset-well">
          <div class="inline-chart-title">
            <span>组件分布走势</span>
            <TrendingUp :size="16" :stroke-width="1.45" />
          </div>
          <div ref="componentChartRef" class="line-chart" role="img" aria-label="组件分布折线图"></div>
          <div class="chart-labels">
            <span v-for="item in chartData" :key="item.label">
              {{ shortLabel(item.label) }}
              <small>{{ item.count }} 条</small>
            </span>
          </div>
        </div>

        <el-empty v-if="!selectedCase" description="选择一个故障案例查看详情" />
        <div v-else class="knowledge-detail-content">
          <div class="detail-title">
            <small>#{{ selectedCase.id }}</small>
            <strong>{{ selectedCase.errorName }}</strong>
          </div>

          <div class="tag-line">
            <em>{{ selectedCase.stage || '未填写环节' }}</em>
            <em>{{ selectedCase.component || '未填写组件' }}</em>
          </div>
          <button class="detail-favorite" type="button" @click="toggleFavorite(selectedCase)">
            <Heart :size="15" :fill="isFavorite(selectedCase.id) ? 'currentColor' : 'none'" :stroke-width="1.8" />
            {{ isFavorite(selectedCase.id) ? '已加入收藏' : '收藏此案例' }}
          </button>

          <article class="inset-well">
            <h3>报错现象</h3>
            <p>{{ selectedCase.symptom || '未填写' }}</p>
          </article>
          <article class="inset-well">
            <h3>可能原因</h3>
            <p>{{ selectedCase.cause || '未填写' }}</p>
          </article>
          <article class="inset-well">
            <h3>解决方案</h3>
            <p>{{ selectedCase.solution || '未填写' }}</p>
          </article>
          <article class="inset-well">
            <h3>文件与命令</h3>
            <code v-if="selectedCase.relatedFile">{{ selectedCase.relatedFile }}</code>
            <code v-if="selectedCase.relatedCommand">{{ selectedCase.relatedCommand }}</code>
            <p v-if="!selectedCase.relatedFile && !selectedCase.relatedCommand">未填写</p>
          </article>
        </div>
      </section>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑故障案例' : '新增故障案例'" width="760px" class="knowledge-dialog">
      <el-form class="case-form" label-position="top">
        <el-form-item label="故障名称">
          <el-input v-model="caseForm.errorName" placeholder="例如：端口被占用" />
        </el-form-item>
        <el-form-item label="发生环节">
          <el-input v-model="caseForm.stage" placeholder="例如：后端启动" />
        </el-form-item>
        <el-form-item label="技术组件">
          <el-input v-model="caseForm.component" placeholder="例如：Spring Boot" />
        </el-form-item>
        <el-form-item label="匹配关键词">
          <el-input v-model="caseForm.keyword" placeholder="多个关键词用逗号分隔" />
        </el-form-item>
        <el-form-item label="报错现象">
          <el-input v-model="caseForm.symptom" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="可能原因">
          <el-input v-model="caseForm.cause" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="解决方案">
          <el-input v-model="caseForm.solution" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="相关文件">
          <el-input v-model="caseForm.relatedFile" placeholder="例如：application.yml" />
        </el-form-item>
        <el-form-item label="相关命令">
          <el-input v-model="caseForm.relatedCommand" placeholder="例如：netstat -ano | findstr 8080" />
        </el-form-item>
        <el-form-item label="风险提示">
          <el-input v-model="caseForm.riskTip" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <button class="dialog-secondary" type="button" @click="dialogVisible = false">取消</button>
        <button class="dialog-primary" type="button" :disabled="saving" @click="saveCase">
          {{ saving ? '保存中...' : '保存' }}
        </button>
      </template>
    </el-dialog>

    <el-dialog v-model="submissionDialogVisible" title="投稿排障经验" width="1080px" class="knowledge-dialog submission-dialog">
      <div class="submission-dialog-grid">
        <section class="submission-source-form">
          <p class="section-kicker">RAW EXPERIENCE</p>
          <h3>原始经验</h3>
          <label>
            <span>报错日志 / 异常堆栈</span>
            <textarea v-model="submissionForm.rawLog" rows="8" placeholder="粘贴完整报错日志、异常类型或关键堆栈"></textarea>
          </label>
          <div class="two-fields">
            <label>
              <span>技术栈</span>
              <select v-model="submissionForm.techStack">
                <option v-for="item in submissionTechStacks" :key="item" :value="item">{{ item }}</option>
              </select>
            </label>
            <label>
              <span>发生环节</span>
              <input v-model="submissionForm.stage" placeholder="例如：后端启动 / 数据库连接" />
            </label>
          </div>
          <label>
            <span>我的解决过程</span>
            <textarea v-model="submissionForm.userSolution" rows="5" placeholder="写下你最后怎么修好的，越具体越好"></textarea>
          </label>
          <div class="two-fields">
            <label>
              <span>相关文件</span>
              <input v-model="submissionForm.relatedFile" placeholder="application.yml / pom.xml" />
            </label>
            <label>
              <span>相关命令</span>
              <input v-model="submissionForm.relatedCommand" placeholder="netstat -ano | findstr 8080" />
            </label>
          </div>
          <button class="action-button" type="button" :disabled="drafting" @click="generateDraft">
            <Sparkles :size="16" :stroke-width="1.6" />
            {{ drafting ? 'AI 整理中...' : 'AI 整理候选案例' }}
          </button>
        </section>

        <section class="submission-draft-form">
          <p class="section-kicker">AI DRAFT</p>
          <h3>候选案例预览</h3>
          <div class="draft-editor">
            <label>
              <span>故障名称</span>
              <input v-model="draftForm.errorName" placeholder="AI 生成或手动填写" />
            </label>
            <label>
              <span>关键词</span>
              <input v-model="draftForm.keyword" placeholder="多个关键词用逗号分隔" />
            </label>
            <label>
              <span>报错现象</span>
              <textarea v-model="draftForm.symptom" rows="2"></textarea>
            </label>
            <label>
              <span>可能原因</span>
              <textarea v-model="draftForm.cause" rows="2"></textarea>
            </label>
            <label>
              <span>解决方案</span>
              <textarea v-model="draftForm.solution" rows="3"></textarea>
            </label>
            <label>
              <span>相关文件</span>
              <input v-model="draftForm.relatedFile" />
            </label>
            <label>
              <span>相关命令</span>
              <input v-model="draftForm.relatedCommand" />
            </label>
            <label>
              <span>风险提示</span>
              <textarea v-model="draftForm.riskTip" rows="2"></textarea>
            </label>
          </div>
        </section>
      </div>
      <template #footer>
        <button class="dialog-secondary" type="button" @click="submissionDialogVisible = false">取消</button>
        <button class="dialog-primary" type="button" :disabled="submittingSubmission" @click="submitExperience">
          {{ submittingSubmission ? '提交中...' : '提交审核' }}
        </button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import './knowledge-base-view.css'
import axios from 'axios'
import * as echarts from 'echarts'
import { BookOpen, Heart, Inbox, Network, Plus, RefreshCw, Sparkles, TrendingUp, UploadCloud } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, shallowRef, watch } from 'vue'

const props = defineProps({
  currentUser: {
    type: Object,
    default: () => ({})
  }
})

const cases = ref([])
const selectedCase = ref(null)
const loading = ref(false)
const importing = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const componentChartRef = ref(null)
const componentChart = shallowRef(null)
const submissionDialogVisible = ref(false)
const reviewPanelVisible = ref(false)
const drafting = ref(false)
const submittingSubmission = ref(false)
const submissionLoading = ref(false)
const approving = ref(false)
const rejecting = ref(false)
const submissions = ref([])
const selectedSubmission = ref(null)
const submissionFilter = ref('PENDING')
const favoriteIds = ref([])

const filters = reactive({
  keyword: '',
  component: '',
  stage: ''
})

const caseForm = reactive(emptyCaseForm())
const submissionForm = reactive(emptySubmissionForm())
const draftForm = reactive(emptyDraftForm())
const reviewDraft = reactive(emptyDraftForm())

const componentOptions = computed(() => unique(cases.value.map(item => item.component)))
const stageOptions = computed(() => unique(cases.value.map(item => item.stage)))
const componentCount = computed(() => componentOptions.value.length)
const stageCount = computed(() => stageOptions.value.length)
const filteredHint = computed(() => `${cases.value.length} 条记录`)
const canReviewSubmissions = computed(() => hasRole('ADMIN') || hasRole('MAINTAINER'))
const canMaintainKnowledge = computed(() => hasRole('ADMIN') || hasRole('MAINTAINER'))
const pendingCount = computed(() => submissions.value.filter(item => item.status === 'PENDING').length)
const submissionTechStacks = ['Spring Boot', 'MySQL', 'Vue', 'Neo4j', 'Maven', 'LLM API', 'Axios', '其他']

const componentDistribution = computed(() => {
  const counts = new Map()
  for (const item of cases.value) {
    const label = item.component || '未分类'
    counts.set(label, (counts.get(label) || 0) + 1)
  }
  const total = Math.max(cases.value.length, 1)
  return [...counts.entries()]
    .map(([label, count]) => ({ label, count, percent: Math.round((count / total) * 100) }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 6)
})

const chartData = computed(() => (
  componentDistribution.value.length
    ? componentDistribution.value.slice(0, 5)
    : [{ label: '暂无数据', count: 0, percent: 0 }]
))

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
    userId: props.currentUser?.id || 1
  }
}

function emptySubmissionForm() {
  return {
    submitUserId: props.currentUser?.id || 1,
    rawLog: '',
    techStack: 'Spring Boot',
    stage: '',
    userSolution: '',
    relatedFile: '',
    relatedCommand: ''
  }
}

function emptyDraftForm() {
  return {
    errorName: '',
    keyword: '',
    symptom: '',
    cause: '',
    solution: '',
    relatedFile: '',
    relatedCommand: '',
    riskTip: ''
  }
}

function unique(values) {
  return [...new Set(values.map(item => item?.trim()).filter(Boolean))]
}

function shortLabel(value) {
  if (!value) return '未分类'
  return value.length > 12 ? `${value.slice(0, 12)}...` : value
}

function hasRole(roleCode) {
  return props.currentUser?.roles?.some(role => role.roleCode === roleCode)
}

function currentUserId() {
  return props.currentUser?.id || 1
}

function isFavorite(caseId) {
  return favoriteIds.value.includes(Number(caseId))
}

function statusText(status) {
  const labels = {
    PENDING: '待审核',
    APPROVED: '已通过',
    REJECTED: '已驳回'
  }
  return labels[status] || status || '未知'
}

function resetForm(values = emptyCaseForm()) {
  Object.assign(caseForm, values)
  caseForm.userId = props.currentUser?.id || 1
}

function resetSubmissionForm() {
  Object.assign(submissionForm, emptySubmissionForm())
  Object.assign(draftForm, emptyDraftForm())
}

function copyDraft(target, source = {}) {
  Object.assign(target, {
    errorName: source.errorName || source.draftErrorName || '',
    keyword: source.keyword || source.draftKeyword || '',
    symptom: source.symptom || source.draftSymptom || '',
    cause: source.cause || source.draftCause || '',
    solution: source.solution || source.draftSolution || '',
    relatedFile: source.relatedFile || source.draftRelatedFile || '',
    relatedCommand: source.relatedCommand || source.draftRelatedCommand || '',
    riskTip: source.riskTip || source.draftRiskTip || ''
  })
}

function openSubmissionDialog() {
  resetSubmissionForm()
  submissionDialogVisible.value = true
}

async function generateDraft() {
  if (!submissionForm.rawLog.trim() || !submissionForm.userSolution.trim()) {
    ElMessage.warning('请先填写报错日志和解决过程')
    return
  }
  drafting.value = true
  try {
    const response = await axios.post('/api/submissions/draft', {
      ...submissionForm,
      submitUserId: props.currentUser?.id || 1
    })
    copyDraft(draftForm, response.data.draft)
    ElMessage.success('AI 已整理候选案例，可继续编辑')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || 'AI 整理失败，请确认后端和 DeepSeek 配置')
  } finally {
    drafting.value = false
  }
}

async function submitExperience() {
  if (!submissionForm.rawLog.trim() || !submissionForm.userSolution.trim()) {
    ElMessage.warning('请先填写报错日志和解决过程')
    return
  }
  if (!draftForm.errorName.trim() || !draftForm.keyword.trim() || !draftForm.solution.trim()) {
    ElMessage.warning('请先生成或补全候选案例字段')
    return
  }
  submittingSubmission.value = true
  try {
    await axios.post('/api/submissions/create', {
      ...submissionForm,
      submitUserId: props.currentUser?.id || 1,
      draft: { ...draftForm }
    })
    ElMessage.success('投稿已提交审核')
    submissionDialogVisible.value = false
    if (canReviewSubmissions.value) {
      await loadSubmissions()
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '投稿提交失败')
  } finally {
    submittingSubmission.value = false
  }
}

async function loadSubmissions() {
  if (!canReviewSubmissions.value) return
  submissionLoading.value = true
  try {
    const params = {}
    if (submissionFilter.value) params.status = submissionFilter.value
    const response = await axios.get('/api/submissions/list', { params })
    submissions.value = response.data.items || []
    if (!selectedSubmission.value || !submissions.value.some(item => item.id === selectedSubmission.value.id)) {
      selectSubmission(submissions.value[0] || null)
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '投稿审核列表加载失败')
  } finally {
    submissionLoading.value = false
  }
}

async function toggleReviewPanel() {
  reviewPanelVisible.value = !reviewPanelVisible.value
  if (reviewPanelVisible.value) {
    await loadSubmissions()
  }
}

function selectSubmission(item) {
  selectedSubmission.value = item
  copyDraft(reviewDraft, item || {})
}

async function approveSubmission() {
  if (!selectedSubmission.value) return
  approving.value = true
  try {
    await axios.put(`/api/submissions/${selectedSubmission.value.id}/approve`, {
      reviewerId: props.currentUser?.id || 1,
      reviewComment: '审核通过，沉淀为知识库案例',
      draft: { ...reviewDraft }
    })
    ElMessage.success('投稿已通过并写入故障知识库')
    await loadSubmissions()
    await loadCases()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '审核通过失败')
  } finally {
    approving.value = false
  }
}

async function rejectSubmission() {
  if (!selectedSubmission.value) return
  try {
    const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回投稿', {
      confirmButtonText: '驳回',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：日志信息不足，建议补充完整堆栈'
    })
    rejecting.value = true
    await axios.put(`/api/submissions/${selectedSubmission.value.id}/reject`, {
      reviewerId: props.currentUser?.id || 1,
      reviewComment: value
    })
    ElMessage.success('投稿已驳回')
    await loadSubmissions()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.response?.data?.message || '驳回失败')
    }
  } finally {
    rejecting.value = false
  }
}

async function loadCases() {
  loading.value = true
  try {
    const response = await axios.get('/api/error/list', { params: filters })
    cases.value = response.data.items || []
    if (!selectedCase.value || !cases.value.some(item => item.id === selectedCase.value.id)) {
      selectedCase.value = cases.value[0] || null
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '故障知识库加载失败，请确认后端已启动')
  } finally {
    loading.value = false
  }
}

async function loadFavorites() {
  try {
    const response = await axios.get('/api/favorites/case-ids', {
      params: { userId: currentUserId() }
    })
    favoriteIds.value = response.data.ids || []
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '收藏列表加载失败')
  }
}

async function toggleFavorite(item) {
  if (!item?.id) return
  try {
    if (isFavorite(item.id)) {
      await axios.delete(`/api/favorites/cases/${item.id}`, {
        params: { userId: currentUserId() }
      })
      favoriteIds.value = favoriteIds.value.filter(id => id !== Number(item.id))
      ElMessage.success('已取消收藏')
    } else {
      await axios.post(`/api/favorites/cases/${item.id}`, null, {
        params: { userId: currentUserId() }
      })
      favoriteIds.value = [...favoriteIds.value, Number(item.id)]
      ElMessage.success('已加入收藏')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '收藏操作失败')
  }
}

async function renderComponentChart() {
  await nextTick()
  if (!componentChartRef.value) return
  if (!componentChart.value) {
    componentChart.value = echarts.init(componentChartRef.value)
  }
  const data = chartData.value
  componentChart.value.setOption({
    animationDuration: 700,
    grid: { left: 8, right: 8, top: 18, bottom: 10, containLabel: false },
    tooltip: {
      trigger: 'axis',
      borderWidth: 0,
      backgroundColor: 'rgba(255,255,255,0.94)',
      textStyle: { color: '#344257' }
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: data.map(item => shortLabel(item.label)),
      show: false
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitNumber: 4,
      axisLabel: { show: false },
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: {
        lineStyle: { color: 'rgba(206, 221, 236, 0.72)', width: 1 }
      }
    },
    series: [{
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 9,
      data: data.map(item => item.count),
      lineStyle: {
        width: 4,
        color: '#75a4ff',
        cap: 'round',
        join: 'round'
      },
      itemStyle: {
        color: '#f8fbff',
        borderColor: '#7ea9ff',
        borderWidth: 3
      },
      areaStyle: {
        color: 'rgba(132, 174, 255, 0.14)'
      },
      emphasis: {
        itemStyle: { borderColor: '#4f86f0' }
      }
    }]
  })
}

function resizeComponentChart() {
  componentChart.value?.resize()
}

function openCreateDialog() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(item) {
  editingId.value = item.id
  resetForm({ ...item, userId: props.currentUser?.id || 1 })
  dialogVisible.value = true
}

async function saveCase() {
  if (!caseForm.errorName.trim()) {
    ElMessage.warning('请填写故障名称')
    return
  }

  saving.value = true
  try {
    if (editingId.value) {
      const response = await axios.put(`/api/error/${editingId.value}`, caseForm)
      selectedCase.value = response.data
      ElMessage.success('故障案例已更新')
    } else {
      const response = await axios.post('/api/error/create', caseForm)
      selectedCase.value = response.data
      ElMessage.success('故障案例已新增')
    }
    dialogVisible.value = false
    await loadCases()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '保存失败，请检查必填项或故障名称是否重复')
  } finally {
    saving.value = false
  }
}

async function deleteCase(item) {
  try {
    await ElMessageBox.confirm(`确认删除“${item.errorName}”？删除后需要重新同步图谱。`, '删除确认', { type: 'warning' })
    await axios.delete(`/api/error/${item.id}`)
    ElMessage.success('故障案例已删除')
    await loadCases()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

async function importToGraph() {
  importing.value = true
  try {
    const response = await axios.post('/api/kg/import')
    ElMessage.success(`已同步 ${response.data.importedCases} 条故障案例到 Neo4j`)
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '同步失败，请确认 Neo4j 已启动且账号密码正确')
  } finally {
    importing.value = false
  }
}

watch(chartData, renderComponentChart, { deep: true })

onMounted(() => {
  loadCases()
  loadFavorites()
  renderComponentChart()
  if (canReviewSubmissions.value) {
    loadSubmissions()
  }
  window.addEventListener('resize', resizeComponentChart)
})

watch(canReviewSubmissions, value => {
  if (value) loadSubmissions()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeComponentChart)
  componentChart.value?.dispose()
})
</script>
