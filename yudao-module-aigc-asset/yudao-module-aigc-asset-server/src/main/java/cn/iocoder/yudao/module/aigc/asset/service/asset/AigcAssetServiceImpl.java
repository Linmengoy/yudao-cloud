package cn.iocoder.yudao.module.aigc.asset.service.asset;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.aigc.asset.controller.admin.vo.AigcAssetDownloadLogPageReqVO;
import cn.iocoder.yudao.module.aigc.asset.controller.admin.vo.AigcAssetPageReqVO;
import cn.iocoder.yudao.module.aigc.asset.controller.admin.vo.AigcAssetSaveReqVO;
import cn.iocoder.yudao.module.aigc.asset.dal.dataobject.AigcAssetDO;
import cn.iocoder.yudao.module.aigc.asset.dal.dataobject.AigcAssetDownloadLogDO;
import cn.iocoder.yudao.module.aigc.asset.dal.dataobject.AigcAssetFileDO;
import cn.iocoder.yudao.module.aigc.asset.dal.mysql.AigcAssetFileMapper;
import cn.iocoder.yudao.module.aigc.asset.dal.mysql.AigcAssetDownloadLogMapper;
import cn.iocoder.yudao.module.aigc.asset.dal.mysql.AigcAssetMapper;
import cn.iocoder.yudao.module.aigc.asset.dal.redis.AigcAssetAccessUrlRedisDAO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetAccessUrlReqDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetAccessUrlRespDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetAuditUpdateReqDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetCreateReqDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetCreateRespDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetDownloadReqDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetFileRespDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetPageReqDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetRespDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetUpdateReqDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetVisibilityUpdateReqDTO;
import cn.iocoder.yudao.module.aigc.asset.enums.AigcAssetAccessModeEnum;
import cn.iocoder.yudao.module.aigc.asset.enums.AigcAssetAccessTypeEnum;
import cn.iocoder.yudao.module.aigc.asset.enums.AigcAssetAuditStatusEnum;
import cn.iocoder.yudao.module.aigc.asset.enums.AigcAssetBizTypeEnum;
import cn.iocoder.yudao.module.aigc.asset.enums.AigcAssetFileRoleEnum;
import cn.iocoder.yudao.module.aigc.asset.enums.AigcAssetSourceTypeEnum;
import cn.iocoder.yudao.module.aigc.asset.enums.AigcAssetStatusEnum;
import cn.iocoder.yudao.module.aigc.asset.enums.AigcAssetTypeEnum;
import cn.iocoder.yudao.module.aigc.asset.enums.AigcAssetVisibilityEnum;
import cn.iocoder.yudao.module.aigc.task.api.AigcTaskApi;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskRespDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskStatusUpdateReqDTO;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import cn.iocoder.yudao.module.infra.api.file.dto.FileCreateRespDTO;
import cn.iocoder.yudao.module.infra.api.file.dto.FilePresignRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.math.RoundingMode;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.aigc.asset.dal.redis.RedisKeyConstants.ASSET_ACCESS_URL;
import static cn.iocoder.yudao.module.aigc.asset.dal.redis.RedisKeyConstants.ASSET_ACCESS_URL_LOCK;
import static cn.iocoder.yudao.module.aigc.asset.enums.ErrorCodeConstants.*;

@Service
@Validated
@Slf4j
public class AigcAssetServiceImpl implements AigcAssetService {

    private static final int REMOTE_FILE_DOWNLOAD_TIMEOUT_MILLIS = 20_000;
    private static final int REMOTE_FILE_DOWNLOAD_TIMEOUT_SECONDS = 30;
    private static final int VIDEO_FRAME_CAPTURE_TIMEOUT_SECONDS = 45;
    private static final BigDecimal LAST_FRAME_OFFSET_SECONDS = new BigDecimal("0.05");
    private static final String REMOTE_FILE_DOWNLOAD_USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36";

    @Resource
    private AigcAssetMapper assetMapper;
    @Resource
    private AigcAssetDownloadLogMapper downloadLogMapper;
    @Resource
    private AigcAssetFileMapper assetFileMapper;
    @Resource
    private AigcAssetAccessUrlRedisDAO accessUrlRedisDAO;
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
        FileCreateRespDTO file = fileApi.createFileV2(content, uniqueAssetStorageFileName(fileName, mimeType), "aigc/asset", mimeType);
        AigcAssetSaveReqVO reqVO = new AigcAssetSaveReqVO()
                .setUserId(userId)
                .setAssetType(assetType)
                .setSourceType(AigcAssetSourceTypeEnum.UPLOAD.getCode())
                .setTitle(title)
                .setVisibility(AigcAssetVisibilityEnum.PRIVATE.getCode())
                .setAuditStatus(AigcAssetAuditStatusEnum.PENDING.getCode());
        Long assetId = createAsset(reqVO);
        assetFileMapper.insert(buildAssetFileDO(assetId, AigcAssetFileRoleEnum.ORIGINAL.getCode(), file, null, null, null, null));
        return assetId;
    }

    @Override
    public Long captureVideoFrame(Long userId, Long videoAssetId, String capturedAt, BigDecimal timeSec, String title) {
        AigcAssetDO videoAsset = getAccessibleAsset(videoAssetId, userId);
        if (!AigcAssetTypeEnum.VIDEO.getCode().equals(videoAsset.getAssetType())) {
            throw exception(ASSET_FILE_TYPE_UNSUPPORTED);
        }
        AigcAssetFileDO videoFile = assetFileMapper.selectByAssetIdAndRole(videoAssetId, AigcAssetFileRoleEnum.ORIGINAL.getCode());
        if (videoFile == null) {
            throw exception(ASSET_FILE_EMPTY);
        }
        AigcAssetAccessUrlRespDTO accessUrl = getAccessUrl(videoAsset, videoFile, AigcAssetAccessTypeEnum.DOWNLOAD.getCode(), userId);
        byte[] frameContent = captureVideoFrameBytes(accessUrl.getUrl(), resolveCaptureSecond(capturedAt, timeSec, videoFile.getDuration()));
        String frameTitle = StrUtil.blankToDefault(title, StrUtil.blankToDefault(videoAsset.getTitle(), "Video") + " 截帧.png");
        return uploadAsset(userId, AigcAssetTypeEnum.IMAGE.getCode(), frameTitle, ensurePngFileName(frameTitle), "image/png", frameContent);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AigcAssetCreateRespDTO createAsset(AigcAssetCreateReqDTO reqDTO) {
        AigcAssetFileDO file = prepareFile(reqDTO);
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
        file.setAssetId(asset.getId());
        assetFileMapper.insert(file);
        tryMarkTaskSuccess(asset);
        return buildCreateRespDTO(asset, file);
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
    public AigcAssetRespDTO getAssetResp(Long id, Long userId) {
        AigcAssetDO asset = userId == null ? validateAssetExists(id) : getAccessibleAsset(id, userId);
        return buildAssetRespDTO(asset, assetFileMapper.selectListByAssetId(id), userId);
    }

    @Override
    public List<AigcAssetDO> getAssetList(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return assetMapper.selectBatchIds(ids);
    }

    @Override
    public List<AigcAssetRespDTO> getAssetRespList(Collection<Long> ids, Long userId) {
        List<AigcAssetDO> assets = getAssetList(ids);
        Map<Long, List<AigcAssetFileDO>> fileMap = assetFileMapper.selectListByAssetIds(ids).stream()
                .collect(Collectors.groupingBy(AigcAssetFileDO::getAssetId));
        return assets.stream().map(asset -> buildAssetRespDTO(asset, fileMap.get(asset.getId()), userId)).collect(Collectors.toList());
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
    public List<AigcAssetDO> getUserAssetList(AigcAssetPageReqVO reqVO, Long userId) {
        reqVO.setUserId(userId);
        reqVO.setStatus(AigcAssetStatusEnum.NORMAL.getCode());
        return assetMapper.selectList(reqVO);
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
                .setDownloadUrl(null)
                .setClientIp(reqDTO.getClientIp())
                .setUserAgent(reqDTO.getUserAgent())
                .setReferer(reqDTO.getReferer())
                .setResult("SUCCESS"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AigcAssetAccessUrlRespDTO getAccessUrl(AigcAssetAccessUrlReqDTO reqDTO, Long userId) {
        AigcAssetDO asset = getAccessibleAsset(reqDTO.getAssetId(), userId);
        validateAssetAvailable(asset);
        AigcAssetFileDO file = assetFileMapper.selectByAssetIdAndRole(asset.getId(), reqDTO.getFileRole());
        if (file == null) {
            file = assetFileMapper.selectByAssetIdAndRole(asset.getId(), AigcAssetFileRoleEnum.ORIGINAL.getCode());
        }
        if (file == null) {
            throw exception(ASSET_FILE_EMPTY);
        }
        AigcAssetAccessUrlRespDTO resp = getAccessUrl(asset, file, reqDTO.getAccessType(), userId);
        if (AigcAssetAccessTypeEnum.DOWNLOAD.getCode().equals(reqDTO.getAccessType())) {
            assetMapper.increaseDownloadCount(asset.getId());
            downloadLogMapper.insert(new AigcAssetDownloadLogDO()
                    .setAssetId(asset.getId())
                    .setAssetNo(asset.getAssetNo())
                    .setUserId(userId)
                    .setOwnerUserId(asset.getUserId())
                    .setDownloadUrl(null)
                    .setResult("SUCCESS"));
        }
        return resp;
    }

    @Override
    public List<AigcAssetAccessUrlRespDTO> getAccessUrls(List<AigcAssetAccessUrlReqDTO> reqDTOs, Long userId) {
        if (reqDTOs == null || reqDTOs.isEmpty()) {
            return Collections.emptyList();
        }
        return reqDTOs.stream().map(reqDTO -> getAccessUrl(reqDTO, userId)).collect(Collectors.toList());
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

    private BigDecimal resolveCaptureSecond(String capturedAt, BigDecimal timeSec, BigDecimal duration) {
        if ("first".equalsIgnoreCase(capturedAt)) {
            return BigDecimal.ZERO;
        }
        if ("last".equalsIgnoreCase(capturedAt) && duration != null && duration.compareTo(BigDecimal.ZERO) > 0) {
            return duration.subtract(LAST_FRAME_OFFSET_SECONDS).max(BigDecimal.ZERO);
        }
        if (timeSec == null || timeSec.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (duration != null && duration.compareTo(BigDecimal.ZERO) > 0) {
            return timeSec.min(duration.subtract(LAST_FRAME_OFFSET_SECONDS).max(BigDecimal.ZERO));
        }
        return timeSec;
    }

    private byte[] captureVideoFrameBytes(String videoUrl, BigDecimal second) {
        Path outputFile = null;
        try {
            outputFile = Files.createTempFile("aigc-video-frame-", ".png");
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "ffmpeg",
                    "-hide_banner",
                    "-loglevel", "error",
                    "-y",
                    "-ss", second.setScale(3, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString(),
                    "-i", videoUrl,
                    "-frames:v", "1",
                    "-f", "image2",
                    outputFile.toString());
            Process process = processBuilder.start();
            boolean finished = process.waitFor(VIDEO_FRAME_CAPTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!finished) {
                process.destroyForcibly();
                log.warn("[captureVideoFrameBytes][videoUrl({}) second({}) ffmpeg 截帧超时]", videoUrl, second);
                throw exception(ASSET_DOWNLOAD_FAILED);
            }
            if (process.exitValue() != 0) {
                log.warn("[captureVideoFrameBytes][videoUrl({}) second({}) ffmpeg 截帧失败 exit({}) error({})]",
                        videoUrl, second, process.exitValue(), StrUtil.maxLength(stderr, 512));
                throw exception(ASSET_DOWNLOAD_FAILED);
            }
            byte[] content = Files.readAllBytes(outputFile);
            if (content.length == 0) {
                throw exception(ASSET_DOWNLOAD_FAILED);
            }
            return content;
        } catch (Exception ex) {
            log.warn("[captureVideoFrameBytes][videoUrl({}) second({}) 截帧异常]", videoUrl, second, ex);
            throw exception(ASSET_DOWNLOAD_FAILED);
        } finally {
            if (outputFile != null) {
                try {
                    Files.deleteIfExists(outputFile);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private String ensurePngFileName(String title) {
        String fileName = StrUtil.blankToDefault(title, "video-frame.png");
        return StrUtil.endWithIgnoreCase(fileName, ".png") ? fileName : fileName + ".png";
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
        AigcAssetFileDO file = assetFileMapper.selectByAssetIdAndRole(asset.getId(), AigcAssetFileRoleEnum.ORIGINAL.getCode());
        return buildCreateRespDTO(asset, file);
    }

    private AigcAssetCreateRespDTO buildCreateRespDTO(AigcAssetDO asset, AigcAssetFileDO file) {
        AigcAssetCreateRespDTO respDTO = new AigcAssetCreateRespDTO()
                .setId(asset.getId())
                .setAssetNo(asset.getAssetNo())
                .setAssetType(asset.getAssetType());
        if (file == null) {
            return respDTO;
        }
        return respDTO.setAssetFileId(file.getId())
                .setFileId(file.getFileId())
                .setFileUrl(file.getPublicUrl())
                .setObjectKey(file.getObjectKey())
                .setFilePath(file.getFilePath());
    }

    private String generateAssetNo() {
        return "AST" + UUID.fastUUID().toString(true).toUpperCase();
    }

    private AigcAssetFileDO prepareFile(AigcAssetCreateReqDTO reqDTO) {
        if (StrUtil.isBlank(reqDTO.getFileUrl()) && StrUtil.isBlank(reqDTO.getOriginUrl())) {
            throw exception(ASSET_FILE_EMPTY);
        }
        if (StrUtil.isNotBlank(reqDTO.getFileUrl())) {
            return buildExternalAssetFileDO(reqDTO);
        }
        if (StrUtil.startWithIgnoreCase(reqDTO.getOriginUrl(), "data:")) {
            return prepareDataUrlFile(reqDTO);
        }
        byte[] content = downloadOriginFile(reqDTO);
        if (content == null || content.length == 0) {
            throw exception(ASSET_DOWNLOAD_FAILED);
        }
        validateFileSize(content.length);
        FileCreateRespDTO file = fileApi.createFileV2(content, uniqueAssetStorageFileName(reqDTO.getTitle(), reqDTO.getMimeType()), "aigc/asset", reqDTO.getMimeType());
        reqDTO.setFileSize((long) content.length);
        return buildAssetFileDO(null, AigcAssetFileRoleEnum.ORIGINAL.getCode(), file, reqDTO.getOriginUrl(), reqDTO.getWidth(), reqDTO.getHeight(), reqDTO.getDuration());
    }

    private byte[] downloadOriginFile(AigcAssetCreateReqDTO reqDTO) {
        if (isSocksProxy(reqDTO)) {
            return downloadOriginFileByCurl(reqDTO);
        }
        HttpRequest request = HttpRequest.get(reqDTO.getOriginUrl())
                .header(Header.USER_AGENT, REMOTE_FILE_DOWNLOAD_USER_AGENT)
                .header(Header.ACCEPT, "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                .timeout(REMOTE_FILE_DOWNLOAD_TIMEOUT_MILLIS);
        try (HttpResponse response = AigcAssetProxyUtils.execute(request, reqDTO)) {
            if (!response.isOk()) {
                log.warn("[downloadOriginFile][originUrl({}) proxy({}:{}) HTTP 下载失败 status({}) body({})]",
                        reqDTO.getOriginUrl(), reqDTO.getProxyHost(), reqDTO.getProxyPort(), response.getStatus(), StrUtil.maxLength(response.body(), 512));
                throw exception(ASSET_DOWNLOAD_FAILED);
            }
            return response.bodyBytes();
        } catch (Exception ex) {
            log.warn("[downloadOriginFile][originUrl({}) proxy({}:{}) 下载异常]",
                    reqDTO.getOriginUrl(), reqDTO.getProxyHost(), reqDTO.getProxyPort(), ex);
            throw exception(ASSET_DOWNLOAD_FAILED);
        }
    }

    private byte[] downloadOriginFileByCurl(AigcAssetCreateReqDTO reqDTO) {
        Path outputFile = null;
        try {
            outputFile = Files.createTempFile("aigc-origin-", ".download");
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "curl",
                    "--socks5-hostname", reqDTO.getProxyHost() + ":" + reqDTO.getProxyPort(),
                    "--proxy-user", StrUtil.blankToDefault(reqDTO.getProxyUsername(), "") + ":" + StrUtil.blankToDefault(reqDTO.getProxyPassword(), ""),
                    "--location",
                    "--fail",
                    "--silent",
                    "--show-error",
                    "--max-time", String.valueOf(REMOTE_FILE_DOWNLOAD_TIMEOUT_SECONDS),
                    "--user-agent", REMOTE_FILE_DOWNLOAD_USER_AGENT,
                    "--header", "Accept: image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8",
                    "--output", outputFile.toString(),
                    reqDTO.getOriginUrl());
            Process process = processBuilder.start();
            boolean finished = process.waitFor(REMOTE_FILE_DOWNLOAD_TIMEOUT_SECONDS + 5L, TimeUnit.SECONDS);
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!finished) {
                process.destroyForcibly();
                log.warn("[downloadOriginFileByCurl][originUrl({}) proxy({}:{}) curl 下载超时]",
                        reqDTO.getOriginUrl(), reqDTO.getProxyHost(), reqDTO.getProxyPort());
                throw exception(ASSET_DOWNLOAD_FAILED);
            }
            if (process.exitValue() != 0) {
                log.warn("[downloadOriginFileByCurl][originUrl({}) proxy({}:{}) curl 下载失败 exit({}) error({})]",
                        reqDTO.getOriginUrl(), reqDTO.getProxyHost(), reqDTO.getProxyPort(), process.exitValue(), StrUtil.maxLength(stderr, 512));
                throw exception(ASSET_DOWNLOAD_FAILED);
            }
            return Files.readAllBytes(outputFile);
        } catch (Exception ex) {
            log.warn("[downloadOriginFileByCurl][originUrl({}) proxy({}:{}) 下载异常]",
                    reqDTO.getOriginUrl(), reqDTO.getProxyHost(), reqDTO.getProxyPort(), ex);
            throw exception(ASSET_DOWNLOAD_FAILED);
        } finally {
            if (outputFile != null) {
                try {
                    Files.deleteIfExists(outputFile);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private boolean isSocksProxy(AigcAssetCreateReqDTO reqDTO) {
        return reqDTO != null
                && Boolean.TRUE.equals(reqDTO.getProxyEnabled())
                && StrUtil.isNotBlank(reqDTO.getProxyHost())
                && reqDTO.getProxyPort() != null
                && ("SOCKS5".equalsIgnoreCase(reqDTO.getProxyProtocol()) || "SOCKS5H".equalsIgnoreCase(reqDTO.getProxyProtocol()));
    }

    private AigcAssetFileDO prepareDataUrlFile(AigcAssetCreateReqDTO reqDTO) {
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
        String fileName = uniqueAssetStorageFileName(StrUtil.blankToDefault(reqDTO.getTitle(), "aigc-asset") + "." + fileExt, mimeType);
        FileCreateRespDTO file = fileApi.createFileV2(content, fileName, "aigc/asset", mimeType);
        reqDTO.setFileSize((long) content.length);
        reqDTO.setMimeType(mimeType);
        reqDTO.setFileExt(fileExt);
        reqDTO.setOriginUrl(null);
        return buildAssetFileDO(null, AigcAssetFileRoleEnum.ORIGINAL.getCode(), file, null, reqDTO.getWidth(), reqDTO.getHeight(), reqDTO.getDuration())
                .setFileExt(fileExt);
    }

    private AigcAssetFileDO buildExternalAssetFileDO(AigcAssetCreateReqDTO reqDTO) {
        return new AigcAssetFileDO()
                .setFileRole(AigcAssetFileRoleEnum.ORIGINAL.getCode())
                .setObjectKey(reqDTO.getFileUrl())
                .setFilePath(reqDTO.getFileUrl())
                .setPublicUrl(reqDTO.getFileUrl())
                .setOriginUrl(reqDTO.getOriginUrl())
                .setFileName(reqDTO.getTitle())
                .setFileExt(reqDTO.getFileExt())
                .setMimeType(reqDTO.getMimeType())
                .setFileSize(reqDTO.getFileSize())
                .setWidth(reqDTO.getWidth())
                .setHeight(reqDTO.getHeight())
                .setDuration(reqDTO.getDuration())
                .setAccessMode(AigcAssetAccessModeEnum.PUBLIC.getCode())
                .setStatus(AigcAssetStatusEnum.NORMAL.getCode())
                .setMetadata(reqDTO.getMetadata());
    }

    private AigcAssetFileDO buildAssetFileDO(Long assetId, String fileRole, FileCreateRespDTO file, String originUrl,
                                            Integer width, Integer height, java.math.BigDecimal duration) {
        boolean publicAccess = BooleanUtil.isTrue(file.getPublicAccess());
        return new AigcAssetFileDO()
                .setAssetId(assetId)
                .setFileRole(fileRole)
                .setFileId(file.getId())
                .setStorageConfigId(file.getConfigId())
                .setStorageType(file.getStorageType())
                .setBucket(file.getBucket())
                .setObjectKey(StrUtil.blankToDefault(file.getObjectKey(), file.getPath()))
                .setFilePath(file.getPath())
                .setOriginUrl(originUrl)
                .setFileName(file.getName())
                .setFileExt(fileExtFromFileName(file.getName()))
                .setMimeType(file.getType())
                .setFileSize(file.getSize())
                .setWidth(width)
                .setHeight(height)
                .setDuration(duration)
                .setAccessMode(publicAccess ? AigcAssetAccessModeEnum.PUBLIC.getCode() : AigcAssetAccessModeEnum.PRIVATE_SIGNED.getCode())
                .setPublicUrl(publicAccess ? file.getUrl() : null)
                .setStatus(AigcAssetStatusEnum.NORMAL.getCode());
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

    private String uniqueAssetStorageFileName(String fileName, String mimeType) {
        String normalizedFileName = StrUtil.blankToDefault(fileName, "aigc-asset");
        String fileExt = fileExtFromFileName(normalizedFileName);
        if (StrUtil.isBlank(fileExt) && StrUtil.isNotBlank(mimeType)) {
            fileExt = fileExtFromMimeType(mimeType);
        }
        String fileBaseName = normalizedFileName;
        if (StrUtil.isNotBlank(fileExt)) {
            fileBaseName = StrUtil.removeSuffixIgnoreCase(fileBaseName, "." + fileExt);
        }
        fileBaseName = StrUtil.blankToDefault(fileBaseName.replaceAll("[\\\\/:*?\"<>|\\s]+", "-"), "aigc-asset");
        String uniqueName = fileBaseName + "-" + UUID.fastUUID().toString(true);
        return StrUtil.isBlank(fileExt) ? uniqueName : uniqueName + "." + fileExt;
    }

    private String fileExtFromFileName(String fileName) {
        if (StrUtil.isBlank(fileName) || !fileName.contains(".")) {
            return null;
        }
        return StrUtil.subAfter(fileName, ".", true);
    }

    private AigcAssetRespDTO buildAssetRespDTO(AigcAssetDO asset, List<AigcAssetFileDO> files, Long userId) {
        AigcAssetRespDTO respDTO = BeanUtils.toBean(asset, AigcAssetRespDTO.class);
        if (files == null || files.isEmpty()) {
            return respDTO.setFiles(Collections.emptyList());
        }
        List<AigcAssetFileRespDTO> fileRespDTOs = files.stream().map(file -> {
            AigcAssetAccessUrlRespDTO accessUrl = getAccessUrl(asset, file, accessTypeForRole(file.getFileRole()), userId);
            return new AigcAssetFileRespDTO()
                    .setAssetFileId(file.getId())
                    .setFileRole(file.getFileRole())
                    .setFileName(file.getFileName())
                    .setFileExt(file.getFileExt())
                    .setMimeType(file.getMimeType())
                    .setFileSize(file.getFileSize())
                    .setWidth(file.getWidth())
                    .setHeight(file.getHeight())
                    .setDuration(file.getDuration())
                    .setAccessUrl(accessUrl.getUrl())
                    .setExpireSeconds(accessUrl.getExpireSeconds())
                    .setExpireTime(accessUrl.getExpireTime())
                    .setPublicAccess(accessUrl.getPublicAccess());
        }).collect(Collectors.toList());
        respDTO.setFiles(fileRespDTOs);
        files.stream().filter(file -> AigcAssetFileRoleEnum.ORIGINAL.getCode().equals(file.getFileRole())).findFirst()
                .ifPresent(file -> fillLegacyFileFields(respDTO, file, fileRespDTOs));
        files.stream().filter(file -> AigcAssetFileRoleEnum.COVER.getCode().equals(file.getFileRole())).findFirst()
                .ifPresent(file -> fileRespDTOs.stream().filter(resp -> Objects.equals(resp.getAssetFileId(), file.getId())).findFirst()
                        .ifPresent(resp -> respDTO.setCoverUrl(resp.getAccessUrl())));
        files.stream().filter(file -> AigcAssetFileRoleEnum.THUMBNAIL.getCode().equals(file.getFileRole())).findFirst()
                .ifPresent(file -> fileRespDTOs.stream().filter(resp -> Objects.equals(resp.getAssetFileId(), file.getId())).findFirst()
                        .ifPresent(resp -> respDTO.setThumbnailUrl(resp.getAccessUrl())));
        return respDTO;
    }

    private void fillLegacyFileFields(AigcAssetRespDTO respDTO, AigcAssetFileDO file, List<AigcAssetFileRespDTO> fileRespDTOs) {
        respDTO.setFileId(file.getFileId())
                .setMimeType(file.getMimeType())
                .setFileExt(file.getFileExt())
                .setFileSize(file.getFileSize());
        fileRespDTOs.stream().filter(resp -> Objects.equals(resp.getAssetFileId(), file.getId())).findFirst()
                .ifPresent(resp -> respDTO.setFileUrl(resp.getAccessUrl()));
    }

    private String accessTypeForRole(String fileRole) {
        if (AigcAssetFileRoleEnum.THUMBNAIL.getCode().equals(fileRole)) {
            return AigcAssetAccessTypeEnum.THUMBNAIL.getCode();
        }
        if (AigcAssetFileRoleEnum.COVER.getCode().equals(fileRole)) {
            return AigcAssetAccessTypeEnum.COVER.getCode();
        }
        return AigcAssetAccessTypeEnum.PREVIEW.getCode();
    }

    private AigcAssetAccessUrlRespDTO getAccessUrl(AigcAssetDO asset, AigcAssetFileDO file, String accessType, Long userId) {
        if (!Objects.equals(AigcAssetAccessModeEnum.PRIVATE_SIGNED.getCode(), file.getAccessMode())) {
            return new AigcAssetAccessUrlRespDTO()
                    .setAssetId(asset.getId())
                    .setAssetFileId(file.getId())
                    .setFileRole(file.getFileRole())
                    .setAccessType(accessType)
                    .setUrl(file.getPublicUrl())
                    .setPublicAccess(true)
                    .setCacheHit(false);
        }
        String key = buildAccessUrlCacheKey(file, accessType, userId);
        AigcAssetAccessUrlRespDTO cached = accessUrlRedisDAO.get(key);
        if (cached != null) {
            return cached.setCacheHit(true);
        }
        String lockKey = buildAccessUrlLockKey(file, accessType, userId);
        try {
            return accessUrlRedisDAO.executeWithLock(lockKey, () -> {
                AigcAssetAccessUrlRespDTO cachedAgain = accessUrlRedisDAO.get(key);
                if (cachedAgain != null) {
                    return cachedAgain.setCacheHit(true);
                }
                AigcAssetAccessUrlRespDTO generated = generateAccessUrl(asset, file, accessType);
                accessUrlRedisDAO.set(key, generated, generated.getExpireSeconds());
                return generated;
            });
        } catch (Exception ex) {
            AigcAssetAccessUrlRespDTO generated = generateAccessUrl(asset, file, accessType);
            accessUrlRedisDAO.set(key, generated, generated.getExpireSeconds());
            return generated;
        }
    }

    private AigcAssetAccessUrlRespDTO generateAccessUrl(AigcAssetDO asset, AigcAssetFileDO file, String accessType) {
        Integer expireSeconds = AigcAssetAccessTypeEnum.getExpireSeconds(accessType);
        FilePresignRespDTO presign = fileApi.presignGetUrlV2(file.getStorageConfigId(), file.getFilePath(), expireSeconds).getCheckedData();
        return new AigcAssetAccessUrlRespDTO()
                .setAssetId(asset.getId())
                .setAssetFileId(file.getId())
                .setFileRole(file.getFileRole())
                .setAccessType(accessType)
                .setUrl(presign.getUrl())
                .setExpireSeconds(presign.getExpireSeconds())
                .setExpireTime(presign.getExpireTime())
                .setPublicAccess(presign.getPublicAccess())
                .setCacheHit(false);
    }

    private String buildAccessUrlCacheKey(AigcAssetFileDO file, String accessType, Long userId) {
        Long tenantId = TenantContextHolder.getTenantId() == null ? 0L : TenantContextHolder.getTenantId();
        String userKey = AigcAssetAccessTypeEnum.DOWNLOAD.getCode().equals(accessType) ? String.valueOf(userId) : "PUBLIC";
        return String.format(ASSET_ACCESS_URL, tenantId, file.getId(), file.getFileRole(), accessType, userKey);
    }

    private String buildAccessUrlLockKey(AigcAssetFileDO file, String accessType, Long userId) {
        Long tenantId = TenantContextHolder.getTenantId() == null ? 0L : TenantContextHolder.getTenantId();
        String userKey = AigcAssetAccessTypeEnum.DOWNLOAD.getCode().equals(accessType) ? String.valueOf(userId) : "PUBLIC";
        return String.format(ASSET_ACCESS_URL_LOCK, tenantId, file.getId(), file.getFileRole(), accessType, userKey);
    }

    private void validateFileSize(long fileSize) {
        if (fileSize > MAX_FILE_SIZE) {
            throw exception(ASSET_FILE_SIZE_EXCEED);
        }
    }

}
