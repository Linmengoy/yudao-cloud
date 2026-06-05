package cn.iocoder.yudao.module.aigc.asset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "RPC 服务 - AIGC 资产 Response DTO")
@Data
@Accessors(chain = true)
public class AigcAssetRespDTO {

    @Schema(description = "资产编号", example = "1024")
    private Long id;

    @Schema(description = "资产流水号", example = "AST202605260001")
    private String assetNo;

    @Schema(description = "用户编号", example = "1024")
    private Long userId;

    @Schema(description = "资产类型", example = "IMAGE")
    private String assetType;

    @Schema(description = "来源类型", example = "GENERATE")
    private String sourceType;

    @Schema(description = "业务类型", example = "TASK")
    private String bizType;

    @Schema(description = "业务编号")
    private String bizId;

    @Schema(description = "任务编号")
    private Long taskId;

    @Schema(description = "任务流水号")
    private String taskNo;

    @Schema(description = "模型编号")
    private Long modelId;

    @Schema(description = "渠道商编号")
    private Long providerId;

    @Schema(description = "资产标题")
    private String title;

    @Schema(description = "资产描述")
    private String description;

    @Schema(description = "标签")
    private String tags;

    @Schema(description = "资产文件列表")
    private List<AigcAssetFileRespDTO> files;

    @Schema(description = "兼容字段：文件编号")
    private Long fileId;

    @Schema(description = "兼容字段：文件访问 URL")
    private String fileUrl;

    @Schema(description = "兼容字段：封面访问 URL")
    private String coverUrl;

    @Schema(description = "兼容字段：缩略图访问 URL")
    private String thumbnailUrl;

    @Schema(description = "兼容字段：MIME 类型")
    private String mimeType;

    @Schema(description = "兼容字段：文件扩展名")
    private String fileExt;

    @Schema(description = "兼容字段：文件大小")
    private Long fileSize;

    @Schema(description = "宽度")
    private Integer width;

    @Schema(description = "高度")
    private Integer height;

    @Schema(description = "时长")
    private BigDecimal duration;

    @Schema(description = "扩展元数据 JSON")
    private String metadata;

    @Schema(description = "可见性")
    private String visibility;

    @Schema(description = "审核状态")
    private String auditStatus;

    @Schema(description = "审核原因")
    private String auditReason;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "预览次数")
    private Integer viewCount;

    @Schema(description = "下载次数")
    private Integer downloadCount;

    @Schema(description = "使用次数")
    private Integer useCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
