package engine;

public class SimulationClock {
    private final int timeMultiplier;
    private volatile long startTimeMillis;
    private volatile boolean isRunning;

    public SimulationClock(int timeMultiplier) {
        this.timeMultiplier = timeMultiplier;
        this.startTimeMillis = 0;
        this.isRunning = false;
    }

    public int getTimeMultiplier() {
        return timeMultiplier;
    }

    public void start() {
        startTimeMillis = System.currentTimeMillis();
        isRunning = true;
    }

    public void stop() {
        isRunning = false;
    }

    public void reset() {
        startTimeMillis = 0;
        isRunning = false;
    }

    /**
     * 取得當前虛擬時間(TU)
     * <p>
     * 公式：TU = 真實世界流逝的毫秒數 / timeMultiplier
     *
     * @return 當前模擬時間單位（Time Unit）
     */
    public int getCurrentTU() {
        if (!isRunning) {
            return 0;
        } else {
            return (int) ((System.currentTimeMillis() - startTimeMillis) / timeMultiplier);
        }
    }
}
