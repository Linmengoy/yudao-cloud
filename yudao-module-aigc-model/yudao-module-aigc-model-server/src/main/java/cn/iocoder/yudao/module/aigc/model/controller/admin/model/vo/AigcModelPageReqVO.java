package cn.iocoder.yudao.module.aigc.model.controller.admin.model.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - AIGC 模型分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class AigcModelPageReqVO extends PageParam {

    @Schema(description = "渠道商编号", example = "1024")
    private Long providerId;

    @Schema(description = "模型编码", example = "kling-v1")
    private String code;

    @Schema(description = "模型名称", example = "可灵视频模型")
    private String name;

    @Schema(description = "模型类型", example = "1")
    private Integer type;

    @Schema(description = "状态", example = "0")
    private Integer status;

}
