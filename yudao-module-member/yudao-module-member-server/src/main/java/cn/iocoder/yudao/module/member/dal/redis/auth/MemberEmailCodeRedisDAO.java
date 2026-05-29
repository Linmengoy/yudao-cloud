package cn.iocoder.yudao.module.member.dal.redis.auth;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.iocoder.yudao.module.member.dal.redis.RedisKeyConstants;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Repository
public class MemberEmailCodeRedisDAO {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public Boolean tryAcquireSendInterval(Long tenantId, String scene, String email, Duration timeout) {
        return stringRedisTemplate.opsForValue().setIfAbsent(formatSendIntervalKey(tenantId, scene, email), "1",
                timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    public Long incrementEmailDaily(Long tenantId, String scene, String email) {
        String key = formatDailyCountKey(tenantId, scene, email);
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, Duration.between(LocalDateTime.now(), LocalDate.now().plusDays(1).atStartOfDay()));
        }
        return count;
    }

    public Long incrementIpHourly(Long tenantId, String createIp, Duration timeout) {
        String key = formatIpHourlyCountKey(tenantId, createIp);
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, timeout);
        }
        return count;
    }

    private String formatSendIntervalKey(Long tenantId, String scene, String email) {
        return RedisKeyConstants.EMAIL_CODE_SEND_INTERVAL + tenantId + ":" + scene + ":" + email;
    }

    private String formatDailyCountKey(Long tenantId, String scene, String email) {
        return RedisKeyConstants.EMAIL_CODE_DAILY_COUNT + tenantId + ":" + scene + ":"
                + DateUtil.format(LocalDateTime.now(), DatePattern.PURE_DATE_PATTERN) + ":" + email;
    }

    private String formatIpHourlyCountKey(Long tenantId, String createIp) {
        return RedisKeyConstants.EMAIL_CODE_IP_HOURLY_COUNT + tenantId + ":" + createIp;
    }

}
