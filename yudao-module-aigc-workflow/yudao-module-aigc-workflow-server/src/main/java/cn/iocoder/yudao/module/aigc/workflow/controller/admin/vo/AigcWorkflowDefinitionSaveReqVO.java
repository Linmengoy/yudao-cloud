package cn.iocoder.yudao.module.aigc.workflow.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - AIGC 工作流定义新增/修改 Request VO")
@Data
public class AigcWorkflowDefinitionSaveReqVO {

    @Schema(description = "工作流编号", example = "1024")
    private Long id;

    @Schema(description = "工作流名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "工作流名称不能为空")
    private String name;

    @Schema(description = "工作流编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "工作流编码不能为空")
    private String code;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "封面")
    private String coverUrl;

    @Schema(description = "分类编号", example = "2048")
    private Long categoryId;

    @Schema(description = "可见性", example = "PRIVATE")
    private String visibility;

    @Schema(description = "输入参数 Schema JSON")
    private String inputSchema;

    @Schema(description = "输出参数 Schema JSON")
    private String outputSchema;

    @Schema(description = "工作流配置 JSON")
    private String config;

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getVisibility() {
        return visibility;
    }

}
