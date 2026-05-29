package cn.iocoder.yudao.module.aigc.billing.service.packageconfig;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.billing.controller.admin.packageconfig.vo.AigcRechargePackagePageReqVO;
import cn.iocoder.yudao.module.aigc.billing.controller.admin.packageconfig.vo.AigcRechargePackageSaveReqVO;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcRechargePackageDO;

import java.util.List;

public interface AigcRechargePackageService {

    Long createRechargePackage(AigcRechargePackageSaveReqVO createReqVO);
    void updateRechargePackage(AigcRechargePackageSaveReqVO updateReqVO);
    void deleteRechargePackage(Long id);
    AigcRechargePackageDO getRechargePackage(Long id);
    AigcRechargePackageDO getEnabledRechargePackage(Long id);
    PageResult<AigcRechargePackageDO> getRechargePackagePage(AigcRechargePackagePageReqVO pageReqVO);
    List<AigcRechargePackageDO> getEnabledRechargePackageList();

}
