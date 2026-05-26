package cn.iocoder.yudao.module.aigc.task.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface ErrorCodeConstants {

    ErrorCode TASK_NOT_EXISTS = new ErrorCode(1_041_200_000, "任务不存在");
    ErrorCode TASK_NO_DUPLICATE = new ErrorCode(1_041_200_001, "任务编号重复");
    ErrorCode TASK_STATUS_INVALID = new ErrorCode(1_041_200_002, "任务状态不合法");
    ErrorCode TASK_STATUS_TRANSFER_INVALID = new ErrorCode(1_041_200_003, "任务状态流转不合法");
    ErrorCode TASK_NOT_OWNER = new ErrorCode(1_041_200_004, "无权访问该任务");
    ErrorCode TASK_CANCEL_NOT_ALLOWED = new ErrorCode(1_041_200_005, "当前状态不允许取消");
    ErrorCode TASK_RETRY_NOT_ALLOWED = new ErrorCode(1_041_200_006, "当前状态不允许重试");

    ErrorCode TASK_CALLBACK_NOT_EXISTS = new ErrorCode(1_041_202_000, "回调记录不存在");
    ErrorCode TASK_CALLBACK_DUPLICATE = new ErrorCode(1_041_202_001, "回调记录重复");
    ErrorCode TASK_CALLBACK_PROCESS_FAILED = new ErrorCode(1_041_202_002, "回调处理失败");

    ErrorCode TASK_RETRY_NOT_EXISTS = new ErrorCode(1_041_203_000, "重试记录不存在");
    ErrorCode TASK_RETRY_EXCEED_LIMIT = new ErrorCode(1_041_203_001, "超过最大重试次数");

    ErrorCode TASK_COMPENSATE_FAILED = new ErrorCode(1_041_204_000, "任务补偿失败");

}
