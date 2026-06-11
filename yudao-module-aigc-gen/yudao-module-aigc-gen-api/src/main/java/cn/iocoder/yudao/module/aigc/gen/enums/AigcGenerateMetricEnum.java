package cn.iocoder.yudao.module.aigc.gen.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AigcGenerateMetricEnum {

    GEN_SUBMIT_TOTAL("gen_submit_total"),
    SUBMIT_FAILED_TOTAL("aigc_gen_submit_failed_total"),
    SUBMIT_SUCCESS_TOTAL("aigc_gen_submit_success_total"),
    CALLBACK_TOTAL("aigc_gen_callback_total"),
    CALLBACK_INVALID_TOTAL("aigc_gen_callback_invalid_total"),
    TIMEOUT_TOTAL("aigc_gen_timeout_total"),
    PROVIDER_DURATION_MS("aigc_gen_provider_duration_ms"),
    SUCCESS_TOTAL("aigc_gen_success_total"),
    FAILED_TOTAL("aigc_gen_failed_total");

    private final String name;

}
