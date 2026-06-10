package cn.iocoder.yudao.module.aigc.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "RPC 服务 - AIGC 模型提交预处理 Response DTO")
@Data
public class AigcModelSubmitPrepareRespDTO {

    @Schema(description = "模型信息")
    private AigcModelRespDTO model;

    @Schema(description = "渠道商信息")
    private AigcModelProviderRespDTO provider;

    @Schema(description = "价格信息")
    private AigcModelPriceCalculateRespDTO price;

}
