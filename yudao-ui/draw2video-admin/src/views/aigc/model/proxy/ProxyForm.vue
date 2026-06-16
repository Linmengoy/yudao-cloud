<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="680px">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px" v-loading="formLoading">
      <el-form-item :label="t('aigc.model.fields.proxyName')" prop="name">
        <el-input v-model="formData.name" :placeholder="t('aigc.model.placeholders.proxyName')" />
      </el-form-item>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item :label="t('aigc.model.fields.protocol')" prop="protocol">
            <el-select v-model="formData.protocol" class="!w-1/1" :placeholder="t('aigc.model.placeholders.protocol')">
              <el-option v-for="item in AIGC_PROXY_PROTOCOLS" :key="item.value" :label="getOptionLabel([item], item.value, t)" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('aigc.model.fields.port')" prop="port">
            <el-input-number v-model="formData.port" class="!w-1/1" :min="1" :max="65535" controls-position="right" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item :label="t('aigc.model.fields.proxyAddress')" prop="host">
        <el-input v-model="formData.host" :placeholder="t('aigc.model.placeholders.proxyAddress')" />
      </el-form-item>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item :label="t('aigc.model.fields.username')" prop="username">
            <el-input v-model="formData.username" clearable :placeholder="t('aigc.model.placeholders.optional')" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('aigc.model.fields.password')" prop="password">
            <el-input v-model="formData.password" show-password :placeholder="t('aigc.model.placeholders.passwordKeepEmpty')" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item :label="t('common.status')" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)" :key="dict.value" :value="dict.value">{{ dict.label }}</el-radio>
        </el-radio-group>
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
import { AigcModelProxyApi, AigcModelProxySaveReqVO } from '@/api/aigc/model/proxy'
import { AIGC_PROXY_PROTOCOLS, getOptionLabel } from '../constants'

defineOptions({ name: 'AigcModelProxyForm' })

const { t } = useI18n()
const message = useMessage()
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()
const formData = ref<AigcModelProxySaveReqVO>({
  id: undefined,
  name: undefined,
  protocol: 'SOCKS5',
  host: undefined,
  port: undefined,
  username: undefined,
  password: undefined,
  status: CommonStatusEnum.ENABLE,
  remark: undefined
})
const formRules = reactive({
  name: [{ required: true, message: '代理名称不能为空', trigger: 'blur' }],
  protocol: [{ required: true, message: '代理协议不能为空', trigger: 'change' }],
  host: [{ required: true, message: '代理地址不能为空', trigger: 'blur' }],
  port: [{ required: true, message: '代理端口不能为空', trigger: 'blur' }],
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
      formData.value = await AigcModelProxyApi.getProxy(id)
      formData.value.password = undefined
      formData.value.protocol = formData.value.protocol || 'SOCKS5'
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open })

const emit = defineEmits(['success'])
const removeEmptyPassword = (data: AigcModelProxySaveReqVO) => {
  if (typeof data.password === 'string' && data.password.trim() === '') {
    delete data.password
  }
  if (data.password === null || data.password === undefined) {
    delete data.password
  }
}

const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    const data = { ...formData.value }
    if (formType.value === 'update') {
      removeEmptyPassword(data)
    }
    if (formType.value === 'create') {
      await AigcModelProxyApi.createProxy(data)
      message.success(t('common.createSuccess'))
    } else {
      await AigcModelProxyApi.updateProxy(data)
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
    name: undefined,
    protocol: 'SOCKS5',
    host: undefined,
    port: undefined,
    username: undefined,
    password: undefined,
    status: CommonStatusEnum.ENABLE,
    remark: undefined
  }
  formRef.value?.resetFields()
}
</script>
