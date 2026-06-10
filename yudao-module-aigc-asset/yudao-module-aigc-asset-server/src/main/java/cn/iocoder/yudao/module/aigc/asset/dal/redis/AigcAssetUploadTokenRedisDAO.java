package cn.iocoder.yudao.module.aigc.asset.dal.redis;

import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetDirectUploadPrepareReqDTO;
import cn.iocoder.yudao.module.infra.api.file.dto.FilePresignPutRespDTO;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.io.Serializable;
import java.time.Duration;

@Repository
public class AigcAssetUploadTokenRedisDAO {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    public void set(String key, UploadToken value, int expireSeconds) {
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(expireSeconds));
    }

    public UploadToken get(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        return value instanceof UploadToken ? (UploadToken) value : null;
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    @Data
    @Accessors(chain = true)
    public static class UploadToken implements Serializable {

        private Long userId;

        private String assetType;

        private String title;

        private String fileName;

        private String mimeType;

        private Long fileSize;

        private Long configId;

        private String storageType;

        private String bucket;

        private String objectKey;

        private String path;

        private String url;

        private Boolean publicAccess;

        public static UploadToken of(Long userId, AigcAssetDirectUploadPrepareReqDTO reqDTO,
                FilePresignPutRespDTO presign) {
            return new UploadToken()
                    .setUserId(userId)
                    .setAssetType(reqDTO.getAssetType())
                    .setTitle(reqDTO.getTitle())
                    .setFileName(reqDTO.getFileName())
                    .setMimeType(reqDTO.getMimeType())
                    .setFileSize(reqDTO.getFileSize())
                    .setConfigId(presign.getConfigId())
                    .setStorageType(presign.getStorageType())
                    .setBucket(presign.getBucket())
                    .setObjectKey(presign.getObjectKey())
                    .setPath(presign.getPath())
                    .setUrl(presign.getUrl())
                    .setPublicAccess(presign.getPublicAccess());
        }

    }

}
