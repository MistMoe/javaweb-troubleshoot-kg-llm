<template>
  <section class="favorites-view">
    <div class="favorites-grid">
      <section class="favorite-card favorites-overview surface-card surface-card--strong">
        <div class="favorites-overview__copy">
          <p class="section-kicker">SAVED KNOWLEDGE</p>
          <h1>我的收藏</h1>
          <p>沉淀你常用的故障案例，演示时可以快速打开报错现象、可能原因和解决方案。</p>
        </div>
        <div class="favorites-overview__metrics">
          <article class="metric-plate favorites-overview__metric is-blue">
            <strong>{{ favorites.length }}</strong>
            <span>收藏案例</span>
          </article>
          <article class="metric-plate favorites-overview__metric is-violet">
            <strong>{{ componentCount }}</strong>
            <span>技术组件</span>
          </article>
          <article class="metric-plate favorites-overview__metric is-cyan">
            <strong>{{ topComponent }}</strong>
            <span>常用组件</span>
          </article>
        </div>
      </section>

      <section class="favorite-card favorites-toolbar surface-card">
        <div class="favorites-toolbar__context">
          <span>SAVED WORKSPACE</span>
          <small>整理你的排障速查案例</small>
        </div>
        <div class="favorites-actions">
          <button class="action-button favorites-refresh-action" type="button" :disabled="loading" title="刷新收藏" aria-label="刷新收藏" @click="loadFavorites">
            <RefreshCw :size="16" :stroke-width="1.6" />
          </button>
          <button class="action-button action-button--primary" type="button" @click="$emit('navigate', 'knowledge')">
            <BookOpen :size="17" :stroke-width="1.7" />
            去知识库
          </button>
        </div>
      </section>

      <section class="favorite-card favorites-filter-card surface-card">
        <label>
          <span>关键词</span>
          <input v-model="keyword" placeholder="搜索故障名、组件、关键词" />
        </label>
        <button class="action-button" type="button" @click="keyword = ''">清空</button>
      </section>

      <section class="favorite-card favorites-list-card surface-card">
        <div class="panel-heading">
          <div>
            <p class="section-kicker">FAVORITE CASES</p>
            <h2>收藏列表</h2>
          </div>
          <span>{{ filteredFavorites.length }} 条</span>
        </div>

        <div v-if="loading" class="loading-state">
          <span></span>
          <p>正在加载收藏案例...</p>
        </div>
        <div v-else-if="!favorites.length" class="empty-state">
          <Heart :size="44" :stroke-width="1.35" />
          <p>还没有收藏案例。去故障知识库点亮“收藏”，这里就会变成你的排障速查夹。</p>
          <button class="action-button action-button--primary" type="button" @click="$emit('navigate', 'knowledge')">去知识库收藏</button>
        </div>
        <div v-else class="favorite-list">
          <button
            v-for="item in filteredFavorites"
            :key="item.id"
            type="button"
            class="favorite-row"
            :class="{ active: selectedFavorite?.id === item.id }"
            @click="selectedFavorite = item"
          >
            <span class="row-icon">
              <Heart :size="17" fill="currentColor" :stroke-width="1.6" />
            </span>
            <span class="row-main">
              <strong>{{ item.errorName }}</strong>
              <small>{{ item.stage || '未填写环节' }} / {{ item.component || '未填写组件' }}</small>
            </span>
            <span class="row-keyword">{{ item.keyword || '无关键词' }}</span>
            <i @click.stop="removeFavorite(item)">取消收藏</i>
          </button>
        </div>
      </section>

      <aside class="favorite-card favorites-detail-card surface-card">
        <div class="panel-heading">
          <div>
            <p class="section-kicker">CASE SNAPSHOT</p>
            <h2>收藏详情</h2>
          </div>
          <BookmarkCheck :size="18" :stroke-width="1.45" />
        </div>

        <div v-if="!selectedFavorite" class="empty-state detail-empty">
          <FileText :size="42" :stroke-width="1.35" />
          <p>选择左侧收藏，查看它对应的技术栈和匹配关键词。</p>
        </div>
        <template v-else>
          <div class="detail-title">
            <small>#{{ selectedFavorite.errorCaseId }}</small>
            <strong>{{ selectedFavorite.errorName }}</strong>
          </div>
          <div class="tag-line">
            <em>{{ selectedFavorite.stage || '未填写环节' }}</em>
            <em>{{ selectedFavorite.component || '未填写组件' }}</em>
          </div>
          <article class="inset-well">
            <h3>匹配关键词</h3>
            <p>{{ selectedFavorite.keyword || '暂无关键词' }}</p>
          </article>
          <article class="inset-well">
            <h3>收藏时间</h3>
            <p>{{ formatTime(selectedFavorite.createTime) }}</p>
          </article>
          <div class="detail-actions">
            <button class="action-button" type="button" @click="$emit('navigate', 'knowledge')">回到知识库</button>
            <button class="action-button action-button--danger" type="button" @click="removeFavorite(selectedFavorite)">取消收藏</button>
          </div>
        </template>
      </aside>
    </div>
  </section>
</template>

<script setup>
import './favorite-cases-view.css'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { BookmarkCheck, BookOpen, FileText, Heart, RefreshCw } from 'lucide-vue-next'
import { computed, onMounted, ref, watch } from 'vue'

const props = defineProps({
  currentUser: {
    type: Object,
    default: () => ({})
  }
})

defineEmits(['navigate'])

const favorites = ref([])
const selectedFavorite = ref(null)
const loading = ref(false)
const keyword = ref('')

const filteredFavorites = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  if (!text) return favorites.value
  return favorites.value.filter(item => [
    item.errorName,
    item.stage,
    item.component,
    item.keyword
  ].some(value => String(value || '').toLowerCase().includes(text)))
})

const componentCount = computed(() => new Set(favorites.value.map(item => item.component).filter(Boolean)).size)
const topComponent = computed(() => {
  if (!favorites.value.length) return '暂无'
  const counts = new Map()
  favorites.value.forEach(item => {
    const key = item.component || '未分类'
    counts.set(key, (counts.get(key) || 0) + 1)
  })
  return [...counts.entries()].sort((a, b) => b[1] - a[1])[0][0]
})

function currentUserId() {
  return props.currentUser?.id || 1
}

async function loadFavorites() {
  loading.value = true
  try {
    const response = await axios.get('/api/favorites/cases', {
      params: { userId: currentUserId() }
    })
    favorites.value = response.data.items || []
    if (!selectedFavorite.value || !favorites.value.some(item => item.id === selectedFavorite.value.id)) {
      selectedFavorite.value = favorites.value[0] || null
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '收藏案例加载失败')
  } finally {
    loading.value = false
  }
}

async function removeFavorite(item) {
  if (!item?.errorCaseId) return
  try {
    await axios.delete(`/api/favorites/cases/${item.errorCaseId}`, {
      params: { userId: currentUserId() }
    })
    ElMessage.success('已取消收藏')
    await loadFavorites()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '取消收藏失败')
  }
}

function formatTime(value) {
  if (!value) return '未知'
  return String(value).replace('T', ' ').slice(0, 16)
}

watch(filteredFavorites, value => {
  if (selectedFavorite.value && !value.some(item => item.id === selectedFavorite.value.id)) {
    selectedFavorite.value = value[0] || null
  }
})

onMounted(loadFavorites)
</script>
