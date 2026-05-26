package cn.iocoder.yudao.module.aigc.gen.controller.admin.providerlog;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.gen.controller.admin.providerlog.vo.AigcGenerateProviderLogPageReqVO;
import cn.iocoder.yudao.module.aigc.gen.dal.dataobject.AigcGenerateProviderLogDO;
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

@Tag(name = "管理后台 - AIGC 渠道调用日志")
@RestController
@RequestMapping("/aigc/gen/provider-log")
@Validated
public class AigcGenerateProviderLogController {

    @Resource
    private AigcGenerateRecordService generateRecordService;

    @GetMapping("/page")
    @Operation(summary = "获取渠道调用日志分页")
    @PreAuthorize("@ss.hasPermission('aigc:gen:query')")
    public CommonResult<PageResult<AigcGenerateProviderLogDO>> getProviderLogPage(@Valid AigcGenerateProviderLogPageReqVO reqVO) {
        return success(generateRecordService.getProviderLogPage(reqVO));
    }
}
