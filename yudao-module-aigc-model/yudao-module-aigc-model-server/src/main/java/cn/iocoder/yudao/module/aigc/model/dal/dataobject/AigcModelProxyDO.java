package cn.iocoder.yudao.module.aigc.model.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.mybatis.core.type.EncryptTypeHandler;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@TableName(value = "aigc_model_proxy", autoResultMap = true)
@KeySequence("aigc_model_proxy_seq")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcModelProxyDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;

    private String name;

    private String protocol;

    private String host;

    private Integer port;

    private String username;

    @TableField(typeHandler = EncryptTypeHandler.class)
    private String password;

    private Integer status;

    private String remark;

}
