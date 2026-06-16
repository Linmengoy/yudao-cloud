package cn.iocoder.yudao.module.aigc.model.controller.admin.channel.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - AIGC 模型渠道实现克隆 Request VO")
@Data
public class AigcModelChannelCloneReqVO {

    @Schema(description = "源渠道实现编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "源渠道实现编号不能为空")
    private Long sourceChannelId;

    @Schema(description = "目标渠道商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "目标渠道商编号不能为空")
    private Long targetProviderId;

    @Schema(description = "渠道商真实模型标识，不填则沿用源渠道", example = "Doubao-Seedream-3-0")
    private String providerModel;

    @Schema(description = "渠道实现名称，不填则使用源渠道名称加克隆后缀", example = "火山方舟备用")
    private String name;

    @Schema(description = "权重，不填则沿用源渠道", example = "50")
    private Integer weight;

}
