package cn.iocoder.yudao.module.member.dal.mysql.auth;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.member.dal.dataobject.auth.MemberEmailCodeDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;

@Mapper
public interface MemberEmailCodeMapper extends BaseMapperX<MemberEmailCodeDO> {

    default MemberEmailCodeDO selectLastByEmailAndScene(String email, String scene) {
        return selectOne(new LambdaQueryWrapperX<MemberEmailCodeDO>()
                .eq(MemberEmailCodeDO::getEmail, email)
                .eq(MemberEmailCodeDO::getScene, scene)
                .orderByDesc(MemberEmailCodeDO::getId)
                .last("LIMIT 1"));
    }

    default Long selectCountByEmailAndSceneToday(String email, String scene, LocalDateTime beginTime) {
        return selectCount(new LambdaQueryWrapperX<MemberEmailCodeDO>()
                .eq(MemberEmailCodeDO::getEmail, email)
                .eq(MemberEmailCodeDO::getScene, scene)
                .ge(MemberEmailCodeDO::getCreateTime, beginTime));
    }

    default Long selectCountByCreateIpSince(String createIp, LocalDateTime beginTime) {
        return selectCount(new LambdaQueryWrapperX<MemberEmailCodeDO>()
                .eq(MemberEmailCodeDO::getCreateIp, createIp)
                .ge(MemberEmailCodeDO::getCreateTime, beginTime));
    }

    default MemberEmailCodeDO selectUnusedCode(String email, String scene, String code) {
        return selectOne(new LambdaQueryWrapperX<MemberEmailCodeDO>()
                .eq(MemberEmailCodeDO::getEmail, email)
                .eq(MemberEmailCodeDO::getScene, scene)
                .eq(MemberEmailCodeDO::getCode, code)
                .eq(MemberEmailCodeDO::getUsed, false)
                .orderByDesc(MemberEmailCodeDO::getId)
                .last("LIMIT 1"));
    }

}
