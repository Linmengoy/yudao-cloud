package cn.iocoder.yudao.module.pay.api.notify.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PayNotifyTaskRespDTO {

    private Long id;
    private Long appId;
    private Integer type;
    private Long dataId;
    private String merchantOrderId;
    private String merchantRefundId;
    private String merchantTransferId;
    private Integer status;
    private LocalDateTime nextNotifyTime;
    private LocalDateTime lastExecuteTime;
    private Integer notifyTimes;
    private Integer maxNotifyTimes;
    private String notifyUrl;

}
