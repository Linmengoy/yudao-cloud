package cn.iocoder.yudao.module.aigc.asset.service.template;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.asset.controller.app.vo.template.AigcPromptTemplateModelRespVO;
import cn.iocoder.yudao.module.aigc.asset.controller.app.vo.template.AigcPromptTemplatePageReqVO;
import cn.iocoder.yudao.module.aigc.asset.controller.app.vo.template.AigcPromptTemplateRespVO;
import cn.iocoder.yudao.module.aigc.asset.controller.app.vo.template.AigcPromptTemplateShareReqVO;
import cn.iocoder.yudao.module.aigc.asset.dal.dataobject.AigcAssetDO;
import cn.iocoder.yudao.module.aigc.asset.dal.dataobject.AigcAssetFileDO;
import cn.iocoder.yudao.module.aigc.asset.dal.dataobject.AigcPromptTemplateDO;
import cn.iocoder.yudao.module.aigc.asset.dal.mysql.AigcAssetFileMapper;
import cn.iocoder.yudao.module.aigc.asset.dal.mysql.AigcPromptTemplateMapper;
import cn.iocoder.yudao.module.aigc.asset.enums.AigcAssetAccessModeEnum;
import cn.iocoder.yudao.module.aigc.asset.enums.AigcAssetAccessTypeEnum;
import cn.iocoder.yudao.module.aigc.asset.enums.AigcAssetAuditStatusEnum;
import cn.iocoder.yudao.module.aigc.asset.enums.AigcAssetFileRoleEnum;
import cn.iocoder.yudao.module.aigc.asset.enums.AigcAssetStatusEnum;
import cn.iocoder.yudao.module.aigc.asset.enums.AigcAssetVisibilityEnum;
import cn.iocoder.yudao.module.aigc.asset.service.asset.AigcAssetService;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import cn.iocoder.yudao.module.infra.api.file.dto.FilePresignRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.aigc.asset.enums.ErrorCodeConstants.PROMPT_TEMPLATE_NOT_EXISTS;
import static cn.iocoder.yudao.module.aigc.asset.enums.ErrorCodeConstants.PROMPT_TEMPLATE_STATUS_INVALID;

@Service
@Validated
@Slf4j
public class AigcPromptTemplateServiceImpl implements AigcPromptTemplateService {

    private static final String SOURCE_TYPE_USER_SHARE = "USER_SHARE";
    private static final DateTimeFormatter TEMPLATE_NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Resource
    private AigcPromptTemplateMapper promptTemplateMapper;
    @Resource
    private AigcAssetService assetService;
    @Resource
    private AigcAssetFileMapper assetFileMapper;
    @Resource
    private FileApi fileApi;

    @Override
    public AigcPromptTemplateDO validateTemplateAvailable(Long id) {
        AigcPromptTemplateDO template = promptTemplateMapper.selectById(id);
        if (template == null) {
            throw exception(PROMPT_TEMPLATE_NOT_EXISTS);
        }
        if (!"NORMAL".equals(template.getStatus())
                || !"PUBLIC".equals(template.getVisibility())
                || !"PASS".equals(template.getAuditStatus())) {
            throw exception(PROMPT_TEMPLATE_STATUS_INVALID);
        }
        return template;
    }

    @Override
    public AigcPromptTemplateRespVO getTemplate(Long id) {
        AigcPromptTemplateDO template = validateTemplateAvailable(id);
        return buildRespVO(template);
    }

    @Override
    public PageResult<AigcPromptTemplateRespVO> getTemplatePage(AigcPromptTemplatePageReqVO reqVO) {
        PageResult<AigcPromptTemplateDO> pageResult = promptTemplateMapper.selectPage(reqVO);
        PageResult<AigcPromptTemplateRespVO> respPage = new PageResult<>();
        respPage.setTotal(pageResult.getTotal());
        respPage.setList(pageResult.getList().stream().map(this::buildRespVO).collect(Collectors.toList()));
        return respPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long shareTemplate(Long userId, AigcPromptTemplateShareReqVO reqVO) {
        AigcAssetDO coverAsset = assetService.getUserAsset(reqVO.getCoverAssetId(), userId);
        AigcAssetFileDO coverFile = assetFileMapper.selectByAssetIdAndRole(coverAsset.getId(),
                AigcAssetFileRoleEnum.ORIGINAL.getCode());

        AigcPromptTemplateDO template = new AigcPromptTemplateDO()
                .setTemplateNo(generateTemplateNo())
                .setSourceType(SOURCE_TYPE_USER_SHARE)
                .setSourceCaseId(coverAsset.getId())
                .setSourceLabel("User " + userId)
                .setTitle(reqVO.getTitle())
                .setPrompt(reqVO.getPrompt())
                .setPromptPreview(StrUtil.maxLength(reqVO.getPrompt(), 200))
                .setModelCode(reqVO.getModelCode())
                .setModelName(reqVO.getModelName())
                .setModelParams(reqVO.getModelParams())
                .setCoverFileId(coverFile == null ? null : coverFile.getFileId())
                .setStorageConfigId(coverFile == null ? null : coverFile.getStorageConfigId())
                .setStorageType(coverFile == null ? null : coverFile.getStorageType())
                .setBucket(coverFile == null ? null : coverFile.getBucket())
                .setObjectKey(coverFile == null ? null : coverFile.getObjectKey())
                .setFilePath(coverFile == null ? null : coverFile.getFilePath())
                .setPublicUrl(coverFile == null ? null : coverFile.getPublicUrl())
                .setWidth(firstNonNull(coverFile == null ? null : coverFile.getWidth(), coverAsset.getWidth()))
                .setHeight(firstNonNull(coverFile == null ? null : coverFile.getHeight(), coverAsset.getHeight()))
                .setMimeType(coverFile == null ? coverAsset.getMimeType() : coverFile.getMimeType())
                .setFileSize(coverFile == null ? coverAsset.getFileSize() : coverFile.getFileSize())
                .setAccessMode(coverFile == null ? AigcAssetAccessModeEnum.PRIVATE_SIGNED.getCode() : coverFile.getAccessMode())
                .setVisibility(normalizeVisibility(reqVO.getVisibility()))
                .setAuditStatus(resolveShareAuditStatus(reqVO.getVisibility()))
                .setStatus(AigcAssetStatusEnum.NORMAL.getCode())
                .setFeatured(false)
                .setSort(0)
                .setViewCount(0)
                .setCopyCount(0)
                .setUseCount(0);
        promptTemplateMapper.insert(template);
        return template.getId();
    }

    @Override
    public List<String> getCategoryList() {
        return promptTemplateMapper.selectCategoryList();
    }

    @Override
    public List<AigcPromptTemplateModelRespVO> getModelList() {
        return promptTemplateMapper.selectModelList();
    }

    @Override
    public void increaseViewCount(Long id) {
        validateTemplateAvailable(id);
        promptTemplateMapper.increaseViewCount(id);
    }

    @Override
    public void increaseCopyCount(Long id) {
        validateTemplateAvailable(id);
        promptTemplateMapper.increaseCopyCount(id);
    }

    @Override
    public void increaseUseCount(Long id) {
        validateTemplateAvailable(id);
        promptTemplateMapper.increaseUseCount(id);
    }

    private AigcPromptTemplateRespVO buildRespVO(AigcPromptTemplateDO template) {
        AccessUrl accessUrl = resolveImageUrl(template);
        return new AigcPromptTemplateRespVO()
                .setId(template.getId())
                .setTemplateNo(template.getTemplateNo())
                .setTitle(template.getTitle())
                .setDescription(template.getDescription())
                .setPrompt(template.getPrompt())
                .setPromptPreview(template.getPromptPreview())
                .setCategory(template.getCategory())
                .setModelCode(template.getModelCode())
                .setModelName(template.getModelName())
                .setModelParams(template.getModelParams())
                .setStyles(template.getStyles())
                .setScenes(template.getScenes())
                .setTags(template.getTags())
                .setImageUrl(accessUrl.url)
                .setImageUrlExpireTime(accessUrl.expireTime)
                .setPublicAccess(accessUrl.publicAccess)
                .setWidth(template.getWidth())
                .setHeight(template.getHeight())
                .setMimeType(template.getMimeType())
                .setFileSize(template.getFileSize())
                .setSourceLabel(template.getSourceLabel())
                .setSourceUrl(template.getSourceUrl())
                .setGithubUrl(template.getGithubUrl())
                .setFeatured(template.getFeatured())
                .setViewCount(template.getViewCount())
                .setCopyCount(template.getCopyCount())
                .setUseCount(template.getUseCount())
                .setCreateTime(template.getCreateTime());
    }

    private AccessUrl resolveImageUrl(AigcPromptTemplateDO template) {
        if (!Objects.equals(AigcAssetAccessModeEnum.PRIVATE_SIGNED.getCode(), template.getAccessMode())) {
            return new AccessUrl(template.getPublicUrl(), null, true);
        }
        if (template.getStorageConfigId() == null || StrUtil.isBlank(template.getFilePath())) {
            return new AccessUrl(template.getPublicUrl(), null, false);
        }
        try {
            FilePresignRespDTO presign = fileApi.presignGetUrlV2(template.getStorageConfigId(), template.getFilePath(),
                    AigcAssetAccessTypeEnum.PREVIEW.getExpireSeconds()).getCheckedData();
            if (presign == null || StrUtil.isBlank(presign.getUrl())) {
                return new AccessUrl(template.getPublicUrl(), null, false);
            }
            return new AccessUrl(presign.getUrl(), presign.getExpireTime(), presign.getPublicAccess());
        } catch (Exception ex) {
            log.warn("[resolveImageUrl][templateId({}) storageConfigId({}) filePath({}) presign failed]",
                    template.getId(), template.getStorageConfigId(), template.getFilePath(), ex);
            return new AccessUrl(template.getPublicUrl(), null, false);
        }
    }

    private String generateTemplateNo() {
        return "TPL" + LocalDateTime.now().format(TEMPLATE_NO_TIME_FORMATTER)
                + UUID.fastUUID().toString(true).substring(0, 8).toUpperCase();
    }

    private String normalizeVisibility(String visibility) {
        if (AigcAssetVisibilityEnum.PRIVATE.getCode().equals(visibility)) {
            return AigcAssetVisibilityEnum.PRIVATE.getCode();
        }
        return AigcAssetVisibilityEnum.PUBLIC.getCode();
    }

    private String resolveShareAuditStatus(String visibility) {
        return AigcAssetVisibilityEnum.PRIVATE.getCode().equals(visibility)
                ? AigcAssetAuditStatusEnum.PASS.getCode()
                : AigcAssetAuditStatusEnum.PENDING.getCode();
    }

    private <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    private record AccessUrl(String url, LocalDateTime expireTime, Boolean publicAccess) {
    }

}
