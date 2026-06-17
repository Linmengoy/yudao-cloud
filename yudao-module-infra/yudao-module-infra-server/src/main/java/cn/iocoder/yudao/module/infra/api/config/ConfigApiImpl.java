package cn.iocoder.yudao.module.infra.api.config;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.infra.api.config.dto.ConfigSaveValueReqDTO;
import cn.iocoder.yudao.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController // 提供 RESTful API 接口，给 Feign 调用
@Validated
public class ConfigApiImpl implements ConfigApi {

    @Resource
    private ConfigService configService;

    @Override
    public CommonResult<String> getConfigValueByKey(String key) {
        ConfigDO config = configService.getConfigByKey(key);
        return success(config != null ? config.getValue() : null);
    }

    @Override
    public CommonResult<Boolean> saveConfigValueByKey(ConfigSaveValueReqDTO reqDTO) {
        ConfigDO config = configService.getConfigByKey(reqDTO.getKey());
        ConfigSaveReqVO reqVO = new ConfigSaveReqVO();
        reqVO.setId(config != null ? config.getId() : null);
        reqVO.setCategory(reqDTO.getCategory());
        reqVO.setName(reqDTO.getName());
        reqVO.setKey(reqDTO.getKey());
        reqVO.setValue(reqDTO.getValue());
        reqVO.setVisible(reqDTO.getVisible());
        reqVO.setRemark(reqDTO.getRemark());
        if (config == null) {
            configService.createConfig(reqVO);
        } else {
            configService.updateConfig(reqVO);
        }
        return success(true);
    }

}
