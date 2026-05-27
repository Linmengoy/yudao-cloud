package cn.iocoder.yudao.module.aigc.gen.controller.admin.record.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - AIGC 生成记录 Response VO")
@Data
public class AigcGenerateRecordRespVO {

    @Schema(description = "生成记录编号", example = "1024")
    private Long id;

    @Schema(description = "任务编号", example = "2048")
    private Long taskId;

    @Schema(description = "用户编号", example = "1024")
    private Long userId;

    @Schema(description = "生成流水号", example = "GEN202605260001")
    private String generateNo;

    @Schema(description = "客户端请求编号", example = "REQ202605260001")
    private String clientRequestId;

    @Schema(description = "生成类型", example = "IMAGE")
    private String generateType;

    @Schema(description = "生成模式", example = "TEXT_TO_IMAGE")
    private String generateMode;

    @Schema(description = "模型编号", example = "1024")
    private Long modelId;

    @Schema(description = "模型编码", example = "gpt-image-2")
    private String modelCode;

    @Schema(description = "渠道商编号", example = "2048")
    private Long providerId;

    @Schema(description = "渠道编码", example = "openai")
    private String providerCode;

    @Schema(description = "第三方任务编号")
    private String providerTaskId;

    @Schema(description = "第三方状态")
    private String providerStatus;

    @Schema(description = "状态", example = "SUCCESS")
    private String status;

    @Schema(description = "提示词")
    private String prompt;

    @Schema(description = "输入参数 JSON")
    private String inputParams;

    @Schema(description = "文本输出")
    private String outputText;

    @Schema(description = "结构化输出 JSON")
    private String outputData;

    @Schema(description = "结果 URL JSON")
    private String outputUrls;

    @Schema(description = "资产编号 JSON")
    private String assetIds;

    @Schema(description = "冻结记录编号", example = "4096")
    private Long freezeId;

    @Schema(description = "销售价", example = "10.00")
    private BigDecimal priceAmount;

    @Schema(description = "成本价", example = "2.00")
    private BigDecimal costAmount;

    @Schema(description = "提交时间")
    private LocalDateTime submitTime;

    @Schema(description = "回调时间")
    private LocalDateTime callbackTime;

    @Schema(description = "完成时间")
    private LocalDateTime finishTime;

    @Schema(description = "失败原因")
    private String failReason;

    @Schema(description = "失败信息")
    private String failMessage;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
