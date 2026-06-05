package cn.iocoder.yudao.module.aigc.asset.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - AIGC 资产新增/修改 Request VO")
@Data
public class AigcAssetSaveReqVO {

    @Schema(description = "资产编号", example = "1024")
    private Long id;

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "用户编号不能为空")
    private Long userId;

    @Schema(description = "资产类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "IMAGE")
    @NotBlank(message = "资产类型不能为空")
    private String assetType;

    @Schema(description = "来源类型", example = "UPLOAD")
    private String sourceType;

    @Schema(description = "资产标题")
    private String title;

    @Schema(description = "资产描述")
    private String description;

    @Schema(description = "标签")
    private String tags;

    @Schema(description = "平台文件编号")
    private Long fileId;

    @Schema(description = "平台文件 URL")
    private String fileUrl;

    @Schema(description = "封面 URL")
    private String coverUrl;

    @Schema(description = "缩略图 URL")
    private String thumbnailUrl;

    @Schema(description = "MIME 类型")
    private String mimeType;

    @Schema(description = "文件扩展名")
    private String fileExt;

    @Schema(description = "文件大小")
    private Long fileSize;

    @Schema(description = "宽度")
    private Integer width;

    @Schema(description = "高度")
    private Integer height;

    @Schema(description = "时长")
    private BigDecimal duration;

    @Schema(description = "扩展元数据 JSON")
    private String metadata;

    @Schema(description = "可见性", example = "PRIVATE")
    private String visibility;

    @Schema(description = "审核状态", example = "PENDING")
    private String auditStatus;

}
