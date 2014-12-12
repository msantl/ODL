package com.example.statscollector.internal;

import com.example.statscollector.IStatsCollector;

import java.util.List;
import java.util.ArrayList;

import java.util.Map;
import java.util.Collections;

import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Iterator;
import java.util.Date;

import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.core.Node;
// import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.reader.NodeConnectorStatistics;
import org.opendaylight.controller.statisticsmanager.IStatisticsManager;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatsCollector implements IStatsCollector{
    private static final Logger log = LoggerFactory.getLogger(StatsCollector.class);
    private final Map<Integer, String> Cache = Collections
        .synchronizedMap(new LRUCache<Integer, String>(1000));

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

    public StatsCollector() {

    }

    void init() {
        log.debug("INIT called!");
        // init thread
    }

    void destroy() {
        log.debug("DESTROY called!");
    }

    void start() {
        log.debug("START called!");

        // get first stats
        Date date = new java.util.Date();
        System.out.println(date.getTime());

        getCurrentStatistics();

        // start the thread
    }

    void stop() {
        log.debug("STOP called!");
        // stop the thread
    }

    void getCurrentStatistics() {
        String containerName = "default";

        IStatisticsManager statsManager = (IStatisticsManager) ServiceHelper
            .getInstance(IStatisticsManager.class, containerName, this);

        ISwitchManager switchManager = (ISwitchManager) ServiceHelper
            .getInstance(ISwitchManager.class, containerName, this);

        for (Node node : switchManager.getNodes()) {
            System.out.println("Node: " + node);
            List<NodeConnectorStatistics> stat = statsManager
                .getNodeConnectorStatistics(node);

            for (NodeConnectorStatistics ncs : stat) {
                System.out.println(ncs.getReceiveByteCount() + " / " + ncs.getTransmitByteCount());
            }
        }

        return;
    }

    @Override
    public List<String> getTest() {
        List<String> ret = new ArrayList();

        ret.add("test1");
        ret.add("test2");
        ret.add("test3");

        return ret;
    }
}
