package model;

/**
 * 模擬器設定檔
 *
 * @param coreCount      CPU核心數量
 * @param totalProcesses 總程序數量
 * @param timeMultiplier 時間倍率
 * @param timeSlice      時間切片
 */
public record SimulationConfig(
        int coreCount,
        int totalProcesses,
        int timeMultiplier,
        int timeSlice
) {
}
