package cn.iocoder.yudao.module.aigc.workflow.service.canvas;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "canvas.snapshot")
public class AigcCanvasSnapshotStorageProperties {

    private int inlineMaxBytes = 512 * 1024;

    private int inlineMaxNodes = 200;

    private int inlineMaxEdges = 500;

    private int inlineMaxNodeBytes = 64 * 1024;

    private int forceObjectStorageBytes = 2 * 1024 * 1024;

    private String directory = "canvas/snapshots";

}
