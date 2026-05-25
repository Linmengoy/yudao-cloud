package cn.iocoder.yudao.module.aigc.model.controller.admin.route.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - AIGC 模型路由规则分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class AigcModelRoutePageReqVO extends PageParam {

    @Schema(description = "路由名称", example = "默认文生图路由")
    private String name;

    @Schema(description = "任务类型", example = "image")
    private String taskType;

    @Schema(description = "模型能力", example = "TEXT_TO_IMAGE")
    private String capability;

    @Schema(description = "状态", example = "0")
    private Integer status;

}
