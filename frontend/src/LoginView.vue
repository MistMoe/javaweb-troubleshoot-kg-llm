<template>
  <main class="auth-page">
    <section class="auth-card">
      <div class="form-panel">
        <ThinStarLogo />

        <div class="copy-block">
          <p class="kicker">JavaWeb Troubleshoot KG LLM</p>
          <h1>欢迎回来</h1>
          <p>登录后继续使用智能排障与知识图谱工作台。</p>
        </div>

        <el-alert
          v-if="loginError"
          :title="loginError"
          type="error"
          show-icon
          :closable="false"
          class="auth-alert"
        />

        <form class="minimal-form" @submit.prevent="$emit('login')">
          <label class="line-field">
            <span>用户名</span>
            <input v-model="loginForm.username" autocomplete="username" placeholder="admin" />
          </label>
          <label class="line-field">
            <span>显示名称</span>
            <input readonly value="Operator" />
          </label>
          <label class="line-field">
            <span>邮箱</span>
            <input readonly value="demo@troubleshoot.local" />
          </label>
          <label class="line-field password-field">
            <span>密码</span>
            <input
              v-model="loginForm.password"
              :type="passwordVisible ? 'text' : 'password'"
              autocomplete="current-password"
              placeholder="123456"
            />
            <button type="button" class="eye-button" @click="passwordVisible = !passwordVisible">
              <el-icon><View /></el-icon>
            </button>
          </label>

          <p class="terms">
            登录即代表进入 JavaWeb 智能排障工作空间。
          </p>

          <div class="auth-actions">
            <button class="submit-button" type="submit" :disabled="loginLoading">
              {{ loginLoading ? '登录中' : '登录系统' }}
            </button>
            <button class="secondary-button" type="button" @click="$emit('create-account')">
              创建账户
            </button>
          </div>
        </form>
      </div>

      <div class="hero-panel" aria-hidden="true"></div>

      <footer class="footer-left">
        <div class="pager" aria-label="轮播进度">
          <span></span>
          <span></span>
          <span class="active"></span>
          <span></span>
        </div>
        <div class="socials" aria-label="工作区能力">
          <button type="button" @click="$emit('fill-demo', 'admin')">AI</button>
          <button type="button" @click="$emit('fill-demo', 'maintainer')">KG</button>
          <button type="button" @click="$emit('fill-demo', 'user01')">DB</button>
          <button type="button" class="compact" @click="$emit('login')">API</button>
        </div>
      </footer>

      <footer class="footer-right">
        <span>进入智能排障工作空间</span>
        <span class="arrow">-></span>
      </footer>
    </section>
  </main>
</template>

<script setup>
import { View } from '@element-plus/icons-vue'
import { ref } from 'vue'
import ThinStarLogo from './ThinStarLogo.vue'

defineProps({
  loginForm: {
    type: Object,
    required: true
  },
  loginLoading: {
    type: Boolean,
    default: false
  },
  loginError: {
    type: String,
    default: ''
  }
})

defineEmits(['login', 'fill-demo', 'create-account'])

const passwordVisible = ref(false)
</script>

<style scoped>
@import "./auth-view.css";
</style>
