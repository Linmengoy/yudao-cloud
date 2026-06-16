package cn.iocoder.yudao.module.aigc.community.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.community.controller.admin.vo.AigcCommunityAdminCommentPageReqVO;
import cn.iocoder.yudao.module.aigc.community.controller.admin.vo.AigcCommunityAdminPostPageReqVO;
import cn.iocoder.yudao.module.aigc.community.controller.admin.vo.AigcCommunityAuditReqVO;
import cn.iocoder.yudao.module.aigc.community.controller.vo.AigcCommunityCommentRespVO;
import cn.iocoder.yudao.module.aigc.community.controller.vo.AigcCommunityPostRespVO;
import cn.iocoder.yudao.module.aigc.community.service.AigcCommunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "Admin - AIGC Community")
@RestController
@RequestMapping("/aigc/community/admin")
@Validated
public class AigcCommunityAdminController {

    @Resource
    private AigcCommunityService communityService;

    @GetMapping("/post/page")
    @Operation(summary = "Get community post audit page")
    @PreAuthorize("@ss.hasPermission('aigc:community-post:query')")
    public CommonResult<PageResult<AigcCommunityPostRespVO>> getPostPage(@Valid AigcCommunityAdminPostPageReqVO reqVO) {
        return success(communityService.getAdminPostPage(reqVO));
    }

    @GetMapping("/post/get")
    @Operation(summary = "Get community post detail")
    @Parameter(name = "id", required = true)
    @PreAuthorize("@ss.hasPermission('aigc:community-post:query')")
    public CommonResult<AigcCommunityPostRespVO> getPost(@RequestParam("id") Long id) {
        return success(communityService.getAdminPost(id));
    }

    @PutMapping("/post/audit-pass")
    @Operation(summary = "Pass community post")
    @PreAuthorize("@ss.hasPermission('aigc:community-post:audit')")
    public CommonResult<Boolean> auditPassPost(@Valid @RequestBody AigcCommunityAuditReqVO reqVO) {
        communityService.auditPassPost(reqVO.getId(), getLoginUserId());
        return success(true);
    }

    @PutMapping("/post/audit-reject")
    @Operation(summary = "Reject community post")
    @PreAuthorize("@ss.hasPermission('aigc:community-post:audit')")
    public CommonResult<Boolean> auditRejectPost(@Valid @RequestBody AigcCommunityAuditReqVO reqVO) {
        communityService.auditRejectPost(reqVO.getId(), reqVO.getReason(), getLoginUserId());
        return success(true);
    }

    @PutMapping("/post/offline")
    @Operation(summary = "Offline community post")
    @PreAuthorize("@ss.hasPermission('aigc:community-post:audit')")
    public CommonResult<Boolean> offlinePost(@Valid @RequestBody AigcCommunityAuditReqVO reqVO) {
        communityService.offlinePost(reqVO.getId(), reqVO.getReason(), getLoginUserId());
        return success(true);
    }

    @PutMapping("/post/restore")
    @Operation(summary = "Restore community post")
    @PreAuthorize("@ss.hasPermission('aigc:community-post:audit')")
    public CommonResult<Boolean> restorePost(@Valid @RequestBody AigcCommunityAuditReqVO reqVO) {
        communityService.restorePost(reqVO.getId(), getLoginUserId());
        return success(true);
    }

    @GetMapping("/comment/page")
    @Operation(summary = "Get community comment audit page")
    @PreAuthorize("@ss.hasPermission('aigc:community-comment:query')")
    public CommonResult<PageResult<AigcCommunityCommentRespVO>> getCommentPage(@Valid AigcCommunityAdminCommentPageReqVO reqVO) {
        return success(communityService.getAdminCommentPage(reqVO));
    }

    @PutMapping("/comment/hide")
    @Operation(summary = "Hide community comment")
    @PreAuthorize("@ss.hasPermission('aigc:community-comment:audit')")
    public CommonResult<Boolean> hideComment(@Valid @RequestBody AigcCommunityAuditReqVO reqVO) {
        communityService.hideComment(reqVO.getId(), reqVO.getReason(), getLoginUserId());
        return success(true);
    }

    @DeleteMapping("/comment/delete")
    @Operation(summary = "Delete community comment")
    @PreAuthorize("@ss.hasPermission('aigc:community-comment:audit')")
    public CommonResult<Boolean> deleteComment(@RequestParam("id") Long id, @RequestParam("reason") String reason) {
        communityService.deleteComment(id, reason, getLoginUserId());
        return success(true);
    }

}
