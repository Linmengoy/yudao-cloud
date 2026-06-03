package cn.iocoder.yudao.module.aigc.model.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AigcModelParamUtilsTest {

    @Test
    public void testParseOptions_cleanEscapedValues() {
        List<String> options = AigcModelParamUtils.parseOptions("[\"\\\"1:1\\\"\",\"\\\"2:3\\\"\"]");

        assertEquals(List.of("1:1", "2:3"), options);
    }

}
