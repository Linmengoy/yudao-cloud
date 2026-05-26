package cn.iocoder.yudao.module.aigc.asset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "RPC 服务 - AIGC 资产创建 Response DTO")
@Data
@Accessors(chain = true)
public class AigcAssetCreateRespDTO {

    @Schema(description = "资产编号", example = "1024")
    private Long id;

    @Schema(description = "资产流水号", example = "AST202605260001")
    private String assetNo;

    @Schema(description = "资产类型", example = "IMAGE")
    private String assetType;

    @Schema(description = "文件 URL")
    private String fileUrl;

}
