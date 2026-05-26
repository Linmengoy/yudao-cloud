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
        AigcCostRecordDO exists = costRecordMapper.selectByTaskId(reqDTO.getTaskId());
        if (exists != null) {
            return exists.getId();
        }
        
        AigcCostRecordDO record = BeanUtils.toBean(reqDTO, AigcCostRecordDO.class);
        record.setCostNo(billingNoGenerator.generateCostRecordNo());
        record.setGrossProfit(reqDTO.getSaleAmount().subtract(reqDTO.getCostAmount()));
        record.setGrossProfitRate(calculateRate(record.getGrossProfit(), reqDTO.getSaleAmount()));
        if (record.getCurrencyType() == null) {
            record.setCurrencyType(POINT.getCode());
        }
        record.setStatus("SUCCESS");
        
        try {
            costRecordMapper.insert(record);
        } catch (DuplicateKeyException ex) {
            AigcCostRecordDO duplicateExists = costRecordMapper.selectByTaskId(reqDTO.getTaskId());
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
        AigcCostRecordDO record = costRecordMapper.selectByTaskId(taskId);
        if (record == null) {
            throw exception(COST_RECORD_NOT_EXISTS);
        }
        return BeanUtils.toBean(record, AigcGrossProfitRespDTO.class);
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

}
