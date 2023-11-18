package cp2023.solution;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class MultiMap<T1, T2> {

    private final Map<T1,LinkedList<T2>> map = new LinkedHashMap<T1,LinkedList<T2>>();

    public Set<T1> keySet() {
        return map.keySet();
    }

    public LinkedList<T2> get(T1 device) {
        return map.get(device);
    }

    public void put(T1 devi, T2 newPath) {
        if(map.containsKey(devi)) {
            LinkedList<T2> temp = map.get(devi);
            temp.add(newPath);
            map.put(devi, temp);
        }
        else {
            LinkedList<T2> temp = new LinkedList<T2>();
            temp.add(newPath);
            map.put(devi, temp);
        }
    }

    public LinkedList<T2> values() {
        LinkedList<T2> temp = new LinkedList<T2>();
        for(LinkedList<T2> elem : map.values()) {
            temp.addAll(elem);
        }

        return temp;

    }

    public void remove(T1 destination) {
        map.remove(destination);
    }

}
