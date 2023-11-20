package cp2023.solution;

import java.util.LinkedHashMap;
import java.util.concurrent.Semaphore;

import cp2023.base.ComponentId;
import cp2023.base.DeviceId;

public class QueueImplemetation {

    private final Semaphore mutex = new Semaphore(1);
    private final LinkedHashMap<ComponentId, QueueElement> queue = new LinkedHashMap<ComponentId, QueueElement>();
    private final LinkedHashMap<ComponentId, DeviceId> connections = new LinkedHashMap<ComponentId, DeviceId>();

    public int size() throws InterruptedException {
        mutex.acquire();
        int result = queue.size();
        mutex.release();
        return result;
    }

    public QueueElement popLast() throws InterruptedException {
        mutex.acquire();
        ComponentId component = queue.keySet().iterator().next();
        QueueElement result = queue.get(component);
        connections.remove(component);
        queue.remove(component);
        mutex.release();
        return result;

    }

    public QueueElement popSpecific(ComponentId component) throws InterruptedException {
        mutex.acquire();
        QueueElement result = queue.get(component);
        connections.remove(component);
        queue.remove(component);
        mutex.release();
        return result;
    }

    public QueueElement put(ComponentId comp) throws InterruptedException {
        mutex.acquire();
        QueueElement result = new QueueElement();
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

    public DeviceId getMapping(ComponentId comp) throws InterruptedException {
        mutex.acquire();
        DeviceId result = connections.get(comp);
        mutex.release();
        return result;
    }

    public int whatPos(ComponentId comp) throws InterruptedException {
        mutex.acquire();
        int result = 0;
        for(ComponentId elem : queue.keySet()) {
            result++;
            if(elem == comp) {
                break;
            }
        }
        mutex.release();
        return result;
    }

}
