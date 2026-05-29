package cn.iocoder.yudao.module.aigc.billing.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

@TableName("aigc_recharge_package")
@KeySequence("aigc_recharge_package_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcRechargePackageDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String name;
    private Integer payAmount;
    private BigDecimal pointAmount;
    private BigDecimal giftAmount;
    private BigDecimal totalPointAmount;
    private String description;
    private String features;
    private Boolean recommendStatus;
    private Integer sort;
    private Integer status;
    private String remark;

}
