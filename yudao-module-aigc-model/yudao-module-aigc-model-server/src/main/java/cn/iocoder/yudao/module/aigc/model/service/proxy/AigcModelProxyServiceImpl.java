package cn.iocoder.yudao.module.aigc.model.service.proxy;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.model.controller.admin.proxy.vo.AigcModelProxyPageReqVO;
import cn.iocoder.yudao.module.aigc.model.controller.admin.proxy.vo.AigcModelProxySaveReqVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelProxyDO;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelProviderMapper;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelProxyMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.aigc.model.enums.ErrorCodeConstants.*;

@Service
@Validated
public class AigcModelProxyServiceImpl implements AigcModelProxyService {

    private static final Set<String> SUPPORTED_PROTOCOLS = Set.of("HTTP", "HTTPS", "SOCKS5", "SOCKS5H");

    @Resource
    private AigcModelProxyMapper proxyMapper;
    @Resource
    private AigcModelProviderMapper providerMapper;

    @Override
    public Long createProxy(AigcModelProxySaveReqVO reqVO) {
        validateProxyNameUnique(null, reqVO.getName());
        validateProxyConfig(reqVO);

        AigcModelProxyDO proxy = BeanUtils.toBean(reqVO, AigcModelProxyDO.class);
        proxyMapper.insert(proxy);
        return proxy.getId();
    }

    @Override
    public void updateProxy(AigcModelProxySaveReqVO reqVO) {
        AigcModelProxyDO proxy = validateProxyExists(reqVO.getId());
        validateProxyNameUnique(reqVO.getId(), reqVO.getName());
        validateProxyConfig(reqVO);

        AigcModelProxyDO updateObj = BeanUtils.toBean(reqVO, AigcModelProxyDO.class);
        if (StrUtil.isBlank(reqVO.getPassword())) {
            updateObj.setPassword(proxy.getPassword());
        }
        proxyMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProxy(Long id) {
        validateProxyExists(id);
        if (providerMapper.selectCountByProxyId(id) > 0) {
            throw exception(MODEL_PROXY_HAS_PROVIDER);
        }
        proxyMapper.deleteById(id);
    }

    @Override
    public AigcModelProxyDO getProxy(Long id) {
        return proxyMapper.selectById(id);
    }

    @Override
    public AigcModelProxyDO validateProxyExists(Long id) {
        AigcModelProxyDO proxy = proxyMapper.selectById(id);
        if (proxy == null) {
            throw exception(MODEL_PROXY_NOT_EXISTS);
        }
        return proxy;
    }

    @Override
    public AigcModelProxyDO validateProxyExistsAndEnable(Long id) {
        AigcModelProxyDO proxy = validateProxyExists(id);
        if (!CommonStatusEnum.isEnable(proxy.getStatus())) {
            throw exception(MODEL_PROXY_NOT_EXISTS);
        }
        return proxy;
    }

    @Override
    public PageResult<AigcModelProxyDO> getProxyPage(AigcModelProxyPageReqVO reqVO) {
        return proxyMapper.selectPage(reqVO);
    }

    @Override
    public List<AigcModelProxyDO> getSimpleProxyList() {
        return proxyMapper.selectListByStatus(CommonStatusEnum.ENABLE.getStatus());
    }

    @Override
    public void updateProxyStatus(Long id, Integer status) {
        validateProxyExists(id);
        proxyMapper.updateById(new AigcModelProxyDO().setId(id).setStatus(status));
    }

    private void validateProxyNameUnique(Long id, String name) {
        AigcModelProxyDO proxy = proxyMapper.selectByName(name);
        if (proxy == null) {
            return;
        }
        if (!ObjectUtil.equal(proxy.getId(), id)) {
            throw exception(MODEL_PROXY_NAME_DUPLICATE);
        }
    }

    private void validateProxyConfig(AigcModelProxySaveReqVO reqVO) {
        if (!SUPPORTED_PROTOCOLS.contains(reqVO.getProtocol())) {
            throw exception(MODEL_PROXY_CONFIG_INVALID);
        }
        if (reqVO.getPort() == null || reqVO.getPort() < 1 || reqVO.getPort() > 65535) {
            throw exception(MODEL_PROXY_CONFIG_INVALID);
        }
    }

}
