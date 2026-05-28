import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: {
      title: '首页 - 呼呼AI智能体应用平台',
      description: '呼呼AI智能体应用平台提供浙江旅游助手和AI超级智能体服务，满足您的各种AI对话需求'
    }
  },
  {
    path: '/tourism',
    name: 'Tourism',
    component: () => import('../views/Tourism.vue'),
    meta: {
      title: '浙江旅游助手 - 呼呼AI智能体应用平台',
      description: '浙江旅游助手是呼呼AI智能体应用平台的文旅专家，为您提供浙江景点、美食、住宿、交通和行程规划咨询'
    }
  },
  {
    path: '/super-agent',
    name: 'SuperAgent',
    component: () => import('../views/SuperAgent.vue'),
    meta: {
      title: 'AI超级智能体 - 呼呼AI智能体应用平台',
      description: 'AI超级智能体是呼呼AI智能体应用平台的全能助手，能解答各类专业问题，提供精准建议和解决方案'
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局导航守卫，设置文档标题
router.beforeEach((to, from, next) => {
  // 设置页面标题
  if (to.meta.title) {
    document.title = to.meta.title
  }
  next()
})

export default router 