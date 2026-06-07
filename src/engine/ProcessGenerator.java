package engine;

import model.Process;
import model.SimulationConfig;
import scheduler.Scheduler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class ProcessGenerator implements Runnable {
    private final SimulationConfig config;
    private final Scheduler scheduler;
    private final SimulationClock clock;
    private final List<Process> generatedProcesses;
    private final Random random;

    public ProcessGenerator(SimulationConfig config, Scheduler scheduler, SimulationClock clock, List<Process> generatedProcesses) {
        this.config = config;
        this.scheduler = scheduler;
        this.clock = clock;
        this.generatedProcesses = generatedProcesses;
        //?
        this.random = config.randomSeed() != null ? new Random(config.randomSeed()) : new Random();
    }

    @Override
    public void run() {
        //1.根據模式決定程序數量
        List<Process> processes =
                config.isRandomMode() ? generateRandomProcesses() : new ArrayList<>(config.customProcesses());

        //2.排序 (Arrival Time 升序)
        processes.sort(Comparator.comparingInt(Process::getArrivalTime));

        //3.時間到了丟進去
        for (Process p : processes) {
            try {
                //需要等待的時間：(抵達時間 - 當前時間)
                int timeToWait = p.getArrivalTime() - clock.getCurrentTU();

                if (timeToWait > 0) {
                    Thread.sleep((long) timeToWait * config.timeMultiplier());
                }

                generatedProcesses.add(p);
                scheduler.addProcess(p);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }


    private List<Process> generateRandomProcesses() {
        List<Process> processes = new ArrayList<>();

        for (int i = 1; i <= config.randomProcessCount(); i++) {
            int arrivalTime = random.nextInt(21);   //Arrival Time：0 ~ 20 TU
            int burstTime = random.nextInt(10) + 1; //Burst Time：1 ~ 10 TU
            processes.add(new Process("P" + i, arrivalTime, burstTime));
        }
        return processes;
    }
}
