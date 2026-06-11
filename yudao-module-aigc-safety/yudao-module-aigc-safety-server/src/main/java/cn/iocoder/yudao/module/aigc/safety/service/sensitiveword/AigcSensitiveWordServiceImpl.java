package cn.iocoder.yudao.module.aigc.safety.service.sensitiveword;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.safety.controller.admin.sensitiveword.vo.AigcSensitiveWordPageReqVO;
import cn.iocoder.yudao.module.aigc.safety.controller.admin.sensitiveword.vo.AigcSensitiveWordSaveReqVO;
import cn.iocoder.yudao.module.aigc.safety.controller.admin.sensitiveword.vo.AigcSensitiveWordStatusReqVO;
import cn.iocoder.yudao.module.aigc.safety.dal.dataobject.AigcSensitiveWordDO;
import cn.iocoder.yudao.module.aigc.safety.dal.mysql.AigcSensitiveWordMapper;
import cn.iocoder.yudao.module.aigc.safety.dal.redis.AigcSensitiveWordRedisDAO;
import cn.iocoder.yudao.module.aigc.safety.enums.AigcSafetySceneEnum;
import cn.iocoder.yudao.module.aigc.safety.enums.AigcSensitiveWordMatchTypeEnum;
import cn.iocoder.yudao.module.aigc.safety.enums.AigcSensitiveWordStatusEnum;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.aigc.safety.dal.redis.RedisKeyConstants.SENSITIVE_WORD_ENABLED;
import static cn.iocoder.yudao.module.aigc.safety.enums.ErrorCodeConstants.*;

@Service
@Validated
public class AigcSensitiveWordServiceImpl implements AigcSensitiveWordService {

    @Resource
    private AigcSensitiveWordMapper sensitiveWordMapper;
    @Resource
    private AigcSensitiveWordRedisDAO sensitiveWordRedisDAO;

    @Override
    public Long createSensitiveWord(AigcSensitiveWordSaveReqVO reqVO) {
        normalizeAndValidate(reqVO);
        validateSensitiveWordDuplicate(null, reqVO.getWord(), reqVO.getScene());
        AigcSensitiveWordDO sensitiveWord = BeanUtils.toBean(reqVO, AigcSensitiveWordDO.class)
                .setMatchType(reqVO.getMatchType())
                .setStatus(reqVO.getStatus());
        sensitiveWordMapper.insert(sensitiveWord);
        sensitiveWordRedisDAO.delete(buildEnabledSensitiveWordKey(reqVO.getScene()));
        return sensitiveWord.getId();
    }

    @Override
    public void updateSensitiveWord(AigcSensitiveWordSaveReqVO reqVO) {
        validateSensitiveWordExists(reqVO.getId());
        normalizeAndValidate(reqVO);
        validateSensitiveWordDuplicate(reqVO.getId(), reqVO.getWord(), reqVO.getScene());
        AigcSensitiveWordDO oldSensitiveWord = getSensitiveWord(reqVO.getId());
        AigcSensitiveWordDO updateObj = BeanUtils.toBean(reqVO, AigcSensitiveWordDO.class);
        sensitiveWordMapper.updateById(updateObj);
        sensitiveWordRedisDAO.delete(buildEnabledSensitiveWordKey(oldSensitiveWord.getScene()));
        sensitiveWordRedisDAO.delete(buildEnabledSensitiveWordKey(reqVO.getScene()));
    }

    @Override
    public void deleteSensitiveWord(Long id) {
        AigcSensitiveWordDO sensitiveWord = validateSensitiveWordExists(id);
        sensitiveWordMapper.deleteById(id);
        sensitiveWordRedisDAO.delete(buildEnabledSensitiveWordKey(sensitiveWord.getScene()));
    }

    @Override
    public AigcSensitiveWordDO getSensitiveWord(Long id) {
        return sensitiveWordMapper.selectById(id);
    }

    @Override
    public AigcSensitiveWordDO validateSensitiveWordExists(Long id) {
        AigcSensitiveWordDO sensitiveWord = sensitiveWordMapper.selectById(id);
        if (sensitiveWord == null) {
            throw exception(SENSITIVE_WORD_NOT_EXISTS);
        }
        return sensitiveWord;
    }

    @Override
    public PageResult<AigcSensitiveWordDO> getSensitiveWordPage(AigcSensitiveWordPageReqVO reqVO) {
        return sensitiveWordMapper.selectPage(reqVO);
    }

    @Override
    public void updateSensitiveWordStatus(AigcSensitiveWordStatusReqVO reqVO) {
        AigcSensitiveWordDO sensitiveWord = validateSensitiveWordExists(reqVO.getId());
        if (!AigcSensitiveWordStatusEnum.ENABLE.getCode().equals(reqVO.getStatus())
                && !AigcSensitiveWordStatusEnum.DISABLE.getCode().equals(reqVO.getStatus())) {
            throw exception(SENSITIVE_WORD_STATUS_INVALID);
        }
        sensitiveWordMapper.updateById(new AigcSensitiveWordDO().setId(reqVO.getId()).setStatus(reqVO.getStatus()));
        sensitiveWordRedisDAO.delete(buildEnabledSensitiveWordKey(sensitiveWord.getScene()));
    }

    @Override
    public List<AigcSensitiveWordDO> getEnabledSensitiveWords(String scene) {
        String key = buildEnabledSensitiveWordKey(scene);
        List<AigcSensitiveWordDO> sensitiveWords = sensitiveWordRedisDAO.get(key);
        if (sensitiveWords != null) {
            return sensitiveWords;
        }
        sensitiveWords = sensitiveWordMapper.selectListBySceneAndStatus(scene, AigcSensitiveWordStatusEnum.ENABLE.getCode());
        sensitiveWordRedisDAO.set(key, sensitiveWords);
        return sensitiveWords;
    }

    private String buildEnabledSensitiveWordKey(String scene) {
        Long tenantId = TenantContextHolder.getTenantId() == null ? 0L : TenantContextHolder.getTenantId();
        return String.format(SENSITIVE_WORD_ENABLED, tenantId, scene);
    }

    private void validateSensitiveWordDuplicate(Long id, String word, String scene) {
        AigcSensitiveWordDO sensitiveWord = sensitiveWordMapper.selectByWordAndScene(word, scene);
        if (sensitiveWord == null) {
            return;
        }
        if (id == null || !sensitiveWord.getId().equals(id)) {
            throw exception(SENSITIVE_WORD_DUPLICATE);
        }
    }

    private void normalizeAndValidate(AigcSensitiveWordSaveReqVO reqVO) {
        reqVO.setMatchType(StrUtil.blankToDefault(reqVO.getMatchType(), AigcSensitiveWordMatchTypeEnum.CONTAINS.getCode()));
        reqVO.setStatus(StrUtil.blankToDefault(reqVO.getStatus(), AigcSensitiveWordStatusEnum.ENABLE.getCode()));
        validateScene(reqVO.getScene());
        validateMatchType(reqVO.getMatchType());
        validateStatus(reqVO.getStatus());
    }

    private void validateScene(String scene) {
        for (AigcSafetySceneEnum sceneEnum : AigcSafetySceneEnum.values()) {
            if (sceneEnum.getCode().equals(scene)) {
                return;
            }
        }
        throw exception(SENSITIVE_WORD_SCENE_INVALID);
    }

    private void validateMatchType(String matchType) {
        if (AigcSensitiveWordMatchTypeEnum.CONTAINS.getCode().equals(matchType)
                || AigcSensitiveWordMatchTypeEnum.EXACT.getCode().equals(matchType)) {
            return;
        }
        throw exception(SENSITIVE_WORD_MATCH_TYPE_INVALID);
    }

    private void validateStatus(String status) {
        if (AigcSensitiveWordStatusEnum.ENABLE.getCode().equals(status)
                || AigcSensitiveWordStatusEnum.DISABLE.getCode().equals(status)) {
            return;
        }
        throw exception(SENSITIVE_WORD_STATUS_INVALID);
    }

}
