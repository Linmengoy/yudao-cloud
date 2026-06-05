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

@TableName(value = "aigc_asset_file", autoResultMap = true)
@KeySequence("aigc_asset_file_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcAssetFileDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long assetId;
    private String fileRole;
    private Long fileId;
    private Long storageConfigId;
    private String storageType;
    private String bucket;
    private String objectKey;
    private String filePath;
    private String originUrl;
    private LocalDateTime originExpireTime;
    private String fileName;
    private String fileExt;
    private String mimeType;
    private Long fileSize;
    private String etag;
    private String checksum;
    private Integer width;
    private Integer height;
    private BigDecimal duration;
    private Integer accessMode;
    private String publicUrl;
    private String status;
    private String metadata;

}
