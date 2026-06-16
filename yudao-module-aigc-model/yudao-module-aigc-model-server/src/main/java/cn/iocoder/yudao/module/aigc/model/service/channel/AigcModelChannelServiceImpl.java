package cn.iocoder.yudao.module.aigc.model.service.channel;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.aigc.model.controller.admin.channel.vo.AigcModelChannelCloneReqVO;
import cn.iocoder.yudao.module.aigc.model.controller.admin.channel.vo.AigcModelChannelPageReqVO;
import cn.iocoder.yudao.module.aigc.model.controller.admin.channel.vo.AigcModelChannelSaveReqVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelChannelDO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelRouteDO;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelChannelMapper;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelMapper;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelProviderMapper;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelRouteMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.aigc.model.enums.ErrorCodeConstants.*;

@Service
@Validated
public class AigcModelChannelServiceImpl implements AigcModelChannelService {

    @Resource
    private AigcModelChannelMapper channelMapper;

    @Resource
    private AigcModelMapper modelMapper;

    @Resource
    private AigcModelProviderMapper providerMapper;

    @Resource
    private AigcModelRouteMapper routeMapper;

    @Override
    public Long createChannel(AigcModelChannelSaveReqVO reqVO) {
        validateModelExists(reqVO.getModelId());
        validateProviderExists(reqVO.getProviderId());
        validateChannelUnique(null, reqVO.getModelId(), reqVO.getProviderId(), reqVO.getProviderModel());

        AigcModelChannelDO channel = BeanUtils.toBean(reqVO, AigcModelChannelDO.class);
        if (channel.getWeight() == null) {
            channel.setWeight(100);
        }
        if (channel.getPriority() == null) {
            channel.setPriority(100);
        }
        channelMapper.insert(channel);
        return channel.getId();
    }

    @Override
    public Long cloneChannel(AigcModelChannelCloneReqVO reqVO) {
        AigcModelChannelDO source = validateChannelExists(reqVO.getSourceChannelId());
        validateProviderExists(reqVO.getTargetProviderId());

        String providerModel = StrUtil.blankToDefault(reqVO.getProviderModel(), source.getProviderModel());
        validateChannelUnique(null, source.getModelId(), reqVO.getTargetProviderId(), providerModel);

        AigcModelChannelDO clone = BeanUtils.toBean(source, AigcModelChannelDO.class);
        clone.setId(null);
        clone.setProviderId(reqVO.getTargetProviderId());
        clone.setProviderModel(providerModel);
        clone.setName(StrUtil.blankToDefault(reqVO.getName(), source.getName() + "-克隆"));
        clone.setWeight(ObjectUtil.defaultIfNull(reqVO.getWeight(), source.getWeight()));
        clone.setStatus(CommonStatusEnum.DISABLE.getStatus());
        channelMapper.insert(clone);
        return clone.getId();
    }

    @Override
    public void updateChannel(AigcModelChannelSaveReqVO reqVO) {
        validateChannelExists(reqVO.getId());
        validateModelExists(reqVO.getModelId());
        validateProviderExists(reqVO.getProviderId());
        validateChannelUnique(reqVO.getId(), reqVO.getModelId(), reqVO.getProviderId(), reqVO.getProviderModel());

        channelMapper.updateById(BeanUtils.toBean(reqVO, AigcModelChannelDO.class));
    }

    @Override
    public void deleteChannel(Long id) {
        validateChannelExists(id);
        validateChannelNotReferencedByRoute(id);
        channelMapper.deleteById(id);
    }

    @Override
    public AigcModelChannelDO getChannel(Long id) {
        return channelMapper.selectById(id);
    }

    @Override
    public AigcModelChannelDO validateChannelExists(Long id) {
        AigcModelChannelDO channel = channelMapper.selectById(id);
        if (channel == null) {
            throw exception(MODEL_CHANNEL_NOT_EXISTS);
        }
        return channel;
    }

    @Override
    public AigcModelChannelDO validateChannelExistsAndEnable(Long id) {
        AigcModelChannelDO channel = validateChannelExists(id);
        if (!CommonStatusEnum.isEnable(channel.getStatus())) {
            throw exception(MODEL_CHANNEL_DISABLED);
        }
        return channel;
    }

    @Override
    public PageResult<AigcModelChannelDO> getChannelPage(AigcModelChannelPageReqVO reqVO) {
        return channelMapper.selectPage(reqVO);
    }

    @Override
    public List<AigcModelChannelDO> listChannelsByModelId(Long modelId) {
        return channelMapper.selectListByModelId(modelId);
    }

    @Override
    public List<AigcModelChannelDO> listEnabledChannelsByModelId(Long modelId) {
        return channelMapper.selectEnabledListByModelId(modelId);
    }

    @Override
    public void updateChannelStatus(Long id, Integer status) {
        validateChannelExists(id);
        channelMapper.updateById(new AigcModelChannelDO().setId(id).setStatus(status));
    }

    private void validateModelExists(Long modelId) {
        if (TenantUtils.executeIgnore(() -> modelMapper.selectById(modelId)) == null) {
            throw exception(MODEL_NOT_EXISTS);
        }
    }

    private void validateProviderExists(Long providerId) {
        if (TenantUtils.executeIgnore(() -> providerMapper.selectById(providerId)) == null) {
            throw exception(MODEL_PROVIDER_NOT_EXISTS);
        }
    }

    private void validateChannelUnique(Long id, Long modelId, Long providerId, String providerModel) {
        Long count = channelMapper.selectCountByModelIdAndProviderModel(id, modelId, providerId, providerModel);
        if (count > 0) {
            throw exception(MODEL_CHANNEL_DUPLICATE);
        }
    }

    private void validateChannelNotReferencedByRoute(Long channelId) {
        List<AigcModelRouteDO> routes = routeMapper.selectListWithChannelIds().stream()
                .filter(route -> containsChannelId(route.getChannelIds(), channelId))
                .toList();
        if (routes.isEmpty()) {
            return;
        }
        String routeNames = routes.stream()
                .map(AigcModelRouteDO::getName)
                .collect(Collectors.joining("、"));
        throw exception(MODEL_CHANNEL_REFERENCED_BY_ROUTE, routeNames);
    }

    private boolean containsChannelId(String channelIds, Long channelId) {
        if (StrUtil.isBlank(channelIds) || !JSONUtil.isTypeJSONArray(channelIds)) {
            return false;
        }
        return JSONUtil.toList(channelIds, Long.class).contains(channelId);
    }

}
