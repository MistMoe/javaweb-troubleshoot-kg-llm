<template>
  <section class="dashboard-view">
    <div class="workspace-mosaic">
      <header class="tile overview-tile surface-card surface-card--strong">
        <div class="overview-copy">
          <p class="eyebrow">WORKSPACE / SYSTEM OVERVIEW</p>
          <h1>欢迎回来，{{ displayName }}</h1>
          <p>智能排障、知识沉淀与图谱服务均已就绪。</p>
        </div>
        <div class="status-plates" aria-label="系统状态">
          <span
            v-for="item in statusCapsules"
            :key="item.label"
            class="status-plate metric-plate"
            :class="`status-plate--${item.tone}`"
          >
            <i aria-hidden="true"></i>
            <span><strong>{{ item.label }}</strong><small>{{ item.detail }}</small></span>
          </span>
        </div>
      </header>

      <section class="tile trend-tile surface-card">
        <div class="tile-heading">
          <div>
            <p class="eyebrow">ANALYSIS PULSE / 07 DAYS</p>
            <h2>排障分析趋势</h2>
          </div>
          <div class="heading-meta">
            <span>实时工作区</span>
            <Activity :size="18" :stroke-width="1.45" />
          </div>
        </div>

        <div class="trend-layout">
          <div class="chart-frame inset-well">
            <div ref="trendChartRef" class="trend-chart" role="img" aria-label="最近七日排障分析趋势"></div>
          </div>

          <aside class="daily-focus">
            <div class="status-ring inset-well" aria-label="解决成功率 85%">
              <div ref="statusChartRef" class="status-chart"></div>
              <div class="ring-inner">
                <strong>85%</strong>
                <span>解决成功率</span>
              </div>
            </div>
            <div class="focus-metrics">
              <span class="metric-plate"><strong>32</strong><small>今日分析</small></span>
              <span class="metric-plate"><strong>2.4s</strong><small>平均响应</small></span>
            </div>
            <button class="action-button action-button--primary" type="button" @click="emit('navigate', 'analyze')">
              开始排障 <ArrowRight :size="14" :stroke-width="1.5" />
            </button>
          </aside>
        </div>
      </section>

      <section class="tile service-tile surface-card">
        <section class="service-section connection-section">
          <div class="tile-heading compact">
            <div>
              <p class="eyebrow">CONNECTIONS</p>
              <h2>系统连接状态</h2>
            </div>
            <Server :size="18" :stroke-width="1.45" />
          </div>
          <ul class="status-list">
            <li v-for="item in connections" :key="item.label">
              <i :class="item.tone" aria-hidden="true"></i>
              <span>{{ item.label }}</span>
              <small>{{ item.state }}</small>
            </li>
          </ul>
        </section>

        <section class="service-section llm-section">
          <div class="tile-heading compact">
            <div>
              <p class="eyebrow">MODEL SERVICE</p>
              <h2>LLM 模型服务</h2>
            </div>
            <Cpu :size="18" :stroke-width="1.45" />
          </div>
          <div class="llm-grid">
            <span>当前模型</span><strong>DeepSeek</strong>
            <span>接口状态</span><strong class="ready-text">Ready</strong>
            <span>今日调用</span><strong>18</strong>
            <span>平均延迟</span><strong>1.6s</strong>
          </div>
          <p class="service-note"><i></i>模型服务运行正常</p>
        </section>
      </section>

      <section class="tile records-tile surface-card">
        <div class="tile-heading">
          <div>
            <p class="eyebrow">RECENT REPORTS</p>
            <h2>最近分析记录</h2>
          </div>
          <button class="inline-link" type="button" @click="emit('navigate', historyTarget)">
            查看全部 <ArrowRight :size="14" :stroke-width="1.5" />
          </button>
        </div>
        <div class="record-list inset-well">
          <button v-for="item in records" :key="item.error" class="record-row" type="button">
            <span>{{ item.error }}</span>
            <small>{{ item.stack }}</small>
            <em :class="item.statusClass">{{ item.status }}</em>
          </button>
        </div>
      </section>

      <section class="tile distribution-tile surface-card">
        <div class="tile-heading">
          <div>
            <p class="eyebrow">DISTRIBUTION</p>
            <h2>故障类型分布</h2>
          </div>
          <Layers :size="18" :stroke-width="1.45" />
        </div>
        <div class="chart-frame inset-well distribution-frame">
          <div ref="distributionChartRef" class="distribution-chart" role="img" aria-label="故障类型分布"></div>
        </div>
        <button class="inline-link" type="button" @click="emit('navigate', 'knowledge')">
          进入知识库 <ArrowRight :size="14" :stroke-width="1.5" />
        </button>
      </section>
    </div>
  </section>
</template>

<script setup>
import './dashboard-view.css'
import * as echarts from 'echarts'
import { Activity, ArrowRight, Cpu, Layers, Server } from 'lucide-vue-next'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, shallowRef } from 'vue'
import {
  chartAxisStyle,
  chartPalette,
  chartTooltipStyle,
  createChartResizeObserver
} from '../../utils/chartTheme'

const props = defineProps({
  currentUser: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['navigate'])

const displayName = computed(() => props.currentUser?.nickname || props.currentUser?.username || '系统管理员')
const historyTarget = computed(() => (props.currentUser?.roles?.some(role => role.roleCode === 'USER') ? 'my-history' : 'history'))

const statusChartRef = ref(null)
const trendChartRef = ref(null)
const distributionChartRef = ref(null)
const statusChart = shallowRef(null)
const trendChart = shallowRef(null)
const distributionChart = shallowRef(null)
const stopResizeObservers = []

const statusCapsules = [
  { label: 'System Online', detail: '核心服务', tone: 'blue' },
  { label: 'Neo4j Connected', detail: '图谱连接', tone: 'violet' },
  { label: 'LLM Ready', detail: '模型就绪', tone: 'cyan' }
]

const connections = [
  { label: 'Neo4j Connected', state: 'online', tone: 'violet' },
  { label: 'MySQL Connected', state: 'online', tone: 'cyan' },
  { label: 'Backend Running', state: 'stable', tone: 'blue' },
  { label: 'Frontend Ready', state: 'ready', tone: 'mint' }
]

const records = [
  { error: 'NullPointerException', stack: 'Spring Boot', status: '已解决', statusClass: 'done' },
  { error: 'SQLSyntaxErrorException', stack: 'MyBatis', status: '已解决', statusClass: 'done' },
  { error: 'Connection timeout', stack: 'MySQL', status: '处理中', statusClass: 'pending' },
  { error: 'Neo4j Unauthorized', stack: 'Neo4j', status: '已解决', statusClass: 'done' }
]

const distributions = [
  { label: 'Spring Boot', value: 38 },
  { label: 'MySQL', value: 24 },
  { label: 'Vue', value: 18 },
  { label: 'Neo4j', value: 12 },
  { label: 'LLM API', value: 8 }
]

function renderStatusChart() {
  if (!statusChartRef.value) return
  if (!statusChart.value) statusChart.value = echarts.init(statusChartRef.value)

  statusChart.value.setOption({
    animationDuration: 450,
    series: [{
      type: 'gauge',
      startAngle: 220,
      endAngle: -40,
      min: 0,
      max: 100,
      radius: '96%',
      center: ['50%', '50%'],
      progress: {
        show: true,
        roundCap: true,
        width: 13,
        itemStyle: { color: chartPalette[1] }
      },
      axisLine: {
        roundCap: true,
        lineStyle: { width: 13, color: [[1, 'rgba(139, 164, 191, 0.18)']] }
      },
      pointer: { show: false },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: { show: false },
      anchor: { show: false },
      title: { show: false },
      detail: { show: false },
      data: [{ value: 85 }]
    }]
  })
}

function renderTrendChart() {
  if (!trendChartRef.value) return
  if (!trendChart.value) trendChart.value = echarts.init(trendChartRef.value)

  trendChart.value.setOption({
    animationDuration: 420,
    color: chartPalette,
    tooltip: { trigger: 'axis', ...chartTooltipStyle },
    legend: {
      top: 0,
      right: 0,
      itemWidth: 12,
      itemHeight: 7,
      textStyle: { color: '#6f8299', fontSize: 11 },
      data: ['分析量', '解决率']
    },
    grid: { top: 38, right: 42, bottom: 26, left: 38, containLabel: false },
    xAxis: {
      type: 'category',
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '今日'],
      boundaryGap: true,
      ...chartAxisStyle
    },
    yAxis: [
      {
        type: 'value',
        min: 0,
        max: 36,
        interval: 12,
        ...chartAxisStyle
      },
      {
        type: 'value',
        min: 60,
        max: 90,
        interval: 10,
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { color: '#9689e6', fontSize: 10, formatter: '{value}%' },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '分析量',
        type: 'bar',
        data: [18, 21, 19, 26, 24, 30, 32],
        barMaxWidth: 18,
        itemStyle: { color: chartPalette[0], borderRadius: [4, 4, 1, 1] }
      },
      {
        name: '解决率',
        type: 'line',
        yAxisIndex: 1,
        data: [72, 76, 74, 81, 82, 84, 85],
        smooth: 0.28,
        symbol: 'circle',
        symbolSize: 7,
        lineStyle: { color: chartPalette[1], width: 2.5 },
        itemStyle: { color: '#f6f9fc', borderColor: chartPalette[1], borderWidth: 2 },
        emphasis: { scale: 1.25 }
      }
    ]
  })
}

function renderDistributionChart() {
  if (!distributionChartRef.value) return
  if (!distributionChart.value) distributionChart.value = echarts.init(distributionChartRef.value)

  distributionChart.value.setOption({
    animationDuration: 420,
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, ...chartTooltipStyle },
    grid: { top: 8, right: 34, bottom: 8, left: 92 },
    xAxis: {
      type: 'value',
      max: 40,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { show: false },
      splitLine: { show: false }
    },
    yAxis: {
      type: 'category',
      inverse: true,
      data: distributions.map(item => item.label),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#6f8299', fontSize: 11 }
    },
    series: [{
      type: 'bar',
      data: distributions.map((item, index) => ({
        value: item.value,
        itemStyle: { color: chartPalette[index], borderRadius: [0, 5, 5, 0] }
      })),
      barWidth: 11,
      label: {
        show: true,
        position: 'right',
        color: '#6f8299',
        fontSize: 10,
        formatter: '{c}%'
      }
    }]
  })
}

function resizeCharts() {
  statusChart.value?.resize()
  trendChart.value?.resize()
  distributionChart.value?.resize()
}

onMounted(async () => {
  await nextTick()
  renderStatusChart()
  renderTrendChart()
  renderDistributionChart()
  stopResizeObservers.push(
    createChartResizeObserver(statusChartRef.value, () => statusChart.value?.resize()),
    createChartResizeObserver(trendChartRef.value, () => trendChart.value?.resize()),
    createChartResizeObserver(distributionChartRef.value, () => distributionChart.value?.resize())
  )
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  stopResizeObservers.forEach(stop => stop())
  statusChart.value?.dispose()
  trendChart.value?.dispose()
  distributionChart.value?.dispose()
})
</script>
