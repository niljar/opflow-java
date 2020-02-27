package com.devebot.opflow;

import com.devebot.opflow.supports.OpflowEnvTool;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author acegik
 */
public class OpflowConstant {
    private final static OpflowEnvTool ENVTOOL = OpflowEnvTool.instance;

    public final String FRAMEWORK_ID = "opflow";

    public final String INSTANCE_ID = "instanceId";
    public final String COMPONENT_ID = "componentId";
    public final String COMPONENT_TYPE = "componentType";
    public final String RPC_MASTER_ID = "rpcMasterId";
    public final String RPC_WORKER_ID = "rpcWorkerId";

    public final String REQUEST_ID = "requestId";
    public final String REQUEST_TIME = "requestTime";

    public final String AMQP_PROTOCOL_VERSION;
    public final String AMQP_HEADER_ROUTINE_ID;
    public final String AMQP_HEADER_ROUTINE_TIMESTAMP;
    public final String AMQP_HEADER_ROUTINE_SIGNATURE;
    public final String AMQP_HEADER_ROUTINE_TAGS;

    public final String COMPNAME_COMMANDER = "commander";
    public final String COMPNAME_SERVERLET = "serverlet";
    public final String COMPNAME_PUBLISHER = "publisher";
    public final String COMPNAME_CONFIGURER = "configurer";
    public final String COMPNAME_SUBSCRIBER = "subscriber";
    public final String COMPNAME_MEASURER = "measurer";
    public final String COMPNAME_PROM_EXPORTER = "promExporter";
    public final String COMPNAME_RESTRICTOR = "restrictor";
    public final String COMPNAME_REQ_EXTRACTOR = "reqExtractor";
    public final String COMPNAME_SPEED_METER = "speedMeter";
    public final String COMPNAME_RPC_MASTER = "rpcMaster";
    public final String COMPNAME_RPC_WORKER = "rpcWorker";
    public final String COMPNAME_NATIVE_WORKER = "ReservedWorker";
    public final String COMPNAME_REMOTE_WORKER = "DetachedWorker";
    public final String COMPNAME_RPC_WATCHER = "rpcWatcher";
    public final String COMPNAME_RPC_OBSERVER = "rpcObserver";
    public final String COMPNAME_REST_SERVER = "restServer";

    public final String RPC_INVOCATION_FLOW_PUBLISHER = "publisher";
    public final String RPC_INVOCATION_FLOW_RPC_MASTER = "master";
    public final static String RPC_INVOCATION_FLOW_DETACHED_WORKER = "detached_worker";
    public final static String RPC_INVOCATION_FLOW_RESERVED_WORKER = "reserved_worker";

    public final static String REQUEST_TRACER_NAME = "reqTracer";

    private OpflowConstant() {
        AMQP_PROTOCOL_VERSION = ENVTOOL.getSystemProperty("OPFLOW_AMQP_PROTOCOL_VERSION", "0");
        switch (AMQP_PROTOCOL_VERSION) {
            case "1":
                AMQP_HEADER_ROUTINE_ID = "oxId";
                AMQP_HEADER_ROUTINE_TIMESTAMP = "oxTimestamp";
                AMQP_HEADER_ROUTINE_SIGNATURE = "oxSignature";
                AMQP_HEADER_ROUTINE_TAGS = "oxTags";
                break;
            default:
                AMQP_HEADER_ROUTINE_ID = "requestId";
                AMQP_HEADER_ROUTINE_TIMESTAMP = "requestTime";
                AMQP_HEADER_ROUTINE_SIGNATURE = "routineId";
                AMQP_HEADER_ROUTINE_TAGS = "requestTags";
                break;
        }
    }

    public Map<String, String> getProtocolInfo() {
        Map<String, String> info = new LinkedHashMap<>();
        info.put("AMQP_PROTOCOL_VERSION", AMQP_PROTOCOL_VERSION);
        info.put("AMQP_HEADER_ROUTINE_ID", AMQP_HEADER_ROUTINE_ID);
        info.put("AMQP_HEADER_ROUTINE_TIMESTAMP", AMQP_HEADER_ROUTINE_TIMESTAMP);
        info.put("AMQP_HEADER_ROUTINE_SIGNATURE", AMQP_HEADER_ROUTINE_SIGNATURE);
        info.put("AMQP_HEADER_ROUTINE_TAGS", AMQP_HEADER_ROUTINE_TAGS);
        return info;
    }
    
    private static OpflowConstant instance = null;

    public static OpflowConstant CURRENT() {
        if (instance == null) {
            synchronized (OpflowConstant.class) {
                if (instance == null) {
                    instance = new OpflowConstant();
                }
            }
        }
        return instance;
    }
}
