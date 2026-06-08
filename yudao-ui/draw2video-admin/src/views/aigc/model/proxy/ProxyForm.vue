<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="680px">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px" v-loading="formLoading">
      <el-form-item label="代理名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入代理名称" />
      </el-form-item>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="协议" prop="protocol">
            <el-select v-model="formData.protocol" class="!w-1/1" placeholder="请选择协议">
              <el-option v-for="item in AIGC_PROXY_PROTOCOLS" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="端口" prop="port">
            <el-input-number v-model="formData.port" class="!w-1/1" :min="1" :max="65535" controls-position="right" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="代理地址" prop="host">
        <el-input v-model="formData.host" placeholder="请输入代理 Host 或 IP" />
      </el-form-item>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="formData.username" clearable placeholder="可选" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="密码" prop="password">
            <el-input v-model="formData.password" show-password placeholder="不修改请留空" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)" :key="dict.value" :value="dict.value">{{ dict.label }}</el-radio>
        </el-radio-group>
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
import { AigcModelProxyApi, AigcModelProxySaveReqVO } from '@/api/aigc/model/proxy'
import { AIGC_PROXY_PROTOCOLS } from '../constants'

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
