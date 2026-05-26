package cn.iocoder.yudao.module.member.service.auth;

public interface MemberEmailCodeService {

    void sendEmailCode(String email, String scene, String createIp);

    void validateEmailCode(String email, String scene, String code);

    void useEmailCode(String email, String scene, String code, String usedIp);

}
