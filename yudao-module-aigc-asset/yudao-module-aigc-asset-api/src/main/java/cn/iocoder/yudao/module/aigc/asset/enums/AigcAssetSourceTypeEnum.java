package cn.iocoder.yudao.module.aigc.asset.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AigcAssetSourceTypeEnum {

    GENERATE("GENERATE", "AIGC 生成"),
    UPLOAD("UPLOAD", "用户上传"),
    IMPORT("IMPORT", "外部导入"),
    EDIT("EDIT", "编辑产生"),
    CLONE("CLONE", "克隆产生");

    private final String code;
    private final String name;

}
