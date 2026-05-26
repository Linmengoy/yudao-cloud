package cn.iocoder.yudao.module.aigc.task.controller.admin.retry.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - AIGC 任务重试分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class AigcTaskRetryPageReqVO extends PageParam {

    @Schema(description = "任务编号", example = "1024")
    private Long taskId;

    @Schema(description = "平台任务编号", example = "TASK202605260001")
    private String taskNo;

    @Schema(description = "重试状态", example = "WAITING")
    private String retryStatus;

}
