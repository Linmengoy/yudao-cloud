package cn.iocoder.yudao.module.aigc.model.service.param;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.model.controller.admin.param.vo.AigcModelParamTemplateSaveReqVO;
import cn.iocoder.yudao.module.aigc.model.dal.dataobject.AigcModelParamTemplateDO;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelMapper;
import cn.iocoder.yudao.module.aigc.model.dal.mysql.AigcModelParamTemplateMapper;
import cn.iocoder.yudao.module.aigc.model.enums.AigcModelParamTypeEnum;
import cn.iocoder.yudao.module.aigc.model.util.AigcModelParamUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.aigc.model.enums.ErrorCodeConstants.*;

@Service
@Validated
public class AigcModelParamServiceImpl implements AigcModelParamService {

    @Resource
    private AigcModelParamTemplateMapper paramTemplateMapper;

    @Resource
    private AigcModelMapper modelMapper;

    @Override
    public Long createParamTemplate(AigcModelParamTemplateSaveReqVO reqVO) {
        validateModelExists(reqVO.getModelId());
        validateParamKeyUnique(null, reqVO.getModelId(), reqVO.getCapability(), reqVO.getParamKey());

        AigcModelParamTemplateDO template = BeanUtils.toBean(reqVO, AigcModelParamTemplateDO.class);
        template.setOptions(normalizeOptions(reqVO.getOptions()));
        paramTemplateMapper.insert(template);
        return template.getId();
    }

    @Override
    public void updateParamTemplate(AigcModelParamTemplateSaveReqVO reqVO) {
        validateParamTemplateExists(reqVO.getId());
        AigcModelParamTemplateDO existing = paramTemplateMapper.selectById(reqVO.getId());
        validateParamKeyUnique(reqVO.getId(), existing.getModelId(), existing.getCapability(), reqVO.getParamKey());

        AigcModelParamTemplateDO updateObj = BeanUtils.toBean(reqVO, AigcModelParamTemplateDO.class);
        updateObj.setOptions(normalizeOptions(reqVO.getOptions()));
        paramTemplateMapper.updateById(updateObj);
    }

    @Override
    public void deleteParamTemplate(Long id) {
        validateParamTemplateExists(id);
        paramTemplateMapper.deleteById(id);
    }

    @Override
    public AigcModelParamTemplateDO getParamTemplate(Long id) {
        return paramTemplateMapper.selectById(id);
    }

    @Override
    public List<AigcModelParamTemplateDO> getParamTemplateList(Long modelId, String capability) {
        return paramTemplateMapper.selectListByModelIdAndCapability(modelId, capability);
    }

    @Override
    public void validateParams(Long modelId, String capability, Map<String, Object> params) {
        List<AigcModelParamTemplateDO> templates = paramTemplateMapper.selectListByModelIdAndCapability(modelId, capability);

        for (AigcModelParamTemplateDO template : templates) {
            String paramKey = template.getParamKey();
            Object value = params.get(paramKey);

            if (Boolean.TRUE.equals(template.getRequiredStatus()) && ObjectUtil.isNull(value)) {
                throw exception(MODEL_PARAM_REQUIRED, template.getParamName());
            }

            if (ObjectUtil.isNotNull(value)) {
                validateParamValue(template, value);
            }
        }
    }

    private void validateParamValue(AigcModelParamTemplateDO template, Object value) {
        AigcModelParamTypeEnum type = AigcModelParamTypeEnum.getByValue(template.getParamType());
        if (type == null) {
            return;
        }

        String strValue = String.valueOf(value);

        switch (type) {
            case NUMBER:
                if (!NumberUtil.isNumber(strValue)) {
                    throw exception(MODEL_PARAM_TYPE_ERROR, template.getParamName(), "数字");
                }
                BigDecimal numValue = new BigDecimal(strValue);
                if (template.getMinValue() != null && numValue.compareTo(template.getMinValue()) < 0) {
                    throw exception(MODEL_PARAM_RANGE_ERROR, template.getParamName(), template.getMinValue(), template.getMaxValue());
                }
                if (template.getMaxValue() != null && numValue.compareTo(template.getMaxValue()) > 0) {
                    throw exception(MODEL_PARAM_RANGE_ERROR, template.getParamName(), template.getMinValue(), template.getMaxValue());
                }
                break;
            case BOOLEAN:
                if (!"true".equalsIgnoreCase(strValue) && !"false".equalsIgnoreCase(strValue)) {
                    throw exception(MODEL_PARAM_TYPE_ERROR, template.getParamName(), "布尔值");
                }
                break;
            case SELECT:
                if (template.getOptions() != null) {
                    List<String> options = JSONUtil.toList(template.getOptions(), String.class);
                    if (!options.contains(strValue)) {
                        throw exception(MODEL_PARAM_OPTION_ERROR, template.getParamName());
                    }
                }
                break;
            case STRING:
                if (template.getRegexPattern() != null && !ReUtil.isMatch(template.getRegexPattern(), strValue)) {
                    throw exception(MODEL_PARAM_FORMAT_ERROR, template.getParamName());
                }
                break;
        }
    }

    private String normalizeOptions(String options) {
        if (StrUtil.isBlank(options)) {
            return null;
        }
        List<String> optionList;
        if (JSONUtil.isTypeJSONArray(options)) {
            optionList = JSONUtil.toList(options, String.class);
        } else {
            optionList = Arrays.stream(options.split(","))
                    .map(StrUtil::trim)
                    .filter(StrUtil::isNotBlank)
                    .toList();
        }
        optionList = optionList.stream()
                .map(AigcModelParamUtils::decodeOptionValue)
                .filter(StrUtil::isNotBlank)
                .toList();
        return CollUtil.isEmpty(optionList) ? null : JSONUtil.toJsonStr(optionList);
    }

    private void validateModelExists(Long modelId) {
        if (modelMapper.selectById(modelId) == null) {
            throw exception(MODEL_NOT_EXISTS);
        }
    }

    private void validateParamTemplateExists(Long id) {
        if (paramTemplateMapper.selectById(id) == null) {
            throw exception(MODEL_PARAM_TEMPLATE_NOT_EXISTS);
        }
    }

    private void validateParamKeyUnique(Long id, Long modelId, String capability, String paramKey) {
        AigcModelParamTemplateDO template = paramTemplateMapper.selectByModelIdAndCapabilityAndParamKey(modelId, capability, paramKey);
        if (template == null) {
            return;
        }
        if (!ObjectUtil.equal(template.getId(), id)) {
            throw exception(MODEL_PARAM_KEY_DUPLICATE);
        }
    }

}
