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
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

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
            Long caseId = caseJson.getLong("id");
            AigcPromptTemplateDO exists = promptTemplateMapper.selectBySource(SOURCE_REPO, caseId);
            if (exists != null) {
                skipCount++;
                continue;
            }
            Path imagePath = resolveImagePath(imageDirPath, caseJson.getStr("image"));
            if (imagePath == null || !Files.isRegularFile(imagePath)) {
                skipCount++;
                log.warn("[importAwesomeGptImageCases][caseId({}) 图片不存在，跳过]", caseId);
                continue;
            }
            AigcPromptTemplateDO template = buildTemplate(caseJson, imagePath, reqVO.getStorageDirectory());
            promptTemplateMapper.insert(template);
            createCount++;
        }
        return new AigcPromptTemplateImportRespVO()
                .setTotalCount(cases.size())
                .setCreateCount(createCount)
                .setUpdateCount(updateCount)
                .setSkipCount(skipCount);
    }

    @Override
    public AigcPromptTemplateImportRespVO importAwesomeGptImageCaseFiles(MultipartFile casesJson, MultipartFile[] images,
                                                                        String storageDirectory) {
        if (casesJson == null || casesJson.isEmpty()) {
            throw invalidParamException("cases.json 文件不能为空");
        }
        if (images == null || images.length == 0) {
            throw invalidParamException("图片文件不能为空");
        }
        Path tempDir = createTempDir();
        try {
            Path casesJsonPath = tempDir.resolve("cases.json");
            Path imageDirPath = tempDir.resolve("images");
            Files.createDirectories(imageDirPath);
            transferMultipartFile(casesJson, casesJsonPath);
            for (MultipartFile image : images) {
                if (image == null || image.isEmpty()) {
                    continue;
                }
                String fileName = Path.of(image.getOriginalFilename() == null ? image.getName() : image.getOriginalFilename())
                        .getFileName().toString();
                transferMultipartFile(image, imageDirPath.resolve(fileName));
            }
            AigcPromptTemplateImportReqVO reqVO = new AigcPromptTemplateImportReqVO();
            reqVO.setCasesJsonPath(casesJsonPath.toString());
            reqVO.setImageDirPath(imageDirPath.toString());
            reqVO.setStorageDirectory(storageDirectory);
            return importAwesomeGptImageCases(reqVO);
        } catch (IOException ex) {
            throw invalidParamException("保存上传文件失败：{}", ex.getMessage());
        } finally {
            deleteTempDir(tempDir);
        }
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

    private Path createTempDir() {
        try {
            return Files.createTempDirectory("aigc-prompt-template-");
        } catch (IOException ex) {
            throw invalidParamException("创建临时目录失败：{}", ex.getMessage());
        }
    }

    private void transferMultipartFile(MultipartFile file, Path targetPath) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             OutputStream outputStream = Files.newOutputStream(targetPath)) {
            inputStream.transferTo(outputStream);
        }
    }

    private void deleteTempDir(Path tempDir) {
        if (tempDir == null || !Files.exists(tempDir)) {
            return;
        }
        try {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ex) {
                            log.warn("[deleteTempDir][path({}) 删除失败]", path, ex);
                        }
                    });
        } catch (IOException ex) {
            log.warn("[deleteTempDir][tempDir({}) 遍历失败]", tempDir, ex);
        }
    }

    private record ImageSize(Integer width, Integer height) {
    }

}
