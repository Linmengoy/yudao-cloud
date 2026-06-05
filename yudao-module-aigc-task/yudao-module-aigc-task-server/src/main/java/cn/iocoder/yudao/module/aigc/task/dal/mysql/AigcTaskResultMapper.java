package cn.iocoder.yudao.module.aigc.task.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aigc.task.dal.dataobject.AigcTaskResultDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AigcTaskResultMapper extends BaseMapperX<AigcTaskResultDO> {

    default AigcTaskResultDO selectByTaskId(Long taskId) {
        return selectOne(new LambdaQueryWrapperX<AigcTaskResultDO>()
                .eq(AigcTaskResultDO::getTaskId, taskId));
    }

    default void saveByTaskId(Long taskId, String outputText, String outputData) {
        AigcTaskResultDO exists = selectByTaskId(taskId);
        if (exists == null) {
            insert(new AigcTaskResultDO()
                    .setTaskId(taskId)
                    .setOutputText(outputText)
                    .setOutputData(outputData));
            return;
        }
        update(null, new LambdaUpdateWrapper<AigcTaskResultDO>()
                .set(AigcTaskResultDO::getOutputText, outputText)
                .set(AigcTaskResultDO::getOutputData, outputData)
                .eq(AigcTaskResultDO::getTaskId, taskId));
    }

}
