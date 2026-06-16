package cn.iocoder.yudao.module.aigc.model.controller.admin.release.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - AIGC 版本更新记录保存 Request VO")
@Data
public class AigcReleaseNoteSaveReqVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "版本号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "版本号不能为空")
    private String version;

    @Schema(description = "发布日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "发布日期不能为空")
    private LocalDate releaseDate;

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "标题不能为空")
    private String title;

    @Schema(description = "更新摘要")
    private String summary;

    @Schema(description = "更新内容")
    private String content;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "发布人")
    private String publisher;

}
