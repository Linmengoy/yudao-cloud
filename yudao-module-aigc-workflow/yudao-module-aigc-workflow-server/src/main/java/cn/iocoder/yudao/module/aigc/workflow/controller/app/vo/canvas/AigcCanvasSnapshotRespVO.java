package cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "用户端 - AIGC 画布快照 Response VO")
@Data
public class AigcCanvasSnapshotRespVO {

    @Schema(description = "快照编号")
    private Long id;
    @Schema(description = "项目编号")
    private Long projectId;
    @Schema(description = "版本")
    private Long version;
    @Schema(description = "快照存储类型 INLINE/OSS/MINIO")
    private String storageType;
    @Schema(description = "快照文件存储配置编号")
    private Long storageConfigId;
    @Schema(description = "快照 Bucket")
    private String bucket;
    @Schema(description = "快照对象 Key")
    private String snapshotObjectKey;
    @Schema(description = "快照大小")
    private Long snapshotSize;
    @Schema(description = "快照 Hash")
    private String snapshotHash;
    @Schema(description = "节点 JSON")
    private String nodesJson;
    @Schema(description = "连线 JSON")
    private String edgesJson;
    @Schema(description = "视口 JSON")
    private String viewportJson;
    @Schema(description = "创建人")
    private Long createdBy;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
