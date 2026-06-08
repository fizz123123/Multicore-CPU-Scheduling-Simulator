package engine;

import model.EventRecord;
import model.Process;
import model.SimulationConfig;
import model.SimulationResult;
import scheduler.Scheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

public class SimulationEngine {
    private final SimulationConfig config;
    private final Scheduler scheduler;
    private final SimulationClock clock;
    private final ConcurrentLinkedQueue<EventRecord> eventLog;
    private final CountDownLatch latch;
    private final CpuCorePool corePool;
    private final List<Process> processes;

    public SimulationEngine(SimulationConfig config, Scheduler scheduler) {
        this.config = config;
        this.scheduler = scheduler;
        this.clock = new SimulationClock(config.timeMultiplier());
        this.eventLog = new ConcurrentLinkedQueue<>();
        this.latch = new CountDownLatch(config.totalProcesses());
        this.corePool = new CpuCorePool(config.coreCount());
        this.processes = Collections.synchronizedList(new ArrayList<>());
    }

    public SimulationResult run() {
        startSimulation();
        return waitForCompletion();
    }

    private void startSimulation() {
        clock.start();

        //1.啟動CPU核心
        for (int i = 1; i <= config.coreCount(); i++) {
            CpuCore core = new CpuCore("Core-" + i, scheduler, eventLog, latch, clock);
            corePool.startCore(core);
        }
        //2.啟動任務產生器
        ProcessGenerator generator = new ProcessGenerator(config, scheduler, clock, processes);
        Thread generatorThread = new Thread(generator, "ProcessGenerator");
        generatorThread.start();
    }

    private SimulationResult waitForCompletion() {
        int totalTime;
        double totalTAT = 0;
        double totalWT = 0;

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }

        totalTime = clock.getCurrentTU();
        clock.stop();
        corePool.shutdownNowAndAwaitTermination();

        for (Process p : processes) {
            double currentTAT = p.getCompletionTime() - p.getArrivalTime();
            double currentWT = currentTAT - p.getBurstTime();

            totalTAT += currentTAT;
            totalWT += currentWT;
        }

        int totalProcesses = config.totalProcesses();
        if (totalProcesses > 0) {
            double avgWT = totalWT / totalProcesses;
            double avgTAT = totalTAT / totalProcesses;
            return new SimulationResult(eventLog, new ArrayList<>(processes), avgWT, avgTAT, totalTime);
        }

        return new SimulationResult(eventLog, new ArrayList<>(processes), 0, 0, totalTime);
    }
}
