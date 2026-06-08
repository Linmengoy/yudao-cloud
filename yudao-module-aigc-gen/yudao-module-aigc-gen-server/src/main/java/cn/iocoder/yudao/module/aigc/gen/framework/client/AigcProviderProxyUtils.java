package cn.iocoder.yudao.module.aigc.gen.framework.client;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.iocoder.yudao.module.aigc.gen.framework.client.dto.AigcProviderSubmitReqDTO;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;

public final class AigcProviderProxyUtils {

    private static final Object AUTHENTICATOR_LOCK = new Object();

    private AigcProviderProxyUtils() {
    }

    public static HttpRequest applyProxy(HttpRequest request, AigcProviderSubmitReqDTO reqDTO) {
        if (reqDTO == null || !Boolean.TRUE.equals(reqDTO.getProxyEnabled()) || StrUtil.isBlank(reqDTO.getProxyHost()) || reqDTO.getProxyPort() == null) {
            return request;
        }
        request.setProxy(new Proxy(resolveProxyType(reqDTO.getProxyProtocol()), new InetSocketAddress(reqDTO.getProxyHost(), reqDTO.getProxyPort())));
        if (!isSocks(reqDTO.getProxyProtocol()) && (StrUtil.isNotBlank(reqDTO.getProxyUsername()) || StrUtil.isNotBlank(reqDTO.getProxyPassword()))) {
            request.basicProxyAuth(StrUtil.blankToDefault(reqDTO.getProxyUsername(), ""), StrUtil.blankToDefault(reqDTO.getProxyPassword(), ""));
        }
        return request;
    }

    public static HttpResponse execute(HttpRequest request, AigcProviderSubmitReqDTO reqDTO) {
        HttpRequest proxiedRequest = applyProxy(request, reqDTO);
        if (reqDTO == null || !isSocks(reqDTO.getProxyProtocol()) || (StrUtil.isBlank(reqDTO.getProxyUsername()) && StrUtil.isBlank(reqDTO.getProxyPassword()))) {
            return proxiedRequest.execute();
        }
        synchronized (AUTHENTICATOR_LOCK) {
            Authenticator previous = Authenticator.getDefault();
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                            StrUtil.blankToDefault(reqDTO.getProxyUsername(), ""),
                            StrUtil.blankToDefault(reqDTO.getProxyPassword(), "").toCharArray());
                }
            });
            try {
                return proxiedRequest.execute();
            } finally {
                Authenticator.setDefault(previous);
            }
        }
    }

    private static Proxy.Type resolveProxyType(String protocol) {
        if (isSocks(protocol)) {
            return Proxy.Type.SOCKS;
        }
        return Proxy.Type.HTTP;
    }

    private static boolean isSocks(String protocol) {
        return "SOCKS5".equalsIgnoreCase(protocol) || "SOCKS5H".equalsIgnoreCase(protocol);
    }

}
