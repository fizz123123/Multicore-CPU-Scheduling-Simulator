package model;

import java.util.List;

/**
 * 模擬器設定檔
 *
 * @param coreCount          CPU核心數量
 * @param timeMultiplier     時間倍率
 * @param timeSlice          時間切片
 * @param isRandomMode       是否為隨機模式 (true = 隨機滑桿, false = 自訂表單)
 * @param randomProcessCount 隨機模式的程序數量 (10 ~ 30)
 * @param randomSeed         隨機亂數種子
 * @param customProcesses    自訂模式下程序清單 (3 ~ 20)
 */
public record SimulationConfig(
        int coreCount,
        int timeMultiplier,
        int timeSlice,
        boolean isRandomMode,
        int randomProcessCount,
        Long randomSeed,
        List<Process> customProcesses
) {
    public int totalProcesses() {
        return isRandomMode ? randomProcessCount : customProcesses.size();
    }
}
