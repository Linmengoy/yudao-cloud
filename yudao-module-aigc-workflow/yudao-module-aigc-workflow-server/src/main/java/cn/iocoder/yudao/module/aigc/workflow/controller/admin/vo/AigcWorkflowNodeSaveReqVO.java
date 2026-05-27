package cn.iocoder.yudao.module.aigc.workflow.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - AIGC 工作流节点新增/修改 Request VO")
@Data
public class AigcWorkflowNodeSaveReqVO {

    @Schema(description = "节点编号", example = "1024")
    private Long id;

    @Schema(description = "工作流编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "工作流编号不能为空")
    private Long workflowId;

    @Schema(description = "版本编号；为空表示草稿节点", example = "4096")
    private Long versionId;

    @Schema(description = "节点唯一键", requiredMode = Schema.RequiredMode.REQUIRED, example = "image_1")
    @NotBlank(message = "节点唯一键不能为空")
    private String nodeKey;

    @Schema(description = "节点名称")
    private String nodeName;

    @Schema(description = "节点类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "IMAGE_GENERATE")
    @NotBlank(message = "节点类型不能为空")
    private String nodeType;

    @Schema(description = "生成类型", example = "IMAGE")
    private String generateType;

    @Schema(description = "生成模式", example = "TEXT_TO_IMAGE")
    private String generateMode;

    @Schema(description = "模型编号", example = "1024")
    private Long modelId;

    @Schema(description = "入参映射 JSON")
    private String inputMapping;

    @Schema(description = "出参映射 JSON")
    private String outputMapping;

    @Schema(description = "参数配置 JSON")
    private String paramConfig;

    @Schema(description = "重试配置 JSON")
    private String retryConfig;

    @Schema(description = "超时时间秒", example = "300")
    private Integer timeoutSeconds;

    @Schema(description = "画布位置 JSON")
    private String position;

}
