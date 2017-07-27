package net.acegik.jsondataflow;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author drupalex
 */
public class OpflowRpcResponse {

    final Logger logger = LoggerFactory.getLogger(OpflowRpcResponse.class);
    
    private final Channel channel;
    private final AMQP.BasicProperties properties;
    private final String queueName;
    private String requestId;
    
    public OpflowRpcResponse(Channel channel, AMQP.BasicProperties properties, String queueName) {
        this.channel = channel;
        this.properties = properties;
        this.queueName = queueName;
        if (properties != null && 
                properties.getHeaders() != null && 
                properties.getHeaders().get("requestId") != null) {
            this.requestId = properties.getHeaders().get("requestId").toString();
            if (logger.isTraceEnabled()) logger.trace("requestId: " + this.requestId);
        } else {
            if (logger.isTraceEnabled()) logger.trace("requestId is empty");
        }
    }
    
    private Map<String, Object> createHeaders(String status) {
        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put("status", status);
        if (this.requestId != null) {
            headers.put("requestId", this.requestId);
        }
        return headers;
    }
    
    public void emitStarted() {
        emitStarted("{}");
    }
    
    public void emitStarted(String content) {
        if (logger.isTraceEnabled()) {
            logger.trace("Request[" + this.requestId + "] emitStarted() - parameter: " + content);
        }
        emitStarted(OpflowUtil.getBytes(content));
    }
    
    public void emitStarted(byte[] info) {
        if (info == null) info = new byte[0];
        AMQP.BasicProperties replyProps = new AMQP.BasicProperties
            .Builder()
            .correlationId(properties.getCorrelationId())
            .headers(createHeaders("started"))
            .build();
        basicPublish(info, replyProps);
        if (logger.isTraceEnabled()) {
            logger.trace("Request[" + this.requestId + "] emitStarted() - byte[].length: " + info.length);
        }
    }
    
    public void emitProgress(int completed, int total) {
        emitProgress(completed, total, null);
    }
    
    public void emitProgress(int completed, int total, String jsonData) {
        AMQP.BasicProperties replyProps = new AMQP.BasicProperties
            .Builder()
            .correlationId(properties.getCorrelationId())
            .headers(createHeaders("progress"))
            .build();
        int percent = -1;
        if (total > 0 && completed >= 0 && completed <= total) {
            percent = (total == 100) ? completed : Math.round((completed * 100) / total);
        }
        String result;
        if (jsonData == null) {
            result = "{ \"percent\": " + percent + " }";
        } else {
            result = "{ \"percent\": " + percent + ", \"data\": " + jsonData + "}";
        }
        basicPublish(OpflowUtil.getBytes(result), replyProps);
        if (logger.isTraceEnabled()) {
            logger.trace("Request[" + this.requestId + "] emitProgress(): " + result);
        }
    }
    
    public void emitFailed(String error) {
        emitFailed(OpflowUtil.getBytes(error));
    }
    
    public void emitFailed(byte[] error) {
        if (error == null) error = new byte[0];
        AMQP.BasicProperties replyProps = new AMQP.BasicProperties
            .Builder()
            .correlationId(properties.getCorrelationId())
            .headers(createHeaders("failed"))
            .build();
        basicPublish(error, replyProps);
        if (logger.isTraceEnabled()) {
            logger.trace("Request[" + this.requestId + "] emitFailed() - byte[].length: " + error.length);
        }
    }
    
    public void emitCompleted(String result) {
        emitCompleted(OpflowUtil.getBytes(result));
    }

    public void emitCompleted(byte[] result) {
        if (result == null) result = new byte[0];
        AMQP.BasicProperties replyProps = new AMQP.BasicProperties
            .Builder()
            .correlationId(properties.getCorrelationId())
            .headers(createHeaders("completed"))
            .build();
        basicPublish(result, replyProps);
        if (logger.isTraceEnabled()) {
            logger.trace("Request[" + this.requestId + "] emitCompleted() - byte[].length: " + result.length);
        }
    }
    
    private void basicPublish(byte[] data, AMQP.BasicProperties replyProps) {
        try {
            channel.basicPublish("", queueName, replyProps, data);
        } catch (IOException exception) {
            throw new OpflowGeneralException(exception);
        }
    }
}