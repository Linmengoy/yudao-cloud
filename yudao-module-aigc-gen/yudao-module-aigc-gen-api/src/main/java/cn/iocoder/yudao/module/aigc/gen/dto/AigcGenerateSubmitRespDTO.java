package cn.iocoder.yudao.module.aigc.gen.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "RPC 服务 - AIGC 生成提交 Response DTO")
@Data
@Accessors(chain = true)
public class AigcGenerateSubmitRespDTO {

    @Schema(description = "生成记录编号", example = "1024")
    private Long id;

    @Schema(description = "任务编号", example = "2048")
    private Long taskId;

    @Schema(description = "生成流水号", example = "GEN202605260001")
    private String generateNo;

    @Schema(description = "状态", example = "CREATED")
    private String status;

}
