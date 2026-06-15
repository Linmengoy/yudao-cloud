package cn.iocoder.yudao.module.aigc.asset.service.template;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.module.aigc.asset.controller.admin.vo.AigcPromptTemplateImportReqVO;
import cn.iocoder.yudao.module.aigc.asset.controller.admin.vo.AigcPromptTemplateImportRespVO;
import cn.iocoder.yudao.module.aigc.asset.dal.dataobject.AigcPromptTemplateDO;
import cn.iocoder.yudao.module.aigc.asset.dal.mysql.AigcPromptTemplateMapper;
import cn.iocoder.yudao.module.aigc.asset.enums.AigcAssetAccessModeEnum;
import cn.iocoder.yudao.module.aigc.asset.enums.AigcAssetAuditStatusEnum;
import cn.iocoder.yudao.module.aigc.asset.enums.AigcAssetSourceTypeEnum;
import cn.iocoder.yudao.module.aigc.asset.enums.AigcAssetStatusEnum;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import cn.iocoder.yudao.module.infra.api.file.dto.FileCreateRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
@Validated
@Slf4j
public class AigcPromptTemplateImportServiceImpl implements AigcPromptTemplateImportService {

    private static final String SOURCE_REPO = "https://github.com/freestylefly/awesome-gpt-image-2";

    @Resource
    private AigcPromptTemplateMapper promptTemplateMapper;
    @Resource
    private FileApi fileApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AigcPromptTemplateImportRespVO importAwesomeGptImageCases(AigcPromptTemplateImportReqVO reqVO) {
        Path casesJsonPath = Path.of(reqVO.getCasesJsonPath());
        Path imageDirPath = Path.of(reqVO.getImageDirPath());
        if (!Files.isRegularFile(casesJsonPath)) {
            throw invalidParamException("cases.json 文件不存在：{}", casesJsonPath);
        }
        if (!Files.isDirectory(imageDirPath)) {
            throw invalidParamException("图片目录不存在：{}", imageDirPath);
        }
        JSONObject root = JSONUtil.parseObj(readUtf8(casesJsonPath));
        JSONArray cases = root.getJSONArray("cases");
        if (cases == null) {
            throw invalidParamException("cases.json 缺少 cases 数组");
        }
        int createCount = 0;
        int updateCount = 0;
        int skipCount = 0;
        for (Object item : cases) {
            JSONObject caseJson = (JSONObject) item;
            Path imagePath = resolveImagePath(imageDirPath, caseJson.getStr("image"));
            if (imagePath == null || !Files.isRegularFile(imagePath)) {
                skipCount++;
                log.warn("[importAwesomeGptImageCases][caseId({}) 图片不存在，跳过]", caseJson.getLong("id"));
                continue;
            }
            AigcPromptTemplateDO template = buildTemplate(caseJson, imagePath, reqVO.getStorageDirectory());
            AigcPromptTemplateDO exists = promptTemplateMapper.selectBySource(SOURCE_REPO, template.getSourceCaseId());
            if (exists == null) {
                promptTemplateMapper.insert(template);
                createCount++;
            } else {
                template.setId(exists.getId());
                promptTemplateMapper.updateById(template);
                updateCount++;
            }
        }
        return new AigcPromptTemplateImportRespVO()
                .setTotalCount(cases.size())
                .setCreateCount(createCount)
                .setUpdateCount(updateCount)
                .setSkipCount(skipCount);
    }

    private AigcPromptTemplateDO buildTemplate(JSONObject caseJson, Path imagePath, String storageDirectory) {
        FileCreateRespDTO file = uploadImage(caseJson, imagePath, storageDirectory);
        ImageSize imageSize = readImageSize(imagePath);
        boolean publicAccess = BooleanUtil.isTrue(file.getPublicAccess());
        Long caseId = caseJson.getLong("id");
        return new AigcPromptTemplateDO()
                .setTemplateNo("TPL-AWESOME-GPT-IMAGE-" + caseId)
                .setSourceType(AigcAssetSourceTypeEnum.IMPORT.getCode())
                .setSourceCaseId(caseId)
                .setSourceRepo(SOURCE_REPO)
                .setSourceLabel(caseJson.getStr("sourceLabel"))
                .setSourceUrl(caseJson.getStr("sourceUrl"))
                .setGithubUrl(caseJson.getStr("githubUrl"))
                .setTitle(caseJson.getStr("title"))
                .setDescription(caseJson.getStr("imageAlt"))
                .setPrompt(caseJson.getStr("prompt"))
                .setPromptPreview(caseJson.getStr("promptPreview"))
                .setCategory(caseJson.getStr("category"))
                .setStyles(JSONUtil.toJsonStr(caseJson.getJSONArray("styles")))
                .setScenes(JSONUtil.toJsonStr(caseJson.getJSONArray("scenes")))
                .setTags(buildTags(caseJson))
                .setCoverFileId(file.getId())
                .setStorageConfigId(file.getConfigId())
                .setStorageType(file.getStorageType())
                .setBucket(file.getBucket())
                .setObjectKey(StrUtil.blankToDefault(file.getObjectKey(), file.getPath()))
                .setFilePath(file.getPath())
                .setPublicUrl(publicAccess ? file.getUrl() : null)
                .setWidth(imageSize.width())
                .setHeight(imageSize.height())
                .setMimeType(file.getType())
                .setFileSize(file.getSize())
                .setAccessMode(publicAccess ? AigcAssetAccessModeEnum.PUBLIC.getCode()
                        : AigcAssetAccessModeEnum.PRIVATE_SIGNED.getCode())
                .setFeatured(BooleanUtil.isTrue(caseJson.getBool("featured")))
                .setSort(caseId == null ? 0 : Math.toIntExact(caseId))
                .setVisibility("PUBLIC")
                .setAuditStatus(AigcAssetAuditStatusEnum.PASS.getCode())
                .setStatus(AigcAssetStatusEnum.NORMAL.getCode())
                .setViewCount(0)
                .setCopyCount(0)
                .setUseCount(0);
    }

    private FileCreateRespDTO uploadImage(JSONObject caseJson, Path imagePath, String storageDirectory) {
        try {
            String fileName = imagePath.getFileName().toString();
            String mimeType = Files.probeContentType(imagePath);
            if (StrUtil.isBlank(mimeType)) {
                mimeType = fileName.endsWith(".png") ? "image/png" : "image/jpeg";
            }
            byte[] content = Files.readAllBytes(imagePath);
            return fileApi.createFileV2(content, fileName, StrUtil.blankToDefault(storageDirectory, "aigc/templates"),
                    mimeType);
        } catch (IOException ex) {
            throw invalidParamException("读取模板图片失败：{}", imagePath);
        }
    }

    private Path resolveImagePath(Path imageDirPath, String image) {
        if (StrUtil.isBlank(image)) {
            return null;
        }
        String fileName = image;
        int slashIndex = image.lastIndexOf('/');
        if (slashIndex >= 0) {
            fileName = image.substring(slashIndex + 1);
        }
        return imageDirPath.resolve(fileName);
    }

    private String readUtf8(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw invalidParamException("读取 UTF-8 JSON 文件失败：{}", path);
        }
    }

    private ImageSize readImageSize(Path imagePath) {
        try {
            BufferedImage image = ImageIO.read(imagePath.toFile());
            return image == null ? new ImageSize(null, null) : new ImageSize(image.getWidth(), image.getHeight());
        } catch (IOException ex) {
            return new ImageSize(null, null);
        }
    }

    private String buildTags(JSONObject caseJson) {
        StringBuilder tags = new StringBuilder();
        appendTag(tags, caseJson.getStr("category"));
        JSONArray styles = caseJson.getJSONArray("styles");
        if (styles != null) {
            styles.forEach(style -> appendTag(tags, String.valueOf(style)));
        }
        JSONArray scenes = caseJson.getJSONArray("scenes");
        if (scenes != null) {
            scenes.forEach(scene -> appendTag(tags, String.valueOf(scene)));
        }
        return tags.toString();
    }

    private void appendTag(StringBuilder tags, String tag) {
        if (StrUtil.isBlank(tag)) {
            return;
        }
        if (!tags.isEmpty()) {
            tags.append(',');
        }
        tags.append(tag.trim());
    }

    private record ImageSize(Integer width, Integer height) {
    }

}
