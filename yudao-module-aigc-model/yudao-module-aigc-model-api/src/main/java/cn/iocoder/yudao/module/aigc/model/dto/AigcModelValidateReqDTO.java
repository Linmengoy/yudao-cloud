package cn.iocoder.yudao.module.aigc.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Schema(description = "RPC 服务 - AIGC 模型参数校验 Request DTO")
@Data
public class AigcModelValidateReqDTO {

    @Schema(description = "模型编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long modelId;

    @Schema(description = "模型能力", requiredMode = Schema.RequiredMode.REQUIRED, example = "TEXT_TO_IMAGE")
    private String capability;

    @Schema(description = "调用参数")
    private Map<String, Object> params;

}
