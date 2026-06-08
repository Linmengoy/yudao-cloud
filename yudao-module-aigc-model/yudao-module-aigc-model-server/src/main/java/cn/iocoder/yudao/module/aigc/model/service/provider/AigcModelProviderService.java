package cn.iocoder.yudao.module.aigc.model.service.provider;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.model.controller.admin.provider.vo.AigcModelProviderPageReqVO;
import cn.iocoder.yudao.module.aigc.model.controller.admin.provider.vo.AigcModelProviderSaveReqVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelProviderDO;

import java.util.Collection;
import java.util.List;

public interface AigcModelProviderService {

    Long createProvider(AigcModelProviderSaveReqVO reqVO);

    void updateProvider(AigcModelProviderSaveReqVO reqVO);

    void deleteProvider(Long id);

    AigcModelProviderDO getProvider(Long id);

    AigcModelProviderDO getProviderWithProxy(Long id);

    AigcModelProviderDO validateProviderExists(Long id);

    AigcModelProviderDO validateProviderExistsAndEnable(Long id);

    PageResult<AigcModelProviderDO> getProviderPage(AigcModelProviderPageReqVO reqVO);

    List<AigcModelProviderDO> getProviderList(Collection<Long> ids);

    void updateProviderStatus(Long id, Integer status);

    void testProvider(Long id);

}
