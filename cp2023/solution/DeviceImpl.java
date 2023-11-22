package cp2023.solution;

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

    public final QueueImplemetation queue = new QueueImplemetation();

    private final LinkedList<Semaphore> free = new LinkedList<Semaphore>();

    public final ArriveOrReserve arriveOrReserve = new ArriveOrReserve();
    public final AddOrRemove addOrRemove = new AddOrRemove();
    public Semaphore stateMutex = new Semaphore(1);

    public DeviceImpl(DeviceId id, int numberOfSlots, Set<ComponentId> initialComponentIds, StorageSystemImpl system) {

        if (numberOfSlots <= 0) {
            throw new IllegalArgumentException();
        }

        if (id == null) {
            throw new IllegalArgumentException();
        }

        this.numberOfSlots = numberOfSlots;
        this.system = system;
        this.id = id;

        if (initialComponentIds != null) {

            if (numberOfSlots < initialComponentIds.size()) {
                throw new IllegalArgumentException();
            }

            memory.addAll(initialComponentIds);
        }

        for (int i = 0; i < this.numberOfSlots - memory.size(); i++) {
            free.add(new Semaphore(1));
        }

    }

    /**
     * Sprawdzamy czy urządzenie zawiera zadany element w pamięci
     * 
     * @param elem : id elementu do sprawdzenia
     * @return : boolean określający obecność elementu
     * @throws InterruptedException
     */
    public boolean doIHave(ComponentId elem) throws InterruptedException {

        stateMutex.acquire();

        boolean result = memory.contains(elem);

        stateMutex.release();

        return result;
    }

    /**
     * Sprawdzamy na której pozycji w kolejce stoi element o zadanym id
     * 
     * @param comp : id elementu do sprawdzenia
     * @return : int określający pozycje elementu
     * @throws InterruptedException
     */
    public int whatPositionAmI(ComponentId comp) throws InterruptedException {
        return queue.whatPos(comp);
    }

    /**
     * Sprawdzamy na miejsce na którym urządzeniu czeka transfer o podanym id
     * 
     * @param comp : id elementu do sprawdzenia
     * @return Id urządzenia na którym na miejsce czeka transfer
     * @throws InterruptedException
     */
    public DeviceId getMapping(ComponentId comp) throws InterruptedException {
        return queue.getMapping(comp);
    };

    public class AddOrRemove {

        /**
         * Dodajemy element to pamięci urządzenia
         * 
         * @param elem : id elementu do dodania
         * @throws InterruptedException
         */
        public void remove(ComponentId elem) throws InterruptedException {
            stateMutex.acquire();
            memory.remove(elem);
            stateMutex.release();
        }

        /**
         * Usuwamy element z pamięci urządzenia
         * 
         * @param comp
         * @throws InterruptedException
         */
        public void add(ComponentId comp) throws InterruptedException {
            stateMutex.acquire();
            memory.add(comp);
            stateMutex.release();
        }

    }

    public class ArriveOrReserve {

        public Semaphore release(LinkedList<ComponentId> cycleRemainder) throws InterruptedException {
            stateMutex.acquire();
            if (cycleRemainder == null) {
                if (queue.size() == 0) {
                    Semaphore result = new Semaphore(0);
                    free.add(result);
                    stateMutex.release();
                    return result;
                } else {
                    QueueElement elem = queue.popLast();
                    elem.waitForCondition.release();
                    stateMutex.release();
                    return elem.doneWithPrepare;
                }
            }
            if (cycleRemainder.size() == 0) {
                stateMutex.release();
                return new Semaphore(1);
            } else {
                ComponentId comp = cycleRemainder.getFirst();
                cycleRemainder.remove();
                QueueElement elem = queue.popSpecific(comp);
                elem.cycleRemainders.list = cycleRemainder;
                elem.waitForCondition.release();
                stateMutex.release();
                return elem.doneWithPrepare;

            }

        }

        /**
         * Rezerwujemy miejsce na urządzeniu docelowym
         * 
         * @param comp   : id transferowonego komponentu
         * @param source : urządzenie źródłowe
         * @param op     : typ wykonywanej operacji
         * @return wrapper mutexów na które należy zaczekać
         * @throws InterruptedException
         */
        public AdditionMutexWrapper reserveForAddition(ComponentId comp) throws InterruptedException {
            stateMutex.acquire();
            if (free.size() > 0) {
                Semaphore prepSem = free.remove();
                AdditionMutexWrapper result = new AdditionMutexWrapper(new Semaphore(1), prepSem);
                stateMutex.release();
                return result;
            } else {
                QueueElement qElem = queue.put(comp);
                AdditionMutexWrapper result = new AdditionMutexWrapper(qElem.waitForCondition, qElem.doneWithPrepare);
                stateMutex.release();
                return result;
            }
        }

        /**
         * Rezerwujemy miejsce na urządzeniu docelowym
         * 
         * @param comp   : id transferowonego komponentu
         * @param source : urządzenie źródłowe
         * @param op     : typ wykonywanej operacji
         * @return wrapper mutexów na które należy zaczekać
         * @throws InterruptedException
         */
        public TransferMutexWrapper reserveForTransfer(ComponentId comp, DeviceId source) throws InterruptedException {
            stateMutex.acquire();
            if (free.size() > 0) {
                Semaphore prepSem = free.remove();
                TransferMutexWrapper result = new TransferMutexWrapper(new LinkedListWrapper(null), new Semaphore(1),
                        prepSem);
                stateMutex.release();
                return result;
            } else {
                LinkedList<ComponentId> cycle = system.lookForCycles(source, id);
                if (cycle != null) {
                    QueueElement qElem = queue.put(comp);
                    cycle.add(comp);
                    TransferMutexWrapper result = new TransferMutexWrapper(new LinkedListWrapper(cycle),
                            new Semaphore(1),
                            qElem.doneWithPrepare);
                    stateMutex.release();
                    return result;
                } else {
                    QueueElement qElem = queue.put(comp);
                    queue.putConnection(comp, source);
                    TransferMutexWrapper result = new TransferMutexWrapper(qElem.cycleRemainders,
                            qElem.waitForCondition, qElem.doneWithPrepare);
                    stateMutex.release();
                    return result;
                }
            }
        }

    }

    /**
     * Odczytujemy transfery oczekujące na miejsce na urządzeniu
     * 
     * @return mapa określająca połączenia - kluczami są urządzenia z których
     *         transfery próbują przenieść elementy a wartościami id tych elementów
     * @throws InterruptedException
     */
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
