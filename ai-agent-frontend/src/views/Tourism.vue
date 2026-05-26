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
      <button
        :class="['mode-btn', { active: currentMode === 'basic' }]"
        @click="currentMode = 'basic'"
      >
        基础模式
      </button>
      <button
        :class="['mode-btn', { active: currentMode === 'tools+rag' }]"
        @click="currentMode = 'tools+rag'"
      >
        工具+RAG
      </button>
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
import { chatWithTourism, chatWithTourismToolsAndRag } from '../api'

const router = useRouter()
const messages = ref([])
const chatId = ref('')
const connectionStatus = ref('disconnected')
const currentMode = ref('tools+rag')
let eventSource = null

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

  if (currentMode.value === 'tools+rag') {
    eventSource = chatWithTourismToolsAndRag(message, chatId.value)
  } else {
    eventSource = chatWithTourism(message, chatId.value)
  }

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
  addMessage('您好！我是浙江文旅专家，很高兴为您服务。请告诉我您想了解浙江的哪些旅游信息，我可以为您介绍景点、美食、住宿、交通和行程规划。', false)
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
  min-height: 100vh;
  background: #f8fafc;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: #fff;
  border-bottom: 1px solid #eee;
  position: sticky;
  top: 0;
  z-index: 10;
}

.back-button {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: color 0.2s;
}

.back-button:hover {
  color: #333;
}

.back-button svg {
  width: 18px;
  height: 18px;
}

.title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0;
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
}

.chat-id {
  font-size: 12px;
  color: #999;
}

.mode-selector {
  display: flex;
  gap: 8px;
  padding: 12px 20px;
  background: #fff;
  border-bottom: 1px solid #eee;
}

.mode-btn {
  padding: 6px 16px;
  border: 1px solid #ddd;
  border-radius: 20px;
  background: #fff;
  color: #666;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.mode-btn:hover {
  border-color: #10b981;
  color: #10b981;
}

.mode-btn.active {
  background: linear-gradient(135deg, #10b981, #34d399);
  border-color: #10b981;
  color: #fff;
}

.chat-area {
  flex: 1;
  padding: 20px;
  display: flex;
  flex-direction: column;
}

.footer-container {
  margin-top: auto;
}

@media (max-width: 768px) {
  .header {
    padding: 12px 16px;
  }

  .chat-area {
    padding: 12px;
  }
}
</style>