package cn.iocoder.yudao.module.pay.api.notify.dto;

import lombok.Data;

import java.util.List;

@Data
public class PayNotifyDiagnosticRespDTO {

    private PayNotifyTaskRespDTO task;
    private List<PayNotifyLogRespDTO> logs;

}
