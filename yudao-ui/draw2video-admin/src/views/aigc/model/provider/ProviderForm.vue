<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="760px">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px" v-loading="formLoading">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item :label="t('aigc.model.fields.providerCode')" prop="code">
            <el-input v-model="formData.code" :placeholder="t('aigc.model.placeholders.providerCode')" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('aigc.model.fields.providerName')" prop="name">
            <el-input v-model="formData.name" :placeholder="t('aigc.model.placeholders.providerName')" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item :label="t('aigc.model.fields.apiBaseUrl')" prop="apiBaseUrl">
        <el-input v-model="formData.apiBaseUrl" :placeholder="t('aigc.model.placeholders.apiBaseUrl')" />
      </el-form-item>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item :label="t('aigc.model.fields.authType')" prop="authType">
            <el-select v-model="formData.authType" class="!w-1/1" :placeholder="t('aigc.model.placeholders.authType')">
              <el-option v-for="item in AIGC_PROVIDER_AUTH_TYPES" :key="item.value" :label="getOptionLabel([item], item.value, t)" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('common.status')" prop="status">
            <el-radio-group v-model="formData.status">
              <el-radio v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)" :key="dict.value" :value="dict.value">{{ dict.label }}</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item :label="t('aigc.model.fields.apiKey')" prop="apiKey">
            <el-input v-model="formData.apiKey" show-password :placeholder="t('aigc.model.placeholders.passwordKeepEmpty')" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('aigc.model.fields.secretKey')" prop="secretKey">
            <el-input v-model="formData.secretKey" show-password :placeholder="t('aigc.model.placeholders.passwordKeepEmpty')" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item :label="t('aigc.model.fields.timeoutSeconds')" prop="timeoutSeconds">
            <el-input-number v-model="formData.timeoutSeconds" class="!w-1/1" :min="1" :placeholder="t('aigc.model.fields.timeoutSeconds')" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('aigc.model.fields.healthStatus')" prop="healthStatus">
            <el-select v-model="formData.healthStatus" class="!w-1/1" clearable :placeholder="t('aigc.model.placeholders.healthStatus')">
              <el-option v-for="item in AIGC_HEALTH_STATUSES" :key="item.value" :label="getOptionLabel([item], item.value, t)" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item :label="t('aigc.model.fields.proxy')" prop="proxyEnabled">
        <el-switch v-model="formData.proxyEnabled" />
      </el-form-item>
      <template v-if="formData.proxyEnabled">
        <el-form-item :label="t('aigc.model.fields.proxy')" prop="proxyId">
          <div class="flex w-1/1 gap-8px">
            <el-select
              v-model="formData.proxyId"
              class="flex-1"
              clearable
              filterable
              :placeholder="t('aigc.model.placeholders.proxy')"
              :loading="proxyLoading"
            >
              <el-option
                v-for="item in proxyList"
                :key="item.id"
                :label="`${item.name}（${getOptionLabel(AIGC_PROXY_PROTOCOLS, item.protocol, t)} ${item.host}:${item.port}）`"
                :value="item.id!"
              />
            </el-select>
            <el-button @click="openProxyManage">{{ t('aigc.model.actions.createProxy') }}</el-button>
          </div>
        </el-form-item>
      </template>
      <el-form-item :label="t('aigc.model.fields.extraConfig')" prop="extraConfig">
        <el-input v-model="formData.extraConfig" type="textarea" :rows="3" :placeholder="t('aigc.model.placeholders.json')" />
      </el-form-item>
      <el-form-item :label="t('aigc.model.fields.rateLimitConfig')" prop="rateLimitConfig">
        <el-input v-model="formData.rateLimitConfig" type="textarea" :rows="3" :placeholder="t('aigc.model.placeholders.json')" />
      </el-form-item>
      <el-form-item :label="t('aigc.model.fields.remark')" prop="remark">
        <el-input v-model="formData.remark" type="textarea" :placeholder="t('aigc.model.placeholders.remark')" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">{{ t('common.ok') }}</el-button>
      <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { CommonStatusEnum } from '@/utils/constants'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { AigcModelProviderApi, AigcModelProviderSaveReqVO } from '@/api/aigc/model/provider'
import { AigcModelProxyApi } from '@/api/aigc/model/proxy'
import type { AigcModelProxyRespVO } from '@/api/aigc/model/types'
import { AIGC_HEALTH_STATUSES, AIGC_PROVIDER_AUTH_TYPES, AIGC_PROXY_PROTOCOLS, getOptionLabel } from '../constants'

defineOptions({ name: 'AigcModelProviderForm' })

const { t } = useI18n()
const message = useMessage()
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()
const proxyLoading = ref(false)
const proxyList = ref<AigcModelProxyRespVO[]>([])
const formData = ref<AigcModelProviderSaveReqVO>({
  id: undefined,
  code: undefined,
  name: undefined,
  apiBaseUrl: undefined,
  authType: 'BEARER',
  apiKey: undefined,
  secretKey: undefined,
  extraConfig: undefined,
  timeoutSeconds: 60,
  proxyEnabled: false,
  proxyId: undefined,
  rateLimitConfig: undefined,
  healthStatus: 'UNKNOWN',
  status: CommonStatusEnum.ENABLE,
  remark: undefined
})
const formRules = reactive({
  code: [{ required: true, message: t('aigc.model.validation.providerCodeRequired'), trigger: 'blur' }],
  name: [{ required: true, message: t('aigc.model.validation.providerNameRequired'), trigger: 'blur' }],
  apiBaseUrl: [{ required: true, message: t('aigc.model.validation.apiBaseUrlRequired'), trigger: 'blur' }],
  authType: [{ required: true, message: t('aigc.model.validation.authTypeRequired'), trigger: 'change' }],
  proxyId: [{ required: true, message: t('aigc.model.validation.proxyRequired'), trigger: 'change' }],
  status: [{ required: true, message: t('aigc.model.validation.statusRequired'), trigger: 'change' }]
})

const loadProxyList = async () => {
  proxyLoading.value = true
  try {
    proxyList.value = await AigcModelProxyApi.getSimpleProxyList()
  } finally {
    proxyLoading.value = false
  }
}

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  await loadProxyList()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await AigcModelProviderApi.getProvider(id)
      formData.value.apiKey = undefined
      formData.value.secretKey = undefined
      formData.value.proxyEnabled = Boolean(formData.value.proxyEnabled)
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open })

const emit = defineEmits(['success'])
const removeEmptySecret = (data: AigcModelProviderSaveReqVO) => {
  if (typeof data.apiKey === 'string' && data.apiKey.trim() === '') {
    delete data.apiKey
  }
  if (typeof data.secretKey === 'string' && data.secretKey.trim() === '') {
    delete data.secretKey
  }
  if (data.apiKey === null || data.apiKey === undefined) {
    delete data.apiKey
  }
  if (data.secretKey === null || data.secretKey === undefined) {
    delete data.secretKey
  }
}

const removeLegacyProxyFields = (data: AigcModelProviderSaveReqVO) => {
  delete data.proxyProtocol
  delete data.proxyHost
  delete data.proxyPort
  delete data.proxyUsername
  delete data.proxyPassword
}

const openProxyManage = () => {
  window.open('/aigc/model/proxy', '_blank', 'noopener,noreferrer')
}

const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    const data = { ...formData.value }
    removeLegacyProxyFields(data)
    if (!data.proxyEnabled) {
      data.proxyId = undefined
    }
    if (formType.value === 'update') {
      removeEmptySecret(data)
    }
    if (formType.value === 'create') {
      await AigcModelProviderApi.createProvider(data)
      message.success(t('common.createSuccess'))
    } else {
      await AigcModelProviderApi.updateProvider(data)
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
    code: undefined,
    name: undefined,
    apiBaseUrl: undefined,
    authType: 'BEARER',
    apiKey: undefined,
    secretKey: undefined,
    extraConfig: undefined,
    timeoutSeconds: 60,
    proxyEnabled: false,
    proxyId: undefined,
    rateLimitConfig: undefined,
    healthStatus: 'UNKNOWN',
    status: CommonStatusEnum.ENABLE,
    remark: undefined
  }
  formRef.value?.resetFields()
}

watch(
  () => formData.value.proxyEnabled,
  (enabled) => {
    if (!enabled) {
      formData.value.proxyId = undefined
    }
  }
)
</script>
