package cn.iocoder.yudao.module.aigc.asset.service.template;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.aigc.asset.controller.app.vo.template.AigcPromptTemplatePageReqVO;
import cn.iocoder.yudao.module.aigc.asset.controller.app.vo.template.AigcPromptTemplateRespVO;
import cn.iocoder.yudao.module.aigc.asset.dal.dataobject.AigcPromptTemplateDO;
import cn.iocoder.yudao.module.aigc.asset.dal.mysql.AigcPromptTemplateMapper;
import cn.iocoder.yudao.module.aigc.asset.enums.AigcAssetAccessModeEnum;
import cn.iocoder.yudao.module.aigc.asset.enums.AigcAssetAccessTypeEnum;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import cn.iocoder.yudao.module.infra.api.file.dto.FilePresignRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.aigc.asset.enums.ErrorCodeConstants.PROMPT_TEMPLATE_NOT_EXISTS;
import static cn.iocoder.yudao.module.aigc.asset.enums.ErrorCodeConstants.PROMPT_TEMPLATE_STATUS_INVALID;

@Service
@Validated
public class AigcPromptTemplateServiceImpl implements AigcPromptTemplateService {

    @Resource
    private AigcPromptTemplateMapper promptTemplateMapper;
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
    public List<String> getCategoryList() {
        return promptTemplateMapper.selectCategoryList();
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
        FilePresignRespDTO presign = fileApi.presignGetUrlV2(template.getStorageConfigId(), template.getFilePath(),
                AigcAssetAccessTypeEnum.PREVIEW.getExpireSeconds()).getCheckedData();
        return new AccessUrl(presign.getUrl(), presign.getExpireTime(), presign.getPublicAccess());
    }

    private record AccessUrl(String url, LocalDateTime expireTime, Boolean publicAccess) {
    }

}
