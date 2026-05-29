package cn.iocoder.yudao.module.aigc.billing.service.packageconfig;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.billing.controller.admin.packageconfig.vo.AigcRechargePackagePageReqVO;
import cn.iocoder.yudao.module.aigc.billing.controller.admin.packageconfig.vo.AigcRechargePackageSaveReqVO;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcRechargePackageDO;
import cn.iocoder.yudao.module.aigc.billing.dal.mysql.AigcRechargePackageMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.aigc.billing.enums.ErrorCodeConstants.RECHARGE_PACKAGE_NOT_EXISTS;

@Service
@Validated
public class AigcRechargePackageServiceImpl implements AigcRechargePackageService {

    @Resource
    private AigcRechargePackageMapper rechargePackageMapper;

    @Override
    public Long createRechargePackage(AigcRechargePackageSaveReqVO createReqVO) {
        AigcRechargePackageDO rechargePackage = BeanUtils.toBean(createReqVO, AigcRechargePackageDO.class);
        fillAmount(rechargePackage);
        rechargePackageMapper.insert(rechargePackage);
        return rechargePackage.getId();
    }

    @Override
    public void updateRechargePackage(AigcRechargePackageSaveReqVO updateReqVO) {
        validateRechargePackageExists(updateReqVO.getId());
        AigcRechargePackageDO updateObj = BeanUtils.toBean(updateReqVO, AigcRechargePackageDO.class);
        fillAmount(updateObj);
        rechargePackageMapper.updateById(updateObj);
    }

    @Override
    public void deleteRechargePackage(Long id) {
        validateRechargePackageExists(id);
        rechargePackageMapper.deleteById(id);
    }

    private AigcRechargePackageDO validateRechargePackageExists(Long id) {
        AigcRechargePackageDO rechargePackage = rechargePackageMapper.selectById(id);
        if (rechargePackage == null) {
            throw exception(RECHARGE_PACKAGE_NOT_EXISTS);
        }
        return rechargePackage;
    }

    @Override
    public AigcRechargePackageDO getRechargePackage(Long id) {
        return rechargePackageMapper.selectById(id);
    }

    @Override
    public AigcRechargePackageDO getEnabledRechargePackage(Long id) {
        AigcRechargePackageDO rechargePackage = validateRechargePackageExists(id);
        if (!CommonStatusEnum.ENABLE.getStatus().equals(rechargePackage.getStatus())) {
            throw exception(RECHARGE_PACKAGE_NOT_EXISTS);
        }
        return rechargePackage;
    }

    @Override
    public PageResult<AigcRechargePackageDO> getRechargePackagePage(AigcRechargePackagePageReqVO pageReqVO) {
        return rechargePackageMapper.selectPage(pageReqVO);
    }

    @Override
    public List<AigcRechargePackageDO> getEnabledRechargePackageList() {
        return rechargePackageMapper.selectListByStatus(CommonStatusEnum.ENABLE.getStatus());
    }

    private void fillAmount(AigcRechargePackageDO rechargePackage) {
        BigDecimal giftAmount = rechargePackage.getGiftAmount() == null ? BigDecimal.ZERO : rechargePackage.getGiftAmount();
        rechargePackage.setGiftAmount(giftAmount);
        rechargePackage.setTotalPointAmount(rechargePackage.getPointAmount().add(giftAmount));
    }

}
