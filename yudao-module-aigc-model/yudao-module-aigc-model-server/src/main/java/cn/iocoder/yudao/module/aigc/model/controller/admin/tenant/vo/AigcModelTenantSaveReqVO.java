package cn.iocoder.yudao.module.aigc.model.controller.admin.tenant.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - AIGC 租户模型授权新增/修改 Request VO")
@Data
public class AigcModelTenantSaveReqVO {

    @Schema(description = "租户模型授权编号", example = "1024")
    private Long id;

    @Schema(description = "租户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "租户ID不能为空")
    private Long tenantId;

    @Schema(description = "模型编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "模型ID不能为空")
    private Long modelId;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;

    @Schema(description = "是否用户端展示", example = "true")
    private Boolean publicVisible;

    @Schema(description = "是否默认模型", example = "false")
    private Boolean defaultModel;

    @Schema(description = "排序", example = "1")
    private Integer sort;

    @Schema(description = "最大并发数", example = "10")
    private Integer maxConcurrent;

    @Schema(description = "每日调用限制", example = "1000")
    private Integer dailyLimit;

    @Schema(description = "备注", example = "VIP 租户授权")
    private String remark;

}
