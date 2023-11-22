package cp2023.solution;

import java.util.LinkedHashMap;
import java.util.concurrent.Semaphore;

import cp2023.base.ComponentId;
import cp2023.base.DeviceId;

public class QueueImplemetation {

    private final Semaphore mutex = new Semaphore(1);
    private final LinkedHashMap<ComponentId, QueueElement> queue = new LinkedHashMap<ComponentId, QueueElement>();
    private final LinkedHashMap<ComponentId, DeviceId> connections = new LinkedHashMap<ComponentId, DeviceId>();

    /**
     * Sprawdzamy rozmiar kolejki
     * 
     * @return rozmiar kolejki
     * @throws InterruptedException
     */
    public int size() throws InterruptedException {
        mutex.acquire();
        int result = queue.size();
        mutex.release();
        return result;
    }

    /**
     * Sciągamy najstarszy element z kolejki
     * 
     * @return najstarszy element z kolejki
     * @throws InterruptedException
     */
    public QueueElement popLast() throws InterruptedException {
        mutex.acquire();
        ComponentId component = queue.keySet().iterator().next();
        QueueElement result = queue.get(component);
        connections.remove(component);
        queue.remove(component);
        mutex.release();
        return result;

    }

    /**
     * Sciągamy ostatni element z kolejki
     * 
     * @param component Identyfikator elementu związanege z interesującym nas
     *                  czekającym
     * @return szukany element
     * @throws InterruptedException
     */
    public QueueElement popSpecific(ComponentId component) throws InterruptedException {
        mutex.acquire();
        QueueElement result = queue.get(component);
        connections.remove(component);
        queue.remove(component);
        mutex.release();
        return result;
    }

    /**
     * Wstawiamy element do kolejki
     * 
     * @param comp : identyfikator elementu
     * @return element wstawiony do kolejki
     * @throws InterruptedException
     */
    public QueueElement put(ComponentId comp) throws InterruptedException {
        mutex.acquire();
        QueueElement result = new QueueElement();
        queue.put(comp, result);
        mutex.release();
        return result;
    }

    /**
     * Wstawiamy identyfikator oczekującego połączenia
     * 
     * @param comp   identyfikator transferowanego elementu
     * @param source urządzenie źródłowe
     * @throws InterruptedException
     */
    public void putConnection(ComponentId comp, DeviceId source) throws InterruptedException {
        mutex.acquire();
        connections.put(comp, source);
        mutex.release();
    }

    /**
     * Odczytujemy dane określające oczekujące połączenia
     * 
     * @return Mapa reprezentująca oczekujące połączenia, klucze określają
     *         identyfikatory czekających komponentów a wartości urządzenia z
     *         których są one przenoszone
     * @throws InterruptedException
     */
    public LinkedHashMap<ComponentId, DeviceId> getConnections() throws InterruptedException {
        mutex.acquire();
        LinkedHashMap<ComponentId, DeviceId> result = new LinkedHashMap<ComponentId, DeviceId>(connections);
        mutex.release();
        return result;
    }

    /**
     * Sprawdzamy z którego urządzenia zadany element jest przenoszony
     * @param comp : identyfikator elementu
     * @return id urządzenia z którego urządzenia element jest przenoszony
     * @throws InterruptedException
     */
    public DeviceId getMapping(ComponentId comp) throws InterruptedException {
        mutex.acquire();
        DeviceId result = connections.get(comp);
        mutex.release();
        return result;
    }

    /**
     * Sprawdzamy na której pozycji w kolejce stoi transfer podanego komponentu
     * @param comp : identyfikator komponentui
     * @return integer określający pozycję w kolejce
     * @throws InterruptedException
     */
    public int whatPos(ComponentId comp) throws InterruptedException {
        mutex.acquire();
        int result = 0;
        for (ComponentId elem : queue.keySet()) {
            result++;
            if (elem == comp) {
                break;
            }
        }
        mutex.release();
        return result;
    }

}
