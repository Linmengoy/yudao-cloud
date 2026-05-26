package cn.iocoder.yudao.module.aigc.asset.controller.app;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.asset.controller.admin.vo.AigcAssetPageReqVO;
import cn.iocoder.yudao.module.aigc.asset.dal.dataobject.AigcAssetDO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetDownloadReqDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetRespDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetUpdateReqDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetVisibilityUpdateReqDTO;
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

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "用户端 - AIGC 资产")
@RestController
@RequestMapping("/aigc/asset")
@Validated
public class AigcAssetAppController {

    @Resource
    private AigcAssetService assetService;

    @GetMapping("/my-get")
    @Operation(summary = "获取我的资产详情")
    @Parameter(name = "id", description = "资产编号", required = true)
    public CommonResult<AigcAssetRespDTO> getMyAsset(@RequestParam("id") Long id) {
        AigcAssetDO asset = assetService.getUserAsset(id, getLoginUserId());
        return success(BeanUtils.toBean(asset, AigcAssetRespDTO.class));
    }

    @GetMapping("/my-page")
    @Operation(summary = "获取我的资产分页")
    public CommonResult<PageResult<AigcAssetRespDTO>> getMyAssetPage(@Valid AigcAssetPageReqVO reqVO) {
        PageResult<AigcAssetDO> pageResult = assetService.getUserAssetPage(reqVO, getLoginUserId());
        return success(BeanUtils.toBean(pageResult, AigcAssetRespDTO.class));
    }

    @PostMapping("/upload")
    @Operation(summary = "上传资产")
    public CommonResult<Long> uploadAsset(@RequestParam("file") MultipartFile file,
                                          @RequestParam("assetType") String assetType,
                                          @RequestParam(value = "title", required = false) String title) throws IOException {
        return success(assetService.uploadAsset(getLoginUserId(), assetType, title, file.getOriginalFilename(),
                file.getContentType(), file.getBytes()));
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
    public CommonResult<String> downloadMyAsset(@Valid @RequestBody AigcAssetDownloadReqDTO reqDTO) {
        AigcAssetDO asset = assetService.getAccessibleAsset(reqDTO.getAssetId(), getLoginUserId());
        reqDTO.setUserId(getLoginUserId());
        assetService.increaseDownloadCount(reqDTO);
        return success(asset.getFileUrl());
    }

    @PostMapping("/use")
    @Operation(summary = "标记我的资产被使用")
    @Parameter(name = "id", description = "资产编号", required = true)
    public CommonResult<Boolean> useMyAsset(@RequestParam("id") Long id) {
        assetService.increaseUseCount(id, getLoginUserId());
        return success(true);
    }

}
