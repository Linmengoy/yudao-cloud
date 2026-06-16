package cn.iocoder.yudao.module.aigc.community.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcCommunityAdminCommentPageReqVO extends PageParam {

    private Long postId;
    private Long userId;
    private String status;
    private String auditStatus;

}
