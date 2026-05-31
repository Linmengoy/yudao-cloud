package cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "用户端 - AIGC 画布节点运行 Request VO")
@Data
public class AigcCanvasNodeRunReqVO {

    @Schema(description = "项目编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "项目编号不能为空")
    private Long projectId;

    @Schema(description = "节点编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "节点编号不能为空")
    private String nodeId;

    @Schema(description = "客户端编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "客户端编号不能为空")
    private String clientId;

    @Schema(description = "客户端基于版本", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "客户端基于版本不能为空")
    private Long baseVersion;

    @Schema(description = "客户端运行编号")
    private String runId;

    @Schema(description = "节点类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "节点类型不能为空")
    private String nodeType;

    @Schema(description = "生成类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "生成类型不能为空")
    private String generateType;

    @Schema(description = "生成模式", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "生成模式不能为空")
    private String generateMode;

    @Schema(description = "模型编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "模型编号不能为空")
    private Long modelId;

    @Schema(description = "提示词")
    private String prompt;

    @Schema(description = "输入参数 JSON")
    private String inputParams;

    @Schema(description = "同步生成")
    private Boolean sync;

}
