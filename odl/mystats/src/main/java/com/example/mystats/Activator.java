package com.example.mystats;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.opendaylight.controller.sal.packet.IListenDataPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator extends ComponentActivatorAbstractBase {
    protected static final Logger logger = LoggerFactory
            .getLogger(Activator.class);

    public void init() {
    }

    public void destroy() {
    }

    public Object[] getImplementations() {
        Object[] res = { MyStats.class };
        return res;
    }

    public void configureInstance(Component c, Object imp, String containerName) {
        if (imp.equals(MyStats.class)) {
            Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put("salListenerName", "mystats");

            // export the service
            c.setInterface(new String[] { MyStats.class.getName(),
                    IListenDataPacket.class.getName() }, props);
        }
    }
}
