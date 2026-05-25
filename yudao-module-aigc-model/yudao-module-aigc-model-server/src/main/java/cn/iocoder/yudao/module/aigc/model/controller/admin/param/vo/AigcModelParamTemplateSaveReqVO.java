package cn.iocoder.yudao.module.aigc.model.controller.admin.param.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AigcModelParamTemplateSaveReqVO {

    private Long id;

    @NotNull(message = "模型ID不能为空")
    private Long modelId;

    @NotBlank(message = "能力不能为空")
    private String capability;

    @NotBlank(message = "参数键不能为空")
    private String paramKey;

    @NotBlank(message = "参数名称不能为空")
    private String paramName;

    @NotBlank(message = "参数类型不能为空")
    private String paramType;

    private Boolean requiredStatus;

    private String defaultValue;

    private String options;

    private BigDecimal minValue;

    private BigDecimal maxValue;

    private String regexPattern;

    private Integer sort;

    private Integer status;

}