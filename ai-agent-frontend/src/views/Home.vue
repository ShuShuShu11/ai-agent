<template>
  <div class="home-container">
    <div class="header">
      <h1 class="title">AI超级智能体</h1>
      <p class="subtitle">探索AI的无限可能</p>
    </div>

    <div class="apps-container">
      <div class="app-card" @click="navigateTo('/tourism')">
        <div class="app-icon tourism-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 2L2 7l10 5 10-5-10-5z"/>
            <path d="M2 17l10 5 10-5"/>
            <path d="M2 12l10 5 10-5"/>
          </svg>
        </div>
        <div class="app-info">
          <div class="app-title">浙江旅游助手</div>
          <div class="app-desc">浙江文旅专家，景点美食行程规划全搞定</div>
        </div>
        <div class="app-tags">
          <span class="tag">景点推荐</span>
          <span class="tag">美食攻略</span>
          <span class="tag">行程规划</span>
        </div>
        <div class="app-button">
          <span>立即体验</span>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M5 12h14M12 5l7 7-7 7"/>
          </svg>
        </div>
      </div>

      <div class="app-card" @click="navigateTo('/super-agent')">
        <div class="app-icon robot-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="11" width="18" height="10" rx="2"/>
            <circle cx="12" cy="5" r="3"/>
            <path d="M12 8v3M8 16l-1 2M16 16l1 2M8 14h8"/>
          </svg>
        </div>
        <div class="app-info">
          <div class="app-title">AI超级智能体</div>
          <div class="app-desc">全能型AI助手，解决各类专业问题</div>
        </div>
        <div class="app-tags">
          <span class="tag">智能问答</span>
          <span class="tag">内容创作</span>
          <span class="tag">问题解答</span>
        </div>
        <div class="app-button">
          <span>立即体验</span>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M5 12h14M12 5l7 7-7 7"/>
          </svg>
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
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  flex: 1;
}

.app-card {
  width: 320px;
  background: #fff;
  border-radius: 20px;
  padding: 32px;
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
  width: 64px;
  height: 64px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.app-icon svg {
  width: 32px;
  height: 32px;
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
  margin-bottom: 24px;
}

.app-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 8px;
}

.app-desc {
  font-size: 0.9rem;
  color: #888;
  margin: 0;
  line-height: 1.5;
}

.app-button {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #666;
  font-size: 0.9rem;
  transition: color 0.2s;
}

.app-button svg {
  width: 16px;
  height: 16px;
  transition: transform 0.2s;
}

.app-card:hover .app-button {
  color: #333;
}

.app-card:hover .app-button svg {
  transform: translateX(3px);
}

.app-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 20px;
  justify-content: center;
}

.tag {
  padding: 4px 12px;
  background: #f1f5f9;
  border-radius: 20px;
  font-size: 12px;
  color: #666;
}

@media (max-width: 768px) {
  .title {
    font-size: 2rem;
  }

  .app-card {
    width: 100%;
    max-width: 380px;
  }
}
</style>