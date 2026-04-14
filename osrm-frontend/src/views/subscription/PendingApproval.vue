<template>
  <div class="pending-approval">
    <div class="page-header">
      <div>
        <h1 class="page-title">审批中心</h1>
        <p class="page-subtitle">统一处理待审批事项</p>
      </div>
    </div>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <!-- 订购申请审批 -->
      <el-tab-pane name="subscription">
        <template #label>
          <span>订购申请 <el-badge v-if="subTotal > 0" :value="subTotal" type="danger" /></span>
        </template>

        <div class="stripe-card">
          <el-table v-loading="subLoading" :data="subList" stripe>
            <el-table-column prop="packageName" label="软件包" min-width="150" />
            <el-table-column prop="versionNumber" label="版本" width="100">
              <template #default="{ row }">{{ row.versionNumber || '不限' }}</template>
            </el-table-column>
            <el-table-column prop="systemName" label="业务系统" min-width="150" />
            <el-table-column prop="useScene" label="使用场景" min-width="200" show-overflow-tooltip />
            <el-table-column prop="statusName" label="状态" width="90">
              <template #default="{ row }">
                <span class="status-badge warning">{{ row.statusName }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="申请时间" width="160" />
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button type="success" link @click="approveSubscription(row)">
                  <el-icon><Check /></el-icon>通过
                </el-button>
                <el-button type="danger" link @click="rejectSubscription(row)">
                  <el-icon><Close /></el-icon>拒绝
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-wrapper">
            <el-pagination v-model:current-page="subPage" v-model:page-size="subSize"
              :total="subTotal" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next"
              @size-change="loadSubData" @current-change="loadSubData" />
          </div>
        </div>
      </el-tab-pane>

      <!-- 软件包审批 -->
      <el-tab-pane name="package">
        <template #label>
          <span>软件包审批 <el-badge v-if="pkgTotal > 0" :value="pkgTotal" type="danger" /></span>
        </template>

        <div class="stripe-card">
          <el-table v-loading="pkgLoading" :data="pkgList" stripe>
            <el-table-column prop="packageName" label="软件包名称" min-width="150" />
            <el-table-column prop="packageKey" label="包标识" min-width="150" />
            <el-table-column prop="softwareTypeName" label="类型" width="110">
              <template #default="{ row }"><span class="status-badge">{{ row.softwareTypeName }}</span></template>
            </el-table-column>
            <el-table-column prop="versionCount" label="版本数" width="80" align="center" />
            <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
            <el-table-column prop="createdAt" label="提交时间" width="160" />
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button type="success" link @click="approvePackage(row)">
                  <el-icon><Check /></el-icon>通过
                </el-button>
                <el-button type="danger" link @click="rejectPackage(row)">
                  <el-icon><Close /></el-icon>驳回
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-wrapper">
            <el-pagination v-model:current-page="pkgPage" v-model:page-size="pkgSize"
              :total="pkgTotal" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next"
              @size-change="loadPkgData" @current-change="loadPkgData" />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Close } from '@element-plus/icons-vue'
import { subscriptionApi } from '@/api/subscription'
import { softwareApi } from '@/api/software'
import type { SoftwarePackage } from '@/types/software'

const activeTab = ref('subscription')

// Subscription tab state
const subLoading = ref(false)
const subList = ref<any[]>([])
const subPage = ref(1)
const subSize = ref(10)
const subTotal = ref(0)

// Package tab state
const pkgLoading = ref(false)
const pkgList = ref<SoftwarePackage[]>([])
const pkgPage = ref(1)
const pkgSize = ref(10)
const pkgTotal = ref(0)

const loadSubData = async () => {
  subLoading.value = true
  try {
    const res = await subscriptionApi.pending(subPage.value, subSize.value)
    subList.value = res.content
    subTotal.value = res.totalElements
  } catch (e: any) { ElMessage.error(e.response?.data?.message || '加载失败') }
  finally { subLoading.value = false }
}

const loadPkgData = async () => {
  pkgLoading.value = true
  try {
    const res = await softwareApi.list({ status: 'PENDING', page: pkgPage.value, size: pkgSize.value })
    pkgList.value = res.content
    pkgTotal.value = res.totalElements
  } catch (e: any) { ElMessage.error(e.response?.data?.message || '加载失败') }
  finally { pkgLoading.value = false }
}

const handleTabChange = (tab: string) => {
  if (tab === 'subscription') loadSubData()
  else if (tab === 'package') loadPkgData()
}

const approveSubscription = async (row: any) => {
  await ElMessageBox.confirm(`确认批准 "${row.packageName}" 的订购申请？`, '审批确认', { type: 'success' })
  try {
    await subscriptionApi.approve(row.id)
    ElMessage.success('已批准')
    loadSubData()
  } catch (e: any) { ElMessage.error(e.response?.data?.message || '操作失败') }
}

const rejectSubscription = async (row: any) => {
  await ElMessageBox.confirm(`确认拒绝 "${row.packageName}" 的订购申请？`, '拒绝确认', { type: 'warning' })
  try {
    await subscriptionApi.reject(row.id)
    ElMessage.success('已拒绝')
    loadSubData()
  } catch (e: any) { ElMessage.error(e.response?.data?.message || '操作失败') }
}

const approvePackage = async (row: SoftwarePackage) => {
  await ElMessageBox.confirm(`确认审批通过软件包 "${row.packageName}"？`, '审批确认', { type: 'success' })
  try {
    await softwareApi.approve(row.id)
    ElMessage.success('审批通过')
    loadPkgData()
  } catch (e: any) { ElMessage.error(e.response?.data?.message || '操作失败') }
}

const rejectPackage = async (row: SoftwarePackage) => {
  await ElMessageBox.confirm(`确认驳回软件包 "${row.packageName}"？`, '驳回确认', { type: 'warning' })
  try {
    await softwareApi.reject(row.id)
    ElMessage.success('已驳回')
    loadPkgData()
  } catch (e: any) { ElMessage.error(e.response?.data?.message || '操作失败') }
}

onMounted(() => {
  loadSubData()
})
</script>

<style scoped lang="scss">
.pending-approval {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: var(--space-xl);
    .page-title {
      font-size: var(--font-size-3xl);
      font-weight: var(--font-weight-light);
      margin: 0;
      color: var(--color-text-primary);
      letter-spacing: -0.3px;
    }
    .page-subtitle {
      font-size: var(--font-size-sm);
      color: var(--color-text-secondary);
      margin: var(--space-xs) 0 0;
      font-weight: var(--font-weight-light);
    }
  }

  .stripe-card {
    background: var(--color-bg-card);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    overflow: hidden;
    position: relative;

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 3px;
      background: linear-gradient(135deg, #635bff, #a259ff);
    }
  }

  .pagination-wrapper {
    margin-top: var(--space-lg);
    display: flex;
    justify-content: flex-end;
    padding: var(--space-md) var(--space-lg);
    border-top: 1px solid var(--color-border-light);
  }
}
</style>
