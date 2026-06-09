package cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "用户端 - AIGC 画布项目资产分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class AigcCanvasProjectAssetPageReqVO extends PageParam {

    @Schema(description = "资产类型", example = "IMAGE")
    private String assetType;

}
