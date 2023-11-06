package cp2023.solution;

import java.util.HashMap;
import java.util.HashSet;
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

    private final Map<DeviceId, DeviceImpl> devices;
    private final Set<ComponentId> components;
    private final Semaphore componentMutex = new Semaphore(1, true);
    private final Set<ComponentId> underTransfer = new HashSet<ComponentId>();
    private final Semaphore underTransferMutex = new Semaphore(1, true);

    public StorageSystemImpl(Map<DeviceId, Integer> deviceTotalSlots,
            Map<ComponentId, DeviceId> componentPlacement) {

        this.devices = new HashMap<DeviceId, DeviceImpl>();
        this.components = new HashSet<ComponentId>();

        Map<DeviceId, Set<ComponentId>> temp = new HashMap<DeviceId, Set<ComponentId>>();

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

        for (Map.Entry<DeviceId, Integer> entry : deviceTotalSlots.entrySet()) {
            DeviceImpl deviceToAdd = new DeviceImpl(entry.getKey(), entry.getValue(),
                    temp.getOrDefault(entry.getKey(), null), this);
            devices.put(entry.getKey(), deviceToAdd);
        }

    }

    public Set<DeviceImpl> getDevices(Set<DeviceId> ids) {
        Set<DeviceImpl> result = new HashSet<DeviceImpl>();
        for (DeviceId elem : ids) {
            result.add(devices.get(elem));
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

            // sprawdzamy czy element musi być transferowany
            if (transfer.getDestinationDeviceId() != null) {
                DeviceImpl sourceDev = devices.get(transfer.getDestinationDeviceId());
                if (sourceDev.doIHave(comp)) {
                    throw new ComponentDoesNotNeedTransfer(comp,
                            transfer.getDestinationDeviceId());
                }
            }

            // sprawdzamy czy transfer dla komponentu został już zgłoczony
            underTransferMutex.acquire();
            if (underTransfer.contains(comp)) {
                underTransferMutex.release();
                throw new ComponentIsBeingOperatedOn(comp);
            }
            underTransfer.add(comp);
            underTransferMutex.release();

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
                    throw new ComponentDoesNotExist(comp, sourceDev.id);
                }
            }

            if (opType == operation.INSERT) {
                componentMutex.acquire();
                if (components.contains(comp)) {
                    componentMutex.release();
                    throw new ComponentAlreadyExists(comp);
                }
                componentMutex.release();
            }

            if (opType == operation.TRANSFER) {

                componentMutex.acquire();
                if (destDev.doIHave(comp)) {
                    componentMutex.release();
                    throw new ComponentDoesNotNeedTransfer(comp, destDev.id);
                }
                componentMutex.release();
            }

            switch (opType) {
                case DELETE:

                    // informujemy inne wątki że można zaczynać
                    sourceDev.arriveOrReserve.release(comp, null, false);
                    // przygotowywujemy transfer
                    transfer.prepare();
                    // Usuwamy element
                    sourceDev.addOrRemove.remove(comp, true);
                    // Wykonujemy transfer
                    transfer.perform();

                    break;

                case INSERT:

                    // Rezerwujemy miejsce na urządzeniu
                    destDev.arriveOrReserve.reserve(comp, null, true);
                    // rozpoczynamy transfer
                    transfer.prepare();
                    // Zajmujemy miejsce na dysku
                    destDev.addOrRemove.add(comp, false);
                    // Przenosimy dane
                    transfer.perform();

                    break;

                case TRANSFER:
                    
                    // sourceDev.markAsOutgoing(comp, destDev.id);
                    // Rezerwujemy miejsce na urządzeniu
                    destDev.arriveOrReserve.reserve(comp, sourceDev, false);
                    // informujemy inne wątki że można zaczynać
                    sourceDev.arriveOrReserve.release(comp, destDev.id, false);
                    // rozpoczynamy transfer
                    transfer.prepare();
                    // Usuwamy element
                    sourceDev.addOrRemove.remove(comp, false);
                    // Zajmujemy miejsce na dysku
                    destDev.addOrRemove.add(comp, false);
                    // Przenosimy dane
                    transfer.perform();

                    break;
            }

            underTransferMutex.acquire();
            underTransfer.remove(comp);
            underTransferMutex.release();

        }

        catch (InterruptedException e) {
            throw new RuntimeException("panic: unexpected thread interruption");
        }

    }

}
