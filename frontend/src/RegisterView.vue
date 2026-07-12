<template>
  <main class="auth-page">
    <section class="auth-card">
      <div class="form-panel">
        <ThinStarLogo />

        <div class="copy-block">
          <p class="kicker">JavaWeb Troubleshoot KG LLM</p>
          <h1>创建账户</h1>
          <p>创建个人工作空间，开始记录、分析与沉淀排障经验。</p>
        </div>

        <form class="minimal-form" @submit.prevent="$emit('back-to-sign-in')">
          <label class="line-field">
            <span>用户名</span>
            <input value="Demo" />
          </label>
          <label class="line-field">
            <span>显示名称</span>
            <input value="Operator" />
          </label>
          <label class="line-field">
            <span>邮箱</span>
            <input value="demo@troubleshoot.local" />
          </label>
          <label class="line-field">
            <span>密码</span>
            <input type="password" value="123456" />
          </label>

          <p class="terms">
            本页用于课程演示，请使用系统内置账号完成登录。
          </p>

          <div class="auth-actions">
            <button class="submit-button" type="submit">完成注册</button>
            <button class="secondary-button" type="button" @click="$emit('back-to-sign-in')">
              返回登录
            </button>
          </div>
        </form>
      </div>

      <div class="hero-panel" aria-hidden="true"></div>

      <footer class="footer-left">
        <div class="pager" aria-label="轮播进度">
          <span></span>
          <span class="active"></span>
          <span></span>
          <span></span>
        </div>
        <div class="socials" aria-label="演示账户">
          <button type="button" @click="$emit('back-to-sign-in')">AI</button>
          <button type="button" @click="$emit('back-to-sign-in')">KG</button>
          <button type="button" @click="$emit('back-to-sign-in')">DB</button>
          <button type="button" class="compact" @click="$emit('back-to-sign-in')">API</button>
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
import ThinStarLogo from './ThinStarLogo.vue'

defineEmits(['back-to-sign-in'])
</script>

<style scoped>
@import "./auth-view.css";
</style>
