package cn.iocoder.yudao.module.aigc.model.controller.admin.provider.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - AIGC 模型渠道商分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class AigcModelProviderPageReqVO extends PageParam {

    @Schema(description = "渠道商编码", example = "openai")
    private String code;

    @Schema(description = "渠道商名称", example = "OpenAI")
    private String name;

    @Schema(description = "状态", example = "0")
    private Integer status;

}
