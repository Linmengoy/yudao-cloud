package cn.iocoder.yudao.module.aigc.safety.controller.admin.audit;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.safety.controller.admin.audit.vo.AigcAuditRecordPageReqVO;
import cn.iocoder.yudao.module.aigc.safety.controller.admin.audit.vo.AigcAuditRecordRespVO;
import cn.iocoder.yudao.module.aigc.safety.dal.dataobject.AigcAuditRecordDO;
import cn.iocoder.yudao.module.aigc.safety.dto.AigcAuditPassReqDTO;
import cn.iocoder.yudao.module.aigc.safety.dto.AigcAuditRejectReqDTO;
import cn.iocoder.yudao.module.aigc.safety.service.audit.AigcAuditRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - AIGC 审核记录")
@RestController
@RequestMapping("/aigc/safety/audit-record")
@Validated
public class AigcAuditRecordController {

    @Resource
    private AigcAuditRecordService auditRecordService;

    @GetMapping("/get")
    @Operation(summary = "获取审核记录")
    @Parameter(name = "id", description = "审核记录编号", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:safety-audit-record:query')")
    public CommonResult<AigcAuditRecordRespVO> getAuditRecord(@RequestParam("id") Long id) {
        AigcAuditRecordDO auditRecord = auditRecordService.validateAuditRecordExists(id);
        return success(BeanUtils.toBean(auditRecord, AigcAuditRecordRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获取审核记录分页")
    @PreAuthorize("@ss.hasPermission('aigc:safety-audit-record:query')")
    public CommonResult<PageResult<AigcAuditRecordRespVO>> getAuditRecordPage(@Valid AigcAuditRecordPageReqVO reqVO) {
        PageResult<AigcAuditRecordDO> pageResult = auditRecordService.getAuditRecordPage(reqVO);
        return success(BeanUtils.toBean(pageResult, AigcAuditRecordRespVO.class));
    }

    @PutMapping("/pass")
    @Operation(summary = "人工审核通过")
    @PreAuthorize("@ss.hasPermission('aigc:safety-audit-record:audit')")
    public CommonResult<AigcAuditRecordRespVO> markPass(@Valid @RequestBody AigcAuditPassReqDTO reqDTO) {
        reqDTO.setAuditorUserId(getLoginUserId());
        AigcAuditRecordDO auditRecord = auditRecordService.markPass(reqDTO);
        return success(BeanUtils.toBean(auditRecord, AigcAuditRecordRespVO.class));
    }

    @PutMapping("/reject")
    @Operation(summary = "人工审核拒绝")
    @PreAuthorize("@ss.hasPermission('aigc:safety-audit-record:audit')")
    public CommonResult<AigcAuditRecordRespVO> markReject(@Valid @RequestBody AigcAuditRejectReqDTO reqDTO) {
        reqDTO.setAuditorUserId(getLoginUserId());
        AigcAuditRecordDO auditRecord = auditRecordService.markReject(reqDTO);
        return success(BeanUtils.toBean(auditRecord, AigcAuditRecordRespVO.class));
    }

}
