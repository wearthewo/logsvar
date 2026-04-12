package com.monitoring.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExceptionEventDto extends EventDto {
    
    private String errorType;
    private String message;
    private String stackTrace;
    private String threadName;
    private Integer occurrences;
    
    public ExceptionEventDto() {
        super();
        setEventType(EventType.EXCEPTION);
    }
    
    public String getErrorType() {
        return errorType;
    }
    
    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getStackTrace() {
        return stackTrace;
    }
    
    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }
    
    public String getThreadName() {
        return threadName;
    }
    
    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }
    
    public Integer getOccurrences() {
        return occurrences;
    }
    
    public void setOccurrences(Integer occurrences) {
        this.occurrences = occurrences;
    }
}
