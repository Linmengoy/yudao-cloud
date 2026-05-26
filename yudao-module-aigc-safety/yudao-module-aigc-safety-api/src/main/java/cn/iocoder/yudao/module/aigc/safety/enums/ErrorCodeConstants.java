package cn.iocoder.yudao.module.aigc.safety.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface ErrorCodeConstants {

    ErrorCode SENSITIVE_WORD_NOT_EXISTS = new ErrorCode(1_044_000_000, "敏感词不存在");
    ErrorCode SENSITIVE_WORD_DUPLICATE = new ErrorCode(1_044_000_001, "敏感词已存在");
    ErrorCode SENSITIVE_WORD_STATUS_INVALID = new ErrorCode(1_044_000_002, "敏感词状态不正确");
    ErrorCode SENSITIVE_WORD_SCENE_INVALID = new ErrorCode(1_044_000_003, "敏感词场景不正确");
    ErrorCode SENSITIVE_WORD_MATCH_TYPE_INVALID = new ErrorCode(1_044_000_004, "敏感词匹配方式不正确");
    ErrorCode AUDIT_RECORD_NOT_EXISTS = new ErrorCode(1_044_001_000, "审核记录不存在");
    ErrorCode AUDIT_RECORD_STATUS_INVALID = new ErrorCode(1_044_001_001, "审核记录状态不允许当前操作");
    ErrorCode AUDIT_REJECT_REASON_EMPTY = new ErrorCode(1_044_001_002, "拒绝原因不能为空");
    ErrorCode AUDIT_OBJECT_TYPE_INVALID = new ErrorCode(1_044_001_003, "审核对象类型不正确");
    ErrorCode AUDIT_SCENE_INVALID = new ErrorCode(1_044_001_004, "审核场景不正确");
    ErrorCode AUDIT_RESULT_INVALID = new ErrorCode(1_044_001_005, "审核结果不正确");
    ErrorCode PROMPT_SAFETY_CHECK_NOT_PASS = new ErrorCode(1_044_002_000, "提示词安全检查不通过");

}
