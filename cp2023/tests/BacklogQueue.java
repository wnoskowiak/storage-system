package cp2023.tests;

import java.util.HashMap;
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
        queue.remove(component);
        mutex.release();
    }

    public void notifyLast() throws InterruptedException {
        mutex.acquire();
        ComponentId component = queue.keySet().iterator().next();
        QueueElem elem = queue.get(component);
        queue.remove(component);
        mutex.release();
        elem.mutex.release();
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
