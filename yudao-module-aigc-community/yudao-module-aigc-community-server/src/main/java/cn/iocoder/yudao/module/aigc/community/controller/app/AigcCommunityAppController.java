package cn.iocoder.yudao.module.aigc.community.controller.app;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.community.controller.app.vo.AigcCommunityAuthorPostPageReqVO;
import cn.iocoder.yudao.module.aigc.community.controller.app.vo.AigcCommunityAuthorReqVO;
import cn.iocoder.yudao.module.aigc.community.controller.app.vo.AigcCommunityCommentCreateReqVO;
import cn.iocoder.yudao.module.aigc.community.controller.app.vo.AigcCommunityCommentPageReqVO;
import cn.iocoder.yudao.module.aigc.community.controller.app.vo.AigcCommunityPostLikeReqVO;
import cn.iocoder.yudao.module.aigc.community.controller.app.vo.AigcCommunityPostPageReqVO;
import cn.iocoder.yudao.module.aigc.community.controller.app.vo.AigcCommunityPostPublishReqVO;
import cn.iocoder.yudao.module.aigc.community.controller.app.vo.AigcCommunityShareReqVO;
import cn.iocoder.yudao.module.aigc.community.controller.vo.AigcCommunityAuthorRespVO;
import cn.iocoder.yudao.module.aigc.community.controller.vo.AigcCommunityCommentRespVO;
import cn.iocoder.yudao.module.aigc.community.controller.vo.AigcCommunityPostRespVO;
import cn.iocoder.yudao.module.aigc.community.controller.vo.AigcCommunityShareRespVO;
import cn.iocoder.yudao.module.aigc.community.service.AigcCommunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "App - AIGC Community")
@RestController
@RequestMapping("/aigc/community")
@Validated
public class AigcCommunityAppController {

    @Resource
    private AigcCommunityService communityService;

    @GetMapping("/post/page")
    @Operation(summary = "Get public community posts")
    public CommonResult<PageResult<AigcCommunityPostRespVO>> getPostPage(@Valid AigcCommunityPostPageReqVO reqVO) {
        return success(communityService.getPublicPostPage(reqVO, getLoginUserId()));
    }

    @GetMapping("/post/get")
    @Operation(summary = "Get public community post")
    @Parameter(name = "id", required = true, description = "Post ID or post number")
    public CommonResult<AigcCommunityPostRespVO> getPost(@RequestParam("id") String id) {
        return success(communityService.getPublicPost(id, getLoginUserId()));
    }

    @PostMapping("/post/publish")
    @Operation(summary = "Publish an asset or project to community")
    public CommonResult<Long> publishPost(@Valid @RequestBody AigcCommunityPostPublishReqVO reqVO) {
        return success(communityService.publishPost(reqVO, getLoginUserId()));
    }

    @PutMapping("/post/like")
    @Operation(summary = "Like post")
    public CommonResult<Boolean> likePost(@Valid @RequestBody AigcCommunityPostLikeReqVO reqVO) {
        communityService.likePost(reqVO.getPostId(), getLoginUserId());
        return success(true);
    }

    @DeleteMapping("/post/like")
    @Operation(summary = "Unlike post")
    public CommonResult<Boolean> unlikePost(@RequestParam("postId") Long postId) {
        communityService.unlikePost(postId, getLoginUserId());
        return success(true);
    }

    @PostMapping("/post/share")
    @Operation(summary = "Record share and get share URL")
    public CommonResult<AigcCommunityShareRespVO> sharePost(@Valid @RequestBody AigcCommunityShareReqVO reqVO,
                                                           HttpServletRequest request) {
        return success(communityService.sharePost(reqVO, getLoginUserId(), request.getRemoteAddr(), request.getHeader("User-Agent")));
    }

    @GetMapping("/comment/page")
    @Operation(summary = "Get public comments")
    public CommonResult<PageResult<AigcCommunityCommentRespVO>> getCommentPage(@Valid AigcCommunityCommentPageReqVO reqVO) {
        return success(communityService.getCommentPage(reqVO, getLoginUserId()));
    }

    @PostMapping("/comment/create")
    @Operation(summary = "Create comment")
    public CommonResult<Long> createComment(@Valid @RequestBody AigcCommunityCommentCreateReqVO reqVO) {
        return success(communityService.createComment(reqVO, getLoginUserId()));
    }

    @DeleteMapping("/comment/delete")
    @Operation(summary = "Delete my comment")
    public CommonResult<Boolean> deleteMyComment(@RequestParam("id") Long id) {
        communityService.deleteMyComment(id, getLoginUserId());
        return success(true);
    }

    @PutMapping("/author/follow")
    @Operation(summary = "Follow author")
    public CommonResult<Boolean> followAuthor(@Valid @RequestBody AigcCommunityAuthorReqVO reqVO) {
        communityService.followAuthor(reqVO.getAuthorUserId(), getLoginUserId());
        return success(true);
    }

    @DeleteMapping("/author/follow")
    @Operation(summary = "Unfollow author")
    public CommonResult<Boolean> unfollowAuthor(@RequestParam("authorUserId") Long authorUserId) {
        communityService.unfollowAuthor(authorUserId, getLoginUserId());
        return success(true);
    }

    @GetMapping("/author/follow-status")
    @Operation(summary = "Get current user follow status")
    public CommonResult<Boolean> getFollowStatus(@RequestParam("authorUserId") Long authorUserId) {
        return success(communityService.getFollowStatus(authorUserId, getLoginUserId()));
    }

    @GetMapping("/author/following-page")
    @Operation(summary = "Get my following authors")
    public CommonResult<PageResult<AigcCommunityAuthorRespVO>> getFollowingPage(@Valid PageParam pageParam) {
        return success(communityService.getFollowingPage(pageParam, getLoginUserId()));
    }

    @GetMapping("/author/follower-page")
    @Operation(summary = "Get author followers")
    public CommonResult<PageResult<AigcCommunityAuthorRespVO>> getFollowerPage(@Valid PageParam pageParam,
                                                                              @RequestParam("authorUserId") Long authorUserId) {
        return success(communityService.getFollowerPage(pageParam, authorUserId, getLoginUserId()));
    }

    @GetMapping("/author/get")
    @Operation(summary = "Get public author profile")
    public CommonResult<AigcCommunityAuthorRespVO> getAuthor(@RequestParam("authorUserId") Long authorUserId) {
        return success(communityService.getAuthor(authorUserId, getLoginUserId()));
    }

    @GetMapping("/author/post-page")
    @Operation(summary = "Get public posts of author")
    public CommonResult<PageResult<AigcCommunityPostRespVO>> getAuthorPostPage(@Valid AigcCommunityAuthorPostPageReqVO reqVO) {
        return success(communityService.getAuthorPostPage(reqVO, getLoginUserId()));
    }

}
