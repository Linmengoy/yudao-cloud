package cn.iocoder.yudao.module.aigc.model.controller.admin.release.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Schema(description = "管理后台 - AIGC 版本更新记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class AigcReleaseNotePageReqVO extends PageParam {

    @Schema(description = "版本号")
    private String version;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "发布日期")
    private LocalDate[] releaseDate;

}
