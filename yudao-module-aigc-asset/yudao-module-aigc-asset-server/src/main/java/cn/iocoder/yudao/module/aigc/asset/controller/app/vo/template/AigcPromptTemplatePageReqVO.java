package cn.iocoder.yudao.module.aigc.asset.controller.app.vo.template;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "用户端 - AIGC 提示词模板分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcPromptTemplatePageReqVO extends PageParam {

    @Schema(description = "搜索关键词", example = "海报")
    private String keyword;

    @Schema(description = "分类", example = "Posters & Typography")
    private String category;

    @Schema(description = "模型编码", example = "gpt-image-2")
    private String modelCode;

    @Schema(description = "风格", example = "Poster")
    private String style;

    @Schema(description = "场景", example = "Commerce")
    private String scene;

    @Schema(description = "是否推荐", example = "true")
    private Boolean featured;

}
