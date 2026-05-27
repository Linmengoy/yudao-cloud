package cn.iocoder.yudao.module.aigc.workflow.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface ErrorCodeConstants {

    ErrorCode WORKFLOW_DEFINITION_NOT_EXISTS = new ErrorCode(1_046_000_000, "工作流定义不存在");
    ErrorCode WORKFLOW_VERSION_NOT_EXISTS = new ErrorCode(1_046_000_001, "工作流版本不存在");
    ErrorCode WORKFLOW_INSTANCE_NOT_EXISTS = new ErrorCode(1_046_000_002, "工作流实例不存在");
    ErrorCode WORKFLOW_NODE_NOT_EXISTS = new ErrorCode(1_046_000_003, "工作流节点不存在");
    ErrorCode WORKFLOW_NODE_INSTANCE_NOT_EXISTS = new ErrorCode(1_046_000_004, "工作流节点实例不存在");
    ErrorCode WORKFLOW_DEFINITION_CODE_EXISTS = new ErrorCode(1_046_000_005, "工作流编码已存在");
    ErrorCode WORKFLOW_STATUS_INVALID = new ErrorCode(1_046_000_006, "工作流状态不允许当前操作");
    ErrorCode WORKFLOW_INSTANCE_STATUS_INVALID = new ErrorCode(1_046_000_007, "工作流实例状态不允许当前操作");
    ErrorCode WORKFLOW_NODE_STATUS_INVALID = new ErrorCode(1_046_000_008, "工作流节点状态不允许当前操作");
    ErrorCode WORKFLOW_NODE_TYPE_UNSUPPORTED = new ErrorCode(1_046_000_009, "工作流节点类型不支持");
    ErrorCode WORKFLOW_NO_EXECUTABLE_NODE = new ErrorCode(1_046_000_010, "工作流没有可执行节点");
    ErrorCode WORKFLOW_NO_PERMISSION = new ErrorCode(1_046_000_011, "无权访问该工作流");

}
