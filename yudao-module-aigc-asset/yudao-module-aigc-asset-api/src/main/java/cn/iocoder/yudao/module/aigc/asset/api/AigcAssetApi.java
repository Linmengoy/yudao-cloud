package cn.iocoder.yudao.module.aigc.asset.api;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetAuditUpdateReqDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetCreateReqDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetCreateRespDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetDownloadReqDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetPageReqDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetRespDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetVisibilityUpdateReqDTO;
import cn.iocoder.yudao.module.aigc.asset.enums.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = ApiConstants.NAME)
@Tag(name = "RPC 服务 - AIGC 资产")
public interface AigcAssetApi {

    String PREFIX = ApiConstants.PREFIX;

    @PostMapping(PREFIX + "/create-asset")
    @Operation(summary = "创建资产")
    CommonResult<AigcAssetCreateRespDTO> createAsset(@RequestBody AigcAssetCreateReqDTO reqDTO);

    @PostMapping(PREFIX + "/create-image-asset")
    @Operation(summary = "创建图片资产")
    CommonResult<AigcAssetCreateRespDTO> createImageAsset(@RequestBody AigcAssetCreateReqDTO reqDTO);

    @PostMapping(PREFIX + "/create-video-asset")
    @Operation(summary = "创建视频资产")
    CommonResult<AigcAssetCreateRespDTO> createVideoAsset(@RequestBody AigcAssetCreateReqDTO reqDTO);

    @PostMapping(PREFIX + "/create-audio-asset")
    @Operation(summary = "创建音频资产")
    CommonResult<AigcAssetCreateRespDTO> createAudioAsset(@RequestBody AigcAssetCreateReqDTO reqDTO);

    @PostMapping(PREFIX + "/create-document-asset")
    @Operation(summary = "创建文档资产")
    CommonResult<AigcAssetCreateRespDTO> createDocumentAsset(@RequestBody AigcAssetCreateReqDTO reqDTO);

    @GetMapping(PREFIX + "/get-asset")
    @Operation(summary = "获取资产")
    @Parameter(name = "assetId", description = "资产编号", required = true, example = "1024")
    CommonResult<AigcAssetRespDTO> getAsset(@RequestParam("assetId") Long assetId);

    @PostMapping(PREFIX + "/get-assets")
    @Operation(summary = "批量获取资产")
    CommonResult<List<AigcAssetRespDTO>> getAssets(@RequestBody List<Long> assetIds);

    @GetMapping(PREFIX + "/get-asset-by-task-id")
    @Operation(summary = "根据任务编号获取资产")
    @Parameter(name = "taskId", description = "任务编号", required = true, example = "1024")
    CommonResult<AigcAssetRespDTO> getAssetByTaskId(@RequestParam("taskId") Long taskId);

    @PostMapping(PREFIX + "/get-user-assets")
    @Operation(summary = "获取用户资产分页")
    CommonResult<PageResult<AigcAssetRespDTO>> getUserAssets(@RequestBody AigcAssetPageReqDTO reqDTO);

    @PutMapping(PREFIX + "/increase-download-count")
    @Operation(summary = "增加下载次数")
    CommonResult<Boolean> increaseDownloadCount(@RequestBody AigcAssetDownloadReqDTO reqDTO);

    @PutMapping(PREFIX + "/update-audit-status")
    @Operation(summary = "更新审核状态")
    CommonResult<Boolean> updateAuditStatus(@RequestBody AigcAssetAuditUpdateReqDTO reqDTO);

    @PutMapping(PREFIX + "/update-visibility")
    @Operation(summary = "更新可见性")
    CommonResult<Boolean> updateVisibility(@RequestBody AigcAssetVisibilityUpdateReqDTO reqDTO);

}
