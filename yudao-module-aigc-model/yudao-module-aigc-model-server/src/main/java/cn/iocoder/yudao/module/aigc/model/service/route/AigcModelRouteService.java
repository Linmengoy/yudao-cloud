package cn.iocoder.yudao.module.aigc.model.service.route;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.model.controller.admin.route.vo.AigcModelRoutePageReqVO;
import cn.iocoder.yudao.module.aigc.model.controller.admin.route.vo.AigcModelRouteSaveReqVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelRouteDO;

import java.util.List;

public interface AigcModelRouteService {

    Long createRoute(AigcModelRouteSaveReqVO reqVO);

    void updateRoute(AigcModelRouteSaveReqVO reqVO);

    void deleteRoute(Long id);

    AigcModelRouteDO getRoute(Long id);

    PageResult<AigcModelRouteDO> getRoutePage(AigcModelRoutePageReqVO reqVO);

    void updateRouteStatus(Long id, Integer status);

    List<AigcModelRouteDO> listRoutes(String taskType, String capability);

    Long route(String taskType, String capability);

}