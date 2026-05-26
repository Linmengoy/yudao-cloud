package cn.iocoder.yudao.module.aigc.task.controller.admin.callback.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - AIGC 任务回调 Response VO")
@Data
public class AigcTaskCallbackRespVO {

    private Long id;
    private String callbackNo;
    private Long taskId;
    private String taskNo;
    private Long providerId;
    private String providerCode;
    private String externalTaskId;
    private String callbackType;
    private String callbackStatus;
    private String processResult;
    private String failReason;
    private LocalDateTime receivedTime;
    private LocalDateTime processedTime;
}
