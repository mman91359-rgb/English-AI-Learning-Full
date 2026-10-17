<template>
  <router-view />
  <ToastNotification />
  <AiSidebar />
</template>

<script setup lang="ts">
import ToastNotification from '@/components/ToastNotification.vue'
import AiSidebar from '@/components/AiSidebar.vue'
</script>
