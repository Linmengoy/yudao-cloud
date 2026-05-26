package cn.iocoder.yudao.module.aigc.gen.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface ErrorCodeConstants {

    ErrorCode GENERATE_RECORD_NOT_EXISTS = new ErrorCode(1_045_000_000, "生成记录不存在");
    ErrorCode GENERATE_RECORD_NO_DUPLICATE = new ErrorCode(1_045_000_001, "生成流水号已存在");
    ErrorCode GENERATE_RECORD_STATUS_INVALID = new ErrorCode(1_045_000_002, "生成记录状态不正确");
    ErrorCode GENERATE_PROMPT_NOT_PASS = new ErrorCode(1_045_000_003, "提示词安全检查不通过");
    ErrorCode GENERATE_PROVIDER_CALLBACK_INVALID = new ErrorCode(1_045_000_004, "生成回调签名不正确");
    ErrorCode GENERATE_PROVIDER_RESULT_INVALID = new ErrorCode(1_045_000_005, "第三方生成结果不正确");
    ErrorCode GENERATE_PROVIDER_CONFIG_INVALID = new ErrorCode(1_045_000_006, "第三方渠道配置不正确");
    ErrorCode GENERATE_PROVIDER_REQUEST_FAILED = new ErrorCode(1_045_000_007, "第三方渠道请求失败");
    ErrorCode GENERATE_CALLBACK_NOT_EXISTS = new ErrorCode(1_045_001_000, "生成回调记录不存在");
    ErrorCode GENERATE_CALLBACK_DUPLICATE = new ErrorCode(1_045_001_001, "生成回调记录已存在");
    ErrorCode GENERATE_PROVIDER_LOG_NOT_EXISTS = new ErrorCode(1_045_002_000, "渠道调用日志不存在");

}
