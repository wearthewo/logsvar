package com.monitoring.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class HttpRequestEventDto extends EventDto {
    
    private HttpMethod method;
    private String endpoint;
    private Integer statusCode;
    private Integer latencyMs;
    private String userId;
    private Integer requestSize;
    private Integer responseSize;
    
    public HttpRequestEventDto() {
        super();
        setEventType(EventType.HTTP_REQUEST);
    }
    
    public HttpMethod getMethod() {
        return method;
    }
    
    public void setMethod(HttpMethod method) {
        this.method = method;
    }
    
    public String getEndpoint() {
        return endpoint;
    }
    
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
    
    public Integer getStatusCode() {
        return statusCode;
    }
    
    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }
    
    public Integer getLatencyMs() {
        return latencyMs;
    }
    
    public void setLatencyMs(Integer latencyMs) {
        this.latencyMs = latencyMs;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public Integer getRequestSize() {
        return requestSize;
    }
    
    public void setRequestSize(Integer requestSize) {
        this.requestSize = requestSize;
    }
    
    public Integer getResponseSize() {
        return responseSize;
    }
    
    public void setResponseSize(Integer responseSize) {
        this.responseSize = responseSize;
    }
    
    public enum HttpMethod {
        GET, POST, PUT, DELETE
    }
}
