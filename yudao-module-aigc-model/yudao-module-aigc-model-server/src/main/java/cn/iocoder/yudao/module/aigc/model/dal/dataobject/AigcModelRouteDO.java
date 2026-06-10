package cn.iocoder.yudao.module.aigc.model.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@TableName(value = "aigc_model_route", autoResultMap = true)
@KeySequence("aigc_model_route_seq")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcModelRouteDO extends BaseDO {

    @TableId
    private Long id;

    private String name;

    private String taskType;

    private Long modelId;

    private String capability;

    private String strategy;

    private String modelIds;

    private String channelIds;

    private String userLevel;

    private Integer status;

}
