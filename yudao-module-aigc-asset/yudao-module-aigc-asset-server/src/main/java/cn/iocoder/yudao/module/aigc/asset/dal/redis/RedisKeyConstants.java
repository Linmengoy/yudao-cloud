package cn.iocoder.yudao.module.aigc.asset.dal.redis;

public interface RedisKeyConstants {

    String ASSET_ACCESS_URL = "aigc:asset:access-url:%d:%d:%s:%s:%s";

    String ASSET_ACCESS_URL_LOCK = "aigc:asset:access-url:lock:%d:%d:%s:%s:%s";

    String ASSET_UPLOAD_TOKEN = "aigc:asset:upload-token:%d:%s";

}
