package cn.iocoder.yudao.module.aigc.safety.controller.admin.audit.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - AIGC 审核记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class AigcAuditRecordPageReqVO extends PageParam {

    @Schema(description = "审核对象类型", example = "ASSET")
    private String objectType;

    @Schema(description = "审核对象编号", example = "1024")
    private Long objectId;

    @Schema(description = "审核场景", example = "ASSET")
    private String scene;

    @Schema(description = "审核状态", example = "PENDING")
    private String auditStatus;

    @Schema(description = "审核结果", example = "AUTO_PASS")
    private String auditResult;

    @Schema(description = "风险等级", example = "3")
    private Integer riskLevel;

    @Schema(description = "创建时间")
    private LocalDateTime[] createTime;

}
