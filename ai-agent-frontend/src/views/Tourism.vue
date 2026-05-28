<template>
  <div class="tourism-container">
    <div class="header">
      <div class="back-button" @click="goBack">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M19 12H5M12 19l-7-7 7-7"/>
        </svg>
        返回
      </div>
      <h1 class="title">浙江旅游助手</h1>
      <div class="chat-id">{{ chatId }}</div>
    </div>

    <div class="mode-selector">
      <div class="mode-tabs">
        <button
          v-for="mode in modes"
          :key="mode.value"
          :class="['mode-tab', { active: currentMode === mode.value }]"
          @click="currentMode = mode.value"
        >
          <span class="mode-icon" v-html="mode.icon"></span>
          <span class="mode-label">{{ mode.label }}</span>
          <span class="mode-desc">{{ mode.desc }}</span>
        </button>
      </div>
    </div>

    <div class="chat-area">
      <ChatRoom
        :messages="messages"
        :connection-status="connectionStatus"
        ai-type="tourism"
        @send-message="sendMessage"
      />
    </div>

    <div class="footer-container">
      <AppFooter />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import ChatRoom from '../components/ChatRoom.vue'
import AppFooter from '../components/AppFooter.vue'
import { chatWithTourism, chatWithTourismToolsAndRag, chatWithMcp, chatWithAll } from '../api'

const router = useRouter()
const messages = ref([])
const chatId = ref('')
const connectionStatus = ref('disconnected')
const currentMode = ref('all')
let eventSource = null

const modes = [
  {
    value: 'basic',
    label: '基础对话',
    desc: '纯 AI 问答',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5"/><path d="M2 12l10 5 10-5"/></svg>'
  },
  {
    value: 'tools+rag',
    label: '知识增强',
    desc: '工具+知识库',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>'
  },
  {
    value: 'mcp',
    label: 'MCP 地图',
    desc: '高德地图工具',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>'
  },
  {
    value: 'all',
    label: '全能模式',
    desc: '工具+RAG+MCP',
    icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>'
  }
]

const modeMap = {
  basic: chatWithTourism,
  'tools+rag': chatWithTourismToolsAndRag,
  mcp: chatWithMcp,
  all: chatWithAll
}

const generateChatId = () => {
  return Math.random().toString(36).substring(2, 10)
}

const addMessage = (content, isUser) => {
  messages.value.push({
    content,
    isUser,
    time: new Date().getTime()
  })
}

const sendMessage = (message) => {
  addMessage(message, true)

  if (eventSource) {
    eventSource.close()
  }

  const aiMessageIndex = messages.value.length
  addMessage('', false)

  connectionStatus.value = 'connecting'

  eventSource = modeMap[currentMode.value](message, chatId.value)

  eventSource.onmessage = (event) => {
    const data = event.data
    if (data && data !== '[DONE]') {
      if (aiMessageIndex < messages.value.length) {
        messages.value[aiMessageIndex].content += data
      }
    }

    if (data === '[DONE]') {
      connectionStatus.value = 'disconnected'
      eventSource.close()
    }
  }

  eventSource.onerror = (error) => {
    console.error('SSE Error:', error)
    connectionStatus.value = 'error'
    eventSource.close()
  }
}

const goBack = () => {
  router.push('/')
}

onMounted(() => {
  chatId.value = generateChatId()
  addMessage('嗨！我是浙江旅游助手 呼呼～有什么关于浙江旅游的问题尽管问我吧！', false)
})

onBeforeUnmount(() => {
  if (eventSource) {
    eventSource.close()
  }
})
</script>

<style scoped>
.tourism-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f8fafc;
  overflow: hidden;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: #fff;
  border-bottom: 1px solid #eee;
  position: sticky;
  top: 0;
  z-index: 10;
}

.back-button {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  transition: color 0.2s;
}

.back-button:hover {
  color: #333;
}

.back-button svg {
  width: 16px;
  height: 16px;
}

.title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0;
}

.chat-id {
  font-size: 11px;
  color: #999;
  background: #f5f5f5;
  padding: 2px 8px;
  border-radius: 10px;
}

.mode-selector {
  padding: 16px 20px;
  background: #fff;
  border-bottom: 1px solid #eee;
}

.mode-tabs {
  display: flex;
  gap: 12px;
}

.mode-tab {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  transition: all 0.2s;
}

.mode-tab:hover {
  border-color: #10b981;
  background: #f0fdf4;
}

.mode-tab.active {
  border-color: #10b981;
  background: linear-gradient(135deg, #10b981, #34d399);
  color: #fff;
}

.mode-icon {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.mode-icon :deep(svg) {
  width: 20px;
  height: 20px;
}

.mode-tab.active .mode-icon :deep(svg) {
  stroke: #fff;
}

.mode-label {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a2e;
}

.mode-desc {
  font-size: 11px;
  color: #888;
}

.mode-tab.active .mode-label {
  color: #fff;
}

.mode-tab.active .mode-desc {
  color: rgba(255, 255, 255, 0.8);
}

.chat-area {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.footer-container {
  margin-top: auto;
}

@media (max-width: 768px) {
  .header {
    padding: 12px 16px;
  }

  .mode-selector {
    flex-wrap: wrap;
  }

  .chat-area {
    padding: 12px;
  }
}
</style>