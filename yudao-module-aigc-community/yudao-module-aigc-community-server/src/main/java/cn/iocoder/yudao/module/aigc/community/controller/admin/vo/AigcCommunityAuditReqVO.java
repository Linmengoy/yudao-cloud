package cn.iocoder.yudao.module.aigc.community.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "Admin - Community audit request")
@Data
public class AigcCommunityAuditReqVO {

    @Schema(description = "Object ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "ID cannot be null")
    private Long id;

    @Schema(description = "Reason")
    private String reason;

}
