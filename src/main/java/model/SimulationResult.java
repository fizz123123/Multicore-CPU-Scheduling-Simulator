package model;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 模擬結果的數據傳輸物件(DTO)
 * 封裝了模擬結束後的統計指標與完整事件日誌
 *
 * @param eventLog              執行過程的完整事件日誌
 * @param processes             本次模擬的所有程序清單
 * @param averageWaitingTime    所有程序的平均等待時間
 * @param averageTurnaroundTime 所有程序的平均周轉時間
 * @param totalSimulationTime   模擬運行的總時間 (時間單位：TU)
 */
public record SimulationResult(
        ConcurrentLinkedQueue<EventRecord> eventLog,
        List<Process> processes,
        double averageWaitingTime,
        double averageTurnaroundTime,
        int totalSimulationTime
) {
}
