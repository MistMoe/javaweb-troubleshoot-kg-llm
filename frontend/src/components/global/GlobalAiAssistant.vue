<template>
  <div class="global-ai-assistant" :class="{ open: assistantOpen }">
    <section v-if="assistantOpen" class="global-ai-window" aria-label="AI assistant panel">
      <header class="global-ai-header">
        <div class="assistant-title">
          <span class="assistant-mark" aria-hidden="true">
            <Sparkles :size="18" :stroke-width="1.45" />
          </span>
          <div>
            <h2>AI 助手</h2>
            <p>智能排障与知识图谱助手</p>
          </div>
        </div>
        <div class="assistant-actions">
          <button type="button" aria-label="Minimize AI assistant" @click="assistantOpen = false">
            <Minus :size="16" :stroke-width="1.5" />
          </button>
          <button type="button" aria-label="Close AI assistant" @click="assistantOpen = false">
            <X :size="16" :stroke-width="1.5" />
          </button>
        </div>
      </header>

      <div ref="bodyRef" class="global-ai-body">
        <div
          v-for="message in messages"
          :key="message.id"
          class="assistant-message"
          :class="`assistant-message-${message.role}`"
        >
          <span>{{ message.label }}</span>
          <p>{{ message.text }}</p>
        </div>

        <div v-if="!hasUserMessage" class="assistant-empty-hint">
          <Bot :size="22" :stroke-width="1.45" />
          <strong>等待你的问题</strong>
          <p>当前会请求后端助手接口；DeepSeek 启用后将返回真实模型回复。</p>
        </div>
      </div>

      <div class="assistant-prompts" aria-label="Quick questions">
        <button v-for="item in assistantPrompts" :key="item" type="button" :disabled="assistantBusy" @click="usePrompt(item)">
          {{ item }}
        </button>
      </div>

      <label class="global-ai-input">
        <input
          v-model="inputText"
          placeholder="请输入报错信息或你的问题..."
          :disabled="assistantBusy"
          @keydown.enter.prevent="sendMessage"
        />
        <button type="button" aria-label="Send message" :disabled="assistantBusy || !inputText.trim()" @click="sendMessage">
          <Send :size="17" :stroke-width="1.5" />
        </button>
      </label>
    </section>

    <button class="global-ai-trigger" type="button" :aria-expanded="assistantOpen" @click="assistantOpen = !assistantOpen">
      <span class="trigger-icon" aria-hidden="true">
        <Sparkles :size="20" :stroke-width="1.45" />
      </span>
      <span class="trigger-copy">AI 助手</span>
      <i aria-hidden="true"></i>
    </button>
  </div>
</template>

<script setup>
import { Bot, Minus, Send, Sparkles, X } from 'lucide-vue-next'
import { computed, nextTick, ref } from 'vue'

const assistantOpen = ref(false)
const assistantBusy = ref(false)
const bodyRef = ref(null)
const inputText = ref('')
let messageId = 0

const assistantPrompts = [
  '分析 Spring Boot 启动失败',
  '解释图谱关系',
  '查看最近排障记录',
  '生成排障报告'
]

const messages = ref([
  createMessage('assistant', 'AI', '我可以帮你分析报错、解释知识图谱关系、生成排障报告。'),
  createMessage('assistant', 'KG', '你可以把 Java Web 报错日志、异常类型或排查目标发给我，我会先按知识图谱线索组织分析。')
])

const hasUserMessage = computed(() => messages.value.some(message => message.role === 'user'))

function createMessage(role, label, text) {
  messageId += 1
  return { id: messageId, role, label, text }
}

function usePrompt(prompt) {
  inputText.value = prompt
  sendMessage()
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || assistantBusy.value) return
  messages.value.push(createMessage('user', '你', text))
  inputText.value = ''
  assistantBusy.value = true
  const assistantMessage = createMessage('assistant', 'AI', '正在请求后端 AI 助手...')
  messages.value.push(assistantMessage)
  await scrollToBottom()

  try {
    const response = await fetch('/api/assistant/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ message: text })
    })
    const data = await response.json().catch(() => ({}))
    if (!response.ok) {
      throw new Error(data.message || 'AI 助手接口调用失败')
    }
    assistantMessage.text = data.reply || buildAssistantReply(text)
  } catch (error) {
    assistantMessage.text = `${buildAssistantReply(text)}\n\n后端 AI 助手暂时不可用：${error.message}`
  } finally {
    assistantBusy.value = false
    await scrollToBottom()
  }
}

function buildAssistantReply(text) {
  const normalized = text.toLowerCase()
  if (text.includes('Spring Boot') || normalized.includes('spring')) {
    return '可以先按这条链路排查：确认启动日志中的异常类型，检查端口占用、application.yml 配置、依赖冲突，再到“智能排障”页提交完整日志，让系统匹配知识库并生成图谱线索。'
  }
  if (text.includes('图谱') || normalized.includes('kg') || normalized.includes('neo4j')) {
    return '知识图谱页会把故障、发生环节、组件、原因、方案、文件和命令连起来。你可以先从 Error 节点看 MAY_CAUSED_BY 和 SOLVED_BY 两类关系，通常能快速找到排查方向。'
  }
  if (text.includes('最近') || text.includes('历史') || normalized.includes('history')) {
    return '最近排障记录在“分析历史”页。管理员和维护员可以看全部记录，普通用户看自己的记录；点开记录后能查看原始输入、AI 建议和图谱快照。'
  }
  if (text.includes('报告') || normalized.includes('report')) {
    return '排障报告建议包含四段：报错现象、命中案例、原因链路、处理步骤。当前助手先给静态建议，后续接入真实 LLM 后可以自动生成完整报告文本。'
  }
  return '收到。我建议先把报错现象、技术栈、关键日志和你已经尝试过的操作整理出来，再进入“智能排障”页提交，系统会结合 MySQL 知识库和 Neo4j 图谱给出排查路径。'
}

async function scrollToBottom() {
  await nextTick()
  if (bodyRef.value) {
    bodyRef.value.scrollTop = bodyRef.value.scrollHeight
  }
}
</script>

<style scoped>
.global-ai-assistant {
  position: fixed;
  right: 48px;
  bottom: 56px;
  z-index: 3000;
  display: grid;
  gap: 14px;
  justify-items: end;
  pointer-events: none;
}

.global-ai-window,
.global-ai-trigger {
  pointer-events: auto;
}

.global-ai-trigger {
  display: inline-flex;
  gap: 12px;
  align-items: center;
  justify-content: center;
  min-width: 172px;
  height: 58px;
  padding: 0 20px;
  color: #f7fbff;
  font: inherit;
  font-weight: 800;
  background: #6697df;
  border: 1px solid rgba(179, 213, 255, 0.88);
  border-radius: 25px;
  box-shadow:
    -8px -8px 18px rgba(255, 255, 255, 0.66),
    12px 18px 32px rgba(75, 108, 154, 0.34),
    0 0 0 6px rgba(126, 177, 235, 0.11),
    inset 1px 1px 0 rgba(255, 255, 255, 0.36),
    inset -1px -1px 0 rgba(33, 76, 142, 0.16);
  cursor: pointer;
  text-shadow: 0 1px 1px rgba(42, 82, 137, 0.22);
  transition: transform 160ms ease, border-color 160ms ease, box-shadow 160ms ease, background 160ms ease;
}

.global-ai-trigger:hover,
.global-ai-assistant.open .global-ai-trigger {
  background: #5b8fe0;
  border-color: rgba(217, 236, 255, 0.96);
  box-shadow:
    -9px -9px 20px rgba(255, 255, 255, 0.7),
    14px 20px 36px rgba(70, 103, 148, 0.38),
    0 0 0 7px rgba(126, 177, 235, 0.15),
    inset 1px 1px 0 rgba(255, 255, 255, 0.44),
    inset -1px -1px 0 rgba(33, 76, 142, 0.14);
  transform: translateY(-1px);
}

.trigger-icon {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  color: #eaf4ff;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(232, 244, 255, 0.42);
  border-radius: 14px;
}

.trigger-copy {
  white-space: nowrap;
}

.global-ai-trigger i {
  width: 8px;
  height: 8px;
  background: #d7f0ff;
  border: 1px solid rgba(255, 255, 255, 0.9);
  border-radius: 999px;
  box-shadow:
    0 0 0 4px rgba(220, 242, 255, 0.18),
    0 0 14px rgba(226, 246, 255, 0.62);
}

.global-ai-window {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto auto;
  width: min(460px, calc(100vw - 36px));
  height: min(620px, calc(100vh - 124px));
  overflow: hidden;
  color: #1a2638;
  background: rgba(250, 253, 255, 0.98);
  border: 1px solid rgba(255, 255, 255, 0.92);
  border-radius: 14px;
  box-shadow:
    -16px -16px 32px rgba(255, 255, 255, 0.78),
    18px 24px 48px rgba(86, 112, 144, 0.28),
    inset 1px 1px 0 rgba(255, 255, 255, 0.92),
    inset -1px -1px 0 rgba(145, 172, 204, 0.1);
}

.global-ai-header {
  display: flex;
  gap: 18px;
  align-items: flex-start;
  justify-content: space-between;
  padding: 20px 20px 16px;
  border-bottom: 1px solid rgba(210, 224, 240, 0.62);
}

.assistant-title {
  display: flex;
  gap: 12px;
  align-items: center;
  min-width: 0;
}

.assistant-mark {
  display: grid;
  width: 42px;
  height: 42px;
  flex: 0 0 auto;
  place-items: center;
  color: #5f7db8;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(201, 222, 247, 0.82);
  border-radius: 14px;
}

.assistant-title h2,
.assistant-title p {
  margin: 0;
}

.assistant-title h2 {
  color: #172235;
  font-size: 20px;
}

.assistant-title p {
  margin-top: 4px;
  color: #8391a5;
  font-size: 12px;
}

.assistant-actions {
  display: flex;
  gap: 8px;
}

.assistant-actions button {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  color: #7d8da3;
  background: rgba(255, 255, 255, 0.62);
  border: 1px solid rgba(206, 221, 238, 0.74);
  border-radius: 9px;
  cursor: pointer;
}

.assistant-actions button:hover {
  color: #315fbd;
  border-color: rgba(178, 207, 244, 0.86);
}

.global-ai-body {
  display: grid;
  align-content: start;
  gap: 12px;
  min-height: 0;
  padding: 18px 20px;
  overflow: auto;
}

.assistant-message {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 10px;
  align-items: start;
}

.assistant-message span {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  color: #5d76a7;
  font-size: 11px;
  font-weight: 800;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(207, 224, 243, 0.78);
  border-radius: 12px;
}

.assistant-message p {
  margin: 0;
  padding: 12px 13px;
  color: #64748a;
  font-size: 13px;
  line-height: 1.75;
  white-space: pre-wrap;
  background: rgba(255, 255, 255, 0.52);
  border: 1px solid rgba(213, 226, 241, 0.74);
  border-radius: 12px;
  box-shadow:
    inset 1px 1px 3px rgba(158, 177, 199, 0.08),
    inset -2px -2px 5px rgba(255, 255, 255, 0.86);
}

.assistant-message-user {
  grid-template-columns: minmax(0, 1fr) 34px;
}

.assistant-message-user span {
  grid-column: 2;
  grid-row: 1;
  color: #fff;
  background: #6b97df;
  border-color: rgba(116, 162, 230, 0.82);
}

.assistant-message-user p {
  grid-column: 1;
  grid-row: 1;
  color: #395273;
  background: rgba(232, 243, 255, 0.92);
  border-color: rgba(151, 190, 241, 0.62);
}

.assistant-empty-hint {
  display: grid;
  justify-items: center;
  gap: 8px;
  margin-top: 8px;
  padding: 24px 18px;
  color: #8795a8;
  text-align: center;
  background: rgba(247, 251, 254, 0.64);
  border: 1px dashed rgba(188, 210, 236, 0.78);
  border-radius: 12px;
}

.assistant-empty-hint strong {
  color: #50637c;
}

.assistant-empty-hint p {
  max-width: 280px;
  margin: 0;
  font-size: 12px;
  line-height: 1.7;
}

.assistant-prompts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  padding: 0 20px 16px;
}

.assistant-prompts button {
  min-height: 42px;
  padding: 10px 12px;
  color: #60748f;
  font: inherit;
  font-size: 12px;
  text-align: left;
  background: rgba(255, 255, 255, 0.58);
  border: 1px solid rgba(211, 225, 241, 0.76);
  border-radius: 10px;
  cursor: pointer;
}

.assistant-prompts button:hover {
  color: #315fbd;
  border-color: rgba(175, 207, 245, 0.9);
}

.global-ai-input {
  display: flex;
  gap: 10px;
  align-items: center;
  margin: 0 20px 20px;
  padding: 12px 12px 12px 15px;
  background: rgba(246, 250, 253, 0.78);
  border: 1px solid rgba(207, 222, 238, 0.82);
  border-radius: 13px;
  box-shadow:
    inset 2px 2px 6px rgba(154, 176, 201, 0.12),
    inset -2px -2px 6px rgba(255, 255, 255, 0.88);
}

.global-ai-input input {
  flex: 1;
  min-width: 0;
  color: #26364c;
  font: inherit;
  background: transparent;
  border: 0;
  outline: 0;
}

.global-ai-input input::placeholder {
  color: #a0adbd;
}

.global-ai-input button {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  color: #f8fbff;
  background: #5f83c7;
  border: 1px solid rgba(95, 131, 199, 0.88);
  border-radius: 11px;
  cursor: pointer;
}

.global-ai-input button:disabled {
  cursor: not-allowed;
  opacity: 0.46;
}

@media (max-width: 640px) {
  .global-ai-assistant {
    right: 14px;
    bottom: 14px;
  }

  .global-ai-window {
    width: calc(100vw - 28px);
    height: min(600px, calc(100vh - 96px));
  }

  .assistant-prompts {
    grid-template-columns: 1fr;
  }
}
</style>
