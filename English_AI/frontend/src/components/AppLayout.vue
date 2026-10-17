<template>
  <div class="bg-background text-on-background min-h-screen flex">
    <SideNavBar />
    <main class="flex-1 w-full md:ml-sidebar-width flex flex-col min-h-screen relative">
      <TopAppBar />
      <div class="flex-1 px-gutter py-8 w-full max-w-container-max mx-auto space-y-8 pb-24 md:pb-8">
        <slot />
      </div>
    </main>
    <BottomNavBar />
  </div>
</template>

<script setup lang="ts">
import SideNavBar from './SideNavBar.vue'
import TopAppBar from './TopAppBar.vue'
import BottomNavBar from './BottomNavBar.vue'
</script>
