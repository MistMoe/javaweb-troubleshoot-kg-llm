<template>
  <section class="analyze-view">
    <div class="analyze-grid">
      <section class="analyze-card input-card surface-card surface-card--strong">
        <div class="section-kicker">INTELLIGENT TROUBLESHOOTING</div>
        <h1>智能排障</h1>
        <p class="page-copy">
          输入 JavaWeb 报错日志，系统会先匹配 MySQL 知识库，再检索 Neo4j 图谱，并生成排障建议。
        </p>

        <el-form class="troubleshoot-form" label-position="top" @submit.prevent>
          <el-form-item label="报错日志 / 异常信息">
            <el-input
              v-model="form.inputText"
              type="textarea"
              :rows="10"
              resize="none"
              placeholder="例如：Web server failed to start. Port 8080 already in use."
            />
          </el-form-item>

          <el-form-item label="技术栈">
            <div class="stack-options">
              <button
                v-for="item in techStackOptions"
                :key="item"
                type="button"
                :class="{ active: form.techStack === item }"
                @click="form.techStack = item"
              >
                {{ item }}
              </button>
            </div>
          </el-form-item>

          <div class="sample-strip">
            <button v-for="sample in samples" :key="sample.label" type="button" @click="useSample(sample)">
              {{ sample.label }}
            </button>
          </div>

          <button class="primary-action action-button action-button--primary" type="button" :disabled="analyzing" @click="analyze">
            <BrainCircuit :size="18" :stroke-width="1.6" />
            {{ analyzing ? '分析中...' : '开始智能分析' }}
          </button>
        </el-form>
      </section>

      <section class="analyze-card result-card surface-card">
        <div class="card-heading">
          <div>
            <p class="section-kicker">ANALYSIS RESULT</p>
            <h2>分析结果</h2>
          </div>
          <div class="heading-actions">
            <span class="status-pill" :class="{ matched: result?.matched }">
              {{ resultStatus }}
            </span>
            <button v-if="result" class="export-action action-button" type="button" @click="exportAnalyzeWord">
              <FileDown :size="15" :stroke-width="1.6" />
              Word
            </button>
            <button v-if="result" class="export-action action-button" type="button" @click="exportAnalyzePdf">
              <Printer :size="15" :stroke-width="1.6" />
              PDF
            </button>
          </div>
        </div>

        <div class="analysis-track" aria-label="分析流程">
          <div
            v-for="(step, index) in analysisSteps"
            :key="step.label"
            class="track-step"
            :class="`track-step--${step.state}`"
          >
            <span>{{ index + 1 }}</span>
            <div>
              <strong>{{ step.label }}</strong>
              <small>{{ step.detail }}</small>
            </div>
          </div>
        </div>

        <div v-if="!result && !analyzing" class="result-empty inset-well">
          <el-empty description="提交报错后，这里会展示匹配结果和排障路径" />
        </div>
        <div v-if="analyzing" class="loading-state inset-well">
          <span></span>
          <p>正在检索知识库、图谱和 LLM 建议...</p>
        </div>

        <div v-if="result" class="result-content">
          <div class="fault-title">
            <small>{{ result.stage || 'Unknown Stage' }}</small>
            <strong>{{ result.matched ? result.errorName : '未匹配到明确故障案例' }}</strong>
            <p>{{ result.message }}</p>
          </div>

          <div class="metric-row">
            <div class="metric-plate">
              <strong>{{ result.matched ? '86%' : '32%' }}</strong>
              <span>匹配置信度</span>
            </div>
            <div class="metric-plate">
              <strong>{{ graphNodeCount }}</strong>
              <span>图谱节点</span>
            </div>
            <div class="metric-plate">
              <strong>{{ graphLinkCount }}</strong>
              <span>关联关系</span>
            </div>
          </div>

          <div class="info-grid">
            <article class="inset-well">
              <h3>可能原因</h3>
              <p v-if="!result.causes?.length">暂无明确原因，请补充完整报错、配置和版本。</p>
              <ul v-else>
                <li v-for="item in result.causes" :key="item">{{ item }}</li>
              </ul>
            </article>
            <article class="inset-well">
              <h3>解决方案</h3>
              <p v-if="!result.solutions?.length">暂无明确方案，建议先补充复现步骤。</p>
              <ul v-else>
                <li v-for="item in result.solutions" :key="item">{{ item }}</li>
              </ul>
            </article>
          </div>

          <div class="evidence-list">
            <div class="inset-well">
              <span>相关文件</span>
              <em v-for="item in result.files || []" :key="item">{{ item }}</em>
              <small v-if="!result.files?.length">暂无</small>
            </div>
            <div class="inset-well">
              <span>建议命令</span>
              <code v-for="item in result.commands || []" :key="item">{{ item }}</code>
              <small v-if="!result.commands?.length">暂无</small>
            </div>
          </div>
        </div>
      </section>

      <section class="analyze-card graph-card surface-card">
        <div class="card-heading">
          <div>
            <p class="section-kicker">KNOWLEDGE GRAPH</p>
            <h2>知识图谱证据</h2>
          </div>
          <Network :size="18" :stroke-width="1.5" />
        </div>
        <div v-if="graphNodeCount" class="graph-well inset-well">
          <div ref="chartRef" class="analyze-graph-canvas"></div>
        </div>
        <div v-else class="graph-empty inset-well">
          <Network :size="34" :stroke-width="1.3" />
          <span>命中知识库后会展示 Neo4j 关联路径</span>
        </div>
      </section>

      <section class="analyze-card advice-card surface-card">
        <div class="card-heading">
          <div>
            <p class="section-kicker">AI ADVICE</p>
            <h2>AI 排障建议</h2>
          </div>
          <Sparkles :size="18" :stroke-width="1.5" />
        </div>
        <div v-if="!result" class="advice-empty inset-well">
          <Sparkles :size="32" :stroke-width="1.25" />
          <p class="soft-text">等待一次分析后生成建议。</p>
        </div>
        <div v-else class="advice-body inset-well">{{ result.aiAdvice }}</div>
      </section>
    </div>
  </section>
</template>

<script setup>
import './analyze-view.css'
import axios from 'axios'
import * as echarts from 'echarts'
import { BrainCircuit, FileDown, Network, Printer, Sparkles } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { computed, nextTick, onBeforeUnmount, reactive, ref, shallowRef } from 'vue'
import { buildAnalyzeReport, downloadWordReport, printReportAsPdf } from '../../utils/reportExport'
import { createChartResizeObserver } from '../../utils/chartTheme'

const props = defineProps({
  currentUser: {
    type: Object,
    default: () => ({})
  }
})

const chartRef = ref(null)
const chart = shallowRef(null)
const analyzing = ref(false)
const result = ref(null)
let stopChartResizeObserver = () => {}

const form = reactive({
  inputText: 'Web server failed to start. Port 8080 already in use.',
  techStack: 'Spring Boot'
})

const techStackOptions = ['Spring Boot', 'Maven', 'MySQL', 'Vue', 'Axios', 'Neo4j', 'LLM API']

const samples = [
  {
    label: '端口占用',
    techStack: 'Spring Boot',
    inputText: 'Web server failed to start. Port 8080 already in use.'
  },
  {
    label: 'MySQL 连接失败',
    techStack: 'MySQL',
    inputText: 'Communications link failure. Connection refused when connecting to MySQL.'
  },
  {
    label: '依赖解析失败',
    techStack: 'Maven',
    inputText: 'Could not find artifact. Cannot resolve dependency in Maven build.'
  }
]

const resultStatus = computed(() => {
  if (analyzing.value) return 'ANALYZING'
  if (!result.value) return 'READY'
  return result.value.matched ? 'MATCHED' : 'NEED MORE INFO'
})

const graphNodeCount = computed(() => result.value?.graph?.nodes?.length || 0)
const graphLinkCount = computed(() => result.value?.graph?.links?.length || 0)
const analysisSteps = computed(() => {
  const state = result.value ? 'done' : analyzing.value ? 'active' : 'idle'
  return [
    { label: '知识库匹配', detail: result.value ? '案例检索完成' : analyzing.value ? '正在检索案例' : '等待提交', state },
    { label: '图谱检索', detail: result.value ? '关系证据完成' : analyzing.value ? '正在组织关系' : '等待提交', state },
    { label: 'DeepSeek 建议', detail: result.value ? '模型建议完成' : analyzing.value ? '正在生成建议' : '等待提交', state }
  ]
})

function useSample(sample) {
  form.inputText = sample.inputText
  form.techStack = sample.techStack
}

async function analyze() {
  if (!form.inputText.trim()) {
    ElMessage.warning('请先输入报错日志或异常信息')
    return
  }

  analyzing.value = true
  try {
    const response = await axios.post('/api/error/analyze', {
      inputText: form.inputText,
      techStack: form.techStack,
      userId: props.currentUser?.id || 1
    })
    result.value = response.data
    await renderGraph()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '分析失败，请确认后端、MySQL 和 Neo4j 已启动')
  } finally {
    analyzing.value = false
  }
}

function exportAnalyzeWord() {
  if (!result.value) return
  const html = buildAnalyzeReport(result.value, form, props.currentUser)
  downloadWordReport(html, `智能排障报告-${result.value.errorName || form.techStack || 'analysis'}`)
  ElMessage.success('Word 报告已生成')
}

function exportAnalyzePdf() {
  if (!result.value) return
  try {
    const html = buildAnalyzeReport(result.value, form, props.currentUser)
    printReportAsPdf(html)
  } catch (error) {
    ElMessage.error(error.message || 'PDF 打印窗口打开失败')
  }
}

async function renderGraph() {
  await nextTick()
  const graph = result.value?.graph
  if (!chartRef.value || !graph?.nodes?.length) {
    return
  }

  if (!chart.value) {
    chart.value = echarts.init(chartRef.value)
    stopChartResizeObserver()
    stopChartResizeObserver = createChartResizeObserver(chartRef.value, () => chart.value?.resize())
  }

  const categories = [...new Set(graph.nodes.map(node => node.category))]
  chart.value.setOption({
    color: ['#6f8ff7', '#8cc7df', '#a9a0f4', '#9bd1b3', '#f2c978', '#ef8585', '#7fc2a3', '#b795d7'],
    tooltip: {
      borderWidth: 0,
      backgroundColor: 'rgba(255,255,255,0.92)',
      textStyle: { color: '#344257' }
    },
    legend: {
      top: 4,
      left: 8,
      itemWidth: 14,
      itemHeight: 8,
      textStyle: { color: '#76869a', fontSize: 11 },
      data: categories
    },
    series: [{
      type: 'graph',
      layout: 'force',
      roam: true,
      draggable: true,
      top: 44,
      bottom: 12,
      categories: categories.map(name => ({ name })),
      data: graph.nodes.map(node => ({
        id: node.id,
        name: node.name,
        category: node.category,
        symbolSize: node.category === 'Error' ? 58 : 42,
        label: { color: '#526176' }
      })),
      links: graph.links.map(link => ({
        source: link.source,
        target: link.target,
        label: { show: true, formatter: link.label }
      })),
      force: { repulsion: 230, edgeLength: 118 },
      label: { show: true, fontSize: 11, width: 120, overflow: 'break' },
      lineStyle: { color: '#b8c5d4', opacity: 0.72 },
      edgeSymbol: ['none', 'arrow'],
      edgeSymbolSize: 7,
      edgeLabel: { color: '#98a6b7', fontSize: 10 }
    }]
  })
}

onBeforeUnmount(() => {
  stopChartResizeObserver()
  chart.value?.dispose()
})
</script>
