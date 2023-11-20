package cp2023.solution;

import java.util.concurrent.Semaphore;

public class QueueElement {

    public final Semaphore waitForCondition = new Semaphore(0);
    public final Semaphore doneWithPrepare = new Semaphore(0);
    public LinkedListWrapper cycleRemainders = new LinkedListWrapper(null);
    
}
