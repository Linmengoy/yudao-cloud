package cn.iocoder.yudao.module.member.enums.auth;

import cn.hutool.core.util.ArrayUtil;
import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum MemberEmailCodeSceneEnum implements ArrayValuable<String> {

    REGISTER("REGISTER", "member_email_register_code", "邮箱注册"),
    LOGIN("LOGIN", "member_email_login_code", "邮箱验证码登录"),
    RESET_PASSWORD("RESET_PASSWORD", "member_email_reset_password_code", "邮箱找回密码"),
    BIND_EMAIL("BIND_EMAIL", "member_email_bind_code", "绑定邮箱"),
    CHANGE_EMAIL("CHANGE_EMAIL", "member_email_change_code", "换绑邮箱");

    public static final String[] ARRAYS = Arrays.stream(values()).map(MemberEmailCodeSceneEnum::getScene).toArray(String[]::new);

    private final String scene;

    private final String templateCode;

    private final String description;

    @Override
    public String[] array() {
        return ARRAYS;
    }

    public static MemberEmailCodeSceneEnum getByScene(String scene) {
        return ArrayUtil.firstMatch(sceneEnum -> sceneEnum.getScene().equals(scene), values());
    }

}
