package cp2023.solution;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

import cp2023.base.ComponentId;
import cp2023.base.ComponentTransfer;
import cp2023.base.DeviceId;
import cp2023.base.StorageSystem;
import cp2023.exceptions.ComponentAlreadyExists;
import cp2023.exceptions.ComponentDoesNotExist;
import cp2023.exceptions.ComponentDoesNotNeedTransfer;
import cp2023.exceptions.ComponentIsBeingOperatedOn;
import cp2023.exceptions.DeviceDoesNotExist;
import cp2023.exceptions.IllegalTransferType;
import cp2023.exceptions.TransferException;

enum arriveOrReserve {
    RESERVE,
    RELEASE
}

enum addOrRemove {
    ADD,
    REMOVE
}

enum operation {
    INSERT,
    DELETE,
    TRANSFER
}

public class StorageSystemImpl implements StorageSystem {

    private final Semaphore checkConditions = new Semaphore(1);

    private final Map<DeviceId, DeviceImpl> devices;
    private final Set<ComponentId> components;
    private final Semaphore componentMutex = new Semaphore(1, true);
    private final Set<ComponentId> underTransfer = new HashSet<ComponentId>();
    // private final Semaphore underTransferMutex = new Semaphore(1, true);

    public StorageSystemImpl(Map<DeviceId, Integer> deviceTotalSlots,
            Map<ComponentId, DeviceId> componentPlacement) {

        this.devices = new HashMap<DeviceId, DeviceImpl>();
        this.components = new HashSet<ComponentId>();

        Map<DeviceId, Set<ComponentId>> temp = new HashMap<DeviceId, Set<ComponentId>>();

        if (componentPlacement != null) {
            for (Map.Entry<ComponentId, DeviceId> entry : componentPlacement.entrySet()) {

                components.add(entry.getKey());

                if (temp.containsKey(entry.getValue())) {
                    temp.get(entry.getValue()).add(entry.getKey());
                    continue;
                }
                Set<ComponentId> setToInsert = new HashSet<ComponentId>();
                setToInsert.add(entry.getKey());
                temp.put(entry.getValue(), setToInsert);

            }
        }

        for (Map.Entry<DeviceId, Integer> entry : deviceTotalSlots.entrySet()) {
            DeviceImpl deviceToAdd = new DeviceImpl(entry.getKey(), entry.getValue(),
                    temp.getOrDefault(entry.getKey(), null), this);
            devices.put(entry.getKey(), deviceToAdd);
        }

    }

    public Map<DeviceId, DeviceImpl> getDevices(Set<DeviceId> ids) {
        Map<DeviceId, DeviceImpl> result = new HashMap<DeviceId, DeviceImpl>();
        for (DeviceId elem : ids) {
            result.put(elem, devices.get(elem));
        }
        return result;
    }

    public void acquireMutex() throws InterruptedException {

        componentMutex.acquire();

    }

    public void releaseMutex() {

        componentMutex.release();

    }

    public void addComponent(ComponentId comp) {
        components.add(comp);
    }

    public void removeComponents(ComponentId comp) {
        components.remove(comp);
    }

    public boolean doIHaveComponent(ComponentId comp) {
        return components.contains(comp);
    }

    public void execute(ComponentTransfer transfer) throws TransferException {

        try {

            if (transfer.getComponentId() == null) {
                throw new IllegalArgumentException();
            }

            ComponentId comp = transfer.getComponentId();

            // Sprawdzamy czy transfer ma chociaż jeden DeviceId
            if ((transfer.getSourceDeviceId() == null && transfer.getDestinationDeviceId() == null)) {
                throw new IllegalTransferType(comp);
            }

            // Jeśli podano źródło to sprawdzamy czy istnieje
            if (transfer.getSourceDeviceId() != null && !devices.containsKey(transfer.getSourceDeviceId())) {
                throw new DeviceDoesNotExist(transfer.getSourceDeviceId());
            }

            // Jeśli podano cel to sprawdzamy czy istnieje
            if (transfer.getDestinationDeviceId() != null && !devices.containsKey(transfer.getDestinationDeviceId())) {

                throw new DeviceDoesNotExist(transfer.getDestinationDeviceId());
            }

            checkConditions.acquire();

            // // sprawdzamy czy element musi być transferowany
            // if (transfer.getDestinationDeviceId() != null) {
            //     DeviceImpl destDev = devices.get(transfer.getDestinationDeviceId());
            //     if (destDev.doIHave(comp)) {
            //         checkConditions.release();
            //         throw new ComponentDoesNotNeedTransfer(comp,
            //                 transfer.getDestinationDeviceId());
            //     }
            // }

            // sprawdzamy czy transfer dla komponentu został już zgłoszony
            // underTransferMutex.acquire();
            if (underTransfer.contains(comp)) {
                // underTransferMutex.release();
                checkConditions.release();
                throw new ComponentIsBeingOperatedOn(comp);
            }
            underTransfer.add(comp);
            // underTransferMutex.release();

            operation opType;
            DeviceImpl sourceDev = devices.getOrDefault(transfer.getSourceDeviceId(), null);
            DeviceImpl destDev = devices.getOrDefault(transfer.getDestinationDeviceId(), null);

            if (destDev == null) {
                opType = operation.DELETE;
            } else if (sourceDev == null) {
                opType = operation.INSERT;
            } else {
                opType = operation.TRANSFER;
            }

            if (opType == operation.DELETE || opType == operation.TRANSFER) {
                if (!sourceDev.doIHave(comp)) {
                    checkConditions.release();
                    throw new ComponentDoesNotExist(comp, sourceDev.id);
                }
            }

            if (opType == operation.INSERT) {
                // componentMutex.acquire();
                if (components.contains(comp)) {
                    // componentMutex.release();
                    checkConditions.release();
                    throw new ComponentAlreadyExists(comp);
                }
                // componentMutex.release();
            }

            if (opType == operation.TRANSFER) {

                // componentMutex.acquire();
                if (destDev.doIHave(comp)) {
                    // componentMutex.release();
                    checkConditions.release();
                    throw new ComponentDoesNotNeedTransfer(comp, destDev.id);
                }
                // componentMutex.release();
            }

            switch (opType) {
                case DELETE:

                    // Wiemy że delete wykonuje się zawsze, więc warunki bezpieczeństwa na pewno
                    // zostały spełnione
                    checkConditions.release();
                    // informujemy inny wątek że można zaczynać
                    Semaphore deleteDonePreparing = sourceDev.arriveOrReserve.releaseOldest();
                    // przygotowywujemy transfer
                    transfer.prepare();
                    // informujemy czekający wątek że wykonaliśmy prepare
                    deleteDonePreparing.release();
                    // Wykonujemy transfer
                    transfer.perform();
                    // Usuwamy element zurządzenia
                    sourceDev.addOrRemove.remove(comp);
                    // Usuwamy element z systemu
                    components.remove(comp);

                    break;

                case INSERT:

                    // Rezerwujemy miejsce na urządzeniu
                    AdditionMutexWrapper insertMutexes = destDev.arriveOrReserve.reserveForAddition(comp);
                    // teraz już wiemy że poprawność musi zostać zachowana, więc zwalniamy
                    checkConditions.release();
                    // czekamy aż będziemy w stanie rozpocząć transfer
                    insertMutexes.canStartPrep.acquire();
                    // rozpoczynamy transfer
                    transfer.prepare();
                    // czekamy aż będziemy mogli rozpocząć perform
                    insertMutexes.canStartPerf.acquire();
                    // Przenosimy dane
                    transfer.perform();
                    // Dodajemy element do urządzenia
                    destDev.addOrRemove.add(comp);
                    // Dodajemy element do systemu
                    components.add(comp);

                    break;

                case TRANSFER:

                    // tu trzeba będzie sprawdzić cykle

                    // Rezerwujemy miejsce na urządzeniu
                    TransferMutexWrapper transferMutexes = destDev.arriveOrReserve.reserveForTransfer(comp,
                            sourceDev.id);
                    // teraz już wiemy że poprawność musi zostać zachowana, więc zwalniamy
                    checkConditions.release();
                    // Czekamy aż będziemy mogli rozpocząć prepare
                    transferMutexes.canStartPrep.acquire();
                    // informujemy inny wątek że można zaczynać, z uwzględneniem cyklowania
                    Semaphore transferDonePreparing;
                    if (transferMutexes.cycleRemainder.list == null) {
                        transferDonePreparing = sourceDev.arriveOrReserve.releaseOldest();
                    } else {
                        transferDonePreparing = sourceDev.arriveOrReserve
                                .releaseSpecific(transferMutexes.cycleRemainder.list);
                    }
                    // rozpoczynamy transfer
                    transfer.prepare();
                    // informujemy czekający wątek że wykonaliśmy prepare
                    transferDonePreparing.release();
                    // czekamy aż będziemy mogli rozpocząć perform
                    transferMutexes.canStartPerf.acquire();
                    // Przenosimy dane
                    transfer.perform();
                    // Usuwamy element zurządzenia
                    sourceDev.addOrRemove.remove(comp);
                    // Dodajemy element do urządzenia
                    destDev.addOrRemove.add(comp);

                    break;
            }
            checkConditions.acquire();
            underTransfer.remove(comp);
            checkConditions.release();

        }

        catch (InterruptedException e) {
            throw new RuntimeException("panic: unexpected thread interruption");
        }

    }

    public LinkedList<ComponentId> lookForCycles(DeviceId source, DeviceId destination) throws InterruptedException {

        MultiMap<DeviceId, LinkedList<ComponentId>> nextMap = new MultiMap<DeviceId, LinkedList<ComponentId>>();

        List<LinkedList<ComponentId>> foundCycles = new LinkedList<LinkedList<ComponentId>>();
        Set<DeviceId> visited = new HashSet<DeviceId>();

        DeviceImpl sourceDevice = devices.get(source);
        Map<DeviceId, ComponentId> directions = sourceDevice.getDestinations();

        for (DeviceId dev : directions.keySet()) {
            LinkedList<ComponentId> path = new LinkedList<ComponentId>();
            path.add(directions.get(dev));
            nextMap.put(dev, path);
        }

        if (nextMap.keySet().contains(destination)) {

            for (LinkedList<ComponentId> elem : nextMap.get(destination)) {

                foundCycles.add(elem);

            }

        }

        while (true) {

            MultiMap<DeviceId, LinkedList<ComponentId>> temp = new MultiMap<DeviceId, LinkedList<ComponentId>>();

            Map<DeviceId, DeviceImpl> devices = this.getDevices(nextMap.keySet());

            for (DeviceId device : nextMap.keySet()) {

                DeviceImpl dev = devices.get(device);

                LinkedList<LinkedList<ComponentId>> paths = nextMap.get(device);

                Map<DeviceId, ComponentId> destinations = dev.getDestinations();

                // System.out.println(destinations + " " + dev.id);

                for (LinkedList<ComponentId> path : paths) {

                    for (DeviceId devi : destinations.keySet()) {

                        LinkedList<ComponentId> newPath = new LinkedList<ComponentId>(path);

                        newPath.add(destinations.get(devi));

                        temp.put(devi, newPath);

                    }
                }

            }

            Set<DeviceId> canReach = temp.keySet();

            canReach.removeAll(visited);

            if (canReach.isEmpty()) {
                break;
            }

            if (canReach.contains(destination)) {

                LinkedList<LinkedList<ComponentId>> cycles = temp.get(destination);

                for (LinkedList<ComponentId> cycle : cycles) {

                    foundCycles.add(cycle);

                    temp.remove(destination);

                    canReach.remove(destination);

                }

            }

            visited.addAll(canReach);

            nextMap = temp;

        }

        if (foundCycles.isEmpty()) {
            return null;
        } else {

            // System.out.println(foundCycles);

            List<ChooseRightCycleHelperType> iters = new LinkedList<ChooseRightCycleHelperType>();

            int helper = 0;

            for (LinkedList<ComponentId> elem : foundCycles) {
                iters.add(new ChooseRightCycleHelperType(helper, elem.iterator(), source));
                helper++;
            }

            while (iters.size() > 1) {

                int min = Integer.MAX_VALUE;

                List<ChooseRightCycleHelperType> temp = new LinkedList<ChooseRightCycleHelperType>();

                for (ChooseRightCycleHelperType elem : iters) {

                    DeviceImpl device = devices.get(elem.currentDevice);
                    ComponentId rrr = elem.iterator.next();
                    int pos = device.whatPositionAmI(rrr);

                    if (pos < min) {
                        min = pos;
                        temp.clear();
                        elem.currentDevice = device.getMapping(rrr);
                        temp.add(elem);
                    }

                    else if (pos == min) {
                        elem.currentDevice = device.getMapping(rrr);
                        temp.add(elem);
                    }

                }

                iters = temp;

            }

            return foundCycles.get(iters.get(0).ogPos);
        }

    }

}
