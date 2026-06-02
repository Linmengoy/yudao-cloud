package cn.iocoder.yudao.module.aigc.gen.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Schema(description = "RPC 服务 - AIGC 生成提交 Request DTO")
@Data
@Accessors(chain = true)
public class AigcGenerateSubmitReqDTO {

    @Schema(description = "用户编号；用户端接口由登录态注入", example = "1024")
    private Long userId;

    @Schema(description = "客户端请求编号", example = "REQ202605260001")
    private String clientRequestId;

    @Schema(description = "生成类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "TEXT")
    @NotBlank(message = "生成类型不能为空")
    private String generateType;

    @Schema(description = "生成模式", requiredMode = Schema.RequiredMode.REQUIRED, example = "TEXT_TO_IMAGE")
    @NotBlank(message = "生成模式不能为空")
    private String generateMode;

    @Schema(description = "模型编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "模型编号不能为空")
    private Long modelId;

    @Schema(description = "渠道商编号", example = "2048")
    private Long providerId;

    @Schema(description = "提示词")
    private String prompt;

    @Schema(description = "输入参数 JSON")
    private String inputParams;

    @Schema(description = "同步生成", example = "true")
    private Boolean sync;

    @Schema(description = "结果 URL JSON")
    private String outputUrls;

    @Schema(description = "文本输出")
    private String outputText;

    @Schema(description = "销售价", example = "10.00")
    private BigDecimal priceAmount;

    @Schema(description = "成本价", example = "2.00")
    private BigDecimal costAmount;

    @Schema(description = "冻结记录编号", example = "4096")
    private Long freezeId;

}
