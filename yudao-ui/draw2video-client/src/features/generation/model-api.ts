import { api } from "@/lib/api-client";

export type AigcModel = {
  /** 模型 ID */
  id: number;
  /** 模型编码 */
  code: string;
  /** 模型名称 */
  name: string;
  /** 模型标识（提供商侧的模型名） */
  model: string;
  /** 模型类型：1-图片生成，2-文本生成，3-视频生成 */
  type: number;
  /** 是否公开可见 */
  publicVisible: boolean;
  /** 是否为默认模型 */
  defaultModel: boolean;
  /** 排序优先级 */
  sort: number;
  /** 最大并发数 */
  maxConcurrent?: number;
  /** 超时时间（秒） */
  timeoutSeconds?: number;
  /** 状态：0-禁用，1-启用 */
  status: number;
  /** 提供商 ID */
  providerId: number;
  /** 提供商名称 */
  providerName?: string;
  /** 模型能力列表（如 TEXT_TO_IMAGE, IMAGE_TO_VIDEO 等） */
  capabilities?: string[];
  /** 备注说明 */
  remark?: string;
};

/** AIGC 模型参数模板 */
export type AigcModelParamTemplate = {
  /** 参数模板 ID */
  id: number;
  /** 所属模型 ID */
  modelId: number;
  /** 能力类型（如 TEXT_TO_IMAGE） */
  capability: string;
  /** 参数键名 */
  paramKey: string;
  /** 参数显示名称 */
  paramName: string;
  /** 参数类型（string, number, boolean 等） */
  paramType: string;
  /** 是否必填 */
  requiredStatus: boolean;
  /** 默认值 */
  defaultValue?: string;
  /** 可选值列表（下拉选择时使用） */
  options?: string[];
  /** 最小值（数值类型时） */
  minValue?: number;
  /** 最大值（数值类型时） */
  maxValue?: number;
  /** 正则表达式校验规则 */
  regexPattern?: string;
  /** 排序优先级 */
  sort: number;
  /** 状态：0-禁用，1-启用 */
  status: number;
};

export type AigcModelPriceCalculateReq = {
  modelId: number;
  capability: string;
  taskType?: string;
  params?: Record<string, unknown>;
};

/** AIGC 模型价格信息 */
export type AigcModelPrice = {
  /** 模型 ID */
  modelId: number;
  /** 能力类型 */
  capability: string;
  /** 货币类型（如 CNY, USD） */
  currencyType?: string;
  /** 成本价格 */
  costPrice: number;
  /** 销售价格 */
  salePrice: number;
  /** 计费单位（如 token, image, video 等） */
  billingUnit?: string;
  /** 价格来源 */
  priceSource?: string;
  /** 价格规则 ID */
  priceRuleId?: number;
  /** 价格详情（扩展字段） */
  priceDetail?: Record<string, unknown>;
};

export function getAigcModelList(type?: number, capability?: string) {
  const params = new URLSearchParams();
  if (typeof type === "number") params.set("type", String(type));
  if (capability) params.set("capability", capability);
  const query = params.size > 0 ? `?${params.toString()}` : "";
  return api.get<AigcModel[]>(`/aigc/model/list${query}`);
}

export function getAigcModelDetail(id: number) {
  return api.get<AigcModel>(`/aigc/model/get?id=${encodeURIComponent(id)}`);
}

export function getAigcModelParamList(modelId: number, capability: string) {
  return api.get<AigcModelParamTemplate[]>(
    `/aigc/model/param/list?modelId=${encodeURIComponent(modelId)}&capability=${encodeURIComponent(capability)}`
  );
}

export function calculateAigcModelPrice(data: AigcModelPriceCalculateReq) {
  return api.post<AigcModelPrice>("/aigc/model/price/calculate", data);
}
