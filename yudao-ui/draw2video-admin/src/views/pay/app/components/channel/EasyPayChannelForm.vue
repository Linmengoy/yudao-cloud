<template>
  <div>
    <Dialog v-model="dialogVisible" :title="dialogTitle" width="830px">
      <el-form
        ref="formRef"
        v-loading="formLoading"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="渠道费率" label-width="180px" prop="feeRate">
          <el-input v-model="formData.feeRate" clearable placeholder="请输入渠道费率">
            <template #append>%</template>
          </el-input>
        </el-form-item>
        <el-form-item label="渠道状态" label-width="180px" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio
              v-for="dict in getDictOptions(DICT_TYPE.COMMON_STATUS)"
              :key="parseInt(dict.value)"
              :value="parseInt(dict.value)"
            >
              {{ dict.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="支付网关地址" label-width="180px" prop="config.serverUrl">
          <el-input v-model="formData.config.serverUrl" clearable placeholder="请输入第三方支付网关地址" />
        </el-form-item>
        <el-form-item label="商户 ID" label-width="180px" prop="config.merchantNo">
          <el-input v-model="formData.config.merchantNo" clearable placeholder="请输入第三方支付商户 ID" />
        </el-form-item>
        <el-form-item label="应用编号" label-width="180px" prop="config.appId">
          <el-input v-model="formData.config.appId" clearable placeholder="无应用编号可不填" />
        </el-form-item>
        <el-form-item label="签名类型" label-width="180px" prop="config.signType">
          <el-radio-group v-model="formData.config.signType">
            <el-radio value="MD5">MD5</el-radio>
            <el-radio value="RSA2">RSA2</el-radio>
            <el-radio value="HMAC_SHA256">HMAC_SHA256</el-radio>
          </el-radio-group>
        </el-form-item>
        <div v-if="formData.config.signType === 'RSA2'">
          <el-form-item label="商户私钥" label-width="180px" prop="config.privateKey">
            <el-input
              v-model="formData.config.privateKey"
              :autosize="{ minRows: 6, maxRows: 8 }"
              clearable
              placeholder="请输入商户私钥"
              type="textarea"
            />
          </el-form-item>
          <el-form-item label="平台公钥" label-width="180px" prop="config.publicKey">
            <el-input
              v-model="formData.config.publicKey"
              :autosize="{ minRows: 6, maxRows: 8 }"
              clearable
              placeholder="请输入 EasyPay 平台公钥"
              type="textarea"
            />
          </el-form-item>
        </div>
        <div v-else>
          <el-form-item label="商户密钥" label-width="180px" prop="config.secretKey">
            <el-input v-model="formData.config.secretKey" clearable placeholder="请输入第三方支付商户密钥" />
          </el-form-item>
        </div>
        <el-form-item label="沙箱环境" label-width="180px" prop="config.sandbox">
          <el-radio-group v-model="formData.config.sandbox">
            <el-radio :value="true">是</el-radio>
            <el-radio :value="false">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="支付返回地址" label-width="180px" prop="config.returnUrl">
          <el-input v-model="formData.config.returnUrl" clearable placeholder="可不填，默认使用订单返回地址" />
        </el-form-item>
        <el-form-item label="备注" label-width="180px" prop="remark">
          <el-input v-model="formData.remark" :style="{ width: '100%' }" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="dialogVisible = false">取 消</el-button>
      </template>
    </Dialog>
  </div>
</template>
<script lang="ts" setup>
import { CommonStatusEnum } from '@/utils/constants'
import { DICT_TYPE, getDictOptions } from '@/utils/dict'
import * as ChannelApi from '@/api/pay/channel'

defineOptions({ name: 'EasyPayChannelForm' })

const { t } = useI18n()
const message = useMessage()

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formData = ref<any>({
  appId: '',
  code: '',
  status: undefined,
  feeRate: undefined,
  remark: '',
  config: {
    serverUrl: '',
    merchantNo: '',
    appId: '',
    signType: 'MD5',
    privateKey: '',
    publicKey: '',
    secretKey: '',
    returnUrl: '',
    notifyContentType: 'JSON',
    sandbox: false,
    timeoutSeconds: 10,
    unifiedOrderPath: '/pay/unified-order',
    queryOrderPath: '/pay/query-order',
    successResponse: 'success'
  }
})
const formRules = {
  feeRate: [{ required: true, message: '请输入渠道费率', trigger: 'blur' }],
  status: [{ required: true, message: '渠道状态不能为空', trigger: 'blur' }],
  'config.serverUrl': [{ required: true, message: '请输入第三方支付网关地址', trigger: 'blur' }],
  'config.merchantNo': [{ required: true, message: '请输入第三方支付商户 ID', trigger: 'blur' }],
  'config.signType': [{ required: true, message: '请选择 EasyPay 签名类型', trigger: 'blur' }],
  'config.privateKey': [{ required: true, message: '请输入 EasyPay 商户私钥', trigger: 'blur' }],
  'config.publicKey': [{ required: true, message: '请输入 EasyPay 平台公钥', trigger: 'blur' }],
  'config.secretKey': [{ required: true, message: '请输入第三方支付商户密钥', trigger: 'blur' }],
  'config.sandbox': [{ required: true, message: '请选择是否沙箱环境', trigger: 'blur' }]
}
const formRef = ref()

const open = async (appId, code) => {
  dialogVisible.value = true
  formLoading.value = true
  resetForm(appId, code)
  try {
    const data = await ChannelApi.getChannel(appId, code)
    if (data && data.id) {
      formData.value = data
      formData.value.config = JSON.parse(data.config)
    }
    dialogTitle.value = !formData.value.id ? '创建支付渠道' : '编辑支付渠道'
  } finally {
    formLoading.value = false
  }
}
defineExpose({ open })

const emit = defineEmits(['success'])
const submitForm = async () => {
  if (!formRef) return
  const valid = await formRef.value.validate()
  if (!valid) return
  formLoading.value = true
  try {
    const data = { ...formData.value } as unknown as ChannelApi.ChannelVO
    data.config = JSON.stringify(formData.value.config)
    if (!data.id) {
      await ChannelApi.createChannel(data)
      message.success(t('common.createSuccess'))
    } else {
      await ChannelApi.updateChannel(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

const resetForm = (appId, code) => {
  formData.value = {
    appId: appId,
    code: code,
    status: CommonStatusEnum.ENABLE,
    remark: '',
    feeRate: null,
    config: {
      serverUrl: '',
      merchantNo: '',
      appId: '',
      signType: 'MD5',
      privateKey: '',
      publicKey: '',
      secretKey: '',
      returnUrl: '',
      notifyContentType: 'JSON',
      sandbox: false,
      timeoutSeconds: 10,
      unifiedOrderPath: '/pay/unified-order',
      queryOrderPath: '/pay/query-order',
      successResponse: 'success'
    }
  }
  formRef.value?.resetFields()
}
</script>
