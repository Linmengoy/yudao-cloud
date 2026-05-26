package cn.iocoder.yudao.module.member.controller.app.auth.vo;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.member.enums.auth.MemberEmailCodeSceneEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "用户 APP - 发送邮箱验证码 Request VO")
@Data
public class AppAuthEmailCodeSendReqVO {

    @Schema(description = "邮箱", requiredMode = Schema.RequiredMode.REQUIRED, example = "user@example.com")
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "发送场景", requiredMode = Schema.RequiredMode.REQUIRED, example = "REGISTER")
    @NotBlank(message = "发送场景不能为空")
    @InEnum(MemberEmailCodeSceneEnum.class)
    private String scene;

}
