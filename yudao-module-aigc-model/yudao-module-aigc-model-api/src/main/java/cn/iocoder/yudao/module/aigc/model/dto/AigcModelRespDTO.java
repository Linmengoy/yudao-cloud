package cn.iocoder.yudao.module.aigc.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "RPC 服务 - AIGC 模型 Response DTO")
@Data
public class AigcModelRespDTO {

    @Schema(description = "模型编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "模型编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "kling-v1")
    private String code;

    @Schema(description = "模型名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "可灵视频模型")
    private String name;

    @Schema(description = "模型标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "kling-v1")
    private String model;

    @Schema(description = "实际执行的渠道实现编号", example = "2048")
    private Long channelId;

    @Schema(description = "实际执行的上游模型标识", example = "image2")
    private String providerModel;

    @Schema(description = "模型类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer type;

    @Schema(description = "模型类型名称", example = "文本模型")
    private String typeLabel;

    @Schema(description = "是否用户端展示", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean publicVisible;

    @Schema(description = "是否默认模型", requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
    private Boolean defaultModel;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer sort;

    @Schema(description = "最大并发数", example = "10")
    private Integer maxConcurrent;

    @Schema(description = "超时时间，单位：秒", example = "60")
    private Integer timeoutSeconds;

    @Schema(description = "队列优先级", example = "1")
    private Integer queuePriority;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "渠道商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long providerId;

    @Schema(description = "渠道商名称", example = "OpenAI")
    private String providerName;

    @Schema(description = "模型能力列表", example = "TEXT_TO_IMAGE")
    private List<String> capabilities;

    @Schema(description = "备注", example = "主力模型")
    private String remark;

}
