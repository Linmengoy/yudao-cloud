package cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "用户端 - AIGC 画布操作提交 Request VO")
@Data
public class AigcCanvasOperationSubmitReqVO {

    @Schema(description = "项目编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "项目编号不能为空")
    private Long projectId;
    @Schema(description = "客户端编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "客户端编号不能为空")
    private String clientId;
    @Schema(description = "操作编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "操作编号不能为空")
    private String opId;
    @Schema(description = "基于版本", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "基于版本不能为空")
    private Long baseVersion;
    @Schema(description = "操作类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "操作类型不能为空")
    private String operationType;
    @Schema(description = "操作 JSON", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "操作 JSON 不能为空")
    private String operationJson;
    @Schema(description = "反向操作 JSON")
    private String inverseOperationJson;

}
