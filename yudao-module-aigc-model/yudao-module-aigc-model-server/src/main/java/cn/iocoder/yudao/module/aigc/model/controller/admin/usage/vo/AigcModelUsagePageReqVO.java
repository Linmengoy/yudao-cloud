package cn.iocoder.yudao.module.aigc.model.controller.admin.usage.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - AIGC 模型用量日志分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class AigcModelUsagePageReqVO extends PageParam {

    @Schema(description = "任务编号", example = "1024")
    private Long taskId;

    @Schema(description = "用户编号", example = "2048")
    private Long userId;

    @Schema(description = "模型编号", example = "1")
    private Long modelId;

    @Schema(description = "渠道商编号", example = "1")
    private Long providerId;

    @Schema(description = "模型能力", example = "TEXT_TO_IMAGE")
    private String capability;

    @Schema(description = "调用状态", example = "0")
    private Integer status;

}
