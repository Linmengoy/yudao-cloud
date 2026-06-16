<template>
  <ContentWrap>
    <div class="flex items-center justify-between">
      <div>
        <div class="text-18px font-bold">{{ modelData?.name || '模型详情' }}</div>
        <div class="mt-6px text-13px text-gray-500">
          {{ modelData?.code || '-' }} / {{ modelData?.model || '-' }}
        </div>
      </div>
      <el-button @click="goBack"><Icon icon="ep:back" class="mr-5px" />返回</el-button>
    </div>
  </ContentWrap>

  <ContentWrap>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="基本信息" name="base">
        <el-form ref="baseFormRef" :model="baseForm" :rules="baseRules" label-width="120px" v-loading="baseLoading">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="模型类型" prop="type">
                <el-select v-model="baseForm.type" class="!w-1/1" placeholder="请选择模型类型">
                  <el-option v-for="item in AIGC_MODEL_TYPES" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="模型编码" prop="code">
                <el-input v-model="baseForm.code" placeholder="请输入模型编码" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="模型名称" prop="name">
                <el-input v-model="baseForm.name" placeholder="请输入模型名称" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="英文名称" prop="nameEn">
                <el-input v-model="baseForm.nameEn" placeholder="请输入英文名称" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="模型能力" prop="capabilities">
            <el-select v-model="baseForm.capabilities" class="!w-1/1" multiple filterable placeholder="请选择模型能力">
              <el-option v-for="item in AIGC_MODEL_CAPABILITIES" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="排序" prop="sort">
                <el-input-number v-model="baseForm.sort" class="!w-1/1" :min="0" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="最大并发" prop="maxConcurrent">
                <el-input-number v-model="baseForm.maxConcurrent" class="!w-1/1" :min="1" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="超时时间" prop="timeoutSeconds">
                <el-input-number v-model="baseForm.timeoutSeconds" class="!w-1/1" :min="1" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="用户端展示" prop="publicVisible">
                <el-switch v-model="baseForm.publicVisible" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="默认模型" prop="defaultModel">
                <el-switch v-model="baseForm.defaultModel" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="状态" prop="status">
                <el-radio-group v-model="baseForm.status">
                  <el-radio v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)" :key="dict.value" :value="dict.value">{{ dict.label }}</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="备注" prop="remark">
            <el-input v-model="baseForm.remark" type="textarea" placeholder="请输入备注" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="baseLoading" @click="saveBase">保存</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="渠道实现" name="channel">
        <div class="mb-12px">
          <el-button type="primary" @click="openChannelForm('create')"><Icon icon="ep:plus" class="mr-5px" />新增渠道实现</el-button>
        </div>
        <el-table v-loading="channelLoading" :data="channelList" :stripe="true" :show-overflow-tooltip="true">
          <el-table-column label="实现名称" align="center" prop="name" min-width="140" />
          <el-table-column label="渠道商" align="center" prop="providerId" min-width="140"><template #default="scope">{{ getProviderName(scope.row.providerId) }}</template></el-table-column>
          <el-table-column label="上游模型" align="center" prop="providerModel" min-width="180" />
          <el-table-column label="成本价" align="center" prop="costPrice" width="110" />
          <el-table-column label="权重" align="center" prop="weight" width="90" />
          <el-table-column label="优先级" align="center" prop="priority" width="90" />
          <el-table-column label="状态" align="center" prop="status" width="100"><template #default="scope"><dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" /></template></el-table-column>
          <el-table-column label="操作" align="center" width="180" fixed="right">
            <template #default="scope">
              <el-button link type="primary" @click="openChannelForm('update', scope.row.id)">编辑</el-button>
              <el-button link type="primary" @click="openChannelForm('clone', scope.row.id)">克隆</el-button>
              <el-button link type="danger" @click="deleteChannel(scope.row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="价格规则" name="price">
        <div class="mb-12px">
          <el-button type="primary" @click="openPriceForm('create')"><Icon icon="ep:plus" class="mr-5px" />新增价格规则</el-button>
        </div>
        <el-table v-loading="priceLoading" :data="priceList" :stripe="true" :show-overflow-tooltip="true">
          <el-table-column label="能力" align="center" prop="capability" min-width="140"><template #default="scope">{{ getOptionLabel(AIGC_MODEL_CAPABILITIES, scope.row.capability) }}</template></el-table-column>
          <el-table-column label="计费单位" align="center" prop="billingUnit" min-width="110"><template #default="scope">{{ getOptionLabel(AIGC_BILLING_UNITS, scope.row.billingUnit) }}</template></el-table-column>
          <el-table-column label="成本价" align="center" prop="costPrice" min-width="100" />
          <el-table-column label="销售价" align="center" prop="salePrice" min-width="100" />
          <el-table-column label="币种" align="center" prop="currencyType" min-width="90" />
          <el-table-column label="生效开始" align="center" prop="effectiveStartTime" min-width="160" :formatter="dateFormatter" />
          <el-table-column label="生效结束" align="center" prop="effectiveEndTime" min-width="160" :formatter="dateFormatter" />
          <el-table-column label="状态" align="center" prop="status" min-width="90"><template #default="scope"><dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" /></template></el-table-column>
          <el-table-column label="操作" align="center" width="160" fixed="right">
            <template #default="scope">
              <el-button link type="primary" @click="openPriceForm('update', scope.row.id)">编辑</el-button>
              <el-button link type="danger" @click="deletePrice(scope.row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </ContentWrap>

  <ChannelForm ref="channelFormRef" @success="loadChannelList" />
  <PriceForm ref="priceFormRef" @success="loadPriceList" />
</template>

<script setup lang="ts">
import { CommonStatusEnum } from '@/utils/constants'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import { AigcModelApi, type AigcModelSaveReqVO } from '@/api/aigc/model/model'
import { AigcModelChannelApi } from '@/api/aigc/model/channel'
import { AigcModelPriceApi } from '@/api/aigc/model/price'
import { AigcModelProviderApi } from '@/api/aigc/model/provider'
import type { AigcModelChannelRespVO, AigcModelPriceRespVO, AigcModelProviderRespVO, AigcModelRespVO } from '@/api/aigc/model/types'
import ChannelForm from '../channel/ChannelForm.vue'
import PriceForm from '../price/PriceForm.vue'
import { AIGC_BILLING_UNITS, AIGC_MODEL_CAPABILITIES, AIGC_MODEL_TYPES, getOptionLabel } from '../constants'

defineOptions({ name: 'AigcModelDetail' })

const route = useRoute()
const router = useRouter()
const message = useMessage()
const { t } = useI18n()
const modelId = computed(() => Number(route.params.id || 0))
const activeTab = ref('base')
const baseLoading = ref(false)
const channelLoading = ref(false)
const priceLoading = ref(false)
const modelData = ref<AigcModelRespVO>()
const channelList = ref<AigcModelChannelRespVO[]>([])
const priceList = ref<AigcModelPriceRespVO[]>([])
const providerList = ref<AigcModelProviderRespVO[]>([])
const baseFormRef = ref()
const channelFormRef = ref()
const priceFormRef = ref()
const baseForm = ref<AigcModelSaveReqVO>(createEmptyModel())
const baseRules = reactive({
  code: [{ required: true, message: '模型编码不能为空', trigger: 'blur' }],
  name: [{ required: true, message: '模型名称不能为空', trigger: 'blur' }],
  type: [{ required: true, message: '模型类型不能为空', trigger: 'change' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})

function createEmptyModel(): AigcModelSaveReqVO {
  return {
    id: undefined,
    providerId: undefined,
    code: undefined,
    name: undefined,
    nameEn: undefined,
    model: undefined,
    type: undefined,
    capabilities: [],
    publicVisible: true,
    defaultModel: false,
    sort: 0,
    maxConcurrent: 1,
    timeoutSeconds: 300,
    status: CommonStatusEnum.ENABLE,
    remark: undefined
  }
}

const loadBase = async () => {
  if (!modelId.value) return
  baseLoading.value = true
  try {
    const data = await AigcModelApi.getModel(modelId.value)
    data.capabilities = data.capabilities || []
    modelData.value = data
    baseForm.value = data
  } finally {
    baseLoading.value = false
  }
}

const loadProviders = async () => {
  const data = await AigcModelProviderApi.getProviderPage({ pageNo: 1, pageSize: 100 })
  providerList.value = data.list || []
}

const loadChannelList = async () => {
  if (!modelId.value) return
  channelLoading.value = true
  try {
    const data = await AigcModelChannelApi.getChannelPage({ pageNo: 1, pageSize: 100, modelId: modelId.value })
    channelList.value = data.list || []
  } finally {
    channelLoading.value = false
  }
}

const loadPriceList = async () => {
  if (!modelId.value) return
  priceLoading.value = true
  try {
    priceList.value = await AigcModelPriceApi.getPriceList({ modelId: modelId.value })
  } finally {
    priceLoading.value = false
  }
}

const saveBase = async () => {
  await baseFormRef.value.validate()
  baseLoading.value = true
  try {
    await AigcModelApi.updateModel(baseForm.value)
    message.success(t('common.updateSuccess'))
    await loadBase()
  } finally {
    baseLoading.value = false
  }
}

const openChannelForm = (type: string, id?: number) => {
  channelFormRef.value.open(type, id, modelId.value)
}

const openPriceForm = (type: string, id?: number) => {
  priceFormRef.value.open(type, id, modelId.value)
}

const deleteChannel = async (id?: number) => {
  if (!id) return
  await message.delConfirm()
  await AigcModelChannelApi.deleteChannel(id)
  message.success(t('common.delSuccess'))
  await loadChannelList()
}

const deletePrice = async (id?: number) => {
  if (!id) return
  await message.delConfirm()
  await AigcModelPriceApi.deletePrice(id)
  message.success(t('common.delSuccess'))
  await loadPriceList()
}

const getProviderName = (id?: number) => providerList.value.find((item) => item.id === id)?.name || `渠道 ${id}`
const goBack = () => router.push('/aigc/model/model')

onMounted(async () => {
  await Promise.all([loadBase(), loadProviders(), loadChannelList(), loadPriceList()])
})
</script>
