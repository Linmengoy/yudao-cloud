package cn.iocoder.yudao.module.aigc.gen.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "RPC 服务 - AIGC 生成回调 Request DTO")
@Data
@Accessors(chain = true)
public class AigcGenerateCallbackReqDTO {

    @Schema(description = "渠道编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "openai")
    @NotBlank(message = "渠道编码不能为空")
    private String providerCode;

    @Schema(description = "第三方任务编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "第三方任务编号不能为空")
    private String providerTaskId;

    @Schema(description = "回调编号")
    private String callbackNo;

    @Schema(description = "回调类型")
    private String callbackType;

    @Schema(description = "回调原文")
    private String rawBody;

    @Schema(description = "渠道签名")
    private String signature;

    @Schema(description = "回调结果状态", example = "SUCCESS")
    private String resultStatus;

    @Schema(description = "文本输出")
    private String outputText;

    @Schema(description = "结构化输出 JSON")
    private String outputData;

    @Schema(description = "结果 URL JSON")
    private String outputUrls;

    @Schema(description = "失败原因")
    private String failReason;

}
