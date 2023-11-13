package cp2023.solution;

import java.util.LinkedHashMap;
import java.util.concurrent.Semaphore;

import cp2023.base.ComponentId;
import cp2023.base.DeviceId;

public class NewQueueImplemetation {

    private final Semaphore mutex = new Semaphore(1);
    private final LinkedHashMap<ComponentId, NewQueueElement> queue = new LinkedHashMap<ComponentId, NewQueueElement>();
    private final LinkedHashMap<ComponentId, DeviceId> connections = new LinkedHashMap<ComponentId, DeviceId>();

    public int size() throws InterruptedException {
        mutex.acquire();
        int result = queue.size();
        mutex.release();
        return result;
    }

    public NewQueueElement popLast() throws InterruptedException {
        mutex.acquire();
        ComponentId component = queue.keySet().iterator().next();
        NewQueueElement result = queue.get(component);
        connections.remove(component);
        queue.remove(component);
        mutex.release();
        return result;

    }

    public NewQueueElement popSpecific(ComponentId component) throws InterruptedException {
        mutex.acquire();
        NewQueueElement result = queue.get(component);
        connections.remove(component);
        queue.remove(component);
        mutex.release();
        return result;
    }

    public NewQueueElement put(ComponentId comp) throws InterruptedException {
        mutex.acquire();
        NewQueueElement result = new NewQueueElement();
        queue.put(comp, result);
        mutex.release();
        return result;
    }

    public void putConnection(ComponentId comp, DeviceId source) throws InterruptedException {
        mutex.acquire();
        connections.put(comp, source);
        mutex.release();
    }

    public LinkedHashMap<ComponentId, DeviceId> getConnections() throws InterruptedException {
        mutex.acquire();
        LinkedHashMap<ComponentId, DeviceId> result = new LinkedHashMap<ComponentId, DeviceId>(connections);
        mutex.release();
        return result;
    }

}
