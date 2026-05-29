package cn.iocoder.yudao.module.aigc.billing.controller.admin.packageconfig.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - AIGC 充值套餐分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcRechargePackagePageReqVO extends PageParam {

    @Schema(description = "套餐名称", example = "标准版")
    private String name;

    @Schema(description = "状态", example = "0")
    private Integer status;

}
