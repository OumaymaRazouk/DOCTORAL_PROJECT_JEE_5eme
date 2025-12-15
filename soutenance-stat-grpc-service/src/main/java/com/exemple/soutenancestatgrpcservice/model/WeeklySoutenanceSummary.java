package com.exemple.soutenancestatgrpcservice.model;

import java.util.List;

public class WeeklySoutenanceSummary {
    private String weekStart;
    private String weekEnd;
    private long total;
    private List<SoutenanceView> soutenances;

    public String getWeekStart() { return weekStart; }
    public void setWeekStart(String weekStart) { this.weekStart = weekStart; }

    public String getWeekEnd() { return weekEnd; }
    public void setWeekEnd(String weekEnd) { this.weekEnd = weekEnd; }

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public List<SoutenanceView> getSoutenances() { return soutenances; }
    public void setSoutenances(List<SoutenanceView> soutenances) { this.soutenances = soutenances; }
}
