package cn.iocoder.yudao.module.aigc.asset.service.asset;

import cn.hutool.core.lang.UUID;
import cn.hutool.http.HttpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.asset.controller.admin.vo.AigcAssetDownloadLogPageReqVO;
import cn.iocoder.yudao.module.aigc.asset.controller.admin.vo.AigcAssetPageReqVO;
import cn.iocoder.yudao.module.aigc.asset.controller.admin.vo.AigcAssetSaveReqVO;
import cn.iocoder.yudao.module.aigc.asset.dal.dataobject.AigcAssetDO;
import cn.iocoder.yudao.module.aigc.asset.dal.dataobject.AigcAssetDownloadLogDO;
import cn.iocoder.yudao.module.aigc.asset.dal.mysql.AigcAssetDownloadLogMapper;
import cn.iocoder.yudao.module.aigc.asset.dal.mysql.AigcAssetMapper;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetAuditUpdateReqDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetCreateReqDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetCreateRespDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetDownloadReqDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetPageReqDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetUpdateReqDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetVisibilityUpdateReqDTO;
import cn.iocoder.yudao.module.aigc.asset.enums.AigcAssetAuditStatusEnum;
import cn.iocoder.yudao.module.aigc.asset.enums.AigcAssetBizTypeEnum;
import cn.iocoder.yudao.module.aigc.asset.enums.AigcAssetSourceTypeEnum;
import cn.iocoder.yudao.module.aigc.asset.enums.AigcAssetStatusEnum;
import cn.iocoder.yudao.module.aigc.asset.enums.AigcAssetTypeEnum;
import cn.iocoder.yudao.module.aigc.asset.enums.AigcAssetVisibilityEnum;
import cn.iocoder.yudao.module.aigc.task.api.AigcTaskApi;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskRespDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskStatusUpdateReqDTO;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.aigc.asset.enums.ErrorCodeConstants.*;

@Service
@Validated
public class AigcAssetServiceImpl implements AigcAssetService {

    @Resource
    private AigcAssetMapper assetMapper;
    @Resource
    private AigcAssetDownloadLogMapper downloadLogMapper;
    @Resource
    private AigcTaskApi taskApi;
    @Resource
    private FileApi fileApi;

    private static final long MAX_FILE_SIZE = 200L * 1024 * 1024;

    @Override
    public Long createAsset(AigcAssetSaveReqVO reqVO) {
        AigcAssetDO asset = BeanUtils.toBean(reqVO, AigcAssetDO.class)
                .setAssetNo(generateAssetNo())
                .setSourceType(StrUtil.blankToDefault(reqVO.getSourceType(), AigcAssetSourceTypeEnum.UPLOAD.getCode()))
                .setVisibility(StrUtil.blankToDefault(reqVO.getVisibility(), AigcAssetVisibilityEnum.PRIVATE.getCode()))
                .setAuditStatus(StrUtil.blankToDefault(reqVO.getAuditStatus(), AigcAssetAuditStatusEnum.PENDING.getCode()))
                .setStatus(AigcAssetStatusEnum.NORMAL.getCode())
                .setViewCount(0)
                .setDownloadCount(0)
                .setUseCount(0);
        assetMapper.insert(asset);
        return asset.getId();
    }

    @Override
    public Long uploadAsset(Long userId, String assetType, String title, String fileName, String mimeType, byte[] content) {
        if (content == null || content.length == 0) {
            throw exception(ASSET_FILE_EMPTY);
        }
        validateFileSize(content.length);
        String fileUrl = fileApi.createFile(content, fileName, "aigc/asset", mimeType);
        AigcAssetSaveReqVO reqVO = new AigcAssetSaveReqVO()
                .setUserId(userId)
                .setAssetType(assetType)
                .setSourceType(AigcAssetSourceTypeEnum.UPLOAD.getCode())
                .setTitle(title)
                .setFileUrl(fileUrl)
                .setMimeType(mimeType)
                .setFileSize((long) content.length)
                .setVisibility(AigcAssetVisibilityEnum.PRIVATE.getCode())
                .setAuditStatus(AigcAssetAuditStatusEnum.PENDING.getCode());
        return createAsset(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AigcAssetCreateRespDTO createAsset(AigcAssetCreateReqDTO reqDTO) {
        prepareFile(reqDTO);
        normalizeSnapshotFields(reqDTO);
        if (reqDTO.getTaskId() != null) {
            AigcAssetDO existedAsset = assetMapper.selectByTaskIdAndType(reqDTO.getTaskId(), reqDTO.getAssetType());
            if (existedAsset != null) {
                return buildCreateRespDTO(existedAsset);
            }
            validateTaskExists(reqDTO.getTaskId());
        }
        AigcAssetDO asset = BeanUtils.toBean(reqDTO, AigcAssetDO.class)
                .setAssetNo(generateAssetNo())
                .setSourceType(StrUtil.blankToDefault(reqDTO.getSourceType(), AigcAssetSourceTypeEnum.GENERATE.getCode()))
                .setBizType(StrUtil.blankToDefault(reqDTO.getBizType(), reqDTO.getTaskId() == null ? null : AigcAssetBizTypeEnum.TASK.getCode()))
                .setVisibility(StrUtil.blankToDefault(reqDTO.getVisibility(), AigcAssetVisibilityEnum.PRIVATE.getCode()))
                .setAuditStatus(StrUtil.blankToDefault(reqDTO.getAuditStatus(), AigcAssetAuditStatusEnum.PENDING.getCode()))
                .setStatus(AigcAssetStatusEnum.NORMAL.getCode())
                .setViewCount(0)
                .setDownloadCount(0)
                .setUseCount(0);
        assetMapper.insert(asset);
        tryMarkTaskSuccess(asset);
        return buildCreateRespDTO(asset);
    }

    private void normalizeSnapshotFields(AigcAssetCreateReqDTO reqDTO) {
        reqDTO.setPromptSnapshot(normalizeJsonSnapshot(reqDTO.getPromptSnapshot(), "prompt"));
        reqDTO.setGenerateSnapshot(normalizeJsonSnapshot(reqDTO.getGenerateSnapshot(), "raw"));
    }

    private String normalizeJsonSnapshot(String snapshot, String rawKey) {
        if (StrUtil.isBlank(snapshot)) {
            return null;
        }
        if (JSONUtil.isTypeJSON(snapshot)) {
            return snapshot;
        }
        return JSONUtil.createObj().set(rawKey, snapshot).toString();
    }

    @Override
    public void updateAsset(AigcAssetSaveReqVO reqVO) {
        validateAssetExists(reqVO.getId());
        AigcAssetDO updateObj = BeanUtils.toBean(reqVO, AigcAssetDO.class);
        assetMapper.updateById(updateObj);
    }

    @Override
    public void updateAsset(AigcAssetUpdateReqDTO reqDTO) {
        validateAssetExists(reqDTO.getId());
        assetMapper.updateById(new AigcAssetDO()
                .setId(reqDTO.getId())
                .setTitle(reqDTO.getTitle())
                .setDescription(reqDTO.getDescription())
                .setTags(reqDTO.getTags()));
    }

    @Override
    public void deleteAsset(Long id) {
        validateAssetExists(id);
        assetMapper.updateById(new AigcAssetDO().setId(id).setStatus(AigcAssetStatusEnum.DELETED.getCode()));
    }

    @Override
    public void recoverAsset(Long id) {
        validateAssetExists(id);
        assetMapper.updateById(new AigcAssetDO().setId(id).setStatus(AigcAssetStatusEnum.NORMAL.getCode()));
    }

    @Override
    public AigcAssetDO getAsset(Long id) {
        return assetMapper.selectById(id);
    }

    @Override
    public List<AigcAssetDO> getAssetList(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return assetMapper.selectBatchIds(ids);
    }

    @Override
    public AigcAssetDO validateAssetExists(Long id) {
        AigcAssetDO asset = assetMapper.selectById(id);
        if (asset == null) {
            throw exception(ASSET_NOT_EXISTS);
        }
        return asset;
    }

    @Override
    public AigcAssetDO getAssetByTaskId(Long taskId) {
        return assetMapper.selectByTaskId(taskId);
    }

    @Override
    public PageResult<AigcAssetDO> getAssetPage(AigcAssetPageReqVO reqVO) {
        return assetMapper.selectPage(reqVO);
    }

    @Override
    public PageResult<AigcAssetDO> getAssetPage(AigcAssetPageReqDTO reqDTO) {
        return assetMapper.selectPage(reqDTO);
    }

    @Override
    public AigcAssetDO getUserAsset(Long id, Long userId) {
        AigcAssetDO asset = validateAssetExists(id);
        validateUserPermission(asset, userId);
        return asset;
    }

    @Override
    public AigcAssetDO getAccessibleAsset(Long id, Long userId) {
        AigcAssetDO asset = validateAssetExists(id);
        validateAssetStatus(asset);
        if (asset.getUserId().equals(userId)) {
            return asset;
        }
        if (AigcAssetVisibilityEnum.PUBLIC.getCode().equals(asset.getVisibility())
                && AigcAssetAuditStatusEnum.PASS.getCode().equals(asset.getAuditStatus())) {
            return asset;
        }
        throw exception(ASSET_NO_PERMISSION);
    }

    @Override
    public PageResult<AigcAssetDO> getUserAssetPage(AigcAssetPageReqVO reqVO, Long userId) {
        reqVO.setUserId(userId);
        reqVO.setStatus(AigcAssetStatusEnum.NORMAL.getCode());
        return assetMapper.selectPage(reqVO);
    }

    @Override
    public void updateAuditStatus(AigcAssetAuditUpdateReqDTO reqDTO) {
        validateAssetExists(reqDTO.getId());
        assetMapper.updateById(new AigcAssetDO()
                .setId(reqDTO.getId())
                .setAuditStatus(reqDTO.getAuditStatus())
                .setAuditReason(reqDTO.getAuditReason()));
    }

    @Override
    public void updateVisibility(AigcAssetVisibilityUpdateReqDTO reqDTO) {
        validateAssetExists(reqDTO.getId());
        assetMapper.updateById(new AigcAssetDO().setId(reqDTO.getId()).setVisibility(reqDTO.getVisibility()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void increaseDownloadCount(AigcAssetDownloadReqDTO reqDTO) {
        AigcAssetDO asset = validateAssetExists(reqDTO.getAssetId());
        validateAssetAvailable(asset);
        assetMapper.increaseDownloadCount(reqDTO.getAssetId());
        downloadLogMapper.insert(new AigcAssetDownloadLogDO()
                .setAssetId(asset.getId())
                .setAssetNo(asset.getAssetNo())
                .setUserId(reqDTO.getUserId())
                .setOwnerUserId(asset.getUserId())
                .setDownloadUrl(asset.getFileUrl())
                .setClientIp(reqDTO.getClientIp())
                .setUserAgent(reqDTO.getUserAgent())
                .setReferer(reqDTO.getReferer())
                .setResult("SUCCESS"));
    }

    @Override
    public void increaseUseCount(Long id, Long userId) {
        AigcAssetDO asset = getAccessibleAsset(id, userId);
        validateAssetAvailable(asset);
        assetMapper.increaseUseCount(id);
    }

    @Override
    public PageResult<AigcAssetDownloadLogDO> getDownloadLogPage(AigcAssetDownloadLogPageReqVO reqVO) {
        return downloadLogMapper.selectPage(reqVO);
    }

    @Override
    public Long getAssetCount() {
        return assetMapper.selectNormalCount();
    }

    @Override
    public Long getDownloadCount() {
        return downloadLogMapper.selectSuccessCount();
    }

    private void validateTaskExists(Long taskId) {
        CommonResult<AigcTaskRespDTO> result = taskApi.getTask(taskId);
        if (result.isError() || result.getData() == null) {
            throw exception(ASSET_TASK_NOT_EXISTS);
        }
    }

    private void tryMarkTaskSuccess(AigcAssetDO asset) {
        if (asset.getTaskId() == null) {
            return;
        }
        try {
            taskApi.markAssetCreating(asset.getTaskId()).checkError();
            taskApi.markSuccess(new AigcTaskStatusUpdateReqDTO()
                    .setTaskId(asset.getTaskId())
                    .setOutputAssetId(asset.getId())
                    .setOutputAssetType(asset.getAssetType())
                    .setProgress(100)).checkError();
        } catch (Exception ignored) {
        }
    }

    private void validateUserPermission(AigcAssetDO asset, Long userId) {
        if (!asset.getUserId().equals(userId)) {
            throw exception(ASSET_NO_PERMISSION);
        }
        validateAssetStatus(asset);
    }

    private void validateAssetAvailable(AigcAssetDO asset) {
        validateAssetStatus(asset);
        if (!AigcAssetAuditStatusEnum.PASS.getCode().equals(asset.getAuditStatus())) {
            throw exception(ASSET_AUDIT_NOT_PASS);
        }
    }

    private void validateAssetStatus(AigcAssetDO asset) {
        if (!AigcAssetStatusEnum.NORMAL.getCode().equals(asset.getStatus())) {
            throw exception(ASSET_STATUS_INVALID);
        }
    }

    private AigcAssetCreateRespDTO buildCreateRespDTO(AigcAssetDO asset) {
        return new AigcAssetCreateRespDTO()
                .setId(asset.getId())
                .setAssetNo(asset.getAssetNo())
                .setAssetType(asset.getAssetType())
                .setFileUrl(asset.getFileUrl());
    }

    private String generateAssetNo() {
        return "AST" + UUID.fastUUID().toString(true).toUpperCase();
    }

    private void prepareFile(AigcAssetCreateReqDTO reqDTO) {
        if (StrUtil.isBlank(reqDTO.getFileUrl()) && StrUtil.isBlank(reqDTO.getOriginUrl())) {
            throw exception(ASSET_FILE_EMPTY);
        }
        if (StrUtil.isNotBlank(reqDTO.getFileUrl())) {
            return;
        }
        if (StrUtil.startWithIgnoreCase(reqDTO.getOriginUrl(), "data:")) {
            prepareDataUrlFile(reqDTO);
            return;
        }
        byte[] content = HttpUtil.downloadBytes(reqDTO.getOriginUrl());
        if (content == null || content.length == 0) {
            throw exception(ASSET_DOWNLOAD_FAILED);
        }
        validateFileSize(content.length);
        String fileUrl = fileApi.createFile(content, reqDTO.getTitle(), "aigc/asset", reqDTO.getMimeType());
        reqDTO.setFileUrl(fileUrl);
        reqDTO.setFileSize((long) content.length);
    }

    private void prepareDataUrlFile(AigcAssetCreateReqDTO reqDTO) {
        String dataUrl = reqDTO.getOriginUrl();
        int commaIndex = dataUrl.indexOf(',');
        if (commaIndex <= 5 || !dataUrl.substring(0, commaIndex).contains(";base64")) {
            throw exception(ASSET_DOWNLOAD_FAILED);
        }
        String mimeType = StrUtil.blankToDefault(reqDTO.getMimeType(), dataUrl.substring("data:".length(), dataUrl.indexOf(";base64")));
        byte[] content;
        try {
            content = Base64.getDecoder().decode(dataUrl.substring(commaIndex + 1));
        } catch (IllegalArgumentException ex) {
            throw exception(ASSET_DOWNLOAD_FAILED);
        }
        if (content.length == 0) {
            throw exception(ASSET_DOWNLOAD_FAILED);
        }
        validateFileSize(content.length);
        String fileExt = StrUtil.blankToDefault(reqDTO.getFileExt(), fileExtFromMimeType(mimeType));
        String fileName = StrUtil.blankToDefault(reqDTO.getTitle(), "aigc-asset") + "." + fileExt;
        String fileUrl = fileApi.createFile(content, fileName, "aigc/asset", mimeType);
        reqDTO.setFileUrl(fileUrl);
        reqDTO.setFileSize((long) content.length);
        reqDTO.setMimeType(mimeType);
        reqDTO.setFileExt(fileExt);
        reqDTO.setOriginUrl(null);
    }

    private String fileExtFromMimeType(String mimeType) {
        if (StrUtil.equalsIgnoreCase(mimeType, "image/jpeg")) {
            return "jpg";
        }
        if (StrUtil.startWithIgnoreCase(mimeType, "image/")) {
            return StrUtil.subAfter(mimeType, "image/", true);
        }
        if (StrUtil.startWithIgnoreCase(mimeType, "video/")) {
            return StrUtil.subAfter(mimeType, "video/", true);
        }
        if (StrUtil.startWithIgnoreCase(mimeType, "audio/")) {
            return StrUtil.subAfter(mimeType, "audio/", true);
        }
        return "bin";
    }

    private void validateFileSize(long fileSize) {
        if (fileSize > MAX_FILE_SIZE) {
            throw exception(ASSET_FILE_SIZE_EXCEED);
        }
    }

}
