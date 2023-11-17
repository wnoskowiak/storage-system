package cp2023.solution;

import java.util.Iterator;

import cp2023.base.ComponentId;
import cp2023.base.DeviceId;

public class ChooseRightCycleHelperType {

    public final Iterator<ComponentId> iterator;

    public final int ogPos;

    public DeviceId currentDevice;

    public ChooseRightCycleHelperType(int position, Iterator<ComponentId> iterator, DeviceId currentDevice) {
        this.ogPos = position;
        this.iterator = iterator;
        this.currentDevice = currentDevice;
    }

}
