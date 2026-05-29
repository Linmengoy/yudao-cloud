package cn.iocoder.yudao.module.member.service.auth;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.member.dal.dataobject.auth.MemberEmailCodeDO;
import cn.iocoder.yudao.module.member.dal.dataobject.user.MemberUserDO;
import cn.iocoder.yudao.module.member.dal.mysql.auth.MemberEmailCodeMapper;
import cn.iocoder.yudao.module.member.dal.redis.auth.MemberEmailCodeRedisDAO;
import cn.iocoder.yudao.module.member.enums.auth.MemberEmailCodeSceneEnum;
import cn.iocoder.yudao.module.member.framework.auth.config.MemberEmailCodeProperties;
import cn.iocoder.yudao.module.member.service.user.MemberUserService;
import cn.iocoder.yudao.module.system.api.mail.MailSendApi;
import cn.iocoder.yudao.module.system.api.mail.dto.MailSendSingleToUserReqDTO;
import jakarta.annotation.Resource;
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

    private static final int CODE_LENGTH = 4;

    @Resource
    private MemberEmailCodeMapper emailCodeMapper;
    @Resource
    private MemberEmailCodeRedisDAO emailCodeRedisDAO;
    @Resource
    private MemberEmailCodeProperties emailCodeProperties;
    @Resource
    private MemberUserService userService;
    @Resource
    private MailSendApi mailSendApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendEmailCode(String email, String scene, String createIp) {
        email = normalizeEmail(email);
        // 校验场景
        MemberEmailCodeSceneEnum sceneEnum = validateScene(scene);
        validateEmailByScene(email, sceneEnum);
        int todayIndex = validateSendFrequency(email, scene, createIp);

        String code = RandomUtil.randomNumbers(CODE_LENGTH);
        LocalDateTime now = LocalDateTime.now();
        emailCodeMapper.insert(MemberEmailCodeDO.builder()
                .email(email)
                .code(code)
                .scene(scene)
                .used(false)
                .createIp(createIp)
                .todayIndex(todayIndex)
                .expiresTime(now.plus(emailCodeProperties.getExpireTime()))
                .build());

        MailSendSingleToUserReqDTO reqDTO = new MailSendSingleToUserReqDTO();
        reqDTO.setToMails(Collections.singletonList(email));
        reqDTO.setTemplateCode(sceneEnum.getTemplateCode());
        reqDTO.setTemplateParams(Map.of("code", code, "expireMinutes", emailCodeProperties.getExpireTime().toMinutes(),
                "productName", emailCodeProperties.getProductName()));
        mailSendApi.sendSingleMailToMember(reqDTO).checkError();
    }

    @Override
    public void validateEmailCode(String email, String scene, String code) {
        validateCode(normalizeEmail(email), scene, code);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void useEmailCode(String email, String scene, String code, String usedIp) {
        MemberEmailCodeDO emailCode = validateCode(normalizeEmail(email), scene, code);
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

    private int validateSendFrequency(String email, String scene, String createIp) {
        Long tenantId = TenantContextHolder.getTenantId();
        Boolean acquired = emailCodeRedisDAO.tryAcquireSendInterval(tenantId, scene, email, emailCodeProperties.getSendInterval());
        if (!Boolean.TRUE.equals(acquired)) {
            throw exception(AUTH_EMAIL_CODE_SEND_TOO_FAST);
        }
        Long todayCount = emailCodeRedisDAO.incrementEmailDaily(tenantId, scene, email);
        if (todayCount == null || todayCount > emailCodeProperties.getEmailDailyLimit()) {
            throw exception(AUTH_EMAIL_CODE_SEND_TOO_MANY);
        }
        if (StrUtil.isNotBlank(createIp)) {
            Long ipCount = emailCodeRedisDAO.incrementIpHourly(tenantId, createIp, emailCodeProperties.getIpHourlyWindow());
            if (ipCount == null || ipCount > emailCodeProperties.getIpHourlyLimit()) {
                throw exception(AUTH_EMAIL_CODE_SEND_TOO_MANY);
            }
        }
        return todayCount.intValue();
    }

    private String normalizeEmail(String email) {
        return StrUtil.trim(email).toLowerCase();
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
