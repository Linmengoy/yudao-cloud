package cn.iocoder.yudao.module.aigc.community.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Schema(description = "Admin - Community post page request")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcCommunityAdminPostPageReqVO extends PageParam {

    private Long authorUserId;
    private String title;
    private String assetType;
    private String publishStatus;
    private String auditStatus;
    @Schema(description = "Create time range")
    private LocalDateTime[] createTime;

}
