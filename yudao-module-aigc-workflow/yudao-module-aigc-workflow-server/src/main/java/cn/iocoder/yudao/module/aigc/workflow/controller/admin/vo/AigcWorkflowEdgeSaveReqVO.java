package cn.iocoder.yudao.module.aigc.workflow.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - AIGC 工作流连线新增/修改 Request VO")
@Data
public class AigcWorkflowEdgeSaveReqVO {

    @Schema(description = "连线编号", example = "1024")
    private Long id;

    @Schema(description = "工作流编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "工作流编号不能为空")
    private Long workflowId;

    @Schema(description = "版本编号；为空表示草稿连线", example = "4096")
    private Long versionId;

    @Schema(description = "连线唯一键", requiredMode = Schema.RequiredMode.REQUIRED, example = "edge_start_image")
    @NotBlank(message = "连线唯一键不能为空")
    private String edgeKey;

    @Schema(description = "上游节点键", requiredMode = Schema.RequiredMode.REQUIRED, example = "start")
    @NotBlank(message = "上游节点键不能为空")
    private String sourceNodeKey;

    @Schema(description = "下游节点键", requiredMode = Schema.RequiredMode.REQUIRED, example = "image_1")
    @NotBlank(message = "下游节点键不能为空")
    private String targetNodeKey;

    @Schema(description = "条件配置 JSON")
    private String conditionConfig;

    @Schema(description = "入参映射 JSON")
    private String inputMapping;

}
