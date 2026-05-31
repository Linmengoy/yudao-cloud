package cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "用户端 - AIGC 画布项目 Response VO")
@Data
public class AigcCanvasProjectRespVO {

    @Schema(description = "项目编号")
    private Long id;
    @Schema(description = "拥有者用户编号")
    private Long ownerUserId;
    @Schema(description = "项目名称")
    private String name;
    @Schema(description = "项目类型")
    private String kind;
    @Schema(description = "封面资产编号")
    private Long coverAssetId;
    @Schema(description = "当前版本")
    private Long currentVersion;
    @Schema(description = "最新快照编号")
    private Long latestSnapshotId;
    @Schema(description = "项目状态")
    private String status;
    @Schema(description = "节点数")
    private Integer nodeCount;
    @Schema(description = "资产数")
    private Integer assetCount;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    @Schema(description = "当前用户角色")
    private String role;
    @Schema(description = "是否可编辑")
    private Boolean canEdit;
    @Schema(description = "是否只读")
    private Boolean readonly;

}
