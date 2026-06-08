package engine;

import model.EventRecord;
import model.Process;
import model.ProcessState;
import scheduler.Scheduler;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

public class CpuCore implements Runnable {
    private final String coreName;
    private final Scheduler scheduler;
    private final ConcurrentLinkedQueue<EventRecord> eventLog;
    private final CountDownLatch latch;
    private final SimulationClock clock;

    public CpuCore(
            String coreName,
            Scheduler scheduler,
            ConcurrentLinkedQueue<EventRecord> eventLog,
            CountDownLatch latch,
            SimulationClock clock) {

        this.coreName = coreName;
        this.scheduler = scheduler;
        this.eventLog = eventLog;
        this.latch = latch;
        this.clock = clock;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            //1.跟Scheduler拿任務
            Process process = scheduler.getNextProcess();
            if (process == null) {
                continue;
            }

            try {
                //2.計算這次要跑多久
                int runTime = Math.min(process.getRemainingTime(), scheduler.getTimeSlice());

                //3.透過Clock取得開始時間(TU)
                int startTime = clock.getCurrentTU();

                if (process.getStartTime() == -1) {
                    process.setStartTime(startTime);
                }

                process.setState(ProcessState.RUNNING);

                //4.模擬CPU運算
                Thread.sleep((long) runTime * clock.getTimeMultiplier());

                //5.運算結束，透過Clock取得結束時間(TU)，並扣除剩餘時間
                int endTime = clock.getCurrentTU();
                process.setRemainingTime(process.getRemainingTime() - runTime);

                //6.將運行紀錄寫入eventLog
                EventRecord newRecord = new EventRecord(
                        coreName,
                        process.getProcessId(),
                        startTime,
                        endTime,
                        process.incrementAndGetRunCount()
                );
                eventLog.add(newRecord);

                //7.Context Switch判斷
                if (process.getRemainingTime() > 0) {
                    //7.1 時間切片用完但還沒跑完，重新塞回就緒佇列
                    scheduler.addProcess(process);
                } else {
                    //7.2 徹底跑完，結算完成時間，更改狀態，並countDown
                    process.setCompletionTime(endTime);
                    process.setState(ProcessState.TERMINATED);
                    latch.countDown();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
