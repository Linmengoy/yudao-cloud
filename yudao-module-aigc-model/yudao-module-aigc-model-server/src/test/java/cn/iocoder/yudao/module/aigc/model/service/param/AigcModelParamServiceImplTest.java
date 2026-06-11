package cn.iocoder.yudao.module.aigc.model.service.param;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.aigc.model.controller.admin.param.vo.AigcModelParamTemplateCopyReqVO;
import cn.iocoder.yudao.module.aigc.model.controller.admin.param.vo.AigcModelParamTemplateCopyRespVO;
import cn.iocoder.yudao.module.aigc.model.controller.admin.param.vo.AigcModelParamTemplateSaveReqVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelDO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelParamTemplateDO;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelMapper;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelParamTemplateMapper;
import cn.iocoder.yudao.module.aigc.model.enums.AigcModelCapabilityEnum;
import cn.iocoder.yudao.module.aigc.model.enums.AigcModelParamTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomPojo;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomString;
import static cn.iocoder.yudao.module.aigc.model.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Import(AigcModelParamServiceImpl.class)
public class AigcModelParamServiceImplTest extends BaseDbUnitTest {

    @Resource
    private AigcModelParamServiceImpl paramService;

    @Resource
    private AigcModelMapper modelMapper;
    @Resource
    private AigcModelParamTemplateMapper paramTemplateMapper;

    @Test
    public void testCreateParamTemplate_keyDuplicate() {
        AigcModelDO model = createModel();
        paramTemplateMapper.insert(createTemplate(model.getId(), "ratio", AigcModelParamTypeEnum.SELECT.getCode(), o -> {
        }));
        AigcModelParamTemplateSaveReqVO reqVO = new AigcModelParamTemplateSaveReqVO()
                .setModelId(model.getId()).setCapability(AigcModelCapabilityEnum.TEXT_TO_IMAGE.getCode())
                .setParamKey("ratio").setParamName("比例").setParamType(AigcModelParamTypeEnum.SELECT.getCode());

        assertServiceException(() -> paramService.createParamTemplate(reqVO), MODEL_PARAM_KEY_DUPLICATE);
    }

    @Test
    public void testCreateParamTemplate_normalizeOptions() {
        AigcModelDO model = createModel();
        AigcModelParamTemplateSaveReqVO reqVO = new AigcModelParamTemplateSaveReqVO()
                .setModelId(model.getId()).setCapability(AigcModelCapabilityEnum.TEXT_TO_IMAGE.getCode())
                .setParamKey("ratio").setParamName("比例").setParamType(AigcModelParamTypeEnum.SELECT.getCode())
                .setOptions("[\"\\\"1:1\\\"\",\"\\\"2:3\\\"\"]");

        Long id = paramService.createParamTemplate(reqVO);

        assertEquals("[\"1:1\",\"2:3\"]", paramTemplateMapper.selectById(id).getOptions());
    }

    @Test
    public void testCopyParamTemplates_skipExisting() {
        AigcModelDO sourceModel = createModel();
        AigcModelDO targetModel = createModel();
        paramTemplateMapper.insert(createTemplate(sourceModel.getId(), "duration", AigcModelParamTypeEnum.NUMBER.getCode(), o -> o
                .setCapability(AigcModelCapabilityEnum.TEXT_TO_VIDEO.getCode()).setDefaultValue("5")));
        paramTemplateMapper.insert(createTemplate(targetModel.getId(), "duration", AigcModelParamTypeEnum.NUMBER.getCode(), o -> o
                .setCapability(AigcModelCapabilityEnum.TEXT_TO_VIDEO.getCode()).setDefaultValue("10")));

        AigcModelParamTemplateCopyRespVO result = paramService.copyParamTemplates(new AigcModelParamTemplateCopyReqVO()
                .setSourceModelId(sourceModel.getId())
                .setTargetModelIds(List.of(targetModel.getId()))
                .setCapabilities(List.of(AigcModelCapabilityEnum.TEXT_TO_VIDEO.getCode()))
                .setOverwrite(false));

        assertEquals(0, result.getCreatedCount());
        assertEquals(0, result.getUpdatedCount());
        assertEquals(1, result.getSkippedCount());
        AigcModelParamTemplateDO targetTemplate = paramTemplateMapper.selectByModelIdAndCapabilityAndParamKey(targetModel.getId(),
                AigcModelCapabilityEnum.TEXT_TO_VIDEO.getCode(), "duration");
        assertEquals("10", targetTemplate.getDefaultValue());
    }

    @Test
    public void testCopyParamTemplates_overwriteExistingAndCreateMissing() {
        AigcModelDO sourceModel = createModel();
        AigcModelDO targetModel = createModel();
        paramTemplateMapper.insert(createTemplate(sourceModel.getId(), "duration", AigcModelParamTypeEnum.NUMBER.getCode(), o -> o
                .setCapability(AigcModelCapabilityEnum.TEXT_TO_VIDEO.getCode()).setDefaultValue("5")));
        paramTemplateMapper.insert(createTemplate(sourceModel.getId(), "ratio", AigcModelParamTypeEnum.SELECT.getCode(), o -> o
                .setCapability(AigcModelCapabilityEnum.TEXT_TO_VIDEO.getCode()).setOptions("[\"1:1\",\"16:9\"]")));
        paramTemplateMapper.insert(createTemplate(targetModel.getId(), "duration", AigcModelParamTypeEnum.NUMBER.getCode(), o -> o
                .setCapability(AigcModelCapabilityEnum.TEXT_TO_VIDEO.getCode()).setDefaultValue("10")));

        AigcModelParamTemplateCopyRespVO result = paramService.copyParamTemplates(new AigcModelParamTemplateCopyReqVO()
                .setSourceModelId(sourceModel.getId())
                .setTargetModelIds(List.of(targetModel.getId()))
                .setCapabilities(List.of(AigcModelCapabilityEnum.TEXT_TO_VIDEO.getCode()))
                .setOverwrite(true));

        assertEquals(1, result.getCreatedCount());
        assertEquals(1, result.getUpdatedCount());
        assertEquals(0, result.getSkippedCount());
        AigcModelParamTemplateDO duration = paramTemplateMapper.selectByModelIdAndCapabilityAndParamKey(targetModel.getId(),
                AigcModelCapabilityEnum.TEXT_TO_VIDEO.getCode(), "duration");
        AigcModelParamTemplateDO ratio = paramTemplateMapper.selectByModelIdAndCapabilityAndParamKey(targetModel.getId(),
                AigcModelCapabilityEnum.TEXT_TO_VIDEO.getCode(), "ratio");
        assertEquals("5", duration.getDefaultValue());
        assertEquals("[\"1:1\",\"16:9\"]", ratio.getOptions());
    }

    @Test
    public void testValidateParams_requiredMissing() {
        AigcModelDO model = createModel();
        paramTemplateMapper.insert(createTemplate(model.getId(), "prompt", AigcModelParamTypeEnum.STRING.getCode(), o -> o
                .setParamName("提示词").setRequiredStatus(true)));

        assertServiceException(() -> paramService.validateParams(model.getId(), AigcModelCapabilityEnum.TEXT_TO_IMAGE.getCode(), Map.of()),
                MODEL_PARAM_REQUIRED, "提示词");
    }

    @Test
    public void testValidateParams_numberRangeError() {
        AigcModelDO model = createModel();
        paramTemplateMapper.insert(createTemplate(model.getId(), "duration", AigcModelParamTypeEnum.NUMBER.getCode(), o -> o
                .setParamName("时长").setMinValue(new BigDecimal("5")).setMaxValue(new BigDecimal("10"))));

        assertServiceException(() -> paramService.validateParams(model.getId(), AigcModelCapabilityEnum.TEXT_TO_IMAGE.getCode(), Map.of("duration", 12)),
                MODEL_PARAM_RANGE_ERROR, "时长", new BigDecimal("5.000000"), new BigDecimal("10.000000"));
    }

    @Test
    public void testValidateParams_selectOptionError() {
        AigcModelDO model = createModel();
        paramTemplateMapper.insert(createTemplate(model.getId(), "ratio", AigcModelParamTypeEnum.SELECT.getCode(), o -> o
                .setParamName("比例").setOptions("[\"1:1\",\"16:9\"]")));

        assertServiceException(() -> paramService.validateParams(model.getId(), AigcModelCapabilityEnum.TEXT_TO_IMAGE.getCode(), Map.of("ratio", "9:16")),
                MODEL_PARAM_OPTION_ERROR, "比例");
    }

    @Test
    public void testValidateParams_stringFormatError() {
        AigcModelDO model = createModel();
        paramTemplateMapper.insert(createTemplate(model.getId(), "seed", AigcModelParamTypeEnum.STRING.getCode(), o -> o
                .setParamName("随机种子").setRegexPattern("^[0-9]+$")));

        assertServiceException(() -> paramService.validateParams(model.getId(), AigcModelCapabilityEnum.TEXT_TO_IMAGE.getCode(), Map.of("seed", "abc")),
                MODEL_PARAM_FORMAT_ERROR, "随机种子");
    }

    @Test
    public void testValidateParams_success() {
        AigcModelDO model = createModel();
        paramTemplateMapper.insert(createTemplate(model.getId(), "ratio", AigcModelParamTypeEnum.SELECT.getCode(), o -> o
                .setOptions("[\"1:1\",\"16:9\"]").setRequiredStatus(true)));
        paramTemplateMapper.insert(createTemplate(model.getId(), "private", AigcModelParamTypeEnum.BOOLEAN.getCode(), o -> {
        }));

        paramService.validateParams(model.getId(), AigcModelCapabilityEnum.TEXT_TO_IMAGE.getCode(),
                Map.of("ratio", "1:1", "private", true));
    }

    @Test
    public void testValidateParams_selectEscapedOptionsSuccess() {
        AigcModelDO model = createModel();
        paramTemplateMapper.insert(createTemplate(model.getId(), "ratio", AigcModelParamTypeEnum.SELECT.getCode(), o -> o
                .setOptions("[\"\\\"1:1\\\"\",\"\\\"16:9\\\"\"]").setRequiredStatus(true)));

        paramService.validateParams(model.getId(), AigcModelCapabilityEnum.TEXT_TO_IMAGE.getCode(),
                Map.of("ratio", "1:1"));
    }

    private AigcModelDO createModel() {
        AigcModelDO model = randomPojo(AigcModelDO.class)
                .setCode(randomString()).setStatus(1);
        modelMapper.insert(model);
        return model;
    }

    private AigcModelParamTemplateDO createTemplate(Long modelId, String paramKey, String paramType,
                                                    java.util.function.Consumer<AigcModelParamTemplateDO> consumer) {
        AigcModelParamTemplateDO template = new AigcModelParamTemplateDO()
                .setModelId(modelId).setCapability(AigcModelCapabilityEnum.TEXT_TO_IMAGE.getCode())
                .setParamKey(paramKey).setParamName(paramKey).setParamType(paramType)
                .setRequiredStatus(false).setSort(1).setStatus(CommonStatusEnum.ENABLE.getStatus());
        consumer.accept(template);
        return template;
    }

}
