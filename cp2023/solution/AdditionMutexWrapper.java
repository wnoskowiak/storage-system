package cp2023.solution;

import java.util.concurrent.Semaphore;

public class AdditionMutexWrapper {

    public final Semaphore canStartPrep;
    public final Semaphore canStartPerf;

    public AdditionMutexWrapper(Semaphore canStartPrep, Semaphore canStartPerf) {
        this.canStartPrep = canStartPrep;
        this.canStartPerf = canStartPerf;
    }

}
