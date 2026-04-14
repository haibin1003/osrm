<template>
  <div class="tags-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">标签管理</h1>
        <p class="page-subtitle">管理软件标签</p>
      </div>
      <el-button type="primary" @click="dialogVisible = true"><el-icon><Plus /></el-icon>新增标签</el-button>
    </div>

    <el-card class="table-card stripe-card" shadow="never">
      <el-table v-loading="loading" :data="tagList" stripe>
        <el-table-column prop="tagName" label="标签名称" min-width="150">
          <template #default="{ row }">
            <el-tag size="small">{{ row.tagName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tagCode" label="标签编码" width="160" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-popconfirm title="确定删除？" @confirm="handleDelete(row)">
              <template #reference><el-button type="danger" link>删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="新增标签" width="450px" @close="resetForm">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="标签名称" prop="tagName">
          <el-input v-model="formData.tagName" placeholder="请输入标签名称" />
        </el-form-item>
        <el-form-item label="标签编码" prop="tagCode">
          <el-input v-model="formData.tagCode" placeholder="如：open-source" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { tagApi } from '@/api/category'
import type { Tag, TagForm } from '@/types/category'

const loading = ref(false)
const submitting = ref(false)
const tagList = ref<Tag[]>([])
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()

const formData = reactive<TagForm>({ tagName: '', tagCode: '', description: '' })
const formRules: FormRules = {
  tagName: [{ required: true, message: '请输入标签名称', trigger: 'blur' }],
  tagCode: [{ required: true, message: '请输入标签编码', trigger: 'blur' }, { pattern: /^[a-zA-Z0-9_-]+$/, message: '只支持字母、数字、下划线和连字符', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    tagList.value = await tagApi.findAll()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '加载失败')
  } finally { loading.value = false }
}

const resetForm = () => { formRef.value?.resetFields() }

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    await tagApi.create(formData)
    ElMessage.success('创建成功')
    dialogVisible.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally { submitting.value = false }
}

const handleDelete = async (row: Tag) => {
  try {
    await tagApi.delete(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '删除失败')
  }
}

onMounted(() => loadData())
</script>

<style scoped lang="scss">
.tags-page {
  background: #f6f9fc;
  min-height: calc(100vh - 120px);
  padding: var(--space-xl);

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

  .table-card {
    background: var(--color-bg-card);
    border: 1px solid var(--color-border);
    border-radius: 10px;
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
}
</style>
