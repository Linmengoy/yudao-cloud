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
    ErrorCode WORKFLOW_CURRENT_VERSION_NOT_EXISTS = new ErrorCode(1_046_000_012, "工作流当前版本不存在");
    ErrorCode WORKFLOW_VERSION_NODE_EMPTY = new ErrorCode(1_046_000_013, "工作流版本没有节点");
    ErrorCode WORKFLOW_EDGE_NOT_EXISTS = new ErrorCode(1_046_000_014, "工作流连线不存在");
    ErrorCode WORKFLOW_NODE_KEY_EXISTS = new ErrorCode(1_046_000_015, "工作流节点键已存在");
    ErrorCode WORKFLOW_EDGE_KEY_EXISTS = new ErrorCode(1_046_000_016, "工作流连线键已存在");
    ErrorCode WORKFLOW_GRAPH_INVALID = new ErrorCode(1_046_000_017, "工作流图结构无效");
    ErrorCode CANVAS_PROJECT_NOT_EXISTS = new ErrorCode(1_046_000_100, "画布项目不存在");
    ErrorCode CANVAS_NO_PERMISSION = new ErrorCode(1_046_000_101, "无权访问该画布项目");
    ErrorCode CANVAS_SNAPSHOT_NOT_EXISTS = new ErrorCode(1_046_000_102, "画布快照不存在");
    ErrorCode CANVAS_OPERATION_TYPE_INVALID = new ErrorCode(1_046_000_103, "画布操作类型不支持");
    ErrorCode CANVAS_OPERATION_PAYLOAD_INVALID = new ErrorCode(1_046_000_104, "画布操作内容不合法");
    ErrorCode CANVAS_OPERATION_PAYLOAD_TOO_LARGE = new ErrorCode(1_046_000_105, "画布操作内容过大");
    ErrorCode CANVAS_SNAPSHOT_VERSION_CONFLICT = new ErrorCode(1_046_000_106, "画布快照版本已过期，请同步后重试");
    ErrorCode CANVAS_MEMBER_NOT_EXISTS = new ErrorCode(1_046_000_107, "画布项目成员不存在");
    ErrorCode CANVAS_MEMBER_ROLE_INVALID = new ErrorCode(1_046_000_108, "画布项目成员角色不支持");
    ErrorCode CANVAS_MEMBER_EXISTS = new ErrorCode(1_046_000_109, "画布项目成员已存在");
    ErrorCode CANVAS_OWNER_CAN_NOT_CHANGE = new ErrorCode(1_046_000_110, "画布项目拥有者不能被修改或移除");
    ErrorCode CANVAS_PROJECT_RECYCLE_BIN_NOT_EXISTS = new ErrorCode(1_046_000_111, "画布项目回收站记录不存在");

    ErrorCode CANVAS_INVITE_USER_NOT_EXISTS = new ErrorCode(1_046_000_112, "被邀请用户不存在");
    ErrorCode CANVAS_INVITE_USER_DISABLED = new ErrorCode(1_046_000_113, "被邀请用户已禁用");
    ErrorCode CANVAS_INVITE_SELF_NOT_ALLOWED = new ErrorCode(1_046_000_114, "不能邀请自己加入画布项目");
    ErrorCode CANVAS_MEMBER_SEARCH_KEYWORD_REQUIRED = new ErrorCode(1_046_000_115, "请输入要搜索的用户信息");
    ErrorCode CANVAS_NODE_RUN_TASK_NOT_EXISTS = new ErrorCode(1_046_000_116, "生成任务不存在");
    ErrorCode CANVAS_NODE_RUN_TASK_NOT_BELONG = new ErrorCode(1_046_000_117, "生成任务不属于当前画布节点");
    ErrorCode CANVAS_NODE_RUN_INPUT_PARAMS_INVALID = new ErrorCode(1_046_000_118, "输入参数必须是 JSON 对象");
    ErrorCode CANVAS_NODE_RUN_REFERENCE_IMAGE_REQUIRED = new ErrorCode(1_046_000_119, "图生图缺少可用参考图，请重新选择或上传参考图后重试");
    ErrorCode CANVAS_NODE_RUN_REFERENCE_IMAGE_INVALID = new ErrorCode(1_046_000_120, "参考图不可访问或格式不支持");

}
