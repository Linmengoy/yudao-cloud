package cn.iocoder.yudao.module.aigc.model.controller.admin.channel.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - AIGC 模型渠道实现分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class AigcModelChannelPageReqVO extends PageParam {

    @Schema(description = "业务模型编号", example = "1024")
    private Long modelId;

    @Schema(description = "渠道商编号", example = "2048")
    private Long providerId;

    @Schema(description = "渠道实现名称", example = "OpenAI image2")
    private String name;

    @Schema(description = "状态", example = "0")
    private Integer status;

}
