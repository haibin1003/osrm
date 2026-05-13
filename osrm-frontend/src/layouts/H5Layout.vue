<template>
  <div class="h5-layout">
    <header class="h5-header">
      <div class="h5-header-title">OSRM 存量盘点</div>
    </header>
    <main class="h5-main">
      <router-view />
    </main>
  </div>
</template>

<style scoped lang="scss">
.h5-layout {
  min-height: 100vh;
  background: #f5f7fa;
}

.h5-header {
  height: 48px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: center;
  position: sticky;
  top: 0;
  z-index: 100;
}

.h5-header-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.h5-main {
  padding: 16px;
  max-width: 640px;
  margin: 0 auto;
}
</style>
