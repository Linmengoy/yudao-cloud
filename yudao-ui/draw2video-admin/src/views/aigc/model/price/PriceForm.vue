<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible" width="760px">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px" v-loading="formLoading">
      <el-row :gutter="20">
        <el-col :span="12"><el-form-item label="模型 ID" prop="modelId"><el-input-number v-model="formData.modelId" class="!w-1/1" :min="1" controls-position="right" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="能力" prop="capability"><el-select v-model="formData.capability" class="!w-1/1" placeholder="请选择能力"><el-option v-for="item in AIGC_MODEL_CAPABILITIES" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12"><el-form-item label="计费单位" prop="billingUnit"><el-select v-model="formData.billingUnit" class="!w-1/1" placeholder="请选择计费单位"><el-option v-for="item in AIGC_BILLING_UNITS" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="币种" prop="currencyType"><el-input v-model="formData.currencyType" placeholder="POINT" /></el-form-item></el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12"><el-form-item label="成本价" prop="costPrice"><el-input-number v-model="formData.costPrice" class="!w-1/1" :min="0" :precision="2" controls-position="right" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="销售价" prop="salePrice"><el-input-number v-model="formData.salePrice" class="!w-1/1" :min="0" :precision="2" controls-position="right" /></el-form-item></el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12"><el-form-item label="生效开始" prop="effectiveStartTime"><el-date-picker v-model="formData.effectiveStartTime" class="!w-1/1" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择生效开始时间" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="生效结束" prop="effectiveEndTime"><el-date-picker v-model="formData.effectiveEndTime" class="!w-1/1" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择生效结束时间" /></el-form-item></el-col>
      </el-row>
      <el-form-item label="价格配置" prop="priceConfig"><el-input v-model="formData.priceConfig" type="textarea" :rows="4" placeholder="请输入 JSON 配置" /></el-form-item>
      <el-form-item label="状态" prop="status"><el-radio-group v-model="formData.status"><el-radio v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)" :key="dict.value" :value="dict.value">{{ dict.label }}</el-radio></el-radio-group></el-form-item>
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
import { AigcModelPriceApi, type AigcModelPriceSaveReqVO } from '@/api/aigc/model/price'
import { AIGC_BILLING_UNITS, AIGC_MODEL_CAPABILITIES } from '../constants'

defineOptions({ name: 'AigcModelPriceForm' })

const { t } = useI18n()
const message = useMessage()
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()
const formData = ref<AigcModelPriceSaveReqVO>({ id: undefined, modelId: undefined, capability: undefined, billingUnit: undefined, costPrice: 0, salePrice: 0, currencyType: 'POINT', priceConfig: undefined, effectiveStartTime: undefined, effectiveEndTime: undefined, status: CommonStatusEnum.ENABLE })
const formRules = reactive({ modelId: [{ required: true, message: '模型 ID 不能为空', trigger: 'blur' }], capability: [{ required: true, message: '能力不能为空', trigger: 'change' }], billingUnit: [{ required: true, message: '计费单位不能为空', trigger: 'change' }], salePrice: [{ required: true, message: '销售价不能为空', trigger: 'blur' }], currencyType: [{ required: true, message: '币种不能为空', trigger: 'blur' }], status: [{ required: true, message: '状态不能为空', trigger: 'change' }] })

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await AigcModelPriceApi.getPrice(id)
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
    if (formType.value === 'create') {
      await AigcModelPriceApi.createPrice(formData.value)
      message.success(t('common.createSuccess'))
    } else {
      await AigcModelPriceApi.updatePrice(formData.value)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

const resetForm = () => {
  formData.value = { id: undefined, modelId: undefined, capability: undefined, billingUnit: undefined, costPrice: 0, salePrice: 0, currencyType: 'POINT', priceConfig: undefined, effectiveStartTime: undefined, effectiveEndTime: undefined, status: CommonStatusEnum.ENABLE }
  formRef.value?.resetFields()
}
</script>
