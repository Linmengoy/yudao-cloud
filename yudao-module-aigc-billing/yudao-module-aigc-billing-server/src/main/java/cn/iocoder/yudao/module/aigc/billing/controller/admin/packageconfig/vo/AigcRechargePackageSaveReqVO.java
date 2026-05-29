package cn.iocoder.yudao.module.aigc.billing.controller.admin.packageconfig.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - AIGC 充值套餐创建/修改 Request VO")
@Data
public class AigcRechargePackageSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "套餐名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "标准版")
    @NotBlank(message = "套餐名称不能为空")
    private String name;

    @Schema(description = "支付金额，单位：分", requiredMode = Schema.RequiredMode.REQUIRED, example = "4990")
    @NotNull(message = "支付金额不能为空")
    @Min(value = 0, message = "支付金额不能小于 0")
    private Integer payAmount;

    @Schema(description = "充值积分", requiredMode = Schema.RequiredMode.REQUIRED, example = "500")
    @NotNull(message = "充值积分不能为空")
    @DecimalMin(value = "0", message = "充值积分不能小于 0")
    private BigDecimal pointAmount;

    @Schema(description = "赠送积分", example = "100")
    @DecimalMin(value = "0", message = "赠送积分不能小于 0")
    private BigDecimal giftAmount;

    @Schema(description = "描述", example = "适合轻量创作")
    private String description;

    @Schema(description = "权益说明，每行一条", example = "全部图片模型\n高清分辨率")
    private String features;

    @Schema(description = "是否推荐", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "是否推荐不能为空")
    private Boolean recommendStatus;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "排序不能为空")
    private Integer sort;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "备注", example = "运营活动")
    private String remark;

}
