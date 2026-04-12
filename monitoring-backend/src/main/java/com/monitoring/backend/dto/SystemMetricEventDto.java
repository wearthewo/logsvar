package com.monitoring.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SystemMetricEventDto extends EventDto {
    
    private BigDecimal cpuPercent;
    private Integer memoryUsedMb;
    private Integer memoryTotalMb;
    private Integer activeThreads;
    private Integer gcPauseMs;
    private BigDecimal diskReadMbps;
    
    public SystemMetricEventDto() {
        super();
        setEventType(EventType.SYSTEM_METRIC);
    }
    
    public BigDecimal getCpuPercent() {
        return cpuPercent;
    }
    
    public void setCpuPercent(BigDecimal cpuPercent) {
        this.cpuPercent = cpuPercent;
    }
    
    public Integer getMemoryUsedMb() {
        return memoryUsedMb;
    }
    
    public void setMemoryUsedMb(Integer memoryUsedMb) {
        this.memoryUsedMb = memoryUsedMb;
    }
    
    public Integer getMemoryTotalMb() {
        return memoryTotalMb;
    }
    
    public void setMemoryTotalMb(Integer memoryTotalMb) {
        this.memoryTotalMb = memoryTotalMb;
    }
    
    public Integer getActiveThreads() {
        return activeThreads;
    }
    
    public void setActiveThreads(Integer activeThreads) {
        this.activeThreads = activeThreads;
    }
    
    public Integer getGcPauseMs() {
        return gcPauseMs;
    }
    
    public void setGcPauseMs(Integer gcPauseMs) {
        this.gcPauseMs = gcPauseMs;
    }
    
    public BigDecimal getDiskReadMbps() {
        return diskReadMbps;
    }
    
    public void setDiskReadMbps(BigDecimal diskReadMbps) {
        this.diskReadMbps = diskReadMbps;
    }
}
