package cn.iocoder.yudao.module.aigc.billing.service.record;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcBillingRecordDO;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcWalletDO;
import cn.iocoder.yudao.module.aigc.billing.dal.mysql.AigcBillingRecordMapper;
import cn.iocoder.yudao.module.aigc.billing.dal.mysql.AigcWalletMapper;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingRecordCreateReqDTO;
import cn.iocoder.yudao.module.aigc.billing.service.no.AigcBillingNoGenerator;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.module.aigc.billing.enums.AigcBillingCurrencyTypeEnum.POINT;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.aigc.billing.enums.ErrorCodeConstants.BILLING_RECORD_NOT_EXISTS;
import static cn.iocoder.yudao.module.aigc.billing.enums.ErrorCodeConstants.WALLET_NOT_EXISTS;

@Service
@Validated
public class AigcBillingRecordServiceImpl implements AigcBillingRecordService {

    @Resource
    private AigcBillingRecordMapper billingRecordMapper;
    @Resource
    private AigcWalletMapper walletMapper;
    @Resource
    private AigcBillingNoGenerator billingNoGenerator;

    @Override
    public Long createBillingRecord(AigcBillingRecordCreateReqDTO reqDTO) {
        AigcWalletDO wallet = walletMapper.selectById(reqDTO.getWalletId());
        if (wallet == null) {
            throw exception(WALLET_NOT_EXISTS);
        }
        
        if (reqDTO.getBizType() != null && reqDTO.getBizId() != null) {
            AigcBillingRecordDO exists = billingRecordMapper.selectByBiz(reqDTO.getBizType(), reqDTO.getBizId());
            if (exists != null) {
                return exists.getId();
            }
        }
        
        AigcBillingRecordDO record = BeanUtils.toBean(reqDTO, AigcBillingRecordDO.class);
        record.setRecordNo(billingNoGenerator.generateBillingRecordNo());
        record.setBalanceAfter(wallet.getBalance());
        record.setFrozenBalanceAfter(wallet.getFrozenBalance());
        if (record.getCurrencyType() == null) {
            record.setCurrencyType(POINT.getCode());
        }
        
        try {
            billingRecordMapper.insert(record);
        } catch (DuplicateKeyException ex) {
            if (reqDTO.getBizType() != null && reqDTO.getBizId() != null) {
                AigcBillingRecordDO exists = billingRecordMapper.selectByBiz(reqDTO.getBizType(), reqDTO.getBizId());
                if (exists != null) {
                    return exists.getId();
                }
            }
            throw ex;
        }
        
        return record.getId();
    }

    @Override
    public AigcBillingRecordDO getBillingRecord(Long id) {
        AigcBillingRecordDO record = billingRecordMapper.selectById(id);
        if (record == null) {
            throw exception(BILLING_RECORD_NOT_EXISTS);
        }
        return record;
    }

    @Override
    public PageResult<AigcBillingRecordDO> getBillingRecordPage(PageParam reqVO) {
        return billingRecordMapper.selectPage(reqVO);
    }

    @Override
    public PageResult<AigcBillingRecordDO> getUserBillingRecordPage(PageParam reqVO, Long userId) {
        return billingRecordMapper.selectUserPage(reqVO, userId);
    }

}
