package cn.iocoder.yudao.module.aigc.billing.service.freeze;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcQuotaFreezeDO;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcWalletDO;
import cn.iocoder.yudao.module.aigc.billing.dal.mysql.AigcQuotaFreezeMapper;
import cn.iocoder.yudao.module.aigc.billing.dal.mysql.AigcWalletMapper;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingConfirmReqDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingFreezeReqDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingFreezeRespDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingRecordCreateReqDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingReleaseReqDTO;
import cn.iocoder.yudao.module.aigc.billing.enums.AigcBillingFreezeStatusEnum;
import cn.iocoder.yudao.module.aigc.billing.enums.AigcBillingRecordTypeEnum;
import cn.iocoder.yudao.module.aigc.billing.service.no.AigcBillingNoGenerator;
import cn.iocoder.yudao.module.aigc.billing.service.record.AigcBillingRecordService;
import cn.iocoder.yudao.module.aigc.billing.service.wallet.AigcWalletService;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.aigc.billing.enums.AigcBillingCurrencyTypeEnum.POINT;
import static cn.iocoder.yudao.module.aigc.billing.enums.ErrorCodeConstants.FREEZE_AMOUNT_NOT_MATCH;
import static cn.iocoder.yudao.module.aigc.billing.enums.ErrorCodeConstants.FREEZE_ALREADY_CONFIRMED;
import static cn.iocoder.yudao.module.aigc.billing.enums.ErrorCodeConstants.FREEZE_ALREADY_RELEASED;
import static cn.iocoder.yudao.module.aigc.billing.enums.ErrorCodeConstants.FREEZE_NOT_EXISTS;
import static cn.iocoder.yudao.module.aigc.billing.enums.ErrorCodeConstants.FREEZE_STATUS_INVALID;
import static cn.iocoder.yudao.module.aigc.billing.enums.ErrorCodeConstants.WALLET_BALANCE_NOT_ENOUGH;
import static cn.iocoder.yudao.module.aigc.billing.enums.ErrorCodeConstants.WALLET_FROZEN_BALANCE_NOT_ENOUGH;

@Service
@Validated
public class AigcQuotaFreezeServiceImpl implements AigcQuotaFreezeService {

    @Resource
    private AigcQuotaFreezeMapper quotaFreezeMapper;
    @Resource
    private AigcWalletMapper walletMapper;
    @Resource
    private AigcWalletService walletService;
    @Resource
    private AigcBillingRecordService billingRecordService;
    @Resource
    private AigcBillingNoGenerator billingNoGenerator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AigcBillingFreezeRespDTO freeze(AigcBillingFreezeReqDTO reqDTO) {
        AigcWalletDO wallet = walletService.getOrCreateWallet(reqDTO.getUserId());
        
        AigcQuotaFreezeDO exists = quotaFreezeMapper.selectByBiz(reqDTO.getBizType(), reqDTO.getBizId());
        if (exists != null) {
            String status = exists.getStatus();
            if (AigcBillingFreezeStatusEnum.FROZEN.getCode().equals(status)) {
                return BeanUtils.toBean(exists, AigcBillingFreezeRespDTO.class);
            } else if (AigcBillingFreezeStatusEnum.CONFIRMED.getCode().equals(status)) {
                throw exception(FREEZE_ALREADY_CONFIRMED);
            } else if (AigcBillingFreezeStatusEnum.RELEASED.getCode().equals(status)) {
                throw exception(FREEZE_ALREADY_RELEASED);
            }
            throw exception(FREEZE_STATUS_INVALID);
        }
        
        if (walletMapper.freezeBalance(wallet.getId(), reqDTO.getAmount()) == 0) {
            throw exception(WALLET_BALANCE_NOT_ENOUGH);
        }
        
        AigcQuotaFreezeDO freeze = new AigcQuotaFreezeDO();
        freeze.setFreezeNo(billingNoGenerator.generateFreezeNo());
        freeze.setWalletId(wallet.getId());
        freeze.setUserId(reqDTO.getUserId());
        freeze.setBizType(reqDTO.getBizType());
        freeze.setBizId(reqDTO.getBizId());
        freeze.setTaskId(reqDTO.getTaskId());
        freeze.setTaskNo(reqDTO.getTaskNo());
        freeze.setAmount(reqDTO.getAmount());
        freeze.setConfirmedAmount(BigDecimal.ZERO);
        freeze.setReleasedAmount(BigDecimal.ZERO);
        freeze.setStatus(AigcBillingFreezeStatusEnum.FROZEN.getCode());
        freeze.setExpireTime(reqDTO.getExpireTime());
        
        try {
            quotaFreezeMapper.insert(freeze);
        } catch (DuplicateKeyException ex) {
            AigcQuotaFreezeDO duplicateExists = quotaFreezeMapper.selectByBiz(reqDTO.getBizType(), reqDTO.getBizId());
            if (duplicateExists != null && AigcBillingFreezeStatusEnum.FROZEN.getCode().equals(duplicateExists.getStatus())) {
                return BeanUtils.toBean(duplicateExists, AigcBillingFreezeRespDTO.class);
            }
            throw exception(FREEZE_STATUS_INVALID);
        }
        
        AigcBillingRecordCreateReqDTO record = new AigcBillingRecordCreateReqDTO();
        record.setWalletId(wallet.getId());
        record.setUserId(reqDTO.getUserId());
        record.setBizType(reqDTO.getBizType());
        record.setBizId(buildRecordBizId(reqDTO.getBizId(), AigcBillingRecordTypeEnum.FREEZE.getCode()));
        record.setRecordType(AigcBillingRecordTypeEnum.FREEZE.getCode());
        record.setTitle(reqDTO.getTitle());
        record.setAmount(BigDecimal.ZERO);
        record.setFreezeId(freeze.getId());
        record.setTaskId(reqDTO.getTaskId());
        record.setCurrencyType(POINT.getCode());
        record.setPriceSnapshot(reqDTO.getPriceSnapshot());
        billingRecordService.createBillingRecord(record);
        
        return BeanUtils.toBean(freeze, AigcBillingFreezeRespDTO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmFreeze(AigcBillingConfirmReqDTO reqDTO) {
        AigcQuotaFreezeDO freeze = validateFreezeExists(reqDTO.getFreezeId());
        if (AigcBillingFreezeStatusEnum.CONFIRMED.getCode().equals(freeze.getStatus())) {
            return;
        }
        if (!AigcBillingFreezeStatusEnum.FROZEN.getCode().equals(freeze.getStatus())) {
            throw exception(FREEZE_STATUS_INVALID);
        }
        if (freeze.getAmount().compareTo(reqDTO.getActualAmount()) != 0) {
            throw exception(FREEZE_AMOUNT_NOT_MATCH);
        }
        
        if (walletMapper.confirmFrozen(freeze.getWalletId(), reqDTO.getActualAmount()) == 0) {
            throw exception(WALLET_FROZEN_BALANCE_NOT_ENOUGH);
        }
        
        if (quotaFreezeMapper.updateConfirmed(freeze.getId(), reqDTO.getActualAmount(), reqDTO.getTaskId(), reqDTO.getTaskNo(), LocalDateTime.now()) == 0) {
            if (AigcBillingFreezeStatusEnum.CONFIRMED.getCode().equals(validateFreezeExists(reqDTO.getFreezeId()).getStatus())) {
                return;
            }
            throw exception(FREEZE_STATUS_INVALID);
        }
        
        freeze = validateFreezeExists(reqDTO.getFreezeId());
        AigcBillingRecordCreateReqDTO record = new AigcBillingRecordCreateReqDTO();
        record.setWalletId(freeze.getWalletId());
        record.setUserId(freeze.getUserId());
        record.setBizType(freeze.getBizType());
        record.setBizId(buildRecordBizId(freeze.getBizId(), AigcBillingRecordTypeEnum.CONSUME.getCode()));
        record.setRecordType(AigcBillingRecordTypeEnum.CONSUME.getCode());
        record.setTitle("AIGC 任务扣费");
        record.setAmount(reqDTO.getActualAmount().negate());
        record.setFreezeId(freeze.getId());
        record.setTaskId(freeze.getTaskId());
        record.setModelId(reqDTO.getModelId());
        record.setProviderId(reqDTO.getProviderId());
        record.setCurrencyType(POINT.getCode());
        record.setPriceSnapshot(reqDTO.getPriceSnapshot());
        billingRecordService.createBillingRecord(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseFreeze(AigcBillingReleaseReqDTO reqDTO) {
        AigcQuotaFreezeDO freeze = validateFreezeExists(reqDTO.getFreezeId());
        if (AigcBillingFreezeStatusEnum.RELEASED.getCode().equals(freeze.getStatus())) {
            return;
        }
        if (!AigcBillingFreezeStatusEnum.FROZEN.getCode().equals(freeze.getStatus())) {
            throw exception(FREEZE_STATUS_INVALID);
        }
        
        if (walletMapper.releaseFrozen(freeze.getWalletId(), freeze.getAmount()) == 0) {
            throw exception(WALLET_FROZEN_BALANCE_NOT_ENOUGH);
        }
        
        if (quotaFreezeMapper.updateReleased(freeze.getId(), freeze.getAmount(), reqDTO.getTaskId(), reqDTO.getTaskNo(), reqDTO.getReason(), LocalDateTime.now()) == 0) {
            if (AigcBillingFreezeStatusEnum.RELEASED.getCode().equals(validateFreezeExists(reqDTO.getFreezeId()).getStatus())) {
                return;
            }
            throw exception(FREEZE_STATUS_INVALID);
        }
        
        freeze = validateFreezeExists(reqDTO.getFreezeId());
        AigcBillingRecordCreateReqDTO record = new AigcBillingRecordCreateReqDTO();
        record.setWalletId(freeze.getWalletId());
        record.setUserId(freeze.getUserId());
        record.setBizType(freeze.getBizType());
        record.setBizId(buildRecordBizId(freeze.getBizId(), AigcBillingRecordTypeEnum.RELEASE.getCode()));
        record.setRecordType(AigcBillingRecordTypeEnum.RELEASE.getCode());
        record.setTitle("AIGC 任务释放冻结");
        record.setAmount(BigDecimal.ZERO);
        record.setFreezeId(freeze.getId());
        record.setTaskId(freeze.getTaskId());
        record.setCurrencyType(POINT.getCode());
        billingRecordService.createBillingRecord(record);
    }

    @Override
    public PageResult<AigcQuotaFreezeDO> getFreezePage(PageParam reqVO) {
        return quotaFreezeMapper.selectPage(reqVO);
    }

    @Override
    public AigcQuotaFreezeDO getFreeze(Long id) {
        return validateFreezeExists(id);
    }

    @Override
    public PageResult<AigcQuotaFreezeDO> getUserFreezePage(PageParam reqVO, Long userId) {
        return quotaFreezeMapper.selectUserPage(reqVO, userId);
    }

    @Override
    public int releaseTimeoutFreezes(Integer limit) {
        List<AigcQuotaFreezeDO> freezes = quotaFreezeMapper.selectTimeoutFrozenList(LocalDateTime.now(), limit == null ? 100 : limit);
        int count = 0;
        for (AigcQuotaFreezeDO freeze : freezes) {
            AigcBillingReleaseReqDTO reqDTO = new AigcBillingReleaseReqDTO();
            reqDTO.setFreezeId(freeze.getId());
            reqDTO.setTaskId(freeze.getTaskId());
            reqDTO.setTaskNo(freeze.getTaskNo());
            reqDTO.setReason("冻结超时自动释放");
            releaseFreeze(reqDTO);
            count++;
        }
        return count;
    }

    private AigcQuotaFreezeDO validateFreezeExists(Long freezeId) {
        AigcQuotaFreezeDO freeze = quotaFreezeMapper.selectById(freezeId);
        if (freeze == null) {
            throw exception(FREEZE_NOT_EXISTS);
        }
        return freeze;
    }

    public static String buildRecordBizId(String bizId, String recordType) {
        return bizId + ":" + recordType;
    }

}
