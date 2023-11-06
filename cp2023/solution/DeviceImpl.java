package cp2023.solution;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import cp2023.base.ComponentId;
import cp2023.base.DeviceId;

public class DeviceImpl {

    private final int numberOfSlots;
    public final DeviceId id;
    private final StorageSystemImpl system;
    private final Set<ComponentId> memory = new HashSet<ComponentId>();
    private final Set<ComponentId> reservations = new HashSet<ComponentId>();
    private final Set<ComponentId> leaving = new HashSet<ComponentId>();
    private final HashMap<DeviceId, Integer> skippers = new HashMap<DeviceId, Integer>();
    private final TransferManager outgoing = new TransferManager();

    private final Semaphore leavingMutex = new Semaphore(1, true);
    private final Semaphore memoryMutex = new Semaphore(1, true);
    private final Semaphore outgoingMutex = new Semaphore(1, true);
    private final Semaphore reservationsMutex = new Semaphore(1, true);
    private final Semaphore skipperMutex = new Semaphore(1, true);
    public final ArriveOrReserve arriveOrReserve = new ArriveOrReserve();
    public final AddOrRemove addOrRemove = new AddOrRemove();

    public DeviceImpl(DeviceId id, int numberOfSlots, Set<ComponentId> initialComponentIds, StorageSystemImpl system) {

        this.numberOfSlots = numberOfSlots;
        this.system = system;
        this.id = id;

        if (initialComponentIds != null) {
            memory.addAll(initialComponentIds);
        }

    }

    public void addSkipper(DeviceId dev) throws InterruptedException {

        skipperMutex.acquire();

        if (skippers.containsKey(dev)) {
            int temp = skippers.get(dev);
            temp++;
            skippers.put(dev, temp);
            return;
        }

        skippers.put(dev, 1);
        skipperMutex.release();

    }

    public boolean removeSkipper(DeviceId dev) throws InterruptedException {

        if (dev == null) {
            return false;
        }

        boolean result;
        skipperMutex.acquire();

        if (skippers.containsKey(dev)) {
            result = true;
            int temp = skippers.get(dev);
            temp--;
            if (temp == 0) {
                skippers.remove(dev);
            } else {
                skippers.put(dev, temp);
            }
        } else {
            result = false;
        }

        skipperMutex.release();

        return result;
    }

    public boolean doIHave(ComponentId elem) throws InterruptedException {

        memoryMutex.acquire();

        boolean result = memory.contains(elem);

        memoryMutex.release();
        return result;
    }

    public boolean isBeingTransferred(ComponentId elem) throws InterruptedException {

        outgoingMutex.acquire();
        reservationsMutex.acquire();

        boolean result = outgoing.amIHere(elem) || reservations.contains(elem);

        reservationsMutex.release();
        outgoingMutex.release();
        return result;

    }

    public Set<DeviceId> getDestinations() throws InterruptedException {

        outgoingMutex.acquire();

        Set<DeviceId> result = outgoing.getDeviceIds();

        outgoingMutex.release();
        return result;
    }

    public class AddOrRemove {

        public synchronized void remove(ComponentId elem, boolean deletion) throws InterruptedException {
            memoryMutex.acquire();
            outgoingMutex.acquire();
            leavingMutex.acquire();

            if (deletion) {
                system.acquireMutex();
            }

            memory.remove(elem);
            outgoing.removeTransfer(elem);
            leaving.remove(elem);

            if (deletion) {
                system.removeComponents(elem);
            }

            memoryMutex.release();
            outgoingMutex.release();
            leavingMutex.release();
            if (deletion) {
                system.releaseMutex();
            }

            notify();
        }

        public synchronized void add(ComponentId elem, boolean deletion) throws InterruptedException {
            memoryMutex.acquire();
            reservationsMutex.acquire();

            if (memory.size() == numberOfSlots) {
                memoryMutex.release();
                reservationsMutex.release();
                wait();
                memoryMutex.acquire();
                reservationsMutex.acquire();
            }

            memory.add(elem);
            reservations.remove(elem);

            memoryMutex.release();
            reservationsMutex.release();
        }

    }

    public void acquireOutgoing() throws InterruptedException {
        outgoingMutex.acquire();
    }

    public void releaseOutgoing() {
        outgoingMutex.release();
    }

    public void markAsOutgoing(ComponentId comp, DeviceId dest) throws InterruptedException {

        outgoingMutex.acquire();

        outgoing.addTransfer(comp, dest);

        outgoingMutex.release();

    }

    public class ArriveOrReserve {

        public synchronized void release(ComponentId elem, DeviceId dev, boolean addition) throws InterruptedException {

            leavingMutex.acquire();

            leaving.add(elem);

            leavingMutex.release();

            if (!removeSkipper(dev)) {
                System.out.println("===== " + Thread.currentThread().getId() + " has sent a notify"
                        + " ===");
                notify();
            }
        }

        public synchronized void reserve(ComponentId elem, DeviceImpl source, boolean addition)
                throws InterruptedException {

            reservationsMutex.acquire();
            leavingMutex.acquire();
            memoryMutex.acquire();

            if (numberOfSlots - memory.size() + leaving.size() - reservations.size() <= 0) {

                reservationsMutex.release();
                leavingMutex.release();
                memoryMutex.release();

                if (!addition) {
                    source.markAsOutgoing(elem, id);
                }

                boolean isCycle = false;
                Map<DeviceId, DeviceId> nextMap = new HashMap<DeviceId, DeviceId>();

                if (!addition) {
                    Set<DeviceId> visited = new HashSet<DeviceId>();

                    Set<DeviceImpl> temp = new HashSet<DeviceImpl>();

                    for (DeviceId dupa : getDestinations()) {
                        nextMap.put(dupa, id);
                    }

                    while (true) {

                        temp = system.getDevices(nextMap.keySet());

                        nextMap.clear();

                        for (DeviceImpl el : temp) {
                            if (el != null) {

                                for (DeviceId lead : el.getDestinations()) {
                                    nextMap.put(lead, el.id);
                                }
                            }
                        }

                        for (DeviceId elm : visited) {
                            nextMap.remove(elm);
                        }

                        System.out.println(nextMap);

                        if (nextMap.isEmpty()) {
                            break;
                        }

                        if (nextMap.containsKey(id)) {
                            isCycle = true;
                            break;
                        }

                        visited.addAll(nextMap.keySet());

                    }
                }

                if (!isCycle) {
                    System.out.println("===== " + Thread.currentThread().getId() + " is waiting"
                            + " ===");
                    wait();
                    System.out.println("===== " + Thread.currentThread().getId() + " is going"
                            + " ===");
                } else {

                    addSkipper(nextMap.get(id));

                    System.out.println("=====  ja " + id.toString() + " dodaję siebie do " + nextMap.get(id).toString()
                            + " ===");

                }

                reservationsMutex.acquire();
                leavingMutex.acquire();
                memoryMutex.acquire();

            }

            else {
                System.out.println("===== " + Thread.currentThread().getId() + " no i chuj no i cześć"
                        + " ===");
            }

            if (addition) {
                system.acquireMutex();
                system.addComponent(elem);
            }

            reservations.add(elem);

            if (addition) {
                system.releaseMutex();
            }

            reservationsMutex.release();
            leavingMutex.release();
            memoryMutex.release();

        }
    }
}
