<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="640px">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px" v-loading="formLoading">
      <el-form-item label="敏感词" prop="word">
        <el-input v-model="formData.word" placeholder="请输入敏感词" />
      </el-form-item>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="审核场景" prop="scene">
            <el-select v-model="formData.scene" class="!w-1/1" placeholder="请选择审核场景">
              <el-option v-for="item in AIGC_SAFETY_SCENES" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="风险等级" prop="level">
            <el-select v-model="formData.level" class="!w-1/1" placeholder="请选择风险等级">
              <el-option v-for="item in AIGC_RISK_LEVELS" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="匹配方式" prop="matchType">
            <el-select v-model="formData.matchType" class="!w-1/1" placeholder="请选择匹配方式">
              <el-option v-for="item in AIGC_SENSITIVE_WORD_MATCH_TYPES" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="formData.status">
              <el-radio v-for="item in AIGC_SENSITIVE_WORD_STATUSES" :key="item.value" :value="item.value">{{ item.label }}</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="请输入备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { AigcSensitiveWordApi, AigcSensitiveWordSaveReqVO } from '@/api/aigc/safety/sensitive-word'
import { AIGC_RISK_LEVELS, AIGC_SAFETY_SCENES, AIGC_SENSITIVE_WORD_MATCH_TYPES, AIGC_SENSITIVE_WORD_STATUSES } from '../constants'

defineOptions({ name: 'AigcSensitiveWordForm' })

const { t } = useI18n()
const message = useMessage()
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()
const formData = ref<AigcSensitiveWordSaveReqVO>({
  id: undefined,
  word: undefined,
  scene: 'PROMPT',
  level: 1,
  matchType: 'CONTAINS',
  status: 'ENABLE',
  remark: undefined
})
const formRules = reactive({
  word: [{ required: true, message: '敏感词不能为空', trigger: 'blur' }],
  scene: [{ required: true, message: '审核场景不能为空', trigger: 'change' }],
  level: [{ required: true, message: '风险等级不能为空', trigger: 'change' }]
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await AigcSensitiveWordApi.getSensitiveWord(id)
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open })

const emit = defineEmits(['success'])
const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    const data = { ...formData.value, word: formData.value.word?.trim() }
    if (formType.value === 'create') {
      await AigcSensitiveWordApi.createSensitiveWord(data)
      message.success(t('common.createSuccess'))
    } else {
      await AigcSensitiveWordApi.updateSensitiveWord(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

const resetForm = () => {
  formData.value = {
    id: undefined,
    word: undefined,
    scene: 'PROMPT',
    level: 1,
    matchType: 'CONTAINS',
    status: 'ENABLE',
    remark: undefined
  }
  formRef.value?.resetFields()
}
</script>
