<template>
  <section class="kg-view">
    <div class="kg-grid">
      <section class="kg-card kg-hero-card surface-card surface-card--strong">
        <div>
          <p class="section-kicker">NEO4J KNOWLEDGE GRAPH</p>
          <h1>知识图谱</h1>
          <p>查看故障、环节、组件、原因、方案、文件和命令之间的关系，辅助解释排障路径。</p>
        </div>
      </section>

      <section class="kg-card kg-toolbar surface-card">
        <label>
          <span>故障案例</span>
          <select v-model="selectedErrorId" @change="loadGraph">
            <option value="">请选择故障案例</option>
            <option v-for="item in cases" :key="item.id" :value="item.id">{{ item.errorName }}</option>
          </select>
        </label>
        <label>
          <span>搜索节点</span>
          <input v-model="keyword" placeholder="例如 MySQL / 端口 / application.yml" @keyup.enter="searchGraph" />
        </label>
        <button class="action-button" type="button" :disabled="graphLoading" @click="loadGraph">查看关系</button>
        <button class="action-button" type="button" :disabled="graphLoading" @click="searchGraph">搜索节点</button>
        <button class="action-button kg-refresh-action" type="button" :disabled="loadingCases" title="刷新案例" aria-label="刷新案例" @click="loadCases">
          <RefreshCw :size="16" :stroke-width="1.6" />
        </button>
        <button class="action-button action-button--primary" type="button" :disabled="importing" @click="importGraph">
          <Network :size="17" :stroke-width="1.7" />
          {{ importing ? '导入中...' : '从 MySQL 导入图谱' }}
        </button>
      </section>

      <section class="kg-card kg-stat-card surface-card is-blue">
        <span>节点</span>
        <strong>{{ graphData.nodes.length }}</strong>
        <small>当前图谱结果</small>
      </section>
      <section class="kg-card kg-stat-card surface-card is-violet">
        <span>关系</span>
        <strong>{{ graphData.links.length }}</strong>
        <small>1 到 3 跳关联</small>
      </section>
      <section class="kg-card kg-stat-card surface-card is-cyan">
        <span>类型</span>
        <strong>{{ categories.length }}</strong>
        <small>Error / Cause / File...</small>
      </section>

      <section class="kg-card kg-graph-panel surface-card">
        <div class="kg-panel-heading">
          <div>
            <p class="section-kicker">RELATION MAP</p>
            <h2>故障关系图</h2>
          </div>
          <div class="kg-legend">
            <span v-for="item in categories" :key="item">{{ item }}</span>
          </div>
        </div>

        <div v-if="graphLoading" class="kg-graph-loading inset-well">
          <span></span>
          <p>正在加载 Neo4j 图谱...</p>
        </div>
        <div v-else-if="!graphData.nodes.length" class="kg-graph-empty inset-well">
          <Network :size="42" :stroke-width="1.35" />
          <span>请先导入图谱，或选择一个故障案例查看关系。</span>
        </div>
        <div v-show="graphData.nodes.length && !graphLoading" ref="chartRef" class="kg-graph-canvas inset-well"></div>
      </section>

      <section class="kg-card kg-side-panel surface-card">
        <div class="kg-panel-heading compact">
          <div>
            <p class="section-kicker">NODE DETAIL</p>
            <h2>节点详情</h2>
          </div>
          <PanelRight :size="18" :stroke-width="1.45" />
        </div>
        <el-empty v-if="!activeNode" description="点击图谱节点查看详情" />
        <div v-else class="kg-node-detail inset-well">
          <small>{{ activeNode.category }}</small>
          <strong>{{ activeNode.name || '未命名节点' }}</strong>
          <p>节点 ID：{{ activeNode.id }}</p>
          <div>
            <span>相关连线</span>
            <em>{{ activeNodeLinkCount }} 条</em>
          </div>
        </div>

        <div class="kg-graph-notes inset-well">
          <h3>使用提示</h3>
          <p>滚轮缩放，拖动画布或节点。图谱默认拉开节点距离，适合课堂演示时观察关系。</p>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import './knowledge-graph-view.css'
import axios from 'axios'
import * as echarts from 'echarts'
import { Network, PanelRight, RefreshCw } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, shallowRef } from 'vue'

const cases = ref([])
const selectedErrorId = ref('')
const keyword = ref('')
const graphData = ref({ nodes: [], links: [] })
const activeNode = ref(null)
const loadingCases = ref(false)
const graphLoading = ref(false)
const importing = ref(false)
const chartRef = ref(null)
const chart = shallowRef(null)
let resizeObserver = null

const categories = computed(() => [...new Set(graphData.value.nodes.map(node => node.category).filter(Boolean))])
const activeNodeLinkCount = computed(() => {
  if (!activeNode.value) return 0
  return graphData.value.links.filter(link => link.source === activeNode.value.id || link.target === activeNode.value.id).length
})

async function loadCases() {
  loadingCases.value = true
  try {
    const response = await axios.get('/api/error/list')
    cases.value = response.data.items || []
    if (!selectedErrorId.value && cases.value.length) {
      selectedErrorId.value = String(cases.value[0].id)
      await loadGraph()
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '故障案例加载失败，请确认后端已启动')
  } finally {
    loadingCases.value = false
  }
}

async function importGraph() {
  importing.value = true
  try {
    const response = await axios.post('/api/kg/import')
    ElMessage.success(`已导入 ${response.data.importedCases} 条故障案例到 Neo4j`)
    if (selectedErrorId.value) {
      await loadGraph()
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '导入失败，请确认 Neo4j 已启动且账号密码正确')
  } finally {
    importing.value = false
  }
}

async function loadGraph() {
  if (!selectedErrorId.value) {
    ElMessage.warning('请先选择故障案例')
    return
  }
  graphLoading.value = true
  try {
    const response = await axios.get(`/api/kg/graph/${selectedErrorId.value}`)
    graphData.value = response.data
    activeNode.value = graphData.value.nodes[0] || null
    graphLoading.value = false
    await renderGraph()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '图谱加载失败，请先从 MySQL 导入 Neo4j 数据')
  } finally {
    graphLoading.value = false
  }
}

async function searchGraph() {
  if (!keyword.value.trim()) {
    ElMessage.warning('请输入要搜索的图谱节点')
    return
  }
  graphLoading.value = true
  try {
    const response = await axios.get('/api/kg/search', { params: { keyword: keyword.value } })
    graphData.value = response.data
    activeNode.value = graphData.value.nodes[0] || null
    graphLoading.value = false
    await renderGraph()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '图谱搜索失败')
  } finally {
    graphLoading.value = false
  }
}

async function renderGraph() {
  await nextTick()
  await new Promise(resolve => requestAnimationFrame(resolve))
  if (!chartRef.value || !graphData.value.nodes.length) {
    return
  }
  if (chartRef.value.clientWidth < 20 || chartRef.value.clientHeight < 20) {
    await new Promise(resolve => requestAnimationFrame(resolve))
  }
  if (chartRef.value.clientWidth < 20 || chartRef.value.clientHeight < 20) {
    return
  }
  if (!chart.value) {
    chart.value = echarts.init(chartRef.value)
  }

  const categoryNames = categories.value
  chart.value.off('click')
  chart.value.clear()
  chart.value.resize()
  chart.value.setOption({
    color: ['#6f8ff7', '#8cc7df', '#a9a0f4', '#9bd1b3', '#f2c978', '#ef8585', '#7fc2a3', '#b795d7'],
    tooltip: {
      borderWidth: 0,
      backgroundColor: 'rgba(255,255,255,0.94)',
      textStyle: { color: '#344257' }
    },
    legend: {
      show: false
    },
    series: [{
      type: 'graph',
      layout: 'force',
      top: 42,
      right: 52,
      bottom: 48,
      left: 52,
      center: ['50%', '50%'],
      zoom: 0.92,
      roam: true,
      draggable: true,
      scaleLimit: {
        min: 0.45,
        max: 2.4
      },
      categories: categoryNames.map(name => ({ name })),
      data: graphData.value.nodes.map(node => ({
        id: node.id,
        name: node.name,
        category: node.category,
        symbolSize: node.category === 'Error' ? 78 : 52,
        label: { color: '#44546b' },
        itemStyle: {
          shadowBlur: node.category === 'Error' ? 28 : 18,
          shadowColor: 'rgba(105, 149, 224, 0.25)'
        }
      })),
      links: graphData.value.links.map(link => ({
        source: link.source,
        target: link.target,
        label: { show: true, formatter: link.label }
      })),
      force: {
        repulsion: 760,
        edgeLength: [175, 270],
        friction: 0.22,
        gravity: 0.035
      },
      label: {
        show: true,
        fontSize: 12,
        width: 128,
        overflow: 'break'
      },
      lineStyle: {
        color: '#b7c5d5',
        opacity: 0.7,
        width: 1.4,
        curveness: 0.08
      },
      edgeSymbol: ['none', 'arrow'],
      edgeSymbolSize: 8,
      edgeLabel: {
        color: '#91a0b2',
        fontSize: 10
      },
      emphasis: {
        focus: 'adjacency',
        lineStyle: { width: 2.4 }
      }
    }]
  }, true)
  chart.value.on('click', params => {
    if (params.dataType === 'node') {
      activeNode.value = {
        id: params.data.id,
        name: params.data.name,
        category: params.data.category
      }
    }
  })
  chart.value.resize()
}

function resizeGraph() {
  chart.value?.resize()
}

onMounted(async () => {
  await loadCases()
  await nextTick()
  if (chartRef.value && window.ResizeObserver) {
    resizeObserver = new ResizeObserver(resizeGraph)
    resizeObserver.observe(chartRef.value)
  }
  window.addEventListener('resize', resizeGraph)
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  window.removeEventListener('resize', resizeGraph)
  chart.value?.dispose()
})
</script>
