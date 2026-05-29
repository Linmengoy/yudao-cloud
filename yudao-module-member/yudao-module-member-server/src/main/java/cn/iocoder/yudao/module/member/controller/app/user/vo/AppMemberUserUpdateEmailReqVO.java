package cn.iocoder.yudao.module.member.controller.app.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Schema(description = "用户 APP - 绑定或换绑邮箱 Request VO")
@Data
public class AppMemberUserUpdateEmailReqVO {

    @Schema(description = "邮箱", requiredMode = Schema.RequiredMode.REQUIRED, example = "user@example.com")
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "邮箱验证码", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotBlank(message = "邮箱验证码不能为空")
    @Length(min = 4, max = 6, message = "邮箱验证码长度为 4-6 位")
    @Pattern(regexp = "^[0-9]+$", message = "邮箱验证码必须都是数字")
    private String code;

}
