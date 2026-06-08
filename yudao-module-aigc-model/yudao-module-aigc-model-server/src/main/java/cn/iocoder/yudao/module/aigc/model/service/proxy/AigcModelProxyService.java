package cn.iocoder.yudao.module.aigc.model.service.proxy;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.model.controller.admin.proxy.vo.AigcModelProxyPageReqVO;
import cn.iocoder.yudao.module.aigc.model.controller.admin.proxy.vo.AigcModelProxySaveReqVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelProxyDO;

import java.util.List;

public interface AigcModelProxyService {

    Long createProxy(AigcModelProxySaveReqVO reqVO);

    void updateProxy(AigcModelProxySaveReqVO reqVO);

    void deleteProxy(Long id);

    AigcModelProxyDO getProxy(Long id);

    AigcModelProxyDO validateProxyExists(Long id);

    AigcModelProxyDO validateProxyExistsAndEnable(Long id);

    PageResult<AigcModelProxyDO> getProxyPage(AigcModelProxyPageReqVO reqVO);

    List<AigcModelProxyDO> getSimpleProxyList();

    void updateProxyStatus(Long id, Integer status);

    Long testProxy(Long id);

}
