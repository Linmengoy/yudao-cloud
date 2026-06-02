import { api } from "@/lib/api-client";

export type AigcModel = {
  id: number;
  code: string;
  name: string;
  model: string;
  type: number;
  publicVisible: boolean;
  defaultModel: boolean;
  sort: number;
  maxConcurrent?: number;
  timeoutSeconds?: number;
  status: number;
  providerId: number;
  providerName?: string;
  capabilities?: string[];
  remark?: string;
};

export type AigcModelParamTemplate = {
  id: number;
  modelId: number;
  capability: string;
  paramKey: string;
  paramName: string;
  paramType: string;
  requiredStatus: boolean;
  defaultValue?: string;
  options?: string[];
  minValue?: number;
  maxValue?: number;
  regexPattern?: string;
  sort: number;
  status: number;
};

export type AigcModelPriceCalculateReq = {
  modelId: number;
  capability: string;
  taskType?: string;
  params?: Record<string, unknown>;
};

export type AigcModelPrice = {
  modelId: number;
  capability: string;
  currencyType?: string;
  costPrice: number;
  salePrice: number;
  billingUnit?: string;
  priceSource?: string;
  priceRuleId?: number;
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
