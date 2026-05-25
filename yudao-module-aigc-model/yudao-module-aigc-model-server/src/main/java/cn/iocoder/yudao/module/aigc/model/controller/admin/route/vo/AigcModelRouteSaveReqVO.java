package cn.iocoder.yudao.module.aigc.model.controller.admin.route.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - AIGC 模型路由规则新增/修改 Request VO")
@Data
public class AigcModelRouteSaveReqVO {

    @Schema(description = "路由规则编号", example = "1024")
    private Long id;

    @Schema(description = "路由名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "默认文生图路由")
    @NotBlank(message = "路由名称不能为空")
    private String name;

    @Schema(description = "任务类型", example = "image")
    private String taskType;

    @Schema(description = "模型能力", example = "TEXT_TO_IMAGE")
    private String capability;

    @Schema(description = "路由策略", requiredMode = Schema.RequiredMode.REQUIRED, example = "FIXED_MODEL")
    @NotBlank(message = "路由策略不能为空")
    private String strategy;

    @Schema(description = "模型编号集合")
    private String modelIds;

    @Schema(description = "用户等级", example = "VIP")
    private String userLevel;

    @Schema(description = "状态", example = "0")
    private Integer status;

}
