package scheduler;

import model.Process;

public interface Scheduler {
    /**
     * 當系統產生新的程序時，透過此方法把它加入 "就緒佇列(Ready Queue)"
     *
     * @param process 要加入的程序
     */
    void addProcess(Process process);

    /**
     * 當CPU核心閒置時，呼叫這個方法來取得下一個要執行的程序
     *
     * @return 要取出的程序
     */
    Process getNextProcess();

    /**
     * 檢查當前排程器的就緒佇列中，是否還有程序在排隊
     */
    boolean hasPendingProcesses();

    /**
     * 取得時間切片的大小
     */
    int getTimeSlice();

}
