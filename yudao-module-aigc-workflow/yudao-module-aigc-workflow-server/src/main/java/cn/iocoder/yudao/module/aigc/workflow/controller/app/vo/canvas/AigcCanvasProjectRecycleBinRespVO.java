package cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "用户端 - AIGC 画布项目回收站 Response VO")
@Data
public class AigcCanvasProjectRecycleBinRespVO {

    @Schema(description = "回收站记录编号")
    private Long id;
    @Schema(description = "项目编号")
    private Long projectId;
    @Schema(description = "拥有者用户编号")
    private Long ownerUserId;
    @Schema(description = "项目名称")
    private String projectName;
    @Schema(description = "封面资产编号")
    private Long coverAssetId;
    @Schema(description = "当前版本")
    private Long currentVersion;
    @Schema(description = "最新快照编号")
    private Long latestSnapshotId;
    @Schema(description = "项目状态")
    private String projectStatus;
    @Schema(description = "节点数")
    private Integer nodeCount;
    @Schema(description = "资产数")
    private Integer assetCount;
    @Schema(description = "删除人")
    private Long deletedBy;
    @Schema(description = "删除时间")
    private LocalDateTime deletedTime;
    @Schema(description = "删除原因")
    private String deleteReason;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
