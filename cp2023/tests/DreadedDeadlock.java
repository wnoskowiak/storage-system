package cp2023.tests;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import cp2023.base.ComponentId;
import cp2023.base.ComponentTransfer;
import cp2023.base.DeviceId;
import cp2023.base.StorageSystem;
import cp2023.exceptions.TransferException;
import cp2023.solution.StorageSystemFactory;

public class DreadedDeadlock {


    public static void main(String[] args) {
        StorageSystem system = setupSystem();
        Collection<Thread> users = setupTransferers(system);
        runTransferers(users);
    }

    private final static StorageSystem setupSystem() {
        DeviceId dev1 = new DeviceId(1);
        // DeviceId dev2 = new DeviceId(2);
        // DeviceId dev3 = new DeviceId(3);
        // DeviceId dev4 = new DeviceId(4);
        
        ComponentId comp1 = new ComponentId(101);
        ComponentId comp2 = new ComponentId(102);
        // ComponentId comp3 = new ComponentId(103);
        // ComponentId comp4 = new ComponentId(104);
        
        HashMap<DeviceId, Integer> deviceCapacities = new HashMap<>(4);
        deviceCapacities.put(dev1, 1);
        // deviceCapacities.put(dev2, 1);
        // deviceCapacities.put(dev3, 1);
        // deviceCapacities.put(dev4, 1);
        
        HashMap<ComponentId, DeviceId> initialComponentMapping = new HashMap<>();

        // initialComponentMapping.put(comp1, dev1);
        // initialComponentMapping.put(comp2, dev2);
        // initialComponentMapping.put(comp3, dev3);
        // initialComponentMapping.put(comp4, dev4);
        
        return StorageSystemFactory.newSystem(deviceCapacities, initialComponentMapping);
    }
    
    private final static Collection<Thread> setupTransferers(StorageSystem system) {
        // FIXME: debug
        ArrayList<Thread> transferer = new ArrayList<>();
        transferer.add(new Thread(new Runnable() {
            @Override
            public void run() {
                // sleep(1000);
                System.out.println("Transferer " + Thread.currentThread().getId() + " has started.");
                executeTransfer(system, 101, 0, 1, 20);
                System.out.println("Transferer " + Thread.currentThread().getId() + " has finished.");
            }
        }));
        transferer.add(new Thread(new Runnable() {
            @Override
            public void run() {
                sleep(500);
                System.out.println("Transferer " + Thread.currentThread().getId() + " has started.");
                executeTransfer(system, 102, 0, 1, 20);
                System.out.println("Transferer " + Thread.currentThread().getId() + " has finished.");
            }
        }));
        transferer.add(new Thread(new Runnable() {
            @Override
            public void run() {
                sleep(1000);
                System.out.println("Transferer " + Thread.currentThread().getId() + " has started.");
                executeTransfer(system, 101, 1, 0, 10);
                System.out.println("Transferer " + Thread.currentThread().getId() + " has finished.");
            }
        }));
        // transferer.add(new Thread(new Runnable() {
        //     @Override
        //     public void run() {
        //         System.out.println("Transferer " + Thread.currentThread().getId() + " has started.");
        //         executeTransfer(system, 104, 4, 1, 10);
        //         System.out.println("Transferer " + Thread.currentThread().getId() + " has finished.");
        //     }
        // }));
        return transferer;
    }
    
    private final static void runTransferers(Collection<Thread> users) {
        for (Thread t : users) {
            t.start();
        }
        for (Thread t : users) {
            try {
                t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException("panic: unexpected thread interruption", e);
            }
        }
    }

    
    
    private final static CompTransfImpl executeTransfer(
            StorageSystem system,
            int compId,
            int srcDevId,
            int dstDevId,
            long duration
    ) {
        CompTransfImpl transfer =
                new CompTransfImpl(
                        new ComponentId(compId),
                        srcDevId > 0 ? new DeviceId(srcDevId) : null,
                        dstDevId > 0 ? new DeviceId(dstDevId) : null,
                        duration
                );
        try {
            system.execute(transfer);
        } catch (TransferException e) {
            throw new RuntimeException("Uexpected transfer exception: " + e.toString(), e);
        }
        return transfer;
    }
    
    private final static void sleep(long duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            throw new RuntimeException("panic: unexpected thread interruption", e);
        }
    }
    

    
    private final static class CompTransfImpl implements ComponentTransfer {
        private static int uidGenerator = 0;
        private final int uid;
        private final long owningThread;
        private final Integer phantomSynchronizer;
        private final ComponentId compId;
        private final DeviceId srcDevId;
        private final DeviceId dstDevId;
        private final long duration;
        private boolean prepared;
        private boolean started;
        private boolean done;
        
        private final static synchronized int generateUID() {
            return ++uidGenerator;
        }
        
        public CompTransfImpl(
                ComponentId compId,
                DeviceId srcDevId,
                DeviceId dstDevId,
                long duration
        ) {
            this.uid = generateUID();
            this.phantomSynchronizer = 19;
            this.owningThread = Thread.currentThread().getId();
            this.compId = compId;
            this.srcDevId = srcDevId;
            this.dstDevId = dstDevId;
            this.duration = duration;
            this.prepared = false;
            this.started = false;
            this.done = false;
            System.out.println("Transferer " + this.owningThread +
                    " is about to issue transfer " + this.uid +
                    " of " + this.compId + " from " + this.srcDevId +
                    " to " + this.dstDevId + ".");
        }
        
        @Override
        public ComponentId getComponentId() {
            return this.compId;
        }

        @Override
        public DeviceId getSourceDeviceId() {
            return this.srcDevId;
        }

        @Override
        public DeviceId getDestinationDeviceId() {
            return this.dstDevId;
        }

        @Override
        public void prepare() {
            synchronized (this.phantomSynchronizer) {
                if (this.prepared) {
                    throw new RuntimeException(
                            "Transfer " + this.uid + " is being prepared more than once!");
                }
                if (this.owningThread != Thread.currentThread().getId()) {
                    throw new RuntimeException(
                            "Transfer " + this.uid +
                            " is being prepared by a different thread that scheduled it!");
                }
                this.prepared = true;
            }
            System.out.println("Transfer " + this.uid + " of " + this.compId +
                    " from " + this.srcDevId + " to " + this.dstDevId +
                    " has been prepared by user " + Thread.currentThread().getId() + ".");
        }

        @Override
        public void perform() {
            synchronized (this.phantomSynchronizer) {
                if (! this.prepared) {
                    throw new RuntimeException(
                            "Transfer " + this.uid + " has not been prepared " +
                            "before being performed!");
                }
                if (this.started) {
                    throw new RuntimeException(
                            "Transfer " + this.uid + " is being started more than once!");
                }
                if (this.owningThread != Thread.currentThread().getId()) {
                    throw new RuntimeException(
                            "Transfer " + this.uid +
                            " is being performed by a different thread that scheduled it!");
                }
                this.started = true;
            }
            System.out.println("Transfer " + this.uid + " of " + this.compId +
                    " from " + this.srcDevId + " to " + this.dstDevId + " has been started.");
            sleep(this.duration);
            synchronized (this.phantomSynchronizer) {
                this.done = true;
            }
            System.out.println("Transfer " + this.uid + " of " + this.compId +
                    " from " + this.srcDevId + " to " + this.dstDevId + " has been completed.");
        }
        
    }
    
    
}
