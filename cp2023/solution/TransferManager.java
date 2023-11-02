package cp2023.solution;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

import cp2023.base.ComponentId;
import cp2023.base.DeviceId;

public class TransferManager {

    private final Semaphore destMutex = new Semaphore(1, true);
    private final Semaphore objectsMutex = new Semaphore(1, true);
    private final Semaphore mapMutex = new Semaphore(1, true);

    private final Set<DeviceId> destinations = new HashSet<DeviceId>();
    private final Set<ComponentId> objectsToMove = new HashSet<ComponentId>();
    private final Map<ComponentId, DeviceId> whoGoesWhere = new HashMap<ComponentId, DeviceId>();

    public Set<DeviceId> getDeviceIds() throws InterruptedException {
        destMutex.acquire();
        Set<DeviceId> result = destinations;
        destMutex.release();
        return result;
    }

    public Set<ComponentId> getComponentIds() throws InterruptedException {
        objectsMutex.acquire();
        Set<ComponentId> result = objectsToMove;
        objectsMutex.release();
        return result;
    }

    public int size() throws InterruptedException {
        objectsMutex.acquire();
        int result = objectsToMove.size();
        objectsMutex.release();
        return result;
    }

    public DeviceId whereAmIGoing(ComponentId comp) throws InterruptedException {
        mapMutex.acquire();
        DeviceId result = whoGoesWhere.get(comp);
        mapMutex.release();
        return result;
    }

    public boolean amIHere(ComponentId comp) throws InterruptedException {
        objectsMutex.acquire();
        boolean result = objectsToMove.contains(comp);
        objectsMutex.release();
        return result;
    }

    public void addTransfer(ComponentId comp, DeviceId dev) throws InterruptedException {
        destMutex.acquire();
        objectsMutex.acquire();
        mapMutex.acquire();
        destinations.add(dev);
        objectsToMove.add(comp);
        whoGoesWhere.put(comp, dev);
        destMutex.release();
        objectsMutex.release();
        mapMutex.release();
    }

    public void removeTransfer(ComponentId comp) throws InterruptedException {
        destMutex.acquire();
        objectsMutex.acquire();
        mapMutex.acquire();
        objectsToMove.remove(comp);
        DeviceId temp = whoGoesWhere.get(comp);
        whoGoesWhere.remove(comp);
        destinations.remove(temp);
        destMutex.release();
        objectsMutex.release();
        mapMutex.release();
    }

}
