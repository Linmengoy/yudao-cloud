package cn.iocoder.yudao.module.aigc.model.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface ErrorCodeConstants {

    ErrorCode MODEL_PROVIDER_NOT_EXISTS = new ErrorCode(1_041_000_000, "渠道商不存在");
    ErrorCode MODEL_PROVIDER_DISABLED = new ErrorCode(1_041_000_001, "渠道商已禁用");
    ErrorCode MODEL_PROVIDER_CODE_DUPLICATE = new ErrorCode(1_041_000_002, "渠道商编码重复");
    ErrorCode MODEL_PROVIDER_HAS_MODEL = new ErrorCode(1_041_000_003, "渠道商下存在模型，无法删除");
    ErrorCode MODEL_PROXY_NOT_EXISTS = new ErrorCode(1_041_000_004, "代理不存在");
    ErrorCode MODEL_PROXY_NAME_DUPLICATE = new ErrorCode(1_041_000_005, "代理名称重复");
    ErrorCode MODEL_PROXY_HAS_PROVIDER = new ErrorCode(1_041_000_006, "代理已被渠道商使用，无法删除");
    ErrorCode MODEL_PROXY_CONFIG_INVALID = new ErrorCode(1_041_000_007, "代理配置不正确");

    ErrorCode MODEL_NOT_EXISTS = new ErrorCode(1_041_001_000, "模型不存在");
    ErrorCode MODEL_DISABLED = new ErrorCode(1_041_001_001, "模型已禁用");
    ErrorCode MODEL_CODE_DUPLICATE = new ErrorCode(1_041_001_002, "模型编码重复");
    ErrorCode MODEL_CAPABILITY_NOT_SUPPORTED = new ErrorCode(1_041_001_003, "模型能力不支持");
    ErrorCode MODEL_CAPABILITY_INVALID = new ErrorCode(1_041_001_004, "模型能力配置不合法");
    ErrorCode MODEL_NOT_AUTHORIZED = new ErrorCode(1_041_001_005, "模型未授权");

    ErrorCode MODEL_PARAM_NOT_EXISTS = new ErrorCode(1_041_002_000, "模型参数不存在");
    ErrorCode MODEL_PARAM_INVALID = new ErrorCode(1_041_002_001, "模型参数不合法");
    ErrorCode MODEL_PARAM_CODE_DUPLICATE = new ErrorCode(1_041_002_002, "模型参数编码重复");
    ErrorCode MODEL_PARAM_TEMPLATE_NOT_EXISTS = new ErrorCode(1_041_002_003, "模型参数模板不存在");
    ErrorCode MODEL_PARAM_KEY_DUPLICATE = new ErrorCode(1_041_002_004, "模型参数键重复");
    ErrorCode MODEL_PARAM_REQUIRED = new ErrorCode(1_041_002_005, "模型参数{}必填");
    ErrorCode MODEL_PARAM_TYPE_ERROR = new ErrorCode(1_041_002_006, "模型参数{}类型错误，应为{}");
    ErrorCode MODEL_PARAM_RANGE_ERROR = new ErrorCode(1_041_002_007, "模型参数{}超出范围[{}, {}]");
    ErrorCode MODEL_PARAM_OPTION_ERROR = new ErrorCode(1_041_002_008, "模型参数{}选项不正确");
    ErrorCode MODEL_PARAM_FORMAT_ERROR = new ErrorCode(1_041_002_009, "模型参数{}格式不正确");

    ErrorCode MODEL_PRICE_NOT_EXISTS = new ErrorCode(1_041_003_000, "模型价格未配置");
    ErrorCode MODEL_PRICE_INVALID = new ErrorCode(1_041_003_001, "模型价格配置不合法");
    ErrorCode MODEL_PRICE_NOT_FOUND = new ErrorCode(1_041_003_002, "模型价格不存在");
    ErrorCode MODEL_PRICE_DUPLICATE = new ErrorCode(1_041_003_003, "模型价格配置重复");

    ErrorCode MODEL_ROUTE_NOT_EXISTS = new ErrorCode(1_041_004_000, "模型路由不存在");

    ErrorCode MODEL_TENANT_NOT_EXISTS = new ErrorCode(1_041_005_000, "租户模型授权不存在");

}
