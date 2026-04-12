package com.monitoring.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DbQueryEventDto extends EventDto {
    
    private String queryType;
    private String table;
    private Integer latencyMs;
    private Integer rowsScanned;
    private Integer connectionPoolUsed;
    private Integer connectionPoolMax;
    private Integer slowQueryThresholdMs;
    
    public DbQueryEventDto() {
        super();
        setEventType(EventType.DB_QUERY);
    }
    
    public String getQueryType() {
        return queryType;
    }
    
    public void setQueryType(String queryType) {
        this.queryType = queryType;
    }
    
    public String getTable() {
        return table;
    }
    
    public void setTable(String table) {
        this.table = table;
    }
    
    public Integer getLatencyMs() {
        return latencyMs;
    }
    
    public void setLatencyMs(Integer latencyMs) {
        this.latencyMs = latencyMs;
    }
    
    public Integer getRowsScanned() {
        return rowsScanned;
    }
    
    public void setRowsScanned(Integer rowsScanned) {
        this.rowsScanned = rowsScanned;
    }
    
    public Integer getConnectionPoolUsed() {
        return connectionPoolUsed;
    }
    
    public void setConnectionPoolUsed(Integer connectionPoolUsed) {
        this.connectionPoolUsed = connectionPoolUsed;
    }
    
    public Integer getConnectionPoolMax() {
        return connectionPoolMax;
    }
    
    public void setConnectionPoolMax(Integer connectionPoolMax) {
        this.connectionPoolMax = connectionPoolMax;
    }
    
    public Integer getSlowQueryThresholdMs() {
        return slowQueryThresholdMs;
    }
    
    public void setSlowQueryThresholdMs(Integer slowQueryThresholdMs) {
        this.slowQueryThresholdMs = slowQueryThresholdMs;
    }
}
