<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="720px">
    <el-form ref="formRef" v-loading="formLoading" :model="formData" :rules="formRules" label-width="110px">
      <el-row :gutter="20">
        <el-col :span="12"><el-form-item label="套餐名称" prop="name"><el-input v-model="formData.name" placeholder="请输入套餐名称" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="支付金额（元）" prop="payAmount"><el-input-number v-model="formData.payAmount" class="!w-1/1" :min="0" :precision="2" controls-position="right" /></el-form-item></el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12"><el-form-item label="充值积分" prop="pointAmount"><el-input-number v-model="formData.pointAmount" class="!w-1/1" :min="0" :precision="2" controls-position="right" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="赠送积分" prop="giftAmount"><el-input-number v-model="formData.giftAmount" class="!w-1/1" :min="0" :precision="2" controls-position="right" /></el-form-item></el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12"><el-form-item label="推荐" prop="recommendStatus"><el-switch v-model="formData.recommendStatus" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="排序" prop="sort"><el-input-number v-model="formData.sort" class="!w-1/1" :min="0" controls-position="right" /></el-form-item></el-col>
      </el-row>
      <el-form-item label="状态" prop="status"><el-radio-group v-model="formData.status"><el-radio v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)" :key="dict.value" :value="dict.value">{{ dict.label }}</el-radio></el-radio-group></el-form-item>
      <el-form-item label="描述" prop="description"><el-input v-model="formData.description" placeholder="请输入描述" /></el-form-item>
      <el-form-item label="权益说明" prop="features"><el-input v-model="formData.features" type="textarea" :rows="5" placeholder="每行一条权益" /></el-form-item>
      <el-form-item label="备注" prop="remark"><el-input v-model="formData.remark" placeholder="请输入备注" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { CommonStatusEnum } from '@/utils/constants'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { AigcRechargePackageApi, type AigcRechargePackageVO } from '@/api/aigc/billing/recharge-package'

defineOptions({ name: 'AigcRechargePackageForm' })

const { t } = useI18n()
const message = useMessage()
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formRef = ref()
const formData = ref<AigcRechargePackageVO>({ id: undefined, name: '', payAmount: 0, pointAmount: 0, giftAmount: 0, description: undefined, features: undefined, recommendStatus: false, sort: 0, status: CommonStatusEnum.ENABLE, remark: undefined })
const formRules = reactive({ name: [{ required: true, message: '套餐名称不能为空', trigger: 'blur' }], payAmount: [{ required: true, message: '支付金额不能为空', trigger: 'blur' }], pointAmount: [{ required: true, message: '充值积分不能为空', trigger: 'blur' }], recommendStatus: [{ required: true, message: '是否推荐不能为空', trigger: 'change' }], sort: [{ required: true, message: '排序不能为空', trigger: 'blur' }], status: [{ required: true, message: '状态不能为空', trigger: 'change' }] })

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      const data = await AigcRechargePackageApi.getPackage(id)
      formData.value = { ...data, payAmount: Number(data.payAmount || 0) / 100 }
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
    const data = { ...formData.value, payAmount: Math.round(Number(formData.value.payAmount || 0) * 100) }
    if (formType.value === 'create') {
      await AigcRechargePackageApi.createPackage(data)
      message.success(t('common.createSuccess'))
    } else {
      await AigcRechargePackageApi.updatePackage(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

const resetForm = () => {
  formData.value = { id: undefined, name: '', payAmount: 0, pointAmount: 0, giftAmount: 0, description: undefined, features: undefined, recommendStatus: false, sort: 0, status: CommonStatusEnum.ENABLE, remark: undefined }
  formRef.value?.resetFields()
}
</script>
