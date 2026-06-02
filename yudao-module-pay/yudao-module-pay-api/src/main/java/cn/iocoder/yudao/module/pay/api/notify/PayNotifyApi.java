package cn.iocoder.yudao.module.pay.api.notify;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.pay.api.notify.dto.PayNotifyDiagnosticRespDTO;
import cn.iocoder.yudao.module.pay.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = ApiConstants.NAME)
@Tag(name = "RPC 服务 - 支付通知")
public interface PayNotifyApi {

    String PREFIX = ApiConstants.PREFIX + "/notify";

    @GetMapping(PREFIX + "/get-diagnostic")
    @Operation(summary = "获得支付通知排障信息")
    @Parameter(name = "type", description = "通知类型", required = true)
    CommonResult<PayNotifyDiagnosticRespDTO> getNotifyDiagnostic(@RequestParam("type") Integer type,
                                                                 @RequestParam("dataId") Long dataId);

}
