package cn.iocoder.yudao.module.aigc.gen.controller.admin.callback;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.gen.controller.admin.callback.vo.AigcGenerateCallbackPageReqVO;
import cn.iocoder.yudao.module.aigc.gen.dal.dataobject.AigcGenerateCallbackDO;
import cn.iocoder.yudao.module.aigc.gen.service.record.AigcGenerateRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AIGC 生成回调")
@RestController
@RequestMapping("/aigc/gen/callback")
@Validated
public class AigcGenerateCallbackController {

    @Resource
    private AigcGenerateRecordService generateRecordService;

    @GetMapping("/page")
    @Operation(summary = "获取生成回调分页")
    @PreAuthorize("@ss.hasPermission('aigc:gen:query')")
    public CommonResult<PageResult<AigcGenerateCallbackDO>> getCallbackPage(@Valid AigcGenerateCallbackPageReqVO reqVO) {
        return success(generateRecordService.getCallbackPage(reqVO));
    }
}
