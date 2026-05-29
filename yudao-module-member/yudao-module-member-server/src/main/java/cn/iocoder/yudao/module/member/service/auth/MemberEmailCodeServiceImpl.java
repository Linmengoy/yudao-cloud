package cn.iocoder.yudao.module.member.service.auth;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.member.dal.dataobject.auth.MemberEmailCodeDO;
import cn.iocoder.yudao.module.member.dal.dataobject.user.MemberUserDO;
import cn.iocoder.yudao.module.member.dal.mysql.auth.MemberEmailCodeMapper;
import cn.iocoder.yudao.module.member.enums.auth.MemberEmailCodeSceneEnum;
import cn.iocoder.yudao.module.member.service.user.MemberUserService;
import cn.iocoder.yudao.module.system.api.mail.MailSendApi;
import cn.iocoder.yudao.module.system.api.mail.dto.MailSendSingleToUserReqDTO;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.member.enums.ErrorCodeConstants.*;

@Service
public class MemberEmailCodeServiceImpl implements MemberEmailCodeService {

    private static final int CODE_LENGTH = 6;
    private static final int EXPIRE_MINUTES = 10;
    private static final int SEND_INTERVAL_SECONDS = 60;
    private static final int EMAIL_DAILY_LIMIT = 10;
    private static final int IP_HOURLY_LIMIT = 30;

    @Resource
    private MemberEmailCodeMapper emailCodeMapper;
    @Resource
    private MemberUserService userService;
    @Resource
    private MailSendApi mailSendApi;

    @Value("${yudao.member.email-code.product-name:AIGC 平台}")
    private String productName;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendEmailCode(String email, String scene, String createIp) {
        MemberEmailCodeSceneEnum sceneEnum = validateScene(scene);
        validateEmailByScene(email, sceneEnum);
        validateSendFrequency(email, scene, createIp);

        String code = RandomUtil.randomNumbers(CODE_LENGTH);
        LocalDateTime now = LocalDateTime.now();
        Long todayCount = emailCodeMapper.selectCountByEmailAndSceneToday(email, scene,
                LocalDateTimeUtil.beginOfDay(now));
        emailCodeMapper.insert(MemberEmailCodeDO.builder()
                .email(email)
                .code(code)
                .scene(scene)
                .used(false)
                .createIp(createIp)
                .todayIndex(todayCount.intValue() + 1)
                .expiresTime(now.plusMinutes(EXPIRE_MINUTES))
                .build());

        MailSendSingleToUserReqDTO reqDTO = new MailSendSingleToUserReqDTO();
        reqDTO.setToMails(Collections.singletonList(email));
        reqDTO.setTemplateCode(sceneEnum.getTemplateCode());
        reqDTO.setTemplateParams(Map.of("code", code, "expireMinutes", EXPIRE_MINUTES, "productName", productName));
        mailSendApi.sendSingleMailToMember(reqDTO).checkError();
    }

    @Override
    public void validateEmailCode(String email, String scene, String code) {
        validateCode(email, scene, code);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void useEmailCode(String email, String scene, String code, String usedIp) {
        MemberEmailCodeDO emailCode = validateCode(email, scene, code);
        int updateCount = emailCodeMapper.updateUsedById(emailCode.getId(), usedIp);
        if (updateCount == 0) {
            throw exception(AUTH_EMAIL_CODE_USED);
        }
    }

    private MemberEmailCodeSceneEnum validateScene(String scene) {
        MemberEmailCodeSceneEnum sceneEnum = MemberEmailCodeSceneEnum.getByScene(scene);
        if (sceneEnum == null) {
            throw exception(AUTH_EMAIL_SCENE_NOT_SUPPORT);
        }
        return sceneEnum;
    }

    private void validateEmailByScene(String email, MemberEmailCodeSceneEnum sceneEnum) {
        MemberUserDO user = userService.getUserByEmail(email);
        if (Objects.equals(sceneEnum, MemberEmailCodeSceneEnum.REGISTER)
                || Objects.equals(sceneEnum, MemberEmailCodeSceneEnum.BIND_EMAIL)
                || Objects.equals(sceneEnum, MemberEmailCodeSceneEnum.CHANGE_EMAIL)) {
            if (user != null) {
                throw exception(USER_EMAIL_USED);
            }
            return;
        }
        if (user == null) {
            throw exception(USER_EMAIL_NOT_EXISTS);
        }
    }

    private void validateSendFrequency(String email, String scene, String createIp) {
        LocalDateTime now = LocalDateTime.now();
        MemberEmailCodeDO lastCode = emailCodeMapper.selectLastByEmailAndScene(email, scene);
        if (lastCode != null && lastCode.getCreateTime() != null
                && lastCode.getCreateTime().plusSeconds(SEND_INTERVAL_SECONDS).isAfter(now)) {
            throw exception(AUTH_EMAIL_CODE_SEND_TOO_FAST);
        }
        Long todayCount = emailCodeMapper.selectCountByEmailAndSceneToday(email, scene,
                LocalDateTimeUtil.beginOfDay(now));
        if (todayCount >= EMAIL_DAILY_LIMIT) {
            throw exception(AUTH_EMAIL_CODE_SEND_TOO_MANY);
        }
        if (StrUtil.isNotBlank(createIp)) {
            Long ipCount = emailCodeMapper.selectCountByCreateIpSince(createIp, now.minusHours(1));
            if (ipCount >= IP_HOURLY_LIMIT) {
                throw exception(AUTH_EMAIL_CODE_SEND_TOO_MANY);
            }
        }
    }

    private MemberEmailCodeDO validateCode(String email, String scene, String code) {
        validateScene(scene);
        MemberEmailCodeDO emailCode = emailCodeMapper.selectUnusedCode(email, scene, code);
        if (emailCode == null) {
            throw exception(AUTH_EMAIL_CODE_NOT_FOUND);
        }
        if (Boolean.TRUE.equals(emailCode.getUsed())) {
            throw exception(AUTH_EMAIL_CODE_USED);
        }
        if (emailCode.getExpiresTime().isBefore(LocalDateTime.now())) {
            throw exception(AUTH_EMAIL_CODE_EXPIRED);
        }
        if (!Objects.equals(emailCode.getCode(), code)) {
            throw exception(AUTH_EMAIL_CODE_NOT_CORRECT);
        }
        return emailCode;
    }

}
