package com.devebot.opflow;

import com.devebot.opflow.OpflowLogTracer.Level;
import com.devebot.opflow.exception.OpflowBootstrapException;
import com.devebot.opflow.exception.OpflowOperationException;
import com.devebot.opflow.exception.OpflowRestrictionException;
import com.devebot.opflow.supports.OpflowObjectTree;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author acegik
 */
public class OpflowRpcHttpMaster {
    private final static OpflowConstant CONST = OpflowConstant.CURRENT();
    private final static Logger LOG = LoggerFactory.getLogger(OpflowRpcHttpMaster.class);
    private final static MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    
    private final String componentId;
    private final OpflowLogTracer logTracer;
    private final OpflowPromMeasurer measurer;
    private final OpflowRpcObserver rpcObserver;
    private final OpflowDiscoveryClient discoveryClient;
    private final OpflowRestrictor.Valve restrictor;
    
    private long readTimeout;
    private long writeTimeout;
    private long callTimeout;
    
    private OkHttpClient httpClient = null;
    private final Object httpClientLock = new Object();
    private final boolean autorun;
    private final boolean testException;
    
    public OpflowRpcHttpMaster(Map<String, Object> params) throws OpflowBootstrapException {
        params = OpflowObjectTree.ensureNonNull(params);
        
        componentId = OpflowUtil.getOptionField(params, CONST.COMPONENT_ID, true);
        measurer = (OpflowPromMeasurer) OpflowUtil.getOptionField(params, OpflowConstant.COMP_MEASURER, OpflowPromMeasurer.NULL);
        rpcObserver = (OpflowRpcObserver) OpflowUtil.getOptionField(params, OpflowConstant.COMP_RPC_OBSERVER, null);
        discoveryClient = (OpflowDiscoveryClient) OpflowUtil.getOptionField(params, OpflowConstant.COMP_DISCOVERY_CLIENT, null);
        restrictor = new OpflowRestrictor.Valve();
        
        readTimeout = OpflowObjectTree.getOptionValue(params, "readTimeout", Long.class, 20000l);
        writeTimeout = OpflowObjectTree.getOptionValue(params, "writeTimeout", Long.class, 20000l);
        callTimeout = OpflowObjectTree.getOptionValue(params, "callTimeout", Long.class, 180000l);
        
        logTracer = OpflowLogTracer.ROOT.branch("httpMasterId", componentId);
        
        if (logTracer.ready(LOG, Level.INFO)) LOG.info(logTracer
                .text("httpMaster[${httpMasterId}][${instanceId}].new()")
                .stringify());
        
        if (params.get(OpflowConstant.OPFLOW_COMMON_AUTORUN) instanceof Boolean) {
            autorun = (Boolean) params.get(OpflowConstant.OPFLOW_COMMON_AUTORUN);
        } else {
            autorun = false;
        }
        
        if (params.get("testException") instanceof Boolean) {
            testException = (Boolean) params.get("testException");
        } else {
            testException = false;
        }
        
        if (logTracer.ready(LOG, Level.INFO)) LOG.info(logTracer
                .text("httpMaster[${httpMasterId}][${instanceId}].new() end!")
                .stringify());
        
        // assertions
        assert discoveryClient != null;
        
        if (autorun) {
            this.serve();
        }
    }
    
    public final void serve() {
    }
    
    public void close() {
    }
    
    public void reset() {
        close();
        serve();
    }
    
    public Session request(final String routineSignature, final String body, final OpflowRpcParameter params, final Map<String, Object> options) {
        return request(routineSignature, body != null ? body.getBytes() : null, params, options);
    }
    
    public Session request(final String routineSignature, final byte[] body, final OpflowRpcParameter params, final Map<String, Object> options) {
        if (restrictor == null) {
            return _request_safe(routineSignature, body, params, options);
        }
        try {
            return restrictor.filter(new OpflowRestrictor.Action<Session>() {
                @Override
                public Session process() throws Throwable {
                    return _request_safe(routineSignature, body, params, options);
                }
            });
        }
        catch (OpflowOperationException opflowException) {
            throw opflowException;
        }
        catch (Throwable e) {
            throw new OpflowRestrictionException(e);
        }
    }
    
    private Session _request_safe(final String routineSignature, final byte[] body, final OpflowRpcParameter parameter, final Map<String, Object> options) {
        final OpflowRpcParameter params = (parameter != null) ? parameter : new OpflowRpcParameter(options);
        
        if (routineSignature != null) {
            params.setRoutineSignature(routineSignature);
        }
        
        final OpflowLogTracer reqTracer = logTracer.branch(CONST.REQUEST_TIME, params.getRoutineTimestamp())
                .branch(CONST.REQUEST_ID, params.getRoutineId(), params);
        
        if (reqTracer != null && reqTracer.ready(LOG, Level.DEBUG)) {
            LOG.debug(reqTracer
                    .text("Request[${requestId}][${requestTime}][x-http-master-request] - httpMaster[${httpMasterId}][${instanceId}] - make a request")
                    .stringify());
        }
        
        OkHttpClient client = assertHttpClient();
        
        Request.Builder reqBuilder = new Request.Builder()
            .header(OpflowConstant.HTTP_HEADER_ROUTINE_ID, params.getRoutineId())
            .header(OpflowConstant.HTTP_HEADER_ROUTINE_TIMESTAMP, params.getRoutineTimestamp())
            .header(OpflowConstant.HTTP_HEADER_ROUTINE_SIGNATURE, params.getRoutineSignature());
        
        if (params.getRoutineScope() != null) {
            reqBuilder = reqBuilder.header(OpflowConstant.HTTP_HEADER_ROUTINE_SCOPE, params.getRoutineScope());
        }
        
        OpflowDiscoveryClient.Info info = discoveryClient.locate();
        if (info == null || info.getUri() == null) {
            return Session.asBroken(params);
        }
        
        reqBuilder.url(info.getUri());
        
        if (body != null) {
            RequestBody reqBody = RequestBody.create(body, JSON);
            reqBuilder = reqBuilder.post(reqBody);
        }
        
        Request request = reqBuilder.build();
        
        Call call = client.newCall(request);
        
        Session session = null;
        
        try {
            Response response = call.execute();
            if (testException) {
                throw new IOException(reqTracer.text("Request[${requestId}][${requestTime}] - throw a testing exception").stringify());
            }
            if (response.isSuccessful()) {
                session = Session.asOk(params, response.body().string());
                if (reqTracer != null && reqTracer.ready(LOG, Level.DEBUG)) {
                    LOG.debug(reqTracer
                            .put("protocol", response.protocol().toString())
                            .put("statusCode", response.code())
                            .text("Request[${requestId}][${requestTime}][x-http-master-response-ok] - httpMaster[${httpMasterId}][${instanceId}] - statusCode ${statusCode}")
                            .stringify());
                }
            } else {
                session = Session.asFailed(params, response.body().string());
                if (reqTracer != null && reqTracer.ready(LOG, Level.DEBUG)) {
                    LOG.debug(reqTracer
                            .put("protocol", response.protocol().toString())
                            .put("statusCode", response.code())
                            .text("Request[${requestId}][${requestTime}][x-http-master-response-failed] - httpMaster[${httpMasterId}][${instanceId}] - statusCode ${statusCode}")
                            .stringify());
                }
            }
        }
        catch (SocketTimeoutException exception) {
            session = Session.asTimeout(params, exception);
            if (reqTracer != null && reqTracer.ready(LOG, Level.ERROR)) {
                LOG.error(reqTracer
                        .put("exceptionName", exception.getClass().getName())
                        .text("Request[${requestId}][${requestTime}][x-http-master-response-rwTimeout] - httpMaster[${httpMasterId}][${instanceId}] - readTimeout/writeTimeout")
                        .stringify());
            }
        }
        catch (InterruptedIOException exception) {
            session = Session.asTimeout(params, exception);
            if (reqTracer != null && reqTracer.ready(LOG, Level.ERROR)) {
                LOG.error(reqTracer
                        .put("exceptionName", exception.getClass().getName())
                        .text("Request[${requestId}][${requestTime}][x-http-master-response-callTimeout] - httpMaster[${httpMasterId}][${instanceId}] - callTimeout")
                        .stringify());
            }
        }
        catch (IOException exception) {
            session = Session.asCracked(params, exception);
            if (reqTracer != null && reqTracer.ready(LOG, Level.ERROR)) {
                LOG.error(reqTracer
                        .put("exceptionName", exception.getClass().getName())
                        .text("Request[${requestId}][${requestTime}][x-http-master-response-cracked] - httpMaster[${httpMasterId}][${instanceId}] - Exception ${exceptionName}")
                        .stringify());
            }
        }
        
        return session;
    }
    
    private OkHttpClient assertHttpClient() {
        if (httpClient == null) {
            synchronized (httpClientLock) {
                if (httpClient == null) {
                    httpClient = new OkHttpClient.Builder()
                        .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
                        .writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)
                        .callTimeout(callTimeout, TimeUnit.MILLISECONDS)
                        .build();
                }
            }
        }
        return httpClient;
    }
    
    public static class Session {
        
        public static enum STATUS { OK, BROKEN, CRACKED, FAILED, TIMEOUT }
        
        private final STATUS status;
        private final String value;
        private final String error;
        private final Exception exception;

        public Session(OpflowRpcParameter params, STATUS status, String value, String error, Exception exception) {
            this.status = status;
            this.value = value;
            this.error = error;
            this.exception = exception;
        }
        
        public static Session asOk(OpflowRpcParameter params, String value) {
            return new Session(params, STATUS.OK, value, null, null);
        }
        
        public static Session asBroken(OpflowRpcParameter params) {
            return new Session(params, STATUS.BROKEN, null, null, null);
        }
        
        public static Session asCracked(OpflowRpcParameter params, Exception exception) {
            return new Session(params, STATUS.CRACKED, null, null, exception);
        }
        
        public static Session asFailed(OpflowRpcParameter params, String error) {
            return new Session(params, STATUS.FAILED, null, error, null);
        }
        
        public static Session asTimeout(OpflowRpcParameter params, Exception exception) {
            return new Session(params, STATUS.TIMEOUT, null, null, exception);
        }
        
        public boolean isOk() {
            return status == STATUS.OK;
        }
        
        public boolean isFailed() {
            return status == STATUS.FAILED;
        }
        
        public boolean isCracked() {
            return status == STATUS.CRACKED;
        }
        
        public boolean isTimeout() {
            return status == STATUS.TIMEOUT;
        }
        
        public String getValueAsString() {
            return this.value;
        }
        
        public String getErrorAsString() {
            return this.error;
        }
        
        public Exception getException() {
            return this.exception;
        }
    }
}