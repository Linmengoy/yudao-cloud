package cn.iocoder.yudao.module.aigc.safety.service.audit;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.safety.controller.admin.audit.vo.AigcAuditRecordPageReqVO;
import cn.iocoder.yudao.module.aigc.safety.dal.dataobject.AigcAuditRecordDO;
import cn.iocoder.yudao.module.aigc.safety.dto.AigcAuditPassReqDTO;
import cn.iocoder.yudao.module.aigc.safety.dto.AigcAuditRecordCreateReqDTO;
import cn.iocoder.yudao.module.aigc.safety.dto.AigcAuditRejectReqDTO;

public interface AigcAuditRecordService {

    AigcAuditRecordDO createAuditRecord(AigcAuditRecordCreateReqDTO reqDTO);

    AigcAuditRecordDO markPass(AigcAuditPassReqDTO reqDTO);

    AigcAuditRecordDO markReject(AigcAuditRejectReqDTO reqDTO);

    AigcAuditRecordDO getAuditRecord(Long id);

    AigcAuditRecordDO validateAuditRecordExists(Long id);

    PageResult<AigcAuditRecordDO> getAuditRecordPage(AigcAuditRecordPageReqVO reqVO);

}
