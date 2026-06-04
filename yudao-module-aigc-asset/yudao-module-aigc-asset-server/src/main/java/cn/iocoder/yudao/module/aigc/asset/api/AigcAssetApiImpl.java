package cn.iocoder.yudao.module.aigc.asset.api;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.asset.dal.dataobject.AigcAssetDO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetAuditUpdateReqDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetCreateReqDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetCreateRespDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetDownloadReqDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetPageReqDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetRespDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetVisibilityUpdateReqDTO;
import cn.iocoder.yudao.module.aigc.asset.enums.AigcAssetTypeEnum;
import cn.iocoder.yudao.module.aigc.asset.service.asset.AigcAssetService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@Validated
public class AigcAssetApiImpl implements AigcAssetApi {

    @Resource
    private AigcAssetService assetService;

    @Override
    public CommonResult<AigcAssetCreateRespDTO> createAsset(AigcAssetCreateReqDTO reqDTO) {
        return success(assetService.createAsset(reqDTO));
    }

    @Override
    public CommonResult<AigcAssetCreateRespDTO> createImageAsset(AigcAssetCreateReqDTO reqDTO) {
        reqDTO.setAssetType(AigcAssetTypeEnum.IMAGE.getCode());
        return success(assetService.createAsset(reqDTO));
    }

    @Override
    public CommonResult<AigcAssetCreateRespDTO> createVideoAsset(AigcAssetCreateReqDTO reqDTO) {
        reqDTO.setAssetType(AigcAssetTypeEnum.VIDEO.getCode());
        return success(assetService.createAsset(reqDTO));
    }

    @Override
    public CommonResult<AigcAssetCreateRespDTO> createAudioAsset(AigcAssetCreateReqDTO reqDTO) {
        reqDTO.setAssetType(AigcAssetTypeEnum.AUDIO.getCode());
        return success(assetService.createAsset(reqDTO));
    }

    @Override
    public CommonResult<AigcAssetCreateRespDTO> createDocumentAsset(AigcAssetCreateReqDTO reqDTO) {
        reqDTO.setAssetType(AigcAssetTypeEnum.DOCUMENT.getCode());
        return success(assetService.createAsset(reqDTO));
    }

    @Override
    public CommonResult<AigcAssetRespDTO> getAsset(Long assetId) {
        AigcAssetDO asset = assetService.validateAssetExists(assetId);
        return success(BeanUtils.toBean(asset, AigcAssetRespDTO.class));
    }

    @Override
    public CommonResult<List<AigcAssetRespDTO>> getAssets(List<Long> assetIds) {
        if (assetIds == null || assetIds.isEmpty()) {
            return success(Collections.emptyList());
        }
        return success(BeanUtils.toBean(assetService.getAssetList(assetIds), AigcAssetRespDTO.class));
    }

    @Override
    public CommonResult<AigcAssetRespDTO> getAssetByTaskId(Long taskId) {
        AigcAssetDO asset = assetService.getAssetByTaskId(taskId);
        return success(BeanUtils.toBean(asset, AigcAssetRespDTO.class));
    }

    @Override
    public CommonResult<PageResult<AigcAssetRespDTO>> getUserAssets(AigcAssetPageReqDTO reqDTO) {
        PageResult<AigcAssetDO> pageResult = assetService.getAssetPage(reqDTO);
        return success(BeanUtils.toBean(pageResult, AigcAssetRespDTO.class));
    }

    @Override
    public CommonResult<Boolean> increaseDownloadCount(AigcAssetDownloadReqDTO reqDTO) {
        assetService.increaseDownloadCount(reqDTO);
        return success(true);
    }

    @Override
    public CommonResult<Boolean> updateAuditStatus(AigcAssetAuditUpdateReqDTO reqDTO) {
        assetService.updateAuditStatus(reqDTO);
        return success(true);
    }

    @Override
    public CommonResult<Boolean> updateVisibility(AigcAssetVisibilityUpdateReqDTO reqDTO) {
        assetService.updateVisibility(reqDTO);
        return success(true);
    }

}
