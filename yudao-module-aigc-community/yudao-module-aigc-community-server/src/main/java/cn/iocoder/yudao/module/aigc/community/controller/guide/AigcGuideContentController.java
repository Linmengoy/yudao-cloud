package cn.iocoder.yudao.module.aigc.community.controller.guide;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.community.controller.guide.vo.AigcGuideContentPageReqVO;
import cn.iocoder.yudao.module.aigc.community.controller.guide.vo.AigcGuideContentRespVO;
import cn.iocoder.yudao.module.aigc.community.controller.guide.vo.AigcGuideContentSaveReqVO;
import cn.iocoder.yudao.module.aigc.community.dal.dataobject.AigcGuideContentDO;
import cn.iocoder.yudao.module.aigc.community.service.guide.AigcGuideContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "AIGC Guide Content")
@RestController
@RequestMapping
@Validated
public class AigcGuideContentController {

    @Resource
    private AigcGuideContentService guideContentService;

    @PostMapping("/aigc/guide/content/create")
    @Operation(summary = "Create guide content")
    @PreAuthorize("@ss.hasPermission('aigc:guide:create')")
    public CommonResult<Long> createContent(@Valid @RequestBody AigcGuideContentSaveReqVO reqVO) {
        return success(guideContentService.createContent(reqVO));
    }

    @PutMapping("/aigc/guide/content/update")
    @Operation(summary = "Update guide content")
    @PreAuthorize("@ss.hasPermission('aigc:guide:update')")
    public CommonResult<Boolean> updateContent(@Valid @RequestBody AigcGuideContentSaveReqVO reqVO) {
        guideContentService.updateContent(reqVO);
        return success(true);
    }

    @DeleteMapping("/aigc/guide/content/delete")
    @Operation(summary = "Delete guide content")
    @Parameter(name = "id", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:guide:delete')")
    public CommonResult<Boolean> deleteContent(@RequestParam("id") Long id) {
        guideContentService.deleteContent(id);
        return success(true);
    }

    @GetMapping("/aigc/guide/content/get")
    @Operation(summary = "Get guide content")
    @Parameter(name = "id", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:guide:query')")
    public CommonResult<AigcGuideContentRespVO> getContent(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(guideContentService.getContent(id), AigcGuideContentRespVO.class));
    }

    @GetMapping("/aigc/guide/content/page")
    @Operation(summary = "Get guide content page")
    @PreAuthorize("@ss.hasPermission('aigc:guide:query')")
    public CommonResult<PageResult<AigcGuideContentRespVO>> getContentPage(@Valid AigcGuideContentPageReqVO reqVO) {
        PageResult<AigcGuideContentDO> pageResult = guideContentService.getContentPage(reqVO);
        return success(BeanUtils.toBean(pageResult, AigcGuideContentRespVO.class));
    }

    @PutMapping("/aigc/guide/content/publish")
    @Operation(summary = "Publish guide content")
    @Parameter(name = "id", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:guide:publish')")
    public CommonResult<Boolean> publishContent(@RequestParam("id") Long id) {
        guideContentService.publishContent(id, getLoginUserId());
        return success(true);
    }

    @PutMapping("/aigc/guide/content/unpublish")
    @Operation(summary = "Unpublish guide content")
    @Parameter(name = "id", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:guide:publish')")
    public CommonResult<Boolean> unpublishContent(@RequestParam("id") Long id) {
        guideContentService.unpublishContent(id);
        return success(true);
    }

    @GetMapping("/aigc/guide/content/list")
    @Operation(summary = "Get published guide content snapshot")
    public CommonResult<List<AigcGuideContentRespVO>> getPublishedContentList() {
        return success(BeanUtils.toBean(guideContentService.getPublishedContentList(), AigcGuideContentRespVO.class));
    }

    @GetMapping("/aigc/guide/content/public-get")
    @Operation(summary = "Get published guide content by slug")
    @Parameter(name = "slug", required = true)
    public CommonResult<AigcGuideContentRespVO> getPublishedContent(@RequestParam("slug") String slug) {
        return success(BeanUtils.toBean(guideContentService.getPublishedContent(slug), AigcGuideContentRespVO.class));
    }

}
