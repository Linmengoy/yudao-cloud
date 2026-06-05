package cn.iocoder.yudao.module.aigc.asset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "RPC 服务 - AIGC 资产创建 Request DTO")
@Data
@Accessors(chain = true)
public class AigcAssetCreateReqDTO {

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "用户编号不能为空")
    private Long userId;

    @Schema(description = "资产类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "IMAGE")
    @NotBlank(message = "资产类型不能为空")
    private String assetType;

    @Schema(description = "来源类型", example = "GENERATE")
    private String sourceType;

    @Schema(description = "业务类型", example = "TASK")
    private String bizType;

    @Schema(description = "业务编号", example = "TASK202605260001")
    private String bizId;

    @Schema(description = "任务编号", example = "1024")
    private Long taskId;

    @Schema(description = "任务流水号", example = "TASK202605260001")
    private String taskNo;

    @Schema(description = "模型编号", example = "1024")
    private Long modelId;

    @Schema(description = "渠道商编号", example = "2048")
    private Long providerId;

    @Schema(description = "资产标题", example = "AI 生成图片")
    private String title;

    @Schema(description = "资产描述")
    private String description;

    @Schema(description = "标签")
    private String tags;

    @Schema(description = "平台文件编号", example = "1024")
    private Long fileId;

    @Schema(description = "平台文件 URL")
    private String fileUrl;

    @Schema(description = "第三方原始 URL")
    private String originUrl;

    @Schema(description = "封面文件编号", example = "1024")
    private Long coverFileId;

    @Schema(description = "封面 URL")
    private String coverUrl;

    @Schema(description = "缩略图 URL")
    private String thumbnailUrl;

    @Schema(description = "MIME 类型", example = "image/png")
    private String mimeType;

    @Schema(description = "文件扩展名", example = "png")
    private String fileExt;

    @Schema(description = "文件大小，单位字节", example = "102400")
    private Long fileSize;

    @Schema(description = "宽度", example = "1024")
    private Integer width;

    @Schema(description = "高度", example = "768")
    private Integer height;

    @Schema(description = "时长，单位秒", example = "10.5")
    private java.math.BigDecimal duration;

    @Schema(description = "扩展元数据 JSON")
    private String metadata;

    @Schema(description = "提示词快照 JSON")
    private String promptSnapshot;

    @Schema(description = "生成参数快照 JSON")
    private String generateSnapshot;

    @Schema(description = "可见性", example = "PRIVATE")
    private String visibility;

    @Schema(description = "审核状态", example = "PENDING")
    private String auditStatus;

}
