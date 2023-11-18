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

    public final NewQueueImplemetation queue = new NewQueueImplemetation();

    private Integer free = 0;

    public final ArriveOrReserve arriveOrReserve = new ArriveOrReserve();
    public final AddOrRemove addOrRemove = new AddOrRemove();
    public Semaphore stateMutex = new Semaphore(1);

    public DeviceImpl(DeviceId id, int numberOfSlots, Set<ComponentId> initialComponentIds, StorageSystemImpl system) {

        if (numberOfSlots <= 0) {
            throw new IllegalArgumentException();
        }

        if (numberOfSlots < initialComponentIds.size()) {
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

    public boolean doIHave(ComponentId elem) throws InterruptedException {

        stateMutex.acquire();

        boolean result = memory.contains(elem);

        stateMutex.release();

        return result;
    }

    public int whatPositionAmI(ComponentId comp) throws InterruptedException {
        return queue.whatPos(comp);
    }

    public DeviceId getMapping(ComponentId comp) throws InterruptedException {
        return queue.getMapping(comp);
    };

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

    }

    public class ArriveOrReserve {

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
                TransferMutexWrapper result = new TransferMutexWrapper(new LinkedListWrapper(null), new Semaphore(1),
                        new Semaphore(1));
                stateMutex.release();
                // System.out.println("Transferer " + Thread.currentThread().getId() + " 1");
                return result;
            } else {
                // tu trzeba sprawdzić czy cykl istnieje
                LinkedList<ComponentId> cycle = system.lookForCycles(source, id);
                if (cycle != null) {
                    // tu robimy rozcyklowywanie
                    NewQueueElement qElem = queue.put(comp);
                    cycle.add(comp);
                    TransferMutexWrapper result = new TransferMutexWrapper(new LinkedListWrapper(cycle),
                            new Semaphore(1),
                            qElem.doneWithPrepare);
                    stateMutex.release();
                    // System.out.println("Transferer " + Thread.currentThread().getId() + " 2");
                    return result;
                } else {
                    NewQueueElement qElem = queue.put(comp);
                    // System.out.println("putting connection " + id);
                    queue.putConnection(comp, source);
                    // System.out.println(queue.getConnections().toString() + " " + id.toString());
                    // System.out.println(queue.getConnections().toString() + " " + id.toString());
                    TransferMutexWrapper result = new TransferMutexWrapper(qElem.cycleRemainders,
                            qElem.waitForCondition, qElem.doneWithPrepare);
                    stateMutex.release();
                    // System.out.println("Transferer " + Thread.currentThread().getId() + " 3 " +
                    // qElem.cycleRemainders);
                    return result;
                }
            }
        }

    }

    public Map<DeviceId, ComponentId> getDestinations() throws InterruptedException {

        LinkedHashMap<ComponentId, DeviceId> incoming = queue.getConnections();

        Map<DeviceId, ComponentId> result = new LinkedHashMap<DeviceId, ComponentId>();

        for (ComponentId elem : incoming.keySet()) {

            DeviceId dev = incoming.get(elem);

            if (!result.containsKey(dev)) {

                result.put(dev, elem);

            }

        }

        return result;
    }
}
