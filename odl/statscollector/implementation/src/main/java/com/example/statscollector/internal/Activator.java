package com.example.statscollector.internal;


import org.apache.felix.dm.Component;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
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
        Object[] res = { StatsCollector.class };
        return res;
    }

    public void configureInstance(Component c, Object imp, String containerName) {
        if (imp.equals(StatsCollector.class)) {
            // export the service
            c.setInterface(new String[] { StatsCollector.class.getName() }, null);

        }
    }
}
