package cn.iocoder.yudao.module.aigc.gen.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "RPC 服务 - AIGC 生成结果 Response DTO")
@Data
@Accessors(chain = true)
public class AigcGenerateResultRespDTO {

    @Schema(description = "生成记录编号", example = "1024")
    private Long id;

    @Schema(description = "任务编号", example = "2048")
    private Long taskId;

    @Schema(description = "生成流水号")
    private String generateNo;

    @Schema(description = "生成类型")
    private String generateType;

    @Schema(description = "生成模式")
    private String generateMode;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "第三方任务编号")
    private String providerTaskId;

    @Schema(description = "文本输出")
    private String outputText;

    @Schema(description = "结构化输出 JSON")
    private String outputData;

    @Schema(description = "结果 URL JSON")
    private String outputUrls;

    @Schema(description = "资产编号 JSON")
    private String assetIds;

    @Schema(description = "失败原因")
    private String failReason;

    @Schema(description = "失败信息")
    private String failMessage;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "完成时间")
    private LocalDateTime finishTime;

}
