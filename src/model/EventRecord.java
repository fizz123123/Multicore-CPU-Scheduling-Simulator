package model;

public class EventRecord {
    private final String coreName;
    private final String processId;
    private final int startTime;
    private final int endTime;

    public EventRecord(String coreName, String processId, int startTime, int endTime) {
        this.coreName = coreName;
        this.processId = processId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getCoreName() {
        return coreName;
    }

    public String getProcessId() {
        return processId;
    }

    public int getStartTime() {
        return startTime;
    }

    public int getEndTime() {
        return endTime;
    }

    @Override
    public String toString() {
        return "EventRecord{" +
                "coreName='" + coreName + '\'' +
                ", processId='" + processId + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}
