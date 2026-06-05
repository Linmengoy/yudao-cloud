package cn.iocoder.yudao.module.aigc.asset.dal.redis;

import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetAccessUrlRespDTO;
import jakarta.annotation.Resource;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Repository
public class AigcAssetAccessUrlRedisDAO {

    private static final long CACHE_SAFE_SECONDS = 60L;
    private static final long LOCK_SECONDS = 3L;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RedissonClient redissonClient;

    public AigcAssetAccessUrlRespDTO get(String key) {
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (ttl == null || ttl <= CACHE_SAFE_SECONDS) {
            return null;
        }
        Object value = redisTemplate.opsForValue().get(key);
        return value instanceof AigcAssetAccessUrlRespDTO ? (AigcAssetAccessUrlRespDTO) value : null;
    }

    public void set(String key, AigcAssetAccessUrlRespDTO value, Integer expireSeconds) {
        long ttl = Math.max(1L, expireSeconds - CACHE_SAFE_SECONDS);
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttl));
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public <T> T executeWithLock(String key, Callable<T> callable) throws Exception {
        RLock lock = redissonClient.getLock(key);
        lock.lock(LOCK_SECONDS, TimeUnit.SECONDS);
        try {
            return callable.call();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

}
