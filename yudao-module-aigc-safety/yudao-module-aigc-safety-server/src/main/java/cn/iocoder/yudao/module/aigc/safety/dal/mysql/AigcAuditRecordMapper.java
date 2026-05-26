package cn.iocoder.yudao.module.aigc.safety.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.safety.controller.admin.audit.vo.AigcAuditRecordPageReqVO;
import cn.iocoder.yudao.module.aigc.safety.dal.dataobject.AigcAuditRecordDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AigcAuditRecordMapper extends BaseMapperX<AigcAuditRecordDO> {

    default PageResult<AigcAuditRecordDO> selectPage(AigcAuditRecordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcAuditRecordDO>()
                .eqIfPresent(AigcAuditRecordDO::getObjectType, reqVO.getObjectType())
                .eqIfPresent(AigcAuditRecordDO::getObjectId, reqVO.getObjectId())
                .eqIfPresent(AigcAuditRecordDO::getScene, reqVO.getScene())
                .eqIfPresent(AigcAuditRecordDO::getAuditStatus, reqVO.getAuditStatus())
                .eqIfPresent(AigcAuditRecordDO::getAuditResult, reqVO.getAuditResult())
                .eqIfPresent(AigcAuditRecordDO::getRiskLevel, reqVO.getRiskLevel())
                .betweenIfPresent(AigcAuditRecordDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(AigcAuditRecordDO::getId));
    }

    default AigcAuditRecordDO selectByObject(String objectType, Long objectId) {
        return selectOne(AigcAuditRecordDO::getObjectType, objectType, AigcAuditRecordDO::getObjectId, objectId);
    }

    default int updateStatusIfPending(AigcAuditRecordDO updateObj, String pendingStatus) {
        return update(updateObj, new LambdaUpdateWrapper<AigcAuditRecordDO>()
                .eq(AigcAuditRecordDO::getId, updateObj.getId())
                .eq(AigcAuditRecordDO::getAuditStatus, pendingStatus));
    }

}
