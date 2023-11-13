package cp2023.solution;

import java.util.concurrent.Semaphore;

import cp2023.base.ComponentTransfer;

public class QueueElem {

    public final Semaphore mutex = new Semaphore(0);
    public final ComponentTransfer transfer;
    public boolean deleted = false;
    public final Semaphore reservation = new Semaphore(1);

    public QueueElem(ComponentTransfer transfer) {
        this.transfer = transfer;
    }
}
