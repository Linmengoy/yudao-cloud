package cn.iocoder.yudao.module.aigc.safety.dal.redis;

import cn.iocoder.yudao.module.aigc.safety.dal.dataobject.AigcSensitiveWordDO;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;

@Repository
public class AigcSensitiveWordRedisDAO {

    private static final long CACHE_SECONDS = 300L;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @SuppressWarnings("unchecked")
    public List<AigcSensitiveWordDO> get(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        return value instanceof List ? (List<AigcSensitiveWordDO>) value : null;
    }

    public void set(String key, List<AigcSensitiveWordDO> value) {
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(CACHE_SECONDS));
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

}
