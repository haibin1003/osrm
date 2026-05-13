<template>
  <div class="expired-wrapper">
    <div class="expired-card">
      <div class="expired-icon">!</div>
      <h1 class="expired-title">访问链接已失效</h1>
      <p class="expired-desc">
        该 H5 登记链接已过期或被作废，请联系管理员重新获取最新链接。
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped lang="scss">
.expired-wrapper {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;
}

.expired-card {
  text-align: center;
  background: #fff;
  border-radius: 12px;
  padding: 48px 40px;
  max-width: 420px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
}

.expired-icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #FEF0F0;
  color: #F56C6C;
  font-size: 28px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
}

.expired-title {
  font-size: 20px;
  color: #303133;
  margin: 0 0 12px;
}

.expired-desc {
  font-size: 14px;
  color: #909399;
  line-height: 1.6;
  margin: 0;
}
</style>
