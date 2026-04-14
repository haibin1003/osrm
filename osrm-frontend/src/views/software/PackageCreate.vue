<template>
  <div class="package-create">
    <el-card class="form-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="title">新增软件包</span>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="120px"
        class="package-form"
      >
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="软件名称" prop="name">
              <el-input v-model="formData.name" placeholder="请输入软件名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="版本号" prop="version">
              <el-input v-model="formData.version" placeholder="如：1.0.0" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="分类" prop="category">
              <el-select v-model="formData.category" placeholder="请选择分类" style="width: 100%">
                <el-option label="开发工具" value="dev-tools" />
                <el-option label="数据库" value="database" />
                <el-option label="中间件" value="middleware" />
                <el-option label="操作系统" value="os" />
                <el-option label="办公软件" value="office" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="许可证" prop="license">
              <el-select v-model="formData.license" placeholder="请选择许可证" style="width: 100%">
                <el-option label="MIT" value="MIT" />
                <el-option label="Apache-2.0" value="Apache-2.0" />
                <el-option label="GPL-3.0" value="GPL-3.0" />
                <el-option label="BSD" value="BSD" />
                <el-option label="商业授权" value="commercial" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入软件描述"
          />
        </el-form-item>

        <el-form-item label="官方链接" prop="officialUrl">
          <el-input v-model="formData.officialUrl" placeholder="https://" />
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio label="active">可用</el-radio>
            <el-radio label="inactive">停用</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="上传附件">
          <el-upload
            action="#"
            :auto-upload="false"
            :limit="3"
            drag
          >
            <el-icon :size="50"><Upload /></el-icon>
            <div class="el-upload__text">
              拖拽文件到此处或 <em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持上传软件安装包、文档等附件，单个文件不超过 100MB
              </div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
            <el-icon><Check /></el-icon>提交
          </el-button>
          <el-button @click="handleCancel">
            <el-icon><Close /></el-icon>取消
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Upload, Check, Close } from '@element-plus/icons-vue'

const router = useRouter()
const formRef = ref<FormInstance>()
const submitLoading = ref(false)

const formData = reactive({
  name: '',
  version: '',
  category: '',
  license: '',
  description: '',
  officialUrl: '',
  status: 'active'
})

const formRules: FormRules = {
  name: [
    { required: true, message: '请输入软件名称', trigger: 'blur' }
  ],
  version: [
    { required: true, message: '请输入版本号', trigger: 'blur' },
    { pattern: /^\d+\.\d+\.\d+$/, message: '版本号格式应为 x.x.x', trigger: 'blur' }
  ],
  category: [
    { required: true, message: '请选择分类', trigger: 'change' }
  ],
  license: [
    { required: true, message: '请选择许可证', trigger: 'change' }
  ]
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        // TODO: 调用API创建软件包
        await new Promise(resolve => setTimeout(resolve, 1000))
        ElMessage.success('软件包创建成功')
        router.push('/software/packages')
      } catch (error) {
        ElMessage.error('创建失败')
      } finally {
        submitLoading.value = false
      }
    }
  })
}

const handleCancel = () => {
  router.back()
}
</script>

<style scoped lang="scss">
.package-create {
  background: #f6f9fc;
  min-height: calc(100vh - 120px);
  padding: var(--space-xl);

  .form-card {
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

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .title {
        font-size: var(--font-size-lg);
        font-weight: var(--font-weight-light);
      }
    }

    .package-form {
      max-width: 800px;
      margin: 0 auto;
      padding: 20px 0;
    }
  }
}
</style>
