package cn.iocoder.yudao.module.aigc.asset.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName(value = "aigc_asset", autoResultMap = true)
@KeySequence("aigc_asset_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcAssetDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String assetNo;
    private Long userId;
    private String assetType;
    private String sourceType;
    private String bizType;
    private String bizId;
    private Long taskId;
    private String taskNo;
    private Long modelId;
    private Long providerId;
    private String title;
    private String description;
    private String tags;
    private Long fileId;
    private String fileUrl;
    private String originUrl;
    private Long coverFileId;
    private String coverUrl;
    private String thumbnailUrl;
    private String mimeType;
    private String fileExt;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private BigDecimal duration;
    private String metadata;
    private String promptSnapshot;
    private String generateSnapshot;
    private String visibility;
    private String auditStatus;
    private String auditReason;
    private String status;
    private Integer viewCount;
    private Integer downloadCount;
    private Integer useCount;
    private LocalDateTime lastUsedTime;

}
