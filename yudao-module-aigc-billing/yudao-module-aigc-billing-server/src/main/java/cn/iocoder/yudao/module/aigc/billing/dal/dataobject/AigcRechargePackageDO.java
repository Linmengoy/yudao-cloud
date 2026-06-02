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

    /**
     * 套餐名称
     */
    private String name; 
    /**
     * 充值金额
     */
    private Integer payAmount; 
    /**
     * 充值积分
     */
    private BigDecimal pointAmount; 
    /**
     * 赠送积分
     */
    private BigDecimal giftAmount; 
    /**
     * 到账总积分
     */
    private BigDecimal totalPointAmount; 
    /**
     * 套餐描述
     */
    private String description; 
    /**
     * 套餐功能
     */
    private String features; 
    /**
     * 是否推荐
     */
    private Boolean recommendStatus; 
    /**
     * 排序
     */
    private Integer sort; 
    /**
     * 状态
     */
    private Integer status; 
    /**
     * 备注
     */
    private String remark; 

}
