<template>
  <Dialog v-model="dialogVisible" title="赠送积分" width="520px">
    <el-form ref="formRef" v-loading="formLoading" :model="formData" :rules="formRules" label-width="100px">
      <el-form-item label="用户编号" prop="userId">
        <el-input v-model="formData.userId" disabled />
      </el-form-item>
      <el-form-item label="赠送积分" prop="amount">
        <el-input-number v-model="formData.amount" class="!w-240px" :min="1" :max="99999999" />
      </el-form-item>
      <el-form-item label="赠送原因" prop="remark">
        <el-input v-model="formData.remark" :rows="3" placeholder="请输入赠送原因" type="textarea" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { AigcBillingWalletApi } from '@/api/aigc/billing/wallet'

defineOptions({ name: 'AigcBillingWalletGiftForm' })

const message = useMessage()
const dialogVisible = ref(false)
const formLoading = ref(false)
const formRef = ref()
const formData = ref({ userId: 0, amount: 0, remark: '' })
const formRules = reactive({
  userId: [{ required: true, message: '用户编号不能为空', trigger: 'blur' }],
  amount: [{ required: true, message: '赠送积分不能为空', trigger: 'blur' }],
  remark: [{ required: true, message: '赠送原因不能为空', trigger: 'blur' }]
})

const open = (userId: number) => {
  dialogVisible.value = true
  formData.value = { userId, amount: 0, remark: '' }
}

const emit = defineEmits(['success'])
const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    await AigcBillingWalletApi.giftWallet(formData.value)
    message.success('赠送成功')
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

defineExpose({ open })
</script>
