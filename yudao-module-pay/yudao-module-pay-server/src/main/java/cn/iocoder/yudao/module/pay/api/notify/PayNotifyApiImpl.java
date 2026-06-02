package cn.iocoder.yudao.module.pay.api.notify;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.pay.api.notify.dto.PayNotifyDiagnosticRespDTO;
import cn.iocoder.yudao.module.pay.service.notify.PayNotifyService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@Validated
public class PayNotifyApiImpl implements PayNotifyApi {

    @Resource
    private PayNotifyService payNotifyService;

    @Override
    public CommonResult<PayNotifyDiagnosticRespDTO> getNotifyDiagnostic(Integer type, Long dataId) {
        return success(payNotifyService.getNotifyDiagnostic(type, dataId));
    }

}
