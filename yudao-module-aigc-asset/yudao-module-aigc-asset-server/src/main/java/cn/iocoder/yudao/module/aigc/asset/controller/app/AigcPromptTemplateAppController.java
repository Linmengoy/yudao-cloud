package cn.iocoder.yudao.module.aigc.asset.controller.app;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.asset.controller.app.vo.template.AigcPromptTemplatePageReqVO;
import cn.iocoder.yudao.module.aigc.asset.controller.app.vo.template.AigcPromptTemplateRespVO;
import cn.iocoder.yudao.module.aigc.asset.service.template.AigcPromptTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "用户端 - AIGC 提示词模板")
@RestController
@RequestMapping("/aigc/asset/prompt-template")
@Validated
public class AigcPromptTemplateAppController {

    @Resource
    private AigcPromptTemplateService promptTemplateService;

    @GetMapping("/page")
    @Operation(summary = "获取提示词模板分页")
    public CommonResult<PageResult<AigcPromptTemplateRespVO>> getTemplatePage(
            @Valid AigcPromptTemplatePageReqVO reqVO) {
        return success(promptTemplateService.getTemplatePage(reqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获取提示词模板详情")
    @Parameter(name = "id", description = "模板编号", required = true)
    public CommonResult<AigcPromptTemplateRespVO> getTemplate(@RequestParam("id") Long id) {
        promptTemplateService.increaseViewCount(id);
        return success(promptTemplateService.getTemplate(id));
    }

    @GetMapping("/categories")
    @Operation(summary = "获取提示词模板分类")
    public CommonResult<List<String>> getTemplateCategories() {
        return success(promptTemplateService.getCategoryList());
    }

    @PostMapping("/copy")
    @Operation(summary = "记录提示词模板复制")
    @Parameter(name = "id", description = "模板编号", required = true)
    public CommonResult<Boolean> copyTemplate(@RequestParam("id") Long id) {
        promptTemplateService.increaseCopyCount(id);
        return success(true);
    }

    @PostMapping("/use")
    @Operation(summary = "记录提示词模板复用")
    @Parameter(name = "id", description = "模板编号", required = true)
    public CommonResult<Boolean> useTemplate(@RequestParam("id") Long id) {
        promptTemplateService.increaseUseCount(id);
        return success(true);
    }

}
