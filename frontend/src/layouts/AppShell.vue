<template>
  <div class="shell-root">
    <header class="top-bar shell-surface">
      <div class="top-left">
        <button class="icon-button" type="button" aria-label="Toggle navigation">
          <Menu :size="18" :stroke-width="1.5" />
        </button>
        <div class="brand-copy">
          <strong>JavaWeb Troubleshoot KG LLM</strong>
          <span>{{ activeMenuLabelText }}</span>
        </div>
      </div>

      <div class="top-right">
        <button class="icon-button" type="button" aria-label="Search">
          <Search :size="18" :stroke-width="1.5" />
        </button>
        <button class="icon-button notify-button" type="button" aria-label="Notifications">
          <Bell :size="18" :stroke-width="1.5" />
          <i aria-hidden="true"></i>
        </button>
        <button class="profile-trigger" type="button" @click="$emit('open-profile')">
          <span class="user-meta">
            <span>{{ roleLabel }}</span>
            <small>{{ currentUser?.username || 'user' }}</small>
          </span>
          <span class="avatar" aria-hidden="true">{{ avatarText }}</span>
        </button>
        <button class="icon-button" type="button" aria-label="Log out" @click="$emit('logout')">
          <LogOut :size="18" :stroke-width="1.5" />
        </button>
      </div>
    </header>

    <div class="shell-body">
      <aside class="side-rail rail-well" aria-label="Main navigation">
        <button
          v-for="item in visibleMenus"
          :key="item.key"
          class="rail-item"
          :class="{ active: activeMenu === item.key }"
          type="button"
          :title="menuLabel(item)"
          @click="$emit('select-menu', item.key)"
        >
          <component :is="menuIcon(item.key)" :size="19" :stroke-width="1.45" />
          <span>{{ menuLabel(item) }}</span>
        </button>
      </aside>

      <main class="workspace">
        <slot />
      </main>
    </div>

    <GlobalAiAssistant />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import GlobalAiAssistant from '../components/global/GlobalAiAssistant.vue'
import {
  Bell,
  BookOpen,
  BrainCircuit,
  Heart,
  History,
  Home,
  LogOut,
  Menu,
  Network,
  Search,
  Settings,
  UserRound,
  Users
} from 'lucide-vue-next'

const props = defineProps({
  currentUser: {
    type: Object,
    required: true
  },
  primaryRole: {
    type: String,
    default: 'USER'
  },
  visibleMenus: {
    type: Array,
    default: () => []
  },
  activeMenu: {
    type: String,
    default: 'home'
  },
  activeMenuLabel: {
    type: String,
    default: '工作台'
  }
})

defineEmits(['select-menu', 'logout', 'open-profile'])

const menuMeta = {
  home: { label: '工作台', icon: Home },
  analyze: { label: '智能排障', icon: BrainCircuit },
  graph: { label: '知识图谱', icon: Network },
  knowledge: { label: '故障知识库', icon: BookOpen },
  favorites: { label: '我的收藏', icon: Heart },
  profile: { label: '个人资料', icon: UserRound },
  history: { label: '分析历史', icon: History },
  'my-history': { label: '我的历史', icon: History },
  users: { label: '用户管理', icon: Users },
  roles: { label: '角色设置', icon: Settings }
}

const roleNames = {
  ADMIN: '管理员',
  MAINTAINER: '维护员',
  USER: '普通用户'
}

const roleLabel = computed(() => roleNames[props.primaryRole] || props.primaryRole)
const activeMenuLabelText = computed(() => menuMeta[props.activeMenu]?.label || props.activeMenuLabel)
const avatarText = computed(() => (props.currentUser?.nickname || props.currentUser?.username || 'U').slice(0, 1).toUpperCase())

function menuLabel(item) {
  return menuMeta[item.key]?.label || item.label || item.key
}

function menuIcon(key) {
  return menuMeta[key]?.icon || Settings
}
</script>

<style scoped>
.shell-root {
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  color: var(--text-primary);
  isolation: isolate;
  background: var(--surface-canvas);
}

.top-bar {
  position: relative;
  z-index: 40;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 58px;
  padding: 0 24px;
  background: var(--surface-soft);
  border-bottom: 1px solid rgba(255, 255, 255, 0.76);
  box-shadow:
    0 1px 0 rgba(255, 255, 255, 0.88),
    0 10px 24px rgba(103, 129, 156, 0.12);
}

.top-left,
.top-right {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.brand-copy {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.brand-copy strong {
  overflow: hidden;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.02em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.brand-copy span,
.user-meta small {
  color: var(--text-muted);
  font-size: 11px;
}

.icon-button {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  color: var(--text-secondary);
  background: var(--surface-panel);
  border: 1px solid rgba(255, 255, 255, 0.82);
  border-radius: var(--radius-control);
  box-shadow: var(--shadow-raised-soft);
  cursor: pointer;
  transition: color 160ms ease, border-color 160ms ease, box-shadow 160ms ease;
}

.icon-button:hover {
  color: var(--accent-blue);
  border-color: rgba(79, 134, 232, 0.3);
  box-shadow:
    -6px -6px 15px rgba(255, 255, 255, 0.82),
    8px 10px 18px rgba(99, 126, 155, 0.15);
}

.icon-button:active {
  box-shadow: var(--shadow-pressed);
}

.notify-button {
  position: relative;
}

.notify-button i {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 5px;
  height: 5px;
  background: var(--accent-coral);
  border-radius: 999px;
}

.user-meta {
  display: grid;
  gap: 2px;
  text-align: right;
}

.profile-trigger {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 3px 5px 3px 8px;
  background: transparent;
  border: 0;
  border-radius: var(--radius-control);
  cursor: pointer;
  transition: background 160ms ease;
}

.profile-trigger:hover {
  background: rgba(255, 255, 255, 0.36);
}

.user-meta span {
  color: var(--text-primary);
  font-size: 12px;
  font-weight: 700;
}

.avatar {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 800;
  background: var(--surface-panel);
  border: 1px solid rgba(255, 255, 255, 0.84);
  border-radius: 999px;
  box-shadow: var(--shadow-raised-soft);
}

.shell-body {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  height: calc(100vh - 58px);
  min-height: 0;
  overflow: visible;
}

.side-rail {
  position: relative;
  z-index: 35;
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
  padding: 20px 10px;
  background: var(--surface-soft);
  border-right: 1px solid rgba(255, 255, 255, 0.74);
  overflow: visible;
  box-shadow:
    inset -1px 0 0 rgba(132, 158, 185, 0.1),
    7px 0 18px rgba(105, 132, 160, 0.08);
}

.rail-item {
  position: relative;
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  color: var(--text-secondary);
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--radius-control);
  cursor: pointer;
  transition: color 160ms ease, background 160ms ease, border-color 160ms ease;
}

.rail-item span {
  position: absolute;
  left: 48px;
  z-index: 120;
  width: max-content;
  max-width: 120px;
  padding: 5px 8px;
  color: var(--text-primary);
  font-size: 12px;
  pointer-events: none;
  opacity: 0;
  background: var(--surface-panel-strong);
  border: 1px solid rgba(255, 255, 255, 0.86);
  border-radius: var(--radius-control);
  box-shadow: var(--shadow-raised-soft);
  transform: translateX(-4px);
  transition: opacity 140ms ease, transform 140ms ease;
}

.rail-item:hover span {
  opacity: 1;
  transform: translateX(0);
}

.rail-item:hover,
.rail-item.active {
  color: var(--accent-blue);
  background: rgba(234, 241, 248, 0.86);
  border-color: rgba(255, 255, 255, 0.84);
}

.rail-item:hover {
  box-shadow: var(--shadow-raised-soft);
}

.rail-item.active {
  box-shadow: var(--shadow-pressed);
}

.rail-item.active::before {
  position: absolute;
  left: -10px;
  width: 3px;
  height: 18px;
  content: '';
  background: var(--accent-blue);
  border-radius: 0 3px 3px 0;
}

.workspace {
  position: relative;
  z-index: 1;
  min-width: 0;
  min-height: 0;
  padding: var(--page-gap);
  overflow: auto;
}

@media (max-width: 900px) {
  .top-bar {
    padding: 0 14px;
  }

  .brand-copy span,
  .user-meta {
    display: none;
  }

  .shell-body {
    grid-template-columns: 60px minmax(0, 1fr);
  }

  .side-rail {
    padding-inline: 8px;
  }

  .rail-item {
    width: 38px;
    height: 38px;
  }

  .workspace {
    padding: 14px;
  }
}
</style>
