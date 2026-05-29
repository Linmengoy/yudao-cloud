package cn.iocoder.yudao.module.aigc.billing.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.billing.controller.admin.packageconfig.vo.AigcRechargePackagePageReqVO;
import cn.iocoder.yudao.module.aigc.billing.dal.dataobject.AigcRechargePackageDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AigcRechargePackageMapper extends BaseMapperX<AigcRechargePackageDO> {

    default PageResult<AigcRechargePackageDO> selectPage(AigcRechargePackagePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AigcRechargePackageDO>()
                .likeIfPresent(AigcRechargePackageDO::getName, reqVO.getName())
                .eqIfPresent(AigcRechargePackageDO::getStatus, reqVO.getStatus())
                .orderByAsc(AigcRechargePackageDO::getSort)
                .orderByDesc(AigcRechargePackageDO::getId));
    }

    default List<AigcRechargePackageDO> selectListByStatus(Integer status) {
        return selectList(new LambdaQueryWrapperX<AigcRechargePackageDO>()
                .eq(AigcRechargePackageDO::getStatus, status)
                .orderByAsc(AigcRechargePackageDO::getSort)
                .orderByDesc(AigcRechargePackageDO::getId));
    }

}
