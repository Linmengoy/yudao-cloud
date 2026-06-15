package cn.iocoder.yudao.module.aigc.community.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface ErrorCodeConstants {

    ErrorCode COMMUNITY_POST_NOT_EXISTS = new ErrorCode(1_046_000_000, "Community post does not exist");
    ErrorCode COMMUNITY_POST_NOT_VISIBLE = new ErrorCode(1_046_000_001, "Community post is not visible");
    ErrorCode COMMUNITY_POST_INTERACTION_DISABLED = new ErrorCode(1_046_000_002, "Community post does not allow interactions");
    ErrorCode COMMUNITY_POST_AUDIT_REASON_EMPTY = new ErrorCode(1_046_000_003, "Audit reason cannot be empty");
    ErrorCode COMMUNITY_POST_SOURCE_EMPTY = new ErrorCode(1_046_000_004, "Asset or project is required");
    ErrorCode COMMUNITY_POST_SOURCE_INVALID = new ErrorCode(1_046_000_005, "Community source asset is unavailable");
    ErrorCode COMMUNITY_POST_SOURCE_NO_PERMISSION = new ErrorCode(1_046_000_006, "No permission to publish this community source");
    ErrorCode COMMUNITY_COMMENT_NOT_EXISTS = new ErrorCode(1_046_001_000, "Community comment does not exist");
    ErrorCode COMMUNITY_COMMENT_NO_PERMISSION = new ErrorCode(1_046_001_001, "No permission to operate this comment");
    ErrorCode COMMUNITY_COMMENT_CONTENT_EMPTY = new ErrorCode(1_046_001_002, "Comment content cannot be empty");
    ErrorCode COMMUNITY_FOLLOW_SELF = new ErrorCode(1_046_002_000, "Cannot follow yourself");
    ErrorCode COMMUNITY_AUTHOR_NOT_EXISTS = new ErrorCode(1_046_002_001, "Author does not exist");

}
