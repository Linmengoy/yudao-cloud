package cn.iocoder.yudao.module.aigc.safety.api;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.aigc.safety.dto.AigcAuditPassReqDTO;
import cn.iocoder.yudao.module.aigc.safety.dto.AigcAuditRecordCreateReqDTO;
import cn.iocoder.yudao.module.aigc.safety.dto.AigcAuditRecordRespDTO;
import cn.iocoder.yudao.module.aigc.safety.dto.AigcAuditRejectReqDTO;
import cn.iocoder.yudao.module.aigc.safety.dto.AigcSafetyPromptCheckReqDTO;
import cn.iocoder.yudao.module.aigc.safety.dto.AigcSafetyPromptCheckRespDTO;
import cn.iocoder.yudao.module.aigc.safety.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = ApiConstants.NAME)
@Tag(name = "RPC 服务 - AIGC 审核风控")
public interface AigcSafetyApi {

    String PREFIX = ApiConstants.PREFIX;

    @PostMapping(PREFIX + "/check-prompt")
    @Operation(summary = "检查提示词")
    CommonResult<AigcSafetyPromptCheckRespDTO> checkPrompt(@Valid @RequestBody AigcSafetyPromptCheckReqDTO reqDTO);

    @PostMapping(PREFIX + "/create-audit-record")
    @Operation(summary = "创建审核记录")
    CommonResult<AigcAuditRecordRespDTO> createAuditRecord(@Valid @RequestBody AigcAuditRecordCreateReqDTO reqDTO);

    @PutMapping(PREFIX + "/mark-pass")
    @Operation(summary = "标记审核通过")
    CommonResult<AigcAuditRecordRespDTO> markPass(@Valid @RequestBody AigcAuditPassReqDTO reqDTO);

    @PutMapping(PREFIX + "/mark-reject")
    @Operation(summary = "标记审核拒绝")
    CommonResult<AigcAuditRecordRespDTO> markReject(@Valid @RequestBody AigcAuditRejectReqDTO reqDTO);

}
