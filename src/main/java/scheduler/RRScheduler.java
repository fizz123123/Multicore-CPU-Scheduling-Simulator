package scheduler;

import model.Process;
import model.ProcessState;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class RRScheduler implements Scheduler {
    private final LinkedBlockingQueue<Process> readyQueue;
    private final int timeSlice;

    public RRScheduler(int timeSlice) {
        this.timeSlice = timeSlice;
        readyQueue = new LinkedBlockingQueue<>();
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
        return this.timeSlice;
    }
}
