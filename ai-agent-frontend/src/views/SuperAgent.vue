<template>
  <div class="super-agent-container">
    <div class="header">
      <div class="back-button" @click="goBack">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M19 12H5M12 19l-7-7 7-7"/>
        </svg>
        返回
      </div>
      <h1 class="title">AI超级智能体</h1>
      <div class="placeholder"></div>
    </div>

    <div class="chat-area">
      <ChatRoom
        :messages="messages"
        :connection-status="connectionStatus"
        ai-type="super"
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
import { chatWithManus } from '../api'

const router = useRouter()
const messages = ref([])
const connectionStatus = ref('disconnected')
let eventSource = null

const addMessage = (content, isUser, type = '') => {
  messages.value.push({
    content,
    isUser,
    type,
    time: new Date().getTime()
  })
}

const sendMessage = (message) => {
  addMessage(message, true, 'user-question')

  if (eventSource) {
    eventSource.close()
  }

  connectionStatus.value = 'connecting'

  let messageBuffer = []
  let lastBubbleTime = Date.now()
  let isFirstResponse = true

  const chineseEndPunctuation = ['。', '！', '？', '…']
  const minBubbleInterval = 800

  const createBubble = (content, type = 'ai-answer') => {
    if (!content.trim()) return

    const now = Date.now()
    const timeSinceLastBubble = now - lastBubbleTime

    if (isFirstResponse) {
      addMessage(content, false, type)
      isFirstResponse = false
    } else if (timeSinceLastBubble < minBubbleInterval) {
      setTimeout(() => {
        addMessage(content, false, type)
      }, minBubbleInterval - timeSinceLastBubble)
    } else {
      addMessage(content, false, type)
    }

    lastBubbleTime = now
    messageBuffer = []
  }

  eventSource = chatWithManus(message)

  eventSource.onmessage = (event) => {
    const data = event.data

    if (data && data !== '[DONE]') {
      messageBuffer.push(data)

      const combinedText = messageBuffer.join('')
      const lastChar = data.charAt(data.length - 1)
      const hasCompleteSentence = chineseEndPunctuation.includes(lastChar) || data.includes('\n\n')
      const isLongEnough = combinedText.length > 40

      if (hasCompleteSentence || isLongEnough) {
        createBubble(combinedText)
      }
    }

    if (data === '[DONE]') {
      if (messageBuffer.length > 0) {
        const remainingContent = messageBuffer.join('')
        createBubble(remainingContent, 'ai-final')
      }

      connectionStatus.value = 'disconnected'
      eventSource.close()
    }
  }

  eventSource.onerror = (error) => {
    console.error('SSE Error:', error)
    connectionStatus.value = 'error'
    eventSource.close()

    if (messageBuffer.length > 0) {
      const remainingContent = messageBuffer.join('')
      createBubble(remainingContent, 'ai-error')
    }
  }
}

const goBack = () => {
  router.push('/')
}

onMounted(() => {
  addMessage('你好，我是AI超级智能体。我可以解答各类问题，提供专业建议，请问有什么可以帮助你的吗？', false)
})

onBeforeUnmount(() => {
  if (eventSource) {
    eventSource.close()
  }
})
</script>

<style scoped>
.super-agent-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: #f8fafc;
}

.header {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
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
  justify-self: start;
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
  text-align: center;
}

.placeholder {
  width: 1px;
  justify-self: end;
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