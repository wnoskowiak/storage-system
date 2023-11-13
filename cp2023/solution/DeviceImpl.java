package cp2023.solution;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
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
    // private final Set<ComponentId> reservations = new HashSet<ComponentId>();
    // private final Set<ComponentId> leaving = new HashSet<ComponentId>();
    // private final HashMap<DeviceId, Integer> skippers = new HashMap<DeviceId, Integer>();
    // private final TransferManager outgoing = new TransferManager();

    public final NewQueueImplemetation queue = new NewQueueImplemetation();

    // private Integer queueLength = 0;
    private Integer free = 0;

    // private final Semaphore leavingMutex = new Semaphore(1, true);
    // private final Semaphore memoryMutex = new Semaphore(1, true);
    // private final Semaphore outgoingMutex = new Semaphore(1, true);
    // private final Semaphore reservationsMutex = new Semaphore(1, true);
    // private final Semaphore skipperMutex = new Semaphore(1, true);
    public final ArriveOrReserve arriveOrReserve = new ArriveOrReserve();
    public final AddOrRemove addOrRemove = new AddOrRemove();
    public Semaphore stateMutex = new Semaphore(1);

    public DeviceImpl(DeviceId id, int numberOfSlots, Set<ComponentId> initialComponentIds, StorageSystemImpl system) {
        if (numberOfSlots <= 0) {
            throw new IllegalArgumentException();
        }
        this.numberOfSlots = numberOfSlots;
        this.system = system;
        this.id = id;

        if (initialComponentIds != null) {
            memory.addAll(initialComponentIds);
        }

        free = this.numberOfSlots - memory.size();

    }

    // public void addSkipper(DeviceId dev) throws InterruptedException {

    //     skipperMutex.acquire();

    //     if (skippers.containsKey(dev)) {
    //         int temp = skippers.get(dev);
    //         temp++;
    //         skippers.put(dev, temp);
    //         return;
    //     }

    //     skippers.put(dev, 1);
    //     skipperMutex.release();

    // }

    // public boolean removeSkipper(DeviceId dev) throws InterruptedException {

    //     if (dev == null) {
    //         return false;
    //     }

    //     boolean result;
    //     skipperMutex.acquire();

    //     if (skippers.containsKey(dev)) {
    //         result = true;
    //         int temp = skippers.get(dev);
    //         temp--;
    //         if (temp == 0) {
    //             skippers.remove(dev);
    //         } else {
    //             skippers.put(dev, temp);
    //         }
    //     } else {
    //         result = false;
    //     }

    //     skipperMutex.release();

    //     return result;
    // }

    public boolean doIHave(ComponentId elem) throws InterruptedException {

        stateMutex.acquire();

        boolean result = memory.contains(elem);

        stateMutex.release();

        return result;
    }

    // public boolean isBeingTransferred(ComponentId elem) throws InterruptedException {

    //     outgoingMutex.acquire();
    //     reservationsMutex.acquire();

    //     boolean result = outgoing.amIHere(elem) || reservations.contains(elem);
    //     reservationsMutex.release();
    //     outgoingMutex.release();
    //     return result;

    // }

    // public Map<DeviceId, ComponentId> getDestinations() throws
    // InterruptedException {

    // return queue.getDestinations();

    // // outgoingMutex.acquire();

    // // Set<DeviceId> result = outgoing.getDeviceIds();

    // // outgoingMutex.release();
    // // return result;
    // }

    public class AddOrRemove {

        public void remove(ComponentId elem) throws InterruptedException {
            stateMutex.acquire();
            memory.remove(elem);
            stateMutex.release();
        }

		public void add(ComponentId comp) throws InterruptedException {
            stateMutex.acquire();
            memory.add(comp);
            stateMutex.release();
        }

        

        // public synchronized void remove(ComponentId elem, boolean deletion) throws InterruptedException {
        //     memoryMutex.acquire();
        //     outgoingMutex.acquire();
        //     leavingMutex.acquire();

        //     if (deletion) {
        //         system.acquireMutex();
        //     }

        //     memory.remove(elem);
        //     outgoing.removeTransfer(elem);
        //     leaving.remove(elem);

        //     if (deletion) {
        //         system.removeComponents(elem);
        //     }

        //     memoryMutex.release();
        //     outgoingMutex.release();
        //     leavingMutex.release();
        //     if (deletion) {
        //         system.releaseMutex();
        //     }

        //     notify();
        // }

        // public synchronized void add(ComponentId elem, boolean deletion) throws InterruptedException {
        //     memoryMutex.acquire();
        //     reservationsMutex.acquire();

        //     if (memory.size() == numberOfSlots) {
        //         memoryMutex.release();
        //         reservationsMutex.release();
        //         wait();
        //         memoryMutex.acquire();
        //         reservationsMutex.acquire();
        //     }

        //     memory.add(elem);
        //     reservations.remove(elem);

        //     memoryMutex.release();
        //     reservationsMutex.release();
        // }
    }

    // public void acquireOutgoing() throws InterruptedException {
    //     outgoingMutex.acquire();
    // }

    // public void releaseOutgoing() {
    //     outgoingMutex.release();
    // }

    // public void markAsOutgoing(ComponentId comp, DeviceId dest) throws InterruptedException {

    //     outgoingMutex.acquire();

    //     outgoing.addTransfer(comp, dest);

    //     outgoingMutex.release();

    // }

    public class ArriveOrReserve {

        // private final Semaphore useMutex = new Semaphore(1);

        // public void release(ComponentId elem, DeviceId dev) throws InterruptedException {

        //     useMutex.acquire();
        //     if (queue.size() == 0) {
        //         free++;
        //     } else if (elem != null) {
        //         queue.notifySpecific(elem);
        //     } else {
        //         queue.notifyLast();
        //     }
        //     useMutex.release();

        // }

        // public void reserve(ComponentId elem, DeviceImpl source, ComponentTransfer transferDetails, boolean addition)
        //         throws InterruptedException {

        //     useMutex.acquire();

        //     if (free == 0) {

        //         boolean isCycle = false;

        //         if (!addition) {
        //             // tutaj musimy sprawdzić czy nie ma cyklu
        //             Map<DeviceId, Set<ComponentId>> nextMap = new HashMap<DeviceId, Set<ComponentId>>();
        //             Map<DeviceId, Set<ComponentId>> temp = new HashMap<DeviceId, Set<ComponentId>>();
        //             Set<DeviceId> visited = new HashSet<DeviceId>();

        //             Map<DeviceId, ComponentId> directions = queue.getDestinations();

        //             for (DeviceId dev : directions.keySet()) {
        //                 HashSet<ComponentId> path = new HashSet<ComponentId>();
        //                 path.add(directions.get(dev));
        //                 nextMap.put(dev, path);
        //             }

        //             while (true) {

        //                 Map<DeviceId, DeviceImpl> devices = system.getDevices(nextMap.keySet());

        //                 for (DeviceId device : nextMap.keySet()) {

        //                     DeviceImpl dev = devices.get(device);

        //                     Set<ComponentId> path = nextMap.get(device);

        //                     Map<DeviceId, ComponentId> destinations = dev.getDestinations();

        //                     for (DeviceId ddd : destinations.keySet()) {

        //                         path.add(destinations.get(ddd));

        //                         temp.put(ddd, path);

        //                     }

        //                 }

        //             }

        //         }

        //         if (!isCycle) {
        //             useMutex.release();
        //             queue.addAndWait(elem, transferDetails);
        //             useMutex.acquire();
        //         }
        //     } else {
        //         // tutaj jest wszystko git, dostajemy rezerwacje od razu
        //         free--;
        //     }

        //     if (addition) {
        //         system.acquireMutex();
        //         system.addComponent(elem);
        //         system.releaseMutex();
        //     }

        //     useMutex.release();
        // }

        public Semaphore releaseOldest() throws InterruptedException {
            stateMutex.acquire();
            if (queue.size() == 0) {
                free++;
                stateMutex.release();
                return new Semaphore(0);
            } else {
                NewQueueElement elem = queue.popLast();
                elem.waitForCondition.release();
                stateMutex.release();
                return elem.doneWithPrepare;
            }
        }

        public Semaphore releaseSpecific(LinkedList<ComponentId> cycleRemainder) throws InterruptedException {
            stateMutex.acquire();
            if (cycleRemainder.size() == 0) {
                stateMutex.release();
                return new Semaphore(1);
            } else {
                ComponentId myBoi = cycleRemainder.getFirst();
                cycleRemainder.remove();
                NewQueueElement elem = queue.popSpecific(myBoi);
                elem.cycleRemainders.list = cycleRemainder;
                elem.waitForCondition.release();
                stateMutex.release();
                return elem.doneWithPrepare;

            }

        }

        public AdditionMutexWrapper reserveForAddition(ComponentId comp) throws InterruptedException {
            stateMutex.acquire();
            if (free > 0) {
                free--;
                AdditionMutexWrapper result = new AdditionMutexWrapper(new Semaphore(1), new Semaphore(1));
                stateMutex.release();
                return result;
            } else {
                NewQueueElement qElem = queue.put(comp);
                AdditionMutexWrapper result = new AdditionMutexWrapper(qElem.waitForCondition, qElem.doneWithPrepare);
                stateMutex.release();
                return result;
            }
        }

        public TransferMutexWrapper reserveForTransfer(ComponentId comp, DeviceId source) throws InterruptedException {
            stateMutex.acquire();
            if (free > 0) {
                free--;
                TransferMutexWrapper result = new TransferMutexWrapper(null, new Semaphore(1), new Semaphore(1));
                stateMutex.release();
                return result;
            } else {
                // tu trzeba sprawdzić czy cykl istnieje
                LinkedList<ComponentId> cycle = system.lookForCycles(source, id);
                if (cycle != null) {
                    // tu robimy rozcyklowywanie
                    NewQueueElement qElem = queue.put(comp);
                    TransferMutexWrapper result = new TransferMutexWrapper(new LinkedListWrapper(cycle), new Semaphore(1),
                            qElem.doneWithPrepare);
                    stateMutex.release();
                    return result;
                } else {
                    NewQueueElement qElem = queue.put(comp);
                    queue.putConnection(comp, source);
                    TransferMutexWrapper result = new TransferMutexWrapper(qElem.cycleRemainders,
                            qElem.waitForCondition, qElem.doneWithPrepare);
                    stateMutex.release();
                    return result;
                }
            }
        }

    }

    public Map<DeviceId, ComponentId> getDestinations() throws InterruptedException {

        LinkedHashMap<ComponentId, DeviceId> incoming = queue.getConnections();

        Map<DeviceId, ComponentId> result = new HashMap<DeviceId, ComponentId>();

        for( ComponentId elem : incoming.keySet()) {

            DeviceId dev = incoming.get(elem);

            if(!result.containsKey(dev)) {

                result.put(dev, elem);

            }

        }

        return result;
    }
}
