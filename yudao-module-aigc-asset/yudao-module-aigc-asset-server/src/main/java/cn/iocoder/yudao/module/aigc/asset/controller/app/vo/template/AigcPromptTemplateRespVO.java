package cn.iocoder.yudao.module.aigc.asset.controller.app.vo.template;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "用户端 - AIGC 提示词模板 Response VO")
@Data
@Accessors(chain = true)
public class AigcPromptTemplateRespVO {

    @Schema(description = "模板编号", example = "1024")
    private Long id;

    @Schema(description = "模板流水号", example = "TPL202606150001")
    private String templateNo;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "完整提示词")
    private String prompt;

    @Schema(description = "提示词摘要")
    private String promptPreview;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "风格标签 JSON")
    private String styles;

    @Schema(description = "场景标签 JSON")
    private String scenes;

    @Schema(description = "标签")
    private String tags;

    @Schema(description = "图片访问 URL")
    private String imageUrl;

    @Schema(description = "访问 URL 过期时间")
    private LocalDateTime imageUrlExpireTime;

    @Schema(description = "是否公开访问")
    private Boolean publicAccess;

    @Schema(description = "图片宽度")
    private Integer width;

    @Schema(description = "图片高度")
    private Integer height;

    @Schema(description = "MIME 类型")
    private String mimeType;

    @Schema(description = "文件大小")
    private Long fileSize;

    @Schema(description = "来源作者")
    private String sourceLabel;

    @Schema(description = "来源链接")
    private String sourceUrl;

    @Schema(description = "GitHub 原始链接")
    private String githubUrl;

    @Schema(description = "是否推荐")
    private Boolean featured;

    @Schema(description = "查看次数")
    private Integer viewCount;

    @Schema(description = "复制次数")
    private Integer copyCount;

    @Schema(description = "复用次数")
    private Integer useCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
