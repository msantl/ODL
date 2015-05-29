package com.example.samcra;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.opendaylight.controller.sal.packet.IListenDataPacket;
import org.opendaylight.controller.sal.packet.IDataPacketService;
import org.opendaylight.controller.topologymanager.ITopologyManager;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.statisticsmanager.IStatisticsManager;

public class Activator extends ComponentActivatorAbstractBase {
    protected static final Logger logger = LoggerFactory
            .getLogger(Activator.class);

    public void init() {
    }

    public void destroy() {
    }

    public Object[] getImplementations() {
        Object[] res = { Samcra.class };
        return res;
    }

    public void configureInstance(Component c, Object imp, String containerName) {
        if (imp.equals(Samcra.class)) {
            Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put("salListenerName", "samcra");

            // export the service
            c.setInterface(new String[] { Samcra.class.getName(),
                    IListenDataPacket.class.getName() }, props);

            c.add(createContainerServiceDependency(containerName).setService(
                    ISwitchManager.class).setCallbacks(
                    "setSwitchManager", "unsetSwitchManager")
                    .setRequired(true));

            c.add(createContainerServiceDependency(containerName).setService(
                    ITopologyManager.class).setCallbacks(
                    "setTopologyManager", "unsetTopologyManager")
                    .setRequired(true));

            c.add(createContainerServiceDependency(containerName).setService(
                    IStatisticsManager.class).setCallbacks(
                    "setStatisticsManager", "unsetStatisticsManager")
                    .setRequired(true));

            c.add(createContainerServiceDependency(containerName).setService(
                    IForwardingRulesManager.class).setCallbacks(
                    "setForwardingRulesManager", "unsetForwardingRulesManager")
                    .setRequired(true));

            c.add(createContainerServiceDependency(containerName).setService(
                    IDataPacketService.class).setCallbacks(
                    "setDataPacketService", "unsetDataPacketService")
                    .setRequired(true));

            c.add(createContainerServiceDependency(containerName).setService(
                    IfIptoHost.class).setCallbacks(
                    "setIfIptoHost", "unsetIfIptoHost")
                    .setRequired(true));
        }
    }
}
