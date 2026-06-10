package cn.iocoder.yudao.module.aigc.model.controller.admin.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - AIGC 模型新增/修改 Request VO")
@Data
public class AigcModelSaveReqVO {

    @Schema(description = "模型编号", example = "1024")
    private Long id;

    @Schema(description = "兼容字段：旧渠道商编号，新逻辑使用渠道实现", example = "1024")
    private Long providerId;

    @Schema(description = "模型编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "kling-v1")
    @NotBlank(message = "模型编码不能为空")
    private String code;

    @Schema(description = "模型名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "可灵视频模型")
    @NotBlank(message = "模型名称不能为空")
    private String name;

    @Schema(description = "兼容字段：旧渠道商模型标识，新逻辑使用渠道实现", example = "kling-v1")
    private String model;

    @Schema(description = "模型类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @jakarta.validation.constraints.NotNull(message = "模型类型不能为空")
    private Integer type;

    @Schema(description = "是否用户端展示", example = "true")
    private Boolean publicVisible;

    @Schema(description = "是否默认模型", example = "false")
    private Boolean defaultModel;

    @Schema(description = "排序", example = "1")
    private Integer sort;

    @Schema(description = "最大并发数", example = "10")
    private Integer maxConcurrent;

    @Schema(description = "超时时间，单位：秒", example = "60")
    private Integer timeoutSeconds;

    @Schema(description = "队列优先级", example = "1")
    private Integer queuePriority;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "备注", example = "主力模型")
    private String remark;

    @Schema(description = "模型能力列表", example = "TEXT_TO_IMAGE")
    private List<String> capabilities;

}
