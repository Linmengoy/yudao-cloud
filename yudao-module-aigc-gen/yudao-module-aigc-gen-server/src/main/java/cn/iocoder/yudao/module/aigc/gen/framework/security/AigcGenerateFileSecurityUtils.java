package cn.iocoder.yudao.module.aigc.gen.framework.security;

import java.net.URI;
import java.util.Set;

public class AigcGenerateFileSecurityUtils {

    private static final Set<String> BLOCK_HOSTS = Set.of("localhost", "127.0.0.1", "0.0.0.0", "::1");

    public static boolean isSafeRemoteUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
                return false;
            }
            return host != null && !BLOCK_HOSTS.contains(host.toLowerCase()) && !host.startsWith("10.")
                    && !host.startsWith("192.168.") && !host.startsWith("172.16.") && !host.startsWith("169.254.");
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
