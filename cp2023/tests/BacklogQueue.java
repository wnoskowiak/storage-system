package cp2023.tests;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import cp2023.base.DeviceId;
import cp2023.base.ComponentId;
import cp2023.base.ComponentTransfer;
import cp2023.solution.QueueElem;
import java.util.Map;
import java.util.concurrent.Semaphore;

public class BacklogQueue {

    private final Semaphore mutex = new Semaphore(1);
    private final LinkedHashMap<ComponentId, QueueElem> queue = new LinkedHashMap<>();

    public int size() throws InterruptedException {
        mutex.acquire();
        int result = queue.size();
        mutex.release();
        return result;
    }

    public void add(ComponentId component, ComponentTransfer details) throws InterruptedException {
        QueueElem temp = new QueueElem(details);
        mutex.acquire();
        queue.put(component, temp);
        mutex.release();
    }

    public void wait(ComponentId component) throws InterruptedException {
        mutex.acquire();
        QueueElem elem = queue.get(component);
        mutex.release();
        elem.mutex.acquire();
    }

    public void remove(ComponentId component) throws InterruptedException {
        mutex.acquire();
        queue.get(component).deleted = true;
        queue.remove(component);
        mutex.release();
    }

    public void addAndWait(ComponentId component, ComponentTransfer details) throws InterruptedException {
        QueueElem temp = new QueueElem(details);
        mutex.acquire();
        queue.put(component, temp);
        mutex.release();
        temp.mutex.acquire();
    }

    public boolean getReservation(ComponentId component) throws InterruptedException {
        mutex.acquire();
        boolean result = queue.containsKey(component);
        if (!result) {
            mutex.release();
        }
        return result;
    }

    public void move(ComponentId component) {
        QueueElem elem = queue.get(component);
        queue.remove(component);
        mutex.release();
        elem.mutex.release();
    }

    public boolean placeReservation(ComponentId component) throws InterruptedException {
        mutex.acquire();
        QueueElem elem = queue.get(component);
        if (elem == null) {
            mutex.release();
            return false;

        }
        if (!queue.get(component).reservation.tryAcquire()) {
            mutex.release();
            return false;
        }
        return true;
    }

    public void releaseReservation(ComponentId component) throws InterruptedException {
        mutex.acquire();
        QueueElem elem = queue.get(component);
        elem.reservation.release();
        mutex.release();
    }

    public void notifyLast() throws InterruptedException {
        mutex.acquire();
        Iterator<ComponentId> componentIterator = queue.keySet().iterator();
        if (!componentIterator.hasNext()) {
            mutex.release();
            return;
        }
        ComponentId component = componentIterator.next();
        QueueElem elem = queue.get(component);
        queue.remove(component);
        elem.deleted = true;
        elem.mutex.release();
        mutex.release();
    }

    public void notifySpecific(ComponentId component) throws InterruptedException {
        mutex.acquire();
        QueueElem elem = queue.get(component);
        queue.remove(component);
        mutex.release();
        elem.mutex.release();
    }

    public Map<DeviceId, ComponentId> getDestinations() throws InterruptedException {
        Map<DeviceId, ComponentId> result = new HashMap<>();
        mutex.acquire();
        for (ComponentId comp : queue.keySet()) {
            ComponentTransfer elem = queue.get(comp).transfer;
            if (elem.getDestinationDeviceId() != null) {
                result.put(elem.getDestinationDeviceId(), comp);
            }
        }
        mutex.release();
        return result;
    }

}
