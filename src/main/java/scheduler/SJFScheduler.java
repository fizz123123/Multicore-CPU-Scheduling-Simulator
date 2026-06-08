package scheduler;

import model.Process;
import model.ProcessState;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

public class SJFScheduler implements Scheduler {
    private final PriorityBlockingQueue<Process> readyQueue;

    public SJFScheduler() {
        this.readyQueue = new PriorityBlockingQueue<>(
                11,
                Comparator
                        .comparingInt(Process::getBurstTime)
                        .thenComparingInt(Process::getArrivalTime)
                        .thenComparing(Process::getProcessId)
        );
    }

    @Override
    public void addProcess(Process process) {
        process.setState(ProcessState.READY);
        readyQueue.offer(process);
    }

    @Override
    public Process getNextProcess() {
        try {
            return readyQueue.poll(50, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    @Override
    public boolean hasPendingProcesses() {
        return !readyQueue.isEmpty();
    }

    @Override
    public int getTimeSlice() {
        return Integer.MAX_VALUE;
    }
}
