package model;

/**
 * 記錄CPU核心執行程序片段的歷史軌跡 (Domain Event)
 *
 * @param coreName       執行此片段的核心名稱
 * @param processId      被執行的程序ID
 * @param startTime      此片段的起始時間 (TU)
 * @param endTime        此片段的結束時間 (TU)
 * @param executionCount 該程序被執行次數
 */
public record EventRecord(
        String coreName,
        String processId,
        int startTime,
        int endTime,
        int executionCount
) {
    @Override
    public String toString() {
        return String.format("[%s] %s (#%d) : %d -> %d",
                coreName, processId, executionCount, startTime, endTime);
    }
}