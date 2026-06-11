package cn.iocoder.yudao.module.aigc.billing.service.cost;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcCostRecordDO;
import cn.iocoder.yudao.module.aigc.billing.dal.mysql.AigcCostRecordMapper;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcCostRecordCreateReqDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcGrossProfitRespDTO;
import cn.iocoder.yudao.module.aigc.billing.service.no.AigcBillingNoGenerator;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.aigc.billing.enums.AigcBillingCurrencyTypeEnum.POINT;
import static cn.iocoder.yudao.module.aigc.billing.enums.ErrorCodeConstants.COST_RECORD_NOT_EXISTS;

@Service
@Validated
public class AigcCostRecordServiceImpl implements AigcCostRecordService {

    @Resource
    private AigcCostRecordMapper costRecordMapper;
    @Resource
    private AigcBillingNoGenerator billingNoGenerator;

    @Override
    public Long createCostRecord(AigcCostRecordCreateReqDTO reqDTO) {
        AigcCostRecordDO exists = selectExisting(reqDTO);
        if (exists != null) {
            return exists.getId();
        }
        
        AigcCostRecordDO record = BeanUtils.toBean(reqDTO, AigcCostRecordDO.class);
        record.setCostNo(billingNoGenerator.generateCostRecordNo());
        BigDecimal saleAmount = defaultZero(reqDTO.getSaleAmount());
        BigDecimal costAmount = defaultZero(reqDTO.getCostAmount());
        record.setSaleAmount(saleAmount);
        record.setCostAmount(costAmount);
        record.setGrossProfit(saleAmount.subtract(costAmount));
        record.setGrossProfitRate(calculateRate(record.getGrossProfit(), saleAmount));
        if (record.getCurrencyType() == null) {
            record.setCurrencyType(POINT.getCode());
        }
        record.setStatus("SUCCESS");
        
        try {
            costRecordMapper.insert(record);
        } catch (DuplicateKeyException ex) {
            AigcCostRecordDO duplicateExists = selectExisting(reqDTO);
            if (duplicateExists != null) {
                return duplicateExists.getId();
            }
            throw ex;
        }
        
        return record.getId();
    }

    @Override
    public AigcCostRecordDO getCostRecord(Long id) {
        AigcCostRecordDO record = costRecordMapper.selectById(id);
        if (record == null) {
            throw exception(COST_RECORD_NOT_EXISTS);
        }
        return record;
    }

    @Override
    public AigcGrossProfitRespDTO calculateGrossProfit(Long taskId) {
        List<AigcCostRecordDO> records = costRecordMapper.selectListByTaskId(taskId);
        if (records.isEmpty()) {
            throw exception(COST_RECORD_NOT_EXISTS);
        }
        BigDecimal costAmount = BigDecimal.ZERO;
        BigDecimal saleAmount = BigDecimal.ZERO;
        String currencyType = null;
        for (AigcCostRecordDO record : records) {
            costAmount = costAmount.add(defaultZero(record.getCostAmount()));
            saleAmount = saleAmount.add(defaultZero(record.getSaleAmount()));
            if (currencyType == null) {
                currencyType = record.getCurrencyType();
            }
        }
        BigDecimal grossProfit = saleAmount.subtract(costAmount);
        return new AigcGrossProfitRespDTO()
                .setTaskId(taskId)
                .setCostAmount(costAmount)
                .setSaleAmount(saleAmount)
                .setGrossProfit(grossProfit)
                .setGrossProfitRate(calculateRate(grossProfit, saleAmount))
                .setCurrencyType(currencyType);
    }

    @Override
    public PageResult<AigcCostRecordDO> getCostRecordPage(PageParam reqVO) {
        return costRecordMapper.selectPage(reqVO);
    }

    private BigDecimal calculateRate(BigDecimal grossProfit, BigDecimal saleAmount) {
        if (saleAmount == null || saleAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return grossProfit.divide(saleAmount, 6, RoundingMode.HALF_UP);
    }

    private AigcCostRecordDO selectExisting(AigcCostRecordCreateReqDTO reqDTO) {
        if (reqDTO.getAttemptId() != null) {
            return costRecordMapper.selectByAttemptId(reqDTO.getAttemptId());
        }
        return costRecordMapper.selectByTaskId(reqDTO.getTaskId());
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

}
