package cn.iocoder.yudao.module.aigc.gen.service.record;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aigc.asset.api.AigcAssetApi;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetCreateReqDTO;
import cn.iocoder.yudao.module.aigc.asset.dto.AigcAssetCreateRespDTO;
import cn.iocoder.yudao.module.aigc.billing.api.AigcBillingApi;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingConfirmReqDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingFreezeReqDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingFreezeRespDTO;
import cn.iocoder.yudao.module.aigc.billing.dto.AigcBillingReleaseReqDTO;
import cn.iocoder.yudao.module.aigc.gen.controller.admin.callback.vo.AigcGenerateCallbackPageReqVO;
import cn.iocoder.yudao.module.aigc.gen.controller.admin.providerlog.vo.AigcGenerateProviderLogPageReqVO;
import cn.iocoder.yudao.module.aigc.gen.controller.admin.record.vo.AigcGenerateRecordPageReqVO;
import cn.iocoder.yudao.module.aigc.gen.dal.dataobject.AigcGenerateCallbackDO;
import cn.iocoder.yudao.module.aigc.gen.dal.dataobject.AigcGenerateProviderLogDO;
import cn.iocoder.yudao.module.aigc.gen.dal.dataobject.AigcGenerateRecordDO;
import cn.iocoder.yudao.module.aigc.gen.dal.mysql.AigcGenerateCallbackMapper;
import cn.iocoder.yudao.module.aigc.gen.dal.mysql.AigcGenerateProviderLogMapper;
import cn.iocoder.yudao.module.aigc.gen.dal.mysql.AigcGenerateRecordMapper;
import cn.iocoder.yudao.module.aigc.gen.dto.AigcGenerateCallbackReqDTO;
import cn.iocoder.yudao.module.aigc.gen.dto.AigcGenerateSubmitReqDTO;
import cn.iocoder.yudao.module.aigc.gen.enums.AigcGenerateStatusEnum;
import cn.iocoder.yudao.module.aigc.gen.framework.client.AigcProviderClient;
import cn.iocoder.yudao.module.aigc.gen.framework.client.AigcProviderClientFactory;
import cn.iocoder.yudao.module.aigc.gen.framework.client.dto.AigcProviderSubmitReqDTO;
import cn.iocoder.yudao.module.aigc.gen.framework.client.dto.AigcProviderSubmitRespDTO;
import cn.iocoder.yudao.module.aigc.gen.framework.security.AigcGenerateFileSecurityUtils;
import cn.iocoder.yudao.module.aigc.model.api.AigcModelApi;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelPriceCalculateReqDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelPriceCalculateRespDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelProviderRespDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelRespDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelUsageRecordReqDTO;
import cn.iocoder.yudao.module.aigc.model.dto.AigcModelValidateReqDTO;
import cn.iocoder.yudao.module.aigc.safety.api.AigcSafetyApi;
import cn.iocoder.yudao.module.aigc.safety.dto.AigcSafetyPromptCheckReqDTO;
import cn.iocoder.yudao.module.aigc.safety.dto.AigcSafetyPromptCheckRespDTO;
import cn.iocoder.yudao.module.aigc.task.api.AigcTaskApi;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskCallbackCreateReqDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskCreateReqDTO;
import cn.iocoder.yudao.module.aigc.task.dto.AigcTaskStatusUpdateReqDTO;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.aigc.gen.enums.ErrorCodeConstants.GENERATE_PROMPT_NOT_PASS;
import static cn.iocoder.yudao.module.aigc.gen.enums.ErrorCodeConstants.GENERATE_PROVIDER_CALLBACK_INVALID;
import static cn.iocoder.yudao.module.aigc.gen.enums.ErrorCodeConstants.GENERATE_PROVIDER_RESULT_INVALID;
import static cn.iocoder.yudao.module.aigc.gen.enums.ErrorCodeConstants.GENERATE_RECORD_NOT_EXISTS;

@Service
@Validated
public class AigcGenerateRecordServiceImpl implements AigcGenerateRecordService {

    private static final Set<String> FILE_TYPES = Set.of("IMAGE", "VIDEO", "AUDIO", "DOCUMENT");
    private static final Set<String> WAITING_STATUSES = Set.of(AigcGenerateStatusEnum.SUBMITTED.getCode(), AigcGenerateStatusEnum.CALLBACK_WAITING.getCode(), AigcGenerateStatusEnum.SYNCING.getCode());

    @Resource
    private AigcGenerateRecordMapper generateRecordMapper;
    @Resource
    private AigcGenerateCallbackMapper callbackMapper;
    @Resource
    private AigcGenerateProviderLogMapper providerLogMapper;
    @Resource
    private AigcModelApi modelApi;
    @Resource
    private AigcBillingApi billingApi;
    @Resource
    private AigcTaskApi taskApi;
    @Resource
    private AigcAssetApi assetApi;
    @Resource
    private FileApi fileApi;
    @Resource
    private AigcSafetyApi safetyApi;
    @Resource
    private AigcProviderClientFactory providerClientFactory;
    @Resource
    private ObjectProvider<MeterRegistry> meterRegistryProvider;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AigcGenerateRecordDO createGenerateRecord(AigcGenerateSubmitReqDTO reqDTO) {
        if (reqDTO.getClientRequestId() != null) {
            AigcGenerateRecordDO exists = generateRecordMapper.selectByClientRequestId(reqDTO.getUserId(), reqDTO.getClientRequestId());
            if (exists != null) {
                return exists;
            }
        }
        AigcGenerateRecordDO record = BeanUtils.toBean(reqDTO, AigcGenerateRecordDO.class)
                .setInputParams(sanitizeInputParamsSnapshot(reqDTO.getInputParams()))
                .setGenerateNo(generateGenerateNo())
                .setStatus(AigcGenerateStatusEnum.CREATED.getCode());
        generateRecordMapper.insert(record);
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AigcGenerateRecordDO submitGenerate(AigcGenerateSubmitReqDTO reqDTO) {
        AigcGenerateRecordDO exists = reqDTO.getClientRequestId() == null ? null : generateRecordMapper.selectByClientRequestId(reqDTO.getUserId(), reqDTO.getClientRequestId());
        if (exists != null) {
            return exists;
        }
        checkPrompt(reqDTO);
        recordMetric("aigc_gen_submit_total");
        AigcModelRespDTO model = modelApi.validateModel(reqDTO.getModelId(), reqDTO.getGenerateMode()).getCheckedData();
        Long executionModelId = model.getId();
        AigcModelProviderRespDTO provider = model.getProviderId() == null ? null : modelApi.getProvider(model.getProviderId()).getCheckedData();
        Map<String, Object> inputParams = parseInputParams(reqDTO.getInputParams());
        String inputParamsSnapshot = sanitizeInputParamsSnapshot(reqDTO.getInputParams());
        modelApi.validateParams(new AigcModelValidateReqDTO().setModelId(reqDTO.getModelId()).setCapability(reqDTO.getGenerateMode()).setParams(inputParams)).getCheckedData();
        AigcModelPriceCalculateRespDTO price = modelApi.calculatePrice(new AigcModelPriceCalculateReqDTO()
                .setModelId(reqDTO.getModelId()).setCapability(reqDTO.getGenerateMode()).setTaskType(reqDTO.getGenerateType()).setParams(inputParams)).getCheckedData();
        AigcBillingFreezeRespDTO freeze = billingApi.freeze(new AigcBillingFreezeReqDTO()
                .setUserId(reqDTO.getUserId()).setBizType("AIGC_GENERATE").setBizId(reqDTO.getClientRequestId() == null ? generateGenerateNo() : reqDTO.getClientRequestId())
                .setAmount(price.getSalePrice()).setTitle(reqDTO.getGenerateType() + "生成冻结").setPriceSnapshot(JsonUtils.toJsonString(price))).getCheckedData();
        Long taskId = taskApi.createTask(new AigcTaskCreateReqDTO()
                .setClientRequestId(reqDTO.getClientRequestId()).setUserId(reqDTO.getUserId()).setTaskType(reqDTO.getGenerateType())
                .setCapability(reqDTO.getGenerateMode()).setModelId(executionModelId).setProviderId(model.getProviderId()).setRequestParams(inputParamsSnapshot)
                .setPriceSnapshot(JsonUtils.toJsonString(price)).setFreezeId(freeze.getId()).setSalePrice(price.getSalePrice()).setCostPrice(price.getCostPrice()).setCurrencyType(price.getCurrencyType())).getCheckedData();
        AigcGenerateRecordDO record = BeanUtils.toBean(reqDTO, AigcGenerateRecordDO.class)
                .setInputParams(inputParamsSnapshot)
                .setModelId(executionModelId)
                .setGenerateNo(generateGenerateNo()).setTaskId(taskId).setModelCode(model.getCode()).setProviderId(model.getProviderId())
                .setProviderCode(resolveProviderCode(provider)).setFreezeId(freeze.getId())
                .setPriceAmount(price.getSalePrice()).setCostAmount(price.getCostPrice()).setStatus(AigcGenerateStatusEnum.SUBMITTING.getCode()).setSubmitTime(LocalDateTime.now());
        generateRecordMapper.insert(record);
        taskApi.markQueued(taskId).getCheckedData();
        taskApi.markRunning(taskId).getCheckedData();
        if (!Boolean.TRUE.equals(reqDTO.getSync())) {
            submitProviderAfterCommit(record, reqDTO, provider);
            return generateRecordMapper.selectById(record.getId());
        }
        return processProviderSubmit(record, reqDTO, provider);
    }

    private AigcGenerateRecordDO processProviderSubmit(AigcGenerateRecordDO record, AigcGenerateSubmitReqDTO reqDTO, AigcModelProviderRespDTO provider) {
        AigcProviderSubmitRespDTO providerResp = submitProvider(record, reqDTO, provider);
        if (!Boolean.TRUE.equals(providerResp.getSuccess())) {
            recordMetric("aigc_gen_submit_failed_total");
            failRecord(record, providerResp.getErrorCode(), providerResp.getErrorMessage());
            return generateRecordMapper.selectById(record.getId());
        }
        recordMetric("aigc_gen_submit_success_total");
        AigcGenerateRecordDO updateObj = new AigcGenerateRecordDO().setId(record.getId()).setProviderTaskId(providerResp.getProviderTaskId()).setProviderStatus(providerResp.getProviderStatus());
        if (Boolean.TRUE.equals(providerResp.getFinished())) {
            updateObj.setStatus(AigcGenerateStatusEnum.ASSET_CREATING.getCode()).setOutputText(providerResp.getOutputText()).setOutputData(providerResp.getOutputData()).setOutputUrls(providerResp.getOutputUrls());
            generateRecordMapper.updateById(updateObj);
            finishSuccess(generateRecordMapper.selectById(record.getId()), providerResp);
            generateRecordMapper.updateById(new AigcGenerateRecordDO().setId(record.getId()).setStatus(AigcGenerateStatusEnum.SUCCESS.getCode()).setFinishTime(LocalDateTime.now()));
        } else {
            updateObj.setStatus(AigcGenerateStatusEnum.CALLBACK_WAITING.getCode());
            generateRecordMapper.updateById(updateObj);
            taskApi.markCallbackWaiting(new AigcTaskStatusUpdateReqDTO().setTaskId(record.getTaskId()).setExternalTaskId(providerResp.getProviderTaskId()).setProgress(30)).getCheckedData();
        }
        return generateRecordMapper.selectById(record.getId());
    }

    private void submitProviderAfterCommit(AigcGenerateRecordDO record, AigcGenerateSubmitReqDTO reqDTO, AigcModelProviderRespDTO provider) {
        Runnable task = () -> CompletableFuture.runAsync(() -> {
            try {
                processProviderSubmit(record, reqDTO, provider);
            } catch (Exception ex) {
                failRecord(record, "SUBMIT_EXCEPTION", ex.getMessage());
            }
        });
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            task.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                task.run();
            }
        });
    }

    @Override
    public AigcGenerateRecordDO getGenerateRecord(Long id) {
        return generateRecordMapper.selectById(id);
    }

    @Override
    public AigcGenerateRecordDO getGenerateRecordByTaskId(Long taskId) {
        return generateRecordMapper.selectByTaskId(taskId);
    }

    @Override
    public AigcGenerateRecordDO validateGenerateRecordExists(Long id) {
        AigcGenerateRecordDO record = generateRecordMapper.selectById(id);
        if (record == null) {
            throw exception(GENERATE_RECORD_NOT_EXISTS);
        }
        return record;
    }

    @Override
    public PageResult<AigcGenerateRecordDO> getGenerateRecordPage(AigcGenerateRecordPageReqVO reqVO) {
        return generateRecordMapper.selectPage(reqVO);
    }

    @Override
    public PageResult<AigcGenerateCallbackDO> getCallbackPage(AigcGenerateCallbackPageReqVO reqVO) {
        return callbackMapper.selectPage(reqVO);
    }

    @Override
    public PageResult<AigcGenerateProviderLogDO> getProviderLogPage(AigcGenerateProviderLogPageReqVO reqVO) {
        return providerLogMapper.selectPage(reqVO);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseInputParams(String inputParams) {
        if (StrUtil.isBlank(inputParams) || !JsonUtils.isJsonObject(inputParams)) {
            return Map.of();
        }
        Map<String, Object> params = JsonUtils.parseObject(inputParams, Map.class);
        return params == null ? Map.of() : params;
    }

    private String sanitizeInputParamsSnapshot(String inputParams) {
        if (StrUtil.isBlank(inputParams) || !JSONUtil.isTypeJSON(inputParams)) {
            return inputParams;
        }
        JSONObject params = JSONUtil.parseObj(inputParams);
        JSONArray inputImages = params.getJSONArray("inputImages");
        if (inputImages == null || inputImages.isEmpty()) {
            return params.toString();
        }
        JSONArray sanitizedImages = new JSONArray();
        for (Object item : inputImages) {
            JSONObject image = JSONUtil.parseObj(item);
            image.remove("dataUrl");
            sanitizedImages.add(image);
        }
        params.set("inputImages", sanitizedImages);
        return params.toString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createCallback(AigcGenerateCallbackReqDTO reqDTO) {
        if (reqDTO.getCallbackNo() != null && callbackMapper.selectByCallbackNo(reqDTO.getProviderCode(), reqDTO.getCallbackNo()) != null) {
            return;
        }
        recordMetric("aigc_gen_callback_total");
        AigcGenerateRecordDO record = generateRecordMapper.selectByProviderTask(reqDTO.getProviderCode(), reqDTO.getProviderTaskId());
        AigcProviderClient client = providerClientFactory.getClient(reqDTO.getProviderCode());
        boolean signatureValid = client.verifyCallback(reqDTO);
        AigcGenerateCallbackDO callback = BeanUtils.toBean(reqDTO, AigcGenerateCallbackDO.class)
                .setRecordId(record == null ? null : record.getId()).setTaskId(record == null ? null : record.getTaskId())
                .setSignatureValid(signatureValid).setProcessStatus(signatureValid ? AigcGenerateStatusEnum.SUCCESS.getCode() : AigcGenerateStatusEnum.FAILED.getCode())
                .setProcessTime(LocalDateTime.now());
        callbackMapper.insert(callback);
        if (!signatureValid) {
            recordMetric("aigc_gen_callback_invalid_total");
            throw exception(GENERATE_PROVIDER_CALLBACK_INVALID);
        }
        if (record == null) {
            return;
        }
        taskApi.createCallbackRecord(new AigcTaskCallbackCreateReqDTO().setTaskId(record.getTaskId()).setProviderId(record.getProviderId()).setProviderCode(reqDTO.getProviderCode())
                .setExternalTaskId(reqDTO.getProviderTaskId()).setCallbackType(reqDTO.getCallbackType() == null ? "GEN_CALLBACK" : reqDTO.getCallbackType()).setRawBody(reqDTO.getRawBody()).setSignature(reqDTO.getSignature())).getCheckedData();
        if (AigcGenerateStatusEnum.SUCCESS.getCode().equals(reqDTO.getResultStatus())) {
            AigcProviderSubmitRespDTO resp = new AigcProviderSubmitRespDTO().setProviderTaskId(reqDTO.getProviderTaskId()).setProviderStatus(reqDTO.getResultStatus())
                    .setOutputText(reqDTO.getOutputText()).setOutputData(reqDTO.getOutputData()).setOutputUrls(reqDTO.getOutputUrls()).setSuccess(true).setFinished(true);
            generateRecordMapper.updateById(new AigcGenerateRecordDO().setId(record.getId()).setStatus(AigcGenerateStatusEnum.ASSET_CREATING.getCode()).setOutputText(reqDTO.getOutputText())
                    .setOutputData(reqDTO.getOutputData()).setOutputUrls(reqDTO.getOutputUrls()).setCallbackTime(LocalDateTime.now()));
            finishSuccess(generateRecordMapper.selectById(record.getId()), resp);
            generateRecordMapper.updateById(new AigcGenerateRecordDO().setId(record.getId()).setStatus(AigcGenerateStatusEnum.SUCCESS.getCode()).setFinishTime(LocalDateTime.now()));
        } else if (AigcGenerateStatusEnum.FAILED.getCode().equals(reqDTO.getResultStatus())) {
            failRecord(record, "PROVIDER_FAILED", reqDTO.getFailReason());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncTask(Long taskId) {
        AigcGenerateRecordDO record = generateRecordMapper.selectByTaskId(taskId);
        if (record == null) {
            throw exception(GENERATE_RECORD_NOT_EXISTS);
        }
        AigcProviderSubmitRespDTO resp = providerClientFactory.getClient(record.getProviderCode()).query(buildProviderQueryReq(record));
        if (!Boolean.TRUE.equals(resp.getSuccess())) {
            failRecord(record, resp.getErrorCode(), resp.getErrorMessage());
            return;
        }
        if (Boolean.TRUE.equals(resp.getFinished())) {
            generateRecordMapper.updateById(new AigcGenerateRecordDO().setId(record.getId()).setStatus(AigcGenerateStatusEnum.ASSET_CREATING.getCode())
                    .setProviderStatus(resp.getProviderStatus()).setOutputText(resp.getOutputText()).setOutputData(resp.getOutputData()).setOutputUrls(resp.getOutputUrls()));
            finishSuccess(generateRecordMapper.selectById(record.getId()), resp);
            generateRecordMapper.updateById(new AigcGenerateRecordDO().setId(record.getId()).setStatus(AigcGenerateStatusEnum.SUCCESS.getCode()).setFinishTime(LocalDateTime.now()));
        } else {
            generateRecordMapper.updateById(new AigcGenerateRecordDO().setId(record.getId()).setStatus(AigcGenerateStatusEnum.CALLBACK_WAITING.getCode()).setProviderStatus(resp.getProviderStatus()));
        }
    }

    @Override
    public int syncTimeoutTasks() {
        List<AigcGenerateRecordDO> records = generateRecordMapper.selectTimeoutList(WAITING_STATUSES, LocalDateTime.now().minusMinutes(5));
        records.forEach(record -> syncTask(record.getTaskId()));
        if (!records.isEmpty()) {
            recordMetric("aigc_gen_timeout_total", records.size());
        }
        return records.size();
    }

    private void checkPrompt(AigcGenerateSubmitReqDTO reqDTO) {
        if (reqDTO.getPrompt() == null || reqDTO.getPrompt().isBlank()) {
            return;
        }
        AigcSafetyPromptCheckRespDTO result = safetyApi.checkPrompt(new AigcSafetyPromptCheckReqDTO()
                .setPrompt(reqDTO.getPrompt()).setScene("PROMPT").setModelId(reqDTO.getModelId()).setUserId(reqDTO.getUserId()).setBizId(reqDTO.getClientRequestId())).getCheckedData();
        if (!Boolean.TRUE.equals(result.getPass())) {
            throw exception(GENERATE_PROMPT_NOT_PASS);
        }
    }

    private AigcProviderSubmitRespDTO submitProvider(AigcGenerateRecordDO record, AigcGenerateSubmitReqDTO reqDTO, AigcModelProviderRespDTO provider) {
        AigcModelRespDTO model = modelApi.getModel(record.getModelId()).getCheckedData();
        AigcProviderSubmitReqDTO providerReq = new AigcProviderSubmitReqDTO().setRecordId(record.getId()).setTaskId(record.getTaskId()).setUserId(record.getUserId())
                .setModelId(record.getModelId()).setModelCode(record.getModelCode()).setProviderModel(model == null ? null : model.getModel())
                .setProviderId(record.getProviderId()).setProviderCode(record.getProviderCode())
                .setProviderBaseUrl(provider == null ? null : provider.getApiBaseUrl()).setProviderApiKey(provider == null ? null : provider.getApiKey())
                .setProviderSecretKey(provider == null ? null : provider.getSecretKey()).setProviderExtraConfig(provider == null ? null : provider.getExtraConfig())
                .setProviderTimeoutSeconds(provider == null ? null : provider.getTimeoutSeconds())
                .setProxyEnabled(provider == null ? null : provider.getProxyEnabled()).setProxyProtocol(provider == null ? null : provider.getProxyProtocol())
                .setProxyHost(provider == null ? null : provider.getProxyHost()).setProxyPort(provider == null ? null : provider.getProxyPort())
                .setProxyUsername(provider == null ? null : provider.getProxyUsername()).setProxyPassword(provider == null ? null : provider.getProxyPassword())
                .setGenerateType(record.getGenerateType()).setGenerateMode(record.getGenerateMode()).setPrompt(record.getPrompt()).setInputParams(reqDTO.getInputParams()).setSync(reqDTO.getSync());
        long start = System.currentTimeMillis();
        AigcProviderSubmitRespDTO resp;
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();
        if (meterRegistry == null) {
            resp = providerClientFactory.getClient(record.getProviderCode()).submit(providerReq);
        } else {
            resp = Timer.builder("aigc_gen_provider_duration_ms")
                    .tag("provider", record.getProviderCode() == null ? "unknown" : record.getProviderCode())
                    .register(meterRegistry)
                    .record(() -> providerClientFactory.getClient(record.getProviderCode()).submit(providerReq));
        }
        providerLogMapper.insert(new AigcGenerateProviderLogDO().setRecordId(record.getId()).setTaskId(record.getTaskId()).setProviderCode(record.getProviderCode()).setModelCode(record.getModelCode())
                .setApiAction("submit").setRequestId(record.getGenerateNo()).setRequestSummary(buildRequestSummary(record)).setResponseSummary(buildResponseSummary(resp))
                .setSuccess(Boolean.TRUE.equals(resp.getSuccess())).setErrorCode(resp.getErrorCode()).setErrorMessage(resp.getErrorMessage()).setDurationMs(System.currentTimeMillis() - start));
        return resp;
    }

    private AigcProviderSubmitReqDTO buildProviderQueryReq(AigcGenerateRecordDO record) {
        AigcModelRespDTO model = modelApi.getModel(record.getModelId()).getCheckedData();
        AigcModelProviderRespDTO provider = record.getProviderId() == null ? null : modelApi.getProvider(record.getProviderId()).getCheckedData();
        return new AigcProviderSubmitReqDTO()
                .setRecordId(record.getId())
                .setTaskId(record.getTaskId())
                .setUserId(record.getUserId())
                .setModelId(record.getModelId())
                .setModelCode(record.getModelCode())
                .setProviderModel(model == null ? null : model.getModel())
                .setProviderId(record.getProviderId())
                .setProviderCode(record.getProviderCode())
                .setProviderTaskId(record.getProviderTaskId())
                .setProviderBaseUrl(provider == null ? null : provider.getApiBaseUrl())
                .setProviderApiKey(provider == null ? null : provider.getApiKey())
                .setProviderSecretKey(provider == null ? null : provider.getSecretKey())
                .setProviderExtraConfig(provider == null ? null : provider.getExtraConfig())
                .setProviderTimeoutSeconds(provider == null ? null : provider.getTimeoutSeconds())
                .setProxyEnabled(provider == null ? null : provider.getProxyEnabled())
                .setProxyProtocol(provider == null ? null : provider.getProxyProtocol())
                .setProxyHost(provider == null ? null : provider.getProxyHost())
                .setProxyPort(provider == null ? null : provider.getProxyPort())
                .setProxyUsername(provider == null ? null : provider.getProxyUsername())
                .setProxyPassword(provider == null ? null : provider.getProxyPassword())
                .setGenerateType(record.getGenerateType())
                .setGenerateMode(record.getGenerateMode())
                .setPrompt(record.getPrompt())
                .setInputParams(record.getInputParams());
    }

    private void finishSuccess(AigcGenerateRecordDO record, AigcProviderSubmitRespDTO resp) {
        Long assetId = createAssetIfNecessary(record);
        taskApi.markSuccess(new AigcTaskStatusUpdateReqDTO().setTaskId(record.getTaskId()).setExternalTaskId(record.getProviderTaskId()).setOutputText(record.getOutputText())
                .setOutputData(record.getOutputData()).setOutputAssetId(assetId).setOutputAssetType(assetId == null ? null : record.getGenerateType()).setProgress(100)).getCheckedData();
        billingApi.confirmFreeze(new AigcBillingConfirmReqDTO().setFreezeId(record.getFreezeId()).setTaskId(record.getTaskId()).setActualAmount(record.getPriceAmount())
                .setModelId(record.getModelId()).setProviderId(record.getProviderId()).setPriceSnapshot(record.getInputParams())).getCheckedData();
        modelApi.recordUsage(new AigcModelUsageRecordReqDTO().setTaskId(record.getTaskId()).setUserId(record.getUserId()).setModelId(record.getModelId()).setProviderId(record.getProviderId())
                .setCapability(record.getGenerateMode()).setRequestId(record.getGenerateNo()).setExternalTaskId(record.getProviderTaskId()).setPromptTokens(resp.getPromptTokens())
                .setCompletionTokens(resp.getCompletionTokens()).setTotalTokens(resp.getTotalTokens()).setCostPrice(record.getCostAmount()).setSalePrice(record.getPriceAmount()).setCurrencyType("POINT")
                .setStatus(0).setDurationMillis(resp.getDurationMillis())).getCheckedData();
        recordMetric("aigc_gen_success_total");
    }

    private Long createAssetIfNecessary(AigcGenerateRecordDO record) {
        if (!FILE_TYPES.contains(record.getGenerateType()) || record.getOutputUrls() == null || record.getOutputUrls().isBlank()) {
            return null;
        }
        String url = firstUrl(record.getOutputUrls());
        boolean dataUrl = StrUtil.startWithIgnoreCase(url, "data:");
        if (!dataUrl && !AigcGenerateFileSecurityUtils.isSafeRemoteUrl(url)) {
            throw exception(GENERATE_PROVIDER_RESULT_INVALID);
        }
        AigcAssetCreateReqDTO reqDTO = new AigcAssetCreateReqDTO().setUserId(record.getUserId()).setAssetType(record.getGenerateType()).setSourceType("GENERATE").setBizType("TASK")
                .setBizId(record.getGenerateNo()).setTaskId(record.getTaskId()).setModelId(record.getModelId()).setProviderId(record.getProviderId()).setTitle(record.getGenerateType() + "生成资产")
                .setPromptSnapshot(buildPromptSnapshot(record.getPrompt())).setGenerateSnapshot(record.getInputParams()).setVisibility("PRIVATE").setAuditStatus("PENDING");
        AigcModelProviderRespDTO provider = record.getProviderId() == null ? null : modelApi.getProvider(record.getProviderId()).getCheckedData();
        if (provider != null) {
            reqDTO.setProxyEnabled(provider.getProxyEnabled()).setProxyProtocol(provider.getProxyProtocol()).setProxyHost(provider.getProxyHost()).setProxyPort(provider.getProxyPort())
                    .setProxyUsername(provider.getProxyUsername()).setProxyPassword(provider.getProxyPassword());
        }
        if (dataUrl) {
            reqDTO.setOriginUrl(url);
        } else {
            reqDTO.setOriginUrl(url);
        }
        AigcAssetCreateRespDTO asset = switch (record.getGenerateType()) {
            case "IMAGE" -> assetApi.createImageAsset(reqDTO).getCheckedData();
            case "VIDEO" -> assetApi.createVideoAsset(reqDTO).getCheckedData();
            case "AUDIO" -> assetApi.createAudioAsset(reqDTO).getCheckedData();
            case "DOCUMENT" -> assetApi.createDocumentAsset(reqDTO).getCheckedData();
            default -> assetApi.createAsset(reqDTO).getCheckedData();
        };
        generateRecordMapper.updateById(new AigcGenerateRecordDO().setId(record.getId()).setAssetIds("[" + asset.getId() + "]"));
        return asset.getId();
    }

    private String buildPromptSnapshot(String prompt) {
        if (StrUtil.isBlank(prompt)) {
            return null;
        }
        return JsonUtils.toJsonString(Map.of("prompt", prompt));
    }

    private void fillDataUrlAssetFile(AigcAssetCreateReqDTO reqDTO, String dataUrl) {
        int commaIndex = dataUrl.indexOf(',');
        if (commaIndex <= 5 || !dataUrl.substring(0, commaIndex).contains(";base64")) {
            throw exception(GENERATE_PROVIDER_RESULT_INVALID);
        }
        String mimeType = dataUrl.substring("data:".length(), dataUrl.indexOf(";base64"));
        byte[] content;
        try {
            content = Base64.getDecoder().decode(dataUrl.substring(commaIndex + 1));
        } catch (IllegalArgumentException ex) {
            throw exception(GENERATE_PROVIDER_RESULT_INVALID);
        }
        if (content.length == 0) {
            throw exception(GENERATE_PROVIDER_RESULT_INVALID);
        }
        String fileExt = fileExtFromMimeType(mimeType);
        String fileUrl = fileApi.createFile(content, recordFileName(reqDTO, fileExt), "aigc/asset", mimeType);
        reqDTO.setFileUrl(fileUrl).setMimeType(mimeType).setFileExt(fileExt).setFileSize((long) content.length);
    }

    private String recordFileName(AigcAssetCreateReqDTO reqDTO, String fileExt) {
        String title = StrUtil.blankToDefault(reqDTO.getTitle(), "aigc-asset").replaceAll("[\\\\/:*?\"<>|\\s]+", "-");
        String bizId = StrUtil.blankToDefault(reqDTO.getBizId(), "task-" + reqDTO.getTaskId());
        return title + "-" + bizId + "-" + IdUtil.getSnowflakeNextIdStr() + "." + fileExt;
    }

    private String fileExtFromMimeType(String mimeType) {
        if (StrUtil.equalsIgnoreCase(mimeType, "image/jpeg")) {
            return "jpg";
        }
        if (StrUtil.startWithIgnoreCase(mimeType, "image/")) {
            return StrUtil.subAfter(mimeType, "image/", true);
        }
        if (StrUtil.startWithIgnoreCase(mimeType, "video/")) {
            return StrUtil.subAfter(mimeType, "video/", true);
        }
        if (StrUtil.startWithIgnoreCase(mimeType, "audio/")) {
            return StrUtil.subAfter(mimeType, "audio/", true);
        }
        return "bin";
    }

    private void failRecord(AigcGenerateRecordDO record, String failCode, String failReason) {
        generateRecordMapper.updateById(new AigcGenerateRecordDO().setId(record.getId()).setStatus(AigcGenerateStatusEnum.FAILED.getCode()).setFailReason(failCode).setFailMessage(failReason).setFinishTime(LocalDateTime.now()));
        recordMetric("aigc_gen_failed_total");
        taskApi.markFailed(new AigcTaskStatusUpdateReqDTO().setTaskId(record.getTaskId()).setFailCode(failCode).setFailReason(failReason)).getCheckedData();
        if (record.getFreezeId() != null) {
            billingApi.releaseFreeze(new AigcBillingReleaseReqDTO().setFreezeId(record.getFreezeId()).setTaskId(record.getTaskId()).setReason(failReason)).getCheckedData();
            taskApi.markRefunded(record.getTaskId()).getCheckedData();
        }
    }

    private String generateGenerateNo() {
        return "GEN" + IdUtil.getSnowflakeNextIdStr();
    }

    private String resolveProviderCode(AigcModelProviderRespDTO provider) {
        if (provider != null && provider.getCode() != null) {
            return provider.getCode();
        }
        return "mock";
    }

    private void recordMetric(String name) {
        recordMetric(name, 1D);
    }

    private void recordMetric(String name, double amount) {
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();
        if (meterRegistry != null) {
            counter(meterRegistry, name).increment(amount);
        }
    }

    private Counter counter(MeterRegistry meterRegistry, String name) {
        return Counter.builder(name).register(meterRegistry);
    }

    private String maskPrompt(String prompt) {
        if (prompt == null) {
            return null;
        }
        return prompt.length() <= 16 ? prompt : prompt.substring(0, 16) + "***";
    }

    private String buildRequestSummary(AigcGenerateRecordDO record) {
        return new JSONObject()
                .set("generateMode", record.getGenerateMode())
                .set("modelCode", record.getModelCode())
                .set("prompt", maskPrompt(record.getPrompt()))
                .toString();
    }

    private String buildResponseSummary(AigcProviderSubmitRespDTO resp) {
        return new JSONObject()
                .set("providerStatus", resp.getProviderStatus())
                .set("errorCode", resp.getErrorCode())
                .set("errorMessage", resp.getErrorMessage())
                .toString();
    }

    private String firstUrl(String outputUrls) {
        if (JSONUtil.isTypeJSONArray(outputUrls)) {
            return JSONUtil.parseArray(outputUrls).getStr(0, "");
        }
        return outputUrls.replace("[", "").replace("]", "").replace("\"", "").trim();
    }
}
