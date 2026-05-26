package cn.iocoder.yudao.module.aigc.safety.service.audit;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.asset.api.AigcAssetApi;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetAuditUpdateReqDTO;
import cn.iocoder.yudao.module.aigc.asset.enums.AigcAssetAuditStatusEnum;
import cn.iocoder.yudao.module.aigc.safety.controller.admin.audit.vo.AigcAuditRecordPageReqVO;
import cn.iocoder.yudao.module.aigc.safety.dal.dataobject.AigcAuditRecordDO;
import cn.iocoder.yudao.module.aigc.safety.dal.mysql.AigcAuditRecordMapper;
import cn.iocoder.yudao.module.aigc.safety.dto.AigcAuditPassReqDTO;
import cn.iocoder.yudao.module.aigc.safety.dto.AigcAuditRecordCreateReqDTO;
import cn.iocoder.yudao.module.aigc.safety.dto.AigcAuditRejectReqDTO;
import cn.iocoder.yudao.module.aigc.safety.enums.AigcAuditObjectTypeEnum;
import cn.iocoder.yudao.module.aigc.safety.enums.AigcAuditResultEnum;
import cn.iocoder.yudao.module.aigc.safety.enums.AigcAuditStatusEnum;
import cn.iocoder.yudao.module.aigc.safety.enums.AigcSafetySceneEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.aigc.safety.enums.ErrorCodeConstants.*;

@Service
@Validated
@Slf4j
public class AigcAuditRecordServiceImpl implements AigcAuditRecordService {

    @Resource
    private AigcAuditRecordMapper auditRecordMapper;
    @Resource
    private AigcAssetApi assetApi;

    @Override
    public AigcAuditRecordDO createAuditRecord(AigcAuditRecordCreateReqDTO reqDTO) {
        normalizeAndValidate(reqDTO);
        AigcAuditRecordDO auditRecord = BeanUtils.toBean(reqDTO, AigcAuditRecordDO.class)
                .setAuditStatus(reqDTO.getAuditStatus())
                .setRiskLevel(reqDTO.getRiskLevel() == null ? 0 : reqDTO.getRiskLevel());
        auditRecordMapper.insert(auditRecord);
        return auditRecord;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AigcAuditRecordDO markPass(AigcAuditPassReqDTO reqDTO) {
        AigcAuditRecordDO auditRecord = validatePendingAuditRecord(reqDTO.getAuditId());
        AigcAuditRecordDO updateObj = new AigcAuditRecordDO()
                .setId(reqDTO.getAuditId())
                .setAuditStatus(AigcAuditStatusEnum.PASS.getCode())
                .setAuditResult(AigcAuditResultEnum.MANUAL_PASS.getCode())
                .setAuditorUserId(reqDTO.getAuditorUserId())
                .setAuditTime(LocalDateTime.now());
        if (auditRecordMapper.updateStatusIfPending(updateObj, AigcAuditStatusEnum.PENDING.getCode()) != 1) {
            throw exception(AUDIT_RECORD_STATUS_INVALID);
        }
        registerAssetAuditSync(auditRecord, AigcAssetAuditStatusEnum.PASS.getCode(), reqDTO.getRemark());
        return validateAuditRecordExists(reqDTO.getAuditId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AigcAuditRecordDO markReject(AigcAuditRejectReqDTO reqDTO) {
        if (StrUtil.isBlank(reqDTO.getReason())) {
            throw exception(AUDIT_REJECT_REASON_EMPTY);
        }
        AigcAuditRecordDO auditRecord = validatePendingAuditRecord(reqDTO.getAuditId());
        AigcAuditRecordDO updateObj = new AigcAuditRecordDO()
                .setId(reqDTO.getAuditId())
                .setAuditStatus(AigcAuditStatusEnum.REJECT.getCode())
                .setAuditResult(AigcAuditResultEnum.MANUAL_REJECT.getCode())
                .setRejectReason(reqDTO.getReason())
                .setAuditorUserId(reqDTO.getAuditorUserId())
                .setAuditTime(LocalDateTime.now());
        if (auditRecordMapper.updateStatusIfPending(updateObj, AigcAuditStatusEnum.PENDING.getCode()) != 1) {
            throw exception(AUDIT_RECORD_STATUS_INVALID);
        }
        registerAssetAuditSync(auditRecord, AigcAssetAuditStatusEnum.REJECT.getCode(), reqDTO.getReason());
        return validateAuditRecordExists(reqDTO.getAuditId());
    }

    @Override
    public AigcAuditRecordDO getAuditRecord(Long id) {
        return auditRecordMapper.selectById(id);
    }

    @Override
    public AigcAuditRecordDO validateAuditRecordExists(Long id) {
        AigcAuditRecordDO auditRecord = auditRecordMapper.selectById(id);
        if (auditRecord == null) {
            throw exception(AUDIT_RECORD_NOT_EXISTS);
        }
        return auditRecord;
    }

    @Override
    public PageResult<AigcAuditRecordDO> getAuditRecordPage(AigcAuditRecordPageReqVO reqVO) {
        return auditRecordMapper.selectPage(reqVO);
    }

    private AigcAuditRecordDO validatePendingAuditRecord(Long id) {
        AigcAuditRecordDO auditRecord = validateAuditRecordExists(id);
        if (!AigcAuditStatusEnum.PENDING.getCode().equals(auditRecord.getAuditStatus())) {
            throw exception(AUDIT_RECORD_STATUS_INVALID);
        }
        return auditRecord;
    }

    private void normalizeAndValidate(AigcAuditRecordCreateReqDTO reqDTO) {
        reqDTO.setAuditStatus(StrUtil.blankToDefault(reqDTO.getAuditStatus(), AigcAuditStatusEnum.PENDING.getCode()));
        validateObjectType(reqDTO.getObjectType());
        validateScene(reqDTO.getScene());
        validateCreateStatus(reqDTO.getAuditStatus());
        validateAuditResult(reqDTO.getAuditResult());
    }

    private void validateObjectType(String objectType) {
        for (AigcAuditObjectTypeEnum typeEnum : AigcAuditObjectTypeEnum.values()) {
            if (typeEnum.getCode().equals(objectType)) {
                return;
            }
        }
        throw exception(AUDIT_OBJECT_TYPE_INVALID);
    }

    private void validateScene(String scene) {
        for (AigcSafetySceneEnum sceneEnum : AigcSafetySceneEnum.values()) {
            if (sceneEnum.getCode().equals(scene)) {
                return;
            }
        }
        throw exception(AUDIT_SCENE_INVALID);
    }

    private void validateCreateStatus(String auditStatus) {
        if (!AigcAuditStatusEnum.PENDING.getCode().equals(auditStatus)) {
            throw exception(AUDIT_RECORD_STATUS_INVALID);
        }
    }

    private void validateAuditResult(String auditResult) {
        if (StrUtil.isBlank(auditResult)) {
            return;
        }
        if (AigcAuditResultEnum.AUTO_PASS.getCode().equals(auditResult)
                || AigcAuditResultEnum.AUTO_REJECT.getCode().equals(auditResult)) {
            return;
        }
        throw exception(AUDIT_RESULT_INVALID);
    }

    private void registerAssetAuditSync(AigcAuditRecordDO auditRecord, String auditStatus, String auditReason) {
        if (!AigcAuditObjectTypeEnum.ASSET.getCode().equals(auditRecord.getObjectType())) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

            @Override
            public void afterCommit() {
                syncAssetAuditStatus(auditRecord, auditStatus, auditReason);
            }

        });
    }

    private void syncAssetAuditStatus(AigcAuditRecordDO auditRecord, String auditStatus, String auditReason) {
        try {
            assetApi.updateAuditStatus(new AigcAssetAuditUpdateReqDTO()
                    .setId(auditRecord.getObjectId())
                    .setAuditStatus(auditStatus)
                    .setAuditReason(auditReason));
        } catch (Exception ex) {
            log.error("[syncAssetAuditStatus][auditRecordId({}) assetId({}) auditStatus({}) 同步资产审核状态失败]",
                    auditRecord.getId(), auditRecord.getObjectId(), auditStatus, ex);
        }
    }

}
