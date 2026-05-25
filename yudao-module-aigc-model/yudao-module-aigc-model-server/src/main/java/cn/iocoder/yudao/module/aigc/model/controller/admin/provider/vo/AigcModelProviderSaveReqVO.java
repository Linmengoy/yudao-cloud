package cn.iocoder.yudao.module.aigc.model.controller.admin.provider.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - AIGC 模型渠道商新增/修改 Request VO")
@Data
public class AigcModelProviderSaveReqVO {

    @Schema(description = "渠道商编号", example = "1024")
    private Long id;

    @Schema(description = "渠道商编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "openai")
    @NotBlank(message = "渠道商编码不能为空")
    private String code;

    @Schema(description = "渠道商名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "OpenAI")
    @NotBlank(message = "渠道商名称不能为空")
    private String name;

    @Schema(description = "API 地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://api.openai.com")
    @NotBlank(message = "API地址不能为空")
    private String apiBaseUrl;

    @Schema(description = "鉴权方式", requiredMode = Schema.RequiredMode.REQUIRED, example = "BEARER")
    @NotBlank(message = "鉴权方式不能为空")
    private String authType;

    @Schema(description = "API Key")
    private String apiKey;

    @Schema(description = "Secret Key")
    private String secretKey;

    @Schema(description = "扩展配置")
    private String extraConfig;

    @Schema(description = "超时时间，单位：秒", example = "60")
    private Integer timeoutSeconds;

    @Schema(description = "限流配置")
    private String rateLimitConfig;

    @Schema(description = "健康状态", example = "HEALTHY")
    private String healthStatus;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "备注", example = "主渠道")
    private String remark;

}
