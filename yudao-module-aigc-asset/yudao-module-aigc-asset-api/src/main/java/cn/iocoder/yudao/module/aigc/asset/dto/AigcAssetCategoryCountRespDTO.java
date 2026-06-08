package cn.iocoder.yudao.module.aigc.asset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

@Schema(description = "AIGC 资产分类数量 Response DTO")
@Data
@Accessors(chain = true)
public class AigcAssetCategoryCountRespDTO {

    @Schema(description = "全部")
    private Long allCount;

    @Schema(description = "生成图片")
    private Long generatedImageCount;

    @Schema(description = "上传图片")
    private Long uploadedImageCount;

    @Schema(description = "视频")
    private Long videoCount;

    @Schema(description = "其它")
    private Long otherCount;

}
