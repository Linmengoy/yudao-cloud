package cn.iocoder.yudao.module.aigc.model.controller.app;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.model.controller.app.vo.AigcReleaseNoteRespVO;
import cn.iocoder.yudao.module.aigc.model.service.release.AigcReleaseNoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "App - AIGC 版本更新记录")
@RestController
@RequestMapping("/aigc/release-note")
@Validated
public class AigcReleaseNoteAppController {

    @Resource
    private AigcReleaseNoteService releaseNoteService;

    @GetMapping("/published")
    @Operation(summary = "获取已发布版本更新记录")
    public CommonResult<List<AigcReleaseNoteRespVO>> getPublishedReleaseNotes(
            @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit) {
        return success(BeanUtils.toBean(releaseNoteService.getPublishedReleaseNotes(limit), AigcReleaseNoteRespVO.class));
    }

}
