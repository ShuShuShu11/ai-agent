<template>
  <div class="home-container">
    <div class="header">
      <h1 class="title">呼呼 AI 智能体</h1>
      <p class="subtitle">探索 AI 的无限可能</p>
    </div>

    <div class="apps-container">
      <!-- 浙江旅游助手 -->
      <div class="app-section">
        <div class="section-title">浙江旅游助手</div>
        <div class="app-card" @click="navigateTo('/tourism')">
          <div class="app-icon tourism-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 2L2 7l10 5 10-5-10-5z"/>
              <path d="M2 17l10 5 10-5"/>
              <path d="M2 12l10 5 10-5"/>
            </svg>
          </div>
          <div class="app-info">
            <div class="app-title">呼呼</div>
            <div class="app-desc">浙江文旅专家，景点美食行程规划全搞定</div>
          </div>
          <div class="app-tags">
            <span class="tag">景点推荐</span>
            <span class="tag">美食攻略</span>
            <span class="tag">行程规划</span>
          </div>
          <div class="mode-preview">
            <span class="mode-tag">基础</span>
            <span class="mode-tag">+工具</span>
            <span class="mode-tag">+RAG</span>
            <span class="mode-tag">+MCP</span>
          </div>
        </div>
      </div>

      <!-- AI 超级智能体 -->
      <div class="app-section">
        <div class="section-title">AI 超级智能体</div>
        <div class="app-card" @click="navigateTo('/super-agent')">
          <div class="app-icon robot-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="11" width="18" height="10" rx="2"/>
              <circle cx="12" cy="5" r="3"/>
              <path d="M12 8v3M8 16l-1 2M16 16l1 2M8 14h8"/>
            </svg>
          </div>
          <div class="app-info">
            <div class="app-title">Manus</div>
            <div class="app-desc">全能型 AI 助手，自主规划并调用工具解决复杂任务</div>
          </div>
          <div class="app-tags">
            <span class="tag">智能问答</span>
            <span class="tag">内容创作</span>
            <span class="tag">问题解答</span>
          </div>
        </div>
      </div>
    </div>

    <AppFooter />
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import AppFooter from '../components/AppFooter.vue'

const router = useRouter()

const navigateTo = (path) => {
  router.push(path)
}
</script>

<style scoped>
.home-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}

.header {
  padding: 80px 20px 60px;
  text-align: center;
}

.title {
  font-size: 2.5rem;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 12px;
  letter-spacing: -0.02em;
}

.subtitle {
  font-size: 1.1rem;
  color: #666;
  margin: 0;
  letter-spacing: 0.05em;
}

.apps-container {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 32px;
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
  flex: 1;
}

.app-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-title {
  font-size: 14px;
  color: #888;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  text-align: center;
}

.app-card {
  width: 320px;
  background: #fff;
  border-radius: 20px;
  padding: 28px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  align-items: center;
  border: 1px solid #eee;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.app-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.08);
  border-color: #ddd;
}

.app-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}

.app-icon svg {
  width: 28px;
  height: 28px;
  color: #fff;
}

.tourism-icon {
  background: linear-gradient(135deg, #10b981, #34d399);
}

.robot-icon {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
}

.app-info {
  text-align: center;
  margin-bottom: 16px;
}

.app-title {
  font-size: 1.2rem;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 6px;
}

.app-desc {
  font-size: 0.85rem;
  color: #888;
  margin: 0;
  line-height: 1.5;
}

.app-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
  justify-content: center;
}

.tag {
  padding: 3px 10px;
  background: #f1f5f9;
  border-radius: 20px;
  font-size: 11px;
  color: #666;
}

.mode-preview {
  display: flex;
  gap: 6px;
}

.mode-tag {
  padding: 4px 10px;
  background: linear-gradient(135deg, #f0fdf4, #d1fae5);
  border: 1px solid #10b981;
  border-radius: 12px;
  font-size: 11px;
  color: #10b981;
}

@media (max-width: 768px) {
  .title {
    font-size: 2rem;
  }

  .app-card {
    width: 100%;
    max-width: 340px;
  }
}
</style>