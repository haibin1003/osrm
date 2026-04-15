<template>
  <div class="categories-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">软件分类</h1>
        <p class="page-subtitle">管理软件分类，支持树形结构</p>
      </div>
      <el-button type="primary" @click="showDialog('add')"><el-icon><Plus /></el-icon>新增分类</el-button>
    </div>

    <div class="table-card stripe-card">
      <el-table v-loading="loading" :data="treeData" row-key="id" default-expand-all stripe>
        <el-table-column prop="categoryName" label="分类名称" min-width="180" />
        <el-table-column prop="categoryCode" label="分类编码" width="160" />
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="showDialog('add', row)">添加子分类</el-button>
            <el-button type="primary" link @click="showDialog('edit', null, row)">编辑</el-button>
            <el-popconfirm title="确定删除？" @confirm="handleDelete(row)">
              <template #reference><el-button type="danger" link>删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" @close="resetForm">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="formData.categoryName" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="分类编码" prop="categoryCode">
          <el-input v-model="formData.categoryCode" placeholder="如：languages" :disabled="dialogMode === 'edit'" />
        </el-form-item>
        <el-form-item label="父分类">
          <el-tree-select
            v-model="formData.parentId"
            :data="treeData"
            :props="{ label: 'categoryName', value: 'id', children: 'children' }"
            placeholder="无（顶级分类）"
            clearable
            check-strictly
            :disabled="dialogMode === 'add-child'"
          />
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
import { categoryApi } from '@/api/category'
import type { Category, CategoryForm } from '@/types/category'

const loading = ref(false)
const submitting = ref(false)
const treeData = ref<Category[]>([])
const dialogVisible = ref(false)
const dialogMode = ref<'add' | 'edit' | 'add-child'>('add')
const editId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const formData = reactive<CategoryForm>({ categoryName: '', categoryCode: '', parentId: null, description: '' })
const formRules: FormRules = {
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  categoryCode: [{ required: true, message: '请输入分类编码', trigger: 'blur' }, { pattern: /^[a-zA-Z0-9_-]+$/, message: '只支持字母、数字、下划线和连字符', trigger: 'blur' }]
}

const dialogTitle = { add: '新增分类', edit: '编辑分类', 'add-child': '添加子分类' }

const loadData = async () => {
  loading.value = true
  try {
    treeData.value = await categoryApi.getTree()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const showDialog = (mode: 'add' | 'edit' | 'add-child', parent?: Category, editCat?: Category) => {
  dialogMode.value = mode
  if (mode === 'add-child' && parent) {
    formData.parentId = parent.id
  } else if (mode === 'edit' && editCat) {
    editId.value = editCat.id
    formData.categoryName = editCat.categoryName
    formData.categoryCode = editCat.categoryCode
    formData.parentId = editCat.parentId
    formData.description = editCat.description || ''
  }
  dialogVisible.value = true
}

const resetForm = () => { formRef.value?.resetFields(); editId.value = null; formData.parentId = null }

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  submitting.value = true
  try {
    if (dialogMode.value === 'edit' && editId.value) {
      await categoryApi.update(editId.value, { categoryName: formData.categoryName, parentId: formData.parentId, description: formData.description })
      ElMessage.success('更新成功')
    } else {
      await categoryApi.create(formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally { submitting.value = false }
}

const handleDelete = async (row: Category) => {
  try {
    await categoryApi.delete(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '删除失败')
  }
}

onMounted(() => loadData())
</script>

<style scoped lang="scss">
.categories-page {
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
