package cn.iocoder.yudao.module.aigc.asset.controller.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "用户端 - AIGC 视频截帧 Request VO")
@Data
public class AigcAssetVideoFrameCaptureReqVO {

    @Schema(description = "视频资产编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "视频资产编号不能为空")
    private Long assetId;

    @Schema(description = "截帧位置：first/current/last", example = "current")
    private String capturedAt;

    @Schema(description = "当前帧时间，单位秒", example = "1.25")
    private BigDecimal timeSec;

    @Schema(description = "生成图片标题", example = "Video 当前帧.png")
    private String title;

}
