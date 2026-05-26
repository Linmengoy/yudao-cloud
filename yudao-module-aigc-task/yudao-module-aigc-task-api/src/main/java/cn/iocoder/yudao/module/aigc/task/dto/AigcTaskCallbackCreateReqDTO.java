package cn.iocoder.yudao.module.aigc.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "RPC 服务 - AIGC 任务回调创建 Request DTO")
@Data
public class AigcTaskCallbackCreateReqDTO {

    @Schema(description = "任务编号", example = "1024")
    private Long taskId;

    @Schema(description = "平台任务编号", example = "TASK202605260001")
    private String taskNo;

    @Schema(description = "渠道商编号", example = "2048")
    private Long providerId;

    @Schema(description = "渠道商编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "openai")
    @NotBlank(message = "渠道商编码不能为空")
    private String providerCode;

    @Schema(description = "第三方任务编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "第三方任务编号不能为空")
    private String externalTaskId;

    @Schema(description = "回调类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "回调类型不能为空")
    private String callbackType;

    @Schema(description = "原始回调内容")
    private String rawBody;

    @Schema(description = "请求头 JSON")
    private String headers;

    @Schema(description = "签名")
    private String signature;

}
