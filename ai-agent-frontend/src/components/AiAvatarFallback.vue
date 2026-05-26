<template>
  <div class="ai-avatar-fallback" :class="type">
    <svg v-if="type === 'love'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <path d="M12 21C12 21 3 13.5 3 8.5C3 5.42 5.42 3 8.5 3C10.24 3 11.91 3.81 13 5.08C14.09 3.81 15.76 3 17.5 3C20.58 3 23 5.42 23 8.5C23 13.5 12 21 12 21Z"/>
    </svg>
    <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <rect x="3" y="11" width="18" height="10" rx="2"/>
      <circle cx="12" cy="5" r="3"/>
      <path d="M12 8v3M8 16l-1 2M16 16l1 2M8 14h8"/>
    </svg>
  </div>
</template>

<script setup>
defineProps({
  type: {
    type: String,
    default: 'default'
  }
})
</script>

<style scoped>
.ai-avatar-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.ai-avatar-fallback svg {
  width: 20px;
  height: 20px;
  color: #fff;
}

.love {
  background: linear-gradient(135deg, #ff6b8b, #ff8a9b);
}

.default, .super {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
}
</style>