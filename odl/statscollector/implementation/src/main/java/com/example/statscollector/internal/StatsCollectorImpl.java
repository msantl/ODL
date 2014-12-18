package com.example.statscollector.internal;

import java.util.List;
import java.util.ArrayList;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Iterator;
import java.util.Date;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;

import com.example.statscollector.IStatsCollector;
import com.example.statscollector.Data;

import org.opendaylight.controller.sal.utils.IObjectReader;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.reader.NodeConnectorStatistics;
import org.opendaylight.controller.statisticsmanager.IStatisticsManager;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.switchmanager.Switch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatsCollectorImpl implements IStatsCollector, IObjectReader{
    private static final Logger log = LoggerFactory.getLogger(StatsCollectorImpl.class);
    private Thread statsCollector;
    private final Map<Long, Map<Node, List<Data> > > Cache = Collections
        .synchronizedMap(new LRUCache<Long, Map<Node, List<Data> > >(1000));

    // LRU cache class used to store limited data about stats
    private class LRUCache <K, V> extends LinkedHashMap <K, V> {

        private final int capacity;

        public LRUCache(int capacity) {
            super(capacity+1, 1.0f, true);
            this.capacity = capacity;
        }

        @Override
        protected boolean removeEldestEntry(final Map.Entry<K, V> eldest) {
            return (super.size() > this.capacity);
        }

    }

    void printMap(Map mp) {
        Iterator it = mp.entrySet().iterator();
        System.out.println("--------------------");
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            System.out.println(pairs.getKey() + " = " + pairs.getValue());
        }
        System.out.println("--------------------");
    }

    public StatsCollectorImpl() {
    }

    void init() {
        log.debug("INIT called!");
        // init thread
        statsCollector = new Thread(new StatsCollectorThread());
    }

    void destroy() {
        log.debug("DESTROY called!");
    }

    void start() {
        log.debug("START called!");
        // start the thread
        statsCollector.start();
    }

    void stop() {
        log.debug("STOP called!");
        // stop the thread
        statsCollector.interrupt();
    }

    Map<Node, List<Data> > getCurrentStatistics() {
        String containerName = "default";
        // key for the property map that contains the bandwodth information
        String propertyName = "bandwidth";

        Map<Node, List<Data> > edge = new HashMap();

        IStatisticsManager statsManager = (IStatisticsManager) ServiceHelper
            .getInstance(IStatisticsManager.class, containerName, this);

        ISwitchManager switchManager = (ISwitchManager) ServiceHelper
            .getInstance(ISwitchManager.class, containerName, this);

        // get statistics and bandwidth property
        for (Switch swc : switchManager.getNetworkDevices()) {
            // for each network device
            Node node = swc.getNode();
            // get stats about every nodeconnector from that node
            List<NodeConnectorStatistics> stat = statsManager
                .getNodeConnectorStatistics(node);

            Map<NodeConnector, Data> node_data = new HashMap();

            for (NodeConnector nc : swc.getNodeConnectors()) {
                // get node connector properties
                Map<String,Property> mp =
                    switchManager.getNodeConnectorProps(nc);

                Data data = new Data();

                // update node connector entry
                data.setNodeConnector(nc);
                // update bandwidth entry
                data.setBandwidth(mp.get(propertyName).getStringValue());

                node_data.put(nc, data);
            }

            for (NodeConnectorStatistics ncs : stat) {
                Data data = node_data.get(ncs.getNodeConnector());

                if (data == null) {
                    // skip statistics about switch<->controller connection
                    continue;
                }

                long sent = ncs.getReceivePacketCount() + ncs.getTransmitPacketCount();
                long drop = ncs.getReceiveDropCount() + ncs.getTransmitDropCount();

                // update packet drop entry
                data.setPacketDrop(drop);
                // update packet sent entry
                data.setPacketSent(sent);

                // update transmit byte count entry
                data.setTxCount(ncs.getTransmitByteCount());
                // update receive byte count entry
                data.setRxCount(ncs.getReceiveByteCount());

                node_data.put(ncs.getNodeConnector(), data);
            }

            List<Data> list_data = new ArrayList();

            for (NodeConnector key : node_data.keySet()) {
                // move full stats info in a list
                list_data.add(node_data.get(key));
            }
            // update result map
            edge.put(node, list_data);
        }
        return edge;
    }

    private class StatsCollectorThread implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(2000);
                    // get current time
                    Date date = new java.util.Date();
                    // get current statistics
                    Map<Node, List<Data> > res = getCurrentStatistics();

                    // store current statistics in LRU cache
                    Cache.put(date.getTime(), res);

                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    System.out.println("Something went wrong");
                }
            }
        }
    }

    // implemented interface methods that allows other components to
    // communicate with stats collector
    @Override
    public Map<Long, Map<Node, List<Data> > > getStats() {
        return new HashMap<Long, Map<Node, List<Data> > >(Cache);
    }

    @Override
    public List<String> getTest() {
        List<String> ret = new ArrayList();

        ret.add("test1");
        ret.add("test2");
        ret.add("test3");

        return ret;
    }

    @Override
    public Object readObject(ObjectInputStream ois)
        throws FileNotFoundException, IOException, ClassNotFoundException {
        return ois.readObject();
    }

}
