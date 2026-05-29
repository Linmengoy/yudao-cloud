package cn.iocoder.yudao.module.member.dal.redis.auth;

import cn.iocoder.yudao.framework.test.core.ut.BaseRedisUnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@Import(MemberEmailCodeRedisDAO.class)
public class MemberEmailCodeRedisDAOTest extends BaseRedisUnitTest {

    @Resource
    private MemberEmailCodeRedisDAO emailCodeRedisDAO;

    @Test
    public void testTryAcquireSendInterval() {
        Long tenantId = 1L;
        String scene = "REGISTER";
        String email = "user@example.com";

        Boolean first = emailCodeRedisDAO.tryAcquireSendInterval(tenantId, scene, email, Duration.ofSeconds(60));
        Boolean second = emailCodeRedisDAO.tryAcquireSendInterval(tenantId, scene, email, Duration.ofSeconds(60));

        assertTrue(Boolean.TRUE.equals(first));
        assertFalse(Boolean.TRUE.equals(second));
    }

    @Test
    public void testIncrementEmailDaily() {
        Long tenantId = 1L;
        String scene = "LOGIN";
        String email = "daily@example.com";

        Long first = emailCodeRedisDAO.incrementEmailDaily(tenantId, scene, email);
        Long second = emailCodeRedisDAO.incrementEmailDaily(tenantId, scene, email);

        assertEquals(1L, first);
        assertEquals(2L, second);
    }

    @Test
    public void testIncrementIpHourly() {
        Long tenantId = 1L;
        String createIp = "127.0.0.1";

        Long first = emailCodeRedisDAO.incrementIpHourly(tenantId, createIp, Duration.ofHours(1));
        Long second = emailCodeRedisDAO.incrementIpHourly(tenantId, createIp, Duration.ofHours(1));

        assertEquals(1L, first);
        assertEquals(2L, second);
    }

}
