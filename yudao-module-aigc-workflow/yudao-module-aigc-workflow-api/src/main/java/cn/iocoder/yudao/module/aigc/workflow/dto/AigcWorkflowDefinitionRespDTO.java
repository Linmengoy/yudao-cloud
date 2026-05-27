package cn.iocoder.yudao.module.aigc.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "RPC 服务 - AIGC 工作流定义 Response DTO")
@Data
@Accessors(chain = true)
public class AigcWorkflowDefinitionRespDTO {

    @Schema(description = "工作流编号", example = "1024")
    private Long id;

    @Schema(description = "工作流名称")
    private String name;

    @Schema(description = "工作流编码")
    private String code;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "封面")
    private String coverUrl;

    @Schema(description = "分类编号", example = "2048")
    private Long categoryId;

    @Schema(description = "可见性", example = "PRIVATE")
    private String visibility;

    @Schema(description = "状态", example = "PUBLISHED")
    private String status;

    @Schema(description = "当前版本编号", example = "4096")
    private Long currentVersionId;

    @Schema(description = "输入参数 Schema JSON")
    private String inputSchema;

    @Schema(description = "输出参数 Schema JSON")
    private String outputSchema;

    @Schema(description = "工作流配置 JSON")
    private String config;

    @Schema(description = "创建用户", example = "1024")
    private Long creatorUserId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
