package cn.iocoder.yudao.module.aigc.gen.controller.admin.record;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.gen.controller.admin.record.vo.AigcGenerateRecordPageReqVO;
import cn.iocoder.yudao.module.aigc.gen.controller.admin.record.vo.AigcGenerateRecordRespVO;
import cn.iocoder.yudao.module.aigc.gen.dal.dataobject.AigcGenerateRecordDO;
import cn.iocoder.yudao.module.aigc.gen.service.record.AigcGenerateRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AIGC 生成记录")
@RestController
@RequestMapping("/aigc/gen/record")
@Validated
public class AigcGenerateRecordController {

    @Resource
    private AigcGenerateRecordService generateRecordService;

    @GetMapping("/get")
    @Operation(summary = "获取生成记录")
    @Parameter(name = "id", description = "ID", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:gen:query')")
    public CommonResult<AigcGenerateRecordRespVO> getGenerateRecord(@RequestParam("id") Long id) {
        AigcGenerateRecordDO record = generateRecordService.validateGenerateRecordExists(id);
        return success(BeanUtils.toBean(record, AigcGenerateRecordRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获取生成记录分页")
    @PreAuthorize("@ss.hasPermission('aigc:gen:query')")
    public CommonResult<PageResult<AigcGenerateRecordRespVO>> getGenerateRecordPage(@Valid AigcGenerateRecordPageReqVO reqVO) {
        PageResult<AigcGenerateRecordDO> pageResult = generateRecordService.getGenerateRecordPage(reqVO);
        return success(BeanUtils.toBean(pageResult, AigcGenerateRecordRespVO.class));
    }

    @PostMapping("/sync")
    @Operation(summary = "同步第三方任务")
    @Parameter(name = "taskId", description = "任务编号", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:gen:update')")
    public CommonResult<Boolean> syncTask(@RequestParam("taskId") Long taskId) {
        generateRecordService.syncTask(taskId);
        return success(true);
    }
}
