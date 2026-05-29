package cn.iocoder.yudao.module.member.dal.redis;

public interface RedisKeyConstants {

    String EMAIL_CODE_SEND_INTERVAL = "member:email-code:send-interval:";

    String EMAIL_CODE_DAILY_COUNT = "member:email-code:daily-count:";

    String EMAIL_CODE_IP_HOURLY_COUNT = "member:email-code:ip-hourly-count:";

}
