package cp2023.solution;

import java.util.concurrent.Semaphore;

public class TransferMutexWrapper {
    
    public final LinkedListWrapper cycleRemainder;
    public final Semaphore canStartPrep;
    public final Semaphore canStartPerf;

    public TransferMutexWrapper(LinkedListWrapper cycleRemainder, Semaphore canStartPrep, Semaphore canStartPerf) {
        this.cycleRemainder = cycleRemainder;
        this.canStartPrep = canStartPrep;
        this.canStartPerf = canStartPerf;
    }

}
