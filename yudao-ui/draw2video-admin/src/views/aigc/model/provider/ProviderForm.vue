<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="760px">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px" v-loading="formLoading">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="渠道编码" prop="code">
            <el-input v-model="formData.code" placeholder="请输入渠道编码" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="渠道名称" prop="name">
            <el-input v-model="formData.name" placeholder="请输入渠道名称" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="API 地址" prop="apiBaseUrl">
        <el-input v-model="formData.apiBaseUrl" placeholder="请输入 API 地址" />
      </el-form-item>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="鉴权方式" prop="authType">
            <el-select v-model="formData.authType" class="!w-1/1" placeholder="请选择鉴权方式">
              <el-option v-for="item in AIGC_PROVIDER_AUTH_TYPES" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="formData.status">
              <el-radio v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)" :key="dict.value" :value="dict.value">{{ dict.label }}</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="API Key" prop="apiKey">
            <el-input v-model="formData.apiKey" show-password placeholder="不修改请留空" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="Secret Key" prop="secretKey">
            <el-input v-model="formData.secretKey" show-password placeholder="不修改请留空" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="超时时间" prop="timeoutSeconds">
            <el-input-number v-model="formData.timeoutSeconds" class="!w-1/1" :min="1" placeholder="单位：秒" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="健康状态" prop="healthStatus">
            <el-select v-model="formData.healthStatus" class="!w-1/1" clearable placeholder="请选择健康状态">
              <el-option v-for="item in AIGC_HEALTH_STATUSES" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="启用代理" prop="proxyEnabled">
        <el-switch v-model="formData.proxyEnabled" />
      </el-form-item>
      <template v-if="formData.proxyEnabled">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="代理协议" prop="proxyProtocol">
              <el-select v-model="formData.proxyProtocol" class="!w-1/1" placeholder="请选择代理协议">
                <el-option v-for="item in PROXY_PROTOCOLS" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="代理端口" prop="proxyPort">
              <el-input-number v-model="formData.proxyPort" class="!w-1/1" :min="1" :max="65535" controls-position="right" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="代理地址" prop="proxyHost">
              <el-input v-model="formData.proxyHost" placeholder="请输入代理 Host 或 IP" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="代理用户名" prop="proxyUsername">
              <el-input v-model="formData.proxyUsername" clearable placeholder="可选" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="代理密码" prop="proxyPassword">
          <el-input v-model="formData.proxyPassword" show-password placeholder="不修改请留空" />
        </el-form-item>
      </template>
      <el-form-item label="扩展配置" prop="extraConfig">
        <el-input v-model="formData.extraConfig" type="textarea" :rows="3" placeholder="请输入 JSON 扩展配置" />
      </el-form-item>
      <el-form-item label="限流配置" prop="rateLimitConfig">
        <el-input v-model="formData.rateLimitConfig" type="textarea" :rows="3" placeholder="请输入 JSON 限流配置" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" type="textarea" placeholder="请输入备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { CommonStatusEnum } from '@/utils/constants'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { AigcModelProviderApi, AigcModelProviderSaveReqVO } from '@/api/aigc/model/provider'
import { AIGC_HEALTH_STATUSES, AIGC_PROVIDER_AUTH_TYPES } from '../constants'

defineOptions({ name: 'AigcModelProviderForm' })

const { t } = useI18n()
const message = useMessage()
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()
const PROXY_PROTOCOLS = [
  { label: 'HTTP', value: 'HTTP' },
  { label: 'HTTPS', value: 'HTTPS' },
  { label: 'SOCKS5', value: 'SOCKS5' },
  { label: 'SOCKS5H（远程 DNS）', value: 'SOCKS5H' }
]
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
  proxyProtocol: 'SOCKS5',
  proxyHost: undefined,
  proxyPort: undefined,
  proxyUsername: undefined,
  proxyPassword: undefined,
  rateLimitConfig: undefined,
  healthStatus: 'UNKNOWN',
  status: CommonStatusEnum.ENABLE,
  remark: undefined
})
const formRules = reactive({
  code: [{ required: true, message: '渠道编码不能为空', trigger: 'blur' }],
  name: [{ required: true, message: '渠道名称不能为空', trigger: 'blur' }],
  apiBaseUrl: [{ required: true, message: 'API 地址不能为空', trigger: 'blur' }],
  authType: [{ required: true, message: '鉴权方式不能为空', trigger: 'change' }],
  proxyProtocol: [{ required: true, message: '代理协议不能为空', trigger: 'change' }],
  proxyHost: [{ required: true, message: '代理地址不能为空', trigger: 'blur' }],
  proxyPort: [{ required: true, message: '代理端口不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await AigcModelProviderApi.getProvider(id)
      formData.value.apiKey = undefined
      formData.value.secretKey = undefined
      formData.value.proxyPassword = undefined
      formData.value.proxyProtocol = formData.value.proxyProtocol || 'SOCKS5'
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
  if (typeof data.proxyPassword === 'string' && data.proxyPassword.trim() === '') {
    delete data.proxyPassword
  }
  if (data.proxyPassword === null || data.proxyPassword === undefined) {
    delete data.proxyPassword
  }
}

const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    const data = { ...formData.value }
    if (!data.proxyEnabled) {
      data.proxyProtocol = undefined
      data.proxyHost = undefined
      data.proxyPort = undefined
      data.proxyUsername = undefined
      data.proxyPassword = undefined
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
    proxyProtocol: 'SOCKS5',
    proxyHost: undefined,
    proxyPort: undefined,
    proxyUsername: undefined,
    proxyPassword: undefined,
    rateLimitConfig: undefined,
    healthStatus: 'UNKNOWN',
    status: CommonStatusEnum.ENABLE,
    remark: undefined
  }
  formRef.value?.resetFields()
}
</script>
