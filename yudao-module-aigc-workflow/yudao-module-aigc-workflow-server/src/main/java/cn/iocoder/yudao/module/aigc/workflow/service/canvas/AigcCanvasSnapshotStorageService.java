package cn.iocoder.yudao.module.aigc.workflow.service.canvas;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.module.aigc.workflow.dal.dataobject.canvas.AigcCanvasSnapshotDO;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import cn.iocoder.yudao.module.infra.api.file.dto.FileCreateRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class AigcCanvasSnapshotStorageService {

    private static final String STORAGE_TYPE_INLINE = "INLINE";
    private static final String MIME_TYPE_JSON = "application/json";

    @Resource
    private AigcCanvasSnapshotStorageProperties properties;
    @Resource
    private FileApi fileApi;

    public AigcCanvasSnapshotDO prepareForSave(AigcCanvasSnapshotDO snapshot) {
        SnapshotBody body = new SnapshotBody(snapshot.getNodesJson(), snapshot.getEdgesJson(), snapshot.getViewportJson());
        SnapshotBodyStats stats = stats(body);
        byte[] bytes = body.toJson().getBytes(StandardCharsets.UTF_8);
        snapshot.setSnapshotSize((long) bytes.length);
        snapshot.setSnapshotHash(DigestUtil.sha256Hex(bytes));
        if (shouldInline(stats, bytes.length)) {
            snapshot.setStorageType(STORAGE_TYPE_INLINE);
            snapshot.setStorageConfigId(null);
            snapshot.setBucket(null);
            snapshot.setSnapshotObjectKey(null);
            return snapshot;
        }

        FileCreateRespDTO file = fileApi.createFileV2(bytes, buildSnapshotFileName(snapshot), properties.getDirectory(), MIME_TYPE_JSON);
        snapshot.setStorageType(resolveObjectStorageType(file));
        snapshot.setStorageConfigId(file.getConfigId());
        snapshot.setBucket(file.getBucket());
        snapshot.setSnapshotObjectKey(StrUtil.blankToDefault(file.getObjectKey(), file.getPath()));
        snapshot.setNodesJson(null);
        snapshot.setEdgesJson(null);
        snapshot.setViewportJson(null);
        return snapshot;
    }

    public AigcCanvasSnapshotDO hydrateForRead(AigcCanvasSnapshotDO snapshot) {
        if (snapshot == null || !isExternal(snapshot)) {
            return snapshot;
        }
        byte[] bytes = fileApi.getFileContent(snapshot.getStorageConfigId(), snapshot.getSnapshotObjectKey()).getCheckedData();
        SnapshotBody body = SnapshotBody.fromJson(StrUtil.str(bytes, StandardCharsets.UTF_8));
        snapshot.setNodesJson(body.nodesJson());
        snapshot.setEdgesJson(body.edgesJson());
        snapshot.setViewportJson(body.viewportJson());
        return snapshot;
    }

    private boolean shouldInline(SnapshotBodyStats stats, int bodyBytes) {
        if (bodyBytes >= properties.getForceObjectStorageBytes()) {
            return false;
        }
        return bodyBytes <= properties.getInlineMaxBytes()
                && stats.nodeCount() <= properties.getInlineMaxNodes()
                && stats.edgeCount() <= properties.getInlineMaxEdges()
                && stats.maxNodeBytes() <= properties.getInlineMaxNodeBytes();
    }

    private boolean isExternal(AigcCanvasSnapshotDO snapshot) {
        return StrUtil.isNotBlank(snapshot.getSnapshotObjectKey())
                && snapshot.getStorageConfigId() != null
                && !STORAGE_TYPE_INLINE.equalsIgnoreCase(snapshot.getStorageType());
    }

    private SnapshotBodyStats stats(SnapshotBody body) {
        JSONArray nodes = JSONUtil.isTypeJSONArray(body.nodesJson()) ? JSONUtil.parseArray(body.nodesJson()) : new JSONArray();
        JSONArray edges = JSONUtil.isTypeJSONArray(body.edgesJson()) ? JSONUtil.parseArray(body.edgesJson()) : new JSONArray();
        int maxNodeBytes = 0;
        for (Object item : nodes) {
            int nodeBytes = JSONUtil.toJsonStr(item).getBytes(StandardCharsets.UTF_8).length;
            maxNodeBytes = Math.max(maxNodeBytes, nodeBytes);
        }
        return new SnapshotBodyStats(nodes.size(), edges.size(), maxNodeBytes);
    }

    private String buildSnapshotFileName(AigcCanvasSnapshotDO snapshot) {
        return "project-" + snapshot.getProjectId() + "-v" + snapshot.getVersion() + ".json";
    }

    private String resolveObjectStorageType(FileCreateRespDTO file) {
        if (StrUtil.isNotBlank(file.getStorageType())) {
            return "S3".equalsIgnoreCase(file.getStorageType()) ? "OSS" : file.getStorageType();
        }
        return "OSS";
    }

    private record SnapshotBody(String nodesJson, String edgesJson, String viewportJson) {

        private String toJson() {
            return new JSONObject()
                    .set("nodesJson", StrUtil.blankToDefault(nodesJson, "[]"))
                    .set("edgesJson", StrUtil.blankToDefault(edgesJson, "[]"))
                    .set("viewportJson", viewportJson)
                    .toString();
        }

        private static SnapshotBody fromJson(String json) {
            JSONObject object = JSONUtil.parseObj(json);
            return new SnapshotBody(
                    object.getStr("nodesJson", "[]"),
                    object.getStr("edgesJson", "[]"),
                    object.getStr("viewportJson"));
        }

    }

    private record SnapshotBodyStats(int nodeCount, int edgeCount, int maxNodeBytes) {
    }

}
