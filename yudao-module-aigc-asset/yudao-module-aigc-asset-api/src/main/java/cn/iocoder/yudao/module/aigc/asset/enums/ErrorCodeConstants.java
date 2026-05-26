package cn.iocoder.yudao.module.aigc.asset.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface ErrorCodeConstants {

    ErrorCode ASSET_NOT_EXISTS = new ErrorCode(1_043_000_000, "资产不存在");
    ErrorCode ASSET_NO_PERMISSION = new ErrorCode(1_043_000_001, "无权访问该资产");
    ErrorCode ASSET_STATUS_INVALID = new ErrorCode(1_043_000_002, "资产状态不允许当前操作");
    ErrorCode ASSET_AUDIT_NOT_PASS = new ErrorCode(1_043_000_003, "资产审核未通过");
    ErrorCode ASSET_FILE_EMPTY = new ErrorCode(1_043_000_004, "资产文件为空");
    ErrorCode ASSET_FILE_TYPE_UNSUPPORTED = new ErrorCode(1_043_000_005, "不支持的文件类型");
    ErrorCode ASSET_FILE_SIZE_EXCEED = new ErrorCode(1_043_000_006, "文件大小超限");
    ErrorCode ASSET_UPLOAD_FAILED = new ErrorCode(1_043_000_007, "文件上传失败");
    ErrorCode ASSET_DOWNLOAD_FAILED = new ErrorCode(1_043_000_008, "文件下载失败");
    ErrorCode ASSET_CREATE_DUPLICATE = new ErrorCode(1_043_000_009, "资产重复创建");
    ErrorCode ASSET_TASK_NOT_EXISTS = new ErrorCode(1_043_000_010, "来源任务不存在");
    ErrorCode ASSET_TASK_STATUS_INVALID = new ErrorCode(1_043_000_011, "来源任务状态不允许创建资产");
    ErrorCode ASSET_RELATION_EXISTS = new ErrorCode(1_043_001_000, "资产关系已存在");
    ErrorCode ASSET_VERSION_NOT_EXISTS = new ErrorCode(1_043_002_000, "资产版本不存在");

}
