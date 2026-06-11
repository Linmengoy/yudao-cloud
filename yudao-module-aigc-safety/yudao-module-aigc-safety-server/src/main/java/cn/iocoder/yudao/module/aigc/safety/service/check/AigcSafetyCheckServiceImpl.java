package cn.iocoder.yudao.module.aigc.safety.service.check;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.aigc.safety.dal.dataobject.AigcSensitiveWordDO;
import cn.iocoder.yudao.module.aigc.safety.dto.AigcSafetyPromptCheckReqDTO;
import cn.iocoder.yudao.module.aigc.safety.dto.AigcSafetyPromptCheckRespDTO;
import cn.iocoder.yudao.module.aigc.safety.enums.AigcSensitiveWordMatchTypeEnum;
import cn.iocoder.yudao.module.aigc.safety.service.sensitiveword.AigcSensitiveWordService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Validated
public class AigcSafetyCheckServiceImpl implements AigcSafetyCheckService {

    @Resource
    private AigcSensitiveWordService sensitiveWordService;

    @Override
    public AigcSafetyPromptCheckRespDTO checkPrompt(AigcSafetyPromptCheckReqDTO reqDTO) {
        // 审核提示词是否违规(可以考虑用redis缓存敏感词)
        List<AigcSensitiveWordDO> sensitiveWords = sensitiveWordService.getEnabledSensitiveWords(reqDTO.getScene());
        List<String> hitWords = new ArrayList<>();
        Integer riskLevel = 0;
        for (AigcSensitiveWordDO sensitiveWord : sensitiveWords) {
            if (!match(reqDTO.getPrompt(), sensitiveWord)) {
                continue;
            }
            hitWords.add(sensitiveWord.getWord());
            if (sensitiveWord.getLevel() != null && sensitiveWord.getLevel() > riskLevel) {
                riskLevel = sensitiveWord.getLevel();
            }
        }
        if (hitWords.isEmpty()) {
            return new AigcSafetyPromptCheckRespDTO()
                    .setPass(true)
                    .setHitWords(Collections.emptyList())
                    .setRiskLevel(0);
        }
        return new AigcSafetyPromptCheckRespDTO()
                .setPass(false)
                .setHitWords(hitWords)
                .setRiskLevel(riskLevel)
                .setReason("提示词包含敏感内容");
    }

    private boolean match(String prompt, AigcSensitiveWordDO sensitiveWord) {
        String word = sensitiveWord.getWord();
        if (StrUtil.isBlank(prompt) || StrUtil.isBlank(word)) {
            return false;
        }
        if (AigcSensitiveWordMatchTypeEnum.EXACT.getCode().equals(sensitiveWord.getMatchType())) {
            return StrUtil.equalsIgnoreCase(prompt.trim(), word.trim());
        }
        if (!AigcSensitiveWordMatchTypeEnum.CONTAINS.getCode().equals(sensitiveWord.getMatchType())) {
            return false;
        }
        return StrUtil.containsIgnoreCase(prompt, word);
    }

}
