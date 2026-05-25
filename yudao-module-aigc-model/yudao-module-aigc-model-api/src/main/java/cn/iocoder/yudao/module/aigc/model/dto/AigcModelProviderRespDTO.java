package cn.iocoder.yudao.module.aigc.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "RPC 服务 - AIGC 模型渠道商 Response DTO")
@Data
public class AigcModelProviderRespDTO {

    @Schema(description = "渠道商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "渠道商编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "openai")
    private String code;

    @Schema(description = "渠道商名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "OpenAI")
    private String name;

    @Schema(description = "API 地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://api.openai.com")
    private String apiBaseUrl;

    @Schema(description = "鉴权方式", requiredMode = Schema.RequiredMode.REQUIRED, example = "BEARER")
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

    @Schema(description = "余额", example = "100.00")
    private BigDecimal balance;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "备注", example = "主渠道")
    private String remark;

}
