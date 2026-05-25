package cn.iocoder.yudao.module.aigc.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Schema(description = "RPC 服务 - AIGC 模型价格计算 Request DTO")
@Data
public class AigcModelPriceCalculateReqDTO {

    @Schema(description = "模型编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long modelId;

    @Schema(description = "模型能力", requiredMode = Schema.RequiredMode.REQUIRED, example = "TEXT_TO_IMAGE")
    private String capability;

    @Schema(description = "任务类型", example = "image")
    private String taskType;

    @Schema(description = "计价参数")
    private Map<String, Object> params;

}
