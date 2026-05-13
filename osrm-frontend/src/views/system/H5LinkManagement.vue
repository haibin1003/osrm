<template>
  <div class="h5-link-mgmt">
    <div class="page-header">
      <h2 class="page-title">H5链接管理</h2>
      <el-button type="primary" @click="showGenerateDialog = true">
        <el-icon><Plus /></el-icon>
        生成新链接
      </el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe border style="width: 100%">
      <el-table-column prop="id" label="ID" width="60" align="center" />
      <el-table-column label="Token" min-width="140">
        <template #default="{ row }">
          <code class="token-text">{{ row.token.substring(0, 8) }}...</code>
        </template>
      </el-table-column>
      <el-table-column label="H5链接" min-width="220">
        <template #default="{ row }">
          <div class="url-cell">
            <span class="url-text">{{ h5Url(row.token) }}</span>
            <el-button link type="primary" size="small" @click="copyUrl(row.token)">复制</el-button>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="100" show-overflow-tooltip />
      <el-table-column prop="createdByName" label="创建人" width="100" />
      <el-table-column prop="createdAt" label="创建时间" width="160">
        <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column prop="expireAt" label="过期时间" width="160">
        <template #default="{ row }">{{ fmt(row.expireAt) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType(row)" size="small">{{ statusText(row) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="accessCount" label="访问次数" width="90" align="center" />
      <el-table-column prop="lastAccessAt" label="最近访问" width="160">
        <template #default="{ row }">{{ row.lastAccessAt ? fmt(row.lastAccessAt) : '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="copyUrl(row.token)">复制</el-button>
          <el-button
            v-if="!isExpired(row)"
            link
            type="warning"
            size="small"
            @click="openExtend(row)"
          >延期</el-button>
          <el-popconfirm
            v-if="row.enabled && !isExpired(row)"
            title="确定作废该链接？"
            @confirm="doRevoke(row.id)"
          >
            <template #reference>
              <el-button link type="danger" size="small">作废</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      class="pagination"
      @current-change="loadData"
      @size-change="loadData"
    />

    <!-- 生成弹窗 -->
    <el-dialog v-model="showGenerateDialog" title="生成 H5 链接" width="440px" @closed="genForm = { validDays: 7, remark: '' }">
      <el-form :model="genForm" label-width="90px">
        <el-form-item label="有效天数">
          <el-input-number v-model="genForm.validDays" :min="1" :max="365" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="genForm.remark" placeholder="选填，如：2026Q1存量盘点" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showGenerateDialog = false">取消</el-button>
        <el-button type="primary" @click="doGenerate" :loading="generating">生成</el-button>
      </template>
    </el-dialog>

    <!-- 生成结果弹窗 -->
    <el-dialog v-model="showResultDialog" title="链接已生成" width="500px">
      <el-alert type="success" :closable="false" show-icon title="请复制以下链接发送给需要填写存量登记的用户" />
      <div class="result-url-box">
        <code>{{ generatedUrl }}</code>
      </div>
      <template #footer>
        <el-button type="primary" @click="copyGeneratedUrl">复制链接</el-button>
        <el-button @click="showResultDialog = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 延期弹窗 -->
    <el-dialog v-model="showExtendDialog" title="延长有效期" width="360px">
      <el-form label-width="90px">
        <el-form-item label="延长天数">
          <el-input-number v-model="extendDays" :min="1" :max="365" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showExtendDialog = false">取消</el-button>
        <el-button type="primary" @click="doExtend" :loading="extending">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getH5Tokens, generateH5Token, revokeH5Token, extendH5Token, type H5AccessTokenItem } from '@/api/h5Admin'

const list = ref<H5AccessTokenItem[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)

const showGenerateDialog = ref(false)
const generating = ref(false)
const genForm = ref({ validDays: 7, remark: '' })
const showResultDialog = ref(false)
const generatedUrl = ref('')

const showExtendDialog = ref(false)
const extending = ref(false)
const extendDays = ref(7)
const extendTarget = ref<H5AccessTokenItem | null>(null)

const h5Origin = window.location.origin
const h5Url = (token: string) => `${h5Origin}/h5/inventory?token=${token}`

function loadData() {
  loading.value = true
  getH5Tokens(page.value, size.value)
    .then((res: any) => {
      list.value = res.content
      total.value = res.totalElements
    })
    .catch((e: Error) => ElMessage.error(e.message))
    .finally(() => (loading.value = false))
}

function doGenerate() {
  generating.value = true
  generateH5Token({ validDays: genForm.value.validDays, remark: genForm.value.remark || undefined })
    .then((res: any) => {
      showGenerateDialog.value = false
      generatedUrl.value = h5Url(res.token)
      showResultDialog.value = true
      loadData()
    })
    .catch((e: Error) => ElMessage.error(e.message))
    .finally(() => (generating.value = false))
}

function doRevoke(id: number) {
  revokeH5Token(id)
    .then(() => {
      ElMessage.success('已作废')
      loadData()
    })
    .catch((e: Error) => ElMessage.error(e.message))
}

function openExtend(row: H5AccessTokenItem) {
  extendTarget.value = row
  extendDays.value = 7
  showExtendDialog.value = true
}

function doExtend() {
  if (!extendTarget.value) return
  extending.value = true
  extendH5Token(extendTarget.value.id, extendDays.value)
    .then(() => {
      showExtendDialog.value = false
      ElMessage.success('已延期')
      loadData()
    })
    .catch((e: Error) => ElMessage.error(e.message))
    .finally(() => (extending.value = false))
}

function isExpired(row: H5AccessTokenItem) {
  return new Date(row.expireAt) < new Date()
}

function statusType(row: H5AccessTokenItem): 'success' | 'warning' | 'danger' | 'info' {
  if (!row.enabled) return 'info'
  if (isExpired(row)) return 'danger'
  const ms = new Date(row.expireAt).getTime() - Date.now()
  if (ms < 24 * 3600 * 1000) return 'warning'
  return 'success'
}

function statusText(row: H5AccessTokenItem): string {
  if (!row.enabled) return '已作废'
  if (isExpired(row)) return '已过期'
  const ms = new Date(row.expireAt).getTime() - Date.now()
  if (ms < 24 * 3600 * 1000) return '即将过期'
  return '有效'
}

function fmt(dateStr: string) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN', { hour12: false })
}

async function copyUrl(token: string) {
  try {
    await navigator.clipboard.writeText(h5Url(token))
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败')
  }
}

async function copyGeneratedUrl() {
  try {
    await navigator.clipboard.writeText(generatedUrl.value)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败')
  }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.h5-link-mgmt {
  .page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;

    .page-title {
      font-size: 18px;
      font-weight: 600;
      color: var(--color-text-primary);
      margin: 0;
    }
  }

  .token-text {
    font-size: 12px;
    background: var(--color-bg-page);
    padding: 2px 6px;
    border-radius: 4px;
  }

  .url-cell {
    display: flex;
    align-items: center;
    gap: 8px;

    .url-text {
      font-size: 12px;
      color: var(--color-text-secondary);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      max-width: 180px;
    }
  }

  .pagination {
    margin-top: 16px;
    justify-content: flex-end;
  }

  .result-url-box {
    margin-top: 16px;
    padding: 12px;
    background: var(--color-bg-page);
    border: 1px solid var(--color-border);
    border-radius: 6px;
    word-break: break-all;

    code {
      font-size: 13px;
      color: var(--color-text-primary);
    }
  }
}
</style>
