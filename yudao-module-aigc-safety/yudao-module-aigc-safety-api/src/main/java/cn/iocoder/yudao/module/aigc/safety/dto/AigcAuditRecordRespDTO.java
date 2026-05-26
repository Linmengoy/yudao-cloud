package cn.iocoder.yudao.module.aigc.safety.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "RPC 服务 - AIGC 审核记录 Response DTO")
@Data
public class AigcAuditRecordRespDTO {

    @Schema(description = "审核记录编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "审核对象类型", example = "ASSET")
    private String objectType;

    @Schema(description = "审核对象编号", example = "1024")
    private Long objectId;

    @Schema(description = "被审核内容摘要")
    private String contentSummary;

    @Schema(description = "审核场景", example = "ASSET")
    private String scene;

    @Schema(description = "审核状态", example = "PENDING")
    private String auditStatus;

    @Schema(description = "审核结果", example = "AUTO_PASS")
    private String auditResult;

    @Schema(description = "命中敏感词 JSON")
    private String hitWords;

    @Schema(description = "风险等级", example = "3")
    private Integer riskLevel;

    @Schema(description = "拒绝原因")
    private String rejectReason;

    @Schema(description = "审核人用户编号", example = "1024")
    private Long auditorUserId;

    @Schema(description = "审核时间")
    private LocalDateTime auditTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
