import axios from 'axios'

// 根据环境变量设置 API 基础 URL
const API_BASE_URL = process.env.NODE_ENV === 'production' 
 ? '/api' // 生产环境使用相对路径，适用于前后端部署在同一域名下
 : 'http://localhost:8123/api' // 开发环境指向本地后端服务

// 创建axios实例
const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 60000
})

// 封装SSE连接
export const connectSSE = (url, params, onMessage, onError) => {
  // 构建带参数的URL
  const queryString = Object.keys(params)
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
    .join('&')
  
  const fullUrl = `${API_BASE_URL}${url}?${queryString}`
  
  // 创建EventSource
  const eventSource = new EventSource(fullUrl)
  
  eventSource.onmessage = event => {
    let data = event.data
    
    // 检查是否是特殊标记
    if (data === '[DONE]') {
      if (onMessage) onMessage('[DONE]')
    } else {
      // 处理普通消息
      if (onMessage) onMessage(data)
    }
  }
  
  eventSource.onerror = error => {
    if (onError) onError(error)
    eventSource.close()
  }
  
  // 返回eventSource实例，以便后续可以关闭连接
  return eventSource
}

// 浙江旅游助手聊天
export const chatWithTourism = (message, chatId) => {
  return connectSSE('/ai/tourism/chat/sse', { message, chatId })
}

// 浙江旅游助手聊天（带工具+RAG）
export const chatWithTourismToolsAndRag = (message, chatId) => {
  return connectSSE('/ai/tourism/chat/with_tools_and_rag/sse', { message, chatId })
}

// AI超级智能体聊天
export const chatWithManus = (message) => {
  return connectSSE('/ai/manus/chat', { message })
}

// MCP服务聊天（调用高德地图等MCP工具）
export const chatWithMcp = (message, chatId) => {
  return connectSSE('/ai/tourism/chat/with_mcp/sse', { message, chatId })
}

// 工具 + RAG + MCP
export const chatWithAll = (message, chatId) => {
  return connectSSE('/ai/tourism/chat/with_all/sse', { message, chatId })
}

export default {
  chatWithTourism,
  chatWithTourismToolsAndRag,
  chatWithManus,
  chatWithMcp,
  chatWithAll
} 