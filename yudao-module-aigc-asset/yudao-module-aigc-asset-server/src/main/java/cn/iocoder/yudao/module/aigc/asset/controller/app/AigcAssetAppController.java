package cn.iocoder.yudao.module.aigc.asset.controller.app;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.asset.controller.app.vo.AigcAssetVideoFrameCaptureReqVO;
import cn.iocoder.yudao.module.aigc.asset.controller.admin.vo.AigcAssetPageReqVO;
import cn.iocoder.yudao.module.aigc.asset.dal.dataobject.AigcAssetDO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetAccessUrlReqDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetAccessUrlRespDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetCategoryCountRespDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetDownloadReqDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetRespDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetUpdateReqDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetVisibilityUpdateReqDTO;
import cn.iocoder.yudao.module.aigc.asset.enums.AigcAssetAccessTypeEnum;
import cn.iocoder.yudao.module.aigc.asset.enums.AigcAssetFileRoleEnum;
import cn.iocoder.yudao.module.aigc.asset.service.asset.AigcAssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "用户端 - AIGC 资产")
@RestController
@RequestMapping("/aigc/asset")
@Validated
public class AigcAssetAppController {

    @Resource
    private AigcAssetService assetService;

    // get
    @GetMapping("/my-get")
    @Operation(summary = "获取我的资产详情")
    @Parameter(name = "id", description = "资产编号", required = true)
    public CommonResult<AigcAssetRespDTO> getMyAsset(@RequestParam("id") Long id) {
        return success(assetService.getAssetResp(id, getLoginUserId()));
    }

    @GetMapping("/my-page")
    @Operation(summary = "获取我的资产分页")
    public CommonResult<PageResult<AigcAssetRespDTO>> getMyAssetPage(@Valid AigcAssetPageReqVO reqVO) {
        PageResult<AigcAssetDO> pageResult = assetService.getUserAssetPage(reqVO, getLoginUserId());
        PageResult<AigcAssetRespDTO> respPage = new PageResult<>();
        respPage.setTotal(pageResult.getTotal());
        respPage.setList(assetService.buildAssetRespList(pageResult.getList(), getLoginUserId()));
        return success(respPage);
    }

    @GetMapping("/my-list")
    @Operation(summary = "获取我的资产列表")
    public CommonResult<List<AigcAssetRespDTO>> getMyAssetList(@Valid AigcAssetPageReqVO reqVO) {
        List<AigcAssetDO> list = assetService.getUserAssetList(reqVO, getLoginUserId());
        return success(assetService.buildAssetRespList(list, getLoginUserId()));
    }

    @GetMapping("/my-category-counts")
    @Operation(summary = "鑾峰彇鎴戠殑璧勪骇鍒嗙被鏁伴噺")
    public CommonResult<AigcAssetCategoryCountRespDTO> getMyAssetCategoryCounts(@Valid AigcAssetPageReqVO reqVO) {
        return success(assetService.getUserAssetCategoryCounts(reqVO, getLoginUserId()));
    }

    @PostMapping("/upload")
    @Operation(summary = "上传资产")
    public CommonResult<Long> uploadAsset(@RequestParam("file") MultipartFile file,
                                          @RequestParam("assetType") String assetType,
                                          @RequestParam(value = "title", required = false) String title) throws IOException {
        return success(assetService.uploadAsset(getLoginUserId(), assetType, title, file.getOriginalFilename(),
                file.getContentType(), file.getBytes()));
    }

    @PostMapping("/capture-video-frame")
    @Operation(summary = "截取视频帧并创建图片资产")
    public CommonResult<Long> captureVideoFrame(@Valid @RequestBody AigcAssetVideoFrameCaptureReqVO reqVO) {
        return success(assetService.captureVideoFrame(getLoginUserId(), reqVO.getAssetId(),
                reqVO.getCapturedAt(), reqVO.getTimeSec(), reqVO.getTitle()));
    }

    @PutMapping("/update")
    @Operation(summary = "更新我的资产")
    public CommonResult<Boolean> updateMyAsset(@Valid @RequestBody AigcAssetUpdateReqDTO reqDTO) {
        assetService.getUserAsset(reqDTO.getId(), getLoginUserId());
        assetService.updateAsset(reqDTO);
        return success(true);
    }

    @PutMapping("/visibility")
    @Operation(summary = "更新我的资产可见性")
    public CommonResult<Boolean> updateMyAssetVisibility(@Valid @RequestBody AigcAssetVisibilityUpdateReqDTO reqDTO) {
        assetService.getUserAsset(reqDTO.getId(), getLoginUserId());
        assetService.updateVisibility(reqDTO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除我的资产")
    @Parameter(name = "id", description = "资产编号", required = true)
    public CommonResult<Boolean> deleteMyAsset(@RequestParam("id") Long id) {
        assetService.getUserAsset(id, getLoginUserId());
        assetService.deleteAsset(id);
        return success(true);
    }

    @PostMapping("/download")
    @Operation(summary = "下载我的资产")
    public CommonResult<AigcAssetAccessUrlRespDTO> downloadMyAsset(@Valid @RequestBody AigcAssetDownloadReqDTO reqDTO) {
        return success(assetService.getAccessUrl(new AigcAssetAccessUrlReqDTO()
                .setAssetId(reqDTO.getAssetId())
                .setFileRole(AigcAssetFileRoleEnum.ORIGINAL.getCode())
                .setAccessType(AigcAssetAccessTypeEnum.DOWNLOAD.getCode()), getLoginUserId()));
    }

    @PostMapping("/access-url")
    @Operation(summary = "获取资产访问 URL")
    public CommonResult<AigcAssetAccessUrlRespDTO> getAccessUrl(@Valid @RequestBody AigcAssetAccessUrlReqDTO reqDTO) {
        return success(assetService.getAccessUrl(reqDTO, getLoginUserId()));
    }

    @PostMapping("/access-urls")
    @Operation(summary = "批量获取资产访问 URL")
    public CommonResult<List<AigcAssetAccessUrlRespDTO>> getAccessUrls(@Valid @RequestBody List<AigcAssetAccessUrlReqDTO> reqDTOs) {
        return success(assetService.getAccessUrls(reqDTOs, getLoginUserId()));
    }

    @PostMapping("/use")
    @Operation(summary = "标记我的资产被使用")
    @Parameter(name = "id", description = "资产编号", required = true)
    public CommonResult<Boolean> useMyAsset(@RequestParam("id") Long id) {
        assetService.increaseUseCount(id, getLoginUserId());
        return success(true);
    }

}
