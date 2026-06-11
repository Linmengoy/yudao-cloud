package cn.iocoder.yudao.module.aigc.safety.api;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.safety.dal.dataobject.AigcAuditRecordDO;
import cn.iocoder.yudao.module.aigc.safety.dto.AigcAuditPassReqDTO;
import cn.iocoder.yudao.module.aigc.safety.dto.AigcAuditRecordCreateReqDTO;
import cn.iocoder.yudao.module.aigc.safety.dto.AigcAuditRecordRespDTO;
import cn.iocoder.yudao.module.aigc.safety.dto.AigcAuditRejectReqDTO;
import cn.iocoder.yudao.module.aigc.safety.dto.AigcSafetyPromptCheckReqDTO;
import cn.iocoder.yudao.module.aigc.safety.dto.AigcSafetyPromptCheckRespDTO;
import cn.iocoder.yudao.module.aigc.safety.service.audit.AigcAuditRecordService;
import cn.iocoder.yudao.module.aigc.safety.service.check.AigcSafetyCheckService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@Validated
public class AigcSafetyApiImpl implements AigcSafetyApi {

    @Resource
    private AigcSafetyCheckService safetyCheckService;
    @Resource
    private AigcAuditRecordService auditRecordService;

    /**
     * 审核提示词是否违规
     */
    @Override
    public CommonResult<AigcSafetyPromptCheckRespDTO> checkPrompt(AigcSafetyPromptCheckReqDTO reqDTO) {
        return success(safetyCheckService.checkPrompt(reqDTO));
    }

    @Override
    public CommonResult<AigcAuditRecordRespDTO> createAuditRecord(AigcAuditRecordCreateReqDTO reqDTO) {
        AigcAuditRecordDO auditRecord = auditRecordService.createAuditRecord(reqDTO);
        return success(BeanUtils.toBean(auditRecord, AigcAuditRecordRespDTO.class));
    }

    @Override
    public CommonResult<AigcAuditRecordRespDTO> markPass(AigcAuditPassReqDTO reqDTO) {
        AigcAuditRecordDO auditRecord = auditRecordService.markPass(reqDTO);
        return success(BeanUtils.toBean(auditRecord, AigcAuditRecordRespDTO.class));
    }

    @Override
    public CommonResult<AigcAuditRecordRespDTO> markReject(AigcAuditRejectReqDTO reqDTO) {
        AigcAuditRecordDO auditRecord = auditRecordService.markReject(reqDTO);
        return success(BeanUtils.toBean(auditRecord, AigcAuditRecordRespDTO.class));
    }

}
