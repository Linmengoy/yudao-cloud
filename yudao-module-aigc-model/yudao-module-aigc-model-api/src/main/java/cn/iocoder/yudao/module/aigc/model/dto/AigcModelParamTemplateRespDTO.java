package cn.iocoder.yudao.module.aigc.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "RPC 服务 - AIGC 模型参数模板 Response DTO")
@Data
public class AigcModelParamTemplateRespDTO {

    @Schema(description = "参数模板编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "模型编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long modelId;

    @Schema(description = "模型能力", requiredMode = Schema.RequiredMode.REQUIRED, example = "TEXT_TO_IMAGE")
    private String capability;

    @Schema(description = "参数键", requiredMode = Schema.RequiredMode.REQUIRED, example = "prompt")
    private String paramKey;

    @Schema(description = "参数名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "提示词")
    private String paramName;

    @Schema(description = "参数类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "STRING")
    private String paramType;

    @Schema(description = "是否必填", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean requiredStatus;

    @Schema(description = "默认值")
    private String defaultValue;

    @Schema(description = "可选项")
    private List<String> options;

    @Schema(description = "最小值", example = "1")
    private BigDecimal minValue;

    @Schema(description = "最大值", example = "100")
    private BigDecimal maxValue;

    @Schema(description = "正则表达式")
    private String regexPattern;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer sort;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

}
