<script setup lang="ts">
import { RouterLink, RouterView, useRoute } from 'vue-router';

const route = useRoute();
</script>

<template>
  <div class="app-shell">
    <header class="app-bar">
      <div class="app-bar__brand">
        <span class="app-bar__logo">E</span>
        <div>
          <p class="app-bar__title">员工信息管理</p>
          <p class="app-bar__caption">Employee Workspace</p>
        </div>
      </div>
      <nav class="app-bar__nav">
        <RouterLink to="/employees" class="app-bar__link" :class="{ 'is-active': route.path.startsWith('/employees') }">
          员工列表
        </RouterLink>
      </nav>
    </header>
    <main class="app-main">
      <RouterView />
    </main>
  </div>
</template>
