<template>
  <div class="h5-success">
    <div class="success-icon">
      <el-icon :size="48" color="#67c23a"><CircleCheck /></el-icon>
    </div>
    <div class="success-title">提交成功</div>
    <div class="success-desc">已提交 {{ count }} 个软件盘点，请等待审批。</div>

    <div v-if="initialPassword" class="password-card">
      <div class="password-title">账号信息（请截图保存）</div>
      <div class="password-row">
        <span class="label">用户名</span>
        <span class="value">{{ username }}</span>
        <el-button text type="primary" size="small" @click="copy(username)">复制</el-button>
      </div>
      <div class="password-row">
        <span class="label">初始密码</span>
        <span class="value">{{ initialPassword }}</span>
        <el-button text type="primary" size="small" @click="copy(initialPassword)">复制</el-button>
      </div>
      <div class="password-tip">
        关闭此页面后无法再次查看初始密码，请妥善保存。您可使用此账号登录系统查看自己提交的盘点记录。
      </div>
    </div>

    <div v-else class="tip-card">
      <div class="tip-text">
        您此前已注册过账号，可直接使用原账号密码登录查看盘点记录。
      </div>
    </div>

    <div class="actions">
      <el-button type="primary" size="large" style="width: 100%" @click="goContinue">
        继续登记
      </el-button>
      <el-button size="large" style="width: 100%; margin-top: 12px; margin-left: 0" @click="goLogin">
        去登录
      </el-button>
      <el-button size="large" style="width: 100%; margin-top: 12px; margin-left: 0" @click="goHome">
        返回首页
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { CircleCheck } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

const count = Number(route.query.count) || 0
const username = route.query.username as string
const initialPassword = route.query.initialPassword as string

function copy(text: string) {
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

function goContinue() {
  router.push({ name: 'H5Inventory' })
}

function goLogin() {
  router.push('/login')
}

function goHome() {
  router.push('/landing')
}
</script>

<style scoped lang="scss">
.h5-success {
  text-align: center;
  padding: 32px 16px;
}

.success-icon {
  margin-bottom: 16px;
}

.success-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.success-desc {
  font-size: 14px;
  color: #606266;
  margin-bottom: 24px;
}

.password-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  text-align: left;
  margin-bottom: 24px;
}

.password-title {
  font-size: 15px;
  font-weight: 600;
  color: #e6a23c;
  margin-bottom: 12px;
}

.password-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;

  &:last-child {
    border-bottom: none;
  }
}

.label {
  font-size: 14px;
  color: #606266;
  flex-shrink: 0;
  width: 70px;
}

.value {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  flex: 1;
  word-break: break-all;
  padding: 0 8px;
}

.password-tip {
  margin-top: 12px;
  padding: 10px;
  background: #fdf6ec;
  border: 1px solid #faecd8;
  border-radius: 8px;
  font-size: 13px;
  color: #e6a23c;
  line-height: 1.6;
}

.tip-card {
  background: #f4f4f5;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 24px;
}

.tip-text {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}

.actions {
  margin-top: 8px;
}
</style>
