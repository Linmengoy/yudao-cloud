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
        <el-form-item label="支付网关地址" label-width="180px" prop="config.apiBase">
          <el-input v-model="formData.config.apiBase" clearable placeholder="请输入 EasyPay 网关地址" />
        </el-form-item>
        <el-form-item label="商户号" label-width="180px" prop="config.pid">
          <el-input v-model="formData.config.pid" clearable placeholder="请输入 EasyPay 商户号" />
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
          <el-form-item label="商户密钥" label-width="180px" prop="config.pkey">
            <el-input v-model="formData.config.pkey" clearable placeholder="请输入 EasyPay 商户密钥" />
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
        <el-form-item label="支付通知地址" label-width="180px" prop="config.notifyUrl">
          <el-input v-model="formData.config.notifyUrl" clearable placeholder="可不填，默认使用订单通知地址" />
        </el-form-item>
        <el-form-item label="支付模式" label-width="180px" prop="config.paymentMode">
          <el-radio-group v-model="formData.config.paymentMode">
            <el-radio value="">接口模式</el-radio>
            <el-radio value="popup">收银台跳转</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="支付类型" label-width="180px" prop="config.paymentType">
          <el-input v-model="formData.config.paymentType" clearable placeholder="请输入支付类型，例如 alipay、wxpay" />
        </el-form-item>
        <el-form-item label="通用通道 ID" label-width="180px" prop="config.cid">
          <el-input v-model="formData.config.cid" clearable placeholder="可不填，EasyPay 通用通道 ID" />
        </el-form-item>
        <el-form-item label="支付宝通道 ID" label-width="180px" prop="config.cidAlipay">
          <el-input v-model="formData.config.cidAlipay" clearable placeholder="可不填，支付宝支付优先使用" />
        </el-form-item>
        <el-form-item label="微信通道 ID" label-width="180px" prop="config.cidWxpay">
          <el-input v-model="formData.config.cidWxpay" clearable placeholder="可不填，微信支付优先使用" />
        </el-form-item>
        <el-form-item label="请求超时时间" label-width="180px" prop="config.timeoutSeconds">
          <el-input-number v-model="formData.config.timeoutSeconds" :min="1" :precision="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="回调内容类型" label-width="180px" prop="config.notifyContentType">
          <el-radio-group v-model="formData.config.notifyContentType">
            <el-radio value="JSON">JSON</el-radio>
            <el-radio value="FORM">FORM</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="统一下单路径" label-width="180px" prop="config.unifiedOrderPath">
          <el-input v-model="formData.config.unifiedOrderPath" clearable placeholder="请输入统一下单路径" />
        </el-form-item>
        <el-form-item label="查单路径" label-width="180px" prop="config.queryOrderPath">
          <el-input v-model="formData.config.queryOrderPath" clearable placeholder="请输入查单路径" />
        </el-form-item>
        <el-form-item label="成功响应内容" label-width="180px" prop="config.successResponse">
          <el-input v-model="formData.config.successResponse" clearable placeholder="请输入回调成功响应内容" />
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
    apiBase: '',
    pid: '',
    appId: '',
    signType: 'MD5',
    privateKey: '',
    publicKey: '',
    secretKey: '',
    pkey: '',
    returnUrl: '',
    notifyUrl: '',
    notifyContentType: 'JSON',
    sandbox: false,
    timeoutSeconds: 10,
    unifiedOrderPath: '/pay/unified-order',
    queryOrderPath: '/pay/query-order',
    successResponse: 'success',
    cid: '',
    cidAlipay: '',
    cidWxpay: '',
    paymentMode: '',
    paymentType: 'alipay'
  }
})
const formRules = {
  feeRate: [{ required: true, message: '请输入渠道费率', trigger: 'blur' }],
  status: [{ required: true, message: '渠道状态不能为空', trigger: 'blur' }],
  'config.apiBase': [{ required: true, message: '请输入 EasyPay 网关地址', trigger: 'blur' }],
  'config.pid': [{ required: true, message: '请输入 EasyPay 商户号', trigger: 'blur' }],
  'config.signType': [{ required: true, message: '请选择 EasyPay 签名类型', trigger: 'blur' }],
  'config.privateKey': [{ required: true, message: '请输入 EasyPay 商户私钥', trigger: 'blur' }],
  'config.publicKey': [{ required: true, message: '请输入 EasyPay 平台公钥', trigger: 'blur' }],
  'config.pkey': [{ required: true, message: '请输入 EasyPay 商户密钥', trigger: 'blur' }],
  'config.sandbox': [{ required: true, message: '请选择是否沙箱环境', trigger: 'blur' }],
  'config.timeoutSeconds': [{ required: true, message: '请输入请求超时时间', trigger: 'blur' }],
  'config.notifyContentType': [{ required: true, message: '请选择回调内容类型', trigger: 'blur' }],
  'config.unifiedOrderPath': [{ required: true, message: '请输入统一下单路径', trigger: 'blur' }],
  'config.queryOrderPath': [{ required: true, message: '请输入查单路径', trigger: 'blur' }],
  'config.successResponse': [{ required: true, message: '请输入回调成功响应内容', trigger: 'blur' }],
  'config.paymentType': [{ required: true, message: '请输入支付类型', trigger: 'blur' }]
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
      normalizeConfig()
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
    normalizeConfig()
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
      apiBase: '',
      pid: '',
      appId: '',
      signType: 'MD5',
      privateKey: '',
      publicKey: '',
      secretKey: '',
      pkey: '',
      returnUrl: '',
      notifyUrl: '',
      notifyContentType: 'JSON',
      sandbox: false,
      timeoutSeconds: 10,
      unifiedOrderPath: '/pay/unified-order',
      queryOrderPath: '/pay/query-order',
      successResponse: 'success',
      cid: '',
      cidAlipay: '',
      cidWxpay: '',
      paymentMode: '',
      paymentType: 'alipay'
    }
  }
  formRef.value?.resetFields()
}

const normalizeConfig = () => {
  const config = formData.value.config
  config.apiBase = config.apiBase || config.serverUrl || ''
  config.serverUrl = config.apiBase
  config.pid = config.pid || config.merchantNo || ''
  config.merchantNo = config.pid
  config.pkey = config.pkey || config.secretKey || ''
  config.secretKey = config.pkey
  config.signType = config.signType || 'MD5'
  config.notifyContentType = config.notifyContentType || 'JSON'
  config.sandbox = config.sandbox ?? false
  config.timeoutSeconds = config.timeoutSeconds || 10
  config.unifiedOrderPath = config.unifiedOrderPath || '/pay/unified-order'
  config.queryOrderPath = config.queryOrderPath || '/pay/query-order'
  config.successResponse = config.successResponse || 'success'
  config.paymentMode = config.paymentMode || ''
  config.paymentType = config.paymentType || 'alipay'
}
</script>
