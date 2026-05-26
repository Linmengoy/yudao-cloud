package cn.iocoder.yudao.module.member.dal.dataobject.auth;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

@TableName("member_email_code")
@KeySequence("member_email_code_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberEmailCodeDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String email;

    private String code;

    private String scene;

    private Boolean used;

    private LocalDateTime usedTime;

    private String usedIp;

    private String createIp;

    private Integer todayIndex;

    private LocalDateTime expiresTime;

}
