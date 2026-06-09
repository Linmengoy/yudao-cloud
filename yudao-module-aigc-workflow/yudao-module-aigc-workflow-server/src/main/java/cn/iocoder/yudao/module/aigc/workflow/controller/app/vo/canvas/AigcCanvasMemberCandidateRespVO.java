package cn.iocoder.yudao.module.aigc.workflow.controller.app.vo.canvas;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户端 - AIGC 画布成员候选用户 Response VO")
@Data
public class AigcCanvasMemberCandidateRespVO {

    @Schema(description = "用户编号")
    private Long userId;
    @Schema(description = "昵称")
    private String nickname;
    @Schema(description = "头像")
    private String avatar;
    @Schema(description = "手机号")
    private String mobile;
    @Schema(description = "邮箱")
    private String email;
    @Schema(description = "用户状态")
    private Integer status;
    @Schema(description = "是否已是当前项目成员")
    private Boolean alreadyMember;

}
