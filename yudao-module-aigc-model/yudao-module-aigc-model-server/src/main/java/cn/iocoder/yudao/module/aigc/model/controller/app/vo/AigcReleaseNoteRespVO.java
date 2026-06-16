package cn.iocoder.yudao.module.aigc.model.controller.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "App - AIGC 版本更新记录 Response VO")
@Data
public class AigcReleaseNoteRespVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "版本号")
    private String version;

    @Schema(description = "发布日期")
    private LocalDate releaseDate;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "更新摘要")
    private String summary;

    @Schema(description = "更新内容")
    private String content;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

}
