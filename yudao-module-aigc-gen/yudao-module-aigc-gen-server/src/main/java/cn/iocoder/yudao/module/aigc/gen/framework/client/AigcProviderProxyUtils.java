package cn.iocoder.yudao.module.aigc.gen.framework.client;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.iocoder.yudao.module.aigc.gen.framework.client.dto.AigcProviderSubmitReqDTO;

import java.net.InetSocketAddress;
import java.net.Proxy;

public final class AigcProviderProxyUtils {

    private AigcProviderProxyUtils() {
    }

    public static HttpRequest applyProxy(HttpRequest request, AigcProviderSubmitReqDTO reqDTO) {
        if (reqDTO == null || !Boolean.TRUE.equals(reqDTO.getProxyEnabled()) || StrUtil.isBlank(reqDTO.getProxyHost()) || reqDTO.getProxyPort() == null) {
            return request;
        }
        request.setProxy(new Proxy(resolveProxyType(reqDTO.getProxyProtocol()), new InetSocketAddress(reqDTO.getProxyHost(), reqDTO.getProxyPort())));
        if (StrUtil.isNotBlank(reqDTO.getProxyUsername()) || StrUtil.isNotBlank(reqDTO.getProxyPassword())) {
            request.basicProxyAuth(StrUtil.blankToDefault(reqDTO.getProxyUsername(), ""), StrUtil.blankToDefault(reqDTO.getProxyPassword(), ""));
        }
        return request;
    }

    private static Proxy.Type resolveProxyType(String protocol) {
        if ("SOCKS5".equalsIgnoreCase(protocol) || "SOCKS5H".equalsIgnoreCase(protocol)) {
            return Proxy.Type.SOCKS;
        }
        return Proxy.Type.HTTP;
    }

}
