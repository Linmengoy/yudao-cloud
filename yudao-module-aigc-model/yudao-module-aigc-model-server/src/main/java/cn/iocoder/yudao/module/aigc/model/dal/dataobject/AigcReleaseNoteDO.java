package cn.iocoder.yudao.module.aigc.model.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("aigc_release_note")
@KeySequence("aigc_release_note_seq")
@TenantIgnore
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcReleaseNoteDO extends BaseDO {

    @TableId
    private Long id;

    private String version;

    private LocalDate releaseDate;

    private String title;

    private String summary;

    private String content;

    private Integer status;

    private String publisher;

    private LocalDateTime publishTime;

}
