package cn.iocoder.yudao.module.aigc.asset.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@TableName(value = "aigc_prompt_template", autoResultMap = true)
@KeySequence("aigc_prompt_template_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcPromptTemplateDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String templateNo;
    private String sourceType;
    private Long sourceCaseId;
    private String sourceRepo;
    private String sourceLabel;
    private String sourceUrl;
    private String githubUrl;
    private String title;
    private String description;
    private String prompt;
    private String promptPreview;
    private String category;
    private String styles;
    private String scenes;
    private String tags;
    private Long coverFileId;
    private Long storageConfigId;
    private String storageType;
    private String bucket;
    private String objectKey;
    private String filePath;
    private String publicUrl;
    private Integer width;
    private Integer height;
    private String mimeType;
    private Long fileSize;
    private Integer accessMode;
    private Boolean featured;
    private Integer sort;
    private String visibility;
    private String auditStatus;
    private String status;
    private Integer viewCount;
    private Integer copyCount;
    private Integer useCount;

}
