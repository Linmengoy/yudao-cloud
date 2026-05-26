package cn.iocoder.yudao.module.aigc.gen.framework.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AigcGenerateFileSecurityUtilsTest {

    @Test
    public void testIsSafeRemoteUrl() {
        assertTrue(AigcGenerateFileSecurityUtils.isSafeRemoteUrl("https://example.com/a.png"));
        assertFalse(AigcGenerateFileSecurityUtils.isSafeRemoteUrl("http://127.0.0.1/a.png"));
        assertFalse(AigcGenerateFileSecurityUtils.isSafeRemoteUrl("http://192.168.1.1/a.png"));
        assertFalse(AigcGenerateFileSecurityUtils.isSafeRemoteUrl("file:///tmp/a.png"));
    }
}
