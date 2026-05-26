package cn.iocoder.yudao.module.aigc.asset.dal.dataobject;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@TableName("aigc_asset_download_log")
@KeySequence("aigc_asset_download_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AigcAssetDownloadLogDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long assetId;
    private String assetNo;
    private Long userId;
    private Long ownerUserId;
    private String downloadUrl;
    private String clientIp;
    private String userAgent;
    private String referer;
    private String result;
    private String failReason;

}
