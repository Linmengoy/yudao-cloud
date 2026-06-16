package cn.iocoder.yudao.module.aigc.community.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@TableName("aigc_guide_content")
@KeySequence("aigc_guide_content_seq")
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcGuideContentDO extends TenantBaseDO {

    @TableId
    private Long id;
    private String slug;
    private String title;
    private String category;
    private String summary;
    private String content;
    private Integer sort;
    private String publishStatus;
    private LocalDateTime publishTime;
    private Long publisherUserId;

}
