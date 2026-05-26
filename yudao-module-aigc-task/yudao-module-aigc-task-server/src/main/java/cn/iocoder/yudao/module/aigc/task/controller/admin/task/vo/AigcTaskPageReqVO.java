package cn.iocoder.yudao.module.aigc.task.controller.admin.task.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - AIGC 任务分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class AigcTaskPageReqVO extends PageParam {

    @Schema(description = "用户编号", example = "1024")
    private Long userId;

    @Schema(description = "任务编号", example = "TASK202605260001")
    private String taskNo;

    @Schema(description = "任务类型", example = "TEXT_GENERATE")
    private String taskType;

    @Schema(description = "模型编号", example = "1024")
    private Long modelId;

    @Schema(description = "状态", example = "SUCCESS")
    private String status;

}
