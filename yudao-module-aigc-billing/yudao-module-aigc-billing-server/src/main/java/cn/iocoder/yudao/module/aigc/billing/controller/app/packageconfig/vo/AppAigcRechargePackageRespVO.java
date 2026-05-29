package cn.iocoder.yudao.module.aigc.billing.controller.app.packageconfig.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "用户端 - AIGC 充值套餐 Response VO")
@Data
public class AppAigcRechargePackageRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "套餐名称", example = "标准版")
    private String name;

    @Schema(description = "支付金额，单位：分", example = "4990")
    private Integer payAmount;

    @Schema(description = "充值积分", example = "500")
    private BigDecimal pointAmount;

    @Schema(description = "赠送积分", example = "100")
    private BigDecimal giftAmount;

    @Schema(description = "到账积分总数", example = "600")
    private BigDecimal totalPointAmount;

    @Schema(description = "描述", example = "适合轻量创作")
    private String description;

    @Schema(description = "权益说明，每行一条", example = "全部图片模型\n高清分辨率")
    private String features;

    @Schema(description = "是否推荐", example = "true")
    private Boolean recommendStatus;

    @Schema(description = "排序", example = "10")
    private Integer sort;

}
