package cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "用户端 - AIGC 画布操作 Response VO")
@Data
public class AigcCanvasOperationRespVO {

    @Schema(description = "操作日志编号")
    private Long id;
    @Schema(description = "项目编号")
    private Long projectId;
    @Schema(description = "客户端编号")
    private String clientId;
    @Schema(description = "操作编号")
    private String opId;
    @Schema(description = "操作用户")
    private Long actorUserId;
    @Schema(description = "基于版本")
    private Long baseVersion;
    @Schema(description = "应用后版本")
    private Long nextVersion;
    @Schema(description = "操作类型")
    private String operationType;
    @Schema(description = "操作 JSON")
    private String operationJson;
    @Schema(description = "反向操作 JSON")
    private String inverseOperationJson;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
