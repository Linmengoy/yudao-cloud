package cn.iocoder.yudao.module.aigc.model.service.price;

import cn.iocoder.yudao.module.aigc.model.controller.admin.price.vo.AigcModelPriceSaveReqVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelPriceDO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelPriceCalculateReqDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelPriceCalculateRespDTO;

import java.util.List;

public interface AigcModelPriceService {

    Long createPrice(AigcModelPriceSaveReqVO reqVO);

    void updatePrice(AigcModelPriceSaveReqVO reqVO);

    void deletePrice(Long id);

    AigcModelPriceDO getPrice(Long id);

    List<AigcModelPriceDO> getPriceList(Long modelId, String capability);

    void updatePriceStatus(Long id, Integer status);

    AigcModelPriceCalculateRespDTO calculatePrice(AigcModelPriceCalculateReqDTO reqDTO);

}