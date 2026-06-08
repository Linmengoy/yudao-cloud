package cn.iocoder.yudao.module.aigc.model.service.provider;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.aigc.model.controller.admin.provider.vo.AigcModelProviderPageReqVO;
import cn.iocoder.yudao.module.aigc.model.controller.admin.provider.vo.AigcModelProviderSaveReqVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelProxyDO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelProviderDO;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelMapper;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelProviderMapper;
import cn.iocoder.yudao.module.aigc.model.service.proxy.AigcModelProxyService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.aigc.model.enums.ErrorCodeConstants.*;

@Service
@Validated
public class AigcModelProviderServiceImpl implements AigcModelProviderService {

    @Resource
    private AigcModelProviderMapper providerMapper;

    @Resource
    private AigcModelMapper modelMapper;
    @Resource
    private AigcModelProxyService proxyService;

    @Override
    public Long createProvider(AigcModelProviderSaveReqVO reqVO) {
        validateProviderCodeUnique(null, reqVO.getCode());
        validateProxyConfig(reqVO);

        AigcModelProviderDO provider = BeanUtils.toBean(reqVO, AigcModelProviderDO.class);
        providerMapper.insert(provider);
        return provider.getId();
    }

    @Override
    public void updateProvider(AigcModelProviderSaveReqVO reqVO) {
        AigcModelProviderDO provider = validateProviderExists(reqVO.getId());
        validateProviderCodeUnique(reqVO.getId(), reqVO.getCode());
        validateProxyConfig(reqVO);

        AigcModelProviderDO updateObj = BeanUtils.toBean(reqVO, AigcModelProviderDO.class);
        if (StrUtil.isBlank(reqVO.getApiKey())) {
            updateObj.setApiKey(provider.getApiKey());
        }
        if (StrUtil.isBlank(reqVO.getSecretKey())) {
            updateObj.setSecretKey(provider.getSecretKey());
        }
        if (!Boolean.TRUE.equals(reqVO.getProxyEnabled())) {
            updateObj.setProxyId(null);
        }
        providerMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProvider(Long id) {
        validateProviderExists(id);

        Long modelCount = modelMapper.selectCountByProviderId(id);
        if (modelCount > 0) {
            throw exception(MODEL_PROVIDER_HAS_MODEL);
        }

        providerMapper.deleteById(id);
    }

    @Override
    public AigcModelProviderDO getProvider(Long id) {
        return providerMapper.selectById(id);
    }

    @Override
    public AigcModelProviderDO getProviderWithProxy(Long id) {
        return fillProxyConfig(getProvider(id));
    }

    @Override
    public AigcModelProviderDO validateProviderExists(Long id) {
        AigcModelProviderDO provider = providerMapper.selectById(id);
        if (provider == null) {
            throw exception(MODEL_PROVIDER_NOT_EXISTS);
        }
        return provider;
    }

    @Override
    public AigcModelProviderDO validateProviderExistsAndEnable(Long id) {
        AigcModelProviderDO provider = validateProviderExists(id);
        if (!CommonStatusEnum.isEnable(provider.getStatus())) {
            throw exception(MODEL_PROVIDER_DISABLED);
        }
        return fillProxyConfig(provider);
    }

    @Override
    public PageResult<AigcModelProviderDO> getProviderPage(AigcModelProviderPageReqVO reqVO) {
        PageResult<AigcModelProviderDO> pageResult = providerMapper.selectPage(reqVO);
        pageResult.getList().forEach(this::fillProxyName);
        return pageResult;
    }

    @Override
    public List<AigcModelProviderDO> getProviderList(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        return providerMapper.selectByIds(ids);
    }

    @Override
    public void updateProviderStatus(Long id, Integer status) {
        validateProviderExists(id);
        providerMapper.updateById(new AigcModelProviderDO().setId(id).setStatus(status));
    }

    @Override
    public void testProvider(Long id) {
        validateProviderExists(id);
    }

    private void validateProviderCodeUnique(Long id, String code) {
        AigcModelProviderDO provider = providerMapper.selectByCode(code);
        if (provider == null) {
            return;
        }
        if (!ObjectUtil.equal(provider.getId(), id)) {
            throw exception(MODEL_PROVIDER_CODE_DUPLICATE);
        }
    }

    private void validateProxyConfig(AigcModelProviderSaveReqVO reqVO) {
        if (!Boolean.TRUE.equals(reqVO.getProxyEnabled())) {
            return;
        }
        if (reqVO.getProxyId() == null) {
            throw exception(MODEL_PROXY_NOT_EXISTS);
        }
        proxyService.validateProxyExistsAndEnable(reqVO.getProxyId());
    }

    private AigcModelProviderDO fillProxyConfig(AigcModelProviderDO provider) {
        if (provider == null || !Boolean.TRUE.equals(provider.getProxyEnabled()) || provider.getProxyId() == null) {
            return provider;
        }
        AigcModelProxyDO proxy = proxyService.validateProxyExistsAndEnable(provider.getProxyId());
        provider.setProxyName(proxy.getName());
        provider.setProxyProtocol(proxy.getProtocol());
        provider.setProxyHost(proxy.getHost());
        provider.setProxyPort(proxy.getPort());
        provider.setProxyUsername(proxy.getUsername());
        provider.setProxyPassword(proxy.getPassword());
        return provider;
    }

    private void fillProxyName(AigcModelProviderDO provider) {
        if (provider == null || provider.getProxyId() == null) {
            return;
        }
        AigcModelProxyDO proxy = proxyService.getProxy(provider.getProxyId());
        if (proxy != null) {
            provider.setProxyName(proxy.getName());
        }
    }

}
