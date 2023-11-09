package cp2023.solution;

import java.util.concurrent.Semaphore;

import cp2023.base.ComponentTransfer;

public class QueueElem {

    public final Semaphore mutex = new Semaphore(0);
    public final ComponentTransfer transfer;

    public QueueElem(ComponentTransfer transfer) {
        this.transfer = transfer;
    }
}
