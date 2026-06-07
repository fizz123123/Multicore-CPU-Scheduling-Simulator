package engine;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CpuCorePool {
    private final int coreCount;
    private final ExecutorService executor;

    public CpuCorePool(int coreCount) {
        this.coreCount = coreCount;

        AtomicInteger counter = new AtomicInteger(1);

        this.executor = Executors.newFixedThreadPool(coreCount, r -> {
            Thread thread = new Thread(r, "Core-" + counter.getAndIncrement());
            return thread;
        });
    }

    /**
     * 將CPU核心加入執行緒池執行
     */
    public void startCore(CpuCore core) {
        executor.submit(core);
    }

    /**
     * 強制中斷所有CPU核心運作
     */
    public void shutdownNowAndAwaitTermination() {
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                System.err.println("部分CPU核心未能正常關閉");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int getCoreCount() {
        return coreCount;
    }
}
