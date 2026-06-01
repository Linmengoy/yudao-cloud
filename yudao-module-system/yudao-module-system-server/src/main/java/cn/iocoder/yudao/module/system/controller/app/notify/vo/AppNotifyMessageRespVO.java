package cn.iocoder.yudao.module.system.controller.app.notify.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "用户 App - 站内信 Response VO")
@Data
public class AppNotifyMessageRespVO {

    @Schema(description = "ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "模版编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13013")
    private Long templateId;

    @Schema(description = "模板编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "task_success")
    private String templateCode;

    @Schema(description = "模版发送人名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "系统通知")
    private String templateNickname;

    @Schema(description = "模版内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "任务已完成")
    private String templateContent;

    @Schema(description = "模版类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Integer templateType;

    @Schema(description = "模版参数", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Object> templateParams;

    @Schema(description = "是否已读", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean readStatus;

    @Schema(description = "阅读时间")
    private LocalDateTime readTime;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
