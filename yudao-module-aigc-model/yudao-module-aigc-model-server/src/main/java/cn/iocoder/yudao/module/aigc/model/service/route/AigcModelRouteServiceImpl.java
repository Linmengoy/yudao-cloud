package cn.iocoder.yudao.module.aigc.model.service.route;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.model.controller.admin.route.vo.AigcModelRoutePageReqVO;
import cn.iocoder.yudao.module.aigc.model.controller.admin.route.vo.AigcModelRouteSaveReqVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelRouteDO;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelMapper;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelRouteMapper;
import cn.iocoder.yudao.module.aigc.model.enums.AigcModelRouteStrategyEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.aigc.model.enums.ErrorCodeConstants.*;

@Service
@Validated
public class AigcModelRouteServiceImpl implements AigcModelRouteService {

    @Resource
    private AigcModelRouteMapper routeMapper;

    @Resource
    private AigcModelMapper modelMapper;

    @Override
    public Long createRoute(AigcModelRouteSaveReqVO reqVO) {
        AigcModelRouteDO route = BeanUtils.toBean(reqVO, AigcModelRouteDO.class);
        routeMapper.insert(route);
        return route.getId();
    }

    @Override
    public void updateRoute(AigcModelRouteSaveReqVO reqVO) {
        validateRouteExists(reqVO.getId());

        AigcModelRouteDO updateObj = BeanUtils.toBean(reqVO, AigcModelRouteDO.class);
        routeMapper.updateById(updateObj);
    }

    @Override
    public void deleteRoute(Long id) {
        validateRouteExists(id);
        routeMapper.deleteById(id);
    }

    @Override
    public AigcModelRouteDO getRoute(Long id) {
        return routeMapper.selectById(id);
    }

    @Override
    public PageResult<AigcModelRouteDO> getRoutePage(AigcModelRoutePageReqVO reqVO) {
        return routeMapper.selectPage(reqVO);
    }

    @Override
    public void updateRouteStatus(Long id, Integer status) {
        validateRouteExists(id);
        routeMapper.updateById(new AigcModelRouteDO().setId(id).setStatus(status));
    }

    @Override
    public List<AigcModelRouteDO> listRoutes(String taskType, String capability) {
        return routeMapper.selectListByTaskTypeAndCapability(taskType, capability);
    }

    @Override
    public Long route(String taskType, String capability) {
        List<AigcModelRouteDO> routes = routeMapper.selectListByTaskTypeAndCapability(taskType, capability);
        if (routes.isEmpty()) {
            return null;
        }

        AigcModelRouteDO route = routes.get(0);
        AigcModelRouteStrategyEnum strategy;
        try {
            strategy = AigcModelRouteStrategyEnum.getByValue(route.getStrategy());
        } catch (IllegalArgumentException ex) {
            return null;
        }

        List<Long> modelIds = parseModelIds(route.getModelIds());
        if (modelIds.isEmpty()) {
            return null;
        }

        switch (strategy) {
            case FIXED_MODEL:
                return modelIds.get(0);
            case ROUND_ROBIN:
                return modelIds.get((int) (System.currentTimeMillis() / 1000 % modelIds.size()));
            case LOWEST_COST:
            case HIGHEST_SUCCESS_RATE:
            case FASTEST_RESPONSE:
            default:
                return modelIds.get(0);
        }
    }

    private List<Long> parseModelIds(String modelIds) {
        if (modelIds == null || modelIds.isBlank()) {
            return List.of();
        }
        try {
            return JSONUtil.toList(modelIds, Long.class);
        } catch (Exception ignored) {
            return Arrays.stream(modelIds.split(","))
                    .map(String::trim)
                    .filter(item -> !item.isBlank())
                    .map(item -> {
                        try {
                            return Long.valueOf(item);
                        } catch (NumberFormatException ex) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();
        }
    }

    private void validateRouteExists(Long id) {
        if (routeMapper.selectById(id) == null) {
            throw exception(MODEL_ROUTE_NOT_EXISTS);
        }
    }

}
